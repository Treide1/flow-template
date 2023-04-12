package flow.audio

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import org.openrndr.math.clamp

class VolumeProcessor internal constructor(val eventBufferSize: Int): AudioProcessor {

    // Buffer to store the last decibel values
    private val _volumeBuffer = mutableListOf<Double>()
    val volumeBuffer: List<Double>
        get() = _volumeBuffer.toList()

    // Dynamic range values
    var loRange = LOWEST_RANGE
    var hiRange = HIGHEST_RANGE
    var rangeContraction = 0.98

    // Buffer to store dynamic range, as it changes over time
    private val _rangeBuffer = mutableListOf<ClosedFloatingPointRange<Double>>()
    val rangeBuffer: List<ClosedFloatingPointRange<Double>>
        get() = _rangeBuffer.toList()

    override fun process(audioEvent: AudioEvent): Boolean {
        // Add the decibel value to the volume buffer
        val db = audioEvent.getdBSPL().clamp(LOWEST_RANGE, HIGHEST_RANGE)
        _volumeBuffer.add(db)
        // If the buffer is full, remove the oldest value
        if (_volumeBuffer.size > eventBufferSize) {
            _volumeBuffer.removeAt(0)
        }
        // Update the dynamic range
        // The range is contracted by a factor of rangeContraction, or expanded by the new value
        loRange = if (db < loRange) db else loRange * rangeContraction + db * (1 - rangeContraction)
        hiRange = if (db > hiRange) db else hiRange * rangeContraction + db * (1 - rangeContraction)
        // Add the range to the range buffer
        _rangeBuffer.add(loRange..hiRange)
        // If the buffer is full, remove the oldest value
        if (_rangeBuffer.size > eventBufferSize) {
            _rangeBuffer.removeAt(0)
        }

        // Return true to signal that this operation was "ok"
        return true
    }

    override fun processingFinished() {}

    companion object {
        const val LOWEST_RANGE = -160.0
        const val HIGHEST_RANGE = 0.0
    }
}