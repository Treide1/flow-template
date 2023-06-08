package flow.rendering.scenes

import mu.KotlinLogging
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Drawer
import org.openrndr.draw.Session
import org.openrndr.extra.compositor.Composite
import org.openrndr.extra.compositor.Layer
import org.openrndr.extra.compositor.compose

private val logger = KotlinLogging.logger {}

/**
 *
 */
class Scene(
    val nav: SceneNavigator,
    val composeFunction: Layer.() -> Unit
) {
    var session: Session? = null

    data class SceneResource(
        val composite: Composite
    )

    var resource: SceneResource? = null

    fun start() {
        if (resource != null) {
            logger.info { "Skipping start of scene=$this" }
        }
        else {
            logger.info { "Starting scene=$this" }
            session = nav.parentSession.fork()
            logger.info { "Forked new session=$session" }
            resource = SceneResource(composite = compose(composeFunction))
        }
    }

    /**
     * This will intentionally crash if not started, or after finish !
     */
    fun render(drawer: Drawer): ColorBuffer {
        return withSession(session!!) {
            val composite = resource!!.composite

            logger.debug { "Rendering scene=$this" }
            composite.draw(drawer)
            composite.result
        }
    }

    fun finish() {
        if (session == null) {
            logger.info { "Skipping finish of scene=$this" }
        }
        else {
            logger.info { "Finishing scene=$this" }

            resource = null

            session!!.end()
            Session.stack.remove(session!!)
            session = null
        }
    }

    val name = "Scene-${nameCounter++}"

    override fun toString(): String {
        return "Scene(name='$name', session=$session)"
    }

    fun verboseToString(): String {
        return "Scene(name='$name', session=$session, resource=$resource)"
    }

    companion object {
        private var nameCounter = 0
    }

}
