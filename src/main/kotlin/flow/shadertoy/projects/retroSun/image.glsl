// Source: https://www.shadertoy.com/view/4dcyW7
#define tri(t, scale, shift) ( abs(t * 2. - 1.) - shift ) * (scale)

float smin( float a, float b, float k )
{
    float h = clamp( 0.5+0.5*(b-a)/k, 0.0, 1.0 );
    return mix( b, a, h ) - k*h*(1.0-h);
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    vec2 R = iResolution.xy,
    uv = ( fragCoord - .5* R ) / R.y + .5;

    // sun
    float dist = length(uv-vec2(0.5,0.5));
    float divisions = 6.0;
    float divisionsShift= 0.5;

    float pattern = tri(fract(( uv.y + 0.5)* 20.0), 2.0/  divisions, divisionsShift)- (-uv.y + 0.26) * 0.85;
    float sunOutline = smoothstep( 0.0,-0.015, max( dist - 0.315, -pattern)) ;

    vec3 c = sunOutline * mix(vec3( 4.0, 0.0, 0.2), vec3(1.0, 1.1, 0.0), uv.y);

    // glow
    float glow = max(0.0, 1.0 - dist * 1.25);
    glow = min(glow * glow * glow, 0.325);
    c += glow * vec3(1.5, 0.3, (sin(iTime)+ 1.0)) * 1.1;


    vec2 ground;

    vec2 planeuv = uv;
    /* ground in progress
       planeuv.x =  (planeuv.x - 0.5) * (-planeuv.y) + 0.5;
       //    planeuv.y *= planeuv.y;

       planeuv.y += (iTime  ) * 0.13;
       ground.x = tri(fract(( planeuv.x + 0.5)* 10.0), 1.0/10.0, 0.0);
       ground.y = tri(fract(( planeuv.y + 0.5)* 10.0), 1.0/10.0, 0.0);

       float groud_lines = smin(ground.x,ground.y, 0.015);
          float ground_glow = smin(ground.x,ground.y, 0.06);

       float ground_line_color =  smoothstep( 0.01,-0.01, groud_lines);
       float ground_color =  smoothstep( 0.08,-0.0, ground_glow);
       c = vec3(1.5,2,1) * ground_line_color + vec3(0.1,0.2,0.4) *ground_color;*/
    fragColor = vec4(c,1.0);
}