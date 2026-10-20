package com.lovenote.module.impl;

import com.google.gson.JsonArray;
import com.lovenote.skyblock.SkyBlockIntel;
import com.google.gson.JsonObject;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.util.Gfx;
import com.lovenote.util.HudWidgetBounds;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.BitSet;
import java.util.List;
import java.util.Locale;

/** Offline/manual SkyBlock progression coach. No API calls, gameplay automation or price claims. */
public final class SkyBlockCoach extends Module implements HudWidgetBounds {
    public record Step(String track, String title, String action, String reason, String milestone) { }
    private static Step step(String track,String title,String action,String reason,String milestone) {
        return new Step(track,title,action,reason,milestone);
    }
    public static final List<Step> STEPS = List.of(
        step("START", "Unlock your first skills", "Visit the Hub, obtain beginner tools and explore the skill menus.", "Skill progression unlocks more useful money methods.", "Skills screen unlocked"),
        step("START", "Open the SkyBlock Level menu", "Review the SkyBlock Guide and collect any easy XP rewards.", "Early account XP is often cheapest from first-time unlocks.", "Claim initial SkyBlock XP"),
        step("START", "Get your Bazaar access", "Raise your SkyBlock level to the current Bazaar requirement and visit the Bazaar.", "Reliable trading and sell-price comparisons improve decisions.", "Bazaar unlocked"),
        step("START", "Unlock collections", "Mine, farm, forage and fight to earn starter collection milestones.", "First milestones give quick account XP and recipes.", "Several basic collections unlocked"),
        step("START", "Unlock the Garden", "Follow the Garden access quest and talk to the Garden NPCs.", "Garden offers repeatable farming progression.", "Garden access unlocked"),
        step("START", "Unlock minion slots", "Craft unique minions and upgrade cheap tiers without buying overpriced materials.", "Passive resources and unique crafts make efficient progression.", "Increase minion slots"),
        step("LEVELS", "First-time easy XP", "Open the SkyBlock Guide and complete simple undiscovered tasks first.", "One-time XP sources can be faster than grinding a single skill.", "Collect easy XP rewards"),
        step("LEVELS", "Skill-level targets", "Choose your lowest affordable skill and set a short milestone.", "Balanced skills avoid expensive late-tier grinds.", "Earn next skill level"),
        step("LEVELS", "Bestiary entries", "Visit new islands and defeat safe varieties of mobs.", "Early Bestiary milestones can grant account XP.", "Claim a Bestiary milestone"),
        step("LEVELS", "Fairy souls and exploration", "Explore regions using an up-to-date route for your game version.", "Exploration unlocks areas and permanent progression.", "Complete one exploration milestone"),
        step("LEVELS", "Museum and collections", "Compare low-cost Museum/collection XP sources before committing coins.", "Account XP efficiency varies by current collection progress.", "Claim a new source of SkyBlock XP"),
        step("LEVELS", "Community centre & events", "Check the calendar, Mayor perks and temporary event objectives.", "Events can reduce the time or cost of XP tasks.", "Finish a relevant event objective"),
        step("GARDEN", "Garden visitors", "Complete affordable Garden visitor requests; skip requests with poor value.", "Visitor rewards can boost farming progress.", "Complete an affordable visitor"),
        step("GARDEN", "Crop milestones", "Harvest several unlocked crops to collect low-tier milestone XP.", "Diversification can beat maxing one high crop tier early.", "Earn a new crop milestone"),
        step("GARDEN", "Farming tool basics", "Pick the right harvesting tool for your main crop and its enchantments.", "Appropriate tools improve crops per minute.", "Equip correct crop tool"),
        step("GARDEN", "Farming Fortune upgrades", "Compare affordable armour, tool, pet and Garden bonuses.", "Higher Farming Fortune increases drops, not base XP per crop broken.", "Improve Farming Fortune"),
        step("GARDEN", "Efficient manual rows", "Design a safe row, select correct yaw/pitch and measure crops per minute manually.", "Layout matters more than repeating a generic 8-second macro.", "Complete a smooth row route"),
        step("GARDEN", "Garden contests", "Join Jacobs contests when crop + gear match a realistic medal threshold.", "Contest rewards unlock progression and upgrades.", "Complete a contest"),
        step("GARDEN", "Farming income benchmark", "Count sellable crops over 5 minutes; compare Bazaar versus NPC net proceeds.", "Per-hour measurements outperform outdated profit rankings.", "Record a credible coins/hour estimate"),
        step("MINING", "Mining collections", "Unlock basic ores and early useful recipes in the Mines.", "Cheap collection XP creates early progression.", "Complete initial mining collections"),
        step("MINING", "Dwarven access", "Meet the current entry requirements and unlock Dwarven Mines.", "Unlocks Mining-related progression systems.", "Reach Dwarven Mines"),
        step("MINING", "Commissions", "Complete easy commissions and claim the related rewards.", "Commissions are a predictable mining progression route.", "Claim several commissions"),
        step("MINING", "Heart of the Mountain", "Unlock appropriate HotM upgrades and spend powder based on your activity.", "Progression upgrades matter before expensive setups.", "Reach your next HotM tier"),
        step("MINING", "Powder + equipment", "Upgrade mining speed, fortune, armour and drill/pickaxe responsibly.", "Improve actual yield before chasing advanced routes.", "Benchmark blocks per minute"),
        step("MINING", "Mining profits", "Record a 5-minute manual route and calculate net proceeds after fuel/fees.", "Profit depends heavily on setup and market prices.", "Log a mining coins/hour benchmark"),
        step("COMBAT", "Combat progression", "Improve your weapon, armour and accessory power to survive tougher areas.", "Reliable kills and lower downtime improve experience.", "Survive next combat area"),
        step("COMBAT", "Accessory bag", "Unlock accessory slots and choose a power suitable for your build.", "Affordable accessories can offer cost-effective damage boosts.", "Improve magical power"),
        step("COMBAT", "Slayer basics", "Pick a slayer tier you can consistently finish for your current gear.", "Consistency beats failed higher-tier attempts.", "Claim next Slayer reward"),
        step("COMBAT", "Dungeon preparation", "Choose suitable gear/class and learn rooms before higher floors.", "Repeatable completions help skill and dungeon progression.", "Complete a suitable dungeon"),
        step("COMBAT", "Bestiary and collections", "Target missing early mob entries and collections in safe areas.", "Many small unlocks produce account XP.", "Collect next combat-related XP milestone"),
        step("COINS", "Compare price sources", "Check Bazaar buy/sell spread, NPC value and Auction House fees manually.", "Gross price is not net profit.", "Record real sale proceeds"),
        step("COINS", "Protect starting capital", "Keep a reserve before expensive tool, pet or armour upgrades.", "Liquidity helps exploit good legitimate opportunities.", "Set a spending reserve"),
        step("COINS", "Time a profit sample", "Measure 5 minutes of active farming/mining/fishing and net coins earned.", "Benchmark earnings with your exact gear, not advertised rates.", "Record a tested coins/hour figure"),
        step("COINS", "Choose your best route", "Prioritise the highest repeatable net coins/hour you enjoy and can sustain.", "Less downtime often means more actual earnings.", "Commit to one tested route"),
        step("COINS", "Reinvest intelligently", "Estimate how long a new upgrade will take to pay back its cost.", "Avoid upgrades with years of playtime to repay.", "Calculate payback hours"),
        step("COINS", "Use minions efficiently", "Compare output, fuel and collection benefit against alternatives.", "Passive income helps, but real rates vary by upgrades and prices.", "Review minion setup"));

