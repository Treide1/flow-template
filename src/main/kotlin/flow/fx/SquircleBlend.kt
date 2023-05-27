package flow.fx

import org.openrndr.draw.*
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.resourceText

@Description("Squircle Blend")
class SquircleBlend: Filter2to1(
    filterShaderFromCode(resourceText("/squircleBlend.glsl"), "squircleBlend")
) {
    @DoubleParameter("time", 0.0, 20.0)
    // mathematically remap to "blend"
    var time: Double by parameters

    @DoubleParameter("volume", 0.0, 1.0)
    var volume: Double by parameters

    init {
        time = 0.0
        volume = 0.5
    }

    var bicubicFiltering = true

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {
        if (bicubicFiltering && source.isNotEmpty()) {
            source[0].generateMipmaps()
            source[0].filter(MinifyingFilter.LINEAR_MIPMAP_LINEAR, MagnifyingFilter.LINEAR)
        }
        super.apply(source, target)
    }
}