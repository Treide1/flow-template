package flow.rendering.scenes

import mu.KotlinLogging
import org.openrndr.draw.*
import org.openrndr.extra.compositor.Layer
import org.openrndr.extra.compositor.compose
import org.openrndr.ffmpeg.VideoPlayerFFMPEG

private val logger = KotlinLogging.logger {}

/**
 * Scenes for the [SceneNavigator].
 *
 * A scene can be drawn using [render], which returns a [ColorBuffer] that has been drawn to.
 *
 * Before the first [render] call, [start] needs to be called.
 * After the last [render] call, [finish] needs to be called.
 * Calling [render] before [start] or after [finish] **will crash**.
 *
 * @param nav The [SceneNavigator] that this scene is part of.
 * @param usedRenderTargets The number of [RenderTarget]s that this scene will use.
 */
abstract class Scene(
    val nav: SceneNavigator,
    val usedRenderTargets: Int
) {

    /**
     * A cache of [RenderTarget]s that are used by this scene.
     */
    val cache = mutableListOf<RenderTarget>()

    /**
     * Starts the scene.
     * This acquires [RenderTarget]s, which need to be released in [finish].
     */
    open fun start() {
        repeat(usedRenderTargets) { cache.add(nav.renderTargetPool.acquireAny()) }
    }

    /**
     * Renders the scene.
     * This will be called every frame.
     *
     * Note: **This will crash** if called before [start] or after [finish].
     */
    abstract fun render(drawer: Drawer): ColorBuffer

    /**
     * Finishes the scene.
     * This releases the [RenderTarget]s that were acquired in [start].
     */
    open fun finish() {
        cache.forEach { nav.renderTargetPool.release(it) }
        cache.clear()
    }

    /**
     * The name of the scene.
     */
    val name = "Scene-${nameCounter++}"

    override fun toString(): String {
        return "Scene(name='$name', usedRenderTargets=$usedRenderTargets)"
    }

    companion object {
        private var nameCounter = 0
    }
}

/**
 * A scene that initializes with a [compose] call.
 * By calling [render], the created [Layer.result] is drawn to and returned.
 */
class CompositeScene(
    nav: SceneNavigator,
    composeFunction: Layer.() -> Unit
): Scene(nav, usedRenderTargets = 0) {

    var composite = compose(composeFunction).apply { enabled = false }

    var enabled: Boolean
        get() = composite.enabled
        set(value) { composite.enabled = value }

    override fun start() {
        super.start()
        enabled = true
    }

    override fun render(drawer: Drawer): ColorBuffer {
        logger.debug { "Rendering scene=$this" }
        composite.draw(drawer)
        return composite.result
    }

    override fun finish() {
        enabled = false
        super.finish()
    }
}

/**
 * A scene that renders a [VideoPlayerFFMPEG] to a [RenderTarget].
 */
class VideoScene(
    nav: SceneNavigator,
    val createVideoPlayer: () -> VideoPlayerFFMPEG
) : Scene(nav, usedRenderTargets = 1) {

    var videoPlayer: VideoPlayerFFMPEG? = null

    val rt: RenderTarget
        get() = cache[0]

    override fun start() {
        super.start()
        videoPlayer = createVideoPlayer()
        videoPlayer!!.play()
        videoPlayer!!.ended.listen { videoPlayer!!.restart() }
    }

    override fun render(drawer: Drawer): ColorBuffer {
        drawer.isolatedWithTarget(rt) {
            videoPlayer!!.draw(drawer)
        }
        return rt.colorBuffer(0)
    }

    override fun finish() {
        videoPlayer!!.dispose()
        videoPlayer = null
        super.finish()
    }

}
