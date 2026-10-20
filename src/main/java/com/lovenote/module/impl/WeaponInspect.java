package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Gfx;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

/** Unobtrusive, opt-in held-item information strip. NOT a 3D inspect animation. */
public final class WeaponInspect extends Module {
    private final NumberSetting x = add(new NumberSetting("X", .66, 0, .98, .01));
    private final NumberSetting y = add(new NumberSetting("Y", .89, 0, .98, .01));
    private final BoolSetting showDurability = add(new BoolSetting("Durability bar", true));
    private final BoolSetting compact = add(new BoolSetting("Minimal style", true));
    public WeaponInspect() {
        super("WeaponInspect", Category.HUD);
        describe("Small item-name and condition strip. Full 3D weapon inspection is NOT ported yet.");
    }
    @Override public void onHud(GuiGraphicsExtractor g, float dt) {
        if (mc.player == null || mc.gui.screen() != null) return;
        ItemStack held = mc.player.getMainHandItem();
        if (held.isEmpty()) return;
        String name = held.getHoverName().getString();
        if (name.length() > 20) name = name.substring(0, 19) + "…";
        int w = Math.min(158, Math.max(70, Ui.textW(name) + 26));
        int h = 25;
        int px = Math.max(0, Math.min((int)(x.get() * Gfx.guiW()), Math.max(0, Gfx.guiW() - w)));
        int py = Math.max(0, Math.min((int)(y.get() * Gfx.guiH()), Math.max(0, Gfx.guiH() - h)));
        if (!compact.get()) Ui.widgetCard(g, px, py, w, h, 190);
        else Ui.rect(g, px, py, w, h, 0x9A111420);
        g.item(held, px + 4, py + 3);
        Ui.text(g, name, px + 24, py + 6, Ui.textCol);
        if (showDurability.get() && held.isDamageableItem()) {
            int max = Math.max(1, held.getMaxDamage());
            float remaining = Math.max(0, Math.min(1, (max - held.getDamageValue()) / (float)max));
            Ui.rect(g, px + 24, py + h - 5, w - 28, 2, 0x554E526C);
            Ui.rect(g, px + 24, py + h - 5, Math.round((w - 28) * remaining), 2,
                    remaining < .25f ? 0xFFFF697C : Ui.accent1);
        }
    }
}
