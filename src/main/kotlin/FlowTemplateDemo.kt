import flow.audio.Audio
import flow.autoupdate.AutoUpdate
import flow.autoupdate.AutoUpdate.autoUpdate
import flow.bpm.BeatClock
import flow.envelope.LinearCapacitor
import flow.bpm.toIntervalCount
import flow.color.ColorRepo
import flow.color.ColorRepo.ColorRoles.*
import flow.content.VisualGroup
import flow.fx.MirrorFilter
import flow.fx.MirrorFilter.LookupFunctions.*
import flow.input.InputScheme.TrackTypes.PIANO
import flow.input.InputScheme.TrackTypes.TOGGLE
import flow.input.inputScheme
import flow.rendering.RenderPipeline
import flow.ui.UiDisplay
import org.openrndr.Fullscreen
import org.openrndr.KEY_ESCAPE
import org.openrndr.KEY_SPACEBAR
import org.openrndr.application
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.extra.fx.blur.ApproximateGaussianBlur
import org.openrndr.extra.fx.blur.GaussianBloom
import org.openrndr.extra.fx.distort.Perturb
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.math.smootherstep
import org.openrndr.panel.elements.round
import org.openrndr.shape.contour
import util.CyclicFlag
import util.TWO_PI
import util.lerp
import kotlin.math.*

/**
 * Demo of the "Flow" template.
 */
