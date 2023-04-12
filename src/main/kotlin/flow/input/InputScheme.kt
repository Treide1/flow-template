package flow.input

import org.openrndr.KeyEvent
import org.openrndr.KeyEvents
import org.openrndr.Program

// TODO: Generalize to not just keyboard.keyDown (keyUp, ... / mouse, midi, ...)
class InputScheme(program: Program) {

    // Keybinds
    val keyBinder = KeyBinder()

    init {
        program.keyboard.keyDown.listen {
            keyBinder.handleKeyEvent(it)
        }
    }

    class KeyBinder {
        val bindList = mutableListOf<KeyBinding>()

        fun unbind(keyCode: Int) {
            bindList.removeIf { it.keyCode == keyCode }
        }

        fun handleKeyEvent(event: KeyEvent) {
            val options = listOf(event.key, event.name.hashCode())
            bindList.filter { it.keyCode in options }.forEach {
                it.action()
            }
            event.cancelPropagation()
        }

        fun unbind(keyName: String) = unbind(keyName.hashCode())

        fun Int.bind(action: () -> Unit) = bindList.add(KeyBinding(this, action))
        fun String.bind(action: () -> Unit) = this.hashCode().bind(action)
    }

    fun keyboardKeyDown(bindConfig: KeyBinder.() -> Unit) {
        keyBinder.bindConfig()
    }

    // Tracking
    // TODO: allow for isActive access
    fun KeyEvents.track(toggle: TrackTypes, vararg keyNames: String) {

    }

    fun KeyEvents.track(toggle: TrackTypes, keyNames: List<String>) {

    }

    /**
     * Track types for key events.
     * Defines what pressing behaviour turns a key "active" or "inactive", aka on/off.
     */
    enum class TrackTypes {
        /**
         * Pressing toggles active on/off.
         */
        TOGGLE,
        /**
         * Key is active as long as its pressed.
         */
        PIANO
    }

    // TODO: get the active state of the key
    fun isKeyActive(keyCode: Int) {

    }

    fun isKeyActive(keyName: String) {

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
fun Program.inputScheme(config: InputScheme.() -> Unit): InputScheme {
    return InputScheme(this).also { config(it) }
}
