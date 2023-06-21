@file:Suppress("ClassName", "unused")

package flow.shadertoy

import flow.shadertoy.ShadertoyProject.ShadertoyTab
import flow.shadertoy.ShadertoyProject.ShadertoyTab.*
import org.openrndr.resourceText

/**
 *
 */
class Importer(val projectName: String) {

    /**
     *
     */
    fun fromResourceDirectory(dirPath: String): ShadertoyProject {

        val project = ShadertoyProject(projectName)

        // Load in shader tabs
        for (f in FileType.values()) {
            val content = resourceTextOrNull("$dirPath/${f.fileName}") ?: continue

            when (f) {
                FileType.COMMON_GLSL   -> project.common = Common().apply { code = content }
                FileType.IMAGE_GLSL    -> project.image = Image().apply { code = content }
                FileType.BUFFER_A_GLSL -> project.bufferA = BufferA().apply { code = content }
                FileType.BUFFER_B_GLSL -> project.bufferB = BufferB().apply { code = content }
                FileType.BUFFER_C_GLSL -> project.bufferC = BufferC().apply { code = content }
                FileType.BUFFER_D_GLSL -> project.bufferD = BufferD().apply { code = content }
                FileType.CUBE_A_GLSL   -> project.cubeA = CubeA().apply { code = content }
                FileType.SOUND_GLSL    -> project.sound = Sound().apply { code = content }
            }
        }

        // TODO: Load in project settings from JSON file

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