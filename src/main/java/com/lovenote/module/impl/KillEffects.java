package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Fx;
import com.lovenote.util.Vfx;
import com.lovenote.util.WorldRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;

/** Visual burst on observed death of the last entity you attacked. */
public final class KillEffects extends Module {
    private final ModeSetting colour=add(new ModeSetting("Colour", "Accent", "Accent", "Rainbow", "Pink", "Cyan"));
    private final NumberSetting duration=add(new NumberSetting("Duration (seconds)",1.2,0.3,3.0,0.1));
    private final NumberSetting radius=add(new NumberSetting("Ring size",1.4,0.3,3.5,0.1));
    private final NumberSetting opacity=add(new NumberSetting("Opacity",180,20,255,5));
    private LivingEntity watched;
    private long watchUntil,started;
    private double x,y,z;
    public KillEffects(){ super("KillEffects",Category.VISUALS);
        describe("Depth-tested expanding rings if the most recently attacked living entity dies nearby."); }
    @Override public void onAttack(Entity target){
        if(target instanceof LivingEntity living){watched=living;watchUntil=System.currentTimeMillis()+6000;}
    }
    @Override public void onTick(){
        if(watched==null)return;
        if(mc.level==null||System.currentTimeMillis()>watchUntil){watched=null;return;}
        if(!watched.isAlive()){
            x=watched.getX(); y=watched.getY()+0.1; z=watched.getZ();
            started=System.currentTimeMillis(); watched=null;
        }
    }
    @Override public void onDisable(){watched=null;started=0;}
    @Override public void onRender3D(WorldRenderContext ctx,PoseStack stack){
        if(started==0||mc.level==null)return;
        float phase=(float)((System.currentTimeMillis()-started)/1000.0/duration.get());
        if(phase>=1){started=0;return;}
        Matrix4f mat=stack.last().pose();
        int base=switch(colour.get()){
            case "Rainbow" -> Colors.rainbow(0);
            case "Pink" -> 0xFFFF88CE;
            case "Cyan" -> 0xFF73E8FF;
            default -> Colors.wave(0);
        };
        int c=Colors.alpha(base,(int)(opacity.get()*(1-phase)));
        MeshBuilder mesh=Fx.colorQuadsDepth();
        double r=radius.get()*(0.1+phase);
        Vfx.ringFlat(mesh,mat,x,y,z,r,r+0.07,40,c,Colors.alpha(c,0));
        Vfx.ringFlat(mesh,mat,x,y+0.25,z,r*0.72,r*0.72+0.045,40,c,Colors.alpha(c,0));
        Fx.draw();
    }
}
