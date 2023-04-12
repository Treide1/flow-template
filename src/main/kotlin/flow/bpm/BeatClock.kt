package flow.bpm

import flow.envelope.EnvelopeBuilder
import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.Drawer

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
     * Current phase.
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

    data class Transition(
        val targetBpm: Double,
        val targetStartedAt: Double,
        var remainingTime: Double
    )

    /**
     * List of all samplers.
     */
    private val samplerList = mutableListOf<Sampler>()

    /**
     * Update the phase. Should a transition be present, update the remaining time.
     */
    override fun beforeDraw(drawer: Drawer, program: Program) {

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
     * WIP: Bind an envelope to the beat clock.
     */
    /*
    fun bindEnvelope(config: EnvelopeBuilder.() -> Unit): Sampler {
        val builder = EnvelopeBuilder()
        builder.config()
        return builder.build()
    }
     */

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
            remainingTime = duration
        )
    }

    // TODO: Set startedAt to current time
    fun reset() {

    }
}