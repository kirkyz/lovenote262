package com.lovenote.module.impl;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Gfx;
import com.lovenote.util.HudWidgetBounds;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
/** HUD readout sourced from the current connection. */
public final class ServerInfo extends Module implements HudWidgetBounds {
    private final BoolSetting showPing=add(new BoolSetting("Ping",true));
    private final BoolSetting showPlayers=add(new BoolSetting("Player count",true));
    private final NumberSetting x=add(new NumberSetting("X",.015,0,.95,.005));
    private final NumberSetting y=add(new NumberSetting("Y",.31,0,.95,.005));
    public ServerInfo(){super("ServerInfo",Category.HYPIXEL);alsoIn.add(Category.HUD);describe("Compact server, ping and player-count HUD. Works on any server.");}
    private int drawnX,drawnY,drawnW=80,drawnH=20;
    @Override public int editorX(){return drawnX;}
    @Override public int editorY(){return drawnY;}
    @Override public int editorWidth(){return drawnW;}
    @Override public int editorHeight(){return drawnH;}
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
        drawnX=px;drawnY=py;drawnW=w;drawnH=18;
        Ui.pill(g,px,py,w,18);Ui.text(g,text,px+6,py+5,Ui.textCol);
    }
}
