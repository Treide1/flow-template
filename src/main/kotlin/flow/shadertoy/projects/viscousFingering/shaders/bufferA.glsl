// Added by me
float rand(vec2 co){
    return fract(sin(dot(co, vec2(12.9898, 78.233))) * 43758.5453);
}

uniform float _K0; // center weight = -20/6, range = [-4, 0]
uniform float _K1; // edge-neighbors = 4/6, range = [0, 1]
uniform float _K2; // vertex-neighbors = 1/6, range = [0, 1]
uniform float cs; // curl scale = 0.25, range = [0, 1]
uniform float ls; // laplacian scale = 0.24, range = [0.0, 0.5]
uniform float ps; // laplacian of divergence scale = -0.06, range = [-0.20, 0.00]
uniform float ds; // divergence scale = -0.08, range = [-1, 0]
uniform float pwr; // power when deriving rotation angle from curl = 0.2, range = [0, 1]
uniform float amp; // self-amplification = 1.0, range = [0.9, 1.1]
uniform float sq2; // diagonal weight = 0.7, range = [0, 1]

// Original: https://www.shadertoy.com/view/ttjXzR
bool reset() {
    return iMouse.x < 0.0; // changed to not use iChannel3 = keyboard
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    //const float _K0 = -20.0/6.0; // center weight / -20/6
    // const float _K1 = 4.0/6.0; // edge-neighbors // 4/6
    // const float _K2 = 1.0/6.0; // vertex-neighbors // 1/6
    //const float cs = 0.25; // curl scale // 0.25
    // const float ls = 0.24; // laplacian scale // 0.24
    // const float ps = -0.06; // laplacian of divergence scale // -0.06
    // const float ds = -0.08; // divergence scale // -0.09
    // const float pwr = 0.2; // power when deriving rotation angle from curl // 0.2
    // const float amp = 1.0; // self-amplification // 1.0
    // const float sq2 = 0.7; // diagonal weight // 0.7

    vec2 vUv = fragCoord.xy / iResolution.xy;
    vec2 texel = 1. / iResolution.xy;

    // 3x3 neighborhood coordinates
    float step_x = texel.x;
    float step_y = texel.y;
    vec2 n  = vec2(0.0, step_y);
    vec2 ne = vec2(step_x, step_y);
    vec2 e  = vec2(step_x, 0.0);
    vec2 se = vec2(step_x, -step_y);
    vec2 s  = vec2(0.0, -step_y);
    vec2 sw = vec2(-step_x, -step_y);
    vec2 w  = vec2(-step_x, 0.0);
    vec2 nw = vec2(-step_x, step_y);

    vec3 uv =    texture(iChannel0, vUv).xyz;
    vec3 uv_n =  texture(iChannel0, vUv+n).xyz;
    vec3 uv_e =  texture(iChannel0, vUv+e).xyz;
    vec3 uv_s =  texture(iChannel0, vUv+s).xyz;
    vec3 uv_w =  texture(iChannel0, vUv+w).xyz;
    vec3 uv_nw = texture(iChannel0, vUv+nw).xyz;
    vec3 uv_sw = texture(iChannel0, vUv+sw).xyz;
    vec3 uv_ne = texture(iChannel0, vUv+ne).xyz;
    vec3 uv_se = texture(iChannel0, vUv+se).xyz;

    // uv.x and uv.y are our x and y components, uv.z is divergence 

    // laplacian of all components
    vec3 lapl  = _K0*uv + _K1*(uv_n + uv_e + uv_w + uv_s) + _K2*(uv_nw + uv_sw + uv_ne + uv_se);
    float sp = ps * lapl.z;

    // calculate curl
    // vectors point clockwise about the center point
    float curl = uv_n.x - uv_s.x - uv_e.y + uv_w.y + sq2 * (uv_nw.x + uv_nw.y + uv_ne.x - uv_ne.y + uv_sw.y - uv_sw.x - uv_se.y - uv_se.x);

    // compute angle of rotation from curl
    float sc = cs * sign(curl) * pow(abs(curl), pwr);

    // calculate divergence
    // vectors point inwards towards the center point
    float div  = uv_s.y - uv_n.y - uv_e.x + uv_w.x + sq2 * (uv_nw.x - uv_nw.y - uv_ne.x - uv_ne.y + uv_sw.x + uv_sw.y + uv_se.y - uv_se.x);
    float sd = ds * div;

    vec2 norm = normalize(uv.xy);

    // temp values for the update rule
    float ta = amp * uv.x + ls * lapl.x + norm.x * sp + uv.x * sd;
    float tb = amp * uv.y + ls * lapl.y + norm.y * sp + uv.y * sd;

    // rotate
    float a = ta * cos(sc) - tb * sin(sc);
    float b = ta * sin(sc) + tb * cos(sc);

    // initialize with noise
    if(iFrame<10 || reset()) {
        fragColor = -0.5 + vec4(rand(uv.xy), rand(uv.yz + 34.59), rand(uv.zx + 32.18), 1.0);// Changed to not use: texture(iChannel1, fragCoord.xy / iResolution.xy);
    } else {
        fragColor = clamp(vec4(a,b,div,1), -1., 1.);
    }


}