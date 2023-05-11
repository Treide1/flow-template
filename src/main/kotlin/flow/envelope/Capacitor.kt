package flow.envelope

import util.lerp
import kotlin.reflect.KProperty


/**
 * Capacitor which can be charged and discharged.
 *
 * If the gate is open, the capacitor charges from [offValue] to [holdValue]
 * over the duration of [onGateOpen] with the given envelope. If the gate is closed,
 * the capacitor discharges from [holdValue] to [offValue] over the duration of [onGateClosed].
 */
open class Capacitor(
    val offValue:Double = 0.0,
    val holdValue: Double = 1.0,
) {

    var value = offValue
        private set

    var lastGate = false
        private set

    var time = 0.0
        private set

    /**
     * Envelope that is sampled when the gate is open. Should end up at [holdValue].
     */
    var onGateOpen: Envelope = Envelope()

    /**
     * Envelope that is sampled when the gate is closed. Should end up at [offValue].
     */
    var onGateClosed: Envelope = Envelope()
        set(value) {
            field = value
            time = onGateClosed.length // Time has to start after the gate closing envelope.
        }

    /**
     * Updates the capacitor with the given time delta [dt] and the [gate] state.
     */
    fun update(dt: Double, gate: Boolean) {
        if (gate != lastGate) {
            time = 0.0
        }
        lastGate = gate
        time += dt

        value = if (gate) {
            if (time < onGateOpen.length) onGateOpen.sample(time) else holdValue
        } else {
            if (time < onGateClosed.length) onGateClosed.sample(time) else offValue
        }
    }

    operator fun getValue(requester: Any, property: KProperty<*>): Double {
        return value
    }
}

class LinearCapacitor(
    openDuration: Double,
    closeDuration: Double,
    offValue:Double = 0.0,
    holdValue: Double = 1.0,
): Capacitor(
    offValue,
    holdValue
) {
    init {
        onGateOpen = Envelope(openDuration) { offValue.lerp(holdValue, it / openDuration) }
        onGateClosed = Envelope(closeDuration) { holdValue.lerp(offValue, it / closeDuration) }
    }
}