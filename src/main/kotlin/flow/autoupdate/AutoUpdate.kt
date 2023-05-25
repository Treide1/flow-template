@file:Suppress("unused")

package flow.autoupdate

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.Drawer

/**
 * AutoUpdate API.
 *
 * This extension executes a list of functions before each draw call.
 * You can add inline functions to automatically update your values.
 */
object AutoUpdate: Extension {

    override var enabled = true

    private val updateFunctions = mutableListOf<() -> Unit>()

    /**
     * Before the draw call, the update functions are executed.
     */
    override fun beforeDraw(drawer: Drawer, program: Program) {
        updateFunctions.forEach { it() }
    }

    /**
     * Adds an update function to the list.
     */
    fun <T: Any> T.autoUpdate(block: T.() -> Unit): T {
        updateFunctions.add { block() }
        return this
    }

    /**
     * Adds an update function to the list.
     */
    fun autoUpdate(block: () -> Unit) {
        updateFunctions.add(block)
    }

}