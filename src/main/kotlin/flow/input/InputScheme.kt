package flow.input

import org.openrndr.KeyEvents

class InputScheme {

    class KeyBinder {
        val binds = mutableListOf<KeyBinding>()

        fun unbind(keyCode: Int) {
            binds.removeIf { it.keyCode == keyCode }
        }

        fun handleKeyDown(keyCode: Int) {
            binds.filter { it.keyCode == keyCode }.forEach { it.action() }
        }

        //fun bind(keyString: String, action: () -> Unit) = bind(keyString.hashCode(), action)
        fun unbind(keyString: String) = unbind(keyString.hashCode())
        fun handleKeyDown(keyString: String) = handleKeyDown(keyString.hashCode())

        fun Int.bind(action: () -> Unit) = binds.add(KeyBinding(this, action))
        fun String.bind(action: () -> Unit) = this.hashCode().bind(action)
    }

    fun KeyEvents.keyDown(bindConfig: InputScheme.KeyBinder.() -> Unit) {

    }

}

/**
 * Key binding class. Holds a [keyCode] as either key const for KeyEvent.key
 * or a string (as hashcode) for KeyEvent.name.
 * The press of that key is bound to the [action] function.
 */
data class KeyBinding(val keyCode: Int, val action: () -> Unit)

fun inputScheme(config: InputScheme.() -> Unit): InputScheme {
    return InputScheme().also { config(it) }
}