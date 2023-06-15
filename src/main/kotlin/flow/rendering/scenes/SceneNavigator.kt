@file:Suppress("unused")

package flow.rendering.scenes

import flow.fx.Crossfade
import flow.rendering.scenes.NavigationState.PlayingScene
import flow.rendering.scenes.NavigationState.PlayingTransition
import flow.util.Pool
import mu.KotlinLogging
import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.compositor.Layer
import org.openrndr.extra.compositor.draw
import org.openrndr.ffmpeg.VideoPlayerFFMPEG

/**
 * A function on a [Transition] that takes two [ColorBuffer]s and a progress value between 0.0 and 1.0.
 * The return value has to be a [ColorBuffer] that is the result of the transition.
 */
typealias TransitionFunction =
        Transition.(
            firstSceneBuffer: ColorBuffer,
            secondSceneBuffer: ColorBuffer,
            progress: Double
        ) -> ColorBuffer

private val logger = KotlinLogging.logger {}

/**
 * SceneNavigator is an [Extension] that allows you to display scenes and transitions between them.
 *
 * Starts out with the [defaultScene], only displaying the [defaultClearColor].
 *
 * Using [startTransition], a [Transition] will start to the target scene.
 * During the transition, both scenes are rendered and the transition is applied.
 *
 * After the transition ends, the resources of the previous scene and the transition itself are released.
 *
 * Resource Usage:
 * ```
 * Scene A    | ----------x
 * Scene B    |      x----------
 * Transition |      x----x
 * ```
 */
abstract class SceneNavigator(val program: Program) {

    /**
     * The default clear color of the [defaultScene].
     */
    open val defaultClearColor: ColorRGBa = ColorRGBa.TRANSPARENT


    /**
     * Creates a [CompositeScene] that will render the given [compose] function.
     */
    fun compositeScene(compose: Layer.() -> Unit): CompositeScene {
        return CompositeScene(this, compose)
    }

    /**
     * Creates a [VideoScene] that will play the video player result of [createVideoPlayer].
     * (On [VideoScene.start], a new video player is created by calling [createVideoPlayer])
     */
    fun videoScene(createVideoPlayer: () -> VideoPlayerFFMPEG): VideoScene {
        return VideoScene(this, createVideoPlayer)
    }


    /**
     * Creates a [Transition] that will apply the given [function] to the two given scenes.
     */
    fun transition(function: TransitionFunction): Transition {
        return Transition(this, function)
    }

    /**
     * The default scene that just clears the screen with [defaultClearColor].
     */
    val defaultScene = compositeScene {
        draw {
            clearColor = defaultClearColor
        }
    }

    /**
     * The pool of [RenderTarget]s used by this navigator.
     */
    val renderTargetPool = Pool(4) {
        renderTarget(program.width, program.height) {
            colorBuffer()
            depthBuffer()
        }
    }

    val join = persistent { Crossfade() }

    /**
     * The default transition. Uses [Crossfade] to blend between the two scenes.
     */
    val defaultTransition = transition {s0, s1, progress ->
        val tmp = getTmpBuffer(0)
        join.blend = progress
        join.apply(s0, s1, tmp)
        tmp
    }

    /**
     * The current state of this navigator.
     */
    var state: NavigationState = PlayingScene(defaultScene)
        private set(value) {
            field = value
            logger.info { "State changed to $value" }
        }

    init {
        (state as PlayingScene).scene.start()
    }

    /**
     * The current scene if in state [NavigationState.PlayingScene]. Otherwise: null.
     */
    val currentScene: Scene?
        get() = when (state) {
            is PlayingScene -> (state as PlayingScene).scene
            is PlayingTransition -> null
        }

    /**
     * The current transition's source scene if in state [NavigationState.PlayingTransition]. Otherwise: null.
     */
    val currentTransitionSource: Scene?
        get() = when (state) {
            is PlayingScene -> null
            is PlayingTransition -> (state as PlayingTransition).sourceScene
        }

    /**
     * The current transition's target scene if in state [NavigationState.PlayingTransition]. Otherwise: null.
     */
    val currentTransitionTarget: Scene?
        get() = when (state) {
            is PlayingScene -> null
            is PlayingTransition -> (state as PlayingTransition).targetScene
        }

    /**
     * The current transition if in state [NavigationState.PlayingTransition]. Otherwise: null.
     */
    val currentTransition: Transition?
        get() = when (state) {
            is PlayingScene -> null
            is PlayingTransition -> (state as PlayingTransition).transition
        }

    /**
     * Starts a transition to the given [targetScene] using the given [transition].
     * This will take [durationSeconds] seconds (if positive, or happens instantly otherwise).
     */
    fun startTransition(
        transition: Transition,
        targetScene: Scene,
        durationSeconds: Double
    ) {
        if (state is PlayingTransition) {
            logger.info { "Already playing a transition. Ignoring startTransition." }
            return
        }

        val state = state as PlayingScene

        if (durationSeconds <= 0.0) {
            logger.info { "Duration=$durationSeconds is 0.0 or negative. Finishing source scene and starting target scene." }
            state.scene.finish()
            targetScene.start()

            this.state = PlayingScene(targetScene)
            return
        }

        logger.info { "Starting transition=$transition" }
        val sourceScene = state.scene
        targetScene.start()
        transition.start()

        this.state = PlayingTransition(
            sourceScene = sourceScene,
            targetScene = targetScene,
            transition = transition,
            startTime = program.seconds,
            durationSeconds = durationSeconds
        )
    }

    /**
     * Updates the [state] and renders the current scene or transition.
     * Returns the [ColorBuffer] that was rendered to.
     */
    fun render(drawer: Drawer): ColorBuffer {
        var state = state // Immutable copy to avoid concurrent modification

        // If the transition is finished, switch to the target scene
        if (state is PlayingTransition && program.seconds >= state.startTime + state.durationSeconds) {
            logger.info { "Transition complete. Finishing it and source scene." }

            val ss = state.sourceScene
            logger.debug { "Finishing sourceScene=$ss" }
            ss.finish()

            val t = state.transition
            logger.debug { "Finishing transition=$t" }
            t.finish()
            this.state = PlayingScene(state.targetScene)
        }

        // Update the state reference
        state = this.state

        val cb = when (state) {
            is PlayingScene -> {
                // Render the scene
                state.scene.render(drawer)
            }
            is PlayingTransition -> {
                // Calculate the progress of the transition
                val progress = (program.seconds - state.startTime) / state.durationSeconds
                // Renders both scenes, then the transition
                val b0 = state.sourceScene.render(drawer)
                val b1 = state.targetScene.render(drawer)
                state.transition.render(b0, b1, progress)
            }
        }
        return cb
    }

}

/**
 * Enumerates the possible states of a [SceneNavigator].
 */
sealed class NavigationState {

    /**
     * The scene is currently playing. It only has a single [scene].
     */
    data class PlayingScene(val scene: Scene) : NavigationState()

    /**
     * A transition is currently playing. It has a [sourceScene], a [targetScene] and a [transition].
     * The transition has started at [startTime] and will take [durationSeconds] seconds.
     */
    data class PlayingTransition(
        val sourceScene: Scene,
        val targetScene: Scene,
        val transition: Transition,
        val startTime: Double,
        val durationSeconds: Double,
    ) : NavigationState()
}