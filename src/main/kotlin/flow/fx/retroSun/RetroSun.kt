package flow.fx.retroSun

import flow.FlowProgram
import flow.shadertoy.GlslFileBuilder
import flow.shadertoy.Importer
import flow.shadertoy.ProjectRenderer
import flow.shadertoy.ShadertoyProject
import flow.shadertoy.ShadertoyProject.ShadertoyTab.ChannelSettings.Channel.*
import flow.shadertoy.ShadertoyProject.ShadertoyTab.ChannelSettings.ChannelInput.*
import org.openrndr.application

class RetroSun(program: FlowProgram): ProjectRenderer(program, Importer().fromJson("/generated/retroSun")!!) {

    // parameters and init

}

/**
 * Generate files
 */
fun main(args: Array<String>) {
    application {
        program {
            val project =  Importer().fromResourceDirectory("retroSun", "/retroSun")
            val builder = GlslFileBuilder(project)
            builder.generate() // Only required to be executed once
            application.exit()
        }
    }
}