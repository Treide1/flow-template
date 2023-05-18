package flow.fx

import org.openrndr.draw.Filter
import org.openrndr.draw.filterShaderFromCode
import org.openrndr.resourceText

class MedianDenoisingFilter : Filter(
    filterShaderFromCode(resourceText("/medianDenoising.glsl"), "medianDenoising")
)