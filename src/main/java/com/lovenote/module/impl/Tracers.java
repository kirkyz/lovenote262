package com.lovenote.module.impl;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.NumberSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.util.*;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
/** Simple nearby entity tracers, respecting terrain occlusion by default. */
public final class Tracers extends Module {
    private final NumberSetting radius=add(new NumberSetting("Range",24,4,50,1));
    private final NumberSetting max=add(new NumberSetting("Max tracers",24,1,60,1));
    private final NumberSetting opacity=add(new NumberSetting("Opacity",165,10,255,5));
    private final NumberSetting width=add(new NumberSetting("Line width",0.014,0.005,0.06,0.005));
    private final ModeSetting colour=add(new ModeSetting("Colour","Accent","Accent","Cyan","Pink","Rainbow"));
    public Tracers(){super("Tracers",Category.VISUALS);describe("Depth-tested lines to nearby living entities.");}
    @Override public void onRender3D(WorldRenderContext ctx,PoseStack stack){
        if(mc.player==null||mc.level==null)return;
        Matrix4f mat=stack.last().pose();var cam=ctx.camera().position();
        MeshBuilder mesh=null;int n=0;
        double maxDistance=radius.get()*radius.get();
        double x0=cam.x,y0=cam.y-.16,z0=cam.z;
        for(Entity e:mc.level.entitiesForRendering()){
            if(e==mc.player||!(e instanceof LivingEntity)||!e.isAlive()||e.distanceToSqr(mc.player)>maxDistance)continue;
            int base=switch(colour.get()){
                case "Cyan"->0xFF60DEFF;case "Pink"->0xFFFF7AD8;
                case "Rainbow"->Colors.rainbow(n*.08f);default->Colors.wave(n*.07f);};
            int c=Colors.alpha(base,opacity.getI());
            if (mesh == null) mesh = Fx.colorQuadsDepth();
            Vfx.tube(mesh,mat,x0,y0,z0,e.getX(),e.getY()+e.getBbHeight()*.5,e.getZ(),
                width.getF(),c,c,cam.x,cam.y,cam.z);
            if(++n>=VisualOptimizer.budget(max.getI(), 2))break;
        }
        if(n>0)Fx.draw();
    }
}
