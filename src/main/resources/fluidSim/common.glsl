//Chimera's Breath
//by nimitz 2018 (twitter: @stormoid)

/*
	The main interest here is the addition of vorticity confinement with the curl stored in
	the alpha channel of the simulation texture (which was not used in the paper)
	this in turns allows for believable simulation of much lower viscosity fluids.
	Without vorticity confinement, the fluids that can be simulated are much more akin to
	thick oil.

	Base Simulation based on the 2011 paper: "Simple and fast fluids"
	(Martin Guay, Fabrice Colin, Richard Egli)
	(https://hal.inria.fr/inria-00596050/document)

	The actual simulation only requires one pass, Buffer A, B and C	are just copies
	of each other to increase the simulation speed (3 simulation passes per frame)
	and Buffer D is drawing colors on the simulated fluid
	(could be using particles instead in a real scenario)
*/

#define dt 0.15
#define USE_VORTICITY_CONFINEMENT
//#define MOUSE_ONLY
//#define MOUSE_ADD

//Recommended values between 0.03 and 0.2
//higher values simulate lower viscosity fluids (think billowing smoke)
#define VORTICITY_AMOUNT 0.11
#define VORT 0.11
// (mod(floor(uv.x * 3), 2.0) * 0.4 + 0.11)
#define MAX_R 0.05
#define mouseUV (iMouse.xy/iResolution.xy)

uniform float regionSize;

float regionSDF(vec2 uv) {
    float sdf1 = length(uv - mouseUV) - regionSize;
    float saturated1 = clamp(sdf1 * 5.0, 0.0, 1.0);
    float gate = step(-1.0, iMouse.z); // if mouse is pressed = 1, else = 0
    return mix(saturated1, 1.0, gate);
}

float visc(vec2 uv) {
    return mix(0.01, 1.0, regionSDF(uv));
}

vec2 linearDecay(vec2 vel, float reduction) {
    return max(vec2(0), abs(vel) - reduction)*sign(vel);
}

vec2 manipulateVel(vec2 vel, vec2 uv) {

    vec2 center = mouseUV;
    vec2 toCenter = normalize(center - uv);
    float regionSDF = regionSDF(uv);

    // Manipulations:
    // * As regionSDF approaches 0, the velocity is turned towards center
    // * As regionSDF approaches 0, the linear decay becomes a speedup
    float reductionValue = mix(-0.15, 1e-4, regionSDF);
    vec2 turned = mix(toCenter * 0.5, vel, clamp(sqrt(regionSDF) + 0.001, 0.0, 1.0));
    return linearDecay(turned, reductionValue);
}

float mag2(vec2 p){return dot(p,p);}
vec2 point1(float t) {
    t *= 0.62;
    return vec2(0.12,0.5 + sin(t)*0.2);
}
vec2 point2(float t) {
    t *= 0.62;
    return vec2(0.88,0.5 + cos(t + 1.5708)*0.2);
}

vec4 solveFluid(sampler2D smp, vec2 uv, vec2 w, float time, vec3 mouse, vec3 lastMouse)
{
    const float K = 0.2;
    const float v = 0.55;

    vec4 data = textureLod(smp, uv, 0.0);
    vec4 tr = textureLod(smp, uv + vec2(w.x , 0), 0.0);
    vec4 tl = textureLod(smp, uv - vec2(w.x , 0), 0.0);
    vec4 tu = textureLod(smp, uv + vec2(0 , w.y), 0.0);
    vec4 td = textureLod(smp, uv - vec2(0 , w.y), 0.0);

    vec3 dx = (tr.xyz - tl.xyz)*0.5;
    vec3 dy = (tu.xyz - td.xyz)*0.5;
    vec2 densDif = vec2(dx.z ,dy.z);

    data.z -= dt*dot(vec3(densDif, dx.x + dy.y) ,data.xyz); //density
    vec2 laplacian = tu.xy + td.xy + tr.xy + tl.xy - 4.0*data.xy;
    vec2 viscForce = vec2(v)*laplacian * visc(uv);
    data.xyw = textureLod(smp, uv - dt*data.xy*w, 0.).xyw; //advection

    vec2 newForce = vec2(0);
    #ifndef MOUSE_ONLY
    #if 1
    newForce.xy += 0.75*vec2(.0003, 0.00015)/(mag2(uv-point1(time))+0.0001);
    newForce.xy -= 0.75*vec2(.0003, 0.00015)/(mag2(uv-point2(time))+0.0001);
    #else
    newForce.xy += 0.9*vec2(.0003, 0.00015)/(mag2(uv-point1(time))+0.0002);
    newForce.xy -= 0.9*vec2(.0003, 0.00015)/(mag2(uv-point2(time))+0.0002);
    #endif
    #endif

    #ifdef MOUSE_ADD
    if (mouse.z > 1. && lastMouse.z > 1.)
    {
        vec2 vv = clamp(vec2(mouse.xy*w - lastMouse.xy*w)*400., -6., 6.);
        newForce.xy += .001/(mag2(uv - mouse.xy*w)+0.001)*vv;
    }
    #endif

    data.xy += dt*(viscForce.xy - K/dt*densDif + newForce); //update velocity
    data.xy = manipulateVel(data.xy, uv);

    #ifdef USE_VORTICITY_CONFINEMENT
    data.w = (tr.y - tl.y - tu.x + td.x);
    vec2 vort = vec2(abs(tu.w) - abs(td.w), abs(tl.w) - abs(tr.w));
    vort *= VORT/length(vort + 1e-9)*data.w;
    data.xy += vort;
    #endif

    data.y *= smoothstep(.5,.48,abs(uv.y-0.5)); //Boundaries

    data = clamp(data, vec4(vec2(-10), 0.5 , -10.), vec4(vec2(10), 3.0 , 10.));

    return data;
}