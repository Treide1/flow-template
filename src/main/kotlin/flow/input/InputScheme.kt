@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package flow.input

import org.openrndr.KeyEvent
import org.openrndr.KeyEvents
import org.openrndr.Program
import java.lang.StringBuilder
import java.util.*

// TODO: Generalize to not just keyboard (mouse, midi, ...)
class InputScheme(keyEvents: KeyEvents) {

    // Pressed keys
    private val mutablePressedKeys = mutableSetOf<String>()
    // Set containing the names of the currently pressed keys
    val pressedKeys: Set<String> = mutablePressedKeys

    // Keybinds
    private val keyBinder = KeyBinder() // Keys to actions
    private val keyTracker = KeyTracker() // Keys with active state

    init {
        keyEvents.keyDown.listen { event ->
            mutablePressedKeys.add(event.name)
            keyBinder.handleKeyEvent(event)
            keyTracker.handleKeyDown(event)
            event.cancelPropagation()
        }
        keyEvents.keyUp.listen { event ->
            mutablePressedKeys.remove(event.name)
            keyTracker.handleKeyUp(event)
            event.cancelPropagation()
        }
    }

    /**
     * Class for holding a list of [KeyBinding] from key to action.
     * Keys can be bound with [Int.bind] and [String.bind] or unbound with [unbind], respectively.
     */
    class KeyBinder {
        val bindList = mutableListOf<KeyBinding>()
        val descriptionMap = mutableMapOf<String, String>()

        fun handleKeyEvent(event: KeyEvent) {
            val options = listOf(event.key, event.name.hashCode())
            bindList.filter { it.keyCode in options }.forEach {
                it.action()
            }
        }

        /**
         * Bind this key to an [action]. A [description] is required, which describes the keybind.
         */
        fun Int.bind(description: String, action: () -> Unit) {
            bindList.add(KeyBinding(this, action))
            val name = KeyCodes.nameByCode(this)
            descriptionMap[name] = description
        }

        /**
         * Bind this key to an [action]. A [description] is required, which describes the keybind.
         */
        fun String.bind(description: String, action: () -> Unit) {
            bindList.add(KeyBinding(this.hashCode(), action))
            descriptionMap[this] = description
        }

        /**
         * Unbind this key.
         */
        fun unbind(keyCode: Int) {
            bindList.removeIf { it.keyCode == keyCode }
            val name = KeyCodes.nameByCode(keyCode)
            descriptionMap.remove(name)
        }

        /**
         * Unbind this key.
         */
        fun unbind(keyName: String) {
            unbind(keyName.hashCode())
            descriptionMap.remove(keyName)
        }
    }

    fun keyDown(bindConfig: KeyBinder.() -> Unit) {
        keyBinder.bindConfig()
    }

    class KeyTracker {

        val pianoKeys = mutableMapOf<Int, Boolean>()
        val toggleKeys = mutableMapOf<Int, Boolean>()

        val pianoDescriptions = mutableMapOf<String, String>()
        val toggleDescriptions = mutableMapOf<String, String>()

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

    /**
     * Track a list of keys by their name, like `listOf("w","a","s","d")`.
     * @param trackType Define a [TrackTypes] for the keys at once.
     * @param keyNames List of key names to be tracked. If you are unsure about the name, use [printPressedKeys] (potentially layout specific).
     * @param description Description of the usage. Used for displaying controls.
     */
    fun track(trackType: TrackTypes, keyNames: List<String>, description: String) {
        val isNumbered = keyNames.size > 1
        when (trackType) {
            TrackTypes.PIANO -> {
                keyNames.forEachIndexed { i, keyName ->
                    keyTracker.pianoKeys[keyName.hashCode()] = false
                    keyTracker.pianoDescriptions[keyName] = description + if (isNumbered) " #$i" else ""
                }
            }
            TrackTypes.TOGGLE -> {
                keyNames.forEachIndexed { i, keyName ->
                    keyTracker.toggleKeys[keyName.hashCode()] = false
                    keyTracker.toggleDescriptions[keyName] = description + if (isNumbered) " #$i" else ""
                }
            }
        }
    }

