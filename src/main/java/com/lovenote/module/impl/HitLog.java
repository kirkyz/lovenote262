package com.lovenote.module.impl;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;

/** Logs outgoing attack attempts; does not claim the server confirmed damage. */
public final class HitLog extends LegacyTextWidget {
    private String lastTarget = "";
    private int attacks;
    private int ticksSince;

    public HitLog() {
        super("HitLog", 0.02, 0.64);
        describe("Recently attacked entity and attack attempts this session.");
    }
    @Override public void onAttack(Entity target) {
        if (target == null) return;
        lastTarget = target.getName().getString();
        attacks++;
        ticksSince = 0;
    }
    @Override public void onTick() {
        if (ticksSince < 20 * 15) ticksSince++;
    }
    @Override public void onHud(GuiGraphicsExtractor g, float delta) {
        if (!lastTarget.isEmpty() && ticksSince < 20 * 15)
            drawText(g, "Attacks " + attacks + "  |  Last: " + lastTarget);
        else if (mc.gui.screen() instanceof meteordevelopment.meteorclient.gui.screens.LovenoteModulesScreen hudScreen && hudScreen.isDrainerHudEditor()) drawText(g, "Hit log  |  No recent attacks");
    }
}
