package flow.audio

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.util.fft.FFT
import be.tarsos.dsp.util.fft.HammingWindow
import kotlin.math.sqrt

/**
 * An [AudioProcessor] that performs a forward FFT on incoming audio.
 */
class FftProcessor(
    bufferSize: Int
): AudioProcessor {

    /**
     * The [FFT] running the wave-to-frequency transformation.
     */
    val fft = FFT(bufferSize * 2, HammingWindow())

    /**
     * The magnitudes of the frequencies.
     */
    var magnitudes = FloatArray(bufferSize)

    override fun process(audioEvent: AudioEvent?): Boolean {
        // Clone the buffer and work on it to preserve the original
        val clone = audioEvent!!.floatBuffer.clone()
        fft.forwardTransform(clone)
        for(i in magnitudes.indices) {
            // Calculates the length of the complex number at index i
            // Real and imaginary parts are 2i and 2i+1 (special case clone[1] after fft ignored)
            magnitudes[i] = sqrt( clone[2*i] * clone[2*i] + clone[2*i + 1] * clone[2*i + 1] )
        }

        return true
    }

    override fun processingFinished() {}
}