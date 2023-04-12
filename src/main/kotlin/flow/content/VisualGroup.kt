package flow.content

import org.openrndr.Program
import org.openrndr.draw.Drawer

abstract class VisualGroup {
    abstract fun draw(drawer: Drawer, program: Program)
}