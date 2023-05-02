package flow.color

/**
 * Color repository.
 *
 * Defines a [palette] using a color model [ColorModel] of your choice, like ColorRGBa, ColorXSVa or ColorLABa.
 */
class ColorRepo<ColorModel> {

    /**
     * Palette of colors.
     */
    var palette = listOf<ColorModel>()

    // TODO-OPT: Define blend procedures, provide palette builders like from Adobe website
    //  (See Adobe Kuler color wheel: https://color.adobe.com/create/color-wheel)
}

fun <ColorModel> colorRepo(config: ColorRepo<ColorModel>.() -> Unit): ColorRepo<ColorModel> {
    return ColorRepo<ColorModel>().also { it.config() }
}


