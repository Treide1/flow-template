@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package util

import kotlin.reflect.KProperty

// This file is a dump for common utility functions and data structures.
// It is not meant to be included in the final build.
// But "miscellaneous" doesn't sound that good...

/**
 * Data class for storing [options] and cycling through them with [next].
 * Initial value is the first element of [options]. Repeats after the last one.
 */
class CyclicFlag<T>(val options: List<T>) {
    var index = 0
    private var _value = options[index]

    val value: T
        get() = _value

    fun next() {
        index = (index + 1) % options.size
        _value = options[index]
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
