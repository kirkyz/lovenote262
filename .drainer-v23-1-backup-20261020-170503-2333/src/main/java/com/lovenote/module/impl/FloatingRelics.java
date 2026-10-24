package com.lovenote.module.impl;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Fx;
import com.lovenote.util.Vfx;
import com.lovenote.util.Tex;
import com.lovenote.util.WorldRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import org.joml.Matrix4f;

/** Original 26.2 world-space relics: no native library, bytecode or Anubis shader dependency. */
public final class FloatingRelics extends Module {
    private final ModeSetting style=add(new ModeSetting("Relic shape","Mixed","Mixed","Crystal","Cube","Halo"));
    private final ModeSetting colour=add(new ModeSetting("Colour","Theme","Theme","Rainbow","Ice","Gold","Pink"));
    private final NumberSetting count=add(new NumberSetting("Relics",12,2,40,1));
    private final NumberSetting distance=add(new NumberSetting("Distance",13,5,30,1));
    private final NumberSetting size=add(new NumberSetting("Size",.48,.15,1.5,.05));
    private final NumberSetting spin=add(new NumberSetting("Rotation speed",.65,0,3,.05));
    private final NumberSetting opacity=add(new NumberSetting("Opacity",165,20,255,5));
    private final BoolSetting depth=add(new BoolSetting("Occluded by blocks",true));
    public FloatingRelics(){super("FloatingRelics",Category.VISUALS);describe("Drifting 3D crystals, cubes and halos in stable world coordinates, with adjustable density and colour.");}
    private int tint(float f){return switch(colour.get()){
        case "Rainbow"->Colors.rainbow(f);case "Ice"->0xFF73E5FF;case "Gold"->0xFFFFC363;
        case "Pink"->0xFFFF79D6;default->Colors.wave(f);};}
    @Override public void onRender3D(WorldRenderContext ctx,PoseStack stack){
        if(mc.level==null||mc.player==null)return;
        Matrix4f mat=stack.last().pose();var cam=ctx.camera().position();
        int n=VisualOptimizer.budget(count.getI(),2);double t=System.nanoTime()*1e-9;
        double range=distance.get(),cell=Math.max(3,range*.35);double px=mc.player.getX(),py=mc.player.getY()+1,pz=mc.player.getZ();
        int gx=(int)Math.floor(px/cell),gy=(int)Math.floor(py/cell),gz=(int)Math.floor(pz/cell);
        MeshBuilder b=depth.get()?Fx.colorQuadsDepth():Fx.colorQuads();
        for(int i=0;i<n;i++){
            int tile=(i*11)%125,ix=gx+tile%5-2,iy=gy+(tile/5)%5-2,iz=gz+(tile/25)%5-2;
            float seed=Vfx.hash(ix*1789+iy,iz*911+i/125);
            double x=(ix+.22+.6*Vfx.hash(ix,iz))*cell;
            double y=(iy+.22+.6*Vfx.hash(iy,ix))*cell+Math.sin(t*.7+i)*.33;
            double z=(iz+.22+.6*Vfx.hash(iz,iy))*cell;
            double r2=(x-px)*(x-px)+(y-py)*(y-py)+(z-pz)*(z-pz);
            if(r2>range*range||r2<4)continue;
            float alpha=(float)Math.max(0,1-Math.sqrt(r2)/range);
            int c=Colors.alpha(tint(seed),(int)(opacity.get()*Math.min(1,alpha*3)));
            double yaw=t*spin.get()*(seed>.5?1:-1)+seed*6.283,pitch=Math.sin(t*.37+i)*.30;
            double half=size.get()*(.7+.5*seed);
            Vfx.P p=new Vfx.P(x,y,z);
            int k=style.is("Mixed")?i%3:style.is("Crystal")?0:style.is("Cube")?1:2;
            if(k==0)Vfx.crystal(b,mat,p,half*.72,half*1.4,yaw,pitch,0,.014f,c,c,cam.x,cam.y,cam.z);
            if(k==1)Vfx.cube(b,mat,p,half,yaw,pitch,yaw*.4,.014f,c,cam.x,cam.y,cam.z);
            if(k==2){int segments=14;for(int j=0;j<segments;j++){
                 double a=j*Math.PI*2/segments+yaw,z0=(j+1)*Math.PI*2/segments+yaw;
                 Vfx.tube(b,mat,x+Math.cos(a)*half,y+Math.sin(a)*half,z,x+Math.cos(z0)*half,y+Math.sin(z0)*half,z,.016f,c,c,cam.x,cam.y,cam.z);
            }}
        }
        Fx.draw();
    }
}
