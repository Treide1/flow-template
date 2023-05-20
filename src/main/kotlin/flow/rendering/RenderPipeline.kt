package flow.rendering

import flow.fx.FxRepo
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*

class RenderPipeline(width: Int, height: Int, val drawer: Drawer) {

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

    // Backing field for fxChain
    private var fxChain: RenderPipeline.() -> Unit = {}

    /**
     * Sets the fx chain to be applied in [render].
     */
    fun setFxChain(fxChain: RenderPipeline.() -> Unit) {
        this.fxChain = fxChain
    }

    /**
     * Performs the [drawBlock] onto the draw target, then applies the [fxChain].
     * The result is then drawn to the screen.
     * @param clearDrawBuffer Whether to clear the drawBuffer before rendering.
     * @param clearImageBuffer Whether to clear the imageBuffer before rendering.
     * @param drawBlock The block to execute for drawing.
     */
    fun render(clearDrawBuffer: Boolean = true, clearImageBuffer: Boolean = true, drawBlock: Drawer.() -> Unit = {}) {
        // Clear buffers if needed
        if (clearDrawBuffer) drawBuffer.fill(ColorRGBa.TRANSPARENT)
        if (clearImageBuffer) imageBuffer.fill(ColorRGBa.TRANSPARENT)

        // Draw onto drawTarget
        drawer.isolatedWithTarget(drawTarget) {
            drawBlock()
        }

        // Apply Fx and draw to screen
        fxChain()
        drawer.image(imageBuffer)
    }

    /**
     * The [FxRepo] that holds all the effects.
     */
    val fxRepo = FxRepo(this)

    /**
     * Shortcut for [Filter.apply] with (buffer, buffer) but with temporary buffer inbetween.
     * Should this [Filter] require a custom temporary buffer, you can pass it as [customTmpBuffer].
     */
    fun Filter.apply(buffer: ColorBuffer, customTmpBuffer: ColorBuffer? = null) {
        val tmp = customTmpBuffer ?: tmpBuffer

        this.apply(buffer, tmp)
        tmp.copyTo(buffer)
    }

    /**
     * Applies the [overlay] filter to the [source] and [target] buffer combined,
     * and writes the result to the [target] buffer.
     */
    fun overlay(source: ColorBuffer, target: ColorBuffer) {
        fxRepo.overlay.apply(source, target)
    }
}