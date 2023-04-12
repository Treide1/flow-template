package flow.fx

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Filter
import org.openrndr.extra.fx.blend.Overlay

class FxRepo(var chain: FxRepo.() -> Unit) {

    private val overlay by lazy { Overlay() }

    fun overlay(source: ColorBuffer, target: ColorBuffer) {
        overlay.apply(source, target)
    }

    fun applyChain() {
        chain()
    }

    fun Filter.apply(buffer: ColorBuffer) {
        this.apply(buffer, buffer)
    }
}