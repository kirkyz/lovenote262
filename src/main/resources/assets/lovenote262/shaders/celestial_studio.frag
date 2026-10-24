#version 330 core

// Drainer V46 -- dedicated far-field celestial effects.  No nearby-world
// parallax; the camera changes its view angle but never drags the galaxy.
in vec3 v_SkyDirection;
out vec4 color;

// Same std140 layout as the V43 sky pass; the native aurora is NOT touched.
layout(std140) uniform SkyData {
    vec4 skyControl;       // time, preset, quality, brightness
    vec4 skyPrimary;       // primary RGB, nebula density
    vec4 skySecondary;     // secondary RGB, flow
    vec4 skyExtra;         // cloud scale, black hole size, unused, expanse
    vec4 skyWorld;         // unused in celestial mode
};

const float PI = 3.14159265359;

float hash13(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}
float valueNoise(vec3 x) {
    vec3 p = floor(x);
    vec3 f = fract(x);
    f = f*f*(3.0-2.0*f);
    float x00 = mix(hash13(p), hash13(p+vec3(1,0,0)), f.x);
    float x10 = mix(hash13(p+vec3(0,1,0)), hash13(p+vec3(1,1,0)), f.x);
    float x01 = mix(hash13(p+vec3(0,0,1)), hash13(p+vec3(1,0,1)), f.x);
    float x11 = mix(hash13(p+vec3(0,1,1)), hash13(p+vec3(1,1,1)), f.x);
    return mix(mix(x00,x10,f.y),mix(x01,x11,f.y),f.z);
}
float fBm(vec3 p, float quality) {
    int octaves = quality < 0.5 ? 2 : quality < 1.5 ? 3 : 4;
    float sum=0.0, amp=0.55, total=0.0;
    for (int i=0; i<4; i++) {
        if (i>=octaves) break;
        sum += valueNoise(p) * amp;
        total += amp;
        p=p*2.13+vec3(13.3,5.7,8.1);
        amp*=0.48;
    }
    return sum / max(total,0.001);
}

// Star positions are deterministic in SKY direction.  Different resolutions
// contribute sparse pin-points, never the cross-shaped stars of V36.
float starLayer(vec3 dir, float cells, float threshold) {
    vec3 pos=dir*cells;
    vec3 cell=floor(pos);
    vec3 f=fract(pos);
    float lucky=hash13(cell+vec3(6.3,1.2,8.7));
    vec3 centre=vec3(
        hash13(cell+vec3(17.2,2.1,0.1)),
        hash13(cell+vec3(1.8,11.1,4.9)),
        hash13(cell+vec3(7.1,9.6,13.3))
    )*0.62+0.19;
    float dist=length(f-centre);
    return smoothstep(threshold, 1.0, lucky) * exp(-dist*dist*580.0);
}

vec4 nebula(vec3 d, float t, float q, float flow, float density,
            float cloudScale, float expanse, vec3 a, vec3 b, float brightness) {
    // Wide, slanted celestial band, surrounded by clear regions of sky.  This
    // deliberately avoids the old all-over cloudy-smoke dome.
    vec3 normal=normalize(vec3(-0.53,0.69,0.48));
    float distanceFromBand=abs(dot(d,normal));
    float band=1.0-smoothstep(0.08*expanse,0.38*expanse,distanceFromBand);
    if (band < 0.004 || density < 0.001) {
        float stars=starLayer(d,130.0,0.996)*0.55
                   +starLayer(d,225.0,0.9975)*0.80;
        return vec4(vec3(0.75,0.88,1.0)*stars,clamp(stars*0.92,0.0,0.9));
    }
    // Actual advecting cloud detail, NOT global brightness pulsing.
    float tm=t * clamp(flow,0.0,3.0) * 0.09;
    vec3 p=d * (5.6 * clamp(cloudScale,0.4,2.5) / clamp(expanse,0.7,3.0));
    vec3 warp=vec3(
        fBm(p*0.85+vec3(tm,0.0,-tm*0.25),q),
        fBm(p*0.85+vec3(0.0,-tm*0.35,tm*0.4),q),
        fBm(p*0.85+vec3(-tm*0.3,tm*0.6,0.0),q)
    )-0.5;
    float bulk=fBm(p+warp*1.5+vec3(tm*0.4,-tm*0.35,tm*0.2),q);
    float wisps=fBm(p*2.0-warp*0.55+vec3(-tm*0.8,tm*0.5,0.0),q);
    float clouds=smoothstep(0.40,0.75,bulk*0.79+wisps*0.31);
    float brightRifts=smoothstep(0.58,0.84,bulk*0.75+wisps*0.4);
    float strength=band*clouds*clamp(density,0.0,1.0);
    vec3 tint=mix(a,b,smoothstep(0.35,0.72,wisps));
    vec3 emission=tint*strength*(0.18+0.44*brightRifts)*brightness;
    emission += b*strength*brightRifts*0.18*brightness;
    float stars=(starLayer(d,130.0,0.9955)*0.50+
                 starLayer(d,225.0,0.997)*0.80) * (1.0+0.8*band);
    emission+=vec3(0.70,0.85,1.0)*stars;
    float alpha=clamp(strength*0.50+stars*0.95,0.0,0.84);
    // Convert premultiplied light into the alpha-blended output colour.
    return vec4(alpha>0.0005 ? clamp(emission/max(alpha,0.0005),0.0,1.0) : vec3(0.0),alpha);
}

