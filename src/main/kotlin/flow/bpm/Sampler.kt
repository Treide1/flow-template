package flow.bpm

import flow.bpm.envelope.Envelope
import kotlin.reflect.KProperty

/**
 * Sampler data class. Holds a reference to an [Envelope] and
 * provides a [value] property that is updated on every [update] call.
 * The [value] property can be accessed via delegation.
 */
data class Sampler(val envelope: Envelope) {

    var value = 0.0
        private set

    fun update(phase: Double) {
        value = envelope.sample(phase)
    }

    operator fun getValue(requestee: Nothing?, property: KProperty<*>): Double {
        return value
    }
}