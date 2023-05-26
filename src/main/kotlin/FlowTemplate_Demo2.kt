import flow.FlowProgram.Companion.flowProgram
import flow.FlowProgramConfig
import flow.autoupdate.AutoUpdate.autoUpdate
import flow.color.ColorRepo
import flow.content.VisualGroup
import flow.envelope.LinearCapacitor
import flow.fx.galaxyShadeStyle
import flow.input.InputScheme.TrackTypes.TOGGLE
import flow.rendering.image
import org.openrndr.Fullscreen
import org.openrndr.application
import org.openrndr.draw.Drawer
import org.openrndr.ffmpeg.PlayMode
import org.openrndr.ffmpeg.VideoPlayerFFMPEG
import org.openrndr.math.map
import org.openrndr.math.smoothstep
import util.CyclicFlag
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

        val kickFac by LinearCapacitor(0.1, 0.1).autoUpdate {
            update(beatClock.deltaSeconds, inputScheme.isKeyActive("k"))
        }

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

        // Setup audio
        val volProcessor = audio.createVolumeProcessor()
        audio.start()

        // Setup Fx
        renderPipeline.apply {
            lumaOpacity.autoUpdate {
                foregroundOpacity = ebbAndFlow
                backgroundLuma = ebbAndFlow.smoothstep(0.0, 1.0)
                foregroundLuma = ebbAndFlow.smoothstep(0.0, 1.0) / 2.0
            }

            chromaticAberration.autoUpdate {
                aberrationFactor = kick * kickFac * 20.0
            }

            verticalWave.autoUpdate {
                amplitude = kick * kickFac * 0.02
            }
        }

        // Define visual groups
        val glitchGroup = object: VisualGroup(program) {

            val videoPlayer = VideoPlayerFFMPEG.fromFile(
                fileName="src/main/resources/videos/network_12716(1080p).mp4",
                mode = PlayMode.VIDEO
            )

            init {
                videoPlayer.ended.listen { videoPlayer.restart() }
                videoPlayer.play()
            }

            override fun Drawer.draw() {
                videoPlayer.draw(this)
            }

        }

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

        // Init UI display
        uiDisplay.apply {
            trackValue("Galaxy zoom") { galaxyGroup.zoomVariations.value }
        }

        // val tmpBuffer = renderPipeline.createBuffer() // TODO

        // Draw loop
        extend {

            renderPipeline.render {
                clear()
                galaxyGroup.draw()
                drawBuffer.copyTo(tmpBuffer)

                clear()
                glitchGroup.draw()

                drawBuffer.applyFx(
                    lumaOpacity,
                    bloom,
                    verticalWave,
                    chromaticAberration
                )
                sourceAtop.apply(drawBuffer, tmpBuffer, imageBuffer)
            }

            // Draw final image
            drawer.image(renderPipeline)

            // Draw controls
            uiDisplay.displayOnDrawer(drawer)
        }

        inputScheme.apply {

            track(TOGGLE, "k", "Controls kickFac")

            keyDown {
                "z".bind("Next zoom variation") { galaxyGroup.zoomVariations.next() }
            }
        }
    }
}

