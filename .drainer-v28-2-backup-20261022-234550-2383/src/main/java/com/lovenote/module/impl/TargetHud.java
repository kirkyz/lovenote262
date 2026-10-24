package com.lovenote.module.impl;

import com.lovenote.util.HudGlass;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import java.util.Locale;

/** Health card uses the actual entity under the crosshair, not a mock target. */
public final class TargetHud extends LegacyTextWidget {
    public TargetHud() {
        super("TargetHud", 0.42, 0.69);
        describe("Live target name and health in a slim Drainer glass card.");
    }

    @Override public void onHud(GuiGraphicsExtractor g, float delta) {
        if (mc.player == null) return;
        Entity target = mc.hitResult instanceof EntityHitResult hit ? hit.getEntity() : null;
        boolean editing = mc.gui.screen() instanceof
                meteordevelopment.meteorclient.gui.screens.LovenoteModulesScreen screen
                && screen.isDrainerHudEditor();
        if (target == null && !editing) return;
        final int w = compact.get() ? 198 : 240, h = compact.get() ? 61 : 72;
        drawCustomCard(g, w, h);
        int x = editorX(), y = editorY();
        String name = target != null ? target.getName().getString() : "No target";
        int avX = x + 12, avY = y + 13;
        HudGlass.pill(g, avX, avY, 33, 33, 0x9B303A60);
        String letter = name.isBlank() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT);
        Ui.text(g, letter, avX + (33 - Ui.textW(letter)) / 2, avY + 12, 0xFFEDE9FA);
        Ui.text(g, HudGlass.fit(name, w - 67), x + 54, y + 14, 0xFFF5F2FF);
        if (target instanceof LivingEntity living) {
            double max = Math.max(1, living.getMaxHealth());
            double hp = Math.max(0, living.getHealth());
            String label = String.format(Locale.ROOT, "%.1f / %.1f HP", hp, max);
            Ui.text(g, label, x + 54, y + 31, HudGlass.muted());
            HudGlass.progress(g, x + 13, y + h - 12, w - 26, 5,
                    Math.min(1, hp / max), hp/max < .3 ? 0xFFF17879 : 0xFF65D8A6);
        } else {
            Ui.text(g, target == null ? "Aim at a living entity" : "Entity", x + 54, y + 31, HudGlass.muted());
            HudGlass.progress(g, x + 13, y + h - 12, w - 26, 5, 0, HudGlass.accent());
        }
    }
}
