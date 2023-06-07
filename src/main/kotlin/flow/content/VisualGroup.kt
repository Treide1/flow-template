package flow.content

import org.openrndr.Program
import org.openrndr.draw.Drawer

/**
 * Visual group that can be drawn.
 * Inherit as object and implement [draw].
 *
 * Define group properties as object's properties.
 */
abstract class VisualGroup(program: Program) {

    var drawer = program.drawer

    /**
     * The draw procedure to be implemented.
     */
    abstract fun Drawer.draw()

    /**
     * Draw the visual group.
     */
    fun draw() {
        drawer.pushStyle()
        drawer.draw()
        drawer.popStyle()
    }

}