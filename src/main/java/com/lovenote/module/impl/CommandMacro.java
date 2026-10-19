package com.lovenote.module.impl;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
/** Restricted preset commands: avoids arbitrary unvalidated command injection. */
public final class CommandMacro extends Module {
    private final ModeSetting preset=add(new ModeSetting("Command","/is","/is","/hub","/spawn","/warp home","/sell all","/sellall"));
    private final BoolSetting repeat=add(new BoolSetting("Repeat",false));
    private final NumberSetting interval=add(new NumberSetting("Interval (seconds)",30,10,600,5));
    private int ticks;
    public CommandMacro(){super("CommandMacro",Category.HYPIXEL);alsoIn.add(Category.DONUT);describe("Runs one chosen server command on enable; optional slow repetition. Disabled by default.");}
    @Override public void onEnable(){ticks=1;}
    @Override public void onTick(){
        if(mc.player==null||mc.player.connection==null||mc.gui.screen()!=null)return;
        if(--ticks>0)return;
        mc.player.connection.sendCommand(preset.get().substring(1));
        if(repeat.get())ticks=interval.getI()*20;
        else setEnabled(false);
    }
}
