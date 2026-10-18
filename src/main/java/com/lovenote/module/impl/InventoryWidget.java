package com.lovenote.module.impl;
import com.lovenote.setting.BoolSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
/** Compact inventory-status widget. Item-grid rendering needs 26.2 item extraction integration. */
public final class InventoryWidget extends LegacyTextWidget {
    private final BoolSetting includeHotbar = add(new BoolSetting("Include hotbar", false));
    public InventoryWidget() { super("InventoryWidget", 0.62, 0.73); describe("Inventory space, occupied slots and current held item."); }
    @Override public void onHud(GuiGraphicsExtractor g, float delta) {
        if (mc.player == null) return;
        var inv = mc.player.getInventory();
        int start = includeHotbar.get() ? 0 : 9, slots = includeHotbar.get() ? 36 : 27, filled = 0;
        for (int i = start; i < start + slots; i++) if (!inv.getItem(i).isEmpty()) filled++;
        String held = mc.player.getMainHandItem().isEmpty() ? "None" : mc.player.getMainHandItem().getHoverName().getString();
        drawText(g, "Inventory " + filled + "/" + slots + " | Free " + (slots - filled) + " | Hand " + held);
    }
}
