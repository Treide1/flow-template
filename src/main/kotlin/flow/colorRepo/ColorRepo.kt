package flow.colorRepo

/**
 * Color repository.
 *
 * Defines a [palette] using a color model [ColorModel] of your choice, like ColorRGBa or ColorXSVa.
 *
 * @param config Configuration block for the Color repo.
 */
class ColorRepo<ColorModel>(config: ColorRepo<ColorModel>.() -> Unit) {

    /**
     * Palette of colors.
     */
    var palette = listOf<ColorModel>()

    init {
        config()
    }

    // TODO-OPT: Define blend procedures, provide palette builders like from Adobe website
    //  (See Adobe Kuler color wheel: https://color.adobe.com/create/color-wheel)
}


