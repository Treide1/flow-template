package flow.content

import org.openrndr.Program
import org.openrndr.draw.Drawer

/**
 * Visual group that can be drawn.
 * Inherit as object and implement [draw].
 */
abstract class VisualGroup {
    abstract fun draw(drawer: Drawer, program: Program)
}