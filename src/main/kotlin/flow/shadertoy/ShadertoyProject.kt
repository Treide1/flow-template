@file:Suppress("ClassName", "unused")

package flow.shadertoy

import flow.shadertoy.Shader.RenderStep.Input.Sampler
import flow.shadertoy.ShadertoyProject.ShadertoyTab.*
import flow.shadertoy.ShadertoyProject.ShadertoyTab.ChannelSettings.Channel
import flow.shadertoy.ShadertoyProject.ShadertoyTab.ChannelSettings.ChannelInput
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType

/**
 * Class to represent a Shadertoy channel.
 */
typealias ShadertoyChannel = Channel

/**
 * Class to represent a Shadertoy channel input.
 */
typealias ShadertoyChannelInput = ChannelInput

/**
 * Class to represent a [Shadertoy](https://www.shadertoy.com) project.
 *
 * It models the Shadertoy project as a set of tabs, each tab representing a shader with channels.
 *
 * The model is an intermediate representation that can be used to generate OPENRNDR GLSL 330 code.
 *
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
     * Represents a Shadertoy tab as seen in the Shadertoy editor.
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
         * Sets the [channel] to use the [input].
         */
        fun setInput(channel: ShadertoyChannel, input: ShadertoyChannelInput): ShadertoyTab {
            channelSettings.channelsMap[channel] = input
            return this
        }

        /**
         * Represents the settings for the four channels in a Shadertoy tab.
         */
        data class ChannelSettings(
            val channelsMap: MutableMap<Channel, ChannelInput> = mutableMapOf()
        ) {

            /**
             * Represents the channels 0 to 4 in a Shadertoy tab.
             */
            enum class Channel {
                CHANNEL_0,
                CHANNEL_1,
                CHANNEL_2,
                CHANNEL_3;

                override fun toString(): String = ordinal.toString()
            }

            /**
             * Represents the input for a channel in a Shadertoy tab.
             *
             * NOTE: Most inputs are not supported yet. Allowed input cytpes are 'buffer' and 'image'.
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

/**
 * Convenience function to chain calls on a [ShadertoyProject].
 */
fun <T: ShadertoyProject.ShadertoyTab> T.setCode(code: String): T {
    this.code = code
    return this
}

/**
 * Convenience function to chain calls on a [ShadertoyProject].
 */
fun <T: ShadertoyProject.ShadertoyTab> T.setSettings(settings: ChannelSettings): T {
    this.channelSettings = settings
    return this
}

