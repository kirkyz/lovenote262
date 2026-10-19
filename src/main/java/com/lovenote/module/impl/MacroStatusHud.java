package com.lovenote.module.impl;

import com.lovenote.macro.MacroPlayback;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Gfx;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Small, deliberately inexpensive macro state HUD. */
public final class MacroStatusHud extends Module {
    private final NumberSetting x = add(new NumberSetting("X", 0.015, 0, .95, .005));
    private final NumberSetting y = add(new NumberSetting("Y", 0.16, 0, .95, .005));
    public MacroStatusHud() {
        super("MacroStatusHud", Category.HUD);
        alsoIn.add(Category.HYPIXEL); alsoIn.add(Category.DONUT);
        describe("Compact recording/playback status and progress for native Drainer macros.");
    }
    @Override public void onHud(GuiGraphicsExtractor g, float delta) {
        if (mc.player == null) return;
        String state = MacroPlayback.playing()?"PLAYING":MacroPlayback.recording()?"RECORDING":"IDLE";
        String s = "MACRO  "+state + (MacroPlayback.playing()||MacroPlayback.recording()?"  "+MacroPlayback.label()+"  "+MacroPlayback.progress()+"/"+MacroPlayback.length():"");
        int w = Ui.textW(s)+16;
        int px = Math.max(0, Math.min((int)(x.get()*Gfx.guiW()), Gfx.guiW()-w-2));
        int py = Math.max(0, Math.min((int)(y.get()*Gfx.guiH()), Gfx.guiH()-22));
        Ui.pill(g,px,py,w,20);
        Ui.text(g,s,px+8,py+6,MacroPlayback.playing()?0xFFFFADD9:0xFFE8D7F2);
    }
}
