@file:Suppress("MemberVisibilityCanBePrivate", "CanBeParameter", "unused")

package flow.audio

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.ConstantQ
import flow.realtime.filters.OneEuroFilter
import org.openrndr.math.clamp
import org.openrndr.math.map
import flow.util.QueueCache
import kotlin.math.log2
import kotlin.math.sqrt

/**
 * Audio processor that uses the [ConstantQ](http://academics.wellesley.edu/Physics/brown/pubs/cq1stPaper.pdf)
 * algorithm from [TarsosDSP](https://github.com/JorenSix/TarsosDSP)
 * to calculate the magnitudes of the signal.
 *
 * You can access the magnitudes directly from [magnitudesCache],
 * or use [filteredMagnitudes] to get smoothed version ranging from 0.0 to 1.0.
 *
 * Similarly, you specify the frequency ranges you want to analyze with [freqBands].
 * Access the cached result list with [bandedVolumeCache],
 * or use the current smoothed value list [filteredBandedVolumes].
 *
 * @param binsPerOctave The number of bins per octave. This enforces a certain [Audio.bufferSize] !
 * @param freqBands The frequency bands to analyze. Each band should be within 20Hz to 20kHz.
 * @param sampleRate The sample rate of the audio signal.
 * @param eventBufferSize The size of the buffer that stores the magnitudes.
 */
// TODO: Refactor data caching and filtering out of this class.
//  It is overburdened and should only provide magnitude/volume data.
//  Also, the naming scheme is _very confusing_.
class ConstantQProcessor(
    val binsPerOctave: Int,
    val freqBands: List<ClosedFloatingPointRange<Double>>,
    val sampleRate: Int,
    val eventBufferSize: Int,
): AudioProcessor {

    // Shorthand for audio constants
    // Don't use 20f-20000f range as it will hang on termination.
    private val loFq = 40.0
    private val hiFq = 10000.0

    // ConstantQ runner (processor for TarsosDSP)
    private val constantQ = ConstantQ(sampleRate.toFloat(), loFq.toFloat(), hiFq.toFloat(), binsPerOctave.toFloat())

    // MAGNITUDES
    private val _magnitudesCache = QueueCache<FloatArray>(eventBufferSize)

    /**
     * The cached list of magnitudes. Size is [eventBufferSize]. Latest event is at the end of the list.
     */
    val magnitudesCache by _magnitudesCache

    // (Magnitude -> Db) Filtered
    /**
     * The total number of bins in the [ConstantQ] output.
     * After an audio event process, each bin is calculated to a magnitude.
     */
    val numberOfBins = constantQ.magnitudes.size
    private val magnitudeFilterList = List(numberOfBins) {
        OneEuroFilter(0.5, 0.01, 5.0, 0.0)
    }

    /**
     * The current list of magnitudes, adjusted by [OneEuroFilter]. Range is 0.0 to 1.0.
     */
    var filteredMagnitudes = List(numberOfBins) { 0.0 }


    // BANDS
    private fun frequencyToIndex(frequency: Double): Int {
        val fq = frequency.clamp(loFq, hiFq)
        val index = binsPerOctave * log2(fq / loFq)
        return index.toInt()
    }

    private val bandIndexList = freqBands.map { range ->
        val loIndex = frequencyToIndex(range.start)
        val hiIndex = frequencyToIndex(range.endInclusive)
        loIndex until hiIndex
    }


    private val _bandedVolumeCache = QueueCache<List<Double>>(eventBufferSize)

    /**
     * The cached list of banded passed volumes.
     */
    val bandedVolumeCache by _bandedVolumeCache

    // (Freq Band -> Db) Filtered
    private val rangedVolumeFilterList = List(freqBands.size) {
        OneEuroFilter(0.6, 0.01, 2.0, 0.0)
    }

    /**
     * The current list of volumes by frequency band, adjusted by [OneEuroFilter]. Range is 0.0 to 1.0.
     */
    var filteredBandedVolumes = List(freqBands.size) { 0.0 }

    // Time delta between audio events.
    private val filterDt = constantQ.ffTlength * 1.0 / sampleRate

    /**
     * Process the incoming audio event.
     */
    override fun process(audioEvent: AudioEvent?): Boolean {

        constantQ.process(audioEvent)

        val magnitudes = constantQ.magnitudes!!.clone()
        _magnitudesCache.add(magnitudes)
        filteredMagnitudes = magnitudeFilterList.mapIndexed { index, filter ->
            val y = magnitudes[index].toDouble().toDb()
                .map(Audio.LOWEST_SPL, Audio.HIGHEST_SPL, 0.0, 1.0)
            filter.filter(y, filterDt)
        }

        val rangedVolumes = bandIndexList.map { range ->
            magnitudes.sliceArray(range).map { it.toDouble().toDb() }.rms()
                .times(-1.0) // Correction to be negative db again
        }
        _bandedVolumeCache.add(rangedVolumes.toList())

        filteredBandedVolumes = rangedVolumeFilterList.mapIndexed { index, filter ->
            val y = rangedVolumes[index].map(Audio.LOWEST_SPL, Audio.HIGHEST_SPL, 0.0, 1.0)
            filter.filter(y, filterDt)
        }

        return true
    }

    override fun processingFinished() {}
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
