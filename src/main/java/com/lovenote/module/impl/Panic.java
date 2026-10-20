package com.lovenote.module.impl;

import com.lovenote.Lovenote;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

/** Emergency one-shot switch: disables Drainer AND optionally Meteor modules. */
public final class Panic extends Module {
    private final BoolSetting disableHud = add(new BoolSetting("Include HUD widgets", true));
    private final BoolSetting disableMeteor = add(new BoolSetting("Include Meteor modules", true));

    public Panic() {
        super("Panic", Category.MISC);
        describe("Emergency one-shot: switch off all Drainer modules and optionally Meteor. Bind a key in the menu.");
    }

    @Override public void onEnable() {
        int drainerCount = 0, meteorCount = 0;
        try {
            if (Lovenote.modules != null) {
                // Snapshot avoids surprises if onDisable() changes module state.
                for (Module module : new ArrayList<>(Lovenote.modules.all())) {
                    if (module == this || !module.enabled) continue;
                    if (!disableHud.get() && module.category == Category.HUD) continue;
                    try { module.setEnabled(false); drainerCount++; }
                    catch (RuntimeException ex) {
                        module.enabled = false;
                        System.err.println("[Drainer Panic] Disable failed for " + module.name + ": " + ex);
                    }
                }
            }
            if (disableMeteor.get()) {
                Modules modules = Modules.get();
                if (modules != null) {
                    for (meteordevelopment.meteorclient.systems.modules.Module module : new ArrayList<>(modules.getActive())) {
                        try { module.disable(); meteorCount++; }
                        catch (RuntimeException ex) {
                            System.err.println("[Drainer Panic] Meteor disable failed for " + module.name + ": " + ex);
                        }
                    }
                }
            }
            if (mc.player != null) {
                mc.player.sendSystemMessage(Component.literal(
                        "[Drainer] Panic: disabled " + drainerCount + " Drainer / " + meteorCount + " Meteor modules"));
            }
        } finally {
            // One-shot action; leave the button/keybind ready for the next press.
            enabled = false;
            keyDown = true;
        }
    }
}
