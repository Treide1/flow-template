package flow.rendering.scenes

import flow.util.Unstable
import mu.KotlinLogging
import org.openrndr.draw.*

private val logger = KotlinLogging.logger {}

/**
 *
 */
@Unstable("Prone to crash")
open class Transition(
    val nav: SceneNavigator,
    val function: TransitionFunction
) {

    var session: Session? = null

    class BufferCache(val session: Session, val width: Int, val height: Int) {
        val buffers = mutableMapOf<Int, ColorBuffer>()

        operator fun get(index: Int): ColorBuffer {
            return buffers.getOrPut(index) {
                colorBuffer(width, height, session = session)
            }
        }
    }

    var bufferCache = BufferCache(session ?: Session.active, nav.program.width, nav.program.height)

    fun start() {
        if (session != null) {
            logger.info { "Skipping start of transition=$this" }
        }
        else {
            logger.info { "Starting transition=$this" }
            session = nav.parentSession.forkAndPop()
            logger.info { "Forked new session=$session" }
            bufferCache = BufferCache(session!!, nav.program.width, nav.program.height)
            logger.info { "Started transition=$this" }
        }
    }

    fun render(b0: ColorBuffer, b1: ColorBuffer, progress: Double): ColorBuffer {
        return withSession(session!!) {
            this.function(b0, b1, progress)
        }
    }

    fun finish() {
        if (session == null) {
            logger.info { "Skipping finish of transition=$this" }
        }
        else {
            logger.info { "Finishing transition=$this" }
            session!!.end()

            logger.info { "Destroyed and removed session=$session from stack" }
            session = null
        }
    }

    val name = "Transition-${nameCounter++}"

    override fun toString(): String {
        return  "Transition(name='$name', session=$session)"
    }

    companion object {
        private var nameCounter = 0
    }
}