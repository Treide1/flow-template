package flow.autoupdate

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.Drawer

object AutoUpdate: Extension {

    override var enabled = true

    private val updateFunctions = mutableListOf<() -> Unit>()

    override fun beforeDraw(drawer: Drawer, program: Program) {
        updateFunctions.forEach { it() }
    }

    fun <T: Any> T.autoUpdate(block: T.() -> Unit): T {
        updateFunctions.add { block() }
        return this
    }

    fun autoUpdate(block: () -> Unit) {
        updateFunctions.add(block)
    }

}