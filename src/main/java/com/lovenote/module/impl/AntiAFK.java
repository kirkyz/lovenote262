package com.lovenote.module.impl;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import net.minecraft.world.InteractionHand;
/** Periodic idle action; disabled by default. */
public final class AntiAFK extends Module {
    private final ModeSetting mode = add(new ModeSetting("Mode", "All", "Rotate", "Jump", "Swing", "All"));
    private final NumberSetting interval = add(new NumberSetting("Interval (ticks)", 40, 10, 200, 5));
    private int timer;
    public AntiAFK() { super("AntiAFK", Category.MISC); describe("Perform occasional idle actions."); }
    @Override public void onEnable() { timer = interval.getI(); }
    @Override public void onTick() {
        if (mc.player == null || mc.level == null || mc.gui.screen() != null) return;
        if (timer-- > 0) return;
        timer = interval.getI();
        String selected = mode.get();
        if (selected.equals("Rotate") || selected.equals("All")) mc.player.setYRot(mc.player.getYRot() + 35f);
        if ((selected.equals("Jump") || selected.equals("All")) && mc.player.onGround()) mc.player.jumpFromGround();
        if (selected.equals("Swing") || selected.equals("All")) mc.player.swing(InteractionHand.MAIN_HAND);
    }
}
