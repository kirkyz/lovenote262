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

/** Lightweight screen-space world colour wash (not a shader or biome replacement). */
public final class WorldTint extends Module {
    private final ModeSetting tone = add(new ModeSetting("Colour", "Purple", "Purple", "Cyan", "Pink", "Amber", "Blue", "Dark", "White", "Rainbow"));
    private final NumberSetting opacity = add(new NumberSetting("Opacity", 20, 0, 110, 1));
    private final BoolSetting pulse = add(new BoolSetting("Slow breathing", false));
    private final NumberSetting pulseSpeed = add(new NumberSetting("Breathing speed", 0.6, 0.1, 3.0, 0.1));
    private final BoolSetting hideGui = add(new BoolSetting("Hide when menu open", true));

    public WorldTint() {
        super("WorldTint", Category.VISUALS);
        describe("Tint the in-game view using a subtle transparent colour wash; does not modify blocks.");
    }
    @Override public void onHud(GuiGraphicsExtractor g, float delta) {
        if(mc.level == null || mc.player == null || (hideGui.get() && mc.gui.screen() != null)) return;
        int c = switch (tone.get()) {
            case "Cyan" -> 0xFF61DEED;
            case "Pink" -> 0xFFFF7AC2;
            case "Amber" -> 0xFFFFCB71;
            case "Blue" -> 0xFF5C83F3;
            case "Dark" -> 0xFF060612;
            case "White" -> 0xFFFFFFFF;
            case "Rainbow" -> Colors.rainbow(0);
            default -> 0xFFAA83FF;
        };
        int alpha = opacity.getI();
        if(pulse.get()) {
            double phase = System.nanoTime() * 1.0e-9 * pulseSpeed.get();
            alpha = (int) Math.round(alpha * (0.78 + 0.22 * Math.sin(phase)));
        }
        if(alpha > 0) Ui.rect(g, 0, 0, Gfx.guiW(), Gfx.guiH(), Colors.alpha(c, Math.max(0, Math.min(255, alpha))));
    }
}
