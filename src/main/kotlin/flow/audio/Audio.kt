@file:Suppress("unused")

package flow.audio

import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.filters.BandPass
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory
import flow.FlowProgram
import flow.realtime.oneEuroFilter.OneEuroFilter
import kotlinx.coroutines.*
import org.openrndr.math.map
import kotlin.math.log10
import kotlin.reflect.KProperty

/**
 * Audio extension.
 *
 * Start with `val audio = Audio(flowProgram)` and create processors using your system's default microphone.
 *
 * Performs audio processing (called 'Digital Signal Processing' or 'DSP')
 * with [TarsosDSP](https://github.com/JorenSix/TarsosDSP).
 *
 * @param bufferSize Size of the audio buffer. Default is 1024. Larger buffers are more accurate, but increase lag.
 * @param overlap Size of the overlap between audio buffers. Default is 0.
 * @param sampleRate Sample rate of the audio. Default is 44100. **Deviations from system device will cause problems.**
 */
abstract class Audio(
    flowProgram: FlowProgram,
    val bufferSize: Int = 1024,
    val overlap: Int = 0,
    val sampleRate: Int = 44100,
) {

    var isStarted = false
        private set

    // Actual dispatcher running the audio process chain
    private lateinit var dispatcher: AudioDispatcher

    // Audio thread. Sometimes a little bitch (not closing audio stream).
    private lateinit var audioThread: Thread

    init {
        flowProgram.registerOnExit { this.stop() }
    }

    /**
     * Does setup of immutable audio chain and starts digital signal processing (DSP) on a separate thread.
     *
     * Don't forget to gracefully terminate with [stop].
     */
    fun start() {
        if (isStarted) return
        else isStarted = true

        // Setup
        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(sampleRate, bufferSize, overlap)

        val ap = object: AudioProcessor {
            override fun process(audioEvent: AudioEvent?): Boolean {
                setProcess(audioEvent!!, (bufferSize - overlap) / sampleRate.toDouble())
                return true
            }

            override fun processingFinished() {}
        }

        dispatcher.addAudioProcessor(ap)
        audioThread = Thread(dispatcher)
        audioThread.start()
    }

    /**
     * Stops the audio chain and (attempts to) gracefully terminate the DSP process.
     *
     * Only makes sense after [start].
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

    /**
     * Specify the process to perform everytime a new audio event is received.
     * @param audioEvent The audio event received from the audio stream.
     * @param dt The time between two audio events in seconds.
     */
    abstract fun setProcess(audioEvent: AudioEvent, dt: Double)
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

/**
 * Converts a magnitude value (usually from FFTs) to decibels. See [Double.toDb].
 */
fun Float.toDb(): Double = this.toDouble().toDb()

/**
 * Convert [this] sound pressure level (i.e. volume in Db) to relative volume (in range 0.0 .. 1.0).
 */
fun Double.toRelativeVolume(): Double = this.map(Audio.LOWEST_SPL .. Audio.HIGHEST_SPL, 0.0 .. 1.0)

/**
 * Delegation for [OneEuroFilter] to get its current value.
 */
operator fun OneEuroFilter.getValue(nothing: Any?, property: KProperty<*>): Double {
    return this.value
}

/**
 * Creates a [BandPass] filter with the given [range] and [sampleRate].
 */
fun createBandPass(range: ClosedFloatingPointRange<Double>, sampleRate: Double): BandPass {
    val mid = (range.start + range.endInclusive) / 2
    val bandWidth = range.endInclusive - range.start
    return BandPass(mid.toFloat(), bandWidth.toFloat(), sampleRate.toFloat())
}