package flow.rendering.scenes

import mu.KotlinLogging
import org.openrndr.draw.*

private val logger = KotlinLogging.logger {}

/**
 * Transitions for the [SceneNavigator].
 *
 * A transition can be drawn using [render], which takes two [ColorBuffer]s
 * from two scenes and a progress how far to blend between them.
 *
 * Before the first [render] call, [start] needs to be called.
 * After the last [render] call, [finish] needs to be called.
 */
open class Transition(
    val nav: SceneNavigator,
    val function: TransitionFunction
) {

    val renderTargets = mutableMapOf<Int, RenderTarget>()

    fun getTmpBuffer(index: Int) = renderTargets.getOrPut(index) {
        nav.renderTargetPool.acquireAny()
    }.colorBuffer(0)

    fun start() {
        // pass
    }

    fun render(b0: ColorBuffer, b1: ColorBuffer, progress: Double): ColorBuffer {
        return function(this, b0, b1, progress)
    }

    fun finish() {
        renderTargets.values.forEach { nav.renderTargetPool.release(it) }
        renderTargets.clear()
    }

    val name = "Transition-${nameCounter++}"

    override fun toString(): String {
        return  "Transition(name='$name', renderTargets=$renderTargets)"
    }

    companion object {
        private var nameCounter = 0
    }
}