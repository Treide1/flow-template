@file:Suppress("unused")

package flow.color

import flow.color.ColorRepo.ColorRoles.*
import org.openrndr.color.ColorModel
import org.openrndr.color.ColorRGBa

/**
 * Color repository.
 *
 * Defines a [palette] using a color model [ColorModel] of your choice, like ColorRGBa, ColorXSVa or ColorLABa.
 * @param palette The palette of colors.
 */
class ColorRepo<T: ColorModel<T>>(var palette: Map<String, T> = mapOf()) {

    operator fun get(colorName: String): T {
        return palette[colorName] ?: throw IllegalArgumentException("Color name $colorName not found in palette.")
    }

    operator fun get(colorRole: ColorRoles): T {
        return this[colorRole.name]
    }

    operator fun get(index: Int): T {
        return palette.values.toList()[index]
    }

    /**
     * Enumeration of common color roles.
     */
    enum class ColorRoles {
        PRIMARY,
        PRIMARY_VARIANT,
        SECONDARY,
        SECONDARY_VARIANT,
        TERTIARY,
        TERTIARY_VARIANT,

        CONTRARY,
        ACCENT,
        NEUTRAL,
        BACKGROUND,
    }

    companion object {

        /**
         * The palette of colors used in the demo. Uses turquoise, yellow and purple.
         */
        val DEMO_PALETTE = mapOf(
            PRIMARY.name   to "#90F0CB",
            SECONDARY.name to "#A38641",
            TERTIARY.name  to "#CE60F0",
        ).mapValues{ (_, hex) -> ColorRGBa.fromHex(hex).toXSVa() }

        /**
         *
         */
        fun fromHexMapOf(vararg pairs: Pair<String, String>): ColorRepo<ColorRGBa> {
            return ColorRepo(
                mapOf(*pairs).mapValues { (_, hex) ->
                    ColorRGBa.fromHex(hex)
                }
            )
        }
    }

    // TODO-OPT: Define blend procedures, provide palette builders like from Adobe website
    //  (See Adobe Kuler color wheel: https://color.adobe.com/create/color-wheel)
}


