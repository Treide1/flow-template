@file:Suppress("unused")

package flow.fx

import org.openrndr.draw.ShadeStyle
import org.openrndr.draw.shadeStyle

fun galaxyShadeStyle(time: Double, zoom: Double = 1.0, iterations: Int = 45): ShadeStyle {
    val s = shadeStyle
    s.parameter("time", time)
    s.parameter("zoom", zoom)
    s.parameter("iterations", iterations)
    return s
}

private val shadeStyle by lazy {
    shadeStyle {
        fragmentTransform = """
            vec2 uv = c_boundsPosition.xy;
            uv = uv * 2.0 - 1.0;
            // Zoom by p_zoom
            uv /= p_zoom;
            float t = p_time * .1 + ((.25 + .05 * sin(p_time * .1))/(length(uv) + .07)) * 2.2;
            float si = sin(t);
            float co = cos(t);
            mat2 ma = mat2(co, si, -si, co);
        
            float v1, v2, v3;
            v1 = v2 = v3 = 0.0;
            
            float s = 0.0;
            for (int i = 0; i < p_iterations; i++)
            {
                vec3 p = s * vec3(uv, 0.0);
                p.xy *= ma;
                p += vec3(.22, .3, s - 1.5 - sin(p_time * .13) * .1);
                for (int i = 0; i < 8; i++)	p = abs(p) / dot(p,p) - 0.659;
                v1 += dot(p,p) * .0015 * (1.8 + sin(length(uv * 13.0) + .5  - p_time * .2));
                v2 += dot(p,p) * .0013 * (1.5 + sin(length(uv * 14.5) + 1.2 - p_time * .3));
                v3 += length(p.xy*10.) * .0003;
                s  += .035;
            }
            
            float len = length(uv);
            v1 *= smoothstep(.7, .0, len);
            v2 *= smoothstep(.5, .0, len);
            v3 *= smoothstep(.9, .0, len);
            
            vec3 col = vec3( v3 * (1.5 + sin(p_time * .2) * .4),
                            (v1 + v3) * .3,
                             v2) + smoothstep(0.2, .0, len) * .85 + smoothstep(.0, .6, v3) * .3;
        
            x_fill = vec4(min(pow(abs(col), vec3(1.2)), 1.0), 1.0);
        """.trimIndent()
    }
}
