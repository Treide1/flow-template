package flow.fx.fluidSim

import flow.shadertoy.GlslFileBuilder
import flow.shadertoy.Importer
import flow.shadertoy.ProjectRenderer
import flow.shadertoy.ShadertoyProject.ShadertoyTab.ChannelSettings.Channel.CHANNEL_0
import flow.shadertoy.ShadertoyProject.ShadertoyTab.ChannelSettings.Channel.CHANNEL_1
import flow.shadertoy.ShadertoyProject.ShadertoyTab.ChannelSettings.ChannelInput.*
import org.openrndr.Program
import org.openrndr.application

/**
 *
 */
class FluidSimulation(program: Program): ProjectRenderer(program, Importer.fromJson("/generated/fluidSim")) {

    // parameters and init
    var regionSize: Double by params

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