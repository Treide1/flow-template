@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package flow.audio

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import flow.audio.Audio.Companion.HIGHEST_SPL
import flow.audio.Audio.Companion.LOWEST_SPL
import flow.realtime.filters.OneEuroFilter
import org.openrndr.math.clamp

/**
 * Audio processor that calculates the decibel value of the signal.
 * Allows for registering listeners that are called when the volume is processed.
 */
class VolumeProcessor internal constructor(): AudioProcessor {

    var volume: Double = 0.0
        private set

    val onProcessedVolumeListeners = mutableListOf<(volume: Double) -> Unit>()

    fun onProcessedVolume(listener: (volume: Double) -> Unit) {
        onProcessedVolumeListeners.add(listener)
    }

    /**
     * Processes any incoming audio event.
     */
    override fun process(audioEvent: AudioEvent): Boolean {
        // Calculate the volume and pass it to the listeners
        volume = audioEvent.getdBSPL().clamp(LOWEST_SPL, HIGHEST_SPL)
        onProcessedVolumeListeners.forEach { listener ->
            listener(volume)
        }

        // Return true to signal that this operation was "ok"
        return true
    }

    override fun processingFinished() {}

    fun createSmoothingFilter() = OneEuroFilter(minCutoff = 1.0, beta = 0.01, dCutoff = 1.0)
}