import flow.FlowProgram.Companion.flowProgram
import flow.color.ColorRepo
import flow.fx.fluidSim.FluidSimulation
import flow.fx.retroSun.RetroSun
import flow.shadertoy.ProjectRenderer
import flow.util.CyclicFlag
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.tint

/**
 * Template for "Flow" programs.
 */
fun main() = application {
    configure {
        //fullscreen = Fullscreen.CURRENT_DISPLAY_MODE
        width = 1280
        height = 720
        display = displays.last()
        title = "OPENRNDR ~ Shadertoy Parser"
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

        ////////////////////////////

        val fluidSim = FluidSimulation(this)
        val retroSun = RetroSun(this)

        val shader = CyclicFlag(retroSun, fluidSim)

        var active: ProjectRenderer = shader.value
        var from: ProjectRenderer? = null
        var transition: Double? = null

        fun queueTransition() {
            from = active
            shader.next()
            active = shader. value
            transition = 1.0
        }

        // Define controls for Input Scheme
        inputScheme.apply {
            // Tracked keys

            // Hard-coded input bindings
            keyDown {
                "s".bind("shader") { queueTransition() }
            }
        }

        // Init UI display
        uiDisplay.apply {
            trackValue("frameCount") { "$frameCount" }

            // Shadertoy combiner stats
            trackValue("iResolution") { "${fluidSim.iResolution}" }
            trackValue("iFrame") { "${fluidSim.iFrame}" }
            trackValue("iTime") { "${fluidSim.iTime}" }
            trackValue("iTimeDelta") { "${fluidSim.iTimeDelta}" }
            trackValue("iMouse") {
                fluidSim.iMouse.run {
                    "x: ${x.toInt()}, " +
                    "y: ${y.toInt()}, " +
                    "z: ${z.toInt()}, " +
                    "w: ${w.toInt()}"
                }
            }
        }

        // Draw loop
        extend {

            if (transition == null) {
                drawer.image(active.render())
            }
            else {
                transition = transition!! * 0.99

                val t = transition!!

                drawer.drawStyle.colorMatrix = tint(ColorRGBa.WHITE.opacify(t))
                drawer.image(from!!.render())

                drawer.drawStyle.colorMatrix = tint(ColorRGBa.WHITE.opacify(1-t))
                drawer.image(active.render())

                if (transition!! <= 0.01) {
                    transition = null
                    from = null
                }
            }

            // Draw controls
            uiDisplay.displayOnDrawer(drawer)
        }
    }
}
