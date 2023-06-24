@file:Suppress("CanBeParameter", "MemberVisibilityCanBePrivate")

package flow.shadertoy

import flow.autoupdate.AutoUpdate.autoUpdate
import flow.shadertoy.ShadertoyProject.ShadertoyTab
import flow.shadertoy.ShadertoyProject.ShadertoyTab.ChannelSettings.ChannelInput.*
import org.openrndr.MouseButton
import org.openrndr.MouseEvents
import org.openrndr.Program
import org.openrndr.draw.*
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import kotlin.reflect.KProperty

/**
 * A renderer for a [ShadertoyProject].
 *
 * Parses the [project] to a render procedure. Call [render] to produce the result.
 */
abstract class ProjectRenderer(val program: Program) {

    /**
     * The function that initializes the [project].
     */
    abstract fun importProject(): ShadertoyProject

    /**
     * The project to render.
     */
    val project by lazy { importProject() }

    /**
     * The fork for the parameters of the project. All set values are passed to every shader.
     */
    val parameters = ParameterFork()

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
    /**
     * Shadertoy uniform: The viewport resolution in pixels (z is pixel aspect, usually 1.0)
     */
    var iResolution: Vector3 by parameters

    /**
     * Shadertoy uniform: The current time in seconds
     */
    var iTime: Double by parameters

    /**
     * Shadertoy uniform: The time difference between current [iTime] and the value from previous frame
     */
    var iTimeDelta: Double by parameters

    /**
     * Shadertoy uniform: The current frame index
     */
    var iFrame: Double by parameters

    /**
     * Shadertoy uniform: The mouse pixel coords. xy: current (if MLB down), zw: click
     */
    var iMouse: Vector4 by parameters

    /**
     * color buffer to render to
     */
    val imageBuffer = colorBuffer(
        program.width,
        program.height,
        format = ColorFormat.RGBa,
        type = ColorType.FLOAT16,
    )

    /**
     * A map of all buffers used in the project.
     */
    val bufferMap = mutableMapOf<ShadertoyTab, ColorBuffer>(
        project.image to imageBuffer
    )

    /**
     * The order in which the passes are rendered.
     */
    val passOrder = mutableListOf<Step>()

    /**
     * A step represents a single [filter] process, rendering to [buffer] using [channels] as inputs.
     */
    data class Step(
        val filter: ShadertoyFilter,
        val buffer: ColorBuffer,
        val channels: Map<Int, ColorBuffer>
    )

    // Init steps and add them to the pass order
    // This also adds the filters to the parameters fork
    init {
        // Create buffers for all tabs that are not the image
        val usedTabs = listOfNotNull(
            project.bufferA,
            project.bufferB,
            project.bufferC,
            project.bufferD,
            project.cubeA,
            project.sound,
        )
        usedTabs.forEach { bufferMap[it] = imageBuffer.createEquivalent() }

        // Create steps for (usedTabs + image)
        (usedTabs + project.image).forEach { tab ->
            val filter = ShadertoyFilter(tab).also { parameters.addFilter(it) }
            val buffer = bufferMap[tab]!!
            val channels = tab.getOrderedChannels().associate { (channel, channelInput) ->
                channel.ordinal to channelInput.asColorBuffer()
            }
            passOrder.add(Step(filter, buffer, channels))
        }
    }

    // Set the uniforms for all filters
    init {
        iResolution = Vector3(program.width.toDouble(), program.height.toDouble(), 1.0)
        iTime = 0.0
        iTimeDelta = 0.0015
        iFrame = 0.0
        iMouse = Vector4(0.0, 0.0, 0.0, 0.0)

        // Use autoUpdate to update uniforms
        autoUpdate {
            // iResolution does not need to be updated
            iTimeDelta = program.seconds - iTime
            iTime = program.seconds
            iFrame = program.frameCount.toDouble()
            // iMouse updated below
        }

        // Listen to mouse events and pass to uniform iMouse
        program.mouse.iMouseListen()
    }

    /**
     * Renders the project to the [imageBuffer] and returns it.
     */
    fun render(): ColorBuffer {
        parameters

        passOrder.forEach {
            val from = it.channels.map { (_, buffer) -> buffer }.toTypedArray()
            val to = it.buffer
            it.filter.apply(from, to)
        }
        return imageBuffer
    }

    ////////////////////////////////////////////////////////////////////////////////

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

    // Mouse events are converted to iMouse
    private fun MouseEvents.iMouseListen() {
        fun Double.flipY() = iResolution.y - this

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
 * Tracks parameters just like [Filter.parameters] and can be accessed the same way.
 * In addition, the values are not set-only anymore, but can be read as well.
 *
 * Those parameters are set for all filters that are added to this fork via [addFilter].
 */
class ParameterFork {

    // Map of parameter names to values
    private val map = mutableMapOf<String, Any>()

    // List of filters that receive the parameters of this fork
    private val filterList = mutableListOf<Filter>()

    /**
     * Gets value from map.
     */
    operator fun <T: Any> getValue(requester: Any?, property: KProperty<*>): T {
        return map[property.name] as T
    }

    /**
     * Puts value into map and sets it for all filters.
     */
    operator fun <T: Any> setValue(requester: Any?, property: KProperty<*>, value: T) {
        map[property.name] = value
        filterList.forEach {
            it.parameters[property.name] = value
        }
    }

    /**
     * Adds a [Filter] to receive the parameters of this fork.
     */
    fun addFilter(filter: ShadertoyFilter) {
        filterList.add(filter)
    }
}

/**
 * Filter class for the code from a shadertoy tab that has been converted to OPENRNDR GLSL 330 code.
 *
 * The common shadertoy uniforms are available as parameters.
 */
class ShadertoyFilter(
    val addedTab: ShadertoyTab
) : Filter(filterShaderFromCode(addedTab.code, addedTab.name, includeShaderConfiguration = false))
