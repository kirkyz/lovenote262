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
        if(!load.get())return;
        load.set(false);
        loadPreset(template.get());
    }
    /** Returns false while the recorder is busy or unavailable. Does not start playback. */
    public boolean loadPreset(String name) {
        if (Lovenote.modules == null) return false;
        MacroRecorder recorder=Lovenote.modules.get(MacroRecorder.class);
        if(recorder==null||recorder.isBusy())return false;
        int[] frames=switch(name){
            case "Walk 2s" -> fill(40,1);
            case "Left and right" -> alternating();
            case "Jump once" -> jumping();
            case "Stand still" -> fill(40,0);
            default -> null;
        };
        if(frames==null)return false;
        recorder.loadTemplate(frames);
        return true;
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
