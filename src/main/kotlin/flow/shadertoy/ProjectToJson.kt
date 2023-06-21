package flow.shadertoy

import com.google.gson.*
import flow.shadertoy.FileType.Companion.toFileName
import flow.shadertoy.ShadertoyProject.ShadertoyTab
import java.io.File
import java.lang.reflect.Type
import java.util.Date

internal const val UNSET_STRING = "_"

/**
 *
 */
class ProjectToJson(val resourcePath: String): JsonSerializer<ShadertoyProject> {

    override fun serialize(src: ShadertoyProject, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {

        val tabSerializer = TabToJson(resourcePath)

        val obj = JsonObject()
        obj.add("info", JsonObject().apply {
            add("id", JsonPrimitive(UNSET_STRING))
            add("date", JsonPrimitive((Date().time / 1000).toString()))
            add("name", JsonPrimitive(src.name))
            add("username", JsonPrimitive(UNSET_STRING))
            add("description", JsonPrimitive(UNSET_STRING))
            add("tags", JsonArray())
            add("viewed", JsonPrimitive(-1))
            add("likes", JsonPrimitive(-1))
        })
        obj.add("renderpass", JsonArray().apply {
            listOfNotNull(
                src.common,
                src.image,
                src.bufferA,
                src.bufferB,
                src.bufferC,
                src.bufferD,
                src.cubeA,
                src.sound,
            ).forEach {
                add(tabSerializer.serialize(it, ShadertoyTab::class.java, context))
            }
        })

        return obj
    }

}

/**
 *
 */
class TabToJson(val resourcePath: String): JsonSerializer<ShadertoyTab> {

    override fun serialize(src: ShadertoyTab, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {

        val gson = Gson()

        val obj = JsonObject()
        obj.add("name", JsonPrimitive(src.name))
        obj.add("type", JsonPrimitive(src.type))
        obj.add("inputs", JsonArray().apply {
            src.channelSettings.channelsMap.forEach { (channel, input) ->
                add(JsonObject().apply {
                    add("id", JsonPrimitive(input.id))
                    add("src", JsonPrimitive(UNSET_STRING))
                    add("ctype", JsonPrimitive(input.ctype))
                    add("channel", JsonPrimitive(channel.ordinal))
                    add("sampler", gson.toJsonTree(input.sampler))
                })
            }
        })
        obj.add("outputs", JsonArray().apply {
            src.outputId?.let {
                add(JsonObject().apply {
                    add("id", JsonPrimitive(it))
                    add("channel", JsonPrimitive(0))
                })
            }
        })

        val fileName = src.toFileName()
        val f = File("src/main/resources/$resourcePath/$fileName")
        val code = if (f.exists()) f.readText() else src.code
        obj.add("code", JsonPrimitive(code))

        return obj
    }

}