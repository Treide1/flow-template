package flow.audio

import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory
import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.Drawer

/**
 * Audio extension.
 *
 * Start with `val audio = extend(Audio()) { … }` and configure your audio extension.
 * Uses the system audio device in combination with TarsosDSP.
 */
class Audio: Extension {

    override var enabled = true

    var bufferSize = 1024
    var overlap = 0
    var sampleRate = 44100
    private lateinit var dispatcher: AudioDispatcher


    var ranges = listOf<ClosedFloatingPointRange<Double>>() // Ranges in Hz



    override fun setup(program: Program) {
        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(sampleRate, bufferSize, overlap)
    }

    override fun beforeDraw(drawer: Drawer, program: Program) {

    }

    fun buildVolumeProcessor(): VolumeProcessor {
        return VolumeProcessor(bufferSize)
    }

    companion object {
        const val LOWEST_FQ = 20.0
        const val HIGHEST_FQ = 20000.0

        val BASS = 20.0 .. 160.0
        val LOW_MID = 160.0 .. 320.0
        val MID = 320.0 .. 2000.0
        val TREBLE = 2000.0 .. 5000.0
        val BRILLIANCE = 5000.0 .. 20000.0
    }

}


