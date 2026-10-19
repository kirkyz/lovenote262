package com.lovenote.module.impl;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.item.ItemEntity;
/** Low-cost proximity list of dropped items; 3D beam renderer remains separate work. */
public final class LootBeam extends Module {
    private final NumberSetting radius=add(new NumberSetting("Radius",16,4,64,2));
    private final NumberSetting display=add(new NumberSetting("Max displayed",3,1,8,1));
    private int tick;private String[] entries=new String[0];
    public LootBeam(){super("LootBeam",Category.DONUT);alsoIn.add(Category.HUD);describe("Shows nearby dropped-item names and distances. 3D beams not yet implemented.");}
    @Override public void onTick(){
        if(mc.player==null||mc.level==null){entries=new String[0];return;}
        if(--tick>0)return;tick=20;
        java.util.ArrayList<String> names=new java.util.ArrayList<>();
        for(var e:mc.level.entitiesForRendering()){
            if(!(e instanceof ItemEntity item))continue;
            if(mc.player.distanceToSqr(item)>radius.get()*radius.get())continue;
            if(names.size()>=display.getI())break;
            names.add(item.getItem().getHoverName().getString()+" · "+(int)Math.sqrt(mc.player.distanceToSqr(item))+"m");
        }
        entries=names.toArray(String[]::new);
    }
    @Override public void onHud(GuiGraphicsExtractor g,float delta){
        if(mc.player==null)return;
        for(int i=0;i<entries.length;i++){
            String s="DROP  |  "+entries[i];
            Ui.pill(g,8,185+i*19,Ui.textW(s)+12,18);Ui.text(g,s,14,190+i*19,0xFFEBD1FF);
        }
    }
}
