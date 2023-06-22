package flow.shadertoy

import flow.autoupdate.AutoUpdate.autoUpdate
import flow.shadertoy.ShadertoyProject.ShadertoyTab
import flow.shadertoy.ShadertoyProject.ShadertoyTab.ChannelSettings.ChannelInput.*
import org.openrndr.MouseButton
import org.openrndr.Program
import org.openrndr.draw.*
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4

/**
 *
 */
abstract class ProjectRenderer(val program: Program, val project: ShadertoyProject) {

    // Buffers
    val imageBuffer = colorBuffer(
        program.width,
        program.height,
        format = ColorFormat.RGBa,
        type = ColorType.FLOAT16,
    )


    // Util
    val w = program.width.toDouble()
    val h = program.height.toDouble()

    // Commons from shadertoy
    /*
     * uniform vec3 iResolution;
     * uniform float iTime;
     * uniform float iTimeDelta;
     * uniform float iFrame;
     * uniform float iChannelTime[4]; // Currently unsupported
     * uniform vec4 iMouse;
     * uniform vec4 iDate; // Currently unsupported
     * uniform float iSampleRate; // Currently unsupported
     * uniform vec3 iChannelResolution[4]; // Currently unsupported
     * uniform samplerXX iChannelY; // Current version only supports sampler2D for color buffers
     */
    var iResolution = Vector3(w, h, 1.0)
    var iTime = 0.0
    var iTimeDelta = 0.0015
    var iFrame = 0.0
    var iMouse = Vector4(0.0, 0.0, 0.0, 0.0)

    /**
     *
     */
    val bufferMap = mutableMapOf<ShadertoyTab, ColorBuffer>()

    /**
     *
     */
    val passOrder = mutableListOf<Step>()

    /**
     *
     */
    data class Step(
        val filter: ShadertoyFilter,
        val buffer: ColorBuffer,
        val channels: Map<Int, ColorBuffer>
    )

    init {
        val usedTabs = listOfNotNull(
            project.bufferA,
            project.bufferB,
            project.bufferC,
            project.bufferD,
            project.cubeA,
            project.sound,
        )
        // Create other buffers
        usedTabs.forEach { bufferMap[it] = imageBuffer.createEquivalent() }

        // Create steps
        usedTabs.forEach { tab ->
            val filter = ShadertoyFilter(tab.code, tab.name, program)
            val buffer = bufferMap[tab]!!
            val channels = tab.getOrderedChannels().associate { (channel, channelInput) ->
                channel.ordinal to channelInput.asColorBuffer()
            }
            passOrder.add(Step(filter, buffer, channels))
        }
        passOrder.add(Step(
            ShadertoyFilter(project.image.code, project.image.name, program),
            imageBuffer,
            project.image.getOrderedChannels().associate { (channel, channelInput) ->
                channel.ordinal to channelInput.asColorBuffer()
            }
        ))
    }

    // Conversion function to map inputs to color buffers
    private fun ShadertoyChannelInput.asColorBuffer(): ColorBuffer {
        return when (this) {
            is BUFFER_A_IN -> bufferMap[project.bufferA as ShadertoyTab]!!
            is BUFFER_B_IN -> bufferMap[project.bufferB as ShadertoyTab]!!
            is BUFFER_C_IN -> bufferMap[project.bufferC as ShadertoyTab]!!
            is BUFFER_D_IN -> bufferMap[project.bufferD as ShadertoyTab]!!
            else -> TODO("Not implemented for non-buffer inputs")
        }
    }

    init {
        fun Double.flipY() = h - this

        autoUpdate {
            // iResolution does not need to be updated
            iTimeDelta = program.seconds - iTime
            iTime = program.seconds
            iFrame = program.frameCount.toDouble()
            // iMouse updated below
        }
        program.mouse.apply {
            buttonDown.listen {
                if (it.button != MouseButton.LEFT) return@listen

                iMouse = iMouse.copy(
                    x = it.position.x, y = it.position.y.flipY(),
                    z = it.position.x, w = it.position.y.flipY()
                )

            }
            dragged.listen {
                if (it.button != MouseButton.LEFT) return@listen

                iMouse = iMouse.copy(
                    x = it.position.x, y = it.position.y.flipY(),
                    w = iMouse.w.let { w -> if (w > 0.0) -w else w}
                )
            }
            buttonUp.listen {
                if (it.button != MouseButton.LEFT) return@listen

                iMouse = iMouse.copy(
                    z = -iMouse.z
                )
            }
        }
    }

    /**
     *
     */
    fun render(): ColorBuffer {
        passOrder.forEach {
            it.filter.iResolution = iResolution
            it.filter.iTime = iTime
            it.filter.iTimeDelta = iTimeDelta
            it.filter.iFrame = iFrame
            it.filter.iMouse = iMouse

            val from = it.channels.map { (_, buffer) -> buffer }.toTypedArray()
            val to = it.buffer
            it.filter.apply(from, to)
        }
        return imageBuffer //imagePass.buffer
    }
}

/**
 *
 */
class ShadertoyFilter(
    val shaderCode: String,
    val name: String,
    program: Program,
) : Filter(filterShaderFromCode(shaderCode, name, includeShaderConfiguration = false)) {

    val w = program.width.toDouble()
    val h = program.height.toDouble()

    var iResolution by parameters
    var iTime by parameters
    var iTimeDelta by parameters
    var iFrame by parameters
    var iMouse by parameters

    init {
        iResolution = Vector3(w, h, 1.0)
        iTime = 0.0
        iTimeDelta = 0.0015
        iFrame = 0.0
        iMouse = Vector4(0.0, 0.0, 0.0, 0.0)
    }
}
