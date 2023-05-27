import flow.FlowProgram.Companion.flowProgram
import flow.color.ColorRepo
import flow.ui.UiDisplay
import org.openrndr.Fullscreen
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
    flowProgram {
        // Init colors
        val colorRepo = ColorRepo<ColorRGBa>(
            // ...
        )

        // Define beat-based values
        // val _ by beatClock.bind(...)

        // Init Fx
        // ...

        // Define visual groups
        // ...

        // Define controls for Input Scheme
        inputScheme.apply {
            // Tracked keys
            // ...

            // Hard-coded input bindings
            keyDown {
                // ...
            }
        }

        // Init UI display
        val uiDisplay = UiDisplay(inputScheme).apply {
            // ...
        }

        // Draw loop
        extend {
            renderPipeline.render {
                clear()

                // Draw visual groups
                // ...

                // Use fx
                // ...

                // Eventually draw to image buffer
                drawBuffer.copyTo(imageBuffer)
            }

            // Draw controls
            uiDisplay.displayOnDrawer(drawer)
        }

    }
}
