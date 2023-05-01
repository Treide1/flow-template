@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package util

import flow.bpm.envelope.Envelope
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated

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
            if (time < onGateOpen.length) onGateOpen.sample(dt) else holdValue
        } else {
            if (time < onGateClosed.length) onGateClosed.sample(dt) else offValue
        }
    }
}

/**
 * Linear interpolation between two values.
 */
fun Double.lerp(other: Double, perc: Double): Double {
    return this + (other - this) * perc
}

/**
 * Displays multiple [lines] of text.
 *
 * The lines are offset vertically from each other by [yOff]. The fill color is [color].
 */
fun Drawer.displayLinesOfText(lines: List<String>, yOff: Double = 25.0, color: ColorRGBa = ColorRGBa.WHITE) {
    val x = 10.0
    var y = 20.0
    this.isolated {
        fill = color
        stroke = color
        lines.forEach {  line ->
            text(line, x, y)
            y += yOff
        }
    }
}
