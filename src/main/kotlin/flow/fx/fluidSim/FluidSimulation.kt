package flow.fx.fluidSim

import flow.FlowProgram
import flow.autoupdate.AutoUpdate.autoUpdate
import flow.shadertoy.ProjectImporter
import flow.shadertoy.ProjectRenderer
import flow.shadertoy.ShadertoyProject
import flow.shadertoy.ShadertoyProject.ShadertoyTab.ChannelSettings.Channel.CHANNEL_0
import flow.shadertoy.ShadertoyProject.ShadertoyTab.ChannelSettings.Channel.CHANNEL_1
import flow.shadertoy.ShadertoyProject.ShadertoyTab.ChannelSettings.ChannelInput.*
import org.openrndr.animatable.easing.CubicIn

/**
 *
 */
class FluidSimulation(program: FlowProgram): ProjectRenderer(program) {

    override fun importProject(): ShadertoyProject {
        val original =  ProjectImporter.import("fluidSim", "src/main/resources/fluidSim")
        original.bufferA!!.setInput(CHANNEL_0, BUFFER_C_IN)
        original.bufferB!!.setInput(CHANNEL_0, BUFFER_A_IN)
        original.bufferC!!.setInput(CHANNEL_0, BUFFER_B_IN)
        original.bufferD!!.setInput(CHANNEL_0, BUFFER_A_IN).setInput(CHANNEL_1, BUFFER_D_IN)
        original.image    .setInput(CHANNEL_0, BUFFER_D_IN)

        return ProjectImporter.generateAndImport(original, ".")
    }

    // parameters and init
    var regionSize: Double by parameters

    val attack: Double by program.beatClock.bindEnvelopeBySegments(4.0) {
        segmentJoin(0.1, 1.0)
        segmentJoin(1.0, 0.0) via CubicIn()
        segmentJoin(1.1, 1.0)
        segmentJoin(2.0, 0.0) via CubicIn()
        segmentJoin(2.1, 1.0)
        segmentJoin(3.0, 0.0) via CubicIn()
    }

    init {
        regionSize = 0.1
        autoUpdate { regionSize = attack * 0.15 }
    }
}
