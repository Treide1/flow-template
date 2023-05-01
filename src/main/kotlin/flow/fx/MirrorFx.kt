@file:Suppress("unused")

package flow.fx

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Filter
import org.openrndr.draw.filterShaderFromCode
import org.openrndr.resourceText

/**
 * Filter class for mirroring using flame algorithm variations.
 * Source: https://flam3.com/flame_draves.pdf
 *
 * @param stencilBuffer The stencil buffer to use for the mirror effect. Has to be of format ColorFormat.R, type ColorType.UINT8.
 */
// TODO: use in TemplateProgram.kt
class MirrorFilter(
    stencilBuffer: ColorBuffer
) : Filter(
    filterShaderFromCode(resourceText("/mirrorFlameVars.glsl"), "mirrorFlameVars")
) {
    /**
     * The stencil buffer to use for the mirror effect. Has to be of format ColorFormat.R, type ColorType.UINT8.
     */
    var stencil by parameters

    /**
     * y scale of the stencil buffer. Defaults to the width / height ratio of the stencil buffer.
     */
    var yScl by parameters

    /**
     * Flag to enable / disable fading towards the max iterations.
     */
    var fade by parameters

    // Internal [fade] access
    var _fade = true
        set(value) {
            field = value
            fade = value
        }

    init {
        stencil = stencilBuffer
        yScl = stencilBuffer.width.toDouble() / stencilBuffer.height.toDouble()
        fade = _fade
    }

    companion object {
        val flameVarNames = listOf(
            "identity", // 0
            "sinusoidal", // 1
            "spherical", // 2
            "swirl", // 3
            "horseshoe", // 4
            "polar", // 5
            "handkerchief", // 6
            "heart", // 7
            "disc", // 8
            "spiral", // 9
            "hyperbolic", // 10
            "diamond", // 11
            "ex", // 12
            "julia", // 13
            "bent", // 14
            "waves", // 15
            "fisheye", // 16
        )
    }
}

/**
 * Convert an integer to a ColorRGBa with [this] as the red channel for UINT8 color buffers.
 */
fun Int.toR(): ColorRGBa = ColorRGBa(r = this / 256.0, g = 0.0, b = 0.0)