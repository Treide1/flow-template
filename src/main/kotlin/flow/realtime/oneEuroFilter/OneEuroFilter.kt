package flow.realtime.oneEuroFilter

import kotlin.math.abs

/**
 * The "OneEuroFilter" is a simple filter for noisy signals.
 * It values local stability (when noise > signal), but also accounts for responsiveness (when signal > noise).
 *
 * Original paper: https://gery.casiez.net/1euro/
 *
 * This code is a re-implementation of the Java version by Stéphane Conversy.
 * This is not a direct port. It simplifies and renames to better fit with Kotlin.
 *
 * @param minCutoff The minimum cutoff frequency. Has to be positive.
 * @param beta Cutoff bias. No constraints.
 * @param dCutoff The cutoff frequency when the speed is "large". Has to be positive.
 * @param initVal The initial value of the filter. Defaults to 0.0.
 *
 * @author Lukas Henke
 */
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

    /**
     * The current filtered value.
     */
    var value = initVal

    private fun alpha(cutoff: Double): Double {
        val te = 1.0 / freq
        val tau = 1.0 / (2 * Math.PI * cutoff)
        return 1.0 / (1.0 + tau / te)
    }

    /**
     * Pass the current [value] and the time difference [dTime] and get the updated value.
     */
    fun filter(value: Double, dTime: Double): Double {
        // Update the sampling frequency based on delta time
        freq = 1.0 / dTime
        
        // Estimate the current variation per second
        val dValue = (value - x.y) * freq
        val edValue = dx.filterWithAlpha(dValue, alpha(dCutoff))
        
        // Use it to update the cutoff frequency
        val cutoff = minCutoff + beta * abs(edValue)
        
        // Filter the given value
        this.value = x.filterWithAlpha(value, alpha(cutoff))
        return this.value
    }

    class LowPassFilter(alpha: Double, initVal: Double = 0.0) {

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