package flow.colorRepo

/**
 * TODO: Color Repository.
 *
 * Start with `val colorRepo = extend(ColorRepo()) { … }` and configure your color repository.
 *
 * Define a [palette].
 */
class ColorRepo<M>(config: ColorRepo<M>.() -> Unit) {


    /**
     * Palette of colors.
     */
    var palette = listOf<M>()

    init {
        config()
    }

}


