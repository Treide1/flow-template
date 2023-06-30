package flow.shadertoy.projects.retroSun

import flow.shadertoy.ProjectImporter
import flow.shadertoy.ProjectRenderer
import flow.shadertoy.ShadertoyProject
import org.openrndr.Program

/**
 * A renderer for the shadertoy project ["Retro Sun" by FraglessPlayer](https://www.shadertoy.com/view/4dcyW7).
 */
class RetroSun(program: Program): ProjectRenderer(program) {

    override fun ProjectImporter.importProject(): ShadertoyProject {

        return buildAndImport("$projectsPath/retroSun", "retroSun") {}
    }

    // parameters and init
    // ...

}