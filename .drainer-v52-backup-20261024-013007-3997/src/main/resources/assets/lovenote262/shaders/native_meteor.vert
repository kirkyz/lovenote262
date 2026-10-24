#version 330 core
layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 Texture;
layout(location = 2) in vec4 Color;
layout(std140) uniform MeshData { mat4 u_Proj; mat4 u_ModelView; };
layout(std140) uniform MeteorData {
    vec4 cameraTime;
    vec4 tileAltitudeLength;
    vec4 primary;
    vec4 secondary;
    vec4 extras;
};
out vec2 v_UV;
out vec3 v_Color;
out float v_Alpha;
out float v_Type;
float hash21(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453123);}
void main(){
    int encoded=int(floor(Position.z+0.5));
    int tile=encoded/12;
    int particle=encoded-tile*12;
    int ix=tile%5-2;
    int iz=tile/5-2;
    vec2 id=tileAltitudeLength.xy+vec2(float(ix),float(iz));
    float seed=hash21(id*7.31+float(particle)*15.7);
    float seed2=hash21(id*19.43+float(particle)*5.9);
    float seed3=hash21(id*0.79+float(particle)*31.9);
    vec2 home=(id+vec2(0.1+0.8*seed,0.1+0.8*seed2))*extras.z;
    float t=max(0.0,cameraTime.w);
    float local=t*max(0.01,secondary.w)/(6.0+seed2*8.0)+seed*41.0;
    float cycle=fract(local);
    // Values are sampled by absolute world cell, not camera position.
    // Type 0: migrating meteor with a bright head and tapering tail;
    // Type 1: a fixed, slowly twinkling spark near the meteor field.
    bool isStar=(particle >= 3);
    float altitude=tileAltitudeLength.z+38.0*(seed3-0.5)+float(particle%3)*22.0;
    vec3 world;
    float alpha;
    float width;
    vec3 head;
    vec3 tail;
    if (isStar) {
        head=vec3(home.x,altitude+seed2*42.0,home.y);
        tail=head;
        width=0.7+seed3*1.3;
        alpha=extras.y*(0.35+0.65*pow(0.5+0.5*sin(t*(0.4+1.9*seed)+seed3*6.283185),3.0));
    } else {
        float progress=cycle;
        float theta=2.1+seed3*0.9;
        vec3 direction=normalize(vec3(cos(theta),-0.12-0.25*seed2,sin(theta)));
        float travelled=(progress-0.5)*(240.0+seed2*170.0);
        head=vec3(home.x,altitude,home.y)+direction*travelled;
        tail=head-direction*tileAltitudeLength.w*(0.65+0.8*seed2);
        width=1.6+seed3*2.4;
        float fadeIn=smoothstep(0.04,0.20,progress);
        float fadeOut=1.0-smoothstep(0.72,0.99,progress);
        alpha=fadeIn*fadeOut;
    }
    vec3 along=normalize(head-tail+vec3(0.0001,0.0001,0.0001));
    vec3 middle=(head+tail)*0.5;
    vec3 look=normalize(vec3(cameraTime.x,cameraTime.y,cameraTime.z)-middle);
    vec3 sideways=normalize(cross(along,look));
    if(length(sideways)<0.001)sideways=normalize(cross(along,vec3(0,1,0)));
    if(isStar){
        // Fixed sparks face the camera but are anchored in world space.
        vec3 right=normalize(cross(vec3(0,1,0),look));
        if(length(right)<0.001)right=vec3(1,0,0);
        vec3 up=normalize(cross(look,right));
        world=middle+right*(Texture.x-0.5)*width+up*(Texture.y-0.5)*width;
    } else {
        world=mix(tail,head,Texture.y)+sideways*(Texture.x-0.5)*width;
    }
    // World position is fixed independently of camera translation/rotation.
    vec3 relative=vec3(world.x-cameraTime.x,world.y,world.z-cameraTime.z);
    gl_Position=u_Proj*u_ModelView*vec4(relative,1.0);
    float dist=length(world-vec3(cameraTime.xyz));
    alpha*=1.0-smoothstep(280.0,690.0,dist);
    alpha*=clamp(primary.w*extras.x,0.0,1.0);
    v_UV=Texture;
    v_Type=isStar?1.0:0.0;
    v_Color=mix(primary.rgb,secondary.rgb,seed2);
    v_Alpha=alpha;
}
