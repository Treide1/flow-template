package flow.fx.retroSun

import flow.shadertoy.ProjectImporter
import flow.shadertoy.ProjectRenderer
import flow.shadertoy.ShadertoyProject
import org.openrndr.Program

/**
 * A renderer for the shadertoy project ["Retro Sun" by FraglessPlayer](https://www.shadertoy.com/view/4dcyW7).
 */
class RetroSun(program: Program): ProjectRenderer(program) {

    override fun importProject(): ShadertoyProject {
        val original = ProjectImporter.import("retroSun", "src/main/resources/retroSun")
        return ProjectImporter.generateAndImport(original, ".")
    }

    // parameters and init
    // ...

}