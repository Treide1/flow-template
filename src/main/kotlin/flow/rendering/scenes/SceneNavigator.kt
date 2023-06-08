package flow.rendering.scenes

import flow.fx.Crossfade
import flow.rendering.scenes.NavigationState.PlayingScene
import flow.rendering.scenes.NavigationState.PlayingTransition
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
import org.openrndr.extra.fx.blend.Add
import org.openrndr.extra.fx.blend.Overlay
import org.openrndr.extra.fx.color.LumaOpacity
import org.openrndr.math.map
import org.openrndr.math.smoothstep

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
    fun scene(compose: Layer.() -> Unit): Scene {
        return Scene(this, compose)
    }


    /**
     *
     */
    fun transition(function: TransitionFunction): Transition {
        return Transition(this, function)
    }

    val defaultScene = scene {
        draw {
            clearColor = defaultClearColor
        }
    }

    val join = persistent { Crossfade() }
    val luma = persistent { LumaOpacity() }

    val defaultTransition = transition {s0, s1, progress ->
        luma.apply {
            foregroundOpacity = progress
            backgroundLuma = progress.smoothstep(0.0, 1.0)
            foregroundLuma = backgroundLuma / 2.0
        }
        // luma.apply(s1, s1)

        join.blend = progress
        join.apply(s0, s1, transitionBuffer!!)
        transitionBuffer!!
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

        val _state = state as PlayingScene

        if (durationSeconds <= 0.0) {
            _state.scene.finish()
            targetScene.start()

            state = PlayingScene(targetScene)
            return
        }

        val sourceScene = _state.scene

        transition.start(sourceScene, targetScene)
        targetScene.start()

        state = PlayingTransition(
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

        // If a transition is present, update it
        val progress = when (state) {
            is PlayingScene -> null
            is PlayingTransition -> {
                val state = state as PlayingTransition
                val start = state.startTime
                val end = start + state.durationSeconds
                program.seconds.map(start, end, 0.0, 1.0)
            }
        }

        // If the transition is finished, switch to the target scene
        if (progress != null && progress >= 1.0) {
            logger.info { "Transition complete. Finishing it and source scene." }
            (state as PlayingTransition).apply {
                sourceScene.finish()
                transition.finish()
            }
            val target = (state as PlayingTransition).targetScene
            state = PlayingScene(target)
        }


        val cb = when (state) {
            is PlayingScene -> {
                // Render the scene
                val scene = (state as PlayingScene).scene
                scene.render(drawer)
            }
            is PlayingTransition -> {
                if (progress == 0.0) {
                    println("progress is 0.0, rendering source scene instead")
                    val scene = (state as PlayingTransition).sourceScene
                    scene.render(drawer)
                }
                else {
                    // Renders both scenes, then the transition
                    val transition = (state as PlayingTransition).transition
                    transition.render(drawer, progress!!)
                }
            }
        }
        println("_________________________")
        return cb
    }

}

fun logSessionStack(){
    val stack = Session.stack.map { it }
    logger.debug { "Session stack (size=${stack.size}): " }
    stack.forEachIndexed { index, s ->
        val stats = s.statistics
        logger.debug { "#$index: Session(this=$s, parent=${s.parent}, renderTargets=${stats.renderTargets}, colorBuffers=${stats.colorBuffers})" }
    }
}

inline fun <T> withSession(session: Session, block: () -> T): T {
    Session.stack.addLast(session)
    val pos = Session.stack.size - 1
    val result = block()
    Session.stack.removeAt(pos)
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