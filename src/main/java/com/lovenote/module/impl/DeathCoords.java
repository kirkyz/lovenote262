package com.lovenote.module.impl;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;

/** 26.2 port of the legacy death-position HUD with saved coordinates. */
public final class DeathCoords extends LegacyTextWidget {
    private BlockPos lastDeath;
    private boolean wasAlive = true;

    public DeathCoords() {
        super("DeathCoords", 0.02, 0.70);
        describe("Remember and display your last death coordinates.");
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) {
            wasAlive = true;
            return;
        }
        boolean alive = mc.player.getHealth() > 0f;
        if (wasAlive && !alive) lastDeath = mc.player.blockPosition();
        wasAlive = alive;
    }

    @Override
    public void onHud(GuiGraphicsExtractor g, float delta) {
        if (mc.player == null) return;
        drawText(g, lastDeath == null ? "Death: none yet" :
                "Death: " + lastDeath.getX() + " " + lastDeath.getY() + " " + lastDeath.getZ());
    }

    @Override
    public void saveExtra(JsonObject o) {
        if (lastDeath == null) return;
        o.addProperty("dx", lastDeath.getX());
        o.addProperty("dy", lastDeath.getY());
        o.addProperty("dz", lastDeath.getZ());
    }

    @Override
    public void loadExtra(JsonObject o) {
        if (o.has("dx") && o.has("dy") && o.has("dz"))
            lastDeath = new BlockPos(o.get("dx").getAsInt(), o.get("dy").getAsInt(), o.get("dz").getAsInt());
    }
}
