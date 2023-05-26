@file:Suppress("LeakingThis")

package flow

import flow.audio.Audio
import flow.autoupdate.AutoUpdate
import flow.autoupdate.AutoUpdate.autoUpdate
import flow.bpm.BeatClock
import flow.envelope.LinearCapacitor
import flow.input.InputScheme
import flow.rendering.RenderPipeline
import flow.ui.UiDisplay
import org.openrndr.*
import org.openrndr.extra.gui.GUI

/**
 *
 * @param initialBpm
 * @param isWithGui
 * @param audio
 */
data class FlowProgramConfig(
    val initialBpm: Double = 125.0,
    val isWithGui: Boolean = false,
    val audio: Audio? = null,
)

/**
 *
 */
open class FlowProgram private constructor(
    private val config: FlowProgramConfig = FlowProgramConfig(),
): ProgramImplementation(suspend = false) {

    /**
     *
     */
    val beatClock = extend(BeatClock(bpm= config.initialBpm))

    /**
     *
     */
    val gui by lazy { GUI() }

    /**
     *
     */
    val renderPipeline by lazy { RenderPipeline(width, height, drawer, if (config.isWithGui) gui else null) }

    // TODO: provide access to attachments like this:
    /*{
        attachDrawer("main")
        attachDrawer("blend")
        attachDrawer("tmp")
        attachDrawer("image", out = true)
        attachStencil("stencil")
    }*/

    /**
     *
     */
    val audio by lazy { config.audio ?: Audio() }

    /**
     *
     */
    val inputScheme = InputScheme(keyboard)

    /**
     *
     */
    val uiDisplay = UiDisplay(inputScheme).apply {
        trackValue("BPM") { "${beatClock.bpm}" }
        trackValue("FPS") { "${beatClock.fps}" }
    }

    /**
     *
     */
    internal fun beforeInit() {
        extend(AutoUpdate)

        uiDisplay.alphaCap = LinearCapacitor(0.1, 0.5).autoUpdate {
            update(beatClock.deltaSeconds, inputScheme.isKeyActive("f1").not())
        }

        // Define controls for Input Scheme
        inputScheme.apply {
            // Tracked keys
            track(InputScheme.TrackTypes.TOGGLE, "f1", "Toggle this controls display")

            // Hard-coded input bindings
            keyDown {
                KEY_ESCAPE.bind("Exit Application") { audio.stop(); application.exit() }
                KEY_SPACEBAR.bind("Reset Beat Clock") { beatClock.resetTime() }
            }
        }
    }

    /**
     *
     */
    fun afterInit() {
        if (config.isWithGui) extend(gui)
    }

    companion object {

        /**
         *
         */
        fun ApplicationBuilder.flowProgram(
            config: FlowProgramConfig = FlowProgramConfig(),
            init: suspend FlowProgram.() -> Unit
        ): FlowProgram {

            program = object : FlowProgram(config) {
                override suspend fun setup() {
                    super.setup()
                    beforeInit()
                    init()
                    afterInit()
                }
            }
            return program as FlowProgram
        }
    }

}


