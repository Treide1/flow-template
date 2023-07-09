package flow.shadertoy.projects.jaszUniverse

import flow.FlowProgram
import flow.shadertoy.ProjectImporter
import flow.shadertoy.ProjectRenderer
import flow.shadertoy.ShadertoyProject
import flow.shadertoy.ShadertoyProject.ShadertoyTab.ChannelSettings.Channel.CHANNEL_0
import flow.shadertoy.ShadertoyProject.ShadertoyTab.ChannelSettings.ChannelInput.MICROPHONE
import org.openrndr.math.Vector3
import org.openrndr.math.times

/**
 * A renderer for the shadertoy project ["Jasz Universe" by jaszunio15](https://www.shadertoy.com/view/wsSGRd).
 */
class JaszUniverse(val flowProgram: FlowProgram): ProjectRenderer(flowProgram) {

    override fun ProjectImporter.importProject(): ShadertoyProject {

        return buildAndImport("$projectsPath/jaszUniverse", "jaszUniverse") {
            image.setInput(CHANNEL_0, MICROPHONE)
        }
    }

    // TODO: refactor out to color API
    // Colors
    val a = Vector3(0.5, 0.5, 0.5)
    val b = Vector3(0.5, 0.5, 0.5)
    val c = Vector3(2.0, 1.0, 0.0)
    val d = Vector3(0.50, 0.20, 0.25)

    fun palette(t: Double): Vector3 {
        return a + b * Math::cos.elementwise(6.28318 * (c * t + d))
    }

    // parameters and init
    var color1: Vector3 by parameters
    var color2: Vector3 by parameters
    var musicVolume: Double by parameters

    var midT = 0.0

    init {
        color1 = Vector3(0.0, 1.0, 1.0)
        color2 = Vector3(1.0, 0.0, 0.9)
        musicVolume = 0.0
    }

    fun changeColors() {
        midT += 0.05
        color1 = palette(midT-0.13)
        color2 = palette(midT+0.13)
    }

}

fun Vector3.elementwise(f: (Double) -> Double): Vector3 {
    return Vector3(f(x), f(y), f(z))
}

fun ((Double) -> Double).elementwise(v: Vector3): Vector3 {
    return Vector3(this(v.x), this(v.y), this(v.z))
}