import flow.audio.Audio
import flow.bpm.BeatClock
import flow.colorRepo.ColorRepo
import flow.content.VisualGroup
import flow.fx.FxRepo
import flow.input.InputScheme.TrackTypes.PIANO
import flow.input.InputScheme.TrackTypes.TOGGLE
import flow.input.inputScheme
import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.color.ColorXSVa
import org.openrndr.draw.*
import org.openrndr.extra.fx.blur.ApproximateGaussianBlur
import org.openrndr.extra.fx.blur.FrameBlur
import org.openrndr.extra.fx.blur.GaussianBloom
import org.openrndr.math.Vector2
import util.toIntervalCount
import kotlin.math.pow

/**
 * Template for "Flow"
 */
fun main() = application {
    configure {
        fullscreen = Fullscreen.CURRENT_DISPLAY_MODE
        display = displays.last()
    }
    program {

        // Input Scheme (1st half)
        val inputScheme = inputScheme {
            // Tracked keys
            keyboard.track(TOGGLE, "qe".split(""))
            keyboard.track(PIANO, "wasd".split(""))
        }

        // Init beatClock
        val beatClock = extend(BeatClock(bpm = 120.0))

        // Init beat-based values
        val kick by beatClock.bindEnvelope { phase ->
            val relPhase = phase % 1.0
            4.0.pow(-relPhase)
        }
        val flash by beatClock.bindEnvelope { phase ->
            val interval = 1.0 / 4.0
            val count = phase.toIntervalCount(interval)
            if (count % 2 == 0) 1.0 else 0.0
        }

        // Init audio input
        val audio = extend(Audio())

        // TODO: add volume samples for ranges
        audio.ranges = listOf(
            20.0 .. 200.0,
            500.0 .. 2500.0,
            Audio.TREBLE,
        )

        // Init colors
        val colorRepo = ColorRepo {
            palette = listOf(
                ColorXSVa(000.0, 1.0, 0.8),
                ColorXSVa(060.0, 0.8, 1.0),
                ColorXSVa(120.0, 0.2, 0.1, 0.8)
            )
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
        val circleGroup = object: VisualGroup() {
            override fun draw(drawer: Drawer, program: Program) {
                drawer.fill = colorRepo.palette[0].toRGBa().opacify(flash)
                drawer.stroke = null
                listOf(
                    Vector2(0.2, 0.2),
                    Vector2(0.2, 0.8),
                    Vector2(0.8, 0.2),
                    Vector2(0.8, 0.8)
                ).forEach { (x, y) ->
                    drawer.circle(x * width, y * height, kick * 50.0 + 100.0)
                }
            }
        }

        val diamondGroup = object: VisualGroup() {
            override fun draw(drawer: Drawer, program: Program) {
                drawer.isolated {
                    fill = colorRepo.palette[1].toRGBa().opacify(kick*0.5 + 0.5)
                    stroke = null
                    translate(width/2.0, height/2.0)
                    rotate(45.0)
                    rectangle(-100.0, -100.0, 200.0, 200.0)
                }
            }
        }

        // Draw loop
        extend {
            drawer.isolatedWithTarget(rt) {
                clear(ColorRGBa.TRANSPARENT)

                circleGroup.draw(drawer, program)
                diamondGroup.draw(drawer, program)
            }
            fxRepo.applyChain()
            drawer.image(joinBuffer)
        }

        // Input Scheme (2nd half)
        inputScheme.apply {
            // Hard-coded input bindings
            keyboardKeyDown {
                KEY_ESCAPE.bind { application.exit() }
                KEY_SPACEBAR.bind { beatClock.resetTime(program.seconds) }
                "k".bind { beatClock.animateTo(bpm = 132.0, 1.0) }
            }
        }

    }
}
