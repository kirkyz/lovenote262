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

/** CubeCraft guidance starter. No invented server-side quest completion data. */
public final class CubeCraftCoach extends Module implements HudWidgetBounds {
    private record Goal(String title, String action, String where) { }
    private static final Goal[] GOALS = {
        new Goal("Find your current quests", "Open the CubeCraft SkyBlock quest menu.", "Your SkyBlock island"),
        new Goal("Check the active objective", "Read the quest requirements and pin one objective.", "Quest menu"),
        new Goal("Collect quest resources", "Gather the exact blocks or items your quest requests.", "Quest-listed island or area"),
        new Goal("Claim your reward", "Return to the quest screen and claim completion if available.", "Quest menu"),
        new Goal("Explore your next unlock", "Follow the next unlocked quest or island objective.", "Quest / island map"),
        new Goal("Review your equipment", "Upgrade gear based on the next quest requirements.", "Inventory / crafting"),
        new Goal("Plan dungeon progression", "Check dungeon unlocks and requirements before entering.", "Dungeon interface")
    };
    private final BoolSetting showHud = add(new BoolSetting("Show guide HUD", true));
    private final ModeSetting guidance = add(new ModeSetting("Guidance", "Full Walkthrough", "Minimal", "Guided", "Full Walkthrough"));
    private final NumberSetting stage = add(new NumberSetting("Quest step (manual)", 1, 1, GOALS.length, 1));
    private final NumberSetting x = add(new NumberSetting("HUD X", .025, 0, .95, .005));
    private final NumberSetting y = add(new NumberSetting("HUD Y", .35, 0, .90, .005));
    private final BoolSetting outside = add(new BoolSetting("Preview outside CubeCraft", false));
    private int drawX,drawY,drawW=220,drawH=62;
    public CubeCraftCoach() {
        super("CubeCraft SkyBlock Coach", Category.SERVER_MODULES);
        describe("Step-by-step SkyBlock HUD with live coordinates, manual quest steps and a server-safe tracking status.");
    }
    private boolean onCubeCraft() {
        var server = mc.getCurrentServer();
        return server != null && server.ip != null && server.ip.toLowerCase(java.util.Locale.ROOT).contains("cubecraft");
    }
    @Override public int editorX() { return drawX; }
    @Override public int editorY() { return drawY; }
    @Override public int editorWidth() { return drawW; }
    @Override public int editorHeight() { return drawH; }
    @Override public void onHud(GuiGraphicsExtractor g,float delta) {
        if (mc.player == null || !showHud.get() || (!outside.get() && !onCubeCraft())) return;
        int i = Math.max(0,Math.min(GOALS.length-1,stage.getI()-1));
        Goal goal=GOALS[i];
        boolean minimal=guidance.is("Minimal"), full=guidance.is("Full Walkthrough");
        int w=245,h=minimal?47:full?97:72;
        int xx=Math.min(Math.max(0,(int)(x.get()*Gfx.guiW())),Math.max(0,Gfx.guiW()-w));
        int yy=Math.min(Math.max(0,(int)(y.get()*Gfx.guiH())),Math.max(0,Gfx.guiH()-h));
        drawX=xx;drawY=yy;drawW=w;drawH=h;
        HudGlass.card(g,xx,yy,w,h,195);
        Ui.text(g,"CUBECRAFT  /  SKYBLOCK",xx+9,yy+8,HudGlass.accent());
        Ui.text(g,(i+1)+"/"+GOALS.length+"  "+goal.title(),xx+9,yy+23,HudGlass.text());
        if(!minimal) {
            Ui.text(g,"NEXT: "+goal.action(),xx+9,yy+39,HudGlass.muted());
            if(full) {
                Ui.text(g,"WHERE: "+goal.where(),xx+9,yy+55,HudGlass.muted());
                var pos=mc.player.blockPosition();
                Ui.text(g,"LIVE XYZ: "+pos.getX()+", "+pos.getY()+", "+pos.getZ(),xx+9,yy+71,HudGlass.text());
            }
        }
        // CubeCraft does not expose a supported quest API here. Never infer completion from inventory alone.
    }
}
