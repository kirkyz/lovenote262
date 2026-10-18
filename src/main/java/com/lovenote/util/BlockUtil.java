package com.lovenote.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class BlockUtil {
        private BlockUtil() {}

        public static int findHotbar(Item item) {
                Minecraft mc = Minecraft.getInstance();
                for (int i = 0; i < 9; i++) if (mc.player.getInventory().getItem(i).is(item)) return i;
                return -1;
        }

        public static int findAnyBlock() {
                Minecraft mc = Minecraft.getInstance();
                for (int i = 0; i < 9; i++) {
                        ItemStack s = mc.player.getInventory().getItem(i);
                        if (!s.isEmpty() && s.getItem() instanceof BlockItem) return i;
                }
                return -1;
        }

        public static boolean placeItem(BlockPos pos, Item item) {
                return placeSlot(pos, findHotbar(item));
        }

        public static boolean placeAnyBlock(BlockPos pos) {
                return placeSlot(pos, findAnyBlock());
        }

        private static boolean placeSlot(BlockPos pos, int slot) {
                Minecraft mc = Minecraft.getInstance();
                if (slot < 0 || mc.level == null || mc.player == null || mc.gameMode == null) return false;
                if (!mc.level.getBlockState(pos).isAir()) return false;

                for (Direction d : Direction.values()) {
                        BlockPos n = pos.relative(d);
                        if (mc.level.getBlockState(n).isAir()) continue;
                        Direction face = d.getOpposite();
                        Vec3 hit = Vec3.atCenterOf(n).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
                        Inventory inv = mc.player.getInventory();
                        int prev = inv.getSelectedSlot();
                        inv.setSelectedSlot(slot);
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, n, false));
                        mc.player.swing(InteractionHand.MAIN_HAND);
                        inv.setSelectedSlot(prev);
                        return true;
                }
                return false;
        }
}
