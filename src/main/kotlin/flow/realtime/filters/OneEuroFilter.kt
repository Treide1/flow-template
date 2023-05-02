package flow.realtime.filters

import kotlin.math.abs

/**
 * OneEuroFilter is a simple filter for noisy signals.
 *
 * Original paper: https://gery.casiez.net/1euro/
 *
 * This code is a re-implementation of the Java version by Stéphane Conversy.
 * However, it is not a direct port, but rather a Kotlin version with some
 * direct improvements and clearer names.
 *
 * @param minCutoff The minimum cutoff frequency. Has to be >0.
 * @param beta Cutoff bias. No constraints.
 * @param dCutoff The cutoff frequency when the speed is "large". Has to be >0.
 * @param initVal The initial value of the filter. Defaults to 0.0.
 *
 * @author Lukas Henke
 */
// TODO: Fix stuck-at-zero bug (hitting 0.0 and not recovering)
class OneEuroFilter(
    var minCutoff: Double = 1.0,
    var beta: Double = 0.0,
    var dCutoff: Double = 1.0,
    initVal: Double = 0.0
) {

    /**
     * The current sampling frequency.
     * Initialized to 120Hz, but updated automatically after timestamps.
     */
    var freq = 120.0

    var x = LowPassFilter(alpha(minCutoff), initVal)
    var dx = LowPassFilter(alpha(dCutoff), 0.0)


    fun alpha(cutoff: Double): Double {
        val te = 1.0 / freq
        val tau = 1.0 / (2 * Math.PI * cutoff)
        return 1.0 / (1.0 + tau / te)
    }
    
    fun filter(value: Double, dTime: Double): Double {
        // Update the sampling frequency based on delta time
        freq = 1.0 / dTime
        
        // Estimate the current variation per second
        val dValue = (value - x.y) * freq
        val edValue = dx.filterWithAlpha(dValue, alpha(dCutoff))
        
        // Use it to update the cutoff frequency
        val cutoff = minCutoff + beta * abs(edValue)
        
        // Filter the given value
        return x.filterWithAlpha(value, alpha(cutoff))
    }

    class LowPassFilter(var alpha: Double, initVal: Double = 0.0) {

        var s = initVal
        var y = s
        var a = alpha
            set(value) {
                field = value.coerceAtLeast(0.0).coerceAtMost(1.0)
            }

        fun filter(value: Double): Double {
            y = value
            s = a * y + (1.0 - a) * s
            return s
        }

        fun filterWithAlpha(value: Double, alpha: Double): Double {
            a = alpha
            return filter(value)
        }
    }
}