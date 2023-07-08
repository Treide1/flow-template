package flow.realtime

import flow.realtime.oneEuroFilter.OneEuroFilter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import kotlin.reflect.KProperty

@Description("One Euro Multi Filter")
class OneEuroMultiFilter(minCutoff: Double, beta: Double, dCutoff: Double, initValList: List<Double>) {


    val filters = initValList.map { OneEuroFilter(minCutoff, beta, dCutoff, it) }

    fun filter(valueList: List<Double>, dTime: Double): List<Double> {
        return valueList.mapIndexed { index, value ->
            filters[index].filter(value, dTime)
        }
    }

    @DoubleParameter("min cutoff", 0.0, 10.0)
    var minCutoff = minCutoff
        set(value) {
            field = value
            filters.forEach { it.minCutoff = value }
        }

    @DoubleParameter("beta", 0.0, 1.0)
    var beta = beta
        set(value) {
            field = value
            filters.forEach { it.beta = value }
        }

    @DoubleParameter("d cutoff", 0.0, 10.0)
    var dCutoff = dCutoff
        set(value) {
            field = value
            filters.forEach { it.dCutoff = value }
        }


    operator fun getValue(requester: Any?, property: KProperty<*>): List<Double> {
        return filters.map { it.value }
    }
}