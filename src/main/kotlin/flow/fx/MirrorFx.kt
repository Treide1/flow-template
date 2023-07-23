@file:Suppress("unused")

package flow.fx

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
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
 *
 * (It is recommended to use a median denoise filter after this effect to reduce salt-and-pepper noise.)
 *
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
) : Filter1to1(
    filterShaderFromCode(resourceText("/mirrorFlameVars.glsl"), "mirrorFlameVars")
) {
    /**
     * The stencil buffer to use for the mirror effect. Has to be of format ColorFormat.R, type ColorType.UINT8 !
     */
    private var stencil: ColorBuffer by parameters

    /**
     * y scale of the stencil buffer. Defaults to the width / height ratio of the stencil buffer.
     */
    private var yScl: Double by parameters

    /**
     * The maximum number of iterations to perform. Higher values result in more detailed images.
     */
    private var iterCount: Int by parameters

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
        iterCount = _iterCount
    }

    enum class LookupFunctions(val id: Int) {
        IDENTITY(0),
        FLIP_X(1),
        FLIP_Y(2),
        FLIP_XY(3),
        ROTATE_AND_SCALE(4),
        REFLECT_ANGLED(5),

        SINUSOIDAL(128 + 1),
        SPHERICAL(128 + 2),
        SWIRL(128 + 3),
        HORSESHOE(128 + 4),
        POLAR(128 + 5),
        HANDKERCHIEF(128 + 6),
        HEART(128 + 7),
        DISC(128 + 8),
        SPIRAL(128 + 9),
        HYPERBOLIC(128 + 10),
        DIAMOND(128 + 11),
        EX(128 + 12),
        JULIA(128 + 13),
        BENT(128 + 14),
        WAVES(128 + 15),
        FISHEYE(128 + 16),
        ;

        /**
         * The red-channel value for the stencil buffer.
         */
        val r: ColorRGBa
            get() = this.id.toR()
    }
}

/**
 * Convert an integer to a ColorRGBa with [this] as the red channel for UINT8 color buffers.
 */
private fun Int.toR(): ColorRGBa = ColorRGBa(r = this.toDouble(), g = 0.0, b = 0.0)