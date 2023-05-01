@file:Suppress("unused")

package flow.ui

import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import util.displayLinesOfText

class UiDisplay {

    var controlTextLines = listOf<String>()

    private val valueUpdateList = mutableListOf<Pair<String, () -> String>>()

    fun trackValue(name: String, toValue: () -> String) {
        valueUpdateList += name to { toValue() }
    }

    fun untrackValue(name: String) {
        valueUpdateList.removeAll { it.first == name }
    }

    fun displayUiOn(drawer: Drawer) {
        val maxNameLength = valueUpdateList.maxOfOrNull { it.first.length } ?: 0
        val valueLines = valueUpdateList.map { (name, toValue) ->
            val paddedName = "$name: ".padEnd(maxNameLength+2, ' ')
            "  $paddedName${toValue()}"
        }

        drawer.isolated {
            val lines = controlTextLines + listOf("", "Values:") + valueLines

            drawer.displayLinesOfText(lines)
        }
    }
}