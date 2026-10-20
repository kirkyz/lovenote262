package com.lovenote.module.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.lovenote.Lovenote;
import com.lovenote.macro.MacroTape;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.module.impl.ported.DrainerServerMacros.MacroRecorder;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Native SkyBlock-oriented input templates plus durable user recordings; never executes until requested. */
public final class MacroLibrary extends Module {
    public record Preset(String name, String group, String detail, boolean saved) { }
    private record Saved(int[] frames, float[] yaw, float[] pitch, String group) { }
    private record Builtin(String name, String group, String detail, int[] frames) { }
    private static final int LIMIT = 48;
    private static final int F=1,B=2,L=4,R=8,J=16,S=32,A=64,U=128;
    private static final List<Builtin> BUILTINS = createBuiltins();
    private final Map<String, Saved> custom = new LinkedHashMap<>();
    private final ModeSetting template = add(new ModeSetting("Template", "Walk 2s",
        "Walk 2s", "Left and right", "Jump once", "Stand still",
        "Garden row practice", "Mining strafe practice", "Garden forward harvest",
        "Garden left harvest", "Garden right harvest", "Garden forward replant",
        "Garden corner demo", "Mine in place", "Mine tunnel forward",
        "Mine while strafing left", "Mine while strafing right", "Mining retreat",
        "Fishing timed cast reel", "Fishing cast once", "Attack pulse practice",
        "Jump swing practice", "Sneak forward", "Jump-forward practice", "Garden 2-row zigzag", "Garden variable-row zigzag",
        "Sugar cane row strafe", "Nether wart row strafe", "Cocoa manual row",
        "Mushroom garden row", "Wheat row and replant demo", "Garden turn-around practice",
        "Mining long strafe", "Mining short tunnel and retreat"));
    private final BoolSetting load = add(new BoolSetting("Load into recorder slot", false));
    private final NumberSetting rowSeconds = add(new NumberSetting("Garden row seconds", 9, 2, 40, 1));
    private final NumberSetting shiftTicks = add(new NumberSetting("Garden lane shift ticks", 12, 2, 80, 1));
    private final NumberSetting routeRows = add(new NumberSetting("Garden route rows", 4, 2, 8, 1));

