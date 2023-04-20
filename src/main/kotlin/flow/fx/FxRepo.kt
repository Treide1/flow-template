package flow.fx

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Filter
import org.openrndr.extra.fx.blend.Overlay

/**
 * FxRepo is a repository for all the effects that are applied to the visual content.
 * It allows to predefine the [chain] during config, and apply it with [applyChain].
 */
class FxRepo(var chain: FxRepo.() -> Unit) {

    private val overlay by lazy { Overlay() }

    /**
     * Applies the [overlay] filter to the [source] and [target] buffer combined,
     * and writes the result to the [target] buffer.
     */
    fun overlay(source: ColorBuffer, target: ColorBuffer) {
        overlay.apply(source, target)
    }

    /**
     * Applies the fx chain from [chain].
     */
    fun applyChain() {
        chain()
    }

    /**
     * Shortcut for [Filter.apply] with (buffer, buffer).
     */
    fun Filter.apply(buffer: ColorBuffer) {
        this.apply(buffer, buffer)
    }
}