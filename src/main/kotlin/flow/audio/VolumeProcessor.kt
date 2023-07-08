@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package flow.audio

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import flow.audio.Audio.Companion.HIGHEST_SPL
import flow.audio.Audio.Companion.LOWEST_SPL
import org.openrndr.math.clamp

/**
 * Audio processor that calculates the decibel value of the signal.
 * Allows for registering listeners that are called when the volume is processed.
 */
class VolumeProcessor: AudioProcessor {

    var volume: Double = 0.0
        private set

    /**
     * Processes any incoming audio event.
     */
    override fun process(audioEvent: AudioEvent): Boolean {
        // Calculate the volume and pass it to the listeners
        volume = audioEvent.getdBSPL().clamp(LOWEST_SPL, HIGHEST_SPL)

        // Return true to signal that this operation was "ok"
        return true
    }

    override fun processingFinished() {}
}