@file:Suppress("ClassName", "unused")

package flow.shadertoy

import com.google.gson.GsonBuilder
import flow.shadertoy.ShadertoyProject.ShadertoyTab
import flow.shadertoy.ShadertoyProject.ShadertoyTab.*
import org.openrndr.resourceText

/**
 *
 */
class Importer() {

    /**
     *
     */
    fun fromJson(dirPath: String): ShadertoyProject {
        val json = resourceText("$dirPath/project.json")
        val project = GsonBuilder()
            .registerTypeAdapter(ShadertoyProject::class.java, JsonToProject())
            .create()
            .fromJson(json, ShadertoyProject::class.java)

        return project
    }

    /**
     *
     */
    fun fromResourceDirectory(projectName: String, dirPath: String): ShadertoyProject {

        val project = ShadertoyProject(projectName)

        // Load in shader tabs
        for (f in FileType.values()) {
            val content = resourceTextOrNull("$dirPath/${f.fileName}") ?: continue

            when (f) {
                FileType.COMMON_GLSL   -> project.common = Common()  .setCode(content)
                FileType.IMAGE_GLSL    -> project.image = Image()    .setCode(content)
                FileType.BUFFER_A_GLSL -> project.bufferA = BufferA().setCode(content)
                FileType.BUFFER_B_GLSL -> project.bufferB = BufferB().setCode(content)
                FileType.BUFFER_C_GLSL -> project.bufferC = BufferC().setCode(content)
                FileType.BUFFER_D_GLSL -> project.bufferD = BufferD().setCode(content)
                FileType.CUBE_A_GLSL   -> project.cubeA = CubeA()    .setCode(content)
                FileType.SOUND_GLSL    -> project.sound = Sound()    .setCode(content)
            }
        }

        return project
    }

    /**
     *
     */
    fun fromUrl(): ShadertoyProject {
        TODO()
    }

    companion object {

        fun resourceTextOrNull(path: String): String? {
            return try { resourceText(path) } catch (e: Exception) { null }
        }
    }
}

/**
 *
 */
enum class FileType(val fileName: String) {
    COMMON_GLSL("common.glsl"),
    IMAGE_GLSL("image.glsl"),
    BUFFER_A_GLSL("bufferA.glsl"),
    BUFFER_B_GLSL("bufferB.glsl"),
    BUFFER_C_GLSL("bufferC.glsl"),
    BUFFER_D_GLSL("bufferD.glsl"),
    CUBE_A_GLSL("cubeA.glsl"),
    SOUND_GLSL("sound.glsl");

    companion object {

        /**
         *
         */
        fun ShadertoyTab.toFileName(): String {
            return when (this) {
                is Common -> COMMON_GLSL.fileName
                is Image -> IMAGE_GLSL.fileName
                is BufferA -> BUFFER_A_GLSL.fileName
                is BufferB -> BUFFER_B_GLSL.fileName
                is BufferC -> BUFFER_C_GLSL.fileName
                is BufferD -> BUFFER_D_GLSL.fileName
                is CubeA -> CUBE_A_GLSL.fileName
                is Sound -> SOUND_GLSL.fileName
            }
        }
    }
}