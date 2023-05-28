package flow.fx

import org.openrndr.draw.*
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.resourceText

/**
 * Squircle Blend
 *
 * @param blend How much to blend the squircle effect from source0 to source1
 * @param mode The squircle shape mode
 */
@Description("Squircle Blend")
class SquircleBlend: Filter2to1(
    filterShaderFromCode(resourceText("/squircleBlend.glsl"), "squircleBlend")
) {
    @DoubleParameter("blend", 0.0, 1.0)
    var blend: Double by parameters

    @IntParameter("mode", 0, 17)
    var mode: Int by parameters

    init {
        blend = 0.0
        mode = 0
    }

}