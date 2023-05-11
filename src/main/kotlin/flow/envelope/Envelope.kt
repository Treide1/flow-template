@file:Suppress("unused")

package flow.envelope

import org.openrndr.math.map

/**
 * Envelope holds an evaluation function [eval] over the interval [[0], [length]].
 * Output range is unit interval [[0], [1]] by convention.
 * Use [sample] to get the value at a specific phase.
 *
 * This class goes well with [interpolationList] for creating a list of equidistant entries.
 * The result can then be used element-wise with [add] or [mult].
 */
data class Envelope(
    val length: Double = 4.0,
    val eval: (Double) -> Double = { 0.0 }
) {

    // Input validation: Length needs to be strictly positive.
    init {
        if (length <= 0.0) throw IllegalArgumentException("Interval length needs to be positive. Found $length.")
    }

    /**
     * Sample this Envelope at the value [phase].
     *
     * @param phase Phase at which to sample the eval. Wraps around for values outside [[0], [length]].
     */
    fun sample(phase: Double): Double {
        // If phase is in range, calculate directly. This is the usual case and gets priority.
        if (phase in 0.0..length) return eval(phase)

        // Non-standard values are wrapped around to land inside [0, length].
        val multiple = (phase / length).toInt()
        val samplingPhase = (phase - multiple * length).plusMod(length, length)
        return eval(samplingPhase)
    }

}

/**
 * Get ([this] + [add]) % [mod].
 */
fun Double.plusMod(add: Double, mod: Double): Double = (this + add) % mod


/**
 * Create List of equidistant entries within range [[phaseStart], [phaseEnd]], bounds inclusive.
 * @param phaseStart Lower range bound.
 * @param phaseEnd Upper range bound.
 * @param size Size of interpolation points between [phaseStart] and [phaseEnd], bounds included. Therefore, size of list.
 */
fun interpolationList(phaseStart: Double = 0.0, phaseEnd: Double = 1.0, size: Int): List<Double> {
    return List(size) { index -> index.toDouble().map(0.0, size-1.0, phaseStart, phaseEnd)}
}

/**
 * Get the result of element-wise list addition. If [other] is null, just returns [this].
 */
infix fun List<Double>.add(other: List<Double>?): List<Double> {
    if (other == null) return this
    return this.zip(other) { a, b -> a + b }
}

/**
 * Get the result of element-wise list multiplication. If [other] is null, just returns [this].
 */
infix fun List<Double>.mult(other: List<Double>?): List<Double> {
    if (other == null) return this
    return this.zip(other) { a, b -> a * b }
}

/**
 * Get the result of element-wise list addition. If [other] is null, just returns [this].
 */
infix fun MutableList<Double>.addEqual(other: List<Double>?) {
    if (other == null) return
    if (this.size != other.size) return
    this.zip(other) { a, b -> a + b }.forEachIndexed { i, value ->
        this[i] = value
    }
}

/**
 * Get the result of element-wise list multiplication. If [other] is null, just returns [this].
 */
infix fun MutableList<Double>.multEqual(other: List<Double>?) {
    if (other == null) return
    if (this.size != other.size) return
    this.zip(other) { a, b -> a + b }.forEachIndexed { i, value ->
        this[i] = value
    }
}
