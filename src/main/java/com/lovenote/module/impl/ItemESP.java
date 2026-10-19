package com.lovenote.module.impl;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.NumberSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.util.*;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.joml.Matrix4f;
/** Depth-tested item markers for loaded, nearby dropped items. */
public final class ItemESP extends Module {
    private final NumberSetting radius=add(new NumberSetting("Range",20,4,40,1));
    private final NumberSetting max=add(new NumberSetting("Max items",35,1,80,1));
    private final NumberSetting opacity=add(new NumberSetting("Opacity",175,10,255,5));
    private final NumberSetting size=add(new NumberSetting("Ring size",0.38,0.15,0.8,0.05));
    private final ModeSetting colour=add(new ModeSetting("Colour","Accent","Accent","Cyan","Gold","Rainbow"));
    public ItemESP(){super("ItemESP",Category.VISUALS);describe("Depth-tested rings around nearby dropped items.");}
    @Override public void onRender3D(WorldRenderContext ctx,PoseStack stack){
        if(mc.player==null||mc.level==null)return;
        Matrix4f mat=stack.last().pose(); var cam=ctx.camera().position();
        MeshBuilder mesh=null; int n=0;
        double maxDistance=radius.get()*radius.get();
        for(Entity e:mc.level.entitiesForRendering()){
            if(!(e instanceof ItemEntity)||e.distanceToSqr(mc.player)>maxDistance)continue;
            int base=switch(colour.get()){
                case "Cyan"->0xFF70E8FF;case "Gold"->0xFFFFD378;
                case "Rainbow"->Colors.rainbow(n*.09f);default->Colors.wave(n*.1f);};
            int c=Colors.alpha(base,opacity.getI());
            if (mesh == null) mesh=Fx.colorQuadsDepth();
            double x=e.getX(),y=e.getY()+0.05,z=e.getZ(),r=size.get();
            for(int i=0;i<12;i++){
                double a=i*Math.PI/6,b=(i+1)*Math.PI/6;
                Vfx.tube(mesh,mat,x+Math.cos(a)*r,y,z+Math.sin(a)*r,
                    x+Math.cos(b)*r,y,z+Math.sin(b)*r,.018f,c,c,cam.x,cam.y,cam.z);
            }
            if(++n>=VisualOptimizer.budget(max.getI(), 2))break;
        }
        if(n>0)Fx.draw();
    }
}
