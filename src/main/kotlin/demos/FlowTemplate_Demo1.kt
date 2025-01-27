package demos

import be.tarsos.dsp.AudioEvent
import flow.FlowProgram.Companion.flowProgram
import flow.FlowProgramConfig
import flow.audio.*
import flow.bpm.toIntervalCount
import flow.color.ColorRepo
import flow.color.ColorRepo.ColorRoles.*
import flow.content.VisualGroup
import flow.envelope.LinearCapacitor
import flow.envelope.keyAutoUpdate
import flow.fx.MirrorFilter.LookupFunctions.*
import flow.input.InputScheme.TrackTypes.PIANO
import flow.input.InputScheme.TrackTypes.TOGGLE
import flow.realtime.DynamicRange
import flow.realtime.OneEuroMultiFilter
import flow.realtime.oneEuroFilter.OneEuroFilter
import flow.rendering.image
import flow.util.CyclicFlag
import flow.util.TWO_PI
import flow.util.createTriangleContour
import flow.util.lerp
import org.openrndr.Fullscreen
import org.openrndr.KEY_SPACEBAR
import org.openrndr.application
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.math.Vector2
import org.openrndr.math.clamp
import org.openrndr.math.map
import org.openrndr.math.smootherstep
import org.openrndr.panel.elements.round
import kotlin.math.*

/**
 * Demo 1 of the "Flow" template.
 */
