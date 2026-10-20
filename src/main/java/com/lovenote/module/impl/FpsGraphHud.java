package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Gfx;
import com.lovenote.util.HudWidgetBounds;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Bounded 40-sample FPS graph, entirely in HUD-space. */
public final class FpsGraphHud extends Module implements HudWidgetBounds {
    private final NumberSetting x = add(new NumberSetting("X", .79, 0, .98, .005));
    private final NumberSetting y = add(new NumberSetting("Y", .12, 0, .98, .005));
    private final NumberSetting width = add(new NumberSetting("Width", 122, 65, 250, 5));
    private final NumberSetting height = add(new NumberSetting("Height", 45, 24, 100, 5));
    private final NumberSetting limit = add(new NumberSetting("Graph FPS ceiling", 200, 30, 500, 10));
    private final BoolSetting frame = add(new BoolSetting("Glass frame", true));
    private final BoolSetting low = add(new BoolSetting("Show 1% approximate low", true));
    private final int[] history = new int[40];
    private int n, samples, displayedLow;
    private long nextSample;
    private int drawnX,drawnY;
    public FpsGraphHud() {
        super("FpsGraphHud", Category.HUD);
        describe("Low-overhead rolling FPS chart with optional approximate low-FPS readout.");
    }
    @Override public int editorX(){return drawnX;}
    @Override public int editorY(){return drawnY;}
    @Override public int editorWidth(){return width.getI();}
    @Override public int editorHeight(){return height.getI();}
    @Override public void onEnable(){n=0;samples=0;nextSample=0;displayedLow=0;}
    @Override public void onTick(){
        if(mc.player == null) return;
        long now = System.currentTimeMillis();
        if(now < nextSample) return;
        nextSample = now + 500;
        int fps = mc.getFps();
        history[n++ % history.length] = Math.max(0, fps);
        samples = Math.min(samples + 1, history.length);
        // Minimum sample is an indicator, not a statistically exact 1% low.
        int v = Integer.MAX_VALUE;
        for(int i=0;i<samples;i++) v=Math.min(v,history[i]);
        displayedLow = v==Integer.MAX_VALUE ? 0 : v;
    }
    @Override public void onHud(GuiGraphicsExtractor g,float delta){
        if(mc.player == null) return;
        int w=width.getI(),h=height.getI();
        int px=Math.max(0,Math.min((int)(x.get()*Gfx.guiW()),Gfx.guiW()-w));
        int py=Math.max(0,Math.min((int)(y.get()*Gfx.guiH()),Gfx.guiH()-h));
        drawnX=px; drawnY=py;
        if(frame.get()) Ui.widgetCard(g,px,py,w,h,205);
        Ui.text(g,"FPS  "+mc.getFps()+(low.get()?"  · low "+displayedLow:""),px+5,py+4,Ui.textCol);
        int chartBottom=py+h-4, chartH=Math.max(4,h-20);
        for(int i=0;i<samples;i++){
            int idx=(n-samples+i+history.length*2)%history.length;
            int fps=history[idx];
            int bh=Math.max(1,(int)Math.round(chartH*Math.min(1.0,fps/(double)limit.getI())));
            int bx=px+4+(int)((w-8)*(i/(double)history.length));
            Ui.rect(g,bx,chartBottom-bh,Math.max(1,(w-8)/history.length-1),bh,
                Colors.alpha(fps<50?0xFFFF819B:Ui.accent2,185));
        }
    }
}