    private final NumberSetting x = add(new NumberSetting("HUD X", .015, 0, .90, .005));
    private final NumberSetting y = add(new NumberSetting("HUD Y", .27, 0, .90, .005));
    private final BoolSetting hud = add(new BoolSetting("Show current objective", true));
    private final ModeSetting layout = add(new ModeSetting("HUD Layout", "Compact", "Mini", "Compact", "Detailed"));
    private final NumberSetting hudWidth = add(new NumberSetting("HUD Width", 170, 110, 245, 5));
    // Separate from Minecraft GUI scale: shrinking width alone didn't shrink the large font.
    // Newly introduced setting defaults to 45% even for existing V17 configurations.
    private final NumberSetting hudScale = add(new NumberSetting("HUD Scale %", 45, 25, 100, 5));
    private final BoolSetting showOutsideSkyBlock = add(new BoolSetting("Preview outside SkyBlock", true));
    private final NumberSetting gardenCoins = add(new NumberSetting("Garden net coins in 5m", 0, 0, 1_000_000_000, 100));
    private final NumberSetting miningCoins = add(new NumberSetting("Mining net coins in 5m", 0, 0, 1_000_000_000, 100));
    private final NumberSetting fishingCoins = add(new NumberSetting("Fishing net coins in 5m", 0, 0, 1_000_000_000, 100));
    private final NumberSetting otherCoins = add(new NumberSetting("Other net coins in 5m", 0, 0, 1_000_000_000, 100));
    private final NumberSetting upgradeCost = add(new NumberSetting("Upgrade cost coins", 0, 0, 1_000_000_000, 1000));
    private final BoolSetting waypointHud = add(new BoolSetting("Waypoint arrow", true));
    private final ModeSetting crop = add(new ModeSetting("Garden crop", "Wheat", "Wheat", "Carrots", "Potatoes", "Nether Wart", "Sugar Cane", "Melon", "Pumpkin"));
    // A monotonic practice timer only: actual coins earned are entered manually in module settings.
    private long benchmarkDeadlineNanos;
    private String benchmarkMethod = "Garden";
    private final BitSet done = new BitSet();
    private boolean customPin;
    private double pinX,pinY,pinZ;
    private String pinArea="", pinName="";
    private String advice = "Start with cheap first-time unlocks, then compare your measured net coins/hour.";
    private String track="START";
    private int cursor;
    private int dx,dy,dw=170,dh=72;
    public SkyBlockCoach(){super("SkyBlockCoach",Category.HYPIXEL);alsoIn.add(Category.HUD);alsoIn.add(Category.MISC);
        describe("Interactive SkyBlock progression route with saved goals and an honest manual profit/payback estimator. No server automation.");}
    /** On-server progress evidence is displayed, NOT used to falsely auto-complete quests. */
    @Override public synchronized void onTick() {
        SkyBlockIntel.tick(mc);
        // Being physically in the Garden proves access, unlike a level estimate.
        var live=SkyBlockIntel.get();
        if(live.hypixel() && (live.area().equalsIgnoreCase("The Garden")||live.area().equalsIgnoreCase("Garden"))) {
            if(!done.get(4)) {
                done.set(4);
                if(track.equals("START") && cursor==4)cursor=firstIncomplete("START");
            }
        }
    }
    public SkyBlockIntel.Snapshot live(){return SkyBlockIntel.get();}
    public synchronized String recommendation(){return advice;}
    public synchronized String gardenCrop(){return crop.get();}
    public synchronized String gardenLayout(){
        return switch(crop.get()){
            case "Nether Wart" -> "Soul sand rows. Harvest by hand, then replant; no irrigation needed.";
            case "Sugar Cane" -> "Water beside cane base. Break grown upper cane; preserve bottom stalk.";
            case "Melon" -> "Allow room for fruit next to mature stems; use appropriate axe.";
            case "Pumpkin" -> "Keep stem lanes planted with adjacent fruit space; use appropriate axe.";
            default -> "Hydrated farmland and unobstructed walking lanes. Replant where necessary.";
        };
    }
    public synchronized String gardenUpgrade(){
        return "Check tool -> farming fortune -> affordable armour/pet -> crop upgrades -> pests.";
    }
    public synchronized String gardenSale(){
        return "Sell a 5-minute sample; compare real Bazaar proceeds after fees to NPC total.";
    }
    public synchronized void nextGardenCrop(){
        String[] values={"Wheat","Carrots","Potatoes","Nether Wart","Sugar Cane","Melon","Pumpkin"};
        int i=0;for(int j=0;j<values.length;j++)if(values[j].equals(crop.get())){i=j;break;}
        crop.set(values[(i+1)%values.length]);
    }
    public synchronized String benchmarkMethod(){return benchmarkMethod;}
    public synchronized boolean benchmarkRunning(){return benchmarkDeadlineNanos!=0 && System.nanoTime()<benchmarkDeadlineNanos;}
    public synchronized int benchmarkRemaining(){
        if(benchmarkDeadlineNanos==0)return -1;
        long nanos=benchmarkDeadlineNanos-System.nanoTime();
        return (int)Math.max(0,(nanos+999_999_999L)/1_000_000_000L);
    }
    public synchronized String benchmarkLabel(){
        int seconds=benchmarkRemaining();
        if(seconds<0)return "No test active";
        if(seconds==0)return benchmarkMethod+" test finished - enter net coins in settings";
        return String.format(Locale.ROOT,"%s manual test %d:%02d left",benchmarkMethod,seconds/60,seconds%60);
    }
    public synchronized void toggleBenchmark(){
        if(benchmarkDeadlineNanos!=0){benchmarkDeadlineNanos=0;return;}
        benchmarkMethod=track.equals("MINING")?"Mining":track.equals("COINS")?"Other":track.equals("COMBAT")?"Other":"Garden";
        benchmarkDeadlineNanos=System.nanoTime()+300_000_000_000L;
    }
    /** A priority suggestion, not proof of quest completion or optimal coin rates. */
    public synchronized String suggestNext(){
        String previous=track;
        if(!finished("START"))setTrack("START");
        else if(!finished(track))cursor=firstIncomplete(track);
        else {
            String area=live().area().toUpperCase(Locale.ROOT);
            String preferred=area.contains("GARDEN")?"GARDEN":area.contains("DWARVEN")||area.contains("CRYSTAL")?"MINING":null;
            if(preferred!=null&&!finished(preferred))setTrack(preferred);
            else if(coinsPerHour()<=0&&!finished("COINS"))setTrack("COINS");
            else for(String t:List.of("LEVELS","GARDEN","MINING","COMBAT","COINS")){
                if(!finished(t)){setTrack(t);break;}
            }
        }
        Step goal=current();
        String context=finished("START")?"Compare cost and time for your next upgrade.":"Finish the starter progression before expensive upgrades.";
        advice="NEXT: "+goal.title()+" — "+context
            +(coinsPerHour()>0?" Best *measured* method: "+bestMethod()+"; "+String.format(Locale.ROOT,"%,.0f",coinsPerHour())+" net coins/h.":" Use your own 5-minute profit tests; prices and boosts vary.");
        return (previous.equals(track)?"Stay on ":"Switch to ")+track+": "+advice;
    }
    private boolean finished(String group) {
        for(int i=0;i<STEPS.size();i++)if(STEPS.get(i).track().equals(group)&&!done.get(i))return false;
        return true;
    }
    public synchronized String track(){return track;}
    public record GuidePoint(String area,String name,double x,double y,double z) { }
    /** Hand-verified single-island destinations; not a cross-island route or pathfinder. */
    private static GuidePoint known(Step step) {
        return switch(step.title()) {
            case "Unlock your first skills" -> new GuidePoint("HUB","Hub Village (Jack)",0,70,-54);
            case "Farming tool basics" -> new GuidePoint("HUB","Farm Merchant",63,72,-114);
            case "Dungeon preparation" -> new GuidePoint("DUNGEON HUB","Dungeon Hub (Mort)",-62,123,0);
            default -> null;
        };
    }
    public synchronized GuidePoint destinationPoint(){
        if(customPin)return new GuidePoint(pinArea,pinName,pinX,pinY,pinZ);
        return known(current());
    }
    public synchronized boolean pinSuggested(){
        GuidePoint point=known(current());if(point==null)return false;
        customPin=false;return true;
    }
    public synchronized boolean pinHere(){
        if(mc.player==null)return false;
        pinX=mc.player.getX();pinY=mc.player.getY();pinZ=mc.player.getZ();
        pinArea=live().area();pinName="Custom pin for " + current().title();
        customPin=true;return true;
    }
    public synchronized void clearPin(){customPin=false;pinArea="";pinName="";}
    public synchronized String waypointText(){
        GuidePoint p=destinationPoint();
        if(p==null)return "No verified pin for this objective; use PIN HERE for your own marker.";
        if(mc.player==null)return "Join a world to navigate";
        String area=live().area().toUpperCase(Locale.ROOT);
        boolean correct=p.area().equalsIgnoreCase(area) ||
            (p.area().equals("HUB") && (area.equals("VILLAGE") || area.equals("FARM") || area.equals("HUB") || area.equals("BANK")));
        if(!correct)return "Go to " + p.area() + " first — don't follow cross-island coordinates";
        double dx=p.x()-mc.player.getX(), dz=p.z()-mc.player.getZ();
        double distance=Math.hypot(dx,dz);
        // MC yaw: 0=south (+Z), 90=west (-X), 180=north (-Z).
        double bearing=Math.toDegrees(Math.atan2(-dx,dz));
        double offset=(bearing-mc.player.getYRot()+540)%360-180;
        String direction=distance<4?"ARRIVED":Math.abs(offset)<20?"↑":offset>0?"→":"←";
        return String.format(Locale.ROOT,"%s  %s   %.0fm  (%.0f, %.0f, %.0f)",direction,p.name(),distance,p.x(),p.y(),p.z());
    }
    public synchronized void setTrack(String value){
        if(!List.of("START","LEVELS","GARDEN","MINING","COMBAT","COINS").contains(value))return;
        track=value;cursor=firstIncomplete(value);
    }
    public synchronized int completed(){return done.cardinality();}
    public synchronized int total(){return STEPS.size();}
    public synchronized int completedTrack(){int result=0;for(int i=0;i<STEPS.size();i++)if(STEPS.get(i).track.equals(track)&&done.get(i))result++;return result;}
    public synchronized int trackCount(){int n=0;for(Step s:STEPS)if(s.track.equals(track))n++;return n;}
    public synchronized Step current(){return STEPS.get(Math.max(0,Math.min(STEPS.size()-1,cursor)));}
    public synchronized boolean currentDone(){return done.get(cursor);}
    /** Player-confirmed shortcut for an already-established SkyBlock account. */
    public synchronized void confirmSkillsUnlocked(){
        done.set(0);
        if(track.equals("START") && cursor==0)cursor=firstIncomplete("START");
    }
    public synchronized void mark(boolean value){done.set(cursor,value);if(value)cursor=firstIncomplete(track);}
    private int firstIncomplete(String group){int fallback=0;for(int i=0;i<STEPS.size();i++)if(STEPS.get(i).track.equals(group)){fallback=i;if(!done.get(i))return i;}return fallback;}
    public synchronized void advance(int by){for(int i=cursor+by;i>=0&&i<STEPS.size();i+=by)if(STEPS.get(i).track.equals(track)){cursor=i;return;}}
    public double coinsPerHour(){return Math.max(Math.max(gardenCoins.get(),miningCoins.get()),Math.max(fishingCoins.get(),otherCoins.get()))*12.0;}
    public String bestMethod(){
        double best=coinsPerHour();if(best==0)return "No samples yet";
        if(best==gardenCoins.get()*12.0)return "Garden";
        if(best==miningCoins.get()*12.0)return "Mining";
        if(best==fishingCoins.get()*12.0)return "Fishing";
        return "Other";
    }
    public String payback(){double rate=coinsPerHour();return rate<=0?"Enter a 5m sample":String.format(Locale.ROOT,"%.1f hours",upgradeCost.get()/rate);}
    @Override public synchronized void saveExtra(JsonObject o){
        o.addProperty("customPinV16",customPin);o.addProperty("pinXV16",pinX);o.addProperty("pinYV16",pinY);
        o.addProperty("pinZV16",pinZ);o.addProperty("pinAreaV16",pinArea);o.addProperty("pinNameV16",pinName);
        o.addProperty("trackV16",track);o.addProperty("cursorV16",cursor);o.addProperty("benchMethodV17",benchmarkMethod);JsonArray a=new JsonArray();for(int i=done.nextSetBit(0);i>=0;i=done.nextSetBit(i+1))a.add(i);o.add("doneV16",a);}
    @Override public synchronized void loadExtra(JsonObject o){
        try {
            customPin=o.has("customPinV16")&&o.get("customPinV16").getAsBoolean();
            pinX=o.has("pinXV16")?o.get("pinXV16").getAsDouble():0;
            pinY=o.has("pinYV16")?o.get("pinYV16").getAsDouble():0;
            pinZ=o.has("pinZV16")?o.get("pinZV16").getAsDouble():0;
            pinArea=o.has("pinAreaV16")?o.get("pinAreaV16").getAsString():"";
            pinName=o.has("pinNameV16")?o.get("pinNameV16").getAsString():"";
            if(!Double.isFinite(pinX)||!Double.isFinite(pinY)||!Double.isFinite(pinZ))customPin=false;
        } catch(RuntimeException ignored){customPin=false;}
        done.clear();if(o.has("doneV16")&&o.get("doneV16").isJsonArray())for(var i:o.getAsJsonArray("doneV16"))try{int n=i.getAsInt();if(n>=0&&n<STEPS.size())done.set(n);}catch(RuntimeException ignored){}
        if(o.has("benchMethodV17")){
            String method=o.get("benchMethodV17").getAsString();
            if(List.of("Garden","Mining","Other").contains(method))benchmarkMethod=method;
        }
        benchmarkDeadlineNanos=0; // A session is not resumed after a game restart.
        if(o.has("trackV16"))setTrack(o.get("trackV16").getAsString());
        if(o.has("cursorV16")){int n=o.get("cursorV16").getAsInt();if(n>=0&&n<STEPS.size()&&STEPS.get(n).track.equals(track))cursor=n;}
    }
    /** Offline travel cues. They are directions, NOT live waypoints or verified NPC positions. */
    public static String destination(Step s) {
        return switch(s.title()) {
            case "Unlock your first skills" -> "Private Island -> Hub Village";
            case "Open the SkyBlock Level menu", "First-time easy XP", "Museum and collections" -> "SkyBlock Menu -> SkyBlock Guide";
            case "Get your Bazaar access", "Compare price sources" -> "Hub Village -> Bazaar (after unlock)";
            case "Unlock collections" -> "Hub farm, Mines, Forest or Combat zones";
            case "Unlock the Garden" -> "SkyBlock Menu -> Garden access quest";
            case "Unlock minion slots", "Use minions efficiently" -> "Your Private Island -> Minions";
            case "Skill-level targets" -> "SkyBlock Menu -> Skills";
            case "Bestiary entries", "Bestiary and collections" -> "SkyBlock Menu -> Bestiary / Combat areas";
            case "Fairy souls and exploration" -> "Hub -> unlocked islands -> Fairy Souls";
            case "Community centre & events" -> "Hub -> Community Center / Calendar";
            case "Garden visitors", "Crop milestones", "Farming tool basics", "Farming Fortune upgrades", "Efficient manual rows", "Garden contests", "Farming income benchmark" -> "Garden island -> your crop plots";
            case "Mining collections" -> "Hub -> Coal Mine / Gold Mine / Deep Caverns";
            case "Dwarven access" -> "Deep Caverns -> Dwarven Mines entrance";
            case "Commissions", "Heart of the Mountain", "Powder + equipment", "Mining profits" -> "Dwarven Mines (or unlocked mining area)";
            case "Combat progression", "Accessory bag", "Slayer basics", "Dungeon preparation" -> "Hub -> Combat zones / Dungeon Hub";
            case "Protect starting capital", "Time a profit sample", "Choose your best route", "Reinvest intelligently" -> "Hub -> Bazaar / Your chosen activity";
            default -> "SkyBlock Menu -> current quest / guide";
        };
    }
    public static String nextAction(Step s) { return s.action(); }
    /** This coach does not claim to detect server quest completion. */
    public static String progressNote() { return "Mark complete after verifying your in-game Quest Log or objective."; }
    public synchronized boolean hudVisible(){return hud.get();}
    public synchronized void toggleHud(){hud.set(!hud.get());}
    public synchronized String hudLayout(){return layout.get();}
    public synchronized void cycleHudLayout(){
        layout.set(layout.is("Mini")?"Compact":layout.is("Compact")?"Detailed":"Mini");
    }
    private static String shortLine(String value,int width){
        if(value==null)return "";
        String t=value;
        if(Ui.textW(t)<=width)return t;
        while(t.length()>2&&Ui.textW(t+"...")>width)t=t.substring(0,t.length()-1);
        return t+"...";
    }
    @Override public int editorX(){return dx;}
    @Override public int editorY(){return dy;}
    @Override public int editorWidth(){return dw;}
    @Override public int editorHeight(){return dh;}
    /** Scale the complete quest tracker (font, padding and backdrop), not merely its width. */
    @Override public synchronized void onHud(GuiGraphicsExtractor g,float delta){
        if(mc.player==null||!hud.get())return;
        var state=live();
        if(!showOutsideSkyBlock.get()&&!state.hypixel())return;
        Step goal=current();
        int screenW=Gfx.guiW(), screenH=Gfx.guiH();
        if(screenW<80||screenH<40)return;
        boolean mini=layout.is("Mini"), detailed=layout.is("Detailed");
        int w=Math.min(hudWidth.getI(),Math.max(80,screenW-8));
        int h=mini?42:detailed?113:71;
        float scale=hudScale.getF()/100f;
        int actualW=Math.max(1,Math.round(w*scale)),actualH=Math.max(1,Math.round(h*scale));
        dx=Math.max(0,Math.min((int)(x.get()*screenW),Math.max(0,screenW-actualW-3)));
        dy=Math.max(0,Math.min((int)(y.get()*screenH),Math.max(0,screenH-actualH-3)));
        dw=actualW;dh=actualH;
        var pose=g.pose();
        pose.pushMatrix();
        try {
            pose.translate((float)dx,(float)dy);
            pose.scale(scale,scale);
            Ui.pill(g,0,0,w,h);
            int max=w-12;
            Ui.text(g,shortLine("COACH / "+track+" "+completedTrack()+"/"+trackCount(),max),6,5,0xFFCE9DFF);
            Ui.text(g,shortLine(goal.title(),max),6,17,0xFFF4EDFC);
            String where=destination(goal);
            if(mini){
                String hint=waypointHud.get() && destinationPoint()!=null ? waypointText() : "GO: "+where;
                Ui.text(g,shortLine(hint,max),6,29,0xFFD8BCEC);
                return;
            }
            Ui.text(g,shortLine("GO: "+where,max),6,30,0xFFE0C7F2);
            Ui.text(g,shortLine("DO: "+goal.action(),max),6,43,0xFFDDD5E6);
            String loc=state.hypixel() ? (state.area().equalsIgnoreCase("Unknown")
                ? "LIVE: Hypixel / area unavailable"
                : "LIVE: "+state.area()+(state.level()>=0?" / LV "+state.level():""))
                : "PREVIEW / NOT ON HYPIXEL";
            String guidance=waypointHud.get() && destinationPoint()!=null ? waypointText() : loc;
            Ui.text(g,shortLine(guidance,max),6,56,0xFFABAECE);
            if(!detailed)return;
            if(waypointHud.get())Ui.text(g,shortLine(waypointText(),max),6,69,0xFFCCABED);
            Ui.text(g,shortLine(state.hypixel()&&!state.quest().equals("Not detected")
                ? "QUEST: "+state.quest() : "VERIFY: "+goal.milestone(),max),6,82,0xFFD8C8E7);
            if(benchmarkDeadlineNanos!=0)Ui.text(g,shortLine(benchmarkLabel(),max),6,96,0xFFF5C884);
        } finally {
            pose.popMatrix();
        }
    }

}
