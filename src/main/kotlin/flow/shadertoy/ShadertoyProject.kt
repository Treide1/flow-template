@file:Suppress("ClassName", "unused")

package flow.shadertoy

import flow.shadertoy.Shader.RenderStep.Input.Sampler
import flow.shadertoy.ShadertoyProject.ShadertoyTab.*
import flow.shadertoy.ShadertoyProject.ShadertoyTab.ChannelSettings.Channel
import flow.shadertoy.ShadertoyProject.ShadertoyTab.ChannelSettings.ChannelInput
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType

typealias ShadertoyChannel = Channel
typealias ShadertoyChannelInput = ChannelInput

/**
 * For Shadertoy import discussion, see:<br>
 * [Discourse](https://openrndr.discourse.group/t/from-shadertoy-to-openrndr/85)
 */
class ShadertoyProject(
    val name: String,
    val description: String = "",
    var common: Common? = null,
    var image: Image = Image(),
    var bufferA: BufferA? = null,
    var bufferB: BufferB? = null,
    var bufferC: BufferC? = null,
    var bufferD: BufferD? = null,
    var cubeA: CubeA? = null,
    var sound: Sound? = null,
) {

    /**
     *
     */
    sealed class ShadertoyTab(
        val name: String,
        val type: String = "UNSUPPORTED",
        val outputId: Int? = null,
        var code: String = "// No code added",
        var channelSettings: ChannelSettings = ChannelSettings(),
    ) {
        class Common: ShadertoyTab("Common", "common", outputId = null)
        class Image: ShadertoyTab("Image", "image", outputId = null) // Output implicit, not part of definition
        class BufferA: ShadertoyTab("Buffer A", "buffer", outputId = 257)
        class BufferB: ShadertoyTab("Buffer B", "buffer", outputId = 258)
        class BufferC: ShadertoyTab("Buffer C", "buffer", outputId = 259)
        class BufferD: ShadertoyTab("Buffer D", "buffer", outputId = 260)
        class CubeA: ShadertoyTab("Cube A")
        class Sound: ShadertoyTab("Sound")

        override fun toString(): String {
            return "${name}(type=${type}, outputId=${outputId}, code='${code.substring(0, 10)}...')"
        }

        /**
         *
         */
        fun setInput(channel: ShadertoyChannel, input: ShadertoyChannelInput): ShadertoyTab {
            channelSettings.channelsMap[channel] = input
            return this
        }

        /**
         * 0..3 channels with a possible binding to an input buffers/textures/etc.,
         * and sampling representation linear, repeated, RGBa, 16B float
         */
        data class ChannelSettings(
            val channelsMap: MutableMap<Channel, ChannelInput> = mutableMapOf()
        ) {

            /**
             *
             */
            enum class Channel {
                CHANNEL_0,
                CHANNEL_1,
                CHANNEL_2,
                CHANNEL_3;

                override fun toString(): String = ordinal.toString()
            }

            /**
             *
             */
            sealed class ChannelInput(val id: Int, val ctype: String = "UNSUPPORTED", var sampler: Sampler = Sampler()) {
                object KEYBOARD: ChannelInput(-1) // actually (33, "keyboard") but currently unsupported
                object WEBCAM: ChannelInput(-1)
                object MICROPHONE: ChannelInput(-1)
                object SOUNDCLOUD: ChannelInput(-1)
                object BUFFER_A_IN: ChannelInput(257, "buffer")
                object BUFFER_B_IN: ChannelInput(258, "buffer")
                object BUFFER_C_IN: ChannelInput(259, "buffer")
                object BUFFER_D_IN: ChannelInput(260, "buffer")
                object CUBE_A_IN: ChannelInput(-1)
                data class TEXTURE(
                    val name: String, val width: Int, val height: Int,
                    val colorFormat: ColorFormat = ColorFormat.RGB, val colorType: ColorType = ColorType.UINT8
                ): ChannelInput(-1) // example: (30, "texture")
                data class CUBEMAP(
                    val name: String, val width: Int, val height: Int,
                    val colorFormat: ColorFormat = ColorFormat.RGB, val colorType: ColorType = ColorType.UINT8
                ): ChannelInput(-1)
                data class VOLUME(
                    val name: String, val width: Int = 32, val height: Int = 32, val depth: Int = 32,
                    val colorFormat: ColorFormat = ColorFormat.RGB, val colorType: ColorType
                ): ChannelInput(-1)
                data class VIDEO(
                    val name: String, val width: Int, val height: Int,
                    val colorFormat: ColorFormat = ColorFormat.RGB, val colorType: ColorType = ColorType.UINT8,
                    val fps: Int, val length: Int
                ): ChannelInput(-1)
                data class MUSIC(
                    val name: String, val sampleRate: Int = 44100, val channels: Int = 2,
                    val encoding: ColorType = ColorType.SINT16_INT, val length: Int
                ): ChannelInput(-1)

                override fun toString(): String = "${this::class.java}(id=$id)"

                companion object {
                    fun getById(id: Int): ChannelInput {
                        return when (id) {
                            257 -> BUFFER_A_IN
                            258 -> BUFFER_B_IN
                            259 -> BUFFER_C_IN
                            260 -> BUFFER_D_IN
                            else -> throw IllegalArgumentException("Unsupported channel input id: $id")
                        }
                    }
                }
            }
        }
    }
}

fun <T: ShadertoyProject.ShadertoyTab> T.setCode(code: String): T {
    this.code = code
    return this
}

fun <T: ShadertoyProject.ShadertoyTab> T.setSettings(settings: ChannelSettings): T {
    this.channelSettings = settings
    return this
}

