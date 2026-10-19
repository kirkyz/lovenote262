package com.lovenote.module.impl;

import com.lovenote.setting.BoolSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Local in-world session timer; no server queries or remote calls. */
public final class SessionTimer extends LegacyTextWidget {
    private final BoolSetting onlyInWorld = add(new BoolSetting("Hide in menus", false));
    private final BoolSetting hours = add(new BoolSetting("Show hours", true));
    private long activeSeconds;
    private int ticks;
    public SessionTimer() {
        super("SessionTimer", .73, .13);
        describe("Shows elapsed time in the current play session; resets when enabled.");
    }
    @Override public void onEnable() { activeSeconds = 0; ticks = 0; }
    @Override public void onTick() {
        if (mc.level == null || mc.player == null) return;
        if (++ticks >= 20) { ticks = 0; activeSeconds++; }
    }
    @Override public void onHud(GuiGraphicsExtractor g, float dt) {
        if (mc.player == null || (onlyInWorld.get() && mc.gui.screen() != null)) return;
        long h=activeSeconds/3600,m=(activeSeconds/60)%60,s=activeSeconds%60;
        String time = hours.get() ? String.format("%02d:%02d:%02d",h,m,s)
                : String.format("%02d:%02d",activeSeconds/60,s);
        drawText(g,"Session  " + time);
    }
}
