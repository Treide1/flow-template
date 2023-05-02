@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package flow.audio

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import flow.audio.Audio.Companion.HIGHEST_SPL
import flow.audio.Audio.Companion.LOWEST_SPL
import flow.realtime.filters.OneEuroFilter
import org.openrndr.math.clamp
import org.openrndr.math.map
import util.QueueCache

/**
 * Audio processor that calculates the decibel value of the signal.
 * Also calculates the dynamic range of the signal, which is updated with each event.
 * Stores the last [eventBufferSize] values in [volumeBuffer] and [dynRangeBuffer].
 *
 * @param eventBufferSize The size of the buffer that stores the decibel values.
 */
class VolumeProcessor internal constructor(
    val eventBufferSize: Int,
    val sampleRate: Int,
): AudioProcessor {

    // Cache to store the last decibel values
    private val volumeCache = QueueCache<Double>(eventBufferSize)
    private val dynRangeCache = QueueCache<ClosedFloatingPointRange<Double>>(eventBufferSize)

    /**
     * The last decibel values. Size is [eventBufferSize]. Latest event is at the end of the list.
     */
    val volumeBuffer by volumeCache

    val volumeFilter = OneEuroFilter(1.0, 0.01, 1.0, 0.0)
    var filteredLastVolume = 0.0

    // Dynamic volume range values
    var loLevel = LOWEST_SPL
    var hiLevel = HIGHEST_SPL

    /**
     * The last dynamic ranges. Buffer size is [eventBufferSize]. Latest event is at the end of the list.
     */
    val dynRangeBuffer by dynRangeCache

    // TODO: refactor contraction to be event rate independent, currently updates by factor every event
    /**
     * The update factor for dynamic volume range.
     */
    var rangeContraction = 0.98

    /**
     * Processes any incoming audio event.
     */
    override fun process(audioEvent: AudioEvent): Boolean {
        // Add the decibel value to the volume buffer
        val db = audioEvent.getdBSPL().clamp(LOWEST_SPL, HIGHEST_SPL)
        volumeCache.add(db)
        val y = db.map(LOWEST_SPL, HIGHEST_SPL, 0.0, 1.0)
        filteredLastVolume = volumeFilter.filter(y, 0.01)

        // Update the dynamic range
        // The range is contracted by a factor of rangeContraction, or expanded by the new value
        loLevel = if (db < loLevel) db else loLevel * rangeContraction + db * (1 - rangeContraction)
        hiLevel = if (db > hiLevel) db else hiLevel * rangeContraction + db * (1 - rangeContraction)
        // Add the dynRange to the dynRange buffer
        dynRangeCache.add(loLevel..hiLevel)

        // Return true to signal that this operation was "ok"
        return true
    }

    override fun processingFinished() {}

}