package flow.fx

import org.openrndr.draw.*
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.resourceText

/**
 * SquircleBlend Transition Filter.
 *
 * Blends two images using a [squircle](https://en.wikipedia.org/wiki/Squircle)-like lense.
 */
@Description("Squircle Blend")
class SquircleBlend: Filter2to1(
    filterShaderFromCode(resourceText("/squircleBlend.glsl"), "squircleBlend")
) {

    /**
     * How much to blend the squircle effect from source0 to source1
     */
    @DoubleParameter("blend", 0.0, 1.0)
    var blend: Double by parameters

    /**
     * The squircle shape mode for different amount/placement/curvature of squircle lenses.
     */
    @IntParameter("mode", 0, 17)
    var mode: Int by parameters

    init {
        blend = 0.0
        mode = 0
    }

}