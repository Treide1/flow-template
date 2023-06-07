#version 330

// Interface
in vec2 v_texCoord0;
uniform sampler2D tex0;
uniform sampler2D tex1;
out vec4 o_color;

uniform float blend;

void main()
{
    o_color = (1.0 - blend) * texture(tex0, v_texCoord0) + blend * texture(tex1, v_texCoord0);
}
