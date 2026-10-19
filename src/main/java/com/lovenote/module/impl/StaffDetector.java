package com.lovenote.module.impl;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.Locale;
/** Limited tab-list keyword alerts; NOT an actual staff or vanish detector. */
public final class StaffDetector extends Module {
    private final BoolSetting showHud=add(new BoolSetting("Show HUD",true));
    private int ticks;
    private String label="";
    public StaffDetector(){super("StaffDetector",Category.HYPIXEL);describe("Alerts only when public tab-list labels contain staff-like titles; cannot identify hidden staff.");}
    @Override public void onTick(){
        if(mc.getConnection()==null){label="";return;}
        if(--ticks>0)return;ticks=40;
        label="";
        for(var p:mc.getConnection().getOnlinePlayers()){
            var display=p.getTabListDisplayName();
            if(display==null)continue;
            String tag=display.getString().toLowerCase(Locale.ROOT);
            if(tag.contains("[admin]")||tag.contains("[mod]")||tag.contains("[helper]")||tag.contains("[gm]")){
                label=p.getProfile().name();break;
            }
        }
    }
    @Override public void onHud(GuiGraphicsExtractor g,float delta){
        if(!showHud.get()||label.isEmpty()||mc.player==null)return;
        String s="DR  |  Staff-like public tag: "+label;
        Ui.pill(g,8,137,Ui.textW(s)+12,18);Ui.text(g,s,14,142,0xFFFFAABB);
    }
}
