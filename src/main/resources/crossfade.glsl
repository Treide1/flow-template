#version 330

// Interface
in vec2 v_texCoord0;
uniform sampler2D tex0;
uniform sampler2D tex1;
out vec4 o_color;

uniform float blend;

void main()
{
    vec3 c0 = texture(tex0, v_texCoord0).rgb;
    vec3 c1 = texture(tex1, v_texCoord0).rgb;
    o_color = vec4(mix(c0, c1, blend), 1.0);
}
