@file:Suppress("unused")

package flow.audio

import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory
import kotlinx.coroutines.*

/**
 * Audio extension.
 *
 * Start with `val audio = Audio().apply { … }` and configure your audio extension.
 * Uses the system audio device in combination with TarsosDSP.
 */
class Audio {

    var bufferSize = 1024
    var overlap = 0
    var sampleRate = 44100
    private lateinit var dispatcher: AudioDispatcher

    private val audioCloneList = mutableListOf<AudioProcessor>()
    private lateinit var resetProcessorPair: ResetProcessorPair

    private lateinit var audioThread: Thread

    // Runtime setup
    fun run() {
        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(sampleRate, bufferSize, overlap)
        resetProcessorPair = ResetProcessorPair(bufferSize)

        dispatcher.addAudioProcessor(resetProcessorPair.read)
        audioCloneList.forEach {
            dispatcher.addAudioProcessor(it)
            dispatcher.addAudioProcessor(resetProcessorPair.write)
        }
        audioThread = Thread(dispatcher)
        audioThread.start()
    }

    fun stop() {
        dispatcher.stop()
        runBlocking {
            delay(1000L)
            if (audioThread.isAlive) audioThread.interrupt()
        }
        try {
            audioThread.join(1000L)
        } catch (e: InterruptedException) {
            println("Audio thread interrupted. Didn't join gracefully.")
        }
    }

    // Audio processors
    fun createVolumeProcessor(eventBufferSize: Int = 40): VolumeProcessor {
        return VolumeProcessor(eventBufferSize, sampleRate).also { audioCloneList.add(it) }
    }

    fun createConstantQProcessor(
        rangeList: List<ClosedFloatingPointRange<Double>>,
        eventBufferSize: Int = 40,
        binsPerOctave: Int,
    ): ConstantQProcessor {
        return ConstantQProcessor(binsPerOctave, rangeList, sampleRate, eventBufferSize).also {
            audioCloneList.add(it)
        }
    }

    // Common values across all audio processors
    companion object {
        // Human hearing range in Hz
        const val LOWEST_FQ = 20.0
        const val HIGHEST_FQ = 20000.0

        // Acoustic fine ranges
        // Source: https://housegrail.com/bass-treble-hertz-frequency-chart/
        val BASS = 20.0 .. 160.0
        val LOW_MID = 160.0 .. 320.0
        val MID = 320.0 .. 2500.0
        val TREBLE = 2500.0 .. 5000.0
        val BRILLIANCE = 5000.0 .. 20000.0

        // Milkdrop style ranges
        val MD_BASS = 20.0 .. 320.0
        val MD_MID = 320.0 .. 2500.0
        val MD_TREBLE = 2500.0 .. 20000.0

        // Volume range as Sound Pressure Level (SPL) in decibel
        const val LOWEST_SPL = -160.0
        const val HIGHEST_SPL = 0.0
    }

}


