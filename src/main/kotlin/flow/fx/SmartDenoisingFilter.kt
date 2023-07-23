package flow.fx

import org.openrndr.draw.Filter1to1
import org.openrndr.draw.filterShaderFromCode
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.resourceText

@Description("Smart Denoising")
class SmartDenoisingFilter : Filter1to1(
    filterShaderFromCode(resourceText("/smartDenoise.glsl"), "smartDenoise")
) {

    @DoubleParameter("sigma", 0.0, 10.0)
    var sigma: Double by parameters

    @DoubleParameter("kSigma", 0.0, 10.0)
    var kSigma: Double by parameters

    @DoubleParameter("threshold", 0.0, 1.0)
    var threshold: Double by parameters

    init {
        // Original values:  5.0, 2.0, .100
        sigma = 5.0
        kSigma = 2.0
        threshold = .100
    }
}