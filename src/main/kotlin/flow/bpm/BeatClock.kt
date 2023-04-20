package flow.bpm

import flow.envelope.EnvelopeBuilder
import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.Drawer
import org.openrndr.math.map

// TODO: make transitions not interpolating through potentially
//  large phase diff from org phase to target phase
/**
 * BeatClock repository.
 *
 * Start a builder with `val beatClock = extend(BeatClock()) { … }` to configure settings.
 * Use [phase] to query the relative phase since program start
 * or use [bindEnvelope] to access a [EnvelopeBuilder].
 *
 * Cross-fade to a new bpm rate with [animateTo].
 * For example `animateTo(bpm = 132.0, 1.0)` to start the new beat on keystroke "k",
 * gradually changing the phase and speed to 132.0 over 1.0 second.
 *
 * @param bpm The initial bpm.
 */
class BeatClock(var bpm: Double = 120.0) : Extension {

    override var enabled = true

    /**
     * Current phase. Recalculated on every frame.
     */
    var phase = 0.0
        private set

    /**
     * Time stamp where the current beat started
     */
    var startedAt = 0.0
        private set

    /**
     * Should a transition take place, this will be non-null data holder.
     */
    var transition: Transition? = null

    /**
     * Data holder for a transition.
     */
    data class Transition(
        val targetBpm: Double,
        val targetStartedAt: Double,
        var targetEndAt: Double
    )

    /**
     * List of all samplers.
     */
    private val samplerList = mutableListOf<Sampler>()

    /**
     * Update the phase. Should a transition be present, update the remaining time.
     */
    override fun beforeDraw(drawer: Drawer, program: Program) {
        // Update the phase
        val now = program.seconds
        phase = (now - startedAt) * bpm / 60.0
        var updatePhase = phase

        // Should a transition exist, perform interpolation and consume it if it's done.
        transition?.run {
            val targetPhase = (now - targetStartedAt) * targetBpm / 60.0
            val transitionProgres = now.map(targetStartedAt, targetEndAt, 0.0, 1.0)
            updatePhase = updatePhase * (1.0 - transitionProgres) + targetPhase * transitionProgres

            if (targetEndAt >= now) {
                bpm = targetBpm
                startedAt = targetStartedAt
                transition = null
            }
        }

        // Update all samplers with the new phase
        samplerList.forEach { it.update(updatePhase) }
    }

    /**
     * Bind an envelope to the beat clock.
     * @return A [Sampler] to access the envelope value. Goes well with delegation.
     */
    fun bindEnvelope(eval: (phase: Double) -> Double): Sampler {
        val sampler = Sampler(eval)
        samplerList.add(sampler)
        return sampler
    }

    /**
     * Animate to a new bpm rate.
     *
     * @param bpm The new bpm rate.
     * @param duration The duration of the transition.
     */
    fun animateTo(bpm: Double, duration: Double) {
        transition = Transition(
            targetBpm = bpm,
            targetStartedAt = startedAt,
            targetEndAt = startedAt + duration
        )
    }

    /**
     * Reset the beat clock. Starts at [now].
     */
    fun resetTime(now: Double) {
        startedAt = now
    }
}

/**
 * Convert a Double to an Int representing the number of completed [interval]s it contains.
 */
fun Double.toIntervalCount(interval: Double): Int = (this/interval).toInt()