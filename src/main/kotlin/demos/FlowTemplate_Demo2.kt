package demos

import flow.FlowProgram.Companion.flowProgram
import flow.FlowProgramConfig
import flow.autoupdate.AutoUpdate.autoUpdate
import flow.color.ColorRepo
import flow.content.VisualGroup
import flow.envelope.LinearCapacitor
import flow.envelope.keyAutoUpdate
import flow.fx.Crossfade
import flow.fx.galaxyShadeStyle
import flow.input.InputScheme.TrackTypes.TOGGLE
import flow.rendering.scenes.SceneNavigator
import org.openrndr.Fullscreen
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.extra.compositor.*
import org.openrndr.ffmpeg.PlayMode
import org.openrndr.ffmpeg.VideoPlayerFFMPEG
import org.openrndr.math.map
import org.openrndr.math.smoothstep
import flow.util.CyclicFlag
import flow.util.TWO_PI
import org.openrndr.extra.fx.color.ChromaticAberration
import org.openrndr.extra.fx.distort.VerticalWave
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

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

        renderPipeline.apply {
            lumaOpacity.autoUpdate {
                foregroundOpacity = ebbAndFlow
                backgroundLuma = ebbAndFlow.smoothstep(0.0, 1.0)
                foregroundLuma = backgroundLuma / 2.0
            }
        }

        // Define visual groups
        val galaxyGroup = object: VisualGroup(program) {

            val zoomVariations = CyclicFlag("Step by step", "Full Zoom")

            override fun Drawer.draw() {
                shadeStyle = galaxyShadeStyle(seconds)
                val offset = when (zoomVariations.value) {
                    "Step by step" -> (beatClock.phase%32.0) / 8.0
                    "Full Zoom" -> (beatClock.phase%8.0).pow(4.0) / 200.0
                    else -> 0.0
                }
                val scl = 2.5 - 1.5 * ebbAndFlow.smoothstep(0.0, 1.5) + offset
                rectangle(drawer.bounds.scaledBy(scl))
            }
        }

        // Define scenes and transitions
        val sceneNav = object: SceneNavigator(program) {

            override val defaultClearColor = ColorRGBa.TRANSPARENT

            val compositeS = compositeScene {
                layer {
                    draw {
                        galaxyGroup.draw()
                    }

                    post(ChromaticAberration()) {
                        aberrationFactor = kick * kickFac * 20.0
                    }
                    post(VerticalWave()) {
                        amplitude = kick * kickFac * 0.02
                    }
                }
            }

            val videoS = videoScene {
                VideoPlayerFFMPEG.fromFile(
                    fileName = "src/main/resources/videos/network_12716(1080p).mp4",
                    mode = PlayMode.VIDEO
                )
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

        fun transitionToOtherScene() {
            if (sceneNav.currentScene == sceneNav.compositeS) {
                sceneNav.startTransition(sceneNav.lumaT, sceneNav.videoS, 1.5)
            } else {
                sceneNav.startTransition(sceneNav.squircleT, sceneNav.compositeS, 2.5)
            }
        }

        // Init UI display
        uiDisplay.apply {
            trackValue("Galaxy zoom") { galaxyGroup.zoomVariations.value }
            trackValue("Remaining transitions") { "${sceneNav.remainingTransitions}" }
            trackValue("Kick") { kick.toString() }
            trackValue("Kick Fac") { kickFac.toString() }

        }

        // Main draw loop
        extend {
            if (frameCount == 5) sceneNav.startTransition(sceneNav.defaultTransition, sceneNav.compositeS, 0.5)

            if (sceneNav.remainingTransitions > 0 && sceneNav.currentTransition == null) {
                sceneNav.remainingTransitions--

                transitionToOtherScene()
            }

            val img = sceneNav.render(drawer)
            drawer.image(img)

            uiDisplay.displayOnDrawer(drawer)
        }

        inputScheme.apply {
            track(TOGGLE, "k", "Controls kickFac")

            keyDown {
                "z".bind("Next zoom variation") { galaxyGroup.zoomVariations.next() }
                "t".bind("Queue transition") { sceneNav.remainingTransitions++ }
            }
        }
    }
}

