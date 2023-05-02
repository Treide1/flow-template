import flow.audio.Audio
import flow.bpm.BeatClock
import flow.bpm.envelope.Capacitor
import flow.bpm.envelope.Envelope
import flow.bpm.toIntervalCount
import flow.colorRepo.ColorRepo
import flow.content.VisualGroup
import flow.fx.FxRepo
import flow.input.InputScheme.TrackTypes.PIANO
import flow.input.InputScheme.TrackTypes.TOGGLE
import flow.input.inputScheme
import flow.ui.UiDisplay
import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.fx.blur.ApproximateGaussianBlur
import org.openrndr.extra.fx.blur.FrameBlur
import org.openrndr.extra.fx.blur.GaussianBloom
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.panel.elements.round
import util.CyclicFlag
import util.lerp
import kotlin.math.*

/**
 * Template for "Flow" programs.
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
        val beatClock = extend(BeatClock(125.0))

        // Init beat-based values
        val kick by beatClock.bindEnvelopeBySegments(1.0) {
            segmentJoin(0.15, 0.8)
            segmentJoin(0.2, 1.0)
            segmentJoin(1.0, 0.0) via { x: Double -> x.pow(3.0) }
        }
        val flash by beatClock.bindEnvelope(1/2.0) { phase ->
            val interval = 1.0 / 4.0
            val count = phase.toIntervalCount(interval)
            if (count % 2 == 0) 1.0 else 0.0
        }

        // Init audio input
        val audio = Audio(
            sampleRate = 44100,
            bufferSize = 8192,
            overlap = 8192 - 1024,
        )
        val volProcessor = audio.createVolumeProcessor()

        val ranges = listOf(
            Audio.BASS,
            Audio.LOW_MID,
            Audio.MID,
            Audio.TREBLE,
            Audio.BRILLIANCE,
        )
        val constantQ = audio.createConstantQProcessor(2, ranges, 40)
        audio.start()

        // Init colors
        val colorRepo = ColorRepo {
            palette = listOf(
                "#90F0CB", // primary
                "#A38641", // secondary
                "#CE60F0", // tertiary
            ).map { ColorRGBa.fromHex(it) }
        }

        // Init pipeline
        val rt = renderTarget(width, height) {
            colorBuffer(ColorFormat.RGBa, ColorType.FLOAT32)
            depthBuffer()
        }
        val drawBuffer = rt.colorBuffer(0)
        val joinBuffer = drawBuffer.createEquivalent()

        // Init Fx
        val blur = ApproximateGaussianBlur()
        val bloom = GaussianBloom()
        val frameBlur = FrameBlur()
        val fxRepo = FxRepo {
            blur.apply(drawBuffer)
            bloom.apply(drawBuffer)
            overlay(drawBuffer, joinBuffer)
            frameBlur.apply(joinBuffer)
        }

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
                    var color = colorRepo.palette[colorIndex]
                    if (inputScheme.isKeyActive("e")) color = color.opacify(flash)
                    fill = color
                    stroke = null
                    val r = kick.map(0.0, 1.0, sizeRange.start, sizeRange.endInclusive)
                    circle(x * width, y * height, r)
                }

                // Draw a circle in each diamond direction on screen
                val flashGate = if (inputScheme.isKeyActive("e")) flash else 0.0
                val relR = max(kick, flashGate).map(0.0, 1.0, 0.15, 0.35)
                "w,a,s,d".split(",").forEachIndexed { i, key ->
                    val isActive = inputScheme.isKeyActive(key)
                    if (!isActive) return@forEachIndexed
                    val pos = drawer.bounds.center + Vector2(0.0, -height*relR).rotate(- i * 90.0)
                    fill = colorRepo.palette[0]
                    stroke = null
                    circle(pos, sizeRange.endInclusive)
                }
            }
        }

        val diamondGroup = object: VisualGroup(program) {
            var isShowingMain = true
            val mainSize = 75.0

            var isShowingRing = false
            val ringCount = 12
            val ringRadius = 100.0
            val ringSize = 10.0
            var ringRot = 0.0

            // Opacity of the ring is controlled by a capacitor.
            val capacitor = Capacitor(0.0, 0.8).apply {
                onGateOpen = Envelope(1.0) { t ->
                    if (t < 0.4) (t / 0.4).pow(1/2.0) // Reaches 1.0
                    else 1.0.lerp(holdValue, (t - 0.4) / 0.6)
                }
                onGateClosed = Envelope (0.1) { t ->
                    holdValue.lerp(offValue, t / 0.1)
                }
            }
            val ringOpacity by capacitor

            override fun Drawer.draw() {
                isShowingMain = inputScheme.isKeyActive("c").not()
                isShowingRing = inputScheme.isKeyActive("q")

                // Center diamond
                if (isShowingMain) {
                    val mainColor = colorRepo.palette[0].opacify(kick * 0.5 + 0.5)
                    fill = mainColor
                    stroke = null
                    drawDiamond(width / 2.0, height / 2.0, mainSize)
                }

                capacitor.update(0.016, isShowingRing)

                // Draw a ring of diamonds around the main diamond.
                ringRot += kick * 0.05
                val size = ringSize * if (inputScheme.isKeyActive("e")) flash else 1.0
                val center = bounds.center
                val angleStep = PI * 2 / ringCount
                for (i in 0 until ringCount) {
                    val angle = i * angleStep + ringRot
                    val x = center.x + ringRadius * cos(angle)
                    val y = center.y + ringRadius * sin(angle)
                    fill = colorRepo.palette[i%2 + 1].opacify(ringOpacity)
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

            val capacitor = Capacitor().apply {
                onGateOpen = Envelope(0.5) {
                    it / 0.5
                }
                onGateClosed = Envelope(0.5) {
                    1.0 - it / 0.5
                }
            }
            val alphaFac by capacitor

            val audioMode = CyclicFlag(listOf(
                "Magnitudes", "Ranges"
            ))

            // Draws equidistant rectangles on the screen, with height based on the volume of the
            // corresponding band passed volume processor.
            // The volumeProcessor is drawn above it with alpha = 0.5 and with PINK fill color.
            override fun Drawer.draw() {
                capacitor.update(0.016, inputScheme.isKeyActive("v"))

                val baseVol =  volProcessor.filteredLastVolume // volProcessor.volumeBuffer.lastOrNull() ?: return

                val loX = width * 0.25
                val hiX = width * 0.75
                val loY = height * 0.75
                val hiY = height * 0.25

                // Draw rectangles
                if (audioMode.value == "Ranges") {
                    val rangedVolList = constantQ.filteredRangedVolumesList
                    rangedVolList.forEachIndexed { i, vol ->
                        val volY = loY.lerp(hiY, vol)

                        val freqRange = constantQ.rangeList[i]
                        val xList = listOf(freqRange.start, freqRange.endInclusive).map {
                            log2(it)
                        }.map {
                            it.map(log2(Audio.LOWEST_FQ), log2(Audio.HIGHEST_FQ), loX, hiX)
                        }
                        val x0 = xList[0]
                        val x1 = xList[1]

                        val mixPerc = i * 1.0 / rangedVolList.size
                        fill = colorRepo.palette[0].mix(colorRepo.palette[2], mixPerc).opacify(0.5 * alphaFac)
                        stroke = null
                        rectangle(x0, loY, x1 - x0, volY - loY)
                    }
                } else if (audioMode.value == "Magnitudes") {
                    val volList = constantQ.filteredMagnitudes
                    volList.forEachIndexed { i, vol ->
                        val volY = loY.lerp(hiY, vol)

                        val x0 = (i+0.0) / volList.size * (hiX - loX) + loX
                        val x1 = (i+1.0) / volList.size * (hiX - loX) + loX

                        val mixPerc = i * 1.0 / volList.size
                        fill = colorRepo.palette[0].mix(colorRepo.palette[2], mixPerc).opacify(0.5 * alphaFac)
                        stroke = null
                        rectangle(x0, loY, x1 - x0, volY - loY)
                    }
                }

                // Draw general volume as bar
                val relVol = baseVol.map(Audio.LOWEST_SPL, Audio.HIGHEST_SPL, 0.0, 1.0)
                val volY = loY.lerp(hiY, relVol)

                fill = colorRepo.palette[1].opacify(0.2 * alphaFac)
                stroke = null
                rectangle(loX, loY, hiX - loX, volY - loY)
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
            track(TOGGLE, "f1", "Toggle this controls display")

            // Hard-coded input bindings
            keyDown {
                KEY_ESCAPE.bind("Exit Application") { audio.stop(); application.exit() }
                KEY_SPACEBAR.bind("Reset Beat Clock") { beatClock.resetTime(program.seconds) }
                "k".bind("BPM x0.5") { beatClock.animateTo(bpm = beatClock.bpm / 2.0, program.seconds, 0.1) }
                "l".bind("BPM x2.0") { beatClock.animateTo(bpm = beatClock.bpm * 2.0, program.seconds, 0.1) }
                "b".bind("Cycle Audio mode") { audioGroup.audioMode.next() }
            }
        }

        // Init UI display
        val uiDisplay = UiDisplay().apply {
            controlTextLines = inputScheme.getControlsText().split("\n")
            trackValue("BPM") { "${beatClock.bpm}" }
            trackValue("Phase") { "${beatClock.phase.round(2)}" }
            trackValue("Audio mode") { audioGroup.audioMode.value }
        }

        // Draw loop
        extend {

            // Draw visual groups
            drawer.isolatedWithTarget(rt) {
                clear(ColorRGBa.TRANSPARENT)

                audioGroup.draw()
                circleGroup.draw()
                diamondGroup.draw()
            }

            // Apply Fx and draw to screen
            fxRepo.applyChain()
            drawer.image(joinBuffer)

            // Draw controls
            if (inputScheme.isKeyActive("f1").not()) {
                uiDisplay.displayUiOn(drawer)
            }
        }

    }
}
