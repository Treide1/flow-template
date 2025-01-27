package flow.envelope

import kotlin.reflect.KProperty

/**
 * Sampler data class. Holds a reference to an [Envelope] and
 * provides a [value] property that is updated on every [update] call.
 * The [value] property can be accessed directly, or via delegation.
 *
 * Example:
 * ```
 * val sampler = Sampler(EnvelopeBuilder(...).build())
 * ```
 */
data class Sampler(val envelope: Envelope) {

    var value = 0.0
        private set

    fun update(phase: Double) {
        value = envelope.sample(phase)
    }

    operator fun getValue(requestee: Any?, property: KProperty<*>): Double {
        return value
    }
}