package com.lovenote.module.impl;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
/** Donut SMP selling command, disabled by default. */
public final class AutoSell extends Module {
    private final ModeSetting command = add(new ModeSetting("Command", "/sell all", "/sell all", "/sell hand", "/sellall"));
    private final NumberSetting seconds = add(new NumberSetting("Delay (s)", 5, 1, 60, 1));
    private int timer;
    public AutoSell() { super("AutoSell", Category.DONUT); describe("Run the selected sell command periodically."); }
    @Override public void onEnable() { timer = 10; }
    @Override public void onTick() {
        if (mc.player == null || mc.player.connection == null) return;
        if (timer-- > 0) return;
        String cmd = command.get();
        mc.player.connection.sendCommand(cmd.startsWith("/") ? cmd.substring(1) : cmd);
        timer = seconds.getI() * 20;
    }
}
