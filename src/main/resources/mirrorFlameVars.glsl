#version 330

// Interface
in vec2 v_texCoord0;
uniform sampler2D tex0;
out vec4 o_color;

// ----------------------------------------------
// Parameters:
// Takes a stencil buffer that represents the id for the function to be applied
// NOTE: It is sampled as a uint (that has a fixed 32 bit precision) into which float values are written.
//       This is done to retrieve them as bit-perfect floats 0.0, 1.0, 2.0, etc.
uniform usampler2D stencil;
// Takes the number of iterations to run
uniform int iterCount;
// Takes a y scale parameter, assuming width to go from -1 to 1
uniform float yScl;

// ----------------------------------------------
// Helper functions
vec2 lerp(vec2 first, vec2 second, float perc) {
    return first * (1.0 - perc) + second * perc;
}

float rand(float seed){
    return fract(sin(12.9898 + seed * 78.233)) ;
}

vec2 toMathCoords(vec2 uv) {
    return vec2(uv.x * 2.0 - 1.0, (uv.y * 2.0 - 1.0) / yScl);
}

vec2 toUvCoords(vec2 mathCoords) {
    return vec2((mathCoords.x + 1.0) / 2.0, (mathCoords.y * yScl + 1.0) / 2.0);
}

float getStencilValue(vec2 mathCoords) {
    return uintBitsToFloat(texture(stencil, toUvCoords(mathCoords)).r);
}

#define PI 3.14159265359

// ----------------------------------------------
// Custom functions
// Custom 1
vec2 flipX(vec2 uv) {
    return vec2(-uv.x, uv.y);
}

// Custom 2
vec2 flipY(vec2 uv) {
    return vec2(uv.x, -uv.y);
}

// Custom 3
vec2 flipXY(vec2 uv) {
    return vec2(-uv.x, -uv.y);
}

uniform float rotateAndScale_angle = 0.05;
// Custom 4
vec2 rotateAndScale(vec2 uv, float base, float scale) {
    float r = length(uv);
    float theta = atan(uv.y, uv.x);
    float _r = r * scale ;
    float _theta = theta + rotateAndScale_angle;
    return vec2(cos(_theta), sin(_theta)) * _r;
}

// ----------------------------------------------
// Flame variation functions (are offset by 128)
// Flame var 1
vec2 sinusoidal(vec2 uv) {
    return vec2(sin(uv.x), sin(uv.y));
}

// Flame var 2
vec2 spherical(vec2 uv) {
    float r = length(uv);
    return uv / (r * r);
}

// Flame var 3
vec2 swirl(vec2 uv) {
    float r = length(uv);
    return vec2(uv.x * sin(r) - uv.y * cos(r), uv.x * cos(r) + uv.y * sin(r));
}

// Flame var 4
vec2 horseshoe(vec2 uv) {
    return vec2((uv.x - uv.y) * (uv.x + uv.y), 2.0 * uv.x * uv.y);
}

// Flame var 5
vec2 polar(vec2 uv) {
    float r = length(uv);
    float theta = atan(uv.y, uv.x);
    return vec2(theta / PI, r - 1.0);
}

// Flame var 6
vec2 handkerchief(vec2 uv) {
    float r = length(uv);
    float theta = atan(uv.y, uv.x);
    return vec2(r * sin(theta + r), r * cos(theta - r));
}

// Flame var 7
vec2 heart(vec2 uv) {
    float r = length(uv);
    float theta = atan(uv.y, uv.x);
    return vec2(r * sin(theta * r), -r * cos(theta * r));
}

// Flame var 8
vec2 disc(vec2 uv) {
    float r = length(uv);
    return uv / r;
}

// Flame var 9
vec2 spiral(vec2 uv) {
    float r = length(uv);
    float theta = atan(uv.y, uv.x);
    return vec2(cos(theta) - sin(r), sin(theta) - cos(r)) / r;
}

// Flame var 10
vec2 hyperbolic(vec2 uv) {
    float r = length(uv);
    float theta = atan(uv.y, uv.x);
    return vec2(sin(theta) / r, r * cos(theta));
}

// Flame var 11
vec2 diamond(vec2 uv) {
    float r = length(uv);
    float theta = atan(uv.y, uv.x);
    return vec2(sin(theta) * cos(r), cos(theta) * sin(r));
}

