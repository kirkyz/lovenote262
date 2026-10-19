package com.lovenote.module.impl;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.NumberSetting;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
/** Only reconnects after a disconnect screen; does not reconnect after a manual quit. */
public final class AutoReconnect extends Module {
    private final NumberSetting seconds=add(new NumberSetting("Reconnect delay (seconds)",15,5,120,1));
    private ServerData last;
    private int countdown;
    private boolean wasDisconnected;
    public AutoReconnect(){super("AutoReconnect",Category.DONUT);alsoIn.add(Category.HYPIXEL);describe("Reconnects to the last multiplayer server after a disconnect screen, not a manual exit.");}
    @Override public void onTick(){
        if(mc.player!=null && mc.getCurrentServer()!=null){
            last=mc.getCurrentServer();countdown=0;wasDisconnected=false;return;
        }
        if(!(mc.gui.screen() instanceof DisconnectedScreen)||last==null){
            countdown=0;wasDisconnected=false;return;
        }
        if(!wasDisconnected){wasDisconnected=true;countdown=seconds.getI()*20;}
        if(--countdown<=0){
            ServerData server=last;last=null;wasDisconnected=false;
            ConnectScreen.startConnecting(new TitleScreen(),mc,ServerAddress.parseString(server.ip),server,false,null);
        }
    }
    @Override public void onDisable(){last=null;countdown=0;wasDisconnected=false;}
}
