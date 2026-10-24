package com.lovenote.macro;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.util.Mth;
import java.util.List;

/** Shared movement-key owner. Never allows two Drainer movement replays simultaneously. */
public final class MacroPlayback {
    private MacroPlayback() { }
    private static Object owner;
    private static int applied;
    private static boolean playing;
    private static boolean recording;
    private static String slot = "";
    private static int current, total;

    public static boolean tryClaim(Object who) {
        if (owner != null && owner != who) return false;
        owner = who;
        return true;
    }
    public static boolean ownedBy(Object who) { return owner == who; }
    public static void resetEdges() { applied=0; }

    public static void recordStatus(String s, int count) {
        if (playing) return;
        recording = true; slot = s; current = count; total = count;
    }
    public static void stopRecording() { recording = false; }

    /** Returns false on an unsafe/incompatible container rather than misclicking inventory. */
    public static boolean apply(Minecraft mc, Object who, String s, int mask, int index, int count,
                             float yaw, float pitch, int taps, int screenMode,
                             List<MacroTape.ContainerClick> actions) {
        if (owner != who || mc.options == null || mc.player == null || mc.gameMode == null) return false;
        playing = true; recording = false;
        slot = s; current = index; total = count;
        // Restore the selected item before use/attack, even for user-saved named macros.
        if ((mask & 4096) != 0) {
            int hotbar = (mask >>> 8) & 15;
            if (hotbar < 9 && mc.player.getInventory().getSelectedSlot() != hotbar)
                mc.player.getInventory().setSelectedSlot(hotbar);
        }
        // The original V31 replay rotated AFTER sending click actions. On a farm,
        // a click could target the previous crop while the selected slot changed.
        // Keep the shortest equivalent yaw to avoid a visible 360-degree spin.
        if (screenMode == 0 && Float.isFinite(yaw) && Float.isFinite(pitch)) {
            float previousYaw = mc.player.getYRot();
            mc.player.setYRot(previousYaw + Mth.wrapDegrees(yaw - previousYaw));
            mc.player.setXRot(Math.max(-90f, Math.min(90f, pitch)));
        }
        var opt = mc.options;
        // Player inventory can safely be opened locally; external chests require the
        // normal world interaction and a matching container to have opened already.
        if (screenMode == 1 && !(mc.gui.screen() instanceof InventoryScreen)) {
            if (mc.gui.screen() != null) return false;
            mc.gui.setScreen(new InventoryScreen(mc.player));
        } else if (screenMode == 2 && !(mc.gui.screen() instanceof AbstractContainerScreen<?>)) {
            return false;
        } else if (screenMode == 0 && mc.gui.screen() instanceof AbstractContainerScreen<?>) {
            // Close recorded container screens at the same point in the route.
            mc.gui.setScreen(null);
        }
        boolean inContainer=screenMode!=0;
        opt.keyUp.setDown(!inContainer && (mask & 1) != 0);
        opt.keyDown.setDown(!inContainer && (mask & 2) != 0);
        opt.keyLeft.setDown(!inContainer && (mask & 4) != 0);
        opt.keyRight.setDown(!inContainer && (mask & 8) != 0);
        opt.keyJump.setDown(!inContainer && (mask & 16) != 0);
        opt.keyShift.setDown(!inContainer && (mask & 32) != 0);
        boolean attackHeld = !inContainer && (mask & 64) != 0;
        boolean useHeld = !inContainer && (mask & 128) != 0;
        boolean attackRising = attackHeld && (applied & 64) == 0;
        boolean useRising = useHeld && (applied & 128) == 0;
        opt.keyAttack.setDown(attackHeld);
        int attacks = taps & 15;
        int uses = (taps >>> 4) & 15;
        // An immediate recorded click already performs this frame's use. Keep
        // the key released for this frame to avoid another use on the next tick.
        // A sustained held-use recording can continue normally afterwards.
        opt.keyUse.setDown(useHeld && uses == 0 && !useRising);
        // Old V11 files lack tap metadata: retain legacy rising-edge behaviour.
        if (!inContainer) {
            for (int i = 0; i < Math.min(8, Math.max(attacks, attackRising ? 1 : 0)); i++)
                meteordevelopment.meteorclient.utils.Utils.leftClick();
            for (int i = 0; i < Math.min(8, Math.max(uses, useRising ? 1 : 0)); i++)
                replayUseAtCurrentLook(mc);
        }
        // The GUI consumes player input itself. Do not mutate containers unless
        // expected menu dimensions and slot references all match the recording.
        if (!actions.isEmpty()) {
            if (screenMode == 0 || !(mc.gui.screen() instanceof AbstractContainerScreen<?>)) return false;
            var menu = mc.player.containerMenu;
            if (screenMode == 1 && menu != mc.player.inventoryMenu) return false;
            if (screenMode == 2 && menu == mc.player.inventoryMenu) return false;
            for (MacroTape.ContainerClick a : actions) {
                if (a.slotCount() != menu.slots.size() || a.slot() < -999
                    || a.slot() >= menu.slots.size() || a.button() < 0 || a.button() > 8) return false;
                try { ContainerInput.valueOf(a.type()); }
                catch (IllegalArgumentException ex) { return false; }
            }
            for (MacroTape.ContainerClick a : actions)
                mc.gameMode.handleContainerInput(menu.containerId, a.slot(), a.button(),
                    ContainerInput.valueOf(a.type()), mc.player);
        }
        applied = inContainer ? 0 : mask;
        return true;
    }

    /**
     * Meteor Utils.rightClick() only queues a boolean for the NEXT Minecraft tick.
     * By then the camera may have moved to the next recorded frame, which causes
     * right-clicks to miss farmland. Use the normal game-mode interaction on the
     * block we can currently see, with the just-restored hotbar slot and rotation.
     * Air/item uses still use the original helper to preserve non-block behaviour.
     */
    private static void replayUseAtCurrentLook(Minecraft mc) {
        var hit = mc.player.pick(mc.player.blockInteractionRange(), 1.0F, false);
        if (hit instanceof BlockHitResult blockHit) {
            var result = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, blockHit);
            if (result.consumesAction()) {
                mc.player.swing(InteractionHand.MAIN_HAND);
                return;
            }
        }
        meteordevelopment.meteorclient.utils.Utils.rightClick();
    }

    public static void release(Minecraft mc, Object who) {
        if (owner != who) return;
        if (mc.options != null) {
            // These are simulated states controlled by Drainer; release every key it owns.
            mc.options.keyUp.setDown(false);
            mc.options.keyDown.setDown(false);
            mc.options.keyLeft.setDown(false);
            mc.options.keyRight.setDown(false);
            mc.options.keyJump.setDown(false);
            mc.options.keyShift.setDown(false);
            mc.options.keyAttack.setDown(false);
            mc.options.keyUse.setDown(false);
        }
        owner = null; applied = 0; playing = false; current = 0; total = 0;
    }
    public static void idle() { if (owner == null) { playing = false; recording = false; } }
    public static boolean playing() { return playing; }
    public static boolean recording() { return recording; }
    public static String label() { return slot; }
    public static int progress() { return current; }
    public static int length() { return total; }
}
