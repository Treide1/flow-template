package flow.shadertoy

import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import flow.shadertoy.Shader.RenderStep.Input.Sampler
import flow.shadertoy.ShadertoyProject.ShadertoyTab.*
import java.lang.reflect.Type

/**
 *
 */
class JsonToProject: JsonDeserializer<ShadertoyProject> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): ShadertoyProject {
        val gson = Gson()

        val obj = json.asJsonObject
        val info = obj.get("info").asJsonObject
        val renderpass = obj.get("renderpass").asJsonArray

        val project = ShadertoyProject(info.get("name").asString)

        renderpass.forEach { tab ->
            val settings = ChannelSettings()

            val tabObj = tab.asJsonObject
            val name = tabObj.get("name").asString
            val code = tabObj.get("code").asString
            val inputs = tabObj.get("inputs").asJsonArray

            inputs.map { input ->
                val inputObj = input.asJsonObject
                val channelInt = inputObj.get("channel").asInt
                val id = inputObj.get("id").asInt
                val sampler = gson.fromJson(inputObj.get("sampler").asJsonObject, Sampler::class.java)

                val channel = ChannelSettings.Channel.values()[channelInt]
                val channelInput = ChannelSettings.ChannelInput.getById(id)

                settings.channelsMap[channel] = channelInput
                channelInput.sampler = sampler
            }

            when (name) {
                "Common"   -> project.common = Common()  .setCode(code).setSettings(settings)
                "Image"    -> project.image = Image()    .setCode(code).setSettings(settings)
                "Buffer A" -> project.bufferA = BufferA().setCode(code).setSettings(settings)
                "Buffer B" -> project.bufferB = BufferB().setCode(code).setSettings(settings)
                "Buffer C" -> project.bufferC = BufferC().setCode(code).setSettings(settings)
                "Buffer D" -> project.bufferD = BufferD().setCode(code).setSettings(settings)
                "Cube A"   -> project.cubeA = CubeA()    .setCode(code).setSettings(settings)
                "Sound"    -> project.sound = Sound()    .setCode(code).setSettings(settings)
            }
        }
        return project
    }
}
