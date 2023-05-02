@file:Suppress("MemberVisibilityCanBePrivate", "CanBeParameter", "unused")

package flow.audio

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.ConstantQ
import flow.realtime.filters.OneEuroFilter
import org.openrndr.math.clamp
import org.openrndr.math.map
import util.QueueCache
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.sqrt

class ConstantQProcessor(
    val binsPerOctave: Int,
    val rangeList: List<ClosedFloatingPointRange<Double>>,
    val sampleRate: Int,
    val eventBufferSize: Int,
): AudioProcessor {

    // Exact power of 2 for ConstantQ lo-hi factor
    private val loFq = 20.0
    private val hiFq = 20000.0

    private val constantQ = ConstantQ(sampleRate.toFloat(), loFq.toFloat(), hiFq.toFloat(), binsPerOctave.toFloat())

    private fun frequencyToIndex(frequency: Double): Int {
        val fq = frequency.clamp(loFq, hiFq)
        val index = binsPerOctave * log2(fq / loFq)
        return index.toInt()
    }

    // MAGNITUDES
    private val magnitudesCache = QueueCache<FloatArray>(eventBufferSize)
    val magnitudes by magnitudesCache

    // Magnitudes -> Db Filtered
    val numberOfBins = constantQ.magnitudes.size
    private val magnitudeFilterList = List(numberOfBins) {
        OneEuroFilter(0.5, 0.01, 5.0, 0.0)
    }
    var filteredMagnitudes = List(numberOfBins) { 0.0 }


    // RANGES
    private val indexRangeList = rangeList.map { range ->
        val loIndex = frequencyToIndex(range.start)
        val hiIndex = frequencyToIndex(range.endInclusive)
        loIndex until hiIndex
    }

    private val rangedVolumeCache = QueueCache<List<Double>>(eventBufferSize)
    val rangedVolumesList by rangedVolumeCache

    // Ranges -> Db Filtered
    private val rangedVolumeFilterList = List(rangeList.size) {
        OneEuroFilter(0.6, 0.01, 2.0, 0.0)
    }
    var filteredRangedVolumesList = List(rangeList.size) { 0.0 }

    // On each event, update all values
    private val filterDt = constantQ.ffTlength * 1.0 / sampleRate

    override fun process(audioEvent: AudioEvent?): Boolean {

        constantQ.process(audioEvent)

        val magnitudes = constantQ.magnitudes!!.clone()
        magnitudesCache.add(magnitudes)
        filteredMagnitudes = magnitudeFilterList.mapIndexed { index, filter ->
            val y = magnitudes[index].toDouble().toDb()
                .map(Audio.LOWEST_SPL, Audio.HIGHEST_SPL, 0.0, 1.0)
            filter.filter(y, filterDt)
        }

        val rangedVolumes = indexRangeList.map { range ->
            magnitudes.sliceArray(range).map { it.toDouble().toDb() }.rms()
                .times(-1.0) // Correction to be negative db again
        }
        rangedVolumeCache.add(rangedVolumes.toList())

        filteredRangedVolumesList = rangedVolumeFilterList.mapIndexed { index, filter ->
            val y = rangedVolumes[index].map(Audio.LOWEST_SPL, Audio.HIGHEST_SPL, 0.0, 1.0)
            filter.filter(y, filterDt)
        }

        return true
    }

    override fun processingFinished() {}
}

/**
 * Converts a magnitude value to decibels.
 *
 * Coerced to be at least [Audio.LOWEST_SPL].
 * Should the value be more than [Audio.HIGHEST_SPL], you are doing something wrong anyway.
 */
fun Double.toDb(): Double {
    if (this <= 0) return Audio.LOWEST_SPL
    return (20 * log10(this)).coerceAtLeast(Audio.LOWEST_SPL)
}

/**
 * Calculates the root-mean-square of the values in this array.
 */
private fun List<Double>.rms(): Double {
    var sum = 0.0
    for (i in this.indices) {
        sum += this[i] * this[i]
    }
    return sqrt(sum / this.size)
}
