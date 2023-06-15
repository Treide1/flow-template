@file:Suppress("unused")

package flow.bpm

import org.openrndr.math.map
import java.io.File

/**
 * A timeline that consists of timelines.
 */
class Composite(
    name: String,
    var timelines: List<Timeline>,
): Timeline(name) {

    init {
        require(timelines.isNotEmpty())
        timelines = timelines.sortedBy { it.start }
    }

    override val start: Double
        get() = timelines.first().start

    override val end: Double?
        get() = timelines.last().end

    init {
        val last = timelines.last()
        if (last is Event && last.end == null) last.end = Double.MAX_VALUE
    }

    override fun get(name: String) = timelines.find { it.name == name }

    override fun get(index: Int) = if (index in timelines.indices) timelines[index] else null

    /**
     * Recursively sets the null end values to the start of the next timeline.
     */
    internal fun inferEnds(lastEnd: Double) {
        timelines.forEachIndexed { index, prior ->
            val end = if (index != timelines.lastIndex) {
                timelines[index + 1].start
            } else {
                lastEnd
            }

            when (prior) {
                is Event -> prior.end = end
                is Composite -> prior.inferEnds(end)
            }
        }
    }

    override fun toJson() = "{\n\"name\": \"$name\", \n\"timelines\": [\n ${timelines.joinToString(",\n ") { it.toJson() }}\n]}"
}

/**
 * A single event.
 */
class Event(
    name: String,
    override val start: Double,
    override var end: Double? = null,
): Timeline(name) {

    override fun get(name: String): Timeline? = null

    override fun get(index: Int): Timeline? = null

    override fun toJson() = "{\"name\": \"$name\", \"start\": $start, \"end\": $end}"
}

/**
 * Timeline of temporally disjoint timespans.
 *
 * Timelines can be of type [Composite] consisting of sub-timelines or of type [Event] representing a single event.
 *
 * Timeline declaration:
 * * Has a [name] that is unique within its parent.
 * * Has a [start] and an [end] value.
 * * Allows to get a child timeline by `name` or `index`.
 */
sealed class Timeline(val name: String) {
    /**
     * The timeline start.
     */
    abstract val start: Double

    /**
     * The timeline end. (Only null during setup)
     */
    abstract val end: Double?

    /**
     * Get a child timeline by its [name].
     */
    abstract operator fun get(name: String): Timeline?

    /**
     * Get a child timeline by its [index].
     */
    abstract operator fun get(index: Int): Timeline?

    /**
     * Get a json representation of this timeline.
     */
    abstract fun toJson(): String
}

/////////////////////////////////////////////

fun compositeTimeline(name: String, vararg timelines: Timeline): Composite {
    return Composite(name, timelines.toList()).apply { inferEnds(lastEnd = Double.MAX_VALUE) }
}

fun eventTimeline(name: String, vararg events: Double, end: Double? = null): Composite {
    val timelines = events.mapIndexed { i, v -> Event("$i", v, null) }
    timelines.last().end = end
    return Composite(name, timelines)
}

fun namedTimeline(name: String, vararg entries: Pair<String, Double>, end: Double? = null): Composite {
    val timelines = entries.map { (n, v) -> Event(n, v, null) }
    timelines.last().end = end
    return Composite(name, timelines)
}

/////////////////////////////////////////////

/**
 * Calls the [timeline]'s toJson function and writes it to [path].
 */
fun saveAsJson(timeline: Timeline, path: String) {
    // Create file
    val f = File(path)
    f.createNewFile()

    // Create json
    val json = timeline.toJson()

    // Write json
    f.writeText(json)
}

/////////////////////////////////////////////

/**
 * A timeline clock that provides a [timelineStack] to traverse the tree of active timelines.
 *
 * The [timelineStack] is extended within the [whenActive] or [whenActiveIndex] block.
 * You can use the [fullActivePath] to get a string representation of the active timeline path.
 * The [clockUpdate] function is used to update the clock value.
 */
class TimelineClock(
    val timeline: Timeline,
    var clockUpdate: () -> Double =  { 0.0 }
) {

    /**
     * A string representation of the active timeline path.
     */
    val fullActivePath: String
        get() {
            val path = mutableListOf<Timeline>()
            var active = timeline
            while (isActive(active)) {
                when (active) {
                    is Event -> {
                        path.add(active)
                        break
                    }
                    is Composite -> {
                        val next = active.timelines.find { isActive(it) }!!
                        path.add(next)
                        active = next
                    }
                }
            }
            return path.joinToString("/")
        }

    private var clockValue = 0.0

    /**
     * Updates the clock value.
     */
    fun updateClock() {
        clockValue = clockUpdate()
    }

    /**
     * The hierarchy of timelines that are currently active.
     * Is added upon within [whenActive] block.
     */
    val timelineStack = ArrayDeque(listOf(timeline))

    /**
     * The current time in the timeline.
     */
    val t: Double // abs time
        get() = clockValue

    /**
     * The currently active timeline's start.
     */
    val start: Double // abs start
        get() = timelineStack.last().start

    /**
     * The currently active timeline's end.
     */
    val end: Double // abs end
        get() = timelineStack.last().end!!

    /**
     * The progress within the currently active timeline.
     */
    val progress: Double // rel phase
        get() = t.map(start, end, 0.0, 1.0)

    /**
     * Checks if the [timeline] is active.
     */
    fun isActive(timeline: Timeline): Boolean {
        val s = timeline.start
        val e = timeline.end
        val t = clockValue
        return s <= t && (e == null || t < e)
    }

    /**
     * Finds the timeline with the given name.
     * If it is found, and it's active, then it executes the block with that timeline as the current timeline.
     */
    fun whenActive(name: String, block: TimelineClock.() -> Unit = {}) {

        val sub = timelineStack.last()[name]
        if (sub == null) {
            println("Illegal timeline name: $name")
            return
        }

        if (isActive(sub)) {
            timelineStack.addLast(sub)
            block()
            timelineStack.removeLast()
        }
    }

    /**
     * Find the timeline at the given index.
     * If it is found, and it's active, then it executes the block with that timeline as the current timeline.
     */
    fun whenActiveIndex(block: (activeIndex: Int) -> Unit) {
        val last = timelineStack.last()

        if (t !in start..end || last !is Composite) {
            println("Tried to access index for t=$t, start..end=($start, $end), last=$last")
            return
        }
        else {
            val index = last.timelines.indexOfLast { it.start <= t }
            val sub = last[index]!!
            timelineStack.add(sub)
            block(index)
            timelineStack.removeLast()
        }
    }
}
