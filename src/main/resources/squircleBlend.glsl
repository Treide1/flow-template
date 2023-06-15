#version 330

// Interface
in vec2 v_texCoord0;
uniform sampler2D tex0;
uniform sampler2D tex1;
out vec4 o_color;

// Required for the shader to work
uniform float blend;
#define C o_color
#define R textureSize(tex0, 0)
#define F v_texCoord0
#define iChannel0 tex0
#define iChannel1 tex1

float smootherstep (float edge0, float edge1, float x) {
    x = clamp ((x - edge0) / (edge1 - edge0), 0.0, 1.0);
    return x * x * x * (x * (x * 6. - 15.) + 10.);
}

// Filter from shadertoy: https://www.shadertoy.com/view/DtXGWf
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

uniform int mode;

void main() {
    float t = blend * 10./3. * 2; // smoothstep(0.0, pi * 2, blend * pi * 2);
    float r = R.x / R.y;
    vec2 uv = v_texCoord0;
    vec2 uvn =  (uv - 0.5) * 0.5;

    vec2 zuv = 33. * psin (0.3 * t) * uvn;
    int _mode = mode % 18;
    float l = 0.;
    if (0 == _mode) {
        l = length (psin (zuv));
    }
    else if (1 == _mode) {
        l = length (sin (zuv));
    }
    else if (2 == _mode) {
        l = length (pcos (zuv));
    }
    else if (3 == _mode) {
        l = length (cos (zuv));
    }
    else if (4 == _mode) {
        l = length (tan (zuv));
    }
    else if (5 == _mode) {
        l = length (sin (tan (zuv)));
    }
    else if (6 == _mode) {
        l = length (zuv - sin (zuv));
    }
    else if (7 == _mode) {
        l = length (zuv - psin (zuv));
    }
    else if (8 == _mode) {
        l = length (zuv - cos (zuv));
    }
    else if (9 == _mode) {
        l = length (zuv - pcos (zuv));
    }
    else if (10 == _mode) {
        l = length (zuv - tan (zuv));
    }
    else if (11 == _mode) {
        l = length (zuv - sin (tan (zuv)));
    }
    else if (12 == _mode) {
        l = length (zuv + sin (zuv));
    }
    else if (13 == _mode) {
        l = length (zuv + psin (zuv));
    }
    else if (14 == _mode) {
        l = length (zuv + cos (zuv));
    }
    else if (15 == _mode) {
        l = length (zuv + pcos (zuv));
    }
    else if (16 == _mode) {
        l = length (zuv + tan (zuv));
    }
    else if (17 == _mode) {
        l = length (zuv + sin (atan (zuv)));
    }
    float s = smoothstep (phi * r, -phi0 * r, l);

    // Creative deviation from original shader.
    vec2 tuv = mix (uv, vec2 (s), psin (0.3 * t));
    vec3 p0 = texture (iChannel0, 1. * tuv).rgb;
    vec3 p1 = texture (iChannel1, 1. * tuv).rgb;
    float mixFac = smootherstep (0.0, 1.0, blend);
    vec3 color = mix(p0, p1, mixFac);

    o_color = vec4(color, 1.0);
}