package flow.rendering.scenes

import mu.KotlinLogging
import org.openrndr.draw.*

private val logger = KotlinLogging.logger {}

/**
 *
 */
class Transition(
    val nav: SceneNavigator,
    val function: TransitionFunction
) {

    var session: Session? = null

    class TransitionResource(
        var sourceScene: Scene,
        var targetScene: Scene,
    ) {
        val transitionBuffer by lazy { sourceScene.resource!!.composite.result.createEquivalent() }
    }

    var resource: TransitionResource? = null

    val transitionBuffer: ColorBuffer?
        get() = resource?.transitionBuffer

    fun start(source: Scene, target: Scene) {
        if (session != null) {
            logger.info { "Skipping start of transition=$this" }
        }
        else {
            logger.info { "Starting transition=$this" }
            session = nav.parentSession.fork()
            logger.info { "Forked new session=$session" }
            resource = TransitionResource(sourceScene = source, targetScene = target)
        }
    }

    fun render(drawer: Drawer, progress: Double): ColorBuffer {
        return withSession(session!!) {
            logger.debug { "Rendering transition=$this" }
            logger.debug { "Transition progress=$progress" }
            val sourceScene = resource!!.sourceScene
            val targetScene = resource!!.targetScene

            val b0 = sourceScene.render(drawer)
            val b1 = targetScene.render(drawer)
            this.function(b0, b1, progress)
        }
    }

    fun finish() {
        if (session == null) {
            logger.info { "Skipping finish of transition=$this" }
        }
        else {
            logger.info { "Finishing transition=$this" }

            resource = null

            session!!.end()
            Session.stack.remove(session!!)
            session = null
        }
    }

    val name = "Transition-${nameCounter++}"

    override fun toString(): String {
        return  "Transition(name='$name', session=$session, sourceScene='${resource?.sourceScene?.name}', " +
                "targetScene='${resource?.targetScene?.name}')"
    }

    fun verboseToString(): String {
        return "Transition(name='$name', session=$session, resource=$resource, " +
                "sourceScene=${resource?.sourceScene?.verboseToString()}, " +
                "targetScene=${resource?.targetScene?.verboseToString()})"
    }

    companion object {
        private var nameCounter = 0
    }
}