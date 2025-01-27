package flow.shadertoy

import com.google.gson.GsonBuilder
import flow.shadertoy.FileType.Companion.toFileName
import flow.shadertoy.ShadertoyProject.ShadertoyTab
import mu.KotlinLogging
import java.io.File

/**
 * File creator class for GLSL code generation.
 *
 * Takes a [ShadertoyProject] and generates the corresponding GLSL code and a json to reconstruct it later.
 *
 * @param project The project to generate the GLSL code for.
 * @param dirPath The path where the generated files should be stored. It will be extended to [generatedPath].
 * @param addProjectNameAsPath If true, the project name will be added as a subdirectory to [dirPath].
 */
class GlslFileBuilder(val project: ShadertoyProject, val dirPath: String, val addProjectNameAsPath: Boolean = false) {

    private val logger = KotlinLogging.logger {}

    /**
     * The directory within [dirPath] that contains the generated files.
     *
     * If [addProjectNameAsPath] is true, the subdirectory is named after the project name.
     * Then, it will not collide with other projects.
     */
    val generatedPath = "$dirPath/generated" + if (addProjectNameAsPath) "/${project.name}" else ""


    /**
     * Generates GLSL code and a project json.
     *
     * Procedure:
     * * Retrieve common code
     * * Per Tab
     *    * Update the signature of the `mainImage` shadertoy function to `main`
     *    * Add the Shader interface
     *       * For each channel input, add a uniform sampler2D and define to map channel to texture
     *       * Add uniforms for the shadertoy values like iResolution
     *       * Add defines to replace fragCoord and fragColor with the Filter interface
     *    * Add the common code
     *    * Add the tab-specific code
     *    * Write the code to a file
     * * Build project json
     * * Write project json
     */
    fun generate() {
        // Read out common code before the others tabs. Then it can be integrated into their code.
        val commonCode = project.common?.code ?: "// No 'common.glsl' code added"

        // Build each tab that is import for GLSL code generation
        project.getGlslTabs().forEach { tab ->

            // Make the shadertoy code GLSL version 330 compatible
            // This means: Using the correct main signature

            // This matches the common shadertoy mainImage signature but with arbitrary whitespace
            // "void mainImage( out vec4 fragColor, in vec2 fragCoord )"
            val mainImageSignature = listOf(
                "void", "mainImage", """\(""", "out", "vec4", "fragColor", ",", "in", "vec2", "fragCoord", """\)"""
            ).joinToString("""\s*""").toRegex()
            val updatedCode = tab.code.replace(mainImageSignature, "void main()")

            // GLSL Code (Be careful when changing the indent)
            var texCounter = 0 // Incremented for each channel to maintain FilterNto1 compatibility
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

            val glslPath = "$generatedPath/${tab.toFileName()}"
            logger.info { "Saving shader code at $glslPath" }
            val glslFile = File(glslPath)
            // Create parent directories if they don't exist
            glslFile.parentFile.mkdirs()
            glslFile.writeText(glslCode)
        }

        val gson = GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(ShadertoyProject::class.java, ProjectToJson(generatedPath))
            .registerTypeAdapter(ShadertoyTab::class.java, TabToJson(generatedPath))
            .create()

        // Build a json from the project to be able to reconstruct it later
        val json = gson.toJsonTree(project).asJsonObject
        json.remove("common")

        // Write the json to a project file
        val jsonPath = "$generatedPath/project.json"
        logger.info { "Saving json at $jsonPath" }
        val jsonFile = File(jsonPath)
        jsonFile.parentFile.mkdirs() // Ensure parent dir existence
        jsonFile.writeText(gson.toJson(json))
    }

}

/**
 * Returns a list of all tabs that are relevant for GLSL code generation.
 * This includes all tabs that exist and are not the [ShadertoyProject.common] tab.
 */
fun ShadertoyProject.getGlslTabs() = listOfNotNull(image, bufferA, bufferB, bufferC, bufferD, cubeA, sound)

/**
 * Returns a list of all channels in the [ShadertoyTab.channelSettings], ordered by the channel ordinal.
 *
 * This ensures the same order of channel inputs when passing to GLSL uniform samplers.
 */
fun ShadertoyTab.getOrderedChannels(): List<Pair<ShadertoyChannel, ShadertoyChannelInput>> {
    return this.channelSettings.channelsMap.entries.sortedBy { it.key.ordinal }.map { it.toPair() }
}
