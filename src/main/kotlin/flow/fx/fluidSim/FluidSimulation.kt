package flow.fx.fluidSim

import flow.FlowProgram
import flow.autoupdate.AutoUpdate.autoUpdate
import flow.envelope.LinearCapacitor
import flow.envelope.keyAutoUpdate
import flow.input.InputScheme
import flow.shadertoy.ProjectImporter
import flow.shadertoy.ProjectRenderer
import flow.shadertoy.ShadertoyProject
import flow.shadertoy.ShadertoyProject.ShadertoyTab.ChannelSettings.Channel.CHANNEL_0
import flow.shadertoy.ShadertoyProject.ShadertoyTab.ChannelSettings.Channel.CHANNEL_1
import flow.shadertoy.ShadertoyProject.ShadertoyTab.ChannelSettings.ChannelInput.*
import org.openrndr.animatable.easing.CubicIn

/**
 * A renderer for the shadertoy project ["Chimera's Breath" by nimitz](https://www.shadertoy.com/view/4tGfDW).
 *
 * Edited and wired with uniforms to communicate with the [FlowProgram].
 */
class FluidSimulation(program: FlowProgram): ProjectRenderer(program) {

    /**
     * Imports a project with only the resource tabs.
     * Then sets the channel inputs that project.
     * Then generates files and imports the used project from those.
     */
    override fun importProject(): ShadertoyProject {
        val original =  ProjectImporter.import("fluidSim", "src/main/resources/fluidSim")
        original.bufferA!!.setInput(CHANNEL_0, BUFFER_C_IN)
        original.bufferB!!.setInput(CHANNEL_0, BUFFER_A_IN)
        original.bufferC!!.setInput(CHANNEL_0, BUFFER_B_IN)
        original.bufferD!!.setInput(CHANNEL_0, BUFFER_A_IN).setInput(CHANNEL_1, BUFFER_D_IN)
        original.image    .setInput(CHANNEL_0, BUFFER_D_IN)

        return ProjectImporter.generateAndImport(original, ".")
    }

    /**
     * Region size of the displacer circle.
     */
    var regionSize: Double by parameters

    /**
     * Music-based attack envelope.
     */
    val attack: Double by program.beatClock.bindEnvelopeBySegments(4.0) {
        segmentJoin(0.1, 0.64)
        segmentJoin(1.0, 0.0) via CubicIn()
        segmentJoin(1.1, 1.0)
        segmentJoin(2.0, 0.0) via CubicIn()
        segmentJoin(2.1, 0.36)
        segmentJoin(3.0, 0.0) via CubicIn()
        segmentJoin(3.1, 1.0)
        segmentJoin(4.0, 0.0) via CubicIn()
    }

    /**
     * [LinearCapacitor] to control the activity of the displacer circle.
     */
    val attackFacCapacitor = LinearCapacitor(2.0, 0.3).keyAutoUpdate(program, "o", InputScheme.TrackTypes.TOGGLE)

    init {
        regionSize = 0.1
        autoUpdate { regionSize = attack * attackFacCapacitor.value * 0.15 }
    }
}
