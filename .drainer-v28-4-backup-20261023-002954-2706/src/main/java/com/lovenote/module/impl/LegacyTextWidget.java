package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Gfx;
import com.lovenote.util.HudWidgetBounds;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Common positioning and frame rendering for ported 1.21 text HUD widgets. */
abstract class LegacyTextWidget extends Module implements HudWidgetBounds {
    private int drawnX,drawnY,drawnW=80,drawnH=15;
    @Override public final int editorX(){return drawnX;}
    @Override public final int editorY(){return drawnY;}
    @Override public final int editorWidth(){return drawnW;}
    @Override public final int editorHeight(){return drawnH;}
    protected final NumberSetting x;
    protected final NumberSetting y;
    protected final BoolSetting frame = add(new BoolSetting("Frame", true));
    protected final NumberSetting opacity = add(new NumberSetting("Panel opacity", 205, 50, 255, 5));
    protected final BoolSetting compact = add(new BoolSetting("Compact layout", true));
    protected LegacyTextWidget(String name, double px, double py) {
        super(name, Category.HUD);
        x = add(new NumberSetting("X", px, 0, 0.98, 0.005));
        y = add(new NumberSetting("Y", py, 0, 0.98, 0.005));
    }
    /** Give a custom card stable hitbox dimensions in the native HUD editor. */
    protected final void drawCustomCard(GuiGraphicsExtractor g, int width, int height) {
        int gw = Math.max(1, Gfx.guiW()), gh = Math.max(1, Gfx.guiH());
        drawnW = Math.max(1, width); drawnH = Math.max(1, height);
        drawnX = Math.max(0, Math.min((int) (x.get() * gw), Math.max(0, gw - drawnW)));
        drawnY = Math.max(0, Math.min((int) (y.get() * gh), Math.max(0, gh - drawnH)));
        if (frame.get()) com.lovenote.util.HudGlass.card(g, drawnX, drawnY, drawnW, drawnH, opacity.getI());
    }

    protected final void drawText(GuiGraphicsExtractor g, String value) {
        if (value == null || value.isEmpty()) return;
        // Consistent narrow 'data chip' instead of each widget's old bulky box.
        int pad = compact.get() ? 7 : 10;
        int h = compact.get() ? 19 : 23;
        int w = Ui.textW(value) + pad * 2 + 4;
        int px = Math.max(0, Math.min((int)(x.get() * Gfx.guiW()), Math.max(0, Gfx.guiW() - w)));
        int py = Math.max(0, Math.min((int)(y.get() * Gfx.guiH()), Math.max(0, Gfx.guiH() - h)));
        drawnX=px;drawnY=py;drawnW=w;drawnH=h;
        if (frame.get()) Ui.widgetCard(g, px, py, w, h, opacity.getI());
        // Colour the metric label rather than adding another decorative block.
        int split = value.indexOf(' ');
        if (split > 1 && split < 14) {
            String title = value.substring(0, split);
            Ui.text(g, title, px + pad, py + (h - 8) / 2, com.lovenote.util.HudGlass.accent());
            Ui.text(g, value.substring(split), px + pad + Ui.textW(title),
                    py + (h - 8) / 2, Ui.textCol);
        } else {
            Ui.text(g, value, px + pad, py + (h - 8) / 2, Ui.textCol);
        }
    }
}
