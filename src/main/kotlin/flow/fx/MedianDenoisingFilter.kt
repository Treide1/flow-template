package flow.fx

import org.openrndr.draw.Filter1to1
import org.openrndr.draw.filterShaderFromCode
import org.openrndr.resourceText

class MedianDenoisingFilter : Filter1to1(
    filterShaderFromCode(resourceText("/medianDenoising.glsl"), "medianDenoising")
)