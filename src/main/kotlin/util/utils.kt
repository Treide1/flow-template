@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package util

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
