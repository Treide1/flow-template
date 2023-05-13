package flow.fx

import flow.rendering.RenderPipeline
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Filter
import org.openrndr.extra.fx.blend.Overlay
import org.openrndr.extra.fx.blur.ApproximateGaussianBlur
import org.openrndr.extra.fx.blur.GaussianBloom
import org.openrndr.extra.fx.distort.Perturb

/**
 * FxRepo is a repository for all the effects that are applied to the visual content.
 */
class FxRepo(renderPipeline: RenderPipeline) {

    // TODO: more fx
    val overlay by lazy { Overlay() }
    val blur by lazy { ApproximateGaussianBlur() }
    val bloom by lazy { GaussianBloom() }
    val mirrorFx by lazy { MirrorFilter(renderPipeline.stencilBuffer) }
    val perturb by lazy { Perturb() }
}