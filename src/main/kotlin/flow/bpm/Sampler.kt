package flow.bpm

import kotlin.reflect.KProperty

class Sampler(val eval: (phase: Double) -> Double) {

    var value = 0.0
        private set

    fun update(phase: Double) {
        value = eval(phase)
    }

    operator fun getValue(requestee: Nothing?, property: KProperty<*>): Double {
        return value
    }
}