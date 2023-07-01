package flow.shadertoy.projects.jaszUniverse

import flow.FlowProgram
import flow.shadertoy.ProjectImporter
import flow.shadertoy.ProjectRenderer
import flow.shadertoy.ShadertoyProject
import flow.shadertoy.ShadertoyProject.ShadertoyTab.ChannelSettings.Channel.CHANNEL_0
import flow.shadertoy.ShadertoyProject.ShadertoyTab.ChannelSettings.ChannelInput.MICROPHONE

/**
 * A renderer for the shadertoy project ["Retro Sun Modified" by JennySchub](https://www.shadertoy.com/view/WdGSzz).
 */
class JaszUniverse(val flowProgram: FlowProgram): ProjectRenderer(flowProgram) {

    override fun ProjectImporter.importProject(): ShadertoyProject {

        return buildAndImport("$projectsPath/jaszUniverse", "jaszUniverse") {
            image.setInput(CHANNEL_0, MICROPHONE)

            // TODO: Allow registering microphone
            // Wishful code: registerMicrophone(flowProgram.audio.magnitudes.format(512, 2))
        }
    }

    // parameters and init
    // ...

}