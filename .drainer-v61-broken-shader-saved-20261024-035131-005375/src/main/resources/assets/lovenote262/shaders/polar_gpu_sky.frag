#version 330 core
// Original Drainer native GPU sky effects. No shader pack and no copied
// shaders/bytecode from third-party cracked clients.
in vec3 v_SkyDirection;
out vec4 color;
layout (std140) uniform SkyData {
    vec4 skyControl;     // time, mode, detail level, brightness
    vec4 skyPrimary;     // chosen primary colour, cloud density
    vec4 skySecondary;   // chosen secondary colour, reserved motion scale
    vec4 skyExtra;       // pattern scale
    vec4 skyWorld;
};
float hash13(vec3 p){p=fract(p*.1031);p+=dot(p,p.zyx+33.33);return fract((p.x+p.y)*p.z);}
float noise3(vec3 p){
    vec3 i=floor(p),f=fract(p);f=f*f*(3.-2.*f);
    return mix(mix(mix(hash13(i),hash13(i+vec3(1,0,0)),f.x),
                   mix(hash13(i+vec3(0,1,0)),hash13(i+vec3(1,1,0)),f.x),f.y),
               mix(mix(hash13(i+vec3(0,0,1)),hash13(i+vec3(1,0,1)),f.x),
                   mix(hash13(i+vec3(0,1,1)),hash13(i+vec3(1,1,1)),f.x),f.y),f.z);
}
float fbm(vec3 p) {
    float v=0.,a=.52;
    for(int i=0;i<5;i++){
        if(float(i)>=skyControl.z+2.)break;
        v+=a*noise3(p);p=p*1.91+vec3(3.1,5.7,2.9);a*=.52;
    }
    return v;
}
// Stable world-coordinate sampling of a high-altitude sky layer.
vec3 fixedSkySample(vec3 dir, float layerY, float frequency) {
    if (skyWorld.y >= layerY - 20.0) {
        layerY += 512.0 * ceil((skyWorld.y - layerY + 20.0) / 512.0);
    }
    float rayY = max(dir.y, 0.085);
    float travel = min((layerY - skyWorld.y) / rayY, 4200.0);
    vec2 worldXZ = skyWorld.xz + dir.xz * travel;
    return vec3(worldXZ.x * frequency, dir.y * 2.2, worldXZ.y * frequency);
}
float star(vec3 d, float amount){
    vec3 u=d*230.;vec3 c=floor(u);vec3 f=fract(u)-.5;
    float h=hash13(c);
    float point=1.-smoothstep(.035,.105,length(f));
    float intensity=step(1.-amount,h)*point;
    return intensity*(.65+.35*sin(skyControl.x*2.0+h*20.));
}
void main(){
    vec3 d=normalize(v_SkyDirection);
    float t=skyControl.x*.24;
    float scale=max(.25,skyExtra.x);
    float density=skyPrimary.w;
    float m=skyControl.y;
    float layerY = m > 3.5 ? 320.0 : (m > 2.5 ? 560.0 : 420.0);
    vec3 p=fixedSkySample(d, layerY, 0.011 * scale);
    vec3 deep=fixedSkySample(d, 1100.0, 0.0055 * scale);
    vec3 a=skyPrimary.rgb,b=skySecondary.rgb;
    float n=fbm(p*1.6+vec3(t*.18,-t*.12,t*.09));
    vec3 shade;
    if(m<.5){ // Animated plasma, independent deformation and colour controls.
        float w=.5+.5*sin(p.x*2.2+t+sin(p.z*2.3-t*.6)+sin(p.y*3.0+t*.33));
        float r=pow(clamp(w*.65+n*.65,0.,1.),1.7);
        shade=mix(vec3(.006,.009,.023),a*.38,r);
        shade+=b*.47*pow(r,3.)+a*.20*pow(n,5.);
    } else if(m<1.5){ // Deep ocean waves, procedural rather than a sky texture.
        vec3 q=p+vec3(t*.14,0.,t*.22);
        float waves=.5+.5*sin((q.x+sin(q.z*1.8+t*.7))*3.5+n*4.);
        shade=mix(vec3(.006,.016,.035),b*.35,pow(waves,.9));
        shade+=a*.24*pow(n,3.);
    } else if(m<2.5){ // Animated water striations.
        float waves=.5+.5*sin(p.y*3.8+sin(p.x*3.-t*.6)*.8-t*1.5);
        float foam=pow(clamp(waves*.5+n*.7,0.,1.),3.);
        shade=mix(vec3(.006,.012,.028),b*.30,waves);
        shade+=a*.27*foam;
    } else if(m<3.5){ // Nebula: broad twisted cloud band, subtle stars.
        float band=exp(-pow((d.y-.15-sin(deep.x*.65+deep.z*.42+t*.02)*.25)*2.15,2.));
        float warp=fbm(deep*1.25+vec3(t*.06,-t*.018,t*.05));
        float fine=fbm(p*3.0+vec3(-t*.045,t*.055,t*.03));
        float cloud=pow(clamp((warp*.85+fine*.38-.35)*band*density*2.0,0.,1.),1.3);
        shade=mix(vec3(.004,.007,.019),mix(a,b,clamp(fine,0.,1.))*.72,cloud);
        shade+=pow(cloud,3.)*.24*(a+b);
        shade+=vec3(.68,.76,1.)*star(d,.009);
    } else { // Aurora: vertically flowing ribbons over a dark starfield.
        float x=p.x*1.65+p.z*.62;
        float height=clamp(d.y*2.4,0.,1.);
        float flutter=sin(x*2.2+sin(x*1.25+t*.7)*1.2-t*.35);
        float turbulence=fbm(vec3(p.x*.4,t*.13+p.z*.05,d.y*2.8));
        float curtains=pow(max(0.,.5+.5*sin(x*3.1+flutter*1.6+t*.45+turbulence*3.)),6.0);
        float fold=pow(max(0.,.5+.5*sin(x*6.0-t*.8+turbulence*1.9)),9.0);
        float envelope=smoothstep(.0,.22,d.y)* (1.-smoothstep(.73,1.,d.y));
        float flicker=.72+.28*sin(t*1.3+flutter);
        float light=clamp((curtains*.75+fold*.38)*envelope*flicker*density*1.6,0.,1.);
        shade=vec3(.004,.008,.022)+mix(a,b,clamp(height,0.,1.))*.82*light;
        shade+=vec3(.6,.7,.9)*star(d,.006);
    }
    float horizon=smoothstep(-.42,.19,d.y);
    shade*=mix(.28,1.,horizon);
    color=vec4(clamp(shade*skyControl.w,0.,1.),1.);
}
