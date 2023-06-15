package flow.fx

import org.openrndr.draw.Filter2to1
import org.openrndr.draw.filterShaderFromCode
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.resourceText

@Description("Crossfade")
class Crossfade: Filter2to1(
    filterShaderFromCode(resourceText("/crossfade.glsl"), "crossfade")
) {
    @DoubleParameter("blend", 0.0, 1.0)
    var blend: Double by parameters

    init {
        blend = 0.0
    }
}