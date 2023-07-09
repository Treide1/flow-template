package demos

import be.tarsos.dsp.AudioEvent
import flow.FlowProgram.Companion.flowProgram
import flow.FlowProgramConfig
import flow.audio.*
import flow.autoupdate.AutoUpdate.autoUpdate
import flow.color.ColorRepo
import flow.content.VisualGroup
import flow.envelope.LinearCapacitor
import flow.envelope.keyAutoUpdate
import flow.fx.Crossfade
import flow.fx.galaxyShadeStyle
import flow.input.InputScheme.TrackTypes.TOGGLE
import flow.realtime.oneEuroFilter.OneEuroFilter
import flow.rendering.scenes.SceneNavigator
import flow.shadertoy.projects.jaszUniverse.JaszUniverse
import flow.shadertoy.projects.viscousFingering.ViscousFingering
import flow.util.CyclicFlag
import flow.util.TWO_PI
import org.openrndr.Fullscreen
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.draw.tint
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.color.ChromaticAberration
import org.openrndr.extra.fx.distort.VerticalWave
import org.openrndr.extra.gui.addTo
import org.openrndr.ffmpeg.PlayMode
import org.openrndr.ffmpeg.VideoPlayerFFMPEG
import org.openrndr.math.map
import org.openrndr.math.saturate
import org.openrndr.math.smoothstep
import kotlin.math.*

/**
 * Demo 2 of the "Flow" template.
 */
