package flow.shadertoy.projects.retroSunModified

import flow.shadertoy.ProjectImporter
import flow.shadertoy.ProjectRenderer
import flow.shadertoy.ShadertoyProject
import flow.shadertoy.ShadertoyProject.ShadertoyTab.ChannelSettings.Channel.CHANNEL_0
import flow.shadertoy.ShadertoyProject.ShadertoyTab.ChannelSettings.ChannelInput.BUFFER_A_IN
import org.openrndr.Program

/**
 * A renderer for the shadertoy project ["Retro Sun Modified" by JennySchub](https://www.shadertoy.com/view/WdGSzz).
 */
class RetroSunModified(program: Program): ProjectRenderer(program) {

    override fun ProjectImporter.importProject(): ShadertoyProject {

        return buildAndImport("$projectsPath/retroSunModified", "Retro Sun Modified") {
            bufferA!!.setInput(CHANNEL_0, BUFFER_A_IN)
            image.setInput(CHANNEL_0, BUFFER_A_IN)
        }
    }

    // parameters and init
    // ...

}