import org.openrndr.Fullscreen
import org.openrndr.application
import org.openrndr.extra.olive.oliveProgram

fun main() = application {
    configure {
        fullscreen = Fullscreen.CURRENT_DISPLAY_MODE
        display = displays.last()
    }
    oliveProgram {

        extend {

        }
    }
}