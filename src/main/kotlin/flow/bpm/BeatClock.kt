@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package flow.bpm

import flow.bpm.envelope.Envelope
import flow.bpm.envelope.EnvelopeBuilder
import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.Drawer
import org.openrndr.math.map

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
 * @param bpm The initial bpm. Can be modified during runtime, e.g. during a playlist run.
 */
class BeatClock(var bpm: Double) : Extension {

    override var enabled = true

    /**
     * Current phase. Recalculated on every frame.
     */
    var phase = 0.0
        private set

    /**
     * Time stamp where the beat counting started. This is treated like t = 0.
     */
    var t0 = 0.0
        private set

    /**
     * Time stamp of the last frame.
     */
    var lastNow = 0.0
        private set

    /**
     * Should a transition take place, this will be non-null data holder.
     */
    var transition: Transition? = null
        private set

    /**
     * Data holder for a transition.
     */
    data class Transition(
        val targetBpm: Double,
        val targetT0: Double,
        val transitionBegin: Double,
        val transitionEnd: Double
    )

    /**
     * List of all samplers.
     */
    private val samplerList = mutableListOf<Sampler>()

    /**
     * Update the phase. Should a transition be present, update the remaining time.
     */
    override fun beforeDraw(drawer: Drawer, program: Program) {
        // Update the 'now' time stamp
        val now = program.seconds
        lastNow = now

        // Calculate the phase
        phase = (now - t0) * bpm / 60.0
        var updatePhase = phase

        // Should a transition exist, perform interpolation and consume it if it's done.
        transition?.run {
            val targetPhase = (now - targetT0) * targetBpm / 60.0
            val transitionProgress = now.map(transitionBegin, transitionEnd, 0.0, 1.0)
            updatePhase = updatePhase * (1.0 - transitionProgress) + targetPhase * transitionProgress

            if (transitionEnd >= now) {
                bpm = targetBpm
                t0 = targetT0
                transition = null
            }
        }

        // Update all samplers with the new phase
        samplerList.forEach { it.update(updatePhase) }
    }

    /**
     * Creates a [Sampler] with the an [Envelope] to the beat clock.
     */
    fun bindEnvelope(length: Double = 4.0, eval: (phase: Double) -> Double): Sampler {
        val sampler = Sampler(Envelope(length, eval))
        samplerList.add(sampler)
        return sampler
    }

    /**
     * Creates a [Sampler] with the corresponding [Envelope] to the beat clock.
     */
    fun bindEnvelope(envelope: Envelope): Sampler {
        val sampler = Sampler(envelope)
        samplerList.add(sampler)
        return sampler
    }

    /**
     * Creates a [Sampler] using a [build] block on an [EnvelopeBuilder].
     * Requires a [length] to be specified.
     */
    fun bindEnvelopeBySegments(length: Double, build: EnvelopeBuilder.() -> Unit): Sampler {
        val envelope = EnvelopeBuilder.buildBySegments(length, build)
        val sampler = Sampler(envelope)
        samplerList.add(sampler)
        return sampler
    }

    /**
     * Animate to a new [bpm] rate and [t0].
     *
     * If you want to avoid a large shift in phase,
     * you should try to satisfy the following equation:
     * ```
     * bpm * (now - t0) ≈ _bpm * (now - _t0)
     * ```
     * @param bpm The new bpm rate.
     * @param t0 The new t0 time stamp.
     * @param duration The duration of the transition.
     */
    // TODO: Refactor this to be derived from a general Animation or Blend API.
    fun animateTo(bpm: Double, t0: Double, duration: Double) {
        transition = Transition(
            targetBpm = bpm,
            targetT0 = t0,
            transitionBegin = lastNow,
            transitionEnd = lastNow + duration
        )
    }

    /**
     * Reset the beat clock. Starts at [now].
     */
    fun resetTime(now: Double) {
        t0 = now
    }
}

/**
 * Calculates how often an interval of length [intervalLength] fits into this Double.
 *
 * Example:
 * ```
 * 1.0.toIntervalCount(0.25) // 4
 * 1.0.toIntervalCount(0.5)  // 2
 * 1.5.toIntervalCount(1.0)  // 1
 * ```
 */
fun Double.toIntervalCount(intervalLength: Double): Int = (this/intervalLength).toInt()