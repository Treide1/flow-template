uniform vec3 a;
uniform vec3 b;
uniform vec3 c;
uniform vec3 d;

vec3 palette(float t, vec3 a, vec3 b, vec3 c, vec3 d) {
    return a + b * cos(6.28318 * (c * t + d));
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    vec2 texel = 1. / iResolution.xy;
    vec2 uv = fragCoord.xy / iResolution.xy;
    vec3 components = texture(iChannel0, uv).xyz;
    vec3 norm = normalize(components);
    fragColor = vec4(palette(norm.z, a, b, c, d), 1.0);
}