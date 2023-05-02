package flow.audio

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor

class ResetProcessorPair(bufferSize: Int) {

    var audioFloatArray = FloatArray(bufferSize)

    val read = object: AudioProcessor {
        override fun process(audioEvent: AudioEvent): Boolean {
            audioFloatArray = audioEvent.floatBuffer.clone()
            return true
        }

        override fun processingFinished() {}
    }

    val write = object: AudioProcessor {
        override fun process(audioEvent: AudioEvent): Boolean {
            audioEvent.floatBuffer = audioFloatArray.clone()
            return true
        }

        override fun processingFinished() {}
    }
}
