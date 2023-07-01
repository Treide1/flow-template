package flow.shadertoy.projects.jaszUniverse

import flow.shadertoy.ProjectImporter
import flow.shadertoy.ProjectRenderer
import flow.shadertoy.ShadertoyProject
import flow.shadertoy.ShadertoyProject.ShadertoyTab.ChannelSettings.Channel.CHANNEL_0
import flow.shadertoy.ShadertoyProject.ShadertoyTab.ChannelSettings.ChannelInput.BUFFER_A_IN
import flow.shadertoy.ShadertoyProject.ShadertoyTab.ChannelSettings.ChannelInput.MICROPHONE
import org.openrndr.Program

/**
 * A renderer for the shadertoy project ["Retro Sun Modified" by JennySchub](https://www.shadertoy.com/view/WdGSzz).
 */
class JaszUniverse(program: Program): ProjectRenderer(program) {

    override fun ProjectImporter.importProject(): ShadertoyProject {

        return buildAndImport("$projectsPath/jaszUniverse", "jaszUniverse") {
            image.setInput(CHANNEL_0, MICROPHONE)
        }
    }

    // parameters and init
    // ...

}