@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package util

import flow.bpm.envelope.Envelope

/**
 * Data class for storing [options] and cycling through them with [next].
 * Initial value is the first element of [options]. Repeats after the last one.
 */
class CyclicFlag<T>(val options: List<T>) {
    var index = 0
    private var _value = options[index]

    val value: T
        get() = _value

    fun next() {
        index = (index + 1) % options.size
        _value = options[index]
    }

}


/**
 * Capacitor which can be charged and discharged.
 *
 * If the gate is open, the capacitor charges from [offValue] to [holdValue]
 * over the duration of [onGateOpen] with the given envelope. If the gate is closed,
 * the capacitor discharges from [holdValue] to [offValue] over the duration of [onGateClosed].
 */
data class Capacitor(
    val offValue:Double,
    val holdValue: Double
) {

    var value = offValue
        private set

    var lastGate = false
        private set
    var time = 0.0
        private set

    var onGateOpen: Envelope = Envelope()
    var onGateClosed: Envelope = Envelope()
        set(value) {
            field = value
            time = onGateClosed.length // Time has to start after the gate closing envelope.
        }

    fun update(dt: Double, gate: Boolean) {
        if (gate != lastGate) {
            time = 0.0
        }
        lastGate = gate
        time += dt

        if (gate) {
            value = if (time < onGateOpen.length) onGateOpen.sample(dt) else holdValue
        } else {
            value = if (time < onGateClosed.length) onGateClosed.sample(dt) else offValue
        }
    }
}

fun Double.lerp(other: Double, perc: Double): Double {
    return this + (other - this) * perc
}