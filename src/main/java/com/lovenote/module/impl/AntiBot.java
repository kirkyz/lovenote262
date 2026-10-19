package com.lovenote.module.impl;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
/** Advisory only: not connected to attack filters. */
public final class AntiBot extends Module {
    private final BoolSetting showHud=add(new BoolSetting("Show warnings",true));
    private int ticks,suspect;
    public AntiBot(){super("AntiBot",Category.HYPIXEL);describe("Advisory tab-list check for player entities. False positives possible; no target filtering.");}
    @Override public void onTick(){
        if(mc.player==null||mc.level==null||mc.getConnection()==null){suspect=0;return;}
        if(--ticks>0)return;ticks=40;
        Set<UUID> roster=new HashSet<>();
        for(var e:mc.getConnection().getOnlinePlayers())roster.add(e.getProfile().id());
        int n=0;
        for(var p:mc.level.players())if(p!=mc.player && !roster.contains(p.getUUID()))n++;
        suspect=n;
    }
    @Override public void onHud(GuiGraphicsExtractor g,float delta){
        if(mc.player==null||!showHud.get()||suspect<=0)return;
        String s="DR  |  Unlisted player entities: "+suspect;
        Ui.pill(g,8,160,Ui.textW(s)+12,18);Ui.text(g,s,14,165,0xFFFFC4CD);
    }
}
