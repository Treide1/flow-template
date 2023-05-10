package flow.rendering

import flow.fx.FxRepo
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*

class RenderPipeline(width: Int, height: Int, val drawer: Drawer) {

    val drawTarget = renderTarget(width, height) {
        colorBuffer(ColorFormat.RGBa, ColorType.FLOAT32)
        depthBuffer()
    }
    val drawBuffer = drawTarget.colorBuffer(0)
    val joinBuffer = drawBuffer.createEquivalent()

    val stencilTarget = renderTarget(width, height) {
        colorBuffer(ColorFormat.R, ColorType.UINT8)
    }
    val stencilBuffer = stencilTarget.colorBuffer(0)

    val fxRepo = FxRepo()

    fun render(clearTarget: Boolean, drawBlock: Drawer.() -> Unit) {
        // Draw block to drawTarget
        drawer.isolatedWithTarget(drawTarget) {
            if (clearTarget) {
                // Clear buffer from last frame
                clear(ColorRGBa.TRANSPARENT)
            }
            drawBlock()
        }

        // Apply Fx and draw to screen
        fxRepo.applyChain()
        drawer.image(joinBuffer)
    }
}