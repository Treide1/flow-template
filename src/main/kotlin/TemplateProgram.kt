import flow.audio.Audio
import flow.bpm.BeatClock
import flow.color.colorRepo
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

        // Init colors
        val colorRepo = colorRepo<ColorRGBa> {
            // ...
        }

        // Init render pipeline
        val renderPipeline = RenderPipeline(width, height, drawer)

        // Init audio input
        val audio = Audio()

        // Define beat-based values
        // val _ by beatClock.bind(...)

        // Init Fx
        // ...

        // Set Fx chain
        renderPipeline.apply {
            fxRepo.setChain {
                // Fx on drawBuffer
                overlay(drawBuffer, joinBuffer)
                // Fx on joinBuffer
            }
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
        val uiDisplay = UiDisplay().apply {
            controlTextLines = inputScheme.getControlsText().split("\n")
            trackValue("BPM") { "${beatClock.bpm}" }
            // ...
        }

        // Draw loop
        extend {
            renderPipeline.render(clearTarget = true) {
                // Draw visual groups
                // ...
            }

            // Draw controls
            if (inputScheme.isKeyActive("f1").not()) {
                uiDisplay.displayUiOn(drawer)
            }
        }

    }
}
