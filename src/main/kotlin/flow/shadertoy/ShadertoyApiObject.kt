package flow.shadertoy

// TODO: use for ProjectImporter's importFromApi() method

data class ShadertoyApiObject(var shader: Shader = Shader())

data class Shader(
    var ver: String = "0.1",
    var info: Info = Info(),
    var renderpass: List<RenderStep> = listOf()
) {

    data class Info(
        var id: String = "",
        var date: String = "",
        var viewed: Int = 0,
        var name: String = "",
        var username: String = "",
        var description: String = "",
        var likes: Int = 0,
        var published: Int = 0,
        var flags: Int = 0,
        var usePreview: Int = 0,
        var tags: List<String> = listOf(),
        var hasliked: Int = 0
    )

    data class RenderStep(
        var inputs: List<Input> = listOf(),
        var outputs: List<Output> = listOf(),
        var code: String = "",
        var name: String = "",
        var description: String = "",
        var type: String = "",
    ) {

        data class Input(
            var id: Int,
            var src: String = "",
            var ctype: String,
            var channel: Int,
            var sampler: Sampler = Sampler(),
            var published: Int = 1
        ) {

            data class Sampler(
                var filter: String = "linear",
                var wrap: String = "clamp",
                var vflip: String = "true",
                var srgb: String = "false",
                var internal: String = "byte",
            )
        }

        data class Output(
            var id: Int,
            var channel: Int,
        )
    }
}
