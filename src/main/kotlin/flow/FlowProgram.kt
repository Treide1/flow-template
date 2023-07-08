@file:Suppress("LeakingThis")

package flow

import flow.audio.Audio
import flow.autoupdate.AutoUpdate
import flow.bpm.BeatClock
import flow.envelope.LinearCapacitor
import flow.envelope.keyAutoUpdate
import flow.input.InputScheme
import flow.rendering.RenderPipeline
import flow.ui.UiDisplay
import org.openrndr.*
import org.openrndr.extra.gui.GUI

/**
 *
 * @param initialBpm
 * @param isWithGui
 */
data class FlowProgramConfig(
    val initialBpm: Double = 125.0,
    val isWithGui: Boolean = false,
)

/**
 * The [FlowProgram] is the main class of the Flow framework.
 *
 * It provides several properties to access API instances like [beatClock], [renderPipeline] and many more.
 *
 * @author Lukas Henke, 31.05.23
 */
open class FlowProgram private constructor(
    val config: FlowProgramConfig = FlowProgramConfig(),
    private val init: suspend FlowProgram.() -> Unit,
): ProgramImplementation(suspend = false) {

    /**
     * The [FlowProgram] instance. More readable than `this` in program block.
     */
    val flowProgram = this

    /**
     * [BeatClock] instance.
     */
    val beatClock = extend(BeatClock(bpm = config.initialBpm))

    /**
     * Standard OPENRNDR [GUI] instance. Only used if configured in [config].
     */
    val gui by lazy { GUI() }

    /**
     * [RenderPipeline] instance. Used to render the final image.
     */
    val renderPipeline by lazy { RenderPipeline(width, height, flowProgram) }

    /**
     * [InputScheme] instance. Uses [keyboard] by default.
     */
    val inputScheme = InputScheme(keyboard)

    /**
     * [UiDisplay] instance.
     */
    val uiDisplay = UiDisplay(inputScheme).apply {
        // Default display values.
        trackValue("BPM") { "${beatClock.bpm}" }
        trackValue("FPS") { "${beatClock.fps}" }
    }

    override suspend fun setup() {
        super.setup()

        // BEFORE INIT
        extend(AutoUpdate)

        // Define UiDisplay fade in / fade out
        uiDisplay.alphaCap = LinearCapacitor(0.1, 0.5, offValue = 1.0, holdValue = 0.0).keyAutoUpdate(flowProgram, "f1")

        // Define controls for InputScheme
        inputScheme.apply {
            // Tracked keys
            track(InputScheme.TrackTypes.TOGGLE, "f1", "Toggle this controls display")

            // Default input bindings
            keyDown {
                KEY_ESCAPE.bind("Exit Application") { flowProgram.exit() }
                KEY_SPACEBAR.bind("Reset Beat Clock") { beatClock.resetTime() }
            }
        }

        // INIT
        init()

        // AFTER INIT
        // extend gui after values have been added in init
        if (config.isWithGui) extend(gui)
    }

    private val exitListeners = mutableListOf<() -> Unit>()

    fun registerOnExit(block: () -> Unit) {
        exitListeners.add(block)
    }

    fun exit() {
        exitListeners.forEach { it() }
        application.exit()
    }

    companion object {

        /**
         * Creates and runs a synchronous [FlowProgram].
         * @param config Optional [FlowProgramConfig].
         */
        fun ApplicationBuilder.flowProgram(
            config: FlowProgramConfig = FlowProgramConfig(),
            init: suspend FlowProgram.() -> Unit
        ): FlowProgram {
            program = FlowProgram(config, init)
            return program as FlowProgram
        }
    }

}


