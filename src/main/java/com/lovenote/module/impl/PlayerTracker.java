package com.lovenote.module.impl;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Gfx;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
/** Nearby players list with adjustable range and position. */
public final class PlayerTracker extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", 64, 16, 256, 8));
    private final NumberSetting rows = add(new NumberSetting("Max rows", 6, 1, 16, 1));
    private final NumberSetting x = add(new NumberSetting("X", .02, 0, .98, .005));
    private final NumberSetting y = add(new NumberSetting("Y", .40, 0, .98, .005));
    private final BoolSetting showHealth = add(new BoolSetting("Health", true));
    public PlayerTracker() { super("PlayerTracker", Category.MISC); describe("Nearby players with distance and optional health."); }
    @Override public void onHud(GuiGraphicsExtractor g,float delta) {
        if(mc.player==null || mc.level==null) return;
        var others=new ArrayList<Player>();
        double limit=range.get();
        for (Player p:mc.level.players()) if(p!=mc.player && p.distanceToSqr(mc.player)<limit*limit) others.add(p);
        others.sort(Comparator.comparingDouble(p -> p.distanceToSqr(mc.player)));
        int n=Math.min(rows.getI(),others.size()), px=(int)(x.get()*Gfx.guiW()), py=(int)(y.get()*Gfx.guiH());
        String h="Players " + others.size(); Ui.pill(g,px,py,Math.max(100,Ui.textW(h)+12),19); Ui.text(g,h,px+6,py+5,Ui.textCol);
        for(int i=0;i<n;i++) {
            Player p=others.get(i);
            String s=p.getName().getString()+" "+String.format(Locale.ROOT,"%.0fm",Math.sqrt(p.distanceToSqr(mc.player)))
                +(showHealth.get()?String.format(Locale.ROOT,"  %.0f HP",p.getHealth()):"");
            s=s.length()>45?s.substring(0,45):s;
            Ui.pill(g,px,py+(i+1)*20,Ui.textW(s)+12,19); Ui.text(g,s,px+6,py+(i+1)*20+5,Ui.textCol);
        }
    }
}
