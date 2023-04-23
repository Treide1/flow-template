@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package flow.audio

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import flow.audio.Audio.Companion.HIGHEST_SPL
import flow.audio.Audio.Companion.LOWEST_SPL
import org.openrndr.math.clamp

/**
 * Audio processor that calculates the decibel value of the signal.
 * Also calculates the dynamic range of the signal, which is updated with each event.
 * Stores the last [eventBufferSize] values in [volumeBuffer] and [dynRangeBuffer].
 *
 * @param eventBufferSize The size of the buffer that stores the decibel values.
 */
// TODO: refactor to be event rate independent (see per-event rangeContraction)
class VolumeProcessor internal constructor(val eventBufferSize: Int): AudioProcessor {

    // Buffer to store the last decibel values
    private val _volumeBuffer = mutableListOf<Double>()

    /**
     * The last [eventBufferSize] decibel values.
     */
    val volumeBuffer: List<Double>
        get() = _volumeBuffer.toList()

    // Dynamic range values
    private var loLevel = LOWEST_SPL
    private var hiLevel = HIGHEST_SPL

    /**
     * The dynamic range update factor.
     */
    var rangeContraction = 0.98

    // Buffer to store dynamic range, as it changes over time
    private val _dynRangeBuffer = mutableListOf<ClosedFloatingPointRange<Double>>()
    val dynRangeBuffer: List<ClosedFloatingPointRange<Double>>
        get() = _dynRangeBuffer.toList()

    /**
     * Processes any incoming audio event.
     */
    override fun process(audioEvent: AudioEvent): Boolean {
        // Add the decibel value to the volume buffer
        val db = audioEvent.getdBSPL().clamp(LOWEST_SPL, HIGHEST_SPL)
        _volumeBuffer.add(db)
        // If the buffer is full, remove the oldest value
        if (_volumeBuffer.size > eventBufferSize) {
            _volumeBuffer.removeAt(0)
        }
        // Update the dynamic range
        // The range is contracted by a factor of rangeContraction, or expanded by the new value
        loLevel = if (db < loLevel) db else loLevel * rangeContraction + db * (1 - rangeContraction)
        hiLevel = if (db > hiLevel) db else hiLevel * rangeContraction + db * (1 - rangeContraction)
        // Add the range to the range buffer
        _dynRangeBuffer.add(loLevel..hiLevel)
        // If the buffer is full, remove the oldest value
        if (_dynRangeBuffer.size > eventBufferSize) {
            _dynRangeBuffer.removeAt(0)
        }

        // Return true to signal that this operation was "ok"
        return true
    }

    override fun processingFinished() {}

}