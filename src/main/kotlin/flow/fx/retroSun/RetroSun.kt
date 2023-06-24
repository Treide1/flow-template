package flow.fx.retroSun

import flow.shadertoy.ProjectImporter
import flow.shadertoy.ProjectRenderer
import flow.shadertoy.ShadertoyProject
import org.openrndr.Program

/**
 *
 */
class RetroSun(program: Program): ProjectRenderer(program) {

    override fun importProject(): ShadertoyProject {
        val original = ProjectImporter.import("retroSun", "src/main/resources/retroSun")
        return ProjectImporter.generateAndImport(original, ".")
    }

    // parameters and init

}