#version 330 core
// Drainer Animated Space Collection V66 (independent shader, not third-party JAR code).
// Every named visual has a different geometry/evolution equation. Distinct finite
// world-space celestial features use the existing V63 fixed world anchor; far stars
// intentionally remain at astronomical distance and have no walking parallax.
in vec3 v_SkyDirection;
out vec4 color;
layout(std140) uniform SkyData {
    vec4 skyControl;   // time, selected scene, quality 1..3, brightness
    vec4 skyPrimary;   // primary RGB, star density
    vec4 skySecondary; // secondary RGB, motion/intensity
    vec4 skyExtra;     // overall scale, centre XYZ
    vec4 skyWorld;     // camera XYZ, meteor density
};
const float PI = 3.141592653589793;
const float FAR = 1.0e19;
float sat(float v){return clamp(v,0.,1.);}
float hash31(vec3 p){p=fract(p*.1031);p+=dot(p,p.yzx+33.33);return fract((p.x+p.y)*p.z);}
float hash21(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}
vec3 hash33(vec3 p){return vec3(hash31(p+1.3),hash31(p+19.1),hash31(p+61.9));}
float n3(vec3 p){
  vec3 i=floor(p),f=fract(p);f=f*f*(3.-2.*f);
  float a=mix(hash31(i),hash31(i+vec3(1,0,0)),f.x);
  float b=mix(hash31(i+vec3(0,1,0)),hash31(i+vec3(1,1,0)),f.x);
  float c=mix(hash31(i+vec3(0,0,1)),hash31(i+vec3(1,0,1)),f.x);
  float d=mix(hash31(i+vec3(0,1,1)),hash31(i+vec3(1,1,1)),f.x);
  return mix(mix(a,b,f.y),mix(c,d,f.y),f.z);
}
float fbm(vec3 p,int quality){
  float v=0.,w=.55;
  for(int i=0;i<5;i++){
    if(i>=quality+2)break;
    v+=n3(p)*w;
    p=p*2.07+vec3(13.7,3.4,8.1);
    w*=.51;
  }
  return v;
}
float starPoint(vec3 d,float grid,float chance,float t){
  vec3 g=d*grid,id=floor(g),f=fract(g);
  float seed=hash31(id+vec3(7.3,15.1,42.4));
  vec3 ctr=.16+.68*hash33(id);
  float dist=length(f-ctr);
  float core=exp(-dist*dist*230.0);
  return step(1.-chance,seed)*core*(.76+.24*sin(t*.58+seed*71.));
}
float starfield(vec3 d,float density,float t,int quality){
  float s=starPoint(d,97.0,sat(density*.095),t);
  if(quality>=2)s+=.6*starPoint(d,169.0,sat(density*.050),t+17.);
  if(quality>=3)s+=.3*starPoint(d,259.0,sat(density*.026),t-11.);
  return s;
}
float sphereHit(vec3 origin,vec3 direction,vec3 center,float radius){
  vec3 oc=origin-center;float b=dot(oc,direction);float c=dot(oc,oc)-radius*radius;
  float det=b*b-c;if(det<=0.)return FAR;
  float sd=sqrt(det),tn=-b-sd,tf=-b+sd;
  return tn>.01?tn:(tf>.01?tf:FAR);
}
float closestDist(vec3 ray,vec3 camera,vec3 center){
  vec3 rel=center-camera;float t=max(dot(rel,ray),0.0);
  return length(rel-ray*t);
}
vec3 baseSpace(vec3 d,float t,vec3 a,vec3 b,int quality){
  vec3 base=mix(vec3(.0015,.0021,.0065),vec3(.006,.008,.020),sat(d.y*.6+.5));
  float faint=fbm(d*3.2+vec3(t*.003,0.,-t*.001),quality);
  base+=mix(a,b,.38)*pow(sat(faint-.50),3.)*.085;
  return base;
}
vec3 cosmicNebula(vec3 d,vec3 cam,vec3 center,float t,float size,vec3 a,vec3 b,int quality,float intensity){
  // Finite, feathered celestial gas field. Sample around closest approach to
  // a world-space point rather than a hard sphere silhouette (no marble edge).
  vec3 delta=center-cam;
  float reach=dot(delta,d);
  if(reach<.01)return vec3(0.);
  float radius=78.*size;
  float closest=length(delta-d*reach)/radius;
  // The exponential haze has no hard boundary and naturally tapers off.
  float envelope=exp(-closest*closest*1.25);
  if(envelope<.0008)return vec3(0.);
  vec3 offset=(cam+d*reach-center)/(radius*.55);
  vec3 p=offset+vec3(t*.016,-t*.012,t*.023);
  float folds=fbm(p*1.55+vec3(3.2,0.,9.1),quality);
  float gasA=fbm(p*2.9+vec3(t*.021,-t*.006,-t*.018),quality);
  float gasB=fbm(p*5.2+vec3(-t*.013,t*.029,t*.009)+vec3(9.2),quality);
  float filaments=pow(sat((folds*.72+gasA*.66+gasB*.25-.55)*2.5),1.55);
  float halo=pow(sat((folds+gasA)*.8),2.2);
  float density=envelope*(filaments*.74+halo*.19);
  vec3 colour=mix(a,b,sat(gasB*.73+gasA*.28));
  vec3 result=colour*density*(.42+.38*intensity)+b*pow(density,2.6)*.15;
  // A second slower background stratum adds depth and world-position parallax.
  vec3 rear=center+vec3(radius*.91,radius*.26,-radius*.42);
  float rearReach=dot(rear-cam,d);
  if(rearReach>0.){
    vec3 rearOffset=(cam+d*rearReach-rear)/(radius*.68);
    float veil=exp(-dot(rearOffset,rearOffset)*1.5);
    float rearNoise=fbm(rearOffset*1.8+vec3(t*.012,t*.021,-t*.014),max(1,quality-1));
    result+=mix(a,b,.68)*veil*pow(sat((rearNoise-.36)*1.9),1.7)*.19*intensity;
  }
  return result;
}
vec3 galacticSpiral(vec3 d,vec3 cam,vec3 center,float t,float size,vec3 a,vec3 b,int quality,float intensity){
  vec3 diff=center-cam;float distance=length(diff);vec3 axis=normalize(diff);
  // Angles around a fixed celestial centre, not camera-centred UV.
  vec3 right=normalize(cross(axis,vec3(0.19,1.0,0.13)));
  vec3 up=normalize(cross(right,axis));
  float denom=dot(d,axis);if(denom<.05)return vec3(0.);
  float travel=distance/denom;
  vec3 offset=cam+d*travel-center;
  vec2 g=vec2(dot(offset,right),dot(offset,up))/(60.*size);
  float r=length(g),theta=atan(g.y,g.x);
  float spin=theta-2.3*log(max(r,.07))+t*.025;
  float arms=pow(.5+.5*cos(spin*3.0),7.0);
  float wisps=fbm(vec3(g*2.0,t*.016),quality);
  float disk=exp(-r*r*.95);
  float arm=disk*arms*(.55+.45*wisps);
  float core=exp(-r*r*42.0);
  return mix(a,b,sat(wisps))*(arm*.78+core*.20)*intensity+vec3(1.0,.83,.69)*core*.42;
}
vec3 eventHorizon(vec3 d,vec3 cam,vec3 center,float t,float size,vec3 a,vec3 b,float intensity){
  vec3 delta=center-cam;float along=dot(delta,d);
  if(along<0.)return vec3(0.);
  float dist=length(delta-d*along);
  float radius=29.0*size;
  float disk=exp(-pow((dist-radius*1.40)/(radius*.24),2.));
  float hole=1.-smoothstep(radius*.93,radius*1.06,dist);
  vec3 tangent=normalize(cross(normalize(delta),vec3(.25,.9,.2)));
  vec3 other=normalize(cross(tangent,normalize(delta)));
  vec3 projected=(cam+d*along-center);
  float angle=atan(dot(projected,other),dot(projected,tangent));
  float swirl=.58+.42*cos(angle*5.0-t*2.1+dist*.11);
  float photon=exp(-pow((dist-radius*1.075)/(radius*.085),2.))*(.72+.28*sin(angle*7.0+t*2.3));
  float outGlow=exp(-max(dist-radius,0.)/(radius*.8))*.10;
  vec3 result=(mix(a,b,.55)*disk*swirl*.78 + b*photon*.83 + a*outGlow)*intensity;
  result*=1.0-hole*.95;
  return result;
}
vec3 auroraCurtains(vec3 d,vec3 cam,vec3 center,float t,float size,vec3 a,vec3 b,int quality,float intensity){
  // Finite curtain cells in a fixed region; no view-angle longitude stripes.
  vec3 delta=center-cam;float travel=dot(delta,d);
  if(travel<0.)return vec3(0.);
  vec3 pt=cam+d*travel;
  vec3 q=(pt-center)/(47.*size);
  float x=q.x*.90+q.z*.35;
  float noise=fbm(vec3(x*1.8,q.z*.6,t*.042),quality);
  float bands=pow(.5+.5*sin(x*6.6+sin(q.y*2.6+x*3.1+t*.35)*2.0+noise*7.7-t*.45),3.1);
  float other=pow(.5+.5*cos(x*10.0+q.y*5.5-t*.65+noise*7.0),6.5);
  float veil=exp(-q.x*q.x*1.8-q.z*q.z*.9) * smoothstep(-.55,.05,q.y) * (1.-smoothstep(.78,1.35,q.y));
  float height=sat((q.y+1.0)*.5);
  float fade=(.55+.45*sin(t*.8+x*3.7))*veil;
  return mix(a,b,height)*(bands*.58+other*.27)*fade*.72*intensity;
}
vec3 cosmicStorm(vec3 d,vec3 cam,vec3 center,float t,float size,vec3 a,vec3 b,int quality,float intensity){
  vec3 gas=cosmicNebula(d,cam,center,t*.80,size,a,b,quality,intensity);
  vec3 diff=center-cam;float along=dot(diff,d);if(along<0.)return gas*.5;
  float rad=closestDist(d,cam,center)/(72.0*size);
  float az=atan(d.z,d.x);
  float vortex=exp(-rad*rad*1.5)*pow(.5+.5*cos(rad*18.0-az*6.0-t*1.6),6.0);
  float pulse=pow(max(0.,sin(t*.36+hash31(floor(diff*.02))*11.0)),20.0);
  float flash=pulse*vortex;
  return gas*.58+b*vortex*.29*intensity+vec3(.55,.70,1.)*flash*.39;
}
vec3 voidRift(vec3 d,vec3 cam,vec3 center,float t,float size,vec3 a,vec3 b,float intensity){
  vec3 delta=center-cam;float along=dot(delta,d);if(along<0.)return vec3(0.);
  float dist=closestDist(d,cam,center),radius=40.*size;
  float theta=atan(d.z,d.x);
  float r=dist/radius;
  float edge=exp(-pow((r-(.60+.14*sin(theta*7.0-t*.6)))/.055,2.));
  float accretion=exp(-pow((r-.72)/.20,2.));
  float dash=.5+.5*sin(theta*13.0+t*.73+r*12.0);
  vec3 glow=mix(a,b,dash)*(edge*.62+accretion*.19)*intensity;
  glow+=b*pow(sat(edge),4.)*.21;
  return glow*(.75+.25*sin(t*.65+theta*9.0));
}
vec3 cosmicSilk(vec3 d,vec3 cam,vec3 center,float t,float size,vec3 a,vec3 b,int quality,float intensity){
  vec3 delta=center-cam;float along=dot(delta,d);if(along<0.)return vec3(0.);
  vec3 pt=(cam+d*along-center)/(50.*size);
  float radius=length(pt);
  float envelope=exp(-radius*radius*.72);
  float flow=fbm(pt*1.9+vec3(t*.027,-t*.022,t*.038),quality);
  float sheet=sin(pt.x*15.+pt.y*11.+sin(pt.z*5.+t*.5)*4.+flow*8.+t*.42);
  float fold=pow(1.-abs(sheet),3.0);
  return mix(a,b,sat(flow))*fold*envelope*.8*intensity;
}

// A small constellation of physically separate, world-anchored planets.
// The moons follow fixed orbits around the central gas giant.
vec3 planetarySystem(vec3 d,vec3 cam,vec3 center,float t,float size,vec3 a,vec3 b,float intensity){
  vec3 core=center+vec3(-14.*size,0.,0.);
  vec3 moon1=center+vec3(cos(t*.035)*69.,sin(t*.035)*12.,sin(t*.035)*43.)*size;
  vec3 moon2=center+vec3(cos(t*.022+2.1)*90.,sin(t*.026+1.4)*24.,sin(t*.022+2.1)*69.)*size;
  float d0=sphereHit(cam,d,core,31.*size);
  float d1=sphereHit(cam,d,moon1,12.*size);
  float d2=sphereHit(cam,d,moon2,9.*size);
  float hit=min(d0,min(d1,d2));
  float light=exp(-closestDist(d,cam,core)/(38.*size))*.032;
  vec3 rgb=mix(a,b,.5)*light;
  if(hit<FAR*.5){
    vec3 c=d0<=d1&&d0<=d2?core:(d1<=d2?moon1:moon2);
    float rad=d0<=d1&&d0<=d2?31.*size:(d1<=d2?12.*size:9.*size);
    vec3 n=normalize(cam+d*hit-c);
    float shadow=.19+.81*smoothstep(-.3,.9,dot(n,normalize(vec3(-.5,.7,-.4))));
    float latitude=atan(n.y,length(n.xz));
    float atmosphere=fbm(n*5.+vec3(t*.03,-t*.009,t*.017),2);
    float stripes=.5+.5*sin(latitude*21.+atmosphere*4.+t*.025);
    vec3 planet=mix(a*.40+.11,b*.55+.17,stripes);
    if(rad<20.*size)planet=mix(b*.21+.14,a*.48+.24,atmosphere);
    rgb=mix(rgb,planet*shadow,1.);
    rgb+=mix(a,b,.65)*pow(1.-max(0.,dot(n,-d)),3.)*.12;
  }
  // An understated orbital dust band: tied to the same fixed object.
  float along=dot(core-cam,d);
  if(along>0.){
    float dist=closestDist(d,cam,core)/(51.*size);
    float hoop=exp(-pow((dist-.98)/.06,2.));
    rgb+=mix(a,b,.75)*hoop*.14*(.8+.2*sin(t*.16));
  }
  return rgb*intensity;
}
vec3 supernova(vec3 d,vec3 cam,vec3 center,float t,float size,vec3 a,vec3 b,float intensity){
  vec3 delta=center-cam;
  if(dot(delta,d)<0.)return vec3(0.);
  float r=closestDist(d,cam,center)/(48.*size);
  float beat=.81+.19*sin(t*.22);
  float nucleus=exp(-r*r*70.);
  float rays=pow(abs(sin(atan(d.y,d.x)*19.+t*.17)),19.);
  float shell=0.;
  for(int i=0;i<3;i++){
    float age=fract(t*.022+float(i)/3.);
    shell+=exp(-pow((r-(.18+age*1.5))/.033,2.))*(1.-age)*.68;
  }
  float corona=exp(-r*3.9)*(.5+.5*rays);
  return (mix(a,b,.52)*(shell*.68+corona*.22)+vec3(.96,.79,.85)*nucleus*.78*beat)*intensity;
}
vec3 plasmaVortex(vec3 d,vec3 cam,vec3 center,float t,float size,vec3 a,vec3 b,int quality,float intensity){
  vec3 delta=center-cam;float forward=dot(delta,d);
  if(forward<.1)return vec3(0.);
  vec3 axis=normalize(delta);
  vec3 right=normalize(cross(axis,vec3(.12,1.,.07)));
  vec3 up=normalize(cross(right,axis));
  vec3 q=(cam+d*(length(delta)/max(dot(d,axis),.045))-center)/(63.*size);
  float r=length(q),theta=atan(dot(q,up),dot(q,right));
  float turbulence=fbm(q*2.3+vec3(t*.035,-t*.017,t*.023),quality);
  float spin=theta*6.+log(max(r,.01))*8.-t*.74+turbulence*3.;
  float arc=pow(.5+.5*cos(spin),17.);
  float arc2=pow(.5+.5*sin(spin*.55+r*12.+t*.5),15.);
  float envelope=exp(-r*r*.93);
  float core=exp(-r*r*67.);
  return (mix(a,b,sat(turbulence))*(arc*.74+arc2*.37)*envelope
          +vec3(.91,.82,.95)*core*.42)*intensity;
}

float meteor(vec3 d,float t,float density){
  // Deterministic events. A moving streak exists only during a short life-cycle.
  // A 2D celestial billboard is enough for a tiny fast-moving distant meteor.
  if(density<.01)return 0.;
  vec3 result=vec3(0.);
  float value=0.;
  for(int i=0;i<3;i++){
    float idx=floor(t*.16+float(i)*2.47);
    float seed=hash21(vec2(idx,float(i)*11.));
    float age=fract(t*.16+float(i)*2.47);
    if(seed>min(.75,density*.3)||age>.28)continue;
    vec3 from=normalize(vec3(hash21(vec2(idx+3.,i))*2.-1.,.18+hash21(vec2(idx+7.,i))*.83,hash21(vec2(idx+11.,i))*2.-1.));
    vec3 velocity=normalize(vec3(-from.z,.08,from.x));
    vec3 head=normalize(from+velocity*age*.8);
    vec3 tail=normalize(from+velocity*max(age-.10,0.)*.8);
    float h=dot(d,head),crossLine=length(cross(d,velocity));
    float spot=exp(-max(0.,1.-h)*24000.);
    float trail=exp(-crossLine*crossLine*17000.)*step(dot(d,tail),h)*smoothstep(0.,.05,age)*(1.-smoothstep(.16,.28,age));
    value=max(value,spot*.74+trail*.5);
  }
  return value;
}
void main(){
  vec3 d=normalize(v_SkyDirection);
  vec3 cam=skyWorld.xyz,anchor=skyExtra.yzw;
  float t=skyControl.x,motion=skySecondary.w;
  int quality=int(clamp(skyControl.z,1.,3.));
  int mode=int(skyControl.y+.5);
  float size=clamp(skyExtra.x*.52,.5,2.4);
  vec3 a=clamp(skyPrimary.rgb,0.,1.),b=clamp(skySecondary.rgb,0.,1.);
  float density=skyPrimary.w;
  float anim=t*motion;
  vec3 rgb=baseSpace(d,anim,a,b,quality);
  float star=starfield(d,density,anim,quality);
  rgb+=mix(vec3(.65,.78,1.),b,.16)*star*1.45;
  vec3 fx=vec3(0.);
  if(mode==0)fx=cosmicNebula(d,cam,anchor,anim,size,a,b,quality,1.0); // Living Nebula
  else if(mode==1)fx=galacticSpiral(d,cam,anchor,anim,size,a,b,quality,1.0); // Galaxy
  else if(mode==2)fx=eventHorizon(d,cam,anchor,anim,size,a,b,1.0); // Black hole
  else if(mode==3)fx=cosmicStorm(d,cam,anchor,anim,size,a,b,quality,1.0); // Storm
  else if(mode==4)fx=voidRift(d,cam,anchor,anim,size,a,b,1.0); // Void Rift
  else if(mode==5)fx=auroraCurtains(d,cam,anchor,anim,size,a,b,quality,1.0); // Aurora
  else if(mode==6)fx=cosmicSilk(d,cam,anchor,anim,size,a,b,quality,1.0); // Silk
  else if(mode==7){ // Eclipse
    fx=eventHorizon(d,cam,anchor,anim,size,a,b,.76);
    float c=closestDist(d,cam,anchor)/(32.0*size);
    float occult=1.0-smoothstep(.92,1.0,c);
    rgb*=1.0-occult*.995;
  }
  else if(mode==8)fx=cosmicNebula(d,cam,anchor,anim*.45,size*.72,a,b,quality,.025); // Meteor Shower
  else if(mode==9){ // Starfield+: mostly distant stars, with restrained animated dust
     float haze=fbm(d*6.2+vec3(anim*.008,0.,anim*.005),quality);
     fx=mix(a,b,haze)*pow(sat(haze-.42),3.)*.18;
     rgb+=mix(b,vec3(.8,.9,1.),.65)*starfield(d,density*2.7,anim*1.7,quality);
  }
  else if(mode==10)fx=planetarySystem(d,cam,anchor,anim,size,a,b,1.0); // Fixed constellation of orbiting planets
  else if(mode==11)fx=supernova(d,cam,anchor,anim,size,a,b,1.0); // Expanding shock shells
  else if(mode==12)fx=plasmaVortex(d,cam,anchor,anim,size,a,b,quality,1.0); // Rotating plasma arcs
  // Moving celestial streaks on several modes, separately adjustable.
  float meteorDensity=skyWorld.w;
  if(mode==8)meteorDensity*=3.0;
  if(mode==9)meteorDensity*=.55;
  if(mode==0||mode==1||mode==3||mode==8||mode==9||mode==10||mode==11||mode==12)rgb+=vec3(.63,.82,1.)*meteor(d,anim,meteorDensity);
  // Softly tone map, preserving details without excessively saturated blocks.
  rgb+=fx;
  rgb=vec3(1.)-exp(-rgb*skyControl.w*1.60);
  rgb*=mix(.28,1.,smoothstep(-.44,.17,d.y));
  color=vec4(clamp(rgb,0.,1.),1.);
}
