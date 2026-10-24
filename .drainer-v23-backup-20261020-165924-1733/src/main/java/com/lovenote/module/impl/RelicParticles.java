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
import java.util.Random;
import net.minecraft.world.entity.Entity;
/** Event-driven glow particles. Bounded pool, no world writes or continuous new allocations per render. */
public final class RelicParticles extends Module {
    private final ModeSetting colour=add(new ModeSetting("Colour","Theme","Theme","Gold","Cyan","Rainbow","Rose"));
    private final BoolSetting hits=add(new BoolSetting("Combat sparks",true));
    private final BoolSetting pops=add(new BoolSetting("Totem glyph burst",true));
    private final BoolSetting footsteps=add(new BoolSetting("Footstep glimmers",false));
    private final NumberSetting density=add(new NumberSetting("Particles per burst",18,3,45,1));
    private final NumberSetting size=add(new NumberSetting("Size",.16,.05,.5,.01));
    private final NumberSetting opacity=add(new NumberSetting("Opacity",190,30,255,5));
    private final Random random=new Random();
    private static final class P {double x,y,z,vx,vy,vz;int ticks,max;P(double x,double y,double z,double vx,double vy,double vz,int max){this.x=x;this.y=y;this.z=z;this.vx=vx;this.vy=vy;this.vz=vz;this.max=max;}}
    private final List<P> live=new ArrayList<>();private int stepTick;
    public RelicParticles(){super("RelicParticles",Category.VISUALS);describe("Gold/cyan glyph sparks from hits, pops and optional footsteps; adaptive particle budgets.");}
    private void burst(double x,double y,double z,int requested){
       int n=VisualOptimizer.budget(requested,2);
       for(int i=0;i<n;i++){
          if(live.size()>=240)live.remove(0);
          double angle=random.nextDouble()*Math.PI*2,spread=.03+random.nextDouble()*.09;
          live.add(new P(x,y,z,Math.cos(angle)*spread,(random.nextDouble()-.1)*.09,Math.sin(angle)*spread,12+random.nextInt(24)));
       }
    }
    @Override public void onAttack(Entity e){if(hits.get()&&e!=null)burst(e.getX(),e.getY()+e.getBbHeight()*.60,e.getZ(),density.getI());}
    @Override public void onTotemPop(Entity e){if(pops.get()&&e!=null)burst(e.getX(),e.getY()+1,e.getZ(),density.getI()*2);}
    @Override public void onTick(){
       if(mc.level==null){live.clear();return;}
       if(footsteps.get()&&mc.player!=null&&++stepTick>=8){stepTick=0;
          if(mc.player.onGround()&&mc.player.getDeltaMovement().horizontalDistanceSqr()>.004)
             burst(mc.player.getX(),mc.player.getY()+.06,mc.player.getZ(),3);
       }
       live.removeIf(p->++p.ticks>=p.max);
       for(P p:live){p.x+=p.vx;p.y+=p.vy;p.z+=p.vz;p.vx*=.97;p.vz*=.97;p.vy-=.0015;}
    }
    @Override public void onDisable(){live.clear();}
    @Override public void onRender3D(WorldRenderContext ctx,PoseStack s){
       if(mc.player==null||live.isEmpty())return;
       Matrix4f m=s.last().pose();Fx.cameraBasis(ctx.camera());Tex.ensure();Fx.texture(Tex.STAR);
       MeshBuilder b=Fx.texQuadsDepth();int max=VisualOptimizer.budget(live.size(),0),drawn=0;
       for(P p:live){if(drawn++>=max)break;
         float life=1-p.ticks/(float)p.max;
         int c=switch(colour.get()){case "Gold"->0xFFFFCB75;case "Cyan"->0xFF7BF2FF;
            case "Rainbow"->Colors.rainbow(life);case "Rose"->0xFFFF78C8;default->Colors.wave(life);};
         Fx.billboard(b,m,(float)p.x,(float)p.y,(float)p.z,size.getF()*life,Colors.alpha(c,(int)(life*opacity.getI())));
       }
       Fx.draw();
    }
}
