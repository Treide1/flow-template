package flow.realtime

import org.openrndr.math.clamp
import org.openrndr.math.mix

class DynamicRange(var lowerBound: Double, var upperBound: Double, var contraction: Double) {

    var loLevel = lowerBound
        private set

    var hiLevel = upperBound
        private set

    fun update(value: Double) {
        loLevel = if (value < loLevel) value else mix(loLevel, value, contraction)
        hiLevel = if (value > hiLevel) value else mix(hiLevel, value, contraction)

        loLevel = loLevel.clamp(lowerBound, upperBound)
        hiLevel = hiLevel.clamp(lowerBound, upperBound)
    }
}