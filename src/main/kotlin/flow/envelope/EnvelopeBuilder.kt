@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package flow.envelope

import org.openrndr.animatable.easing.Easer
import org.openrndr.animatable.easing.Easing
import org.openrndr.animatable.easing.Linear

/**
 * Builder class for [Envelope].
 *
 * Example:
 * ```
 * buildBySegments(length = 4.0) {
 *  segment(fromT = 0.25, fromX = 1.0, toT = 0.5, toX = 0.1) // very explicit
 *  segmentJoin(toT = 1.0, toX = 0.0) // usual build step, just join from where you left off
 *  segmentJoin(2.0, 0.5) via Linear() // use via to define an easer
 *  segmentJoin(3.0, 0.9) via { x: Double -> x.pow(3.0) } // you can write your own (unit-to-unit) easer
 * }
 * ```
 */
class EnvelopeBuilder internal constructor(){

    private val segments = mutableListOf<EnvelopeSegment>()

    /**
     * After the last call to [segment] or [segmentJoin], the value [lastT] is updated with `toT`.
     */
    var lastT = 0.0
    /**
     * After the last call to [segment] or [segmentJoin], the value [lastX] is updated with `toX`.
     */
    var lastX = 0.0

    /**
     * Adds a segment to the timeline.
     * Should it be illegal, an [IllegalArgumentException] will be thrown.
     */
    fun segment(fromT: Double, fromX: Double, toT: Double, toX: Double): EnvelopeSegment {
        if (fromT >= toT) throw IllegalArgumentException(
            "Time has to progress forward. Thus, segment requires 'fromT' to be less than 'toT'. " +
                    "Found fromT=$fromT, toT=$toT instead."
        )

        val seg =  EnvelopeSegment(fromT, fromX, toT, toX)
        if (seg.hasTimelineOverlap()) throw IllegalArgumentException(
            "Illegal segment(fromT=$fromT, ..., toT=$toT, ...). This overlaps with the existing timeline."
        )

        segments += seg
        lastT = seg.toT
        lastX = seg.toX
        return seg
    }

    /**
     * Adds a segment to the timeline, connecting from the last used t and x value to [toT] and [toX].
     * Should it be illegal, an [IllegalArgumentException] will be thrown.
     */
    fun segmentJoin(toT: Double, toX: Double): EnvelopeSegment = segment(lastT, lastX, toT, toX)

    /**
     * The [EnvelopeSegment] will be eased using the specified [interpolationFunc].
     * This function will map the interpolation points from [[0], [1]] to [[0], [1]].
     */
    infix fun EnvelopeSegment.via(interpolationFunc: (Double) -> Double) {
        this.easer = object : Easer {
            override fun ease(t: Double, b: Double, c: Double, d: Double): Double {
                val normalizedT = t/d
                return b + interpolationFunc(normalizedT) * c
            }

            // unused in this implementation
            override fun velocity(t: Double, b: Double, c: Double, d: Double): Double = 0.0
        }
    }

    /**
     * The [EnvelopeSegment] will be eased using the specified [easing].
     */
    infix fun EnvelopeSegment.via(easing: Easing) {
        this.easer = easing.easer
    }

    /**
     * The [EnvelopeSegment] will be eased using the specified [easer].
     */
    infix fun EnvelopeSegment.via(easer: Easer) {
        this.easer = easer
    }

    private fun build(length: Double) : Envelope {
        // 1. Sort with complexity O(n log n)
        segments.sortBy { it.fromT }

        // 2. Convert to immutable list to feed to closure.
        val timeline = segments.toList()

        // 3. Define evaluation by timeline iteration.
        // The first segment sufficing is used for evaluation.
        val evaluation = { time: Double ->
            var result = 0.0
            for (seg in timeline) {
                if (seg.fromT <= time && time < seg.toT) {
                    // Call the easing function with according t, b, c, d
                    // t - is now the relative t over the duration d
                    // b - starting value in x
                    // c - delta in x (over fromT to toT)
                    // d - delta in t (duration)
                    result = seg.easer.ease(
                        time - seg.fromT,
                        seg.fromX,
                        seg.deltaX,
                        seg.deltaT
                    )
                    break
                }
            }
            result
        }
        return Envelope(length, evaluation)
    }

    /**
     * Data class for holding a single segment ([fromT], [fromX]) -> ([toT], [toX]) of the envelope.
     * Allows to specify an [easer] to ease the segment interpolation.
     */
    data class EnvelopeSegment(val fromT: Double, val fromX: Double, val toT: Double, val toX: Double, var easer: Easer = Linear()) {
        val deltaT = toT - fromT
        val deltaX = toX - fromX
    }

    /**
     * Returns boolean whether [this] segment overlaps with any other segment so far.
     *
     * Note: segments are treated left-inclusive right-exclusive i.e. as half-open time length.
     * Thus, [a, b) and [b, c) do not overlap.
     */
    private fun EnvelopeSegment.hasTimelineOverlap(): Boolean {
        return !segments.all { other ->
            // Ignore self intersection
            if (this == other) return true

            // If other's right is less-or-equal to this' left
            // or if this' right is less-or-equal to other's left,
            // then timelines don't overlap.
            other.toT <= this.fromT || this.toT <= other.fromT
        }
    }

    companion object {
        private val zeroEaser = object : Easer {
            override fun ease(t: Double, b: Double, c: Double, d: Double): Double = 0.0
            override fun velocity(t: Double, b: Double, c: Double, d: Double): Double = 0.0
        }

        /**
         * Builder access for a new [Envelope].
         * Specify segments over the domain [[0], [length]] to the range [[0], [1]].
         */
        fun buildBySegments(length: Double, block: EnvelopeBuilder.() -> Unit): Envelope {
            val builder = EnvelopeBuilder()
            builder.block()
            return builder.build(length)
        }
    }
}