package com.lovenote.module.impl;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.NumberSetting;
import net.minecraft.client.gui.screens.DeathScreen;
/** Auto respawn without invoking HUD or world-render methods. */
public final class AutoRespawn extends Module {
    private final NumberSetting delay=add(new NumberSetting("Delay (ticks)",10,1,100,1));
    private int ticks;
    public AutoRespawn(){super("AutoRespawn",Category.HYPIXEL);alsoIn.add(Category.DONUT);describe("Respawns on the death screen after a configurable delay.");}
    @Override public void onDisable(){ticks=0;}
    @Override public void onTick(){
        if(mc.player==null || !(mc.gui.screen() instanceof DeathScreen)){ticks=0;return;}
        if(++ticks>=delay.getI()){ticks=0;mc.player.respawn();}
    }
}
