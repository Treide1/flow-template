@file:Suppress("unused")

package flow.audio

import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory
import kotlinx.coroutines.*
import kotlin.math.log10

/**
 * Audio extension.
 *
 * Start with `val audio = Audio()` and create processors using your system's default microphone.
 *
 * Performs audio processing (called 'Digital Signal Processing' or 'DSP')
 * with [TarsosDSP](https://github.com/JorenSix/TarsosDSP).
 *
 * @param bufferSize Size of the audio buffer. Default is 1024. Larger buffers are more accurate, but increase lag.
 * @param overlap Size of the overlap between audio buffers. Default is 0.
 * @param sampleRate Sample rate of the audio. Default is 44100. Deviations from system device *will* cause problems.
 */
// TODO: Refactor to make more clear + provide sampleRate/bufferSize resolution strategy
class Audio(
    val bufferSize: Int = 1024,
    val overlap: Int = 0,
    val sampleRate: Int = 44100,
) {

    var isStarted = false
        private set

    // Actual dispatcher running the audio process chain
    private lateinit var dispatcher: AudioDispatcher

    // List of processors for which the audio events should be cloned
    private val audioCloneList = mutableListOf<AudioProcessor>()
    // Processor pair for cloning audio events
    private lateinit var resetProcessorPair: ResetProcessorPair
    // Audio thread. Sometimes a little bitch (not closing audio stream).
    private lateinit var audioThread: Thread

    /**
     * Does setup of immutable audio chain and starts digital signal processing (DSP) on a separate thread.
     *
     * Gracefully terminate with [stop].
     */
    fun start() {
        if (isStarted) return
        else isStarted = true

        // Setup
        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(sampleRate, bufferSize, overlap)
        resetProcessorPair = ResetProcessorPair(bufferSize)

        // Cloning and processing procedure
        // - Clone the raw audio event
        // - For each processor, process the audio event. Then clone it back to the current audioEvent.
        // This is necessary, as some processors (namely IIRFilters) alter the audio data. This serves as a rollback.
        dispatcher.addAudioProcessor(resetProcessorPair.read)
        audioCloneList.forEach {
            dispatcher.addAudioProcessor(it)
            dispatcher.addAudioProcessor(resetProcessorPair.write)
        }
        audioThread = Thread(dispatcher)
        audioThread.start()
    }

    /**
     * Stops the audio chain and (attempts to) gracefully terminate the DSP process. Only makes sense after [start].
     */
    fun stop() {
        if (isStarted.not()) return
        else isStarted = false

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

    /**
     * Creates a [VolumeProcessor] and adds it to the audio chain.
     */
    fun createVolumeProcessor(): VolumeProcessor {
        return VolumeProcessor().also { audioCloneList.add(it) }
    }

    /**
     * Creates a [ConstantQProcessor] and adds it to the audio chain.
     */
    fun createConstantQProcessor(
        binsPerOctave: Int,
        rangeList: List<ClosedFloatingPointRange<Double>>,
        eventBufferSize: Int = 40,
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
        val DEFAULT_RANGES = listOf(BASS, LOW_MID, MID, TREBLE, BRILLIANCE)

        // Milkdrop style ranges
        val MD_BASS = 20.0 .. 320.0
        val MD_MID = 320.0 .. 2500.0
        val MD_TREBLE = 2500.0 .. 20000.0
        val MILKDROP_RANGES = listOf(MD_BASS, MD_MID, MD_TREBLE)

        // Volume range as Sound Pressure Level (SPL) in decibel
        const val LOWEST_SPL = -160.0
        const val HIGHEST_SPL = 0.0
    }

}

/**
 * Converts a magnitude value (usually from FFTs) to decibels.
 *
 * Coerced to be at least [Audio.LOWEST_SPL].
 * Should the value be more than [Audio.HIGHEST_SPL], you are doing something wrong anyway.
 */
fun Double.toDb(): Double {
    if (this <= 0) return Audio.LOWEST_SPL
    return (20 * log10(this)).coerceAtLeast(Audio.LOWEST_SPL)
}