fun main() = application {
    configure {
        fullscreen = Fullscreen.CURRENT_DISPLAY_MODE
        display = displays.last()
    }

    val config = FlowProgramConfig(
        initialBpm = 125.0 // <- Play your favorite song. Set its bpm here.
    )
    flowProgram(config) {

        // Init colors
        val colorRepo = ColorRepo(ColorRepo.DEMO_PALETTE)

        // Init audio
        val audio = object: Audio(flowProgram, bufferSize = 4096, overlap = 4096 - 1024) {

            private val volumeProcessor = VolumeProcessor()
            private val relVolSmoother = OneEuroFilter(1.0, 0.5, 1.0, 0.0)
            val relVolSmoothed by relVolSmoother

            val numberOfBins = 64
            val bandRanges = DEFAULT_RANGES
            private val magnitudeProcessor = MagnitudeProcessor(numberOfBins, sampleRate, bufferSize)
            private val magsSmoother = OneEuroMultiFilter(1.0, 0.5, 1.0, List(numberOfBins) { 0.0 } )
            val magnitudesSmoothed by magsSmoother
            private val bandPassProcessors = bandRanges.map { createBandPass(it, sampleRate * 1.0) }
            private val bandsSmoother = OneEuroMultiFilter(1.0, 0.5, 1.0, List(bandRanges.size) { 0.0 } )
            val bandsSmoothed by bandsSmoother

            val dynRange = DynamicRange(LOWEST_SPL, HIGHEST_SPL, 0.02)

            // Specify audio process
            override fun setProcess(audioEvent: AudioEvent, dt: Double) {
                volumeProcessor.process(audioEvent)
                val relVol = volumeProcessor.volume.toRelativeVolume()
                relVolSmoother.filter(relVol, dt)

                magnitudeProcessor.process(audioEvent)
                val mags = magnitudeProcessor.magnitudes
                magsSmoother.filter(mags.map { it.toDb().toRelativeVolume() }, dt)

                val bandPassedMags = bandPassProcessors.map { bandPass ->
                    bandPass.process(audioEvent)
                    audioEvent.getdBSPL().clamp(LOWEST_SPL, HIGHEST_SPL).toRelativeVolume()
                }
                bandsSmoother.filter(bandPassedMags, dt)

                dynRange.update(volumeProcessor.volume)
            }
        }
        // Also start the audio immediately
        audio.start()

        // Specify beat-based values
        val kick by beatClock.bindEnvelopeBySegments(1.0) {
            lastX = 0.9
            segmentJoin(0.50, 1.0)
            segmentJoin(0.75, 0.1) via { smootherstep(0.0, 1.0, it) }
            segmentJoin(0.85, 0.0)
            segmentJoin(1.00, 0.9) via { x: Double -> x.pow(3.0) }
        }
        val flash by beatClock.bindEnvelope(1/2.0) { phase ->
            val interval = 1.0 / 4.0
            val count = phase.toIntervalCount(interval)
            if (count % 2 == 0) 1.0 else 0.0
        }
        val ebbAndFlow by beatClock.bindEnvelopeBySegments(4.0) {
            segmentJoin(2.0, 1.0) via { x: Double -> smootherstep(0.0, 1.0, x) }
            segmentJoin(4.0, 0.0) via { x: Double -> smootherstep(0.0, 1.0, x) }
        }

        // Setup fx
        val perturbAmount = CyclicFlag(0, 1, 2, 3)

        // Init visual groups
        val circleGroup = object: VisualGroup(program) {
            val sizeRange = 25.0..35.0

            override fun Drawer.draw() {

                // Draw a circle at each corner of the screen
                listOf(
                    Vector2(0.2, 0.2),
                    Vector2(0.2, 0.8),
                    Vector2(0.8, 0.8),
                    Vector2(0.8, 0.2)
                ).forEachIndexed { i, (x, y) ->
                    val colorIndex = i % 2 + 1
                    var color = colorRepo[colorIndex]
                    if (inputScheme.isKeyActive("e")) color = color.opacify(flash)

                    fill = color.toRGBa()
                    stroke = null
                    val r = kick.map(0.0, 1.0, sizeRange.start, sizeRange.endInclusive)
                    circle(x * width, y * height, r)
                }

                // Draw a circle in each compass direction around center
                val relR = kick.map(0.0, 1.0, 0.15, 0.35)
                "w,a,s,d".split(",").forEachIndexed { i, key ->
                    val isActive = inputScheme.isKeyActive(key)
                    if (!isActive) return@forEachIndexed
                    val pos = drawer.bounds.center + Vector2(0.0, -height*relR).rotate(- i * 90.0)

                    fill = colorRepo[PRIMARY].toRGBa()
                    stroke = null
                    circle(pos, sizeRange.endInclusive)
                }
            }
        }

        val diamondGroup = object: VisualGroup(program) {
            // Main diamond values
            val mainSize = 75.0

            // Diamond ring values
            val ringCount = 12
            val ringRadius = 100.0
            val ringSize = 10.0
            var ringRot = 0.0

            // Opacity of the ring is controlled by a capacitor.
            val ringOpacity by LinearCapacitor(0.5, 0.1).keyAutoUpdate(flowProgram, "q")

            // If active, draw a center diamond and/or a ring of diamonds around it.
            override fun Drawer.draw() {

                // Center diamond
                val isShowingCenter = inputScheme.isKeyActive("c").not()
                if (isShowingCenter) {
                    fill = colorRepo[PRIMARY].opacify(kick * 0.5 + 0.5).toRGBa()
                    stroke = null
                    drawDiamond(width / 2.0, height / 2.0, mainSize)
                }

                // Draw a ring of diamonds around the main diamond.
                ringRot += kick * 0.05
                val size = ringSize * if (inputScheme.isKeyActive("e")) flash else 1.0
                val center = bounds.center
                val angleStep = PI * 2 / ringCount
                for (i in 0 until ringCount) {
                    val angle = i * angleStep + ringRot
                    val x = center.x + ringRadius * cos(angle)
                    val y = center.y + ringRadius * sin(angle)
                    fill = colorRepo[i%2 + 1].opacify(ringOpacity).toRGBa()
                    stroke = null
                    drawDiamond(x, y, size)
                }

            }

            // Draws a diamond at the specified position.
            fun Drawer.drawDiamond(x: Double, y: Double, size: Double) {
                this.isolated {
                    translate(x, y)
                    rotate(45.0)
                    rectangle(-size/2.0, -size/2.0, size, size)
                }
            }

        }

        val audioGroup = object: VisualGroup(program) {

            // Fade in or fade out, as alpha factor controlled by "v"
            val alphaFac by LinearCapacitor(0.5, 0.5).keyAutoUpdate(flowProgram, "v")

            // Audio mode determines how the audio is visualized
            val audioMode = CyclicFlag("Magnitudes", "Bands")

            // Screen space vars
            val loX = width * 0.25
            val hiX = width * 0.75
            val loY = height * 0.75
            val hiY = height * 0.25

            // Draws rectangles based on the volume of the corresponding frequency bands (raw magnitudes/by range).
            // Draws a rectangle for the overall volume on top.
            override fun Drawer.draw() {
                when (audioMode.value) {
                    "Bands" -> drawBands()
                    "Magnitudes" -> drawMagnitudes()
                }
                drawer.drawSoundPressureLevel()
            }

            // Draws bands for the ranges of frequencies.
            fun Drawer.drawBands() {
                val bandedVolList = audio.bandsSmoothed
                bandedVolList.forEachIndexed { i, vol ->
                    val freqBand = audio.bandRanges[i]

                    val x0 = log2(freqBand.start)
                        .map(log2(Audio.LOWEST_FQ), log2(Audio.HIGHEST_FQ), loX, hiX)
                    val x1 = log2(freqBand.endInclusive)
                        .map(log2(Audio.LOWEST_FQ), log2(Audio.HIGHEST_FQ), loX, hiX)
                    val mixPerc = i.toDouble() / bandedVolList.size

                    fill = colorRepo[PRIMARY].mix(colorRepo[TERTIARY], mixPerc).opacify(0.5 * alphaFac).toRGBa()
                    stroke = null
                    drawVolBar(x0, x1, vol)
                }
            }

            // Visualizes the raw magnitudes of the audio.
            fun Drawer.drawMagnitudes() {
                val volList = audio.magnitudesSmoothed
                volList.forEachIndexed { i, vol ->
                    val x0 = (i+0.1) / volList.size * (hiX - loX) + loX
                    val x1 = (i+0.9) / volList.size * (hiX - loX) + loX
                    val mixPerc = i * 1.0 / volList.size

                    fill = colorRepo[PRIMARY].mix(colorRepo[TERTIARY], mixPerc).opacify(0.5 * alphaFac).toRGBa()
                    stroke = null
                    drawVolBar(x0, x1, vol)
                }
            }

            // Draws a rectangle for the overall volume on top.
            fun Drawer.drawSoundPressureLevel() {
                val baseVol = audio.relVolSmoothed

                fill = colorRepo[SECONDARY].opacify(0.2 * alphaFac).toRGBa()
                stroke = null
                drawVolBar(loX, hiX, baseVol)
            }

            fun Drawer.drawVolBar(x0: Double, x1: Double, volume: Double) {
                val volY = loY.lerp(hiY, volume)
                rectangle(x0, loY, x1 - x0, volY - loY)
            }
        }

        val mirrorGroup = object: VisualGroup(program) {

            // A circular shape using the mirror effect (recursive texture).
            // It grows if activated, and shrinks if deactivated.
            val maxR = Vector2(0.3 * width, 0.3 * height).length
            val alphaFac by LinearCapacitor(0.5, 0.5).keyAutoUpdate(flowProgram, "m")

            var mirrorFlipX = false
            var mirrorFlipY = false

            var rotateAndScale_angle by renderPipeline.mirrorFx.parameters

            override fun Drawer.draw() {
                // Update the mirror effect parameters
                rotateAndScale_angle = ebbAndFlow * 0.01 + 0.05

                // Fade in the triangle duo together with the mirror effect.
                // The triangles jiggle across circular arc close to the mirror rim.
                val baseCenter = bounds.center - Vector2(maxR + 40.0, 0.0)
                val off = Vector2(35.0, 0.0).rotate(sin(ebbAndFlow * TWO_PI)*60.0 + 180.0)
                val center0 = baseCenter + off
                val center1 = Vector2(width - center0.x, center0.y)

                fill = colorRepo[PRIMARY].opacify(0.5 * alphaFac).toRGBa()
                stroke = null
                val c1 = createTriangleContour(center0, 20.0, -kick*30.0)
                val c2 = createTriangleContour(center1, 20.0, kick*30.0)
                contours(listOf(c1, c2))

                // Draw the mirror effect stencil.
                drawer.isolatedWithTarget(renderPipeline.stencilTarget) {
                    clear(IDENTITY.r)

                    fill = ROTATE_AND_SCALE.r
                    stroke = null
                    circle(width / 2.0, height / 2.0, maxR * alphaFac)

                    if (mirrorFlipX) {
                        fill = FLIP_X.r
                        val rect = drawer.bounds.scaledBy(xScale = 0.5, yScale = 1.0, uAnchor = 0.0, vAnchor = 0.5)
                        rectangle(rect)
                    }
                    if (mirrorFlipY) {
                        fill = FLIP_Y.r
                        val rect = drawer.bounds.scaledBy(xScale = 1.0, yScale = 0.5, uAnchor = 0.5, vAnchor = 0.0)
                        rectangle(rect)
                    }
                }
            }
        }

        // Define controls for Input Scheme
        inputScheme.apply {
            // Tracked keys
            track(TOGGLE, "q", "Toggle diamond ring")
            track(TOGGLE, "c", "Toggle center diamond")
            track(TOGGLE, "e", "Toggle flash")
            track(PIANO, "w,a,s,d".split(","), "Bouncy balls")
            track(TOGGLE, "v", "Toggle Audio visualization")
            track(TOGGLE, "m", "Toggle mirror fx")

            // Hard-coded input bindings
            keyDown {
                KEY_SPACEBAR.bind("Reset Beat Clock") { beatClock.resetTime() }
                "8".bind("BPM -1") { beatClock.animateTo(bpm = beatClock.bpm - 1.0, 0.1) }
                "9".bind("BPM +1") { beatClock.animateTo(bpm = beatClock.bpm + 1.0, 0.1) }
                "0".bind("BPM Reset") { beatClock.animateTo(bpm = config.initialBpm, 0.0) }
                "b".bind("Cycle Audio mode") { audioGroup.audioMode.next() }
                "j".bind("Toggle mirror flip X") { mirrorGroup.mirrorFlipX = !mirrorGroup.mirrorFlipX }
                "k".bind("Toggle mirror flip Y") { mirrorGroup.mirrorFlipY = !mirrorGroup.mirrorFlipY }
                "p".bind("Cycle Perturb amount") { perturbAmount.next() }
            }
        }

        // Define UI display
        uiDisplay.apply {
            trackValue("Phase") { "${beatClock.phase.round(2)}" }
            trackValue("Audio mode") { audioGroup.audioMode.value }
            trackValue("Perturbations") { "${perturbAmount.value}" }
            trackValue("Volume") { "${audio.relVolSmoothed.round(2)}" }
        }

        // Draw loop
        extend {
            // Perform the render operation with the specified draw block
            renderPipeline.render {
                drawBuffer.clear()
                // Draw visual groups
                audioGroup.draw()
                circleGroup.draw()
                diamondGroup.draw()
                mirrorGroup.draw()

                // Set fx values
                perturb.phase = seconds * 0.01
                perturb.decay = + audio.relVolSmoothed.map(0.3, 0.7, 1.0, 0.0)
                perturb.gain = 0.8

                // Repeat 0 to 3 perturbs
                repeat(perturbAmount.value) { perturb.apply(drawBuffer) }
                // Add "hazy glow" with bloom
                bloom.apply(drawBuffer)
                // Resolve the content of the draw buffer to the image buffer. (For example, rescale it to fit to screen.)
                drawBuffer.copyTo(imageBuffer)
                // Apply mirror effect
                mirrorFx.apply(imageBuffer, useCopyBuffer = tmpBuffer)
                // Apply denoise to reduce salt-and-pepper noise
                denoise.apply(imageBuffer, useCopyBuffer = tmpBuffer)
            }

            // Draw the result to screen
            drawer.image(renderPipeline)

            // Draw controls
            uiDisplay.displayOnDrawer(drawer)
        }

    }
}
