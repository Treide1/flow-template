import flow.audio.Audio
import flow.bpm.BeatClock
import flow.bpm.envelope.Capacitor
import flow.bpm.envelope.Envelope
import flow.bpm.toIntervalCount
import flow.color.ColorRepo
import flow.color.ColorRepo.ColorRoles.*
import flow.content.VisualGroup
import flow.fx.MirrorFilter
import flow.fx.toR
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
import org.openrndr.extra.fx.blur.FrameBlur
import org.openrndr.extra.fx.blur.GaussianBloom
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
        val beatClock = extend(BeatClock(125.0)) // <- Play your favorite song. Set its bpm here.

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
            segmentJoin(0.15, 0.8)
            segmentJoin(0.2, 1.0)
            segmentJoin(1.0, 0.0) via { x: Double -> x.pow(3.0) }
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
        val frameBlur = FrameBlur()

        // Set Fx chain
        renderPipeline.apply {
            fxRepo.setChain {
                blur.apply(drawBuffer)
                bloom.apply(drawBuffer)
                mirrorFx.apply(drawBuffer)
                overlay(drawBuffer, joinBuffer)
                frameBlur.apply(joinBuffer)
            }
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
                    var color = colorRepo[colorIndex]
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
                    fill = colorRepo[PRIMARY]
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

            // If active, draw a center diamond and/or a ring of diamonds around it.
            override fun Drawer.draw() {
                isShowingMain = inputScheme.isKeyActive("c").not()
                isShowingRing = inputScheme.isKeyActive("q")

                // Center diamond
                if (isShowingMain) {
                    val mainColor = colorRepo[PRIMARY].opacify(kick * 0.5 + 0.5)
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
                    fill = colorRepo[i%2 + 1].opacify(ringOpacity)
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

            // Opacity saturate on (towards 1.0) and off (towards 0.0)
            val capacitor = Capacitor().apply {
                onGateOpen = Envelope(0.5) {
                    it / 0.5
                }
                onGateClosed = Envelope(0.5) {
                    1.0 - it / 0.5
                }
            }
            val alphaFac by capacitor

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
                capacitor.update(0.016, inputScheme.isKeyActive("v"))

                // Draw volume bars
                if (audioMode.value == "Bands") {
                    val bandedVolList = constantQ.filteredBandedVolumes
                    bandedVolList.forEachIndexed { i, vol ->
                        val freqBand = constantQ.freqBands[i]

                        val x0 = log2(freqBand.start)
                            .map(log2(Audio.LOWEST_FQ), log2(Audio.HIGHEST_FQ), loX, hiX)
                        val x1 = log2(freqBand.endInclusive)
                            .map(log2(Audio.LOWEST_FQ), log2(Audio.HIGHEST_FQ), loX, hiX)
                        val mixPerc = i.toDouble() / bandedVolList.size

                        fill = colorRepo[PRIMARY].mix(colorRepo[TERTIARY], mixPerc).opacify(0.5 * alphaFac)
                        stroke = null
                        drawVolBar(x0, x1, vol)
                    }
                } else if (audioMode.value == "Magnitudes") {
                    val volList = constantQ.filteredMagnitudes
                    volList.forEachIndexed { i, vol ->
                        val x0 = (i+0.1) / volList.size * (hiX - loX) + loX
                        val x1 = (i+0.9) / volList.size * (hiX - loX) + loX
                        val mixPerc = i * 1.0 / volList.size

                        fill = colorRepo[PRIMARY].mix(colorRepo[TERTIARY], mixPerc).opacify(0.5 * alphaFac)
                        stroke = null
                        drawVolBar(x0, x1, vol)
                    }
                }

                // Draw general volume as bar
                val baseVol = volProcessor.filteredLastVolume

                fill = colorRepo[SECONDARY].opacify(0.2 * alphaFac)
                stroke = null
                drawVolBar(loX, hiX, baseVol)
            }

            /**
             * Draws a volume bar from [x0] to [x1], with [volume] being the relative height in unit range.
             */
            fun Drawer.drawVolBar(x0: Double, x1: Double, volume: Double) {
                val volY = loY.lerp(hiY, volume)
                rectangle(x0, loY, x1 - x0, volY - loY)
            }
        }

        val mirrorGroup = object: VisualGroup(program) {

            // Main circle that grows if activated, and shrinks if deactivated.
            val maxR = Vector2(0.3 * width, 0.3 * height).length
            val capacitor = Capacitor().apply {
                onGateOpen = Envelope(0.5) { it / 0.5 }
                onGateClosed = Envelope(0.5) { 1.0 - it / 0.5 }
            }
            val fac by capacitor

            val triangleR = 20.0
            val triangleVertices = List(3) { Vector2(triangleR, 0.0).rotate(90.0 + it*120.0) }

            var rotateAndScale_angle by mirrorFx.parameters
            var angle = 0.05
                set(value) {
                    field = value
                    rotateAndScale_angle = value
                }
            var maxAngleOff = 0.001

            fun triangleContour(center: Vector2, angleOff: Double) = contour {
                repeat(3) {
                    moveOrLineTo(center + triangleVertices[it].rotate(angleOff))
                }
                close()
            }

            override fun Drawer.draw() {
                capacitor.update(0.016, inputScheme.isKeyActive("m"))
                angle += maxAngleOff

                mirrorFx._fadeExp = flash

                // Draw triangle
                fill = colorRepo[PRIMARY].opacify(0.5 * fac)

                val baseCenter = bounds.center - Vector2(maxR + 20.0, 0.0) //Vector2(25.0, height/2.0) // bounds.center - Vector2(maxR, 0.0)
                val off = Vector2(15.0, 0.0).rotate(sin(ebbAndFlow * TWO_PI)*90.0 + 180.0)
                val center0 = baseCenter + off
                val center1 = Vector2(width - center0.x, center0.y)
                contour(triangleContour(center0, angle))
                contour(triangleContour(center1, -angle))

                drawer.isolatedWithTarget(renderPipeline.stencilTarget) {
                    clear(0.toR())

                    fill = 4.toR()
                    stroke = null
                    circle(width / 2.0, height / 2.0, maxR * fac)
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
                "k".bind("BPM x0.5") { beatClock.animateTo(bpm = beatClock.bpm / 2.0, program.seconds, 0.1) }
                "l".bind("BPM x2.0") { beatClock.animateTo(bpm = beatClock.bpm * 2.0, program.seconds, 0.1) }
                "b".bind("Cycle Audio mode") { audioGroup.audioMode.next() }
                "f".bind("Toggle fade") { mirrorFx._fade = !mirrorFx._fade }
            }
        }

        // Init UI display
        val uiDisplay = UiDisplay(inputScheme).apply {
            trackValue("BPM") { "${beatClock.bpm}" }
            trackValue("Phase") { "${beatClock.phase.round(2)}" }
            trackValue("Audio mode") { audioGroup.audioMode.value }
            trackValue("Fade") { "${mirrorFx._fade}" }
        }

        // Draw loop
        extend {
            renderPipeline.render(clearTarget = true) {
                // Draw visual groups
                audioGroup.draw()
                circleGroup.draw()
                diamondGroup.draw()
                mirrorGroup.draw()
            }

            // Draw controls
            if (inputScheme.isKeyActive("f1").not()) {
                uiDisplay.displayUiOn(drawer)
            }
        }

    }
}
