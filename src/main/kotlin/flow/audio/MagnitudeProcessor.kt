@file:Suppress("MemberVisibilityCanBePrivate", "CanBeParameter", "unused")

package flow.audio

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.ConstantQ
import flow.util.isPowerOf
import mu.KotlinLogging
import kotlin.math.log2

private val logger = KotlinLogging.logger {}

/**
 * Audio processor that uses the [ConstantQ](http://academics.wellesley.edu/Physics/brown/pubs/cq1stPaper.pdf)
 * algorithm from [TarsosDSP](https://github.com/JorenSix/TarsosDSP)
 * to calculate the magnitudes of the signal.
 *
 * You can access the magnitudes directly from [magnitudes].
 *
 * @param numberOfBins Number of bins that hold the magnitudes.
 * @param sampleRate Sample rate of the audio. Deviations from system device *will* cause problems.
 * @param bufferSize Size of the audio buffer. This matches the [ConstantQ.fftLength]. If the value can not be enforced, an exception is thrown.
 * @param kernelThreshold Truncates the FFT kernel. Requires finding a setup sweet spot.
 * @throws IllegalArgumentException if the param combination would lead to an invalid constantQ configuration.
 */
class MagnitudeProcessor(
    numberOfBins: Int,
    sampleRate: Int,
    bufferSize: Int,
    kernelThreshold: Double = 1/32.0,
): AudioProcessor {

    init {
        if (bufferSize.isPowerOf(2).not())
            throw IllegalArgumentException("bufferSize=${bufferSize} must be a power of two.")
    }

    private val loFq = 20.0
    private val hiFq = 20000.0
    val threshold = kernelThreshold.toFloat()

    /**
     * Creates a constantQ processor with the right values for q, numberOfBins and fftLength.
     */
    private fun createConstantQ(
        sampleRate: Int,
        minFrequency: Double,
        maxFrequency: Double,
        numberOfBins: Int,
        fftLength: Int
    ): ConstantQ {
        /*
         *  -- Symbols --
         * r:       sample rate
         * minF:    minimum frequency
         * maxF:    maximum frequency
         * nob:     number of bins
         * bpo:     bins per octave
         * L:       fft length
         * q:       q
         * s:       spread
         *
         * -- ConstantQ --
         * q = 1 / (2^(1/bpo) - 1) / s
         * L = ceil(q * r / minF)        NOTE: L is then increased to be the next power of two
         * nob = floor(bpo * log2(maxF/minF))
         *
         * -- Reversal of ConstantQ --
         * bpo = floor(nob / log2(maxF/minF))
         * q = floor(L * minF / r)    => A double is sufficient, drop "floor"
         * q = L * minF / r
         * s = 1 / (2^(1/bpo) - 1) / q
         */
        val r = sampleRate.toFloat()
        val minF = minFrequency.toFloat()
        val maxF = maxFrequency.toFloat()
        val nob = numberOfBins.toFloat()
        val L = fftLength.toFloat()

        val bpo = nob / log2(maxF/minF)
        val q = L * minF / r
        val s = (1 / (Math.pow(2.0, 1.0 / bpo) - 1) / q).toFloat() * 1.0001f // +0.1% to avoid rounding errors

        logger.info { "Creating ConstantQ audio processor with configuration:" }
        logger.info { "  sample rate: $r" }
        logger.info { "  min frequency: $minF" }
        logger.info { "  max frequency: $maxF" }
        logger.info { "  bins per octave: $bpo" }
        logger.info { "  (implicit) spread: $s" }
        logger.info { "  (implicit) q: $q" }

        val constantQ = ConstantQ(r, minF, maxF, bpo, threshold, s)

        logger.info { "Created constantQ (expected, actual) value pairs:" }
        logger.info { "  fft length: ($L, ${constantQ.ffTlength})" }
        logger.info { "  number of bins: ($nob, ${constantQ.magnitudes.size})" }

        logConstantQCalculation(r, minF, maxF, bpo, s)

        return constantQ
    }

    private fun logConstantQCalculation(sampleRate: Float, minFreq: Float, maxFreq: Float, binsPerOctave: Float, spread: Float) {
        val r = sampleRate
        val minF = minFreq
        val maxF = maxFreq
        val bpo = binsPerOctave.toInt()
        val s = spread

        // Calculate the actual values, just like ConstantQ does.
        logger.debug { "  (calculated) bins per octave: $bpo" }
        val q = 1.0 / (Math.pow(2.0, 1.0 / bpo) - 1.0) / s
        logger.debug { "  (calculated) q: $q" }
        // Not in the original, but it's useful intermediate info.
        val preCeilNob = bpo * Math.log((maxF / minF).toDouble()) / Math.log(2.0)
        logger.debug { "  Number of bins before applying ceil: $preCeilNob" }

        val numberOfBins = Math.ceil(bpo * Math.log((maxF / minF).toDouble()) / Math.log(2.0))
        logger.debug { "  (calculated) number of bins: $numberOfBins" }

        // Not in the original, but it's useful intermediate info.
        val preCeilLen = q * (r / minF).toDouble()
        logger.debug { "  Fft length before applying ceil: $preCeilLen" }

        val calc_fftlen = Math.ceil(q * (r / minF).toDouble()).toFloat()
        logger.debug { "  (calculated) calculated fft length: $calc_fftlen" }
        val fftLength = Math.pow(2.0, Math.ceil(Math.log(calc_fftlen.toDouble()) / Math.log(2.0))).toInt()
        logger.debug { "  (calculated) fft length: $fftLength" }

    }

    /**
     * The ConstantQ processor that calculates the magnitudes.
     *
     * The fft length matches the buffer size.
     */
    val constantQ = createConstantQ(sampleRate, loFq, hiFq, numberOfBins, fftLength = bufferSize)

    var magnitudes = FloatArray(numberOfBins)
        private set

    /**
     * Process the incoming audio event.
     */
    override fun process(audioEvent: AudioEvent?): Boolean {

        constantQ.process(audioEvent)

        magnitudes = constantQ.magnitudes!!.clone()

        return true
    }

    override fun processingFinished() {}
}
