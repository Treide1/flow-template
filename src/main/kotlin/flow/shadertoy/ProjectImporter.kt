@file:Suppress("ClassName", "unused")

package flow.shadertoy

import com.google.gson.GsonBuilder
import flow.shadertoy.ShadertoyProject.ShadertoyTab
import flow.shadertoy.ShadertoyProject.ShadertoyTab.*
import java.io.File

/**
 * Class for importing [ShadertoyProject]s from different sources.
 */
object ProjectImporter {

    /**
     * The path to the folder that contains the shadertoy projects.
     */
    val projectsPath = "src/main/kotlin/flow/shadertoy/projects"

    /**
     * Import a project from a directory that contains a project.json file.
     * @param dirPath The directory that contains the project.json file.
     * @param reloadCode If true, the GLSL code is reloaded from the files in the directory.
     */
    fun importFromJson(dirPath: String, reloadCode: Boolean = false): ShadertoyProject {
        val json = readTextOrNull("$dirPath/project.json")
            ?: throw IllegalArgumentException("No project.json found in '$dirPath'")

        val project = GsonBuilder()
            .registerTypeAdapter(ShadertoyProject::class.java, JsonToProject())
            .create()
            .fromJson(json, ShadertoyProject::class.java)

        if (reloadCode) project.loadAllGlslFiles(dirPath)

        return project
    }

    /**
     * Import a project from a directory that contains glsl files.
     * @param projectName The name of the project. This will be included in the project.json under the `name` field.
     * @param dirPath The directory that contains the glsl files.
     * @throws IllegalArgumentException If the directory does not exist or does not contain an `image.glsl` file.
     */
    fun import(projectName: String, dirPath: String): ShadertoyProject {

        val folder = File(dirPath)
        val imageGlslFile = File("$dirPath/image.glsl")

        // Check if the directory exists
        if (!folder.exists()) throw IllegalArgumentException("Directory '$dirPath' does not exist")
        // Check if "image.glsl" is in that folder
        if (!imageGlslFile.exists()) throw IllegalArgumentException("Directory '$dirPath' does not contain an 'image.glsl' file")

        val project = ShadertoyProject(projectName)
        project.loadAllGlslFiles(dirPath)

        return project
    }

    /**
     * Given the project from [projectPath] with a [projectName], imports a project without input bindings.
     * The specified [setup] step is applied onto it and new project files are generated.
     * Those are imported and returned.
     */
    fun buildAndImport(projectPath: String, projectName: String, setup: ShadertoyProject.() -> Unit): ShadertoyProject {
        val original = import(projectName, projectPath)
        original.setup()

        val builder = GlslFileBuilder(original, projectPath)
        builder.generate()

        return importFromJson("$projectPath/generated", true)
    }

    /**
     * Import a project from the shadertoy API.
     *
     * The resulting project stills needs to be converted to glsl files.
     */
    fun importFromApi(): ShadertoyProject {
        TODO()
    }

    /**
     * Loads all present glsl files from a directory and sets the code of the project accordingly.
     *
     * This requires the files to be named like the [FileType] enum.
     * Example: `common.glsl`, `image.glsl`, `bufferA.glsl`, ...
     */
    private fun ShadertoyProject.loadAllGlslFiles(dirPath: String) {
        for (f in FileType.values()) {
            val content = readTextOrNull("$dirPath/${f.fileName}") ?: continue
            this.setCodeByFileType(content, f)
        }
    }

    // Sets the code of the project according to the file type
    private fun ShadertoyProject.setCodeByFileType(content: String, fileType: FileType) {
        when (fileType) {
            FileType.COMMON_GLSL   -> common =  (common ?: Common())  .setCode(content)
            FileType.IMAGE_GLSL    -> image =    image                .setCode(content)
            FileType.BUFFER_A_GLSL -> bufferA = (bufferA ?: BufferA()).setCode(content)
            FileType.BUFFER_B_GLSL -> bufferB = (bufferB ?: BufferB()).setCode(content)
            FileType.BUFFER_C_GLSL -> bufferC = (bufferC ?: BufferC()).setCode(content)
            FileType.BUFFER_D_GLSL -> bufferD = (bufferD ?: BufferD()).setCode(content)
            FileType.CUBE_A_GLSL   -> cubeA =   (cubeA ?: CubeA())    .setCode(content)
            FileType.SOUND_GLSL    -> sound =   (sound ?: Sound())    .setCode(content)
        }
    }

    // Reads the resource if it exists, returns null otherwise
    private fun readTextOrNull(path: String): String? {
        return try {
            File(path).readText()
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Enum for the different glsl file types.
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
         * Returns the file name for a [ShadertoyTab].
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