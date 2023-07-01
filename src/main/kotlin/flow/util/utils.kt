@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package flow.util

import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour

// This file is a dump for common utility functions and data structures.
// It is not meant to be included in the final build.
// But "miscellaneous" doesn't sound that good...

/**
 * Data class for storing [options] and cycling through them with [next].
 * Initial value is the first element of [options]. Repeats after the last one.
 */
class CyclicFlag<T>(val options: List<T>) {

    constructor(vararg options: T): this(options.toList())

    /**
     * The current index pointing to the current [value].
     */
    var index = 0

    /**
     * The current value.
     */
    val value: T
        get() = options[index]

    /**
     * Set the [index] to the next value.
     */
    fun next() {
        index = (index + 1) % options.size
    }

}

/**
 * Linear interpolation between two values.
 */
fun Double.lerp(other: Double, perc: Double): Double {
    return this + (other - this) * perc
}

/**
 * A cache to store elements in a queue. The latest elements are at the end of [cache].
 * Old values that exceed the [size] are removed from the front.
 */
class QueueCache<T>(val size: Int, initialElements: List<T> = emptyList()) {

    private val _cache = emptyList<T>().toMutableList()

    /**
     * The cache of the last n=[size] many elements.
     */
    val cache: List<T>
        get() = _cache.toList()

    init {
        initialElements.forEach { add(it) }
    }

    /**
     * Add an element.
     */
    fun add(element: T) {
        _cache.add(element)
        if (_cache.size > size) {
            _cache.removeAt(0)
        }
    }

    /**
     * Clears the [cache].
     */
    fun clear() {
        _cache.clear()
    }
}

/**
 * Resource Pool that allows for reusing objects that are often acquired and released.
 *
 * If an element is needed, call [acquireAny]. If you don't need it anymore, call [release].
 *
 * @param initialCount The initial number of elements in the pool.
 * @param creation A function that creates a new element.
 */
class Pool<T>(
    initialCount: Int,
    val creation: (Int) -> T,
) {
    val resourceList = List(initialCount, creation).toMutableList()

    private val inUse = mutableListOf<T>()

    /**
     * Acquires an element from the pool and calls [block] on it. The element is released afterwards.
     */
    fun withAny(block: (T) -> Unit) {
        val element = acquireAny()
        block(element)
        release(element)
    }

    /**
     * Acquires an element from the pool. If none are available, a new one is created.
     */
    fun acquireAny(): T {
        var element = inUse.firstOrNull { it !in resourceList }
        if (element == null) {
            element = creation(resourceList.size)
            resourceList.add(element)
        }
        inUse.add(element!!)
        return element
    }

    /**
     * Releases the [element] back into the pool.
     */
    fun release(element: T) {
        inUse.remove(element)
    }
}


/**
 * Performs the [function] on the mapped [value] and returns the result mapped back.
 * Example:
 * ```
 * withMapping(0.5, 0.0, 1.0, 2*PI, 4*PI) { sin(it) * 0.5 + 0.5 } // sin(3*PI) * 0.5 + 0.5 = 1.0
 * ```
 */
fun withMapping(value: Double, loA: Double, hiA: Double, loB: Double, hiB: Double, function: (Double) -> Double): Double {
    val mapped = value.map(loA, hiA, loB, hiB)
    return function(mapped).map(loB, hiB, loA, hiA)
}

/**
 * Performs the [function] on the mapped [value] and returns the result mapped back.
 * Example:
 * ```
 * withMapping(0.5, 0.0 .. 1.0, 2*PI .. 4*PI) { sin(it) * 0.5 + 0.5 } // sin(3*PI) * 0.5 + 0.5 = 1.0
 * ```
 */
fun withMapping(value: Double, A: ClosedFloatingPointRange<Double>, B: ClosedFloatingPointRange<Double>, function: (Double) -> Double): Double {
    return withMapping(value, A.start, A.endInclusive, B.start, B.endInclusive, function)
}

fun createTriangleContour(position: Vector2, radius: Double, rotation: Double): ShapeContour {

    val triangleVertices = List(3) { Vector2(radius, 0.0).rotate(90.0 + it*120.0) }

    return contour {
        repeat(3) {
            moveOrLineTo(position + triangleVertices[it].rotate(rotation))
        }
        close()
    }
}

const val TWO_PI = 2.0 * Math.PI
const val HALF_PI = 0.5 * Math.PI
const val QUARTER_PI = 0.25 * Math.PI

annotation class Unstable(val reason: String = "Unstable API")