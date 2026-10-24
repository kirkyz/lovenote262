#version 330 core
// Drainer V65 — independently authored ringed gas giant, sky-only shader.
// Camera/world coordinates and V64 uniform layout are deliberately unchanged.
in vec3 v_SkyDirection;
out vec4 color;
layout (std140) uniform SkyData {
    vec4 skyControl;   // time, mode, quality, brightness
    vec4 skyPrimary;   // main hue RGB; density
    vec4 skySecondary; // accent hue RGB
    vec4 skyExtra;     // scale, fixed anchor XYZ
    vec4 skyWorld;     // camera position XYZ
};
const float PI = 3.141592653589793;
float sat(float v) { return clamp(v,0.0,1.0); }
float hash3(vec3 p) {
    p=fract(p*0.1031);
    p+=dot(p,p.yzx+33.33);
    return fract((p.x+p.y)*p.z);
}
float noise3(vec3 p) {
    vec3 i=floor(p),f=fract(p);
    f=f*f*(3.0-2.0*f);
    return mix(mix(mix(hash3(i),hash3(i+vec3(1,0,0)),f.x),
                   mix(hash3(i+vec3(0,1,0)),hash3(i+vec3(1,1,0)),f.x),f.y),
               mix(mix(hash3(i+vec3(0,0,1)),hash3(i+vec3(1,0,1)),f.x),
                   mix(hash3(i+vec3(0,1,1)),hash3(i+vec3(1,1,1)),f.x),f.y),f.z);
}
float fbm3(vec3 p) {
    float value=0.0, amplitude=0.55;
    for(int i=0;i<4;i++) {
        value+=noise3(p)*amplitude;
        p=p*2.04+vec3(4.13,7.91,2.70);
        amplitude*=0.49;
    }
    return value;
}
float starPoint(vec3 direction,float grid,float rarity,float time) {
    vec3 x=direction*grid,id=floor(x),f=fract(x);
    float seed=hash3(id+vec3(3.1,8.7,1.2));
    vec3 center=vec3(hash3(id+vec3(17.1)),hash3(id+vec3(31.4)),hash3(id+vec3(7.7)))*0.62+0.19;
    float size=0.055+0.028*hash3(id+vec3(41.8));
    float dist=length(f-center);
    float glow=exp(-dist*dist/(size*size));
    return step(1.0-rarity,seed)*glow*(0.75+0.25*sin(time*0.7+seed*115.0));
}
float stars(vec3 d,float t,float density) {
    float abundance=sat(density*.48);
    return starPoint(d,88.0,abundance*.044,t)+
        starPoint(d,164.0,abundance*.024,t)*.45;
}
vec3 backdrop(vec3 d,float time,vec3 primary,vec3 accent,float density) {
    float field=fbm3(d*3.2+vec3(time*.018,0.0,time*.009));
    float wisps=pow(sat((field-.43)*2.0),2.3);
    float cloud=pow(sat((fbm3(d*5.7+vec3(8.3))-0.42)*2.0),2.0);
    vec3 base=mix(vec3(.0018,.0027,.0105),vec3(.006,.009,.026),sat(d.y*.6+.5));
    base+=mix(primary,vec3(.12,.17,.33),.80)*wisps*.24;
    base+=mix(accent,vec3(.08,.14,.27),.88)*cloud*.18;
    float s=stars(d,time,density);
    base+=s*vec3(.56,.68,.95);
    return base;
}
// Intersection of ray with the gas giant. Returns positive distance or a huge miss.
float hitSphere(vec3 offset,vec3 ray,float radius) {
    float b=dot(offset,ray);
    float c=dot(offset,offset)-radius*radius;
    float discriminant=b*b-c;
    if(discriminant<=0.0)return 1e20;
    float root=sqrt(discriminant);
    float near=-b-root,far=-b+root;
    return near>0.01?near:(far>0.01?far:1e20);
}
vec3 bodyColour(vec3 n,vec3 ray,vec3 primary,vec3 accent,float time,vec3 pole) {
    float lat=asin(clamp(dot(n,pole),-.999,.999));
    float lon=atan(n.z,n.x);
    float curl=fbm3(n*5.8+vec3(time*.034,-time*.018,time*.026));
    float fine=fbm3(n*14.0+vec3(4.1,time*.053,time*.011));
    // Broad, independently distorted gas belts with soft edges and fine wisps.
    float warpedLat=lat + (curl-.50)*.25 + (fine-.50)*.054;
    float broad=sin(warpedLat*13.8 + sin(lon*2.0+time*.045)*.45);
    float belt=sat(.48+.35*broad+.12*sin(warpedLat*30.0+curl*4.0));
    float mist=sat(.47+.40*curl+.10*fine);
    // Slow oval atmospheric storm drifts across the latitude belts.
    float stormLongitude=lon-time*.027;
    float stormAngle=atan(sin(stormLongitude-.35),cos(stormLongitude-.35));
    float stormOval=exp(-pow(stormAngle/.22,2.)-pow((lat-.18)/.12,2.));
    mist=mix(mist,sat(mist*.35+.72),stormOval*.58);
    // Desaturated atmospheric base, preserving user's custom colour hues.
    vec3 dusk=mix(vec3(.23,.21,.33),primary*.57,0.48);
    vec3 bright=mix(vec3(.67,.55,.64),accent*.61+primary*.18,0.40);
    vec3 shaded=mix(dusk,bright,sat(belt*.72+mist*.28));
    shaded+=vec3(.08,.055,.085)*pow(sat(fine*.95),2.0);
    // Single soft storm oval instead of another repeated stripe.
    float dl=atan(sin(lon-1.3),cos(lon-1.3));
    float storm=exp(-pow(dl/.31,2.0)-pow((lat+.23)/.115,2.0));
    float twist=sin(atan((lat+.23)*6.0,dl)*4.0+time*.02);
    shaded=mix(shaded,mix(vec3(.75,.53,.62),primary*.48,.40),storm*(.35+.10*twist));
    // Consistent illumination and strong edge darkening give a spherical body.
    vec3 sun=normalize(vec3(-.64,.44,-.62));
    float facing=sat(dot(n,sun)*.60+.42);
    float diffuse=.20+.80*smoothstep(.03,.96,facing);
    float view=sat(dot(n,-ray));
    float limb=pow(view,.46);
    return shaded*diffuse*(.40+.60*limb)+primary*.024*pow(1.0-view,4.0);
}
void main() {
    vec3 ray=normalize(v_SkyDirection);
    vec3 center=skyExtra.yzw;
    vec3 offset=skyWorld.xyz-center;
    vec3 primary=clamp(skyPrimary.rgb,0.0,1.0);
    vec3 accent=clamp(skySecondary.rgb,0.0,1.0);
    float time=skyControl.x;
    float amount=clamp(.08+.19*skyExtra.x,.38,1.50);
    float radius=60.0*amount;
    vec3 rgb=backdrop(ray,time,primary,accent,skyPrimary.w);
    float tSphere=hitSphere(offset,ray,radius);
    // Project a finite tilted annulus, never an infinite sky plane.
    vec3 pole=normalize(vec3(-.11,.65,.75));
    float denom=dot(pole,ray);
    float tRing=1e20;
    if(abs(denom)>0.00075) {
        float t=-dot(offset,pole)/denom;
        if(t>0.01 && t<9000.0)tRing=t;
    }
    float ringA=radius*1.25,ringB=radius*2.12;
    float ringAlpha=0.0;
    vec3 ringColor=vec3(0.0);
    if(tRing<1e19) {
        vec3 local=offset+ray*tRing;
        float r=length(local);
        float u=(r-ringA)/(ringB-ringA);
        if(u>-.05&&u<1.05) {
            float edges=smoothstep(0.0,.045,u)*(1.0-smoothstep(.956,1.0,u));
            float fine=sin(r*.55+sin(r*.14)*.72);
            float dust=fbm3(local*.035+vec3(time*.008,time*.012,-time*.006));
            // Independent orbital dust motion: ring density changes without moving its geometry.
            float angle=atan(local.z,local.x);
            float orbital=sin(angle*36.0-time*.33 + r*.11) * .055;
            float lane=.68+.18*sin(r*.16+1.1)+.12*fine+orbital;
            float cassini=1.0-.92*(smoothstep(.47,.485,u)-smoothstep(.505,.52,u));
            float gap2=1.0-.55*(smoothstep(.18,.191,u)-smoothstep(.199,.210,u));
            ringAlpha=edges*cassini*gap2*sat(lane+.22*dust);
            vec3 warm=mix(vec3(.64,.56,.69),primary*.25+vec3(.51,.50,.57),.34);
            vec3 cold=mix(vec3(.30,.36,.55),accent*.21+vec3(.24,.29,.44),.24);
            ringColor=mix(warm,cold,sat(.15+.68*u+.13*sin(r*.08)));
            ringColor*=.67+.30*sat(dust);
            // Soft azimuthal illumination rather than neon saturated bands.
            ringColor*=.61+.39*sat(dot(normalize(local),normalize(vec3(-.64,.44,-.62)))*.5+.5);
        }
    }
    // Correct depth ordering: rear rings, planet, foreground rings.
    if(ringAlpha>0.0 && tRing>tSphere) rgb=mix(rgb,ringColor,ringAlpha);
    if(tSphere<1e19) {
        vec3 n=normalize(offset+ray*tSphere);
        rgb=bodyColour(n,ray,primary,accent,time,pole);
    }
    if(ringAlpha>0.0 && tRing<tSphere) rgb=mix(rgb,ringColor,ringAlpha);
    // Bounded soft atmospheric halo outside the planet silhouette only.
    float forward=-dot(offset,ray);
    if(forward>0.0 && tSphere>1e19) {
        float miss=length(offset+ray*forward);
        float glow=exp(-pow((miss-radius)/(radius*.16),2.0));
        rgb+=mix(primary,vec3(.59,.53,.76),.65)*glow*.095;
    }
    color=vec4(clamp(rgb*skyControl.w,0.0,1.0),1.0);
}
