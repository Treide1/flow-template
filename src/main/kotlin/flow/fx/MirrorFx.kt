@file:Suppress("unused")

package flow.fx

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Filter
import org.openrndr.draw.filterShaderFromCode
import org.openrndr.math.saturate
import org.openrndr.resourceText
import kotlin.math.max

/**
 * Filter class for mirroring using flame algorithm variations.
 * See: [Electric Sheep Flame Algorithm](https://flam3.com/flame_draves.pdf)
 *
 * Inspired by [MilkDrop](https://www.geisswerks.com/milkdrop/).
 * See: [MilkDrop - Wikipedia](https://en.wikipedia.org/wiki/MilkDrop)
 *
 * Takes the [stencil] to use a mirror function for the stencil value.
 * Performs operations on transformed uv coordinates. [0, 1]^2 -> [-1, 1]^2
 * Example:
 * ```
 * // Custom made
 * 0 - identity, f(uv) = uv
 * 1 - flipX,    f(uv) = vec2(-x,  y)
 * 2 - flipY,    f(uv) = vec2(x , -y)
 *
 * // Flame variations
 * 128+1 - sinusoidal, f(uv) = sin(uv)
 * ...
 * 128+16 - fisheye, f(uv) = uv.yx * 2 / (len(uv) + 1)
 * ```
 *
 * @param stencilBuffer The stencil buffer that is bound to the [stencil] parameter. Has to be a single-channel unit8 buffer.
 */
class MirrorFilter(
    stencilBuffer: ColorBuffer
) : Filter(
    filterShaderFromCode(resourceText("/mirrorFlameVars.glsl"), "mirrorFlameVars")
) {
    /**
     * The stencil buffer to use for the mirror effect. Has to be of format ColorFormat.R, type ColorType.UINT8 !
     */
    private var stencil by parameters

    /**
     * y scale of the stencil buffer. Defaults to the width / height ratio of the stencil buffer.
     */
    private var yScl by parameters

    /**
     * Flag to enable / disable fading towards the max iterations.
     */
    private var fade by parameters

    /**
     * The exponent for the fading. Higher values result in a faster fading.
     */
    private var fadeExp by parameters

    /**
     * The maximum number of iterations to perform. Higher values result in more detailed images.
     */
    private var iterCount by parameters

    /**
     * Accessor for [fade].
     */
    var _fade = false
        set(value) {
            field = value
            fade = value
        }

    /**
     * Accessor for [fadeExp].
     */
    var _fadeExp = 2.0
        set(value) {
            field = value
            fadeExp = value.saturate()
        }

    /**
     * Accessor for [iterCount].
     */
    var _iterCount = 100
        set(value) {
            field = value
            iterCount = max(value, 1)
        }

    init {
        // Immutable values
        stencil = stencilBuffer
        yScl = stencilBuffer.width.toDouble() / stencilBuffer.height.toDouble()

        // Mutable values, that require initialization
        fade = _fade
        fadeExp = _fadeExp
        iterCount = _iterCount
    }

    companion object {
        val customNames = listOf(
            "identity", // 0
            "flipX", // 1
            "flipY", // 2
            "flipXY", // 3
            "rotateAndScale", // 4
        )
        val flameVariationNames = listOf(
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