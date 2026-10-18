package com.lovenote.module.impl;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Gfx;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
/** Top-down rotating 2D player radar with Drainer theme accent. */
public final class Radar extends Module {
    private final NumberSetting range=add(new NumberSetting("Range",32,8,96,4));
    private final NumberSetting x=add(new NumberSetting("X", .02,0,.90,.005));
    private final NumberSetting y=add(new NumberSetting("Y", .30,0,.85,.005));
    public Radar(){super("Radar",Category.HUD);describe("Rotating nearby-player minimap.");}
    @Override public void onHud(GuiGraphicsExtractor g,float delta){
        if(mc.player==null||mc.level==null) return;
        int size=84, px=(int)(x.get()*Gfx.guiW()), py=(int)(y.get()*Gfx.guiH());
        px=Math.min(px,Math.max(0,Gfx.guiW()-size));py=Math.min(py,Math.max(0,Gfx.guiH()-size));
        Ui.panel(g,px,py,size,size);
        int cx=px+size/2,cy=py+size/2;
        Ui.rect(g,cx,py+5,1,size-10,0x40FFFFFF);Ui.rect(g,px+5,cy,size-10,1,0x40FFFFFF);
        Ui.rect(g,cx-2,cy-2,5,5,Ui.accent1);
        double angle=Math.toRadians(mc.player.getYRot());double sin=Math.sin(angle),cos=Math.cos(angle);
        double scale=(size/2.0-6)/range.get();
        for(Player other:mc.level.players()){
            if(other==mc.player)continue;
            double dx=other.getX()-mc.player.getX(),dz=other.getZ()-mc.player.getZ();
            double forward=-dx*sin+dz*cos, right=-dx*cos-dz*sin;
            int ox=cx+(int)Math.round(right*scale), oy=cy-(int)Math.round(forward*scale);
            if(ox<px+4||oy<py+4||ox>px+size-5||oy>py+size-5)continue;
            Ui.rect(g,ox-1,oy-1,3,3,Ui.accent2);
        }
    }
}
