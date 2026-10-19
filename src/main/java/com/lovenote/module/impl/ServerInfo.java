package com.lovenote.module.impl;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Gfx;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
/** HUD readout sourced from the current connection. */
public final class ServerInfo extends Module {
    private final BoolSetting showPing=add(new BoolSetting("Ping",true));
    private final BoolSetting showPlayers=add(new BoolSetting("Player count",true));
    private final NumberSetting x=add(new NumberSetting("X",.015,0,.95,.005));
    private final NumberSetting y=add(new NumberSetting("Y",.31,0,.95,.005));
    public ServerInfo(){super("ServerInfo",Category.HYPIXEL);alsoIn.add(Category.HUD);describe("Compact server, ping and player-count HUD. Works on any server.");}
    @Override public void onHud(GuiGraphicsExtractor g,float delta){
        if(mc.player==null||mc.getConnection()==null)return;
        String name=mc.getCurrentServer()==null?"Singleplayer":mc.getCurrentServer().ip;
        if(name.length()>40)name=name.substring(0,37)+"...";
        StringBuilder b=new StringBuilder("DR  |  ").append(name);
        if(showPing.get()){
            var info=mc.getConnection().getPlayerInfo(mc.player.getUUID());
            if(info!=null)b.append("  ·  ").append(info.getLatency()).append("ms");
        }
        if(showPlayers.get())b.append("  ·  ").append(mc.getConnection().getOnlinePlayers().size()).append(" players");
        String text=b.toString();int w=Ui.textW(text)+12;
        int px=Math.max(0,Math.min((int)(x.get()*Gfx.guiW()),Gfx.guiW()-w));
        int py=(int)(y.get()*Gfx.guiH());
        Ui.pill(g,px,py,w,18);Ui.text(g,text,px+6,py+5,Ui.textCol);
    }
}
