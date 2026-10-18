package com.lovenote.module.impl;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Gfx;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import java.util.Locale;
/** Brief HP display of a recently attacked entity; not a confirmed-damage claim. */
public final class DamageIndicator extends Module {
    private final NumberSetting seconds=add(new NumberSetting("Display (s)",4,1,12,1));
    private LivingEntity target; private int ticks;
    public DamageIndicator(){super("DamageIndicator",Category.VISUALS);describe("Show the health of the last living entity you attacked.");}
    @Override public void onAttack(Entity entity){if(entity instanceof LivingEntity living){target=living;ticks=seconds.getI()*20;}}
    @Override public void onTick(){if(ticks>0)ticks--;if(mc.player==null||ticks==0||target==null||!target.isAlive())target=null;}
    @Override public void onHud(GuiGraphicsExtractor g,float delta){
        if(target==null)return;
        String s=String.format(Locale.ROOT,"%s  %.1f HP",target.getName().getString(),target.getHealth());
        int w=Ui.textW(s)+12,px=Math.max(0,(Gfx.guiW()-w)/2),py=(int)(Gfx.guiH()*.60);
        Ui.pill(g,px,py,w,19);Ui.text(g,s,px+6,py+5,Ui.textCol);
    }
    @Override public void onDisable(){target=null;ticks=0;}
}
