package com.lovenote.module.impl;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
/** Hypixel tipping, disabled by default. */
public final class AutoTip extends Module {
    private final ModeSetting command = add(new ModeSetting("Command", "/tip all", "/tip all", "/tip"));
    private final NumberSetting minutes = add(new NumberSetting("Every (min)", 10, 1, 60, 1));
    private int timer;
    public AutoTip() { super("AutoTip", Category.HYPIXEL); describe("Send a tip command periodically."); }
    @Override public void onEnable() { timer = 20; }
    @Override public void onTick() {
        if (mc.player == null || mc.player.connection == null) return;
        if (timer-- > 0) return;
        String cmd = command.get();
        mc.player.connection.sendCommand(cmd.startsWith("/") ? cmd.substring(1) : cmd);
        timer = minutes.getI() * 1200;
    }
}
