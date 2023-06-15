package flow.audio

import ddf.minim.Minim
import ddf.minim.ugens.FilePlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

/**
 * Audio player for a single file.
 *
 * Note: The audio format of the file is crucial.
 * As of now, the file must be a 16-bit Little-Endian, 44.1 kHz, stereo WAV file.
 */
class AudioPlayer(val filename: String, val length: Double) {

    /**
     * The audio file must be in the data/audio folder.
     */
    var filePlayer: FilePlayer? = null

    /**
     * The current audio playback position in seconds.
     */
    val seconds: Double
        get() = filePlayer?.run {
            position().toDouble() / length() * length
        } ?: -1.0

    /**
     * Starts the audio playback. (Uses Minim's UGen FilePlayer.)
     */
    fun start(offset: Double = 0.0) {
        if (offset < 0) {
            runBlocking {
                var remaining = (-offset*1000).toLong()
                while (remaining > 0L) {
                    remaining -= 200L
                    delay(200L)
                }
            }
        }
        try {
            val minim = Minim( object : Any() {
                fun sketchPath(fileName: String): String {
                    return fileName
                }
                fun createInput(fileName: String): InputStream {
                    return FileInputStream(File(fileName))
                }
            })
            filePlayer = FilePlayer(minim.loadFileStream(filename))

            filePlayer!!.patch(minim.lineOut)

            filePlayer!!.play()
            filePlayer!!.cue((offset * 1000).coerceAtLeast(0.0).toInt())
        }
        catch (e: Exception) {
            println("Failed to load audio file: $filename")
        }
    }

    /**
     * Stops the audio playback.
     */
    fun stop() {
        filePlayer?.pause()
        filePlayer?.close()
        filePlayer = null
    }
}