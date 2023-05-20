@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package util

import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import kotlin.reflect.KProperty

// This file is a dump for common utility functions and data structures.
// It is not meant to be included in the final build.
// But "miscellaneous" doesn't sound that good...

/**
 * Data class for storing [options] and cycling through them with [next].
 * Initial value is the first element of [options]. Repeats after the last one.
 */
class CyclicFlag<T>(val options: List<T>) {

    constructor(vararg options: T): this(options.toList())

    var index = 0

    val value: T
        get() = options[index]

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

class QueueCache<T>(val size: Int){

    private val _cache = emptyList<T>().toMutableList()

    val cache: List<T>
        get() = _cache.toList()

    fun add(element: T) {
        _cache.add(element)
        if (_cache.size > size) {
            _cache.removeAt(0)
        }
    }

    operator fun getValue(requester: Any, property: KProperty<*>): List<T> {
        return cache
    }
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