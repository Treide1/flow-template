package flow.rendering.scenes

import flow.fx.Crossfade
import flow.rendering.scenes.NavigationState.PlayingScene
import flow.rendering.scenes.NavigationState.PlayingTransition
import flow.util.Unstable
import mu.KotlinLogging
import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Drawer
import org.openrndr.draw.Session
import org.openrndr.draw.persistent
import org.openrndr.extra.compositor.Layer
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.fx.color.LumaOpacity
import org.openrndr.ffmpeg.VideoPlayerFFMPEG
import org.openrndr.math.smoothstep

/**
 * A function on a [Transition] is a function that takes two [ColorBuffer]s and a progress value between 0.0 and 1.0.
 * The return value has to be a [ColorBuffer] that is the result of the transition.
 */
typealias TransitionFunction =
        Transition.(firstSceneBuffer: ColorBuffer, secondSceneBuffer: ColorBuffer, progress: Double) -> ColorBuffer

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
@Unstable("Prone to crash")
abstract class SceneNavigator(val program: Program) {

    /**
     *
     */
    val parentSession = Session.active

    /**
     *
     */
    open val defaultClearColor: ColorRGBa = ColorRGBa.TRANSPARENT


    /**
     *
     */
    fun compositeScene(compose: Layer.() -> Unit): CompositeScene {
        return CompositeScene(this, compose)
    }

    /**
     * Creates a [VideoScene] that will play the given [videoPlayer].
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

    val defaultScene = compositeScene {
        draw {
            clearColor = defaultClearColor
        }
    }

    val join = persistent { Crossfade() }
    val luma = persistent { LumaOpacity() }

    val defaultTransition = transition {s0, s1, progress ->
        val tmp = bufferCache[0]
        luma.apply {
            foregroundOpacity = progress
            backgroundLuma = progress.smoothstep(0.0, 1.0)
            foregroundLuma = backgroundLuma / 2.0
        }
        // luma.apply(s1, s1)

        join.blend = progress
        join.apply(s0, s1, tmp)
        tmp
    }

    private var state: NavigationState = PlayingScene(defaultScene)

    val currentScene: Scene?
        get() = when (state) {
            is PlayingScene -> (state as PlayingScene).scene
            is PlayingTransition -> null
        }

    val currentTransitionSource: Scene?
        get() = when (state) {
            is PlayingScene -> null
            is PlayingTransition -> (state as PlayingTransition).sourceScene
        }

    val currentTransitionTarget: Scene?
        get() = when (state) {
            is PlayingScene -> null
            is PlayingTransition -> (state as PlayingTransition).targetScene
        }

    val currentTransition: Transition?
        get() = when (state) {
            is PlayingScene -> null
            is PlayingTransition -> (state as PlayingTransition).transition
        }

    init {
        (state as PlayingScene).scene.start()
    }

    fun startTransition(
        transition: Transition,
        targetScene: Scene,
        durationSeconds: Double
    ) {
        if (state is PlayingTransition) return

        val state = state as PlayingScene

        if (durationSeconds <= 0.0) {
            state.scene.finish()
            targetScene.start()

            this.state = PlayingScene(targetScene)
            return
        }

        val sourceScene = state.scene

        transition.start()
        targetScene.start()
        // transition.start()

        this.state = PlayingTransition(
            sourceScene = sourceScene,
            targetScene = targetScene,
            transition = transition,
            startTime = program.seconds,
            durationSeconds = durationSeconds
        )
    }

    /**
     *
     */
    fun render(drawer: Drawer): ColorBuffer {
        logSessionStack()

        var state = state // Immutable copy to avoid concurrent modification

        // If the transition is finished, switch to the target scene
        if (state is PlayingTransition && program.seconds >= state.startTime + state.durationSeconds) {
            logger.info { "Transition complete. Finishing it and source scene." }

            val ss = state.sourceScene
            logger.debug { "Finishing sourceScene=$ss with session index=${Debugger.indexOf(ss.session!!)}" }
            state.sourceScene.finish()

            val t = state.transition
            logger.debug { "Finishing transition=$t with session index ${Debugger.indexOf(t.session!!)}" }
            state.transition.finish()
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
                val progress = if (state.durationSeconds > 0.0) {
                    (program.seconds - state.startTime) / state.durationSeconds
                } else {
                    1.0
                }
                // Renders both scenes, then the transition
                val b0 = state.sourceScene.render(drawer)
                val b1 = state.targetScene.render(drawer)
                state.transition.render(b0, b1, progress)
            }
        }
        println("_________________________")
        return cb
    }

}

object Debugger {
    var sessionCounter = 0
    val sessionTracker = mutableMapOf<Session, Int>()

    fun indexOf(session: Session): Int {
        return sessionTracker.getOrPut(session) {
            sessionCounter++
        }
    }
}

fun Session.forkAndPop(): Session {
    val s = this.fork()
    Session.stack.removeLast()
    return s
}


fun logSessionStack() {
    val stack = Session.stack.map { it }
    logger.debug { "Session stack (size=${stack.size}): " }
    stack.forEachIndexed { index, s ->
        val stats = s.statistics
        logger.debug { "#${Debugger.indexOf(s)}: Session(this=$s, parent=${s.parent}, renderTargets=${stats.renderTargets}, colorBuffers=${stats.colorBuffers})" }
    }
}

inline fun <T> withSession(session: Session, block: () -> T): T {
    Session.stack.addLast(session)
    // val pos = Session.stack.size - 1
    val result = block()
    Session.stack.remove(session) // removeAt(pos)
    return result
}

/**
 *
 */
sealed class NavigationState {

    /**
     *
     */
    data class PlayingScene(val scene: Scene) : NavigationState()

    /**
     *
     */
    data class PlayingTransition(
        val sourceScene: Scene,
        val targetScene: Scene,
        val transition: Transition,
        val startTime: Double,
        val durationSeconds: Double,
    ) : NavigationState()
}