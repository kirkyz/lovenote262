package com.lovenote.module.impl;

import com.lovenote.Lovenote;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.module.impl.ported.DrainerServerMacros.MacroRecorder;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;

/** Starter movement templates; user's SkyBlock farm presets still need real recorded routes. */
public final class MacroLibrary extends Module {
    private final ModeSetting template = add(new ModeSetting("Template", "Walk 2s",
            "Walk 2s", "Left and right", "Jump once", "Stand still"));
    private final BoolSetting load = add(new BoolSetting("Load into recorder slot", false));

    public MacroLibrary() {
        super("MacroLibrary", Category.MISC);
        alsoIn.add(Category.HYPIXEL); alsoIn.add(Category.DONUT);
        describe("Loads movement demo templates into the selected MacroRecorder slot. Does not include server-specific farm routes.");
    }
    @Override public void onTick() {
        if (!load.get()) return;
        load.set(false);
        if (Lovenote.modules == null) return;
        MacroRecorder recorder = Lovenote.modules.get(MacroRecorder.class);
        if (recorder == null || recorder.isBusy()) return;
        int[] frames = switch(template.get()) {
            case "Walk 2s" -> fill(40, 1);
            case "Left and right" -> alternating();
            case "Jump once" -> jumping();
            default -> fill(40, 0);
        };
        recorder.loadTemplate(frames);
    }
    private static int[] fill(int ticks, int mask) {
        int[] a = new int[ticks]; java.util.Arrays.fill(a, mask); return a;
    }
    private static int[] alternating() {
        int[] a = new int[80];
        for (int i=0; i<a.length; i++) a[i] = i<40?4:8;
        return a;
    }
    private static int[] jumping() {
        int[] a = new int[45];
        for (int i=0; i<a.length; i++) a[i] = i>=10 && i<16?16:0;
        return a;
    }
}
