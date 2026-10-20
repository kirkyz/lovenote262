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

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.Entity;
/** An original short-lived wireframe spirit, triggered by the already-wired totem-pop event. */
public final class TotemSpirit extends Module {
    private final ModeSetting colour=add(new ModeSetting("Colour","Theme","Theme","Cyan","Gold","White","Rainbow"));
    private final NumberSetting lifetime=add(new NumberSetting("Lifetime (s)",1.7,.5,5,.1));
    private final NumberSetting size=add(new NumberSetting("Spirit size",1.1,.5,2.5,.1));
    private final NumberSetting opacity=add(new NumberSetting("Opacity",210,30,255,5));
    private final BoolSetting rings=add(new BoolSetting("Spirit rings",true));
    private final BoolSetting preview=add(new BoolSetting("Demo spirit every 7s",true));
    private long nextPreview;
    private record Spirit(double x,double y,double z,long born){}
    private final List<Spirit> active=new ArrayList<>();
    public TotemSpirit(){super("TotemSpirit",Category.VISUALS);describe("Animated rising 3D spirit shown after observed totem pops, with fading glyph rings.");}
    @Override public void onTotemPop(Entity e){if(e!=null){if(active.size()>=10)active.remove(0);active.add(new Spirit(e.getX(),e.getY()+.1,e.getZ(),System.nanoTime()));}}
    @Override public void onDisable(){active.clear();nextPreview=0;}
    @Override public void onTick(){
       // Ordinary effect is only triggered by an actual observed totem pop.
       // Preview makes the model visible for testing even when nobody pops a totem.
       if(preview.get()&&mc.player!=null&&mc.level!=null&&mc.gui.screen()==null){
          long now=System.nanoTime();
          if(now>=nextPreview){nextPreview=now+7_000_000_000L;onTotemPop(mc.player);}
       }
    }
    @Override public void onRender3D(WorldRenderContext ctx,PoseStack s){
       if(mc.level==null||active.isEmpty())return;
       long now=System.nanoTime();double duration=lifetime.get()*1e9;
       active.removeIf(p->(now-p.born())>duration || now<p.born());if(active.isEmpty())return;
       var cam=ctx.camera().position();Matrix4f m=s.last().pose();MeshBuilder mesh=Fx.colorQuadsDepth();
       for(Spirit spirit:active){
          double progress=(now-spirit.born())/duration;
          double x=spirit.x(),y=spirit.y()+progress*2.1,z=spirit.z();
          double k=size.get()*(1+.13*Math.sin(progress*7)),a=opacity.get()*(1-progress)*(1-progress);
          int col=Colors.alpha(switch(colour.get()){
              case "Cyan"->0xFF80F5FF;case "Gold"->0xFFFFDC81;case "White"->0xFFFFFFFF;
              case "Rainbow"->Colors.rainbow((float)progress);default->Colors.wave((float)progress);
          },(int)a);
          if(a<3)continue;
          // Geometric humanoid pose: head, core, animated reaching arms and tapered legs.
          double wave=Math.sin(progress*10)*.13*k;
          Vfx.cube(mesh,m,new Vfx.P(x,y+1.6*k,z),.23*k,progress*3,.1,0,.012f,col,cam.x,cam.y,cam.z);
          Vfx.tube(mesh,m,x,y+.55*k,z,x,y+1.35*k,z,.028f,col,col,cam.x,cam.y,cam.z);
          Vfx.tube(mesh,m,x,y+1.20*k,z,x-.7*k,y+(.88+wave)*k,z,.025f,col,col,cam.x,cam.y,cam.z);
          Vfx.tube(mesh,m,x,y+1.20*k,z,x+.7*k,y+(.88-wave)*k,z,.025f,col,col,cam.x,cam.y,cam.z);
          Vfx.tube(mesh,m,x,y+.55*k,z,x-.4*k,y,z,.025f,col,col,cam.x,cam.y,cam.z);
          Vfx.tube(mesh,m,x,y+.55*k,z,x+.4*k,y,z,.025f,col,col,cam.x,cam.y,cam.z);
          if(rings.get())Vfx.ringFlat(mesh,m,x,y+.06*k,z,.45*k+progress*.7,.49*k+progress*.7,28,col,Colors.alpha(col,0));
       }
       Fx.draw();
    }
}
