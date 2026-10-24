package com.lovenote.macro;

import net.minecraft.client.Minecraft;

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

    public static void recordStatus(String s, int count) {
        if (playing) return;
        recording = true; slot = s; current = count; total = count;
    }
    public static void stopRecording() { recording = false; }

    public static void apply(Minecraft mc, Object who, String s, int mask, int index, int count,
                             float yaw, float pitch) {
        if (owner != who || mc.options == null) return;
        playing = true; recording = false;
        slot = s; current = index; total = count;
        var opt = mc.options;
        // Restore the selected item before use/attack so recorded farming tool swaps replay.
        // Legacy recordings have no bit 12 and never alter the selected slot.
        if (mc.player != null && (mask & 4096) != 0) {
            int hotbar = (mask >>> 8) & 15;
            if (hotbar < 9 && mc.player.getInventory().getSelectedSlot() != hotbar)
                mc.player.getInventory().setSelectedSlot(hotbar);
        }
        opt.keyUp.setDown((mask & 1) != 0);
        opt.keyDown.setDown((mask & 2) != 0);
        opt.keyLeft.setDown((mask & 4) != 0);
        opt.keyRight.setDown((mask & 8) != 0);
        opt.keyJump.setDown((mask & 16) != 0);
        opt.keyShift.setDown((mask & 32) != 0);
        // Rising edges must trigger an actual click (tap-only fishing/planting presets).
        // The held state then supports sustained block breaking and item use.
        boolean attackRising = (mask & 64) != 0 && (applied & 64) == 0;
        boolean useRising = (mask & 128) != 0 && (applied & 128) == 0;
        opt.keyAttack.setDown((mask & 64) != 0);
        opt.keyUse.setDown((mask & 128) != 0);
        if (attackRising) {
            meteordevelopment.meteorclient.utils.Utils.leftClick();
            opt.keyAttack.setDown(true);
        }
        if (useRising) meteordevelopment.meteorclient.utils.Utils.rightClick();
        if (mc.player != null && Float.isFinite(yaw) && Float.isFinite(pitch)) {
            mc.player.setYRot(yaw);
            mc.player.setXRot(pitch);
        }
        applied = mask;
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
