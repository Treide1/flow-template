@file:Suppress("unused")

package flow.rendering

import flow.FlowProgram
import flow.fx.MedianDenoisingFilter
import flow.fx.MirrorFilter
import flow.fx.SquircleBlend
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.fx.blend.Overlay
import org.openrndr.extra.fx.blend.SourceAtop
import org.openrndr.extra.fx.blur.ApproximateGaussianBlur
import org.openrndr.extra.fx.blur.GaussianBloom
import org.openrndr.extra.fx.color.ChromaticAberration
import org.openrndr.extra.fx.color.LumaOpacity
import org.openrndr.extra.fx.distort.Perturb
import org.openrndr.extra.fx.distort.VerticalWave
import org.openrndr.extra.gui.addTo

class RenderPipeline(
    width: Int,
    height: Int,
    private val flowProgram: FlowProgram
) {

    /**
     * The target onto which the [render] function draws.
     */
    val drawTarget by lazy { renderTarget(width, height) {
        colorBuffer(ColorFormat.RGBa, ColorType.FLOAT32)
        depthBuffer() // Shapes and contours require a depth buffer. Unused besides that.
    } }

    /**
     * The buffer of the [drawTarget].
     */
    val drawBuffer by lazy { drawTarget.colorBuffer(0) }

    /**
     * A color buffer for temporary draw or fx results.
     */
    val tmpBuffer by lazy { drawBuffer.createEquivalent() }

    /**
     * Final render buffer that is displayed on screen.
     */
    val imageBuffer by lazy { drawBuffer.createEquivalent() }

    /**
     * Stencil target for effects. (Currently only MirrorFx)
     */
    val stencilTarget by lazy { renderTarget(width, height) {
        colorBuffer(ColorFormat.R, ColorType.UINT8)
    } }

    /**
     * The buffer of [stencilTarget].
     */
    val stencilBuffer by lazy { stencilTarget.colorBuffer(0) }

    /**
     *
     */
    fun render(block: RenderPipeline.() -> Unit = {}) {

        // Draw onto drawTarget
        flowProgram.drawer.isolatedWithTarget(drawTarget) {
            block()
        }

        flowProgram. drawer.image(imageBuffer)
    }

    /**
     * Clears the [ColorBuffer] with the given [color].
     */
    fun ColorBuffer.clear(color: ColorRGBa = ColorRGBa.TRANSPARENT) {
        this.fill(color)
    }

    // Add filter to gui if the gui is configured
    private fun <T: Filter> T.addToGui(): T {
        if (flowProgram.config.isWithGui) this.addTo(flowProgram.gui)
        return this
    }

    val overlay by lazy { Overlay().addToGui() }
    val sourceAtop by lazy { SourceAtop().addToGui()}
    val lumaOpacity by lazy { LumaOpacity().addToGui() }
    val blur by lazy { ApproximateGaussianBlur().addToGui() }
    val bloom by lazy { GaussianBloom().apply { window = 1 }.addToGui() }
    val mirrorFx by lazy { MirrorFilter(stencilBuffer).addToGui() }
    val perturb by lazy { Perturb().addToGui() }
    val chromaticAberration by lazy { ChromaticAberration().addToGui() }
    val verticalWave by lazy { VerticalWave().apply { segments = 1; phase = 0.5 }.addToGui() }
    val squircleBlend by lazy { SquircleBlend().addToGui() }
    val medianDenoise by lazy { MedianDenoisingFilter().addToGui() }

    /**
     * Shortcut for [Filter.apply] with (buffer, buffer).
     * Allows to use an inbetween [ColorBuffer] if the [useCopyBuffer] flag is set.
     */
    fun Filter.apply(buffer: ColorBuffer, useCopyBuffer: ColorBuffer? = null) {
        if (useCopyBuffer != null) {
            this.apply(buffer, useCopyBuffer)
            useCopyBuffer.copyTo(buffer)
        }
        else {
            this.apply(buffer, buffer)
        }
    }
}

/**
 * Shortcut for [Drawer.image] with [RenderPipeline.imageBuffer].
 */
fun Drawer.image(renderPipeline: RenderPipeline) {
    this.image(renderPipeline.imageBuffer)
}
