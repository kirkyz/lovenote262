package com.lovenote.module.impl;

import com.lovenote.setting.BoolSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Small cached biome HUD; never scans world blocks. */
public final class BiomeHud extends LegacyTextWidget {
    private final BoolSetting coordinates = add(new BoolSetting("Show coordinates", false));
    private String biome = "";
    private int cooldown;
    public BiomeHud() {
        super("BiomeHud", .02, .58);
        describe("Current biome readout, with optional player coordinates.");
    }
    @Override public void onTick() {
        if(mc.level == null || mc.player == null) { biome = ""; return; }
        if(cooldown-- > 0) return;
        cooldown = 30;
        biome = mc.level.getBiome(mc.player.blockPosition()).unwrapKey()
            .map(key -> key.identifier().getPath().replace('_', ' ')).orElse("Unknown");
    }
    @Override public void onHud(GuiGraphicsExtractor g,float dt) {
        if(mc.player == null || mc.level == null) return;
        String s = "Biome  " + (biome.isEmpty()?"...":biome);
        if(coordinates.get()) s += "  " + mc.player.blockPosition().getX() + ", " + mc.player.blockPosition().getZ();
        drawText(g, s);
    }
}
