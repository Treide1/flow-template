package flow.audio

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.util.fft.FFT

class FftProcessor(
    val bufferSize: Int
): AudioProcessor {

    val fft = FFT(bufferSize * 2)
    var magnitudes = FloatArray(bufferSize)

    override fun process(audioEvent: AudioEvent?): Boolean {

        val clone = audioEvent!!.floatBuffer.clone()
        fft.forwardTransform(clone)
        for(i in magnitudes.indices) {
            magnitudes[i] = clone[2*i] * clone[2*i] + clone[2*i + 1] * clone[2*i + 1]
        }

        return true
    }

    override fun processingFinished() {}
}