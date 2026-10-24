package com.lovenote.util;

import com.google.gson.JsonObject;
import com.lovenote.module.Module;
import com.lovenote.setting.NumberSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** V12 per-widget zoom, independent of a module's internal scale settings. */
public final class HudScale {
    private static final Map<String,Double> VALUES=new ConcurrentHashMap<>();
    private HudScale(){}
    public static double get(Module m){return VALUES.getOrDefault(m.name,1.0);}
    public static void set(Module m,double value){VALUES.put(m.name,Math.max(.20,Math.min(5.00,value)));}
    public static void load(JsonObject o){
        VALUES.clear();if(o==null)return;
        for(var e:o.entrySet()) {
            try{VALUES.put(e.getKey(),Math.max(.20,Math.min(5.00,e.getValue().getAsDouble())));}
            catch(Exception ignored){}
        }
    }
    public static JsonObject save(){
        JsonObject o=new JsonObject();VALUES.forEach((name,value)->o.addProperty(name,value));return o;
    }
    public static void draw(Module m,GuiGraphicsExtractor g,float tickDelta){
        double scale=get(m);
        if(Math.abs(scale-1)<.001){m.onHud(g,tickDelta);return;}
        double ax=0,ay=0;
        for(var setting:m.settings)if(setting instanceof NumberSetting n){
            if(n.name.equalsIgnoreCase("X")) ax=n.max<=1.5?n.get()*Gfx.guiW():n.get();
            if(n.name.equalsIgnoreCase("Y")) ay=n.max<=1.5?n.get()*Gfx.guiH():n.get();
        }
        var pose=g.pose();pose.pushMatrix();
        pose.translate((float)ax,(float)ay);
        pose.scale((float)scale,(float)scale);
        pose.translate((float)-ax,(float)-ay);
        try{m.onHud(g,tickDelta);}finally{pose.popMatrix();}
    }
}
