package demos

import flow.FlowProgram.Companion.flowProgram
import flow.FlowProgramConfig
import flow.autoupdate.AutoUpdate.autoUpdate
import flow.color.ColorRepo
import flow.content.VisualGroup
import flow.envelope.LinearCapacitor
import flow.envelope.keyAutoUpdate
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
import kotlin.math.exp
import kotlin.math.pow

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
        val kick by beatClock.bindEnvelopeBySegments(1.0) {
            lastX = 0.9
            segmentJoin(0.50, 1.0)
            segmentJoin(0.75, 0.1) via { org.openrndr.math.smootherstep(0.0, 1.0, it) }
            segmentJoin(0.85, 0.0)
            segmentJoin(1.00, 0.9) via { x: Double -> x.pow(3.0) }
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

            val s1 = scene {
                draw {
                    galaxyGroup.draw()
                }

                post(renderPipeline.chromaticAberration) {
                    aberrationFactor = kick * kickFac * 20.0
                }
                post(renderPipeline.verticalWave) {
                    amplitude = kick * kickFac * 0.02
                }
            }

            val s2 = scene {
                val videoPlayer = VideoPlayerFFMPEG.fromFile(
                    fileName = "src/main/resources/videos/network_12716(1080p).mp4",
                    mode = PlayMode.VIDEO
                )

                videoPlayer.ended.listen { videoPlayer.restart() }
                videoPlayer.play()

                draw {
                    videoPlayer.draw(drawer)
                }
            }

            var remainingTransitions = 0

            val t1 = transition { s0, s1, t ->
                renderPipeline.squircleBlend.blend = t
                renderPipeline.squircleBlend.apply(s0, s1, transitionBuffer!!)
                transitionBuffer!!
            }
        }

        // Init UI display
        uiDisplay.apply {
            trackValue("Galaxy zoom") { galaxyGroup.zoomVariations.value }
        }

        // Main draw loop
        extend {
            if (frameCount == 5) sceneNav.startTransition(sceneNav.defaultTransition, sceneNav.s1, 0.5)

            if (sceneNav.remainingTransitions > 0 && sceneNav.currentTransition == null) {
                sceneNav.remainingTransitions--

                val otherScene = if (sceneNav.currentScene == sceneNav.s1) {
                    sceneNav.s2
                } else {
                    sceneNav.s1
                }
                sceneNav.startTransition(sceneNav.t1, otherScene, 2.0)
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

