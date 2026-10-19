package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Gfx;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Lightweight screen-space visual effects, not a world shader. */
public final class ScreenFX extends Module {
    private final ModeSetting style=add(new ModeSetting("Style", "Cinematic", "Cinematic", "Scanlines", "Border", "Tint"));
    private final ModeSetting colour=add(new ModeSetting("Colour", "Accent", "Accent", "Blue", "Pink", "Black", "White"));
    private final NumberSetting opacity=add(new NumberSetting("Opacity", 25, 0, 110, 5));
    private final NumberSetting strength=add(new NumberSetting("Edge size", 24, 3, 90, 1));
    private final NumberSetting spacing=add(new NumberSetting("Scanline spacing", 6, 2, 16, 1));
    private final BoolSetting animate=add(new BoolSetting("Animate", true));
    public ScreenFX(){ super("ScreenFX",Category.VISUALS);
        describe("Optional cinematic edge shading, scanlines or colour tint; no GPU shader required."); }
    @Override public void onHud(GuiGraphicsExtractor g,float delta){
        if(mc.level==null||mc.player==null||mc.gui.screen()!=null)return;
        int w=Gfx.guiW(),h=Gfx.guiH();
        int color=switch(colour.get()){
            case "Blue" -> 0xFF568FFF;
            case "Pink" -> 0xFFFF74C7;
            case "Black" -> 0xFF000000;
            case "White" -> 0xFFFFFFFF;
            default -> Colors.C3;
        };
        int a=opacity.getI();
        if(style.is("Tint")){Ui.rect(g,0,0,w,h,Colors.alpha(color,a));return;}
        int edge=Math.min(strength.getI(),Math.min(w,h)/3);
        if(style.is("Cinematic")||style.is("Border")){
            for(int i=0;i<edge;i+=2){
                int alpha=(int)(a*(1.0-i/(double)edge));
                int c=Colors.alpha(color,Math.max(0,alpha));
                Ui.rect(g,0,i,w,2,c);Ui.rect(g,0,h-i-2,w,2,c);
                Ui.rect(g,i,0,2,h,c);Ui.rect(g,w-i-2,0,2,h,c);
            }
        }
        if(style.is("Cinematic")||style.is("Scanlines")){
            int offset=animate.get()?((int)(System.currentTimeMillis()/140)%spacing.getI()):0;
            int c=Colors.alpha(color,Math.max(0,a/3));
            for(int y=offset;y<h;y+=spacing.getI())Ui.rect(g,0,y,w,1,c);
        }
    }
}