vec4 blackHole(vec3 d, float t, float q, float flow, float size,
               vec3 a, vec3 b, float brightness) {
    // Sky-fixed axis.  Project onto a tangent plane: camera rotation reveals
    // the same stable astronomical object, not a billboard fixed to the UI.
    vec3 axis=normalize(vec3(0.25,0.46,-0.85));
    float facing=dot(d,axis);
    if (facing < 0.82) return vec4(0.0);
    vec3 right=normalize(cross(axis,vec3(0,1,0)));
    vec3 up=cross(right,axis);
    vec2 p=vec2(dot(d,right),dot(d,up))/max(0.1,facing);
    p*=1.0/clamp(size,0.45,1.8);
    // A tilted, finite accretion disc with no repetitive radial spikes.
    float tilt=0.24;
    vec2 ringPos=vec2(p.x+p.y*tilt,p.y*1.90);
    float r=length(ringPos);
    float phi=atan(ringPos.y,ringPos.x);
    float tm=t*max(0.0,flow);
    float turbulence=fBm(vec3(2.5*cos(phi-tm*0.45),2.5*sin(phi-tm*0.45),
                             r*13.0-tm*0.13),q);
    float ring=exp(-pow((r-0.22)/0.065,2.0));
    float inner=exp(-pow((r-0.137)/0.029,2.0));
    float filament=0.67+0.33*smoothstep(0.2,0.8,turbulence);
    float doppler=0.65+0.35*clamp(0.5+0.5*cos(phi-0.45),0.0,1.0);
    vec3 glow=mix(a,b,smoothstep(0.2,0.78,turbulence));
    vec3 disc=(ring*0.77+inner*0.34)*filament*doppler*glow*brightness;
    float halo=exp(-pow((r-0.22)/0.11,2.0))*0.14;
    disc+=a*halo*brightness;
    float core=1.0-smoothstep(0.096,0.112,length(p));
    float alpha=clamp(ring*0.78+inner*0.30+halo*0.32+core*0.99,0.0,0.99);
    vec3 premult=disc*(1.0-core)+vec3(0.002,0.002,0.006)*core;
    vec3 col=alpha>0.00001 ? premult/alpha : vec3(0.0);
    return vec4(clamp(col,0.0,1.0),alpha);
}

void main() {
    vec3 dir=normalize(v_SkyDirection);
    float time=skyControl.x;
    float mode=skyControl.y;
    float quality=skyControl.z;
    float brightness=clamp(skyControl.w,0.0,1.6);
    float density=clamp(skyPrimary.w,0.0,1.0);
    float flow=clamp(skySecondary.w,0.0,3.0);
    vec3 primary=skyPrimary.rgb;
    vec3 secondary=skySecondary.rgb;
    if (mode > 1.5) {
        // V48: distant, astronomical star pass only, deliberately independent
        // of the nearby volumetric nebula's absolute world coordinates.
        float stars=starLayer(dir,130.0,0.9955)*0.68
                   +starLayer(dir,225.0,0.9970)*0.84;
        float alpha=clamp(stars*0.95,0.0,0.90);
        color=vec4(vec3(0.76,0.86,1.0)*clamp(brightness,0.0,1.6),alpha);
    } else if (mode < 0.5) {
        color=nebula(dir,time,quality,flow,density,skyExtra.x,skyExtra.w,
                     primary,secondary,brightness);
    } else {
        color=blackHole(dir,time,quality,flow,skyExtra.y,
                        primary,secondary,brightness);
    }
    if (color.a < 0.002) discard;
}
