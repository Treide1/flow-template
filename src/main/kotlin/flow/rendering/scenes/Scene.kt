package flow.rendering.scenes

import flow.util.Unstable
import mu.KotlinLogging
import org.openrndr.draw.*
import org.openrndr.extra.compositor.Composite
import org.openrndr.extra.compositor.Layer
import org.openrndr.extra.compositor.compose
import org.openrndr.ffmpeg.VideoPlayerFFMPEG

private val logger = KotlinLogging.logger {}

@Unstable("Prone to crash")
abstract class Scene {
    abstract fun start()
    abstract fun render(drawer: Drawer): ColorBuffer
    abstract fun finish()

    var session: Session? = null

    val name = "Scene-${nameCounter++}"

    override fun toString(): String {
        return "Scene(name='$name', session=$session)"
    }

    companion object {
        private var nameCounter = 0
    }
}

/**
 *
 */
class CompositeScene(
    val nav: SceneNavigator,
    val composeFunction: Layer.() -> Unit
): Scene() {

    var composite: Composite? = null

    override fun start() {
        if (composite != null) {
            logger.info { "Skipping start of scene=$this" }
        }
        else {
            logger.info { "Starting scene=$this" }
            session = nav.parentSession.forkAndPop()
            logger.info { "Forked new session=$session" }
            composite = withSession(session!!) { compose(composeFunction) }
        }
    }

    /**
     * This will intentionally crash if not started, or after finish !
     */
    override fun render(drawer: Drawer): ColorBuffer {
        logger.debug { "Rendering scene=$this" }
        withSession(session!!) { composite!!.draw(drawer) }
        return composite!!.result
    }

    override fun finish() {
        if (session == null) {
            logger.info { "Skipping finish of scene=$this" }
        }
        else {
            logger.info { "Finishing scene=$this" }

            composite = null

            session!!.end()
            //Session.stack.remove(session!!)
            session = null
        }
    }
}

class VideoScene(
    val nav: SceneNavigator,
    // val videoPlayer: VideoPlayerFFMPEG
    val createVideoPlayer: () -> VideoPlayerFFMPEG
) : Scene() {

    var rt: RenderTarget? = null
    var videoPlayer: VideoPlayerFFMPEG? = null

    override fun start() {
        session = nav.parentSession.forkAndPop()
        withSession(session!!) {
            videoPlayer = createVideoPlayer()
            videoPlayer!!.play()
            rt = renderTarget(videoPlayer!!.width, videoPlayer!!.height) {
                colorBuffer()
            }
        }
        videoPlayer!!.ended.listen { videoPlayer!!.restart() }
    }

    override fun render(drawer: Drawer): ColorBuffer {
        drawer.isolatedWithTarget(rt!!) {
            videoPlayer!!.draw(drawer)
        }
        return rt!!.colorBuffer(0)
    }

    override fun finish() {
        rt!!.destroy()
        rt = null

        videoPlayer!!.dispose()
        videoPlayer = null

        session!!.end()
        session = null
    }

}