fun main() = application {
    configure {
        fullscreen = Fullscreen.CURRENT_DISPLAY_MODE
        display = displays.last()
    }

    program {
        // Init inputScheme
        val inputScheme = inputScheme(keyboard)

        // Init beatClock
        val bpm = 120.0 // <- Play your favorite song. Set its bpm here.
        val beatClock = extend(BeatClock(bpm))

        // Init autoUpdate
        extend(AutoUpdate)

        // Init colors
        val colorRepo = ColorRepo(ColorRepo.DEMO_PALETTE)

        // Init audio input
        val audio = Audio(
            sampleRate = 44100,
            bufferSize = 8192,
            overlap = 8192 - 1024,
        )

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

        // Specify audio processors
        val volProcessor = audio.createVolumeProcessor()
        val constantQ = audio.createConstantQProcessor(2, Audio.DEFAULT_RANGES, 40)
        audio.start()

        // Init render pipeline
        val renderPipeline = RenderPipeline(width, height, drawer)

        // Init Fx
        val blur = ApproximateGaussianBlur()
        val bloom = GaussianBloom()
        val mirrorFx = MirrorFilter(renderPipeline.stencilBuffer)
        val perturb = Perturb()
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
            val ringOpacity by LinearCapacitor(0.5, 0.1).autoUpdate {
                update(beatClock.deltaSeconds, inputScheme.isKeyActive("q"))
            }

            // If active, draw a center diamond and/or a ring of diamonds around it.
            override fun Drawer.draw() {

                // Center diamond
                val isShowingMain = inputScheme.isKeyActive("c").not()
                if (isShowingMain) {
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
            val alphaFac by LinearCapacitor(0.5, 0.5).autoUpdate {
                update(beatClock.deltaSeconds, inputScheme.isKeyActive("v"))
            }

            // Audio mode
            val audioMode = CyclicFlag("Magnitudes", "Bands")

            // Screen vars
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

            fun Drawer.drawBands() {
                val bandedVolList = constantQ.filteredBandedVolumes
                bandedVolList.forEachIndexed { i, vol ->
                    val freqBand = constantQ.freqBands[i]

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

            fun Drawer.drawMagnitudes() {
                val volList = constantQ.filteredMagnitudes
                volList.forEachIndexed { i, vol ->
                    val x0 = (i+0.1) / volList.size * (hiX - loX) + loX
                    val x1 = (i+0.9) / volList.size * (hiX - loX) + loX
                    val mixPerc = i * 1.0 / volList.size

                    fill = colorRepo[PRIMARY].mix(colorRepo[TERTIARY], mixPerc).opacify(0.5 * alphaFac).toRGBa()
                    stroke = null
                    drawVolBar(x0, x1, vol)
                }
            }

            fun Drawer.drawSoundPressureLevel() {
                val baseVol = volProcessor.filteredLastVolume

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

            // Circle of the mirror effect (recursive texture).
            // It grows if activated, and shrinks if deactivated.
            val maxR = Vector2(0.3 * width, 0.3 * height).length
            val alphaFac by LinearCapacitor(0.5, 0.5).autoUpdate {
                update(beatClock.deltaSeconds, inputScheme.isKeyActive("m"))
            }

            val triangleR = 20.0
            val triangleVertices = List(3) { Vector2(triangleR, 0.0).rotate(90.0 + it*120.0) }

            var mirrorFlipX = false

            fun triangleContour(center: Vector2, angleOff: Double) = contour {
                repeat(3) {
                    moveOrLineTo(center + triangleVertices[it].rotate(angleOff))
                }
                close()
            }

            var rotateAndScale_angle by mirrorFx.parameters

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
                contour(triangleContour(center0, -kick*30.0))
                contour(triangleContour(center1, kick*30.0))

                // Draw the mirror effect stencil.
                // 0 is identity function, 4 is scaleAndRotate function.
                drawer.isolatedWithTarget(renderPipeline.stencilTarget) {
                    clear(IDENTITY.r)

                    fill = ROTATE_AND_SCALE.r
                    stroke = null
                    strokeWeight = 0.0
                    circle(width / 2.0, height / 2.0, maxR * alphaFac)

                    if (mirrorFlipX) {
                        fill = FLIP_X.r
                        stroke = null
                        val rect = drawer.bounds.scaledBy(yScale = 1.0, xScale = 0.5, vAnchor = 0.5, uAnchor = 0.0)
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
            track(TOGGLE, "f1", "Toggle this controls display")

            // Hard-coded input bindings
            keyDown {
                KEY_ESCAPE.bind("Exit Application") { audio.stop(); application.exit() }
                KEY_SPACEBAR.bind("Reset Beat Clock") { beatClock.resetTime(program.seconds) }
                "8".bind("BPM x0.5") { beatClock.animateTo(bpm = beatClock.bpm / 2.0, program.seconds, 0.1) }
                "9".bind("BPM x2.0") { beatClock.animateTo(bpm = beatClock.bpm * 2.0, program.seconds, 0.1) }
                "0".bind("BPM Reset") { beatClock.animateTo(bpm = bpm, program.seconds, 0.0)}
                "b".bind("Cycle Audio mode") { audioGroup.audioMode.next() }
                "n".bind("Toggle mirror flip X") { mirrorGroup.mirrorFlipX = !mirrorGroup.mirrorFlipX}
                "p".bind("Cycle Perturb amount") { perturbAmount.next() }
            }
        }

        // Init UI display
        val uiDisplay = UiDisplay(inputScheme).apply {
            trackValue("BPM") { "${beatClock.bpm}" }
            trackValue("Phase") { "${beatClock.phase.round(2)}" }
            trackValue("Audio mode") { audioGroup.audioMode.value }
            trackValue("Perturbations") { "${perturbAmount.value}" }
        }
        uiDisplay.alphaCap = LinearCapacitor(0.1, 0.5).autoUpdate {
            update(beatClock.deltaSeconds, inputScheme.isKeyActive("f1").not())
        }

        // Set Fx chain
        renderPipeline.setFxChain {
            // Update parameters
            perturb.phase = seconds * 0.01
            perturb.decay = + volProcessor.filteredLastVolume.map(0.3, 0.7, 1.0, 0.0)
            perturb.gain = 0.8

            // Repeat 0 to 3 perturbs
            repeat(perturbAmount.value) { perturb.apply(drawBuffer) }
            // Add "hazy glow" with blur+bloom
            blur.apply(drawBuffer)
            bloom.apply(drawBuffer)
            // Resolve the content of the draw buffer to the image buffer. (For example, rescale it to fit to screen.)
            drawBuffer.copyTo(imageBuffer)
            // Apply mirror effect
            mirrorFx.apply(imageBuffer, tmpBuffer = tmpBuffer)
        }

        // Draw loop
        extend {
            // Perform the render operation with the specified draw block
            renderPipeline.render {
                // Draw visual groups
                audioGroup.draw()
                circleGroup.draw()
                diamondGroup.draw()
                mirrorGroup.draw()
            }

            // Draw controls
            uiDisplay.displayOnDrawer(drawer)
        }

    }
}
