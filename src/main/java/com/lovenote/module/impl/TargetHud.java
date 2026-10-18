package com.lovenote.module.impl;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import java.util.Locale;

/** Read-only target-under-crosshair HUD. */
public final class TargetHud extends LegacyTextWidget {
    public TargetHud() {
        super("TargetHud", 0.42, 0.69);
        describe("Show the entity under your crosshair with health when available.");
    }
    @Override public void onHud(GuiGraphicsExtractor g, float delta) {
        if (mc.player == null || !(mc.hitResult instanceof EntityHitResult hit)) return;
        Entity entity = hit.getEntity();
        if (entity == null) return;
        String text = entity.getName().getString();
        if (entity instanceof LivingEntity living)
            text += String.format(Locale.ROOT, "  %.1f HP", living.getHealth());
        drawText(g, text);
    }
}
