import flow.audio.Audio
import flow.bpm.BeatClock
import flow.bpm.envelope.Envelope
import flow.bpm.toIntervalCount
import flow.colorRepo.ColorRepo
import flow.content.VisualGroup
import flow.fx.FxRepo
import flow.input.InputScheme.TrackTypes.PIANO
import flow.input.InputScheme.TrackTypes.TOGGLE
import flow.input.inputScheme
import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.fx.blur.ApproximateGaussianBlur
import org.openrndr.extra.fx.blur.FrameBlur
import org.openrndr.extra.fx.blur.GaussianBloom
import org.openrndr.math.Vector2
import org.openrndr.math.map
import util.Capacitor
import util.displayLinesOfText
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
        val beatClock = extend(BeatClock(bpm = 125.0))

        // Init beat-based values
        val kick by beatClock.bindEnvelopeBySegments(length = 1.0) {
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
        // TODO: AUDIO
        val audio = extend(Audio()) {
            buildVolumeProcessor()

            val ranges = listOf(
                Audio.BASS,
                Audio.MID,
                Audio.TREBLE,
            )
            // buildVolumeRangeProcessor(ranges)
        }

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
                    val pos = drawer.bounds.center + Vector2(0.0, -height*relR).rotate(90 - i * 90.0)
                    fill = colorRepo.palette[0]
                    circle(pos, sizeRange.endInclusive)
                }
            }
        }

        val diamondGroup = object: VisualGroup(program) {
            val mainSize = 75.0

            val ringCount = 12
            val ringRadius = 100.0
            val ringSize = 10.0
            var ringRot = 0.0

            override fun Drawer.draw() {
                // Main diamond
                val mainColor = colorRepo.palette[0].opacify(kick*0.5 + 0.5)
                fill = mainColor
                stroke = null
                drawDiamond(width/2.0, height/2.0, mainSize)

                // If "q" is pressed, draw a ring of diamonds around the main diamond
                val ringOpacity = Capacitor(0.0, 0.8).apply {
                    onGateOpen = Envelope(1.0) { t ->
                        if (t < 0.4) (t / 0.4).pow(1/2.0) // Reaches 1.0
                        else 1.0.lerp(holdValue, (t - 0.4) / 0.6)
                    }
                    onGateClosed = Envelope (0.5) { t ->
                        holdValue.lerp(offValue, t / 0.2)
                    }
                }

                ringOpacity.update(program.seconds, inputScheme.isKeyActive("q"))

                // Draw a ring of diamonds around the main diamond
                ringRot += kick * 0.05
                val size = ringSize * if (inputScheme.isKeyActive("e")) flash else 1.0
                val center = bounds.center
                val angleStep = PI * 2 / ringCount
                for (i in 0 until ringCount) {
                    val angle = i * angleStep + ringRot
                    val x = center.x + ringRadius * cos(angle)
                    val y = center.y + ringRadius * sin(angle)
                    fill = colorRepo.palette[i%2 + 1].opacify(ringOpacity.value)
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

        // Draw loop
        extend {

            // Draw visual groups
            drawer.isolatedWithTarget(rt) {
                clear(ColorRGBa.TRANSPARENT)

                circleGroup.draw()
                diamondGroup.draw()
            }

            // Apply Fx and draw to screen
            fxRepo.applyChain()
            drawer.image(joinBuffer)

            // Draw controls
            if (inputScheme.isKeyActive("f1")) {
                drawer.displayLinesOfText(inputScheme.getControlsText().split("\n"))
            }
        }

        // Define controls for Input Scheme
        inputScheme.apply {
            // Tracked keys
            track(TOGGLE, "q", "Toggle diamond ring")
            track(TOGGLE, "e", "Toggle flash")
            track(TOGGLE, "f1", "Toggle this controls display")
            track(PIANO, "w,a,s,d".split(","), "Bouncy balls")


            // Hard-coded input bindings
            keyDown {
                KEY_ESCAPE.bind("Exit Application") { application.exit() }
                KEY_SPACEBAR.bind("Reset Beat Clock") { beatClock.resetTime(program.seconds) }
                "k".bind("BPM x0.5") { beatClock.animateTo(bpm = beatClock.bpm / 2.0, program.seconds, 0.1) }
                "l".bind("BPM x2.0") { beatClock.animateTo(bpm = beatClock.bpm * 2.0, program.seconds, 0.1) }
            }
        }

    }
}
