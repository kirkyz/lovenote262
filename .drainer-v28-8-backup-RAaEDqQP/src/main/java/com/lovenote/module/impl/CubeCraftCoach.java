package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Gfx;
import com.lovenote.util.HudGlass;
import com.lovenote.util.HudWidgetBounds;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.Scoreboard;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Client-observed CubeCraft HUD: never represents guessed quests as confirmed progress. */
public final class CubeCraftCoach extends Module implements HudWidgetBounds {
    private static final Pattern COUNTER = Pattern.compile("(?i).*(?:\\d+\\s*/\\s*\\d+|\\d+%).*");
    private final BoolSetting showHud = add(new BoolSetting("Show guide HUD", true));
    private final ModeSetting guidance = add(new ModeSetting("Guidance", "Guided", "Minimal", "Guided", "Full Walkthrough"));
    private final NumberSetting x = add(new NumberSetting("HUD X", .025, 0, .95, .005));
    private final NumberSetting y = add(new NumberSetting("HUD Y", .35, 0, .90, .005));
    private final BoolSetting outside = add(new BoolSetting("Preview outside CubeCraft", false));
    private final BoolSetting debug = add(new BoolSetting("Show raw server lines", false));
    private final NumberSetting questNumber = add(new NumberSetting("Quest number", 1, 1, 30, 1));
    private int drawX,drawY,drawW=174,drawH=52;
    private List<String> serverLines = List.of();
    private String quest = "";
    private String progress = "";
    private long readAt;
    private String menuQuest = "";
    private String menuObjective = "";
    private String menuPlace = "";
    private int menuDone = -1, menuTotal = -1;
    private long menuSeenAt;
    private record QuestEntry(String title, String objective, String place, int done, int total, boolean main) {}
    private List<QuestEntry> detectedQuests = List.of();
    private void selectQuest() {
        if (detectedQuests.isEmpty()) return;
        int index = Math.max(0, Math.min(detectedQuests.size() - 1, (int)Math.round(questNumber.get()) - 1));
        QuestEntry q = detectedQuests.get(index);
        menuQuest=q.title(); menuObjective=q.objective(); menuPlace=q.place();
        menuDone=q.done(); menuTotal=q.total();
    }
    private String menuDebug = "Menu not scanned";
    private int scannedMenus, scannedItems, scannedLore;
    private static final Pattern OBJECTIVE = Pattern.compile("(?i)^\\s*[-•]?\\s*(.+?)\\s*:\\s*(\\d[\\d,]*)\\s*/\\s*(\\d[\\d,]*).*$");
    private static final Pattern PLACE = Pattern.compile("(?i)^(?:island|dimension|location):\\s*(.+)$");

