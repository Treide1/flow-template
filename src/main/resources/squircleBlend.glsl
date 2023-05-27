#version 330

// Interface
in vec2 v_texCoord0;
uniform sampler2D tex0;
uniform sampler2D tex1;
out vec4 o_color;

// Required for the shader to work
uniform float time;
uniform float volume;
#define iTime time
#define C o_color
#define R textureSize(tex0, 0)
#define F v_texCoord0

// Filter from shadertoy: https://www.shadertoy.com/view/mtX3WX
const float pi = 355. / 113.;
const float halfpi = 0.5 * pi;
const float phi = 0.5 * (1. + sqrt (5.));
const float phi0 = phi - 1.;
const float phi0i = 1. - phi0;

// Normalized sin and cos,
// Remapped sin: psin(0) = 0, psin(1) = 1
float psin (float t) {
    return 0.5 * (1. + sin (pi * t - halfpi));
}
vec2 psin (vec2 t) {
    return 0.5 * (1. + sin (pi * t - halfpi));
}
// Remapped cos: pcos(0) = 1, pcos(1) = 0
float pcos (float t) {
    return 0.5 * (1. + cos (pi * t));
}
vec2 pcos (vec2 t) {
    return 0.5 * (1. + cos (pi * t));
}

void main() {
    float t = iTime;
    float r = R.x / R.y;
    vec2 uv = v_texCoord0;
    vec2 uvn = uv - (0.5 * R.yx) / max (R.x, R.y);

    float k = 1. * volume; //texture (iChannel2, vec2 (0.09, 0.)).r;
    k = 2.1 * cos (k * halfpi);

    vec2 zuv = 9.* r * uvn; //33. * psin (0.5 * t) * uvn;
    float t_mode = 0.0 * t;
    int mode = int (floor (mod (t_mode, 18.)));
    float l = 0.;
    l = max (l, length (zuv + sin (k*zuv)));
    l = mix (l, length (zuv - sin (zuv)), psin (2.*t));// 0.1*k); //k*0.8);//psin (t));
    l = mix (l, k*length (tan (zuv)), k-0.2); //k);//sin (1.*t));

    float s = smoothstep (phi * 2. * r, -phi0 * r, l);

    vec2 tuv = mix (uvn, vec2 (s), 0.777);//pcos (1.*t));
    vec3 color = vec3 (0);
    vec3 bg = vec3 (0.0);
    bg = texture(tex0, 1. * uvn).rgb;
    bg = mix (bg, pow (s, .8) * (1.-vec3 (0.1, 1.0, 0.1)), 0.888);
    vec3 fg = vec3 (0.0, 0.5, 0.0);
    fg = texture(tex1, 1.5 * tuv).rgb;
    color += (1. - s) * bg;
    color += (0. + s) * fg;

    C = vec4(color, 1.0);
}