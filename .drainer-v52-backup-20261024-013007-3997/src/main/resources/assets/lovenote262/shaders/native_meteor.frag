#version 330 core
in vec2 v_UV;
in vec3 v_Color;
in float v_Alpha;
in float v_Type;
out vec4 color;
void main(){
    float across=1.0-abs(v_UV.x*2.0-1.0);
    float glow=pow(max(across,0.0),2.2);
    float a;
    if(v_Type>0.5){
        float r=length(v_UV-vec2(0.5));
        a=(1.0-smoothstep(0.06,0.49,r))*v_Alpha;
    }else{
        // Bright leading head at y=1 with a decaying, smooth tail.
        float head=pow(clamp(v_UV.y,0.0,1.0),1.6);
        a=(0.12+0.88*head)*glow*v_Alpha;
    }
    if(a<0.004)discard;
    color=vec4(clamp(v_Color*(0.68+0.58*glow),0.0,1.0),clamp(a,0.0,0.94));
}
