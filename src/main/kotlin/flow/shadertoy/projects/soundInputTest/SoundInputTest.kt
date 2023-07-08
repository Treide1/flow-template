package flow.shadertoy.projects.soundInputTest

import flow.FlowProgram
import flow.shadertoy.ProjectImporter
import flow.shadertoy.ProjectRenderer
import flow.shadertoy.ShadertoyProject
import flow.shadertoy.ShadertoyProject.ShadertoyTab.ChannelSettings.Channel.CHANNEL_0
import flow.shadertoy.ShadertoyProject.ShadertoyTab.ChannelSettings.ChannelInput.MICROPHONE

/**
 * A renderer for the shadertoy project ["Input - Sound" by iq](https://www.shadertoy.com/view/Xds3Rr).
 */
class SoundInputTest(val flowProgram: FlowProgram): ProjectRenderer(flowProgram) {

    override fun ProjectImporter.importProject(): ShadertoyProject {

        return buildAndImport("$projectsPath/soundInputTest", "soundInputTest") {
            image.setInput(CHANNEL_0, MICROPHONE)
        }
    }
}