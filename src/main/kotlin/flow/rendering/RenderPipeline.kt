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
    val flowProgram: FlowProgram
    // builder: RenderPipelineBuilder.() -> Unit = {} // TODO: for scenes api
) {

    // init { builder(RenderPipelineBuilder()) }

    /**
     * The target onto which the [render] function draws.
     */
    val drawTarget = renderTarget(width, height) {
        colorBuffer(ColorFormat.RGBa, ColorType.FLOAT32)
        depthBuffer() // Shapes and contours require a depth buffer. Otherwise, unused.
    }

    /**
     * The buffer of the [drawTarget].
     */
    val drawBuffer = drawTarget.colorBuffer(0)

    val tmpBuffer = drawBuffer.createEquivalent()

    /**
     * Final render buffer that is displayed on screen.
     */
    val imageBuffer = drawBuffer.createEquivalent()

    /**
     * Stencil target for effects. (Currently only MirrorFx)
     */
    val stencilTarget = renderTarget(width, height) {
        colorBuffer(ColorFormat.R, ColorType.UINT8)
    }

    /**
     * The buffer of [stencilTarget].
     */
    val stencilBuffer = stencilTarget.colorBuffer(0)

    /**
     *
     */
    fun render(drawBlock: RenderPipeline.() -> Unit = {}) {

        // Draw onto drawTarget
        flowProgram.drawer.isolatedWithTarget(drawTarget) {
            drawBlock()
        }

        flowProgram. drawer.image(imageBuffer)
    }

    fun clear(colorBuffer: ColorBuffer = drawBuffer, color: ColorRGBa = ColorRGBa.TRANSPARENT) {
        colorBuffer.fill(color)
    }

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

    fun ColorBuffer.applyFx(vararg filters: Filter): ColorBuffer {
        filters.forEach {
            it.apply(this)
        }
        return this
    }


    /**
     * Shortcut for [Filter.apply] with (buffer, buffer).
     * Allows to use inbetween [copyBuffer] if the [useCopyBuffer] flag is true.
     */
    fun Filter.apply(buffer: ColorBuffer, useCopyBuffer: Boolean = false, copyBuffer: ColorBuffer = tmpBuffer) {
        if (useCopyBuffer) {
            this.apply(buffer, copyBuffer)
            copyBuffer.copyTo(buffer)
        }
        else {
            this.apply(buffer, buffer)
        }
    }

    /**
     *
     */
    fun draw(function: Drawer.() -> Unit) {
        flowProgram.drawer.isolatedWithTarget(drawTarget, function)
    }

    /**
     *
     */
    fun fx(function: RenderPipeline.() -> Unit) {
        function()
    }
}

/**
 *
 */
fun Drawer.image(renderPipeline: RenderPipeline) {
    this.image(renderPipeline.imageBuffer)
}

class RenderPipelineBuilder {

    fun attachDrawer(name: String, out: Boolean = false) {
        // TODO()
    }

    fun attachStencil(name: String) {
        // TODO()
    }
}