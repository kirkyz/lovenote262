package com.lovenote.module.impl;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Counts loaded entities periodically rather than scanning each rendered frame. */
public final class EntityCounter extends LegacyTextWidget {
    private int ticks, lastCount;
    public EntityCounter() {
        super("EntityCounter", .73, .18);
        describe("Loaded entity count for diagnosing low FPS; sampled every two seconds.");
    }
    @Override public void onTick() {
        if (mc.level == null) { lastCount = 0; return; }
        if (++ticks < 40) return;
        ticks = 0;
        int count = 0;
        for (var ignored : mc.level.entitiesForRendering()) count++;
        lastCount = count;
    }
    @Override public void onHud(GuiGraphicsExtractor g, float dt) {
        if (mc.level != null && mc.player != null) drawText(g,"Entities  " + lastCount + "  |  " + mc.getFps() + " fps");
    }
}
