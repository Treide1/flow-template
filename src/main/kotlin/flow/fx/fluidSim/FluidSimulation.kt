package flow.fx.fluidSim

import flow.shadertoy.GlslFileBuilder
import flow.shadertoy.Importer
import flow.shadertoy.ProjectRenderer
import flow.shadertoy.ShadertoyProject.ShadertoyTab.ChannelSettings.Channel.CHANNEL_0
import flow.shadertoy.ShadertoyProject.ShadertoyTab.ChannelSettings.Channel.CHANNEL_1
import flow.shadertoy.ShadertoyProject.ShadertoyTab.ChannelSettings.ChannelInput.*
import org.openrndr.Program
import org.openrndr.application

class FluidSimulation(program: Program): ProjectRenderer(program, Importer().fromJson("/generated/fluidSim")) {

    // parameters and init

}

/**
 * Generate files
 */
fun main(args: Array<String>) {
    application {
        program {
            val project =  Importer().fromResourceDirectory("fluidSim", "/fluidSim")
            project.bufferA!!.setInput(CHANNEL_0, BUFFER_C_IN)
            project.bufferB!!.setInput(CHANNEL_0, BUFFER_A_IN)
            project.bufferC!!.setInput(CHANNEL_0, BUFFER_B_IN)
            project.bufferD!!.setInput(CHANNEL_0, BUFFER_A_IN).setInput(CHANNEL_1, BUFFER_D_IN)
            project.image    .setInput(CHANNEL_0, BUFFER_D_IN)
            val builder = GlslFileBuilder(project)
            builder.generate() // Only required to be executed once
            application.exit()
        }
    }
}