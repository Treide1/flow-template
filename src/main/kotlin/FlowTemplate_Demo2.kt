import flow.audio.Audio
import flow.autoupdate.AutoUpdate
import flow.autoupdate.AutoUpdate.autoUpdate
import flow.bpm.BeatClock
import flow.color.ColorRepo
import flow.content.VisualGroup
import flow.envelope.LinearCapacitor
import flow.fx.galaxyShadeStyle
import flow.input.InputScheme.TrackTypes.TOGGLE
import flow.input.inputScheme
import flow.rendering.RenderPipeline
import flow.ui.UiDisplay
import org.openrndr.Fullscreen
import org.openrndr.KEY_ESCAPE
import org.openrndr.KEY_SPACEBAR
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.extra.fx.blur.GaussianBloom
import org.openrndr.extra.fx.color.ChromaticAberration
import org.openrndr.extra.fx.color.LumaOpacity
import org.openrndr.extra.fx.distort.VerticalWave
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.ffmpeg.VideoPlayerConfiguration
import org.openrndr.ffmpeg.VideoPlayerFFMPEG
import org.openrndr.math.saturate
import org.openrndr.math.smoothstep
import util.CyclicFlag
import util.TWO_PI
import kotlin.math.*

/**
 * Demo 2 of the "Flow" template.
 */
fun main() = application {
    configure {
        fullscreen = Fullscreen.CURRENT_DISPLAY_MODE
        display = displays.last()
    }
    program {
        // Init inputScheme
        val inputScheme = inputScheme(keyboard)

        // Init beatClock
        val beatClock = extend(BeatClock(bpm=128.0)) // <- Play your favorite song. Set its bpm here.

        // Define beat-based values
        val kick by beatClock.bindEnvelopeBySegments(1.0) {
            lastX = 0.9
            segmentJoin(0.50, 1.0)
            segmentJoin(0.75, 0.1) via { org.openrndr.math.smootherstep(0.0, 1.0, it) }
            segmentJoin(0.85, 0.0)
            segmentJoin(1.00, 0.9) via { x: Double -> x.pow(3.0) }
        }

        val ebbAndFlow by beatClock.bindEnvelope(8.0) { phase ->
            (sin(phase/8.0 * TWO_PI)+0.5).saturate()
        }

        val gui = GUI()

        // Init AutoUpdate
        extend(AutoUpdate)

        // Init colors
        val colorRepo = ColorRepo(
            palette = mapOf(
                "0" to "#E006B2",
                "90" to "#82B1E0",
                "180" to "#7AE060",
                "270" to "#E0C18D",
            ).mapValues { (_, v) -> ColorRGBa.fromHex(v) }
        )

        // Init audio input
        val audio = Audio()
        val volProcessor = audio.createVolumeProcessor()
        audio.start()

        // Init render pipeline
        val renderPipeline = RenderPipeline(width, height, drawer)

        // Init Fx
        val lumaOpacity = LumaOpacity().autoUpdate {
            val eafSaturated = ebbAndFlow.saturate()
            backgroundOpacity = 0.0
            foregroundOpacity = eafSaturated
            backgroundLuma = eafSaturated.smoothstep(0.0, 1.0)
            foregroundLuma = eafSaturated.smoothstep(0.0, 1.0) / 2.0
        }
        val bloom = GaussianBloom().apply { window = 1 }
        val chromaticAberration = ChromaticAberration().autoUpdate {
            aberrationFactor = kick * 20.0
        }
        val verticalWave = VerticalWave().autoUpdate {
            amplitude = kick * 0.02
        }

        // Define visual groups
        val glitchGroup = object: VisualGroup(program) {

            val videoPlayer = VideoPlayerFFMPEG.fromFile(
                fileName="src/main/resources/videos/network_12716(1080p).mp4",
                configuration= VideoPlayerConfiguration().apply {
                    //useHardwareDecoding = false
                    //usePacketReaderThread = true
                }
            )

            init {
                videoPlayer.ended.listen { videoPlayer.restart() }
                videoPlayer.play()
            }

            var videoStatistics = videoPlayer.statistics

            override fun Drawer.draw() {
                videoPlayer.draw(this)

                videoStatistics = videoPlayer.statistics
            }
        }

        // Define controls for Input Scheme
        inputScheme.apply {
            // Tracked keys
            track(TOGGLE, "f1", "Toggle this controls display")
            // ...

            // Hard-coded input bindings
            keyDown {
                KEY_ESCAPE.bind("Exit Application") { audio.stop(); application.exit() }
                KEY_SPACEBAR.bind("Reset beat clock") { beatClock.resetTime() }
            }
        }

        // Init UI display
        val uiDisplay = UiDisplay(inputScheme).apply {
            trackValue("BPM") { "${beatClock.bpm}" }
            trackValue("FPS") { "${beatClock.fps}" }
            trackValue("Video Statistics") { "${glitchGroup.videoStatistics}" }
        }
        uiDisplay.alphaCap = LinearCapacitor(0.1, 0.5).autoUpdate {
            update(beatClock.deltaSeconds, inputScheme.isKeyActive("f1").not())
        }

        // extend(gui)

        val zoomVariations = CyclicFlag("Step by step", "Full Zoom")

        // Draw loop
        extend {
            drawer.isolated {
                shadeStyle = galaxyShadeStyle(seconds)
                val offset = when (zoomVariations.value) {
                    "Step by step" -> (beatClock.phase%32.0) / 8.0
                    "Full Zoom" -> (beatClock.phase%8.0).pow(4.0) / 200.0
                    else -> 0.0
                }
                val scl = 2.5 - 1.5 * ebbAndFlow.smoothstep(0.0, 1.5) + offset
                rectangle(drawer.bounds.scaledBy(scl))
            }

            renderPipeline.render {
                // Draw visual groups
                glitchGroup.draw()
            }

            // Draw controls
            uiDisplay.displayOnDrawer(drawer)
        }

        inputScheme.apply {

            keyDown {
                "z".bind("Next zoom variation") { zoomVariations.next() }
            }
        }

        // Set Fx chain
        renderPipeline.setFxChain {
            // Fx on drawBuffer
            lumaOpacity.apply(drawBuffer, drawBuffer)
            drawBuffer.copyTo(imageBuffer)
            // Fx on imageBuffer
            bloom.apply(imageBuffer, imageBuffer)
            verticalWave.apply(imageBuffer, imageBuffer)
            chromaticAberration.apply(imageBuffer, imageBuffer)
        }
    }
}