    // Read authoritative server-supplied lore, and expose diagnostics when the
    // server changes its inventory layout or stops sending objective counters.
    private void readQuestMenu() {
        if (!(mc.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
            menuDebug = "Quest inventory not open";
            return;
        }
        String screenTitle = clean(screen.getTitle().getString());
        if (!screenTitle.toLowerCase(Locale.ROOT).contains("quest")) {
            menuDebug = "Other menu: " + screenTitle;
            return;
        }
        scannedMenus++;
        int items=0,loreItems=0,parsed=0;
        List<QuestEntry> foundQuests = new ArrayList<>();
        // The first slots belong to the container. The last 36 belong to the
        // player's inventory in ordinary server inventory menus.
        int count=Math.max(0,screen.getMenu().slots.size()-36);
        for(int i=0;i<count;i++) {
            Slot slot=screen.getMenu().slots.get(i);
            ItemStack stack=slot.getItem();
            if(stack.isEmpty()) continue;
            items++;
            var lore=stack.get(DataComponents.LORE);
            if(lore==null || lore.lines().isEmpty())continue;
            loreItems++;
            String name=clean(stack.getHoverName().getString());
            String location="",objective="";
            int current=-1,target=-1;
            boolean main=false;
            for(var component:lore.lines()){
                String line=clean(component.getString());
                String lower=line.toLowerCase(Locale.ROOT);
                if(lower.contains("main quest")) main=true;
                Matcher place=PLACE.matcher(line);
                if(place.matches()) location += (location.isEmpty()?"":" / ")+place.group(1).trim();
                Matcher match=OBJECTIVE.matcher(line);
                if(match.matches()) try {
                    int a=Integer.parseInt(match.group(2).replace(",",""));
                    int b=Integer.parseInt(match.group(3).replace(",",""));
                    if(b>0 && a>=0 && a<=b && (current<0 || a<b)){
                        current=a;target=b;objective=match.group(1).trim();
                    }
                } catch(NumberFormatException ignored) { }
            }
            if(current<0||target<=0||name.isBlank())continue;
            parsed++;
            foundQuests.add(new QuestEntry(name, objective, location, current, target, main));
        }
        scannedItems=items;scannedLore=loreItems;
        menuDebug="Quests: "+items+" items / "+loreItems+" lore / "+parsed+" objectives";
        if (!foundQuests.isEmpty()) {
            // Preserve slot order so the user can select any quest by number.
            // Keep the last valid snapshot after closing the menu.
            detectedQuests=List.copyOf(foundQuests);
            menuSeenAt=System.currentTimeMillis();
            selectQuest();
        }
    }

    public CubeCraftCoach() {
        super("CubeCraft SkyBlock Coach", Category.SERVER_MODULES);
        describe("Live CubeCraft sidebar observer. Displays server-provided quest text and progress; does not reset you to a beginner walkthrough.");
    }
    private boolean onCubeCraft() {
        var server=mc.getCurrentServer();
        return server!=null && server.ip!=null && server.ip.toLowerCase(Locale.ROOT).contains("cubecraft");
    }
    private static String clean(String s) {
        return s==null ? "" : s.replaceAll("(?i)§[0-9A-FK-ORX]", "").trim();
    }
    private void readLive() {
        if (mc.level==null) {serverLines=List.of();quest="";progress="";return;}
        try {
            Scoreboard board=mc.level.getScoreboard();
            Objective objective=board.getDisplayObjective(DisplaySlot.SIDEBAR);
            if(objective==null){serverLines=List.of();quest="";progress="";return;}
            ArrayList<String> lines=new ArrayList<>();
            String header=clean(objective.getDisplayName().getString());
            if(!header.isBlank())lines.add(header);
            for(PlayerScoreEntry entry:board.listPlayerScores(objective)) {
                String line=clean(entry.display()!=null?entry.display().getString():entry.owner());
                if(!line.isBlank()&&!line.startsWith("#"))lines.add(line);
            }
            serverLines=List.copyOf(lines);
            String found="",foundProgress="";
            // Only explicit quest/objective lines are treated as quest names.
            for(int i=0;i<lines.size();i++) {
                String line=lines.get(i), lower=line.toLowerCase(Locale.ROOT);
                if(lower.matches(".*\\b(quest|objective|task)\\s*:.*")) {
                    String v=line.replaceFirst("(?i)^.*?\\b(?:quest|objective|task)\\s*:\\s*", "").trim();
                    if(v.isEmpty() && i+1<lines.size())v=lines.get(i+1);
                    if(!v.isBlank()) {found=v;break;}
                }
            }
            if(!found.isBlank()) for(String line:lines) {
                if(COUNTER.matcher(line).matches()){foundProgress=line;break;}
            }
            quest=found;progress=foundProgress;
        } catch(RuntimeException ignored) {serverLines=List.of();quest="";progress="";}
    }
    @Override public void onTick() {
        if(mc.player==null||(!onCubeCraft()&&!outside.get()))return;
        long now=System.currentTimeMillis();
        if(now-readAt<400)return;
        readAt=now;
        readLive();
        readQuestMenu();
    }
    @Override public int editorX(){return drawX;}
    @Override public int editorY(){return drawY;}
    @Override public int editorWidth(){return drawW;}
    @Override public int editorHeight(){return drawH;}
    @Override public void onHud(GuiGraphicsExtractor g,float delta){
        if(mc.player==null||!showHud.get()||(!onCubeCraft()&&!outside.get()))return;
        // HUD render updates the menu snapshot too, including if the event
        // dispatcher does not tick modules while a container screen is open.
        if(mc.gui.screen() instanceof AbstractContainerScreen<?>)readQuestMenu();
        selectQuest(); // Quest number can change in settings without reopening the menu.
        boolean mini=guidance.is("Minimal"),full=guidance.is("Full Walkthrough");
        boolean known=menuDone>=0 && menuTotal>0;
        int w=188,h=mini?32:full?91:69;
        int xx=Math.min(Math.max(0,(int)(x.get()*Gfx.guiW())),Math.max(0,Gfx.guiW()-w));
        int yy=Math.min(Math.max(0,(int)(y.get()*Gfx.guiH())),Math.max(0,Gfx.guiH()-h));
        drawX=xx;drawY=yy;drawW=w;drawH=h;
        HudGlass.card(g,xx,yy,w,h,92);
        int selectedIndex=detectedQuests.isEmpty()?0:Math.max(1,Math.min(detectedQuests.size(),(int)Math.round(questNumber.get())));
        String heading=detectedQuests.isEmpty()?"CUBECRAFT / GUIDE":"CUBECRAFT / QUEST "+selectedIndex+"/"+detectedQuests.size();
        Ui.text(g,HudGlass.fit(heading,w-18),xx+9,yy+6,HudGlass.accent());
        String title=known?menuQuest:(quest.isBlank()?"Open Quests to sync":quest);
        Ui.text(g,HudGlass.fit(title,w-18),xx+9,yy+19,HudGlass.text());
        if(!mini){
            if(known){
                String objective=menuObjective.isBlank()?"Quest objective":menuObjective;
                Ui.text(g,HudGlass.fit(objective+"  "+menuDone+"/"+menuTotal,w-18),xx+9,yy+34,HudGlass.muted());
                HudGlass.progress(g,xx+9,yy+50,w-18,3,Math.min(1.0,(double)menuDone/menuTotal),HudGlass.accent());
            } else {
                Ui.text(g,HudGlass.fit(debug.get()?menuDebug:(quest.isBlank()?"Open quest item tooltip to sync":progress),w-18),xx+9,yy+34,HudGlass.muted());
            }
            if(full){
                String place=known && !menuPlace.isBlank()?menuPlace:"Visit the in-game quest menu";
                Ui.text(g,HudGlass.fit(place,w-18),xx+9,yy+61,HudGlass.muted());
                String footer=debug.get()?menuDebug:(known?"Change quest: Coach settings > Quest number":"Waiting for quest item data");
                Ui.text(g,HudGlass.fit(footer,w-18),xx+9,yy+75,HudGlass.muted());
            }
        }
    }
}
