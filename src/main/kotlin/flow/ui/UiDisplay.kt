@file:Suppress("unused")

package flow.ui

import flow.input.InputScheme
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated

class UiDisplay(val inputScheme: InputScheme) {

    private val valueUpdateList = mutableListOf<Pair<String, () -> String>>()

    fun trackValue(name: String, toValue: () -> String) {
        valueUpdateList += name to { toValue() }
    }

    fun untrackValue(name: String) {
        valueUpdateList.removeAll { it.first == name }
    }

    fun displayOnDrawer(drawer: Drawer) {
        val maxNameLength = valueUpdateList.maxOfOrNull { it.first.length } ?: 0
        val valueLines = valueUpdateList.map { (name, toValue) ->
            val paddedName = "$name: ".padEnd(maxNameLength+2, ' ')
            "  $paddedName${toValue()}"
        }

        drawer.isolated {
            val controlTextLines = inputScheme.getControlsText().split("\n")
            val lines = controlTextLines + listOf("", "Values:") + valueLines

            drawer.displayLinesOfText(lines)
        }
    }
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