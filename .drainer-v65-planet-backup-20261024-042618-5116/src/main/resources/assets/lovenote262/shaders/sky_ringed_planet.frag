#version 330 core
// Drainer V64 Ringed Planet: independently written shader.
// Analytic sphere and finite annulus at a fixed world-space celestial centre.
// No texture packs or material from cracked client files.
in vec3 v_SkyDirection;
out vec4 color;
layout (std140) uniform SkyData {
    vec4 skyControl;   // time, mode, quality, brightness
    vec4 skyPrimary;   // primary RGB + star density
    vec4 skySecondary; // secondary RGB
    vec4 skyExtra;     // scale, anchorX, anchorY, anchorZ
    vec4 skyWorld;     // camera XYZ
};
#define PI 3.14159265359
float sat(float v){return clamp(v,0.0,1.0);}
float hash(vec3 p){p=fract(p*.1031);p+=dot(p,p.yzx+33.33);return fract((p.x+p.y)*p.z);}
float stars(vec3 dir){
    vec3 g=dir*225.0, id=floor(g), cell=fract(g)-.5;
    float h=hash(id);
    float amount=clamp(skyPrimary.w*0.006,0.0,0.016);
    float point=1.0-smoothstep(.018,.095,length(cell));
    return step(1.0-amount,h)*point*(0.80+0.20*sin(skyControl.x*.5+h*94.0));
}
void main(){
    vec3 ray=normalize(v_SkyDirection);
    vec3 center=skyExtra.yzw;
    vec3 offset=skyWorld.xyz-center;
    float amount=clamp(skyExtra.x*.20,0.38,2.2);
    float radius=57.0*amount;
    vec3 primary=skyPrimary.rgb;
    vec3 secondary=skySecondary.rgb;
    vec3 rgb=mix(vec3(.0015,.002,.010),vec3(.003,.005,.020),sat(ray.y*.52+.48));
    rgb += stars(ray)*mix(vec3(.7,.8,1.),secondary,.23)*.70;
    // Analytic planet intersection, stable while walking, independent of ray-plane horizon poles.
    float b=dot(offset,ray);
    float c=dot(offset,offset)-radius*radius;
    float disc=b*b-c;
    float tSphere=1e20;
    vec3 surf=vec3(0.0);
    if(disc>0.0){
        float root=sqrt(disc);
        float near=-b-root;
        float far=-b+root;
        if(near>0.1) tSphere=near;
        else if(far>0.1) tSphere=far;
    }
    // Tilted Saturn-style ring: finite-width annulus in the planet's equatorial plane.
    // Ray-plane intersection is safely bounded; it is never used for noise or infinite sky.
    vec3 normal=normalize(vec3(.08,.98,.20));
    float nd=dot(normal,ray);
    float tRing=1e20;
    if(abs(nd)>.0008){
        float t=-dot(offset,normal)/nd;
        if(t>0.1 && t<6000.0)tRing=t;
    }
    float ringA=radius*1.19,ringB=radius*2.12;
    float ringAlpha=0.0;
    vec3 ringColour=vec3(0.0);
    if(tRing<1e19){
        vec3 hit=offset+ray*tRing;
        float r=length(hit);
        float edge=smoothstep(ringA,ringA+radius*.055,r)*(1.0-smoothstep(ringB-radius*.055,ringB,r));
        float stripe=sin(r*0.10+skyControl.x*.012+sin(r*.07)*0.75);
        float gap=0.80+0.20*stripe;
        float groove=0.84+0.16*sin(r*.37+4.6);
        ringAlpha=edge*gap*groove*.92;
        ringColour=mix(primary,secondary,sat((r-ringA)/(ringB-ringA)))*(.52+.30*gap);
        ringColour+=vec3(.20,.17,.25)*.17;
    }
    // Back half of the ring, then the planet, then near half of the ring.
    if(ringAlpha>0.0 && tRing>tSphere)rgb=mix(rgb,ringColour,ringAlpha);
    if(tSphere<1e19){
        vec3 pt=offset+ray*tSphere;
        vec3 n=normalize(pt);
        float latitude=asin(clamp(dot(n,normal),-1.,1.));
        float lon=atan(n.z,n.x);
        float bands=sin(latitude*17.0+sin(lon*2.1+skyControl.x*.025)*.55);
        bands+=0.31*sin(latitude*41.0+sin(lon*4.2)*1.5);
        float swirl=sin(lon*7.2+latitude*23.0+skyControl.x*.017);
        float m=sat(.49+.25*bands+.10*swirl);
        vec3 gas=mix(primary*.78,secondary*.84,m);
        // Gentle gaseous belts and one side illuminated; no blown-out white disk.
        float diffuse=dot(n,normalize(vec3(-.35,.28,-.89)));
        float lighting=.44+.56*sat(diffuse*.48+.72);
        float limb=pow(1.0-sat(dot(n,-ray)),2.0);
        gas=gas*lighting + secondary*.23*limb;
        rgb=gas;
    }
    if(ringAlpha>0.0 && tRing<tSphere)rgb=mix(rgb,ringColour,ringAlpha);
    // Atmospheric halo only close to the silhouette. Smooth, bounded, no horizon lines.
    float forward=-dot(offset,ray);
    float silhouette=length(offset+ray*max(0.,forward));
    float halo=exp(-abs(silhouette-radius)/(radius*.14));
    if(forward>0.0 && tSphere>1e19) rgb+=primary*.085*halo;
    color=vec4(clamp(rgb*skyControl.w,0.0,1.0),1.0);
}
