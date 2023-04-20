package flow.colorRepo

/**
 * Color repository.
 *
 * Defines a [palette] within a color model [ColorModel] of your choice, like ColorRGBa or ColorXSVa.
 *
 * @constructor Create empty Color repo, configure with the `config` block.
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


