package com.lovenote.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class ToolUtil {
        private ToolUtil() {}

        /** Hotbar slot with the fastest tool for this block, or -1. */
        public static int bestSlot(BlockState state) {
                Minecraft mc = Minecraft.getInstance();
                int best = -1;
                float bestSpeed = 1.0f;
                for (int i = 0; i < 9; i++) {
                        ItemStack st = mc.player.getInventory().getItem(i);
                        float sp = st.getDestroySpeed(state);
                        if (sp > bestSpeed) { bestSpeed = sp; best = i; }
                }
                return best;
        }
}
