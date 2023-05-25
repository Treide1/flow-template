import flow.audio.Audio
import flow.autoupdate.AutoUpdate
import flow.bpm.BeatClock
import flow.color.ColorRepo
import flow.input.InputScheme.TrackTypes.TOGGLE
import flow.input.inputScheme
import flow.rendering.RenderPipeline
import flow.ui.UiDisplay
import org.openrndr.Fullscreen
import org.openrndr.KEY_ESCAPE
import org.openrndr.application
import org.openrndr.color.ColorRGBa

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
        val beatClock = extend(BeatClock(125.0)) // <- Play your favorite song. Set its bpm here.

        // Init AutoUpdate
        extend(AutoUpdate)

        // Init colors
        val colorRepo = ColorRepo<ColorRGBa>(
            // ...
        )

        // Init render pipeline
        val renderPipeline = RenderPipeline(width, height, drawer)

        // Init audio input
        val audio = Audio()

        // Define beat-based values
        // val _ by beatClock.bind(...)

        // Init Fx
        // ...

        // Set Fx chain
        renderPipeline.setFxChain {
            // Fx on drawBuffer ...
            drawBuffer.copyTo(imageBuffer)
            // Fx on imageBuffer ...
        }

        // Define visual groups
        // ...

        // Define controls for Input Scheme
        inputScheme.apply {
            // Tracked keys
            track(TOGGLE, "f1", "Toggle this controls display")
            // ...

            // Hard-coded input bindings
            keyDown {
                KEY_ESCAPE.bind("Exit Application") { audio.stop(); application.exit() }
                // ...
            }
        }

        // Init UI display
        val uiDisplay = UiDisplay(inputScheme).apply {
            trackValue("BPM") { "${beatClock.bpm}" }
            trackValue("FPS") { "${beatClock.fps}" }
            // ...
        }

        // Draw loop
        extend {
            renderPipeline.render(clearDrawBuffer = true, clearImageBuffer = true) {
                // Draw visual groups
                // ...
            }

            // Draw controls
            uiDisplay.alphaCap.update(beatClock.deltaSeconds, inputScheme.isKeyActive("f1").not())
            uiDisplay.displayOnDrawer(drawer)
        }

    }
}
