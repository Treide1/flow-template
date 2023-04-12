package flow.colorRepo

/**
 * Color repository.
 *
 * Defines a [palette] within a color model of your choice, like ColorXSVa.
 *
 * @constructor Create empty Color repo, configure with the [config] lambda.
 */
class ColorRepo<ColorModel>(config: ColorRepo<ColorModel>.() -> Unit) {

    /**
     * Palette of colors.
     */
    var palette = listOf<ColorModel>()

    init {
        config()
    }

    // OPT: Define blend procedures, provide palette builders like from Adobe website
    // (See Adobe Kuler color wheel: https://color.adobe.com/create/color-wheel)
}