fun main() = application {
    configure {
        fullscreen = Fullscreen.CURRENT_DISPLAY_MODE
        display = displays.last()
    }
    flowProgram(
        FlowProgramConfig(
            initialBpm = 125.0, // <- Play your favorite song. Set its bpm here.
        )
    ) {

        // Define beat-based values
        val kick by beatClock.bindEnvelope(1.0) { phase ->
            sin(phase * TWO_PI).coerceAtLeast(0.0)
        }

        val kickFac by LinearCapacitor(0.1, 0.1).keyAutoUpdate(this, "k")

        val ebbAndFlow by beatClock.bindEnvelopeBySegments(8.0) {
            val exponentialDecay = { x: Double -> exp(-x).map(1.0, exp(-1.0), 0.0, 1.0) }
            lastX = 1.0
            segmentJoin(7.0, 0.0) via exponentialDecay
            segmentJoin(8.0, 1.0) via exponentialDecay
        }

        // Init colors
        val colorRepo = ColorRepo.fromHexMapOf(
            "0" to "#E006B2",
            "90" to "#82B1E0",
            "180" to "#7AE060",
            "270" to "#E0C18D",
        )

        val audio = object: Audio(flowProgram, bufferSize = 1024, overlap = 512) {
            val waveformBuffer = FloatArray(512)
            val fftBuffer = FloatArray(512)
            val fftProcessor = FftProcessor(512)

            val volProcessor = VolumeProcessor()
            val volSmoother = OneEuroFilter(1.0, 0.1, 1.0, 0.0)
            val smoothedVol by volSmoother

            override fun setProcess(audioEvent: AudioEvent, dt: Double) {
                fftProcessor.process(audioEvent)
                fftProcessor.magnitudes.copyInto(fftBuffer, endIndex = 512)
                audioEvent.floatBuffer.copyInto(waveformBuffer, endIndex = 512)

                volProcessor.process(audioEvent)
                volSmoother.filter(volProcessor.volume.toRelativeVolume(), dt)
            }
        }
        audio.start()

        // Define visual groups
        val galaxyGroup = object: VisualGroup(program) {

            val zoomVariations = CyclicFlag("Step by step", "Full Zoom")

            override fun Drawer.draw() {
                val offset = when (zoomVariations.value) {
                    "Step by step" -> (beatClock.phase%32.0) / 8.0
                    "Full Zoom" -> (beatClock.phase%8.0).pow(4.0) / 200.0
                    else -> 0.0
                }
                val scl = 2.5 - 1.5 * ebbAndFlow.smoothstep(0.0, 1.5) + offset
                shadeStyle = galaxyShadeStyle(seconds, zoom = scl)
                rectangle(drawer.bounds)
            }
        }

        val viscousGroup = object : VisualGroup(program) {
            val renderer = ViscousFingering(flowProgram).addTo(gui, "Viscous Fingering")

            override fun Drawer.draw() {
                image(renderer.render())
            }
        }

        val jaszGroup = object: VisualGroup(program) {
            val renderer = JaszUniverse(flowProgram).addTo(gui, "Jasz Universe")

            override fun Drawer.draw() {
                renderer.writeToMicBuffer(audio.fftBuffer, audio.waveformBuffer)
                renderer.musicVolume = audio.smoothedVol.map(0.7, 0.85, 0.0, 1.0).pow(3).saturate()

                image(renderer.render())
            }
        }

        // Define scenes and transitions
        val sceneNav = object: SceneNavigator(program) {

            override val defaultClearColor = ColorRGBa.TRANSPARENT

            val compositeS = compositeScene {
                layer {
                    draw {
                        // TODO: move into separate scenes, (add color buffer based transitions)
                        //galaxyGroup.draw()
                        //viscousGroup.draw()
                        jaszGroup.draw()
                    }

                    post(ChromaticAberration()) {
                        aberrationFactor = kick * kickFac * 20.0
                    }
                    post(VerticalWave()) {
                        amplitude = kick * kickFac * 0.02
                    }
                }
            }

            val flickerFreq = CyclicFlag(null, 1, 2, 4, 8)

            val videoS = videoScene {
                VideoPlayerFFMPEG.fromFile(
                    fileName = "src/main/resources/videos/network_12716(1080p).mp4",
                    mode = PlayMode.VIDEO
                )
            }.autoUpdate {
                val angle = ebbAndFlow * 180.0 + 90.0
                val relAngle = angle / 90.0
                val i = floor(relAngle).toInt() % 4
                val j = (i + 1) % 4
                val frac = relAngle - i

                val mix = colorRepo[i].mix(colorRepo[j], frac)
                tint = tint(mix)
            }

            var remainingTransitions = 0

            val squircleT = transition { s0, s1, progress ->
                val tmp = getTmpBuffer(0)
                renderPipeline.squircleBlend.blend = progress
                renderPipeline.squircleBlend.apply(s0, s1, tmp)
                tmp
            }

            val lumaT = transition { s0, s1, t ->
                val lumaTmp = getTmpBuffer(0)
                val joinTmp = getTmpBuffer(1)
                renderPipeline.lumaOpacity.apply {
                    foregroundOpacity = t
                    backgroundLuma = t
                    foregroundLuma = backgroundLuma / 2.0
                    apply(s1, lumaTmp)
                }
                val cf = Crossfade()
                cf.blend = t * t
                cf.apply(s0, lumaTmp, joinTmp)
                joinTmp
            }
        }

        // Init UI display
        uiDisplay.apply {
            trackValue("Galaxy zoom") { galaxyGroup.zoomVariations.value }
            trackValue("Remaining transitions") { "${sceneNav.remainingTransitions}" }
            trackValue("Kick") { kick.toString() }
            trackValue("Kick Fac") { kickFac.toString() }
            trackValue("Volume") { audio.smoothedVol.toString() }
        }

        // Main draw loop
        extend {
            sceneNav.apply {
                if (frameCount == 0) startTransition(defaultTransition, compositeS, 0.5)

                if (remainingTransitions > 0 && currentTransition == null) {
                    remainingTransitions--

                    if (currentScene == compositeS) {
                        startTransition(lumaT, videoS, 2.5)
                    } else {
                        startTransition(squircleT, compositeS, 1.5)
                    }
                }
            }

            val img = sceneNav.render(drawer)

            drawer.image(img)

            // TODO: remove from here -> turn into post effect (probably for sceneNavigator)
            drawer.isolated {
                val p = beatClock.phase
                val f = sceneNav.flickerFreq.value?.toDouble() ?: return@isolated
                val v = ((p % (1/f)) * f - 1).pow(2) * (1.0 + log2(f) * 0.5) + 0.75 - log2(f) * 0.25
                fill = ColorRGBa.BLACK.opacify(1-v)
                rectangle(drawer.bounds)
            }

            uiDisplay.displayOnDrawer(drawer)
        }

        inputScheme.apply {
            track(TOGGLE, "k", "Controls kickFac")

            keyDown {
                "+".bind("BPM +1") { beatClock.bpm += 1 }
                "-".bind("BPM -1") { beatClock.bpm -= 1 }
                "z".bind("Next zoom variation") { galaxyGroup.zoomVariations.next() }
                "t".bind("Queue transition") { sceneNav.remainingTransitions++ }
                // Binds 1, 2, ... to the flicker frequency options
                sceneNav.flickerFreq.options.forEachIndexed { index, flickerValue ->
                    "${index+1}".bind("Set flicker frequency (${flickerValue})") {
                        sceneNav.flickerFreq.index = index
                    }
                }
                "c".bind("Change colors") { jaszGroup.renderer.changeColors() }
                "r".bind("Reset viscous fingering") { viscousGroup.renderer.reset() }
            }
        }
    }
}

