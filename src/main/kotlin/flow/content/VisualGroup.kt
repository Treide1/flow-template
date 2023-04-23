package flow.content

import org.openrndr.Program
import org.openrndr.draw.Drawer

/**
 * Visual group that can be drawn.
 * Inherit as object and implement [draw].
 */
abstract class VisualGroup(program: Program) {

    val drawer = program.drawer
    abstract fun Drawer.draw()

    fun draw() {
        drawer.pushStyle()
        drawer.draw()
        drawer.popStyle()
    }

}