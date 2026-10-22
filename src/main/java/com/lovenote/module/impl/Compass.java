package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Gfx;
import com.lovenote.util.HudWidgetBounds;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** 26.2 compass strip based on the legacy Lovenote 15-degree tick layout. */
public final class Compass extends Module implements HudWidgetBounds {
    private final NumberSetting xPos = add(new NumberSetting("X", .22, 0, .98, .005));
    private final NumberSetting yPos = add(new NumberSetting("Y", .015, 0, .98, .005));
    private int drawnX, drawnY, drawnW=200;
    @Override public int editorX(){return drawnX;}
    @Override public int editorY(){return drawnY;}
    @Override public int editorWidth(){return drawnW;}
    @Override public int editorHeight(){return 22;}
    private static final String[] DIRS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
    private final NumberSetting width = add(new NumberSetting("Width", 200, 80, 500, 10));

    public Compass() {
        super("Compass", Category.HUD);
        describe("Heading strip with tick marks and cardinal directions.");
    }

    @Override
    public void onHud(GuiGraphicsExtractor g, float delta) {
        if (mc.player == null) return;
        int w = Math.min(width.getI(), Math.max(100, Gfx.guiW() - 16));
        int x = Math.max(0,Math.min((int)(xPos.get()*Gfx.guiW()),Math.max(0,Gfx.guiW()-w)));
        int y = Math.max(0,Math.min((int)(yPos.get()*Gfx.guiH()),Math.max(0,Gfx.guiH()-22)));
        int h = 22;
        drawnX=x;drawnY=y;drawnW=w;
        Ui.pill(g, x, y, w, h);
        float yaw = ((mc.player.getYRot() % 360f) + 360f) % 360f;
        for (int deg = 0; deg < 360; deg += 15) {
            float diff = deg - yaw;
            while (diff < -180f) diff += 360f;
            while (diff > 180f) diff -= 360f;
            if (Math.abs(diff) > 85f) continue;
            int px = x + w / 2 + Math.round(diff / 90f * (w / 2f));
            boolean major = deg % 45 == 0;
            Ui.rect(g, px, y + (major ? 3 : 7), 1, major ? 6 : 3, major ? Ui.textCol : Ui.textDim);
            if (major) {
                String dir = DIRS[deg / 45];
                Ui.text(g, dir, px - Ui.textW(dir) / 2, y + 12, Ui.textCol);
            }
        }
        Ui.rect(g, x + w / 2, y + 1, 1, h - 2, Ui.accent1);
    }
}