    /**
     * Track a key by its name, like `"k"`.
     * @param trackType Define a [TrackTypes] for the key to use.
     * @param keyName Key name to be tracked. If you are unsure about the name, use [printPressedKeys] (potentially layout specific).
     * @param description Description of the usage. Used for displaying controls.
     */
    fun track(trackType: TrackTypes, keyName: String, description: String) {
        track(trackType, listOf(keyName), description)
    }

    /**
     * Track a key by its code from [KeyCodes], like `KeyCodes.KEY_ESCAPE`.
     * @param trackType Define a [TrackTypes] for the key to use.
     * @param keyCode Key code to be tracked.
     * @param description Description of the usage. Used for displaying controls.
     */
    fun track(trackType: TrackTypes, keyCode: Int, description: String) {
        val keyName = KeyCodes.nameByCode(keyCode)
        track(trackType, keyName, description)
    }

    // TODO: add "untrack", incl update of description

    /**
     * Returns true if the key is currently active. "Active" is defined by the track type.
     */
    fun isKeyActive(keyCode: Int): Boolean {
        return keyTracker.isActive(keyCode)
    }

    /**
     * Returns true if the key is currently active. "Active" is defined by the track type.
     */
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

    /**
     * Key codes for special key events.
     */
    enum class KeyCodes(val code: Int) {
        KEY_SPACEBAR(32),

        KEY_ESCAPE(256),
        KEY_ENTER(257),

        KEY_TAB(258),
        KEY_BACKSPACE(259),
        KEY_INSERT(260),
        KEY_DELETE(261),
        KEY_ARROW_RIGHT(262),
        KEY_ARROW_LEFT(263),
        KEY_ARROW_DOWN(264),
        KEY_ARROW_UP(265),
        KEY_PAGE_UP(266),
        KEY_PAGE_DOWN(267),
        KEY_HOME(268),
        KEY_END(269),

        KEY_CAPSLOCK(280),
        KEY_PRINT_SCREEN(283),

        KEY_F1(290),
        KEY_F2(291),
        KEY_F3(292),
        KEY_F4(293),
        KEY_F5(294),
        KEY_F6(295),
        KEY_F7(296),
        KEY_F8(297),
        KEY_F9(298),
        KEY_F10(299),
        KEY_F11(300),
        KEY_F12(301),

        KEY_LEFT_SHIFT(340),
        KEY_RIGHT_SHIFT(344),
        ;

        companion object {
            /**
             * Get a [KeyCodes] by its code.
             */
            fun findByCode(code: Int): KeyCodes? {
                return values().firstOrNull { it.code == code }
            }

            /**
             * Finds the [KeyCodes] by its code and returns its name.
             */
            fun nameByCode(code: Int): String {
                return findByCode(code)?.name ?: "UNKNOWN_KEY"
            }
        }
    }

    /**
     * Get a pretty string with all key controls and their descriptions.
     */
    fun getControlsText(): String {
        val sb = StringBuilder()
        // Key bindings
        sb.appendLine("Key bindings:")
        keyBinder.descriptionMap.entries.forEach { (nameList, description) ->
            sb.appendControlLine(nameList, description)
        }

        // Key tracking for Piano
        sb.appendLine("Key tracking (Piano):")
        keyTracker.pianoDescriptions.entries.forEach { (nameList, description) ->
            sb.appendControlLine(nameList, description)
        }

        // Key tracking for Toggle
        sb.appendLine("Key tracking (Toggle):")
        keyTracker.toggleDescriptions.entries.forEach { (nameList, description) ->
            sb.appendControlLine(nameList, description)
        }

        // Build and return
        return sb.toString()
    }

    private fun StringBuilder.appendControlLine(nameList: String, description: String) {
        val indent = "  "
        this.append(indent)
        this.appendLine("${nameList.uppercase(Locale.getDefault())} -> $description")
    }

    /**
     * Print the key controls to console.
     */
    fun printControls() {
       println(getControlsText())
    }

    /**
     * Prints all currently pressed keys to console.
     */
    fun printPressedKeys() {
        println("Keys pressed: $pressedKeys")
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
fun inputScheme(keyEvents: KeyEvents, config: InputScheme.() -> Unit = {}): InputScheme {
    return InputScheme(keyEvents).also { config(it) }
}
