package flow.fx.retroSun

import flow.shadertoy.GlslFileBuilder
import flow.shadertoy.Importer
import flow.shadertoy.ProjectRenderer
import org.openrndr.Program
import org.openrndr.application

/**
 *
 */
class RetroSun(program: Program): ProjectRenderer(program, Importer.fromJson("/generated/retroSun")) {

    // parameters and init

}

/**
 * Generate files
 */
fun main(args: Array<String>) {
    application {
        program {
            val project =  Importer.fromResourceDirectory("retroSun", "/retroSun")
            val builder = GlslFileBuilder(project)
            builder.generate() // Only required to be executed once
            application.exit()
        }
    }
}