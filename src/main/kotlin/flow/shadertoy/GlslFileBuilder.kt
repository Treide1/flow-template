package flow.shadertoy

import com.google.gson.GsonBuilder
import flow.shadertoy.FileType.Companion.toFileName
import flow.shadertoy.ShadertoyProject.ShadertoyTab
import flow.shadertoy.ShadertoyProject.ShadertoyTab.ChannelSettings.Channel
import flow.shadertoy.ShadertoyProject.ShadertoyTab.ChannelSettings.ChannelInput
import java.io.File

class GlslFileBuilder(val project: ShadertoyProject) {

    /**
     * Generates the corresponding GLSL code for the given [project] and a json to reconstruct it later.
     *
     * The individual steps are:
     * * Updating the signature of `mainImage` shadertoy functions
     * * Add the Filter header
     *   * For each channel input, add a uniform sampler2D
     * * Add uniforms for the shadertoy values like iResolution
     * * Add defines to replace fragCoord and fragColor with the Filter interface
     * * Add the Common code
     * * Add the tab-specific code
     */
    fun generate() {
        // Read out common code before the others tabs. Then it can be integrated into their code.
        val commonCode = project.common?.code ?: "// No 'common.glsl' code added"

        // Build each tab that is import for GLSL code generation
        project.getGlslTabs().forEach { tab ->

            // Make the shadertoy code GLSL version 330 compatible
            // This means: Using the correct main signature
            val updatedCode = tab.code
                .replace(
                    "void mainImage( out vec4 fragColor, in vec2 fragCoord )", // TODO: make whitespace-robust
                    "void main()"
                )

            // GLSL Code
            var texCounter = 0
            val channels = tab.getOrderedChannels()

            val glslCode = """
            // Shader Interface:
            #version 330
            
            in vec2 v_texCoord0;
            
            """.trimIndent() +

            channels.joinToString("\n") { (index, _) ->
            """
            uniform sampler2D tex$texCounter;
            #define iChannel$index tex${texCounter++}
            """.trimIndent()
            } +

            """
            
            out vec4 o_color;
            
            uniform vec3 iResolution;
            uniform float iTime;
            uniform float iTimeDelta;
            uniform float iFrame;
            uniform vec4 iMouse;
            
            #define fragCoord (v_texCoord0 * iResolution.xy)
            #define fragColor o_color
            
            """.trimIndent() + "\n" +
            "// Common definitions across shaders:\n" +
            "${commonCode.trimIndent()}\n" +
            "\n" +
            "// Shadertoy fragment shader code:\n" +
            updatedCode.trimIndent()

            val glslPath = "$GENERATED_RESOURCE_PREFIX/${project.name}/${tab.toFileName()}"
            println("Saving shader code under /$glslPath")
            val glslFile = File(glslPath)
            // Create parent directories if they don't exist
            glslFile.parentFile.mkdirs()
            glslFile.writeText(glslCode)
        }

        val pathToProject = "/generated/${project.name}"
        val gson = GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(ShadertoyProject::class.java, ProjectToJson(pathToProject))
            .registerTypeAdapter(ShadertoyTab::class.java, TabToJson(pathToProject))
            .create()

        // Build a json from the project to be able to reconstruct it later
        val json = gson.toJsonTree(project).asJsonObject
        json.remove("common")

        // Write the json to a project file
        val jsonPath = "$GENERATED_RESOURCE_PREFIX/${project.name}/project.json"
        println("Saving json for /$jsonPath")
        val jsonFile = File(jsonPath)
        jsonFile.parentFile.mkdirs() // Ensure parent dir existence
        jsonFile.writeText(gson.toJson(json))
    }

    companion object {
        const val GENERATED_RESOURCE_PREFIX = "src/main/resources/generated"
    }

}

/**
 * Returns a list of all tabs that are relevant for GLSL code generation.
 * This includes all tabs that exist and are not the [ShadertoyProject.common] tab.
 */
fun ShadertoyProject.getGlslTabs() = listOfNotNull(image, bufferA, bufferB, bufferC, bufferD, cubeA, sound)

fun ShadertoyTab.getOrderedChannels(): List<Pair<Channel, ChannelInput>> {
    return this.channelSettings.channelsMap.entries.sortedBy { it.key.ordinal }.map { it.toPair() }
}