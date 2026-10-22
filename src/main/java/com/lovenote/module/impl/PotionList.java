package com.lovenote.module.impl;

import com.lovenote.setting.BoolSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.effect.MobEffectInstance;

/** Active-effect list with durations, adapted from the old PotionList HUD. */
public final class PotionList extends LegacyTextWidget {
    private final BoolSetting level = add(new BoolSetting("Show level", true));

    public PotionList() {
        super("PotionList", 0.72, 0.36);
        describe("Show active potion effects and remaining durations.");
    }

    @Override
    public void onHud(GuiGraphicsExtractor g, float delta) {
        if(mc.player==null) return;
        if(mc.player.getActiveEffects().isEmpty()) {
            if(mc.gui.screen() instanceof meteordevelopment.meteorclient.gui.screens.LovenoteModulesScreen hudScreen && hudScreen.isDrainerHudEditor()) drawText(g,"Effects  |  None active");
            return;
        }
        StringBuilder line=new StringBuilder("Effects  |  ");
        int count=0;
        for(MobEffectInstance e:mc.player.getActiveEffects()) {
            if(count++>0)line.append("  ·  ");
            String name=e.getEffect().value().getDisplayName().getString();
            if(level.get() && e.getAmplifier()>0)name+=" "+(e.getAmplifier()+1);
            int secs=Math.max(0,e.getDuration()/20);
            line.append(name).append(" ").append(e.isInfiniteDuration()?"∞":String.format(java.util.Locale.ROOT,"%d:%02d",secs/60,secs%60));
            if(count>=3)break;
        }
        drawText(g,line.toString());
    }
}