    public MacroLibrary() {
        super("MacroLibrary", Category.MISC);
        alsoIn.add(Category.HYPIXEL); alsoIn.add(Category.DONUT);
        describe("SkyBlock-ready input templates and saved route recordings; search, load, play or record from Drainer Macros.");
    }
    @Override public void onTick() {
        if (!load.get()) return;
        load.set(false);
        loadPreset(template.get());
    }
    public synchronized List<Preset> presets() {
        List<Preset> result = new ArrayList<>();
        for(Builtin b:BUILTINS)result.add(new Preset(b.name(),b.group(),b.detail(),false));
        custom.forEach((name,saved)->result.add(new Preset(name,saved.group(),
            "Saved recording: "+saved.frames().length+" ticks ("+
            String.format(java.util.Locale.ROOT,"%.1f",saved.frames().length/20f)+
            "s). Replays recorded inputs and any captured view angles.",true)));
        return List.copyOf(result);
    }
    public synchronized boolean saveSelected(String name) {
        if(name==null)return false;
        String cleaned=name.trim().replaceAll("[\\p{Cntrl}]","");
        if(cleaned.isBlank()||cleaned.length()>36||isBuiltin(cleaned))return false;
        if(custom.size()>=LIMIT&&!custom.containsKey(cleaned))return false;
        MacroRecorder r=recorder();
        if(r==null||r.isBusy())return false;
        int[] frames=r.selectedFrames();
        if(frames.length==0)return false;
        custom.put(cleaned,new Saved(frames.clone(),r.selectedYaw(),r.selectedPitch(),"My recordings"));
        return true;
    }
    public synchronized boolean deleteSaved(String name){return custom.remove(name)!=null;}
    public synchronized boolean loadPreset(String name){
        MacroRecorder r=recorder();
        if(r==null||r.isBusy())return false;
        Saved saved=custom.get(name);
        if(saved!=null){r.loadTemplate(saved.frames().clone(),saved.yaw().clone(),saved.pitch().clone());return true;}
        for(Builtin b:BUILTINS)if(b.name().equals(name)){
            r.loadTemplate(effectiveFrames(b).clone());return true;
        }
        return false;
    }
    /** Load and arm replay; caller must dismiss the GUI before the next tick. */
    public synchronized boolean playPreset(String name){
        MacroRecorder r=recorder();
        return r!=null && loadPreset(name) && r.queuePlayback();
    }
    public boolean startRecording(){MacroRecorder r=recorder();return r!=null&&r.queueRecording();}
    public String selectedSlot(){MacroRecorder r=recorder();return r==null?"No recorder":r.selectedSlot();}
    public synchronized int durationTicks(String name){
        Saved s=custom.get(name);
        if(s!=null)return s.frames().length;
        for(Builtin b:BUILTINS)if(b.name().equals(name))return effectiveFrames(b).length;
        return 0;
    }
    @Override public synchronized void saveExtra(JsonObject o){
        JsonArray presets=new JsonArray();
        JsonObject look=new JsonObject();
        custom.forEach((name,saved)->{
            JsonObject item=new JsonObject();
            item.addProperty("name",name);item.addProperty("group",saved.group());
            JsonArray frames=new JsonArray();
            for(int v:saved.frames())frames.add(v);
            item.add("frames",frames);presets.add(item);
            JsonArray angles=new JsonArray();
            for(int i=0;i<saved.frames().length;i++){
                if(i<saved.yaw().length&&i<saved.pitch().length&&
                    Float.isFinite(saved.yaw()[i])&&Float.isFinite(saved.pitch()[i])){
                    JsonArray pair=new JsonArray();pair.add(saved.yaw()[i]);pair.add(saved.pitch()[i]);angles.add(pair);
                }else angles.add(JsonNull.INSTANCE);
            }
            look.add(name,angles);
        });
        // Old key deliberately preserved for importing presets after an installer rollback.
        o.add("namedMacrosV13",presets);
        o.add("namedMacroLookV15",look);
    }
    @Override public synchronized void loadExtra(JsonObject o){
        custom.clear();
        if(!o.has("namedMacrosV13")||!o.get("namedMacrosV13").isJsonArray())return;
        for(var item:o.getAsJsonArray("namedMacrosV13")){
            try{
                if(custom.size()>=LIMIT||!item.isJsonObject())break;
                JsonObject entry=item.getAsJsonObject();
                String name=entry.get("name").getAsString().trim();
                if(name.isBlank()||name.length()>36||isBuiltin(name))continue;
                JsonArray samples=entry.getAsJsonArray("frames");
                int[] frames=new int[Math.min(MacroTape.MAX_TICKS,samples.size())];
                for(int i=0;i<frames.length;i++)frames[i]=samples.get(i).getAsInt()&255;
                if(frames.length==0)continue;
                String group=entry.has("group")?entry.get("group").getAsString():"My recordings";
                float[] yaw=missing(frames.length),pitch=missing(frames.length);
                if(o.has("namedMacroLookV15")&&o.get("namedMacroLookV15").isJsonObject()){
                    JsonObject all=o.getAsJsonObject("namedMacroLookV15");
                    if(all.has(name)&&all.get(name).isJsonArray()){
                        JsonArray angles=all.getAsJsonArray(name);
                        for(int i=0;i<Math.min(frames.length,angles.size());i++){
                            if(!angles.get(i).isJsonArray())continue;
                            JsonArray pair=angles.get(i).getAsJsonArray();
                            if(pair.size()<2)continue;
                            float y=pair.get(0).getAsFloat(),p=pair.get(1).getAsFloat();
                            if(Float.isFinite(y)&&Float.isFinite(p)){
                                yaw[i]=y;pitch[i]=Math.max(-90f,Math.min(90f,p));
                            }
                        }
                    }
                }
                custom.put(name,new Saved(frames,yaw,pitch,group));
            }catch(RuntimeException ignored){}
        }
    }
    private static boolean isBuiltin(String name){
        for(Builtin b:BUILTINS)if(b.name().equalsIgnoreCase(name))return true;
        return false;
    }
    private static MacroRecorder recorder(){
        return Lovenote.modules==null?null:Lovenote.modules.get(MacroRecorder.class);
    }
    private static float[] missing(int n){float[] values=new float[n];Arrays.fill(values,Float.NaN);return values;}
    private static int[] repeat(int ticks,int mask){int[] values=new int[ticks];Arrays.fill(values,mask);return values;}
    private static int[] combine(int[]...parts){
        int n=0;for(int[] p:parts)n+=p.length;
        int[] all=new int[n];int x=0;
        for(int[] p:parts){System.arraycopy(p,0,all,x,p.length);x+=p.length;}
        return all;
    }
    private static int[] pulse(int length,int wait,int mask){
        int[] out=new int[length];
        for(int i=0;i<length;i+=Math.max(1,wait))out[i]=mask;
        return out;
    }
    private int[] effectiveFrames(Builtin b) {
        int ticks = rowSeconds.getI() * 20;
        int lane = shiftTicks.getI();
        return switch(b.name()) {
            case "Garden 2-row zigzag" -> combine(repeat(ticks,L|A),repeat(lane,F),repeat(ticks,R|A));
            case "Garden variable-row zigzag" -> {
                int rows=routeRows.getI();int[][] parts=new int[rows*2-1][];
                int boundedTicks=Math.max(1,Math.min(ticks,(MacroTape.MAX_TICKS-(rows-1)*lane)/rows));
                for(int k=0;k<rows;k++){parts[2*k]=repeat(boundedTicks,(k%2==0?L:R)|A);if(k<rows-1)parts[2*k+1]=repeat(lane,F);}
                yield combine(parts);
            }
            case "Sugar cane row strafe","Nether wart row strafe","Cocoa manual row","Mushroom garden row" -> repeat(ticks,L|A);
            case "Wheat row and replant demo" -> combine(repeat(ticks,F|A),repeat(10,0),repeat(ticks,B|U));
            case "Garden turn-around practice" -> combine(repeat(ticks,L),repeat(lane,F),repeat(ticks,R));
            case "Mining long strafe" -> repeat(ticks,L|A);
            case "Mining short tunnel and retreat" -> combine(repeat(ticks/2,F|A),repeat(ticks/2,B));
            default -> b.frames();
        };
    }
    private static List<Builtin> createBuiltins(){
        List<Builtin> a=new ArrayList<>();
        a.add(new Builtin("Walk 2s","Movement","Forward input for 2 seconds. Use open, safe ground.",repeat(40,F)));
        a.add(new Builtin("Left and right","Movement","Strafes left for 2s and right for 2s.",combine(repeat(40,L),repeat(40,R))));
        a.add(new Builtin("Jump once","Movement","Waits 10 ticks, jumps for 6, then releases.",combine(repeat(10,0),repeat(6,J),repeat(29,0))));
        a.add(new Builtin("Stand still","Movement","Records 2 seconds of no input.",repeat(40,0)));
        a.add(new Builtin("Garden row practice","SkyBlock / practice","Sample 5s forward only. Not a complete Garden route.",repeat(100,F)));
        a.add(new Builtin("Mining strafe practice","SkyBlock / practice","Sample left-right strafe. Does not mine or turn.",combine(repeat(30,L),repeat(30,R),repeat(30,L),repeat(30,R))));
        a.add(new Builtin("Garden forward harvest","SkyBlock / Garden","8s forward + hold attack. Aim at crops first; no automatic row detection.",repeat(160,F|A)));
        a.add(new Builtin("Garden left harvest","SkyBlock / Garden","8s strafe left + hold attack. Align your camera and row.",repeat(160,L|A)));
        a.add(new Builtin("Garden right harvest","SkyBlock / Garden","8s strafe right + hold attack. Align your camera and row.",repeat(160,R|A)));
        a.add(new Builtin("Garden forward replant","SkyBlock / Garden","8s forward + hold use. Hold seeds, align row and test first.",repeat(160,F|U)));
        a.add(new Builtin("Garden corner demo","SkyBlock / Garden","Forward harvesting 4s, left strafe 0.5s, harvest 4s. Layout-specific demo; no automatic turning.",combine(repeat(80,F|A),repeat(10,L),repeat(80,F|A))));
        a.add(new Builtin("Mine in place","SkyBlock / Mining","Hold attack for 10s at current crosshair target. No ore targeting.",repeat(200,A)));
        a.add(new Builtin("Mine tunnel forward","SkyBlock / Mining","6s forward + hold attack. Clear the path first; does not detect hazards.",repeat(120,F|A)));
        a.add(new Builtin("Mine while strafing left","SkyBlock / Mining","8s left strafe + hold attack. Fixed camera direction.",repeat(160,L|A)));
        a.add(new Builtin("Mine while strafing right","SkyBlock / Mining","8s right strafe + hold attack. Fixed camera direction.",repeat(160,R|A)));
        a.add(new Builtin("Mining retreat","SkyBlock / Mining","Step backward for 5s. No terrain awareness.",repeat(100,B)));
        a.add(new Builtin("Fishing timed cast reel","SkyBlock / Fishing","Use once, wait 10s, use once. Timer only: no bite/bobber detection.",combine(repeat(1,U),repeat(200,0),repeat(1,U),repeat(4,0))));
        a.add(new Builtin("Fishing cast once","SkyBlock / Fishing","One right-click/use pulse then idle. Rod and water must be ready.",combine(repeat(1,U),repeat(19,0))));
        a.add(new Builtin("Attack pulse practice","SkyBlock / Combat","Five seconds of an attack tap every 8 ticks. Aim manually.",pulse(100,8,A)));
        a.add(new Builtin("Jump swing practice","SkyBlock / Combat","5s jump + attack taps; timing demo only.",combine(repeat(5,0),pulse(95,12,J|A))));
        a.add(new Builtin("Sneak forward","SkyBlock / Utility","Sneak and move forward for 6s. No ledge or collision detection.",repeat(120,F|S)));
        a.add(new Builtin("Jump-forward practice","SkyBlock / Utility","Move forward and hop every 1s for 6s. No path detection.",combine(repeat(20,F|J),repeat(20,F),repeat(20,F|J),repeat(20,F),repeat(20,F|J),repeat(20,F))));
        a.add(new Builtin("Garden 2-row zigzag","SkyBlock / Garden",
            "Two active harvest rows with a forward lane shift. Configure Garden row seconds and lane shift ticks; keep the camera aimed at crops.",repeat(1,0)));
        a.add(new Builtin("Garden variable-row zigzag","SkyBlock / Garden",
            "Multiple alternating strafe rows with forward shifts. Configure row seconds, lane shift and route rows. No crop detection or automatic turning.",repeat(1,0)));
        a.add(new Builtin("Sugar cane row strafe","SkyBlock / Garden",
            "Hold left + harvest through a sugar-cane row. Stand at the correct height and alignment. Timings are adjustable.",repeat(1,0)));
        a.add(new Builtin("Nether wart row strafe","SkyBlock / Garden",
            "Hold left + harvest through a wart row. Fit the length to your actual farm; replanting isn't automatic.",repeat(1,0)));
        a.add(new Builtin("Cocoa manual row","SkyBlock / Garden",
            "Hold left + attack against a cocoa wall. Aim manually and confirm growth first.",repeat(1,0)));
        a.add(new Builtin("Mushroom garden row","SkyBlock / Garden",
            "Hold left + attack along a mushroom row. Crop spacing must match your layout.",repeat(1,0)));
        a.add(new Builtin("Wheat row and replant demo","SkyBlock / Garden",
            "Forward harvest then reverse using an item. Test only with a suitable row and seeds; not crop-smart.",repeat(1,0)));
        a.add(new Builtin("Garden turn-around practice","SkyBlock / Garden",
            "Dry-run strafes and a lane shift without attacking. Configure settings before loading.",repeat(1,0)));
        a.add(new Builtin("Mining long strafe","SkyBlock / Mining",
            "Adjustable side movement while mining. Does not target ore, commissions or hazards.",repeat(1,0)));
        a.add(new Builtin("Mining short tunnel and retreat","SkyBlock / Mining",
            "Mine forward and return along the same line; clear hazards before running.",repeat(1,0)));
        return List.copyOf(a);
    }
}
