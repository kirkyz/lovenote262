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
    private final NumberSetting distance=add(new NumberSetting("Distance",18,8,40,1));
    private final NumberSetting size=add(new NumberSetting("Size",.25,.10,.8,.05));
    private final NumberSetting spin=add(new NumberSetting("Rotation speed",.65,0,3,.05));
    private final NumberSetting opacity=add(new NumberSetting("Opacity",125,20,255,5));
    private final BoolSetting depth=add(new BoolSetting("Occluded by blocks",true));
    public FloatingRelics(){super("FloatingRelics",Category.VISUALS);describe("Drifting 3D crystals, cubes and halos in stable world coordinates, with adjustable density and colour.");}
    private int tint(float f){return switch(colour.get()){
        case "Rainbow"->Colors.rainbow(f);case "Ice"->0xFF73E5FF;case "Gold"->0xFFFFC363;
        case "Pink"->0xFFFF79D6;default->Colors.wave(f);};}
    @Override public void onRender3D(WorldRenderContext ctx,PoseStack stack){
        if(mc.level==null||mc.player==null)return;
        Matrix4f mat=stack.last().pose();var cam=ctx.camera().position();
        int n=VisualOptimizer.budget(count.getI(),2);
        double time=System.nanoTime()*1e-9;
        double range=distance.get(),cell=4.5;
        double px=mc.player.getX(),py=mc.player.getY()+1,pz=mc.player.getZ();
        int gx=(int)Math.floor(px/cell),gy=(int)Math.floor(py/cell),gz=(int)Math.floor(pz/cell);
        MeshBuilder b=depth.get()?Fx.colorQuadsDepth():Fx.colorQuads();
        int drawn=0;
        // World-space seeded cells: neither a camera-relative orbit nor new random positions each frame.
        // Each cell keeps the same coordinates/shape until the player leaves its render distance.
        for(int ix=gx-5;ix<=gx+5 && drawn<n;ix++){
          for(int iz=gz-5;iz<=gz+5 && drawn<n;iz++){
            for(int iy=gy-4;iy<=gy+4 && drawn<n;iy++){
              float seed=Vfx.hash(ix*1789+iy*83,iz*911);
              if(seed>.072f)continue; // sparse world-distributed relics
              double x=(ix+.2+.6*Vfx.hash(ix,iz))*cell;
              double z=(iz+.2+.6*Vfx.hash(iz,iy))*cell;
              double baseY=(iy+.2+.6*Vfx.hash(iy,ix))*cell;
              double r2=(x-px)*(x-px)+(baseY-py)*(baseY-py)+(z-pz)*(z-pz);
              if(r2>range*range || r2<5.0)continue;
              // Bob, not player-follow: baseY remains fixed to the cell's WORLD coordinates.
              double y=baseY+Math.sin(time*.75+seed*6.28)*.25;
              float fade=(float)Math.max(0,1-Math.sqrt(r2)/range);
              int c=Colors.alpha(tint(seed),(int)(opacity.get()*Math.min(1,fade*2.8)));
              if((c>>>24)<5)continue;
              double yaw=time*spin.get()*(seed>.5?1:-1)+seed*6.283;
              double pitch=Math.sin(time*.37+seed*18)*.30;
              double half=size.get()*(.7+.5*seed);
              Vfx.P p=new Vfx.P(x,y,z);
              int shape=style.is("Mixed")?Math.abs(ix+iz+iy)%3:style.is("Crystal")?0:style.is("Cube")?1:2;
              if(shape==0)Vfx.crystal(b,mat,p,half*.72,half*1.4,yaw,pitch,0,.012f,c,c,cam.x,cam.y,cam.z);
              else if(shape==1)Vfx.cube(b,mat,p,half,yaw,pitch,yaw*.4,.012f,c,cam.x,cam.y,cam.z);
              else for(int j=0;j<20;j++){
                double a=j*Math.PI/10+yaw, q=(j+1)*Math.PI/10+yaw;
                Vfx.tube(b,mat,x+Math.cos(a)*half,y+Math.sin(a)*half,z,
                  x+Math.cos(q)*half,y+Math.sin(q)*half,z,.012f,c,c,cam.x,cam.y,cam.z);
              }
              drawn++;
            }
          }
        }
        Fx.draw();
    }
}
