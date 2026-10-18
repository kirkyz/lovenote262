package com.lovenote.module.impl;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.util.Gfx;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
/** Best-effort local death counter for attacked entities. Not server-confirmed kills. */
public final class KillCounter extends Module {
    private final BoolSetting show=add(new BoolSetting("Show HUD",true));
    private final Map<UUID,Entry> targets=new HashMap<>();
    private int kills;
    private record Entry(LivingEntity entity,int tick){}
    private int tick;
    public KillCounter(){super("KillCounter",Category.MISC);describe("Count likely kills of recently attacked entities this session.");}
    @Override public void onAttack(Entity target){if(target instanceof LivingEntity living)targets.put(living.getUUID(),new Entry(living,tick));}
    @Override public void onTick(){
        tick++;
        if(mc.level==null){targets.clear();return;}
        Iterator<Entry> it=targets.values().iterator();
        while(it.hasNext()){
            Entry e=it.next();
            if(!e.entity.isAlive() && tick-e.tick<100) {kills++;it.remove();}
            else if(tick-e.tick>=100)it.remove();
        }
    }
    @Override public void onHud(GuiGraphicsExtractor g,float delta){
        if(!show.get()||mc.player==null)return;
        String s="Likely kills: "+kills;int x=Math.max(0,Gfx.guiW()-Ui.textW(s)-22),y=(int)(Gfx.guiH()*.48);
        Ui.pill(g,x,y,Ui.textW(s)+12,19);Ui.text(g,s,x+6,y+5,Ui.textCol);
    }
    @Override public void onDisable(){targets.clear();}
}
