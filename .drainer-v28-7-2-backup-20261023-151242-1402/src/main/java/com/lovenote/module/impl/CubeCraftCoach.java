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
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.Scoreboard;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
    private int drawX,drawY,drawW=194,drawH=65;
    private List<String> serverLines = List.of();
    private String quest = "";
    private String progress = "";
    private long readAt;

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
    }
    @Override public int editorX(){return drawX;}
    @Override public int editorY(){return drawY;}
    @Override public int editorWidth(){return drawW;}
    @Override public int editorHeight(){return drawH;}
    @Override public void onHud(GuiGraphicsExtractor g,float delta){
        if(mc.player==null||!showHud.get()||(!onCubeCraft()&&!outside.get()))return;
        boolean mini=guidance.is("Minimal"),full=guidance.is("Full Walkthrough");
        int w=194,h=mini?41:full?86:62;
        int xx=Math.min(Math.max(0,(int)(x.get()*Gfx.guiW())),Math.max(0,Gfx.guiW()-w));
        int yy=Math.min(Math.max(0,(int)(y.get()*Gfx.guiH())),Math.max(0,Gfx.guiH()-h));
        drawX=xx;drawY=yy;drawW=w;drawH=h;
        HudGlass.card(g,xx,yy,w,h,130);
        Ui.text(g,"CUBECRAFT  •  LIVE GUIDE",xx+9,yy+7,HudGlass.accent());
        Ui.text(g,HudGlass.fit(quest.isBlank()?"Quest not exposed by server":quest,w-18),xx+9,yy+21,HudGlass.text());
        if(!mini){
            String detail=!progress.isBlank()?progress:(serverLines.isEmpty()?"No sidebar data received":"Open Quests to check active task");
            Ui.text(g,HudGlass.fit(detail,w-18),xx+9,yy+36,HudGlass.muted());
            if(full){
                var pos=mc.player.blockPosition();
                Ui.text(g,"XYZ  "+pos.getX()+" / "+pos.getY()+" / "+pos.getZ(),xx+9,yy+52,HudGlass.muted());
                Ui.text(g,debug.get()?HudGlass.fit(String.join(" | ",serverLines),w-18):"Source: server scoreboard",xx+9,yy+68,HudGlass.muted());
            }
        }
    }
}
