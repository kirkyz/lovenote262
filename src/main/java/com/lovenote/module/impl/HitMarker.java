package com.lovenote.module.impl;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.util.Gfx;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
/** Draw an X briefly on outgoing attacks, not confirmed hits. */
public final class HitMarker extends Module {
    private int remaining;
    public HitMarker(){super("HitMarker",Category.VISUALS);describe("Visual marker for attack attempts on living entities.");}
    @Override public void onAttack(Entity target){if(target instanceof LivingEntity)remaining=6;}
    @Override public void onTick(){if(remaining>0)remaining--;}
    @Override public void onHud(GuiGraphicsExtractor g,float delta){
        if(remaining==0||mc.player==null)return;
        int cx=Gfx.guiW()/2,cy=Gfx.guiH()/2;
        int col= (remaining * 42 << 24) | (Ui.accent1 & 0x00FFFFFF);
        for(int k=3;k<8;k++){
            Ui.rect(g,cx+k,cy+k,1,1,col);Ui.rect(g,cx-k,cy+k,1,1,col);
            Ui.rect(g,cx+k,cy-k,1,1,col);Ui.rect(g,cx-k,cy-k,1,1,col);
        }
    }
}
