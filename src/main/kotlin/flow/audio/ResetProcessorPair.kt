package flow.audio

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor

/**
 * Audio processor pair that clones the incoming audio event's float buffer during [read] and
 * clones the stored float buffer back during [write].
 */
class ResetProcessorPair(bufferSize: Int) {

    /**
     * The cloned audio float buffer.
     */
    var audioFloatArray = FloatArray(bufferSize)

    /**
     * Reader of audio event's float buffer.
     */
    val read = object: AudioProcessor {
        override fun process(audioEvent: AudioEvent): Boolean {
            audioFloatArray = audioEvent.floatBuffer.clone()
            return true
        }

        override fun processingFinished() {}
    }

    /**
     * Writer of audio event's float buffer.
     */
    val write = object: AudioProcessor {
        override fun process(audioEvent: AudioEvent): Boolean {
            audioEvent.floatBuffer = audioFloatArray.clone()
            return true
        }

        override fun processingFinished() {}
    }
}