// Flame var 12
vec2 ex(vec2 uv) {
    float r = length(uv);
    float theta = atan(uv.y, uv.x);
    float p0 = sin(theta + r);
    float p1 = cos(theta - r);
    p0 = p0 * p0 * p0;
    p1 = p1 * p1 * p1;
    return vec2(p0 + p1, p0 + p1) * r;
}

// Flame var 13
vec2 julia(vec2 uv) {
    float r = length(uv);
    float theta = atan(uv.y, uv.x);
    float omega = floor(rand(r) * 2.0);
    float arg = theta * 0.5 + omega;
    return vec2(r * cos(arg), r * sin(arg));
}

// Flame var 14
vec2 bent(vec2 uv) {
    return vec2(uv.x >= 0.0 ? uv.x : 2.0 * uv.x, uv.y >= 0.0 ? uv.y : 0.5 * uv.y);
}

uniform float waves_b = 1.4;
uniform float waves_c = 1.1;
uniform float waves_e = 0.7;
uniform float waves_f = 1.6;
// Flame var 15
vec2 waves(vec2 uv) {
    return vec2(uv.x + waves_b * sin(uv.y / waves_c / waves_c), uv.y + waves_e * sin(uv.x / waves_f / waves_f));
}

// Flame var 16
vec2 fisheye(vec2 uv) {
    float r = length(uv);
    return vec2(uv.y, uv.x) * 2.0 / (r + 1.0);
}

// Iterative lookup function
vec4 iterativeLookup(vec2 uv) {
    vec2 mathCoords = toMathCoords(uv);
    float stencilValue = getStencilValue(mathCoords);

    int i = iterCount;

    while (i > 0) {
        // Custom functions
        if (stencilValue < 128.0) {
            if (stencilValue == 0.0) break;
            if (stencilValue == 1.0) mathCoords = flipX(mathCoords);
            if (stencilValue == 2.0) mathCoords = flipY(mathCoords);
            if (stencilValue == 3.0) mathCoords = flipXY(mathCoords);
            if (stencilValue == 4.0) mathCoords = rotateAndScale(mathCoords, 1.25, 1.25);
        }
        // Flame vars
        else if (stencilValue >= 128.0) {
            stencilValue -= 128.0; // Domain update
            if (stencilValue == 0.0) break; // Zero is skipped for consistency with original flam3 paper
            if (stencilValue == 1.0) mathCoords = sinusoidal(mathCoords);
            if (stencilValue == 2.0) mathCoords = spherical(mathCoords);
            if (stencilValue == 3.0) mathCoords = swirl(mathCoords);
            if (stencilValue == 4.0) mathCoords = horseshoe(mathCoords);
            if (stencilValue == 5.0) mathCoords = polar(mathCoords);
            if (stencilValue == 6.0) mathCoords = handkerchief(mathCoords);
            if (stencilValue == 7.0) mathCoords = heart(mathCoords);
            if (stencilValue == 8.0) mathCoords = disc(mathCoords);
            if (stencilValue == 9.0) mathCoords = spiral(mathCoords);
            if (stencilValue == 10.0) mathCoords = hyperbolic(mathCoords);
            if (stencilValue == 11.0) mathCoords = diamond(mathCoords);
            if (stencilValue == 12.0) mathCoords = ex(mathCoords);
            if (stencilValue == 13.0) mathCoords = julia(mathCoords);
            if (stencilValue == 14.0) mathCoords = bent(mathCoords);
            if (stencilValue == 15.0) mathCoords = waves(mathCoords);
            if (stencilValue == 16.0) mathCoords = fisheye(mathCoords);
        }
        // Not supposed to happen. Just break the loop.
        else break;

        // Break if out of bounds
        vec2 p = toUvCoords(mathCoords);
        if (p.x <= 0.0 || p.x >= 1.0 || p.y <= 0.0 || p.y >= 1.0) break;

        // Update stencil value and the loop counter
        stencilValue = getStencilValue(mathCoords);
        i--;
    }

    return texture(tex0, toUvCoords(mathCoords));
}

// Main function:
// For each pixel, perform the iterative lookup
void main() {
    o_color = iterativeLookup(v_texCoord0);
}