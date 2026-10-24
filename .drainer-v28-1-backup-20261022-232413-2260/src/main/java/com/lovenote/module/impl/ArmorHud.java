package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.util.Gfx;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** 26.2 armour durability HUD. Text readout; legacy item icons are not ported yet. */
public final class ArmorHud extends Module {
    private final BoolSetting offhand = add(new BoolSetting("Off hand", true));
    private final EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST,
            EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public ArmorHud() {
        super("ArmorHud", Category.HUD);
        describe("Armour and offhand durability (compact text version).");
    }

    @Override
    public void onHud(GuiGraphicsExtractor g, float delta) {
        if (mc.player == null) return;
        StringBuilder sb = new StringBuilder("Armor");
        for (EquipmentSlot slot : slots) {
            ItemStack item = mc.player.getItemBySlot(slot);
            sb.append("  ").append(shortName(slot));
            if (item.isEmpty()) sb.append(":--");
            else if (item.isDamageableItem() && item.getMaxDamage() > 0) {
                int pct = Math.max(0, Math.round(100f * (item.getMaxDamage() - item.getDamageValue()) / item.getMaxDamage()));
                sb.append(':').append(pct).append('%');
            } else sb.append(":OK");
        }
        if (offhand.get()) sb.append("  Off:").append(mc.player.getOffhandItem().isEmpty() ? "--" : "OK");
        String display = sb.toString();
        int w = Ui.textW(display) + 14, x = Math.max(2, (Gfx.guiW() - w) / 2);
        int y = Math.max(2, Gfx.guiH() - 64);
        Ui.pill(g, x, y, w, 20);
        Ui.text(g, display, x + 7, y + 6, Ui.textCol);
    }

    private static String shortName(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> "H";
            case CHEST -> "C";
            case LEGS -> "L";
            case FEET -> "B";
            default -> "?";
        };
    }
}
