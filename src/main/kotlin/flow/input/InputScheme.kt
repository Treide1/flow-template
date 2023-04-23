@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package flow.input

import org.openrndr.KeyEvent
import org.openrndr.KeyEvents
import org.openrndr.Program

// TODO: Generalize to not just keyboard (mouse, midi, ...)
class InputScheme(keyEvents: KeyEvents) {

    // Keybinds
    val keyBinder = KeyBinder()

    val keyTracker = KeyTracker()

    init {
        keyEvents.keyDown.listen { event ->
            keyBinder.handleKeyEvent(event)
            keyTracker.handleKeyDown(event)
            event.cancelPropagation()
        }
        keyEvents.keyUp.listen { event ->
            keyTracker.handleKeyUp(event)
            event.cancelPropagation()
        }
    }

    class KeyBinder {
        val bindList = mutableListOf<KeyBinding>()

        fun handleKeyEvent(event: KeyEvent) {
            val options = listOf(event.key, event.name.hashCode())
            bindList.filter { it.keyCode in options }.forEach {
                it.action()
            }
        }

        fun Int.bind(action: () -> Unit) = bindList.add(KeyBinding(this, action))
        fun String.bind(action: () -> Unit) = this.hashCode().bind(action)

        fun unbind(keyCode: Int) = bindList.removeIf { it.keyCode == keyCode }

        fun unbind(keyName: String) = unbind(keyName.hashCode())
    }

    fun keyboardKeyDown(bindConfig: KeyBinder.() -> Unit) {
        keyBinder.bindConfig()
    }

    class KeyTracker {

        val pianoKeys = mutableMapOf<Int, Boolean>()
        val toggleKeys = mutableMapOf<Int, Boolean>()

        fun handleKeyDown(event: KeyEvent) {
            val options = listOf(event.key, event.name.hashCode())
            options.forEach { i ->
                if (pianoKeys.containsKey(i)) {
                    pianoKeys[i] = true
                }
                if (toggleKeys.containsKey(i)) {
                    toggleKeys[i] = !toggleKeys[i]!!
                }
            }
        }

        fun handleKeyUp(event: KeyEvent) {
            val options = listOf(event.key, event.name.hashCode())
            options.forEach { i ->
                if (pianoKeys.containsKey(i)) {
                    pianoKeys[i] = false
                }
            }
        }

        fun isActive(keyCode: Int): Boolean {
            return (pianoKeys[keyCode] ?: false) || (toggleKeys[keyCode] ?: false)
        }
    }

    // Tracking
    // TODO: allow for isActive access

    fun track(trackType: TrackTypes, keyNames: List<String>) {
        when (trackType) {
            TrackTypes.PIANO -> {
                keyNames.forEach { keyName ->
                    keyTracker.pianoKeys[keyName.hashCode()] = false
                }
            }
            TrackTypes.TOGGLE -> {
                keyNames.forEach { keyName ->
                    keyTracker.toggleKeys[keyName.hashCode()] = false
                }
            }

        }
    }

    fun track(trackType: TrackTypes, vararg keyNames: String) {
        track(trackType, keyNames.toList())
    }

    fun isKeyActive(keyCode: Int): Boolean {
        return keyTracker.isActive(keyCode)
    }

    fun isKeyActive(keyName: String): Boolean {
        return isKeyActive(keyName.hashCode())
    }

    /**
     * Track types for key events.
     * Defines what pressing behaviour turns a key "active" or "inactive", aka on/off.
     */
    enum class TrackTypes {
        /**
         * Key is active as long as its pressed.
         */
        PIANO,
        /**
         * Pressing toggles active on/off.
         */
        TOGGLE,
    }

}

/**
 * Key binding class. Holds a [keyCode] as either key const for KeyEvent.key
 * or a string (as hashcode) for KeyEvent.name.
 * The press of that key is bound to the [action] function.
 */
data class KeyBinding(val keyCode: Int, val action: () -> Unit)

/**
 * Setup of an [InputScheme] for a [Program].
 * Configure it with the [config] lambda.
 */
fun inputScheme(keyEvents: KeyEvents, config: InputScheme.() -> Unit): InputScheme {
    return InputScheme(keyEvents).also { config(it) }
}
