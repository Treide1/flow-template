package flow.shadertoy.projects.viscousFingering

import flow.FlowProgram
import flow.shadertoy.ProjectImporter
import flow.shadertoy.ProjectRenderer
import flow.shadertoy.ShadertoyProject
import flow.shadertoy.ShadertoyProject.ShadertoyTab.ChannelSettings.Channel.CHANNEL_0
import flow.shadertoy.ShadertoyProject.ShadertoyTab.ChannelSettings.ChannelInput.BUFFER_A_IN
import org.openrndr.extra.parameters.DoubleParameter

class ViscousFingering(program: FlowProgram): ProjectRenderer(program){

    override fun ProjectImporter.importProject(): ShadertoyProject {

        return buildAndImport("$projectsPath/viscousFingering", "Viscous Fingering") {
            bufferA!!.setInput(CHANNEL_0, BUFFER_A_IN)
            image.setInput(CHANNEL_0, BUFFER_A_IN)
        }
    }

    // parameters
    /*
    uniform float _K0; // center weight = -20/6, range = [-4, 0]
    uniform float _K1; // edge-neighbors = 4/6, range = [0, 1]
    uniform float _K2; // vertex-neighbors = 1/6, range = [0, 1]
    uniform float cs; // curl scale = 0.25, range = [0, 1]
    uniform float ls; // laplacian scale = 0.24, range = [0.0, 0.5]
    uniform float ps; // laplacian of divergence scale = -0.06, range = [-0.2, 0.0]
    uniform float ds; // divergence scale = -0.08, range = [-1, 0]
    uniform float pwr; // power when deriving rotation angle from curl = 0.2, range = [0, 1]
    uniform float amp; // self-amplification = 1.0, range = [0.9, 1.1]
    uniform float sq2; // diagonal weight = 0.7, range = [0, 1]
     */

    @DoubleParameter("center weight", -4.0, 0.0)
    var _K0: Double by parameters

    @DoubleParameter("edge-neighbors", 0.0, 1.0)
    var _K1: Double by parameters

    @DoubleParameter("vertex-neighbors", 0.0, 1.0)
    var _K2: Double by parameters

    @DoubleParameter("curl scale", -1.0, 1.0)
    var cs: Double by parameters

    @DoubleParameter("laplacian scale", 0.0, 0.5)
    var ls: Double by parameters

    @DoubleParameter("laplacian of divergence scale", -0.2, 0.0)
    var ps: Double by parameters

    @DoubleParameter("divergence scale", -1.0, 0.0)
    var ds: Double by parameters

    @DoubleParameter("power when deriving rotation angle from curl", 0.0, 1.0)
    var pwr: Double by parameters

    @DoubleParameter("self-amplification", 0.9, 1.1)
    var amp: Double by parameters

    @DoubleParameter("diagonal weight", 0.0, 1.0)
    var sq2: Double by parameters

    init {
        _K0 = -20.0/6.0
        _K1 = 4.0/6.0
        _K2 = 1.0/6.0
        cs = 0.25
        ls = 0.24
        ps = -0.06
        ds = -0.08
        pwr = 0.2
        amp = 1.0
        sq2 = 0.7
    }
}