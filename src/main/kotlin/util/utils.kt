@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package util

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
