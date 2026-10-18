package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.util.Gfx;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.effect.MobEffectInstance;

/** Active-effect list with durations, adapted from the old PotionList HUD. */
public final class PotionList extends Module {
    private final BoolSetting level = add(new BoolSetting("Show level", true));

    public PotionList() {
        super("PotionList", Category.HUD);
        describe("Show active potion effects and remaining durations.");
    }

    @Override
    public void onHud(GuiGraphicsExtractor g, float delta) {
        if (mc.player == null || mc.player.getActiveEffects().isEmpty()) return;
        int y = Gfx.guiH() / 2 + 40;
        for (MobEffectInstance e : mc.player.getActiveEffects()) {
            String name = e.getEffect().value().getDisplayName().getString();
            if (level.get() && e.getAmplifier() > 0) name += " " + (e.getAmplifier() + 1);
            int secs = Math.max(0, e.getDuration() / 20);
            String line = name + "  " + (e.isInfiniteDuration() ? "∞" : String.format("%d:%02d", secs / 60, secs % 60));
            int w = Ui.textW(line) + 14, x = Math.max(2, Gfx.guiW() - w - 8);
            Ui.pill(g, x, y, w, 16);
            Ui.text(g, line, x + 7, y + 4, Ui.textCol);
            y += 19;
        }
    }
}
