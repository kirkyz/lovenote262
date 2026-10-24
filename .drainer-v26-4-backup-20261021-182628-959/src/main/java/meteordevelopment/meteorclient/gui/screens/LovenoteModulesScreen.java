package meteordevelopment.meteorclient.gui.screens;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.InputConstants;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteDraw;
import meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteMenuConfig;
import meteordevelopment.meteorclient.gui.themes.lovenote.LovenotePalette;
import meteordevelopment.meteorclient.gui.themes.lovenote.DrainerSkin;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/** Drainer module and HUD screen, including the streamlined Moon workspace. */
public class LovenoteModulesScreen extends TabScreen {
    private static final int PW = 208, PH = 320, ACC_COLS = 11, RAIL_MIN = 46;
    private static final String[] MACRO_WORKSPACE_LABELS = {"Guides", "Library", "SkyBlock", "Schems"};
    private static final String[] MACRO_SKYBLOCK_TRACKS = {"START", "LEVELS", "GARDEN", "MINING", "COMBAT", "COINS"};
    // Existing registry placeholders stay untouched. Only their menu grouping changes.
    private static final Set<String> RAGE_MODULES = Set.of(
        "Aura", "KillAura", "AimAssist", "Triggerbot", "Criticals", "WTap",
        "AutoClicker", "AutoCrystal", "CrystalTap", "CrystalBreak", "Backtrack",
        "TargetStrafe", "AutoWeb", "Reach", "Surround", "AutoTrap",
        "AutoCity", "OldCombat"
    );
    private static int mainX = Integer.MIN_VALUE, mainY, panX = Integer.MIN_VALUE, panY, page;
    private static long sessionStart;

    private static final int K_NONE = -1, K_CARD = 0, K_SWITCH = 1, K_BOOL = 2, K_SLIDER = 3, K_MODE = 4,
            K_PTOGGLE = 6, K_PCLOSE = 7, K_BIND = 8, K_ACCENT = 9, K_EDITOR = 10, K_CLOSE = 12, K_SEARCH = 13, K_MULTI = 5, K_CONFIG = 14, K_GUIDE = 15, K_COLOR_OPEN = 16, K_COLOR_CHANNEL = 17, K_COLOR_HEX = 18, K_COLOR_PRESET = 19, K_COLOR_RESET = 20, K_COLOR_RAINBOW = 21;

    private static final class Hit {
        int x, y, w, h, kind, idx;
        Row module;
        Object setting;
    }

    private static final class Ripple { int x, y; long t; }

    private final List<com.lovenote.module.Category> cats = new ArrayList<>();
    private final List<Row> allModules = new ArrayList<>();
    // The module roster is immutable between buildRows() calls. Avoid rebuilding
    // and sorting a fresh list every render frame just to read each active state.
    private List<Row> visibleCache = List.of();
    private String visibleCacheQuery = null;
    private int visibleCachePage = Integer.MIN_VALUE;

    private final List<Hit> hits = new ArrayList<>();
    private final List<int[]> pageRects = new ArrayList<>();
    private final List<Ripple> ripples = new ArrayList<>();
    private final Map<String, Float> anim = new HashMap<>();

    private int wx, wy, winW = 600, winH = 372, railW = 118;
    private float gs = 1f;
    private int vw, vh;
    private float dt, ga = 1f, openMs, closeMs, tabMs, scrollTarget, scrollShown, panelScrollTarget, panelScrollShown;
    private float pillY = -1f;
    // Purely visual drawer transition, independent from module state and input.
    private Row v26PreviousSelection;
    private float v26DrawerProgress = 1f;
    // UI-only spring coordinate; follows category selection at any FPS.
    private float motionRailY = Float.NaN;

    private int maxScroll, panelMax;
    private boolean closing, finishClose, animating;
    private long lastNano;
    private Row selected, panelModule, listening;
    private Hit dragging;
    private int dragWin, grabX, grabY;
    private boolean settingFields;
    // Meteor ColorSetting controls: collapsed by default to keep the panel compact.
    private ColorSetting expandedColor;
    private ColorSetting hexEditing;
    private String hexInput = "";
    private int colorDragValue;
    private static final int[] COLOR_PRESETS = {
        0xFFAA67FF, 0xFFFB55C3, 0xFF54AFFF, 0xFF4CDCB1, 0xFFFFD36C, 0xFFFFFFFF
    };
    private String tooltip;
    private float sbAlpha = 0.45f;

    private float[] px, py, pvx, pvy;
    private int particleCount = 45;
    private int moonContentTop;
    private String searchText = "";
    private boolean searchFocused;
    // Drainer-native HUD editor: independent of Meteor's HUD implementation.
    // Appearance is now a dedicated full-screen tab, not hidden in Config.
    private int appearanceTab;
    private int appearancePresetPage;
    private int appearanceHexField; // 0=none, 1=primary, 2=secondary
    private String appearanceHexInput = "";
    private int macroDetailBottom = Integer.MAX_VALUE;
    private boolean macroGuideOpen;
    private int macroGuideSelected, macroGuideScroll;
    private int macroTemplateSelected;
    private boolean macroLibraryMode, macroGuideTyping;
    private int macroWorkspaceTab, schematicSelected, schematicScroll, schematicMaterialOffset;
    private boolean schematicMaterialsExpanded;
    private java.util.List<String> schematicFiles=java.util.List.of();
    private String macroGuideQuery="", macroFeedback="";
    private String macroSaveName="";
    private boolean macroNameTyping;
    private final java.util.List<int[]> macroGuideHits = new java.util.ArrayList<>();
    private boolean drainerHudEditor;
    private com.lovenote.module.Module hudSelected;
    private com.lovenote.module.Module hudDragging;
    private com.lovenote.setting.NumberSetting hudSlider;
    private int hudListScroll, hudSettingsScroll;
    private boolean hudInspectorOpen;
    private boolean hudResizing;
    private static final float HUD_CHROME_SCALE = 0.68f;
    private int hudInspectorLeft = Integer.MAX_VALUE;
    private double hudResizeStartX, hudResizeStartScale;

    private final java.util.List<HudHit> hudHits = new java.util.ArrayList<>();
    private static final class HudHit {
        final int x,y,w,h,action;
        final com.lovenote.module.Module module;
        final com.lovenote.setting.Setting setting;
        HudHit(int x,int y,int w,int h,int action,com.lovenote.module.Module module,com.lovenote.setting.Setting setting) {
            this.x=x;this.y=y;this.w=w;this.h=h;this.action=action;this.module=module;this.setting=setting;
        }
        boolean contains(double mx,double my) {return mx>=x&&my>=y&&mx<x+w&&my<y+h;}
    }
    public boolean isDrainerHudEditor() { return drainerHudEditor; }
    private static boolean hudModule(com.lovenote.module.Module m) {
        if (m.hidden) return false;
        try {
            return m.getClass().getMethod("onHud", GuiGraphicsExtractor.class, float.class)
                .getDeclaringClass() != com.lovenote.module.Module.class;
        } catch (ReflectiveOperationException ex) {
            return m.category == com.lovenote.module.Category.HUD || m.category == com.lovenote.module.Category.SPOTIFY;
        }
    }
    private java.util.List<com.lovenote.module.Module> hudModules() {
        java.util.List<com.lovenote.module.Module> list = new java.util.ArrayList<>();
        if (com.lovenote.Lovenote.modules != null) {
            for (com.lovenote.module.Module m : com.lovenote.Lovenote.modules.all()) if (hudModule(m)) list.add(m);
        }
        return list;
    }
    private static com.lovenote.setting.NumberSetting hudNumber(com.lovenote.module.Module m,String name) {
        if(m==null)return null;
        for (com.lovenote.setting.Setting st:m.settings) {
            if (st instanceof com.lovenote.setting.NumberSetting n && st.name.equalsIgnoreCase(name)) return n;
        }
        return null;
    }
    private void hudHit(int x,int y,int w,int h,int action,com.lovenote.module.Module m,com.lovenote.setting.Setting st) {
        hudHits.add(new HudHit(Math.round(x*HUD_CHROME_SCALE),Math.round(y*HUD_CHROME_SCALE),
            Math.max(1,Math.round(w*HUD_CHROME_SCALE)),Math.max(1,Math.round(h*HUD_CHROME_SCALE)),action,m,st));
    }
    private void enterHudEditor() {
        drainerHudEditor=true;
        hudSelected=null;
        hudDragging=null;hudSlider=null;hudListScroll=0;hudSettingsScroll=0;
        hudInspectorOpen=false;
    }
    private int hudWidgetX(com.lovenote.module.Module m) {
        if (m instanceof com.lovenote.util.HudWidgetBounds bounds) return bounds.editorX();
        if(m instanceof com.lovenote.module.impl.SpotifyHud sp)return sp.editorX();
        if(m instanceof com.lovenote.module.impl.Lyrics lr)return lr.editorX();
        var setting=hudNumber(m,"X");return setting==null?-1:(int)(setting.get()*width);
    }
    private int hudWidgetY(com.lovenote.module.Module m) {
        if (m instanceof com.lovenote.util.HudWidgetBounds bounds) return bounds.editorY();
        if(m instanceof com.lovenote.module.impl.SpotifyHud sp)return sp.editorY();
        if(m instanceof com.lovenote.module.impl.Lyrics lr)return lr.editorY();
        var setting=hudNumber(m,"Y");return setting==null?-1:(int)(setting.get()*height);
    }
    private int hudWidgetW(com.lovenote.module.Module m) {
        if (m instanceof com.lovenote.util.HudWidgetBounds bounds) return bounds.editorWidth();
        if(m instanceof com.lovenote.module.impl.SpotifyHud sp)return sp.editorWidth();
        if(m instanceof com.lovenote.module.impl.Lyrics lr)return lr.editorWidth();
        return 160;
    }
    private int hudWidgetH(com.lovenote.module.Module m) {
        if (m instanceof com.lovenote.util.HudWidgetBounds bounds) return bounds.editorHeight();
        if(m instanceof com.lovenote.module.impl.SpotifyHud sp)return sp.editorHeight();
        if(m instanceof com.lovenote.module.impl.Lyrics lr)return lr.editorHeight();
        return 30;
    }
    private void hudMoveModule(com.lovenote.module.Module m,double x,double y) {
        var sx=hudNumber(m,"X");var sy=hudNumber(m,"Y");
        if(sx!=null)sx.set(Math.max(0,Math.min(1,x/Math.max(1,width))));
        if(sy!=null)sy.set(Math.max(0,Math.min(1,y/Math.max(1,height))));
    }
    // Compact HUD editor: clicking a translucent item in the dock activates that
    // widget; the Minecraft world stays visible while the widget is dragged.
    private void hudDraw(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta) {
        ga=1f;
        hudHits.clear();
        hudInspectorLeft=Integer.MAX_VALUE;
        // Leave the world completely unobstructed; the widget palette has no backdrop.
        java.util.List<com.lovenote.module.Module> mods=hudModules();
        for(com.lovenote.module.Module m:mods) if(m.enabled)
            com.lovenote.util.HudScale.draw(m,g,delta);

        // World-space outline and corner grip follow the actual, unscaled widget.
        if(hudSelected!=null && hudSelected.enabled) {
            int bx=hudWidgetX(hudSelected),by=hudWidgetY(hudSelected);
            if(bx>=0 && by>=0) {
                double sc=com.lovenote.util.HudScale.get(hudSelected);
                int ww=Math.max(16,(int)Math.round(hudWidgetW(hudSelected)*sc));
                int hh=Math.max(12,(int)Math.round(hudWidgetH(hudSelected)*sc));
                // Two thin lines plus a subtle outer halo, rather than a large box.
                rr(g,bx-2,by-2,ww+4,hh+4,4,0x164E1F73);
                fill(g,bx,by,bx+ww,by+1,0xB4DC97F5);
                fill(g,bx,by+hh-1,bx+ww,by+hh,0xB4DC97F5);
                fill(g,bx,by,bx+1,by+hh,0xB4DC97F5);
                fill(g,bx+ww-1,by,bx+ww,by+hh,0xB4DC97F5);
                neon(g,bx+ww-6,by+hh-6,10,10,4,0xFFD8A7ED,0x9FCC75FF);
                hudHits.add(new HudHit(bx+ww-10,by+hh-10,18,18,15,hudSelected,null));
            }
        }

        // The controls alone are drawn at 68% of GUI scale. Gameplay/HUD elements
        // retain their actual scale and coordinates for accurate direct dragging.
        var pose=g.pose();
        pose.pushMatrix();
        pose.scale(HUD_CHROME_SCALE,HUD_CHROME_SCALE);
        try {
            int vWidth=Math.max(1,(int)(width/HUD_CHROME_SCALE));
            int vHeight=Math.max(1,(int)(height/HUD_CHROME_SCALE));
            txt(g,"DRAINER  /  HUD EDITOR",19,18,DrainerSkin.TEXT);
            txt(g,"ESC to close",185,18,DrainerSkin.MUTED);

            // Pure text dock: no full-width window, no chips, no individual boxes.
            // A restrained underline indicates the selected module without hiding gameplay.
            int dockX=18,dockH=32,dockY=vHeight-dockH-12,dockW=vWidth-36;
            txt(g,"WIDGETS  ·  LEFT: SHOW / DRAG  ·  RIGHT: HIDE  ·  SCROLL",dockX,dockY-11,0xAFCBB7DA);
            int chipX=dockX,chipY=dockY+8,chipH=18,gap=18;
            int maxVisible=dockX+dockW-6;
            hudListScroll=Math.max(0,Math.min(Math.max(0,mods.size()-1),hudListScroll));
            for(int i=hudListScroll;i<mods.size();i++) {
                var m=mods.get(i);
                int chipW=Math.max(35,Math.min(120,font.width(m.name)+6));
                if(chipX+chipW>maxVisible)break;
                boolean sel=m==hudSelected;
                String label=m.name;
                while(label.length()>4&&font.width(label)>chipW-6) label=label.substring(0,label.length()-2);
                if (sel) {
                    g.fill(chipX,chipY-1,chipX+chipW,chipY+chipH+1,0x463B3154);
                    DrainerSkin.selectionMark(g,chipX,chipY,chipH);
                }
                txt(g,label,chipX+3,chipY+4,
                    sel?0xFFF6DDFE:m.enabled?0xDAD6BADF:0xA7B09ABA);
                if(sel) {
                    fill(g,chipX+3,chipY+chipH-1,chipX+chipW-1,chipY+chipH,0xCCE4A6F7);
                } else if(m.enabled) {
                    fill(g,chipX+3,chipY+chipH-1,chipX+chipW-1,chipY+chipH,0x558B689D);
                }
                hudHit(chipX,chipY,chipW,chipH,1,m,null);
                chipX+=chipW+gap;
            }
            // The gallery scrolls horizontally via mouse wheel when more widgets exist.
            if(chipX>=maxVisible && !mods.isEmpty())
                txt(g,"›",vWidth-18,chipY+4,0xCBDFB9F1);

            // No permanent settings column: open only after selecting a widget.
            if(hudSelected!=null && hudInspectorOpen) {
                int iw=Math.min(210,Math.max(165,vWidth/3));
                int ih=Math.min(156,Math.max(108,vHeight-dockH-53));
                int ix=vWidth-iw-17,iy=Math.max(46,dockY-ih-10);
                hudInspectorLeft=Math.round(ix*HUD_CHROME_SCALE);
                DrainerSkin.surface(g,ix,iy,iw,ih,9,false);
                txt(g,"WIDGET  /  "+hudSelected.name,ix+12,iy+13,DrainerSkin.TEXT);
                txt(g,"×",ix+iw-18,iy+12,0xFFF6B8E9);
                hudHit(ix+iw-25,iy+5,21,23,16,hudSelected,null);
                int line=iy+34;
                DrainerSkin.surface(g,ix+12,line-2,40,17,7,hudSelected.enabled);
                txt(g,hudSelected.enabled?"ON":"OFF",ix+22,line+3,0xFFFFDCF8);
                hudHit(ix+12,line-2,40,17,2,hudSelected,null);
                txt(g,"SCALE",ix+65,line+2,0xFFD5BCD9);
                double current=com.lovenote.util.HudScale.get(hudSelected);
                txt(g,String.format(java.util.Locale.ROOT,"%d%%",(int)Math.round(current*100)),
                    ix+iw-42,line+2,0xFFFFD3FA);
                int sx=ix+13,sy=line+25,sw=iw-26;
                rr(g,sx,sy,sw,3,2,0xFF493454);
                int fillW=(int)Math.round(sw*(current-.30)/2.20);
                if(fillW>0)rr(g,sx,sy,fillW,3,2,0xFFE6A1F4);
                rr(g,sx+fillW-2,sy-3,7,9,3,0xFFFFC5F8);
                hudHit(sx,sy-7,sw,18,14,hudSelected,null);
                txt(g,"30%",sx,sy+11,0xFFA88EB8);
                txt(g,"250%",ix+iw-42,sy+11,0xFFA88EB8);

                int yy=sy+30,limit=iy+ih-9,index=0;
                for(com.lovenote.setting.Setting st:hudSelected.settings) {
                    if(index++<hudSettingsScroll)continue;
                    if(yy+19>limit)break;
                    if(st instanceof com.lovenote.setting.NumberSetting n) {
                        String label=st.name;
                        while(label.length()>4&&font.width(label)>iw-77)label=label.substring(0,label.length()-2);
                        txt(g,label,ix+13,yy+2,0xFFDDC9E7);
                        txt(g,String.format(java.util.Locale.ROOT,"%.1f",n.get()),ix+iw-47,yy+2,0xFFEBB4EA);
                        hudHit(ix+13,yy,iw-24,17,3,hudSelected,st);
                    } else if(st instanceof com.lovenote.setting.BoolSetting b) {
                        txt(g,st.name,ix+13,yy+2,0xFFDDC9E7);
                        txt(g,b.get()?"ON":"OFF",ix+iw-36,yy+2,b.get()?0xFFECA8EB:0xFF9883A8);
                        hudHit(ix+13,yy,iw-24,17,4,hudSelected,st);
                    } else if(st instanceof com.lovenote.setting.ModeSetting mode) {
                        txt(g,st.name,ix+13,yy+2,0xFFD4B4E0);
                        hudHit(ix+13,yy,iw-24,17,10,hudSelected,st);
                    }
                    yy+=19;
                }
            }
        } finally { pose.popMatrix(); }
    }

    private void neon(GuiGraphicsExtractor g,int x,int y,int w,int h,int r,int fill,int edge) {
        if(w<=2||h<=2)return;
        rr(g,x-3,y-3,w+6,h+6,r+3,(edge&0x00FFFFFF)|0x0B000000);
        rr(g,x-1,y-1,w+2,h+2,r+1,edge);
        rr(g,x,y,w,h,r,fill);
        rr(g,x+1,y+1,w-2,Math.max(3,h/4),Math.max(2,r-1),0x181E142C);
        fill(g,x+Math.max(5,r),y+1,x+w-Math.max(5,r),y+2,0x256FD9F7);
    }
    // V12: Macro Guide and real (four-template) native Macro Library share one small glass window.
    private float macroScale() {
        // The previous cap (.62) filled most of a 1080p display at GUI scale 4.
        // Keep the virtual layout stable so preset hitboxes and save/load actions
        // remain aligned, but reduce its final size on screen.
        return Math.max(.32f, Math.min(.83f,
            Math.min((width - 26f) / 738f, (height - 26f) / 474f)));
    }
/** Same radius, surface and editable accent used by the main Drainer dashboard. */
private void macroGlass(GuiGraphicsExtractor g,int x,int y,int w,int h,int r,int bg,int edge) {
    // Controls and cards must not share one flat fill: derive state from
    // original active colours, keeping the existing hit rectangles intact.
    boolean selected = ((bg >>> 16) & 0xFF) >= 70 ||
        (((edge >>> 24) & 0xFF) >= 0x80 && ((bg >>> 16) & 0xFF) >= 60);
    if (h >= 95) DrainerSkin.surface(g,x,y,w,h,11,false);
    else DrainerSkin.control(g,x,y,w,h,selected);
}
    private void macroGuideDraw(GuiGraphicsExtractor g) {
        macroGuideHits.clear();
        g.fill(0,0,width,height,0x64060912);
        float s=macroScale();
        int vWidth=Math.max(1,Math.round(width/s)),vHeight=Math.max(1,Math.round(height/s));
        var pose=g.pose();
        pose.pushMatrix(); pose.scale(s,s);
        // Compact modal independent of Minecraft's pixelated GUI scale.
        int boxW=Math.min(vWidth-24,macroWorkspaceTab==3?706:640),boxH=Math.min(vHeight-24,macroWorkspaceTab==3?440:405);
        int boxX=(vWidth-boxW)/2,boxY=(vHeight-boxH)/2;
        DrainerSkin.surface(g,boxX,boxY,boxW,boxH,13,false);
        // One restrained active-colour line: same visual language as the module dashboard.
        g.fill(boxX+22,boxY+2,boxX+boxW-22,boxY+3,DrainerSkin.alpha(DrainerSkin.accent(),84));
        // Purposeful compact header: brand lockup, active tab pills and close.
        DrainerSkin.control(g,boxX+14,boxY+9,25,25,true);
        txtS(g,"D",boxX+23,boxY+17,DrainerSkin.TEXT);
        txtS(g,"DRAINER",boxX+49,boxY+11,DrainerSkin.TEXT);
        txt(g,"WORKSPACE  /  "+MACRO_WORKSPACE_LABELS[Math.max(0,Math.min(3,macroWorkspaceTab))].toUpperCase(java.util.Locale.ROOT),
            boxX+49,boxY+24,DrainerSkin.MUTED);
        DrainerSkin.section(g,boxX+15,boxY+40,boxW-30);
        int tabW=Math.max(43,Math.min(67,(boxW-165)/4));
        int tabX=boxX+boxW-4*(tabW+4)-27;
        for(int i=0;i<4;i++) {
            int xx=tabX+i*(tabW+3);
            boolean active=macroWorkspaceTab==i;
            DrainerSkin.control(g,xx,boxY+10,tabW,23,active);
            if (active) g.fill(xx+10,boxY+31,xx+tabW-10,boxY+32,DrainerSkin.alpha(DrainerSkin.accent(),185));
            txt(g,MACRO_WORKSPACE_LABELS[i],xx+8,boxY+18,active?DrainerSkin.TEXT:DrainerSkin.SUBTEXT);
            macroGuideHits.add(new int[]{xx,boxY+10,tabW,23,-20-i});
        }
        int xClose=boxX+boxW-19;
        txtS(g,"x",xClose,boxY+18,DrainerSkin.SUBTEXT);
        macroGuideHits.add(new int[]{xClose-8,boxY+8,24,25,-1});
        DrainerSkin.field(g,boxX+14,boxY+49,Math.min(257,boxW/2),25);
        String placeholder=switch(macroWorkspaceTab){case 1 -> "Search presets...";case 2 -> "SkyBlock objective tracker";case 3 -> "Local schematics folder";default -> "Search guides...";};
        txt(g,macroGuideQuery.isEmpty()?placeholder:macroGuideQuery,
            boxX+24,boxY+57,macroGuideQuery.isEmpty()?DrainerSkin.MUTED:DrainerSkin.TEXT);
        macroGuideHits.add(new int[]{boxX+14,boxY+49,Math.min(257,boxW/2),25,-4});
        txt(g,switch(macroWorkspaceTab){case 1 -> "Built-in presets  /  Saved routes";case 2 -> "Step-by-step levelling  /  Profit planner";case 3 -> "Local files  /  Manual block placement";default -> "Browse guides  /  Instructions";},
            boxX+17,boxY+83,DrainerSkin.MUTED);
        int paneX=boxX+14,paneY=boxY+101,paneW=Math.max(132,Math.min(213,boxW/3)),paneH=boxH-115,rowH=29;
        macroGlass(g,paneX,paneY,paneW,paneH,9,0xB9151122,0x334D3169);
        if(macroWorkspaceTab==2){skyBlockGuideDraw(g,boxX,boxY,boxW,boxH,paneX,paneY,paneW,paneH);pose.popMatrix();return;}
        if(macroWorkspaceTab==3){schematicGuideDraw(g,boxX,boxY,boxW,boxH,paneX,paneY,paneW,paneH);pose.popMatrix();return;}
        var library=com.lovenote.Lovenote.modules == null ? null :
            com.lovenote.Lovenote.modules.get(com.lovenote.module.impl.MacroLibrary.class);
        var presets=library==null?java.util.List.<com.lovenote.module.impl.MacroLibrary.Preset>of():library.presets();
        var guides=com.lovenote.module.MacroGuideCatalog.ALL;
        String q=macroGuideQuery.toLowerCase(java.util.Locale.ROOT).trim();
        java.util.List<Integer> indices=new java.util.ArrayList<>();
        int count=macroLibraryMode?presets.size():guides.size();
        for(int i=0;i<count;i++) {
            String name=macroLibraryMode?presets.get(i).name():guides.get(i).name();
            String group=macroLibraryMode?presets.get(i).group():guides.get(i).group();
            if(q.isEmpty()||name.toLowerCase(java.util.Locale.ROOT).contains(q)
                    ||group.toLowerCase(java.util.Locale.ROOT).contains(q))indices.add(i);
        }
        int rows=Math.max(1,(paneH-13)/rowH);
        macroGuideScroll=Math.max(0,Math.min(Math.max(0,indices.size()-rows),macroGuideScroll));

        for(int row=macroGuideScroll;row<indices.size()&&row<macroGuideScroll+rows;row++) {
            int i=indices.get(row),yy=paneY+7+(row-macroGuideScroll)*rowH;
            boolean selected=macroLibraryMode?i==macroTemplateSelected:i==macroGuideSelected;
            if(selected) {
                DrainerSkin.control(g,paneX+5,yy-1,paneW-10,rowH-3,true);
                DrainerSkin.selectionMark(g,paneX+7,yy-1,rowH-3);
            } else fill(g,paneX+12,yy+rowH-2,paneX+paneW-12,yy+rowH-1,0x22493661);
            String name=macroLibraryMode?presets.get(i).name():guides.get(i).name();
            while(name.length()>4&&font.width(name)>paneW-28)name=name.substring(0,name.length()-2);
            txt(g,name,paneX+13,yy+7,selected?DrainerSkin.TEXT:DrainerSkin.SUBTEXT);
            macroGuideHits.add(new int[]{paneX+5,yy-1,paneW-10,rowH-3,macroLibraryMode?-100-i:i});
        }

        int detailX=paneX+paneW+10,detailW=boxX+boxW-15-detailX;
        macroGlass(g,detailX,paneY,detailW,paneH,10,0xCB131020,0x4B503477);
        int tx=detailX+13,maxWidth=Math.max(50,detailW-26);
        macroDetailBottom=paneY+paneH-8;

        if(macroLibraryMode && !presets.isEmpty()) {
            macroTemplateSelected=Math.max(0,Math.min(presets.size()-1,macroTemplateSelected));
            var entry=presets.get(macroTemplateSelected);
            String name=entry.name();
            while(name.length()>5&&font.width(name)>maxWidth)name=name.substring(0,name.length()-2);
            txtS(g,name,tx,paneY+14,DrainerSkin.TEXT);
            txt(g,entry.saved()?"SAVED RECORDING":entry.group().toUpperCase(java.util.Locale.ROOT),
                tx,paneY+34,0xFFC79BDD);
            DrainerSkin.section(g,tx,paneY+48,maxWidth);
            macroDetail(g,entry.detail(),tx,paneY+58,maxWidth,3);
            txt(g,""+library.durationTicks(entry.name())+" ticks  /  "+library.selectedSlot(),
                tx,paneY+108,0xFFB59AC7);
            int loadY=paneY+paneH-99;
            int actionY=loadY-30;
            int half=(maxWidth-6)/2;
            macroGlass(g,tx,actionY,half,24,7,0xB74E295F,0x6FBB80D6);
            txt(g,"Play & close",tx+8,actionY+7,0xFFFFE8FA);
            macroGuideHits.add(new int[]{tx,actionY,half,24,-9});
            macroGlass(g,tx+half+6,actionY,maxWidth-half-6,24,7,0xB72B263D,0x6F9B7BAE);
            txt(g,"Record & close",tx+half+13,actionY+7,0xFFFFE8FA);
            macroGuideHits.add(new int[]{tx+half+6,actionY,maxWidth-half-6,24,-10});
            macroGlass(g,tx,loadY,maxWidth,24,8,0xBB603471,0x77D69DEE);
            txt(g,"Load into recorder",tx+10,loadY+7,0xFFFFE9FD);
            macroGuideHits.add(new int[]{tx,loadY,maxWidth,24,-5});
            // Type a name, then save the currently selected recorder slot.
            int nameY=loadY+27;
            macroGlass(g,tx,nameY,maxWidth,21,7,0xAF21192F,macroNameTyping?0xC3C68DEA:0x634E356A);
            String hint=macroSaveName.isEmpty()?"Name a recording...":macroSaveName;
            txt(g,hint,tx+8,nameY+7,macroSaveName.isEmpty()?0xFF9887AA:0xFFF6DCF8);
            macroGuideHits.add(new int[]{tx,nameY,maxWidth,21,-6});
            macroGlass(g,tx,nameY+23,maxWidth,20,7,0xA82C2045,0x756B4387);
            txt(g,"Save slot as preset",tx+9,nameY+29,0xFFFFE6FB);
            macroGuideHits.add(new int[]{tx,nameY+23,maxWidth,20,-7});
            if(entry.saved()) {
                txt(g,"Delete saved",tx,nameY+50,0xFFFFA9C1);
                macroGuideHits.add(new int[]{tx,nameY+47,maxWidth,17,-8});
            }
            if(!macroFeedback.isEmpty())txt(g,macroFeedback,boxX+17,boxY+boxH-10,DrainerSkin.SUBTEXT);
        }else if(!guides.isEmpty()) {
            var entry=guides.get(Math.max(0,Math.min(guides.size()-1,macroGuideSelected)));
            String name=entry.name();
            while(name.length()>5&&font.width(name)>maxWidth)name=name.substring(0,name.length()-2);
            txtS(g,name,tx,paneY+13,DrainerSkin.TEXT);
            txt(g,entry.group().toUpperCase(java.util.Locale.ROOT),tx,paneY+33,DrainerSkin.accentText());
            DrainerSkin.section(g,tx,paneY+49,maxWidth);
            int yy=paneY+58;
            yy=macroTextBlock(g,"WHAT IT DOES",entry.what(),tx,yy,maxWidth);
            yy=macroTextBlock(g,"HOW IT WORKS",entry.how(),tx,yy,maxWidth);
            if(yy<paneY+paneH-22)macroDetail(g,entry.usage(),tx,yy,maxWidth,
                Math.max(1,(paneY+paneH-yy-10)/12));
        }

        pose.popMatrix();
    }

    private void skyBlockGuideDraw(GuiGraphicsExtractor g,int boxX,int boxY,int boxW,int boxH,int paneX,int paneY,int paneW,int paneH){
        var coach=com.lovenote.Lovenote.modules==null?null:com.lovenote.Lovenote.modules.get(com.lovenote.module.impl.SkyBlockCoach.class);
        if(coach==null)return;
        String[] tracks=MACRO_SKYBLOCK_TRACKS;
        for(int i=0;i<tracks.length;i++){
            int yy=paneY+7+i*34;
            boolean active=coach.track().equals(tracks[i]);
            macroGlass(g,paneX+7,yy,paneW-14,29,6,active?0xB04F2A64:0x8830203E,active?0xA0AB80CD:0x3D55426F);
            if (active) DrainerSkin.selectionMark(g,paneX+9,yy,29);
            txt(g,tracks[i],paneX+17,yy+9,active?0xFFFFECFF:DrainerSkin.SUBTEXT);
            macroGuideHits.add(new int[]{paneX+7,yy,paneW-14,29,-50-i});
        }
        int detailX=paneX+paneW+10,detailW=boxX+boxW-15-detailX;
        macroGlass(g,detailX,paneY,detailW,paneH,8,0xD3171124,0x4B503477);
        int tx=detailX+11, mw=detailW-22;
        var goal=coach.current();
        txt(g,"OBJECTIVE  "+coach.completedTrack()+" / "+coach.trackCount(),tx,paneY+11,DrainerSkin.accentText());
        macroDetail(g,goal.title(),tx,paneY+31,mw,2);
        txt(g,"WHAT TO DO",tx,paneY+61,DrainerSkin.accentText());
        macroDetail(g,goal.action(),tx,paneY+76,mw,4);
        txt(g,"WHY THIS HELPS",tx,paneY+132,DrainerSkin.accentText());
        macroDetail(g,goal.reason(),tx,paneY+146,mw,3);
        int buttonsY=paneY+paneH-75;
        int third=Math.max(30,(mw-8)/3);
        String[] controls={"< Prev",coach.currentDone()?"Undo":"Mark done","Next >"};
        for(int i=0;i<3;i++){
            int bx=tx+i*(third+4);
            macroGlass(g,bx,buttonsY,third,23,6,0xA0452A60,0x5D865DA8);
            txt(g,controls[i],bx+4,buttonsY+8,0xFFFFE4F8);
            macroGuideHits.add(new int[]{bx,buttonsY,third,23,-70-i});
        }
        String profit=coach.bestMethod()+": "+String.format(java.util.Locale.ROOT,"%,.0f",coach.coinsPerHour())+" net coins/h";
        macroDetail(g,profit,tx,buttonsY+30,mw,2);
        txt(g,"Upgrade payback: "+coach.payback(),tx,buttonsY+47,DrainerSkin.MUTED);
        txt(g,"Enter 5m earnings + upgrade cost in SkyBlockCoach",boxX+17,boxY+boxH-10,DrainerSkin.MUTED);
    }
    private void schematicGuideDraw(GuiGraphicsExtractor g,int boxX,int boxY,int boxW,int boxH,int paneX,int paneY,int paneW,int paneH){
        var assistant=com.lovenote.Lovenote.modules==null?null:com.lovenote.Lovenote.modules.get(com.lovenote.module.impl.SchematicAssist.class);
        if(assistant==null)return;
        txt(g,"SCHEMATICS  /  LOCAL FILES",paneX+9,paneY+11,DrainerSkin.accentText());
        DrainerSkin.control(g,paneX+paneW-59,paneY+28,50,17,false);
        txt(g,"Refresh",paneX+paneW-54,paneY+33,DrainerSkin.SUBTEXT);
        macroGuideHits.add(new int[]{paneX+paneW-59,paneY+28,50,17,-84});
        int visible=Math.max(1,(paneH-78)/29);
        schematicScroll=Math.max(0,Math.min(schematicScroll,Math.max(0,schematicFiles.size()-visible)));
        for(int j=schematicScroll;j<schematicFiles.size()&&j<schematicScroll+visible;j++){
            int yy=paneY+51+(j-schematicScroll)*29;
            String file=schematicFiles.get(j);
            macroGlass(g,paneX+6,yy,paneW-12,26,6,j==schematicSelected?0xA64F2862:0x99301F3D,0x474D3A70);
            if (j==schematicSelected) DrainerSkin.selectionMark(g,paneX+8,yy,26);
            while(file.length()>8&&font.width(file)>paneW-25)file=file.substring(0,file.length()-2);
            txt(g,file,paneX+12,yy+8,DrainerSkin.TEXT);
            macroGuideHits.add(new int[]{paneX+6,yy,paneW-12,26,-1000-j});
        }
        txt(g,schematicFiles.size()+" FILES  /  SCROLL TO BROWSE",paneX+9,paneY+paneH-15,DrainerSkin.MUTED);
        int detailX=paneX+paneW+10,detailW=boxX+boxW-15-detailX;
        macroGlass(g,detailX,paneY,detailW,paneH,8,0xD3171124,0x4B503477);
        int tx=detailX+12,mw=detailW-24;
        txt(g,"SCHEMATIC ASSIST",tx,paneY+12,DrainerSkin.TEXT);
        String chosen=schematicFiles.isEmpty()?"No schematics found":schematicFiles.get(Math.min(schematicSelected,schematicFiles.size()-1));
        macroDetail(g,chosen,tx,paneY+32,mw,2);
        macroDetail(g,assistant.message(),tx,paneY+60,mw,3);
        String summary=assistant.loaded()?assistant.total()+" blocks   /   "+assistant.dimensions():"Supports .schem / .litematic / .nbt";
        macroDetail(g,summary,tx,paneY+105,mw,2);
        if(assistant.loaded()){
            if(!schematicMaterialsExpanded){
                macroDetail(g,assistant.topMaterials(),tx,paneY+122,mw,1);
                txt(g,"PREVIEW: "+(assistant.wireframeEnabled()?"3D ON":"3D OFF")+"  /  ROT "+assistant.rotation()+" / LAYER "+(assistant.layer()+1),tx,paneY+185,0xFFD4B5ED);
            } else {
                int count=Math.min(8,Math.max(0,assistant.materialTypes()-schematicMaterialOffset));
                for(int i=0;i<count;i++)
                    txt(g,assistant.materialLine(i+schematicMaterialOffset),tx,paneY+123+i*13,DrainerSkin.SUBTEXT);
            }
            if(!schematicMaterialsExpanded){
                txt(g,"BLOCK "+(assistant.currentIndex()+1)+" / "+assistant.total(),tx,paneY+143,0xFFCEA1E0);
                macroDetail(g,assistant.nextInstructions(),tx,paneY+156,mw,2);
            }
        }
        int by=paneY+paneH-57,half=(mw-6)/2;
        int barY=by-75;
        if(assistant.loaded()){
            macroGlass(g,tx,barY,mw,20,5,0xAA2C203C,0x76856E99);
            txt(g,schematicMaterialsExpanded?"MATERIALS LIST  /  PAGE "+(schematicMaterialOffset/8+1):"OPEN COMPLETE MATERIALS LIST",tx+7,barY+6,DrainerSkin.TEXT);
            macroGuideHits.add(new int[]{tx,barY,mw,20,-86});
            macroGlass(g,tx,barY+22,half,19,5,0x9C312343,0x608879A5);
            txt(g,schematicMaterialsExpanded?"Previous 8":"Preview 3D",tx+6,barY+28,0xFFEEDBF9);
            macroGuideHits.add(new int[]{tx,barY+22,half,19,schematicMaterialsExpanded?-87:-89});
            macroGlass(g,tx+half+6,barY+22,mw-half-6,19,5,0x9C312343,0x608879A5);
            txt(g,schematicMaterialsExpanded?"Next 8":"Rotate 90",tx+half+12,barY+28,0xFFEEDBF9);
            macroGuideHits.add(new int[]{tx+half+6,barY+22,mw-half-6,19,schematicMaterialsExpanded?-88:-90});
            if(!schematicMaterialsExpanded){
                txt(g,"Layers - / +  (use module settings for all layers)",tx,barY+52,0xFFBFA9D7);
                macroGuideHits.add(new int[]{tx,barY+47,half,17,-91});
                macroGuideHits.add(new int[]{tx+half,barY+47,half,17,-92});
            }
        }
        txt(g,"Copy to Litematica folder",tx,by-17,0xFFBC9FD8);
        macroGuideHits.add(new int[]{tx,by-23,mw,22,-85});
        int third=(mw-12)/3;
        macroGlass(g,tx,by,third,22,6,0x9C63367A,0x678966A4);
        txt(g,"Load",tx+8,by+7,DrainerSkin.TEXT);
        macroGuideHits.add(new int[]{tx,by,third,22,-80});
        macroGlass(g,tx+third+6,by,third,22,6,0x9C35274F,0x678966A4);
        txt(g,"Set anchor",tx+third+11,by+7,DrainerSkin.TEXT);
        macroGuideHits.add(new int[]{tx+third+6,by,third,22,-81});
        macroGlass(g,tx+2*third+12,by,mw-2*third-12,22,6,0x9C3D2438,0x678966A4);
        txt(g,"Unload",tx+2*third+19,by+7,0xFFFFD5E0);
        macroGuideHits.add(new int[]{tx+2*third+12,by,mw-2*third-12,22,-93});
        by+=25;
        macroGlass(g,tx,by,half,22,6,0x9C35274F,0x678966A4);
        txt(g,"< -"+assistant.stepSize(),tx+6,by+7,DrainerSkin.TEXT);
        macroGuideHits.add(new int[]{tx,by,half,22,-82});
        macroGlass(g,tx+half+6,by,mw-half-6,22,6,0x9C63367A,0x678966A4);
        txt(g,"+"+assistant.stepSize()+" >",tx+half+11,by+7,DrainerSkin.TEXT);
        macroGuideHits.add(new int[]{tx+half+6,by,mw-half-6,22,-83});
        // Refresh is in the sidebar header; no clipped footer action.
    }
    private void macroDetail(GuiGraphicsExtractor g,String value,int x,int y,int maxWidth,int maxLines) {
        StringBuilder line=new StringBuilder();
        int row=0;
        for(String word:value.split(" ")) {
            String candidate=line.isEmpty()?word:line+" "+word;
            if(font.width(candidate)>maxWidth && !line.isEmpty()) {
                txt(g,line.toString(),x,y+row*13,DrainerSkin.TEXT);
                if(++row>=maxLines)return;
                line=new StringBuilder(word);
            } else line=new StringBuilder(candidate);
        }
        if(!line.isEmpty() && row<maxLines)txt(g,line.toString(),x,y+row*13,DrainerSkin.TEXT);
    }
    private int macroTextBlock(GuiGraphicsExtractor g,String label,String value,int x,int y,int maxWidth) {
        if(y >= macroDetailBottom-14) return y;
        txt(g,label,x,y,DrainerSkin.accentText());y+=17;
        StringBuilder line=new StringBuilder();
        for(String word:value.split(" ")) {
            String candidate=line.isEmpty()?word:line+" "+word;
            if(!line.isEmpty() && font.width(candidate)>maxWidth) {
                if(y >= macroDetailBottom-12)return macroDetailBottom;
                txt(g,line.toString(),x,y,DrainerSkin.TEXT);y+=12;line=new StringBuilder(word);
            } else line=new StringBuilder(candidate);
        }
        if(!line.isEmpty() && y<macroDetailBottom-12){txt(g,line.toString(),x,y,DrainerSkin.TEXT);y+=12;}
        return y+17;
    }
    private boolean saveMacroFromUi() {
        if(com.lovenote.Lovenote.modules==null)return false;
        var library=com.lovenote.Lovenote.modules.get(com.lovenote.module.impl.MacroLibrary.class);
        if(library==null)return false;
        boolean saved=library.saveSelected(macroSaveName);
        macroFeedback=saved?"SAVED TO MY MACROS":"NAME EMPTY / SLOT EMPTY / BUSY";
        if(saved){
            var presets=library.presets();
            for(int i=0;i<presets.size();i++)if(presets.get(i).name().equals(macroSaveName.trim()))macroTemplateSelected=i;
            macroGuideScroll=Math.max(0,macroTemplateSelected-4);
            macroNameTyping=false;
            com.lovenote.config.LovenoteConfig.save(true);
        }
        return saved;
    }
    private boolean macroGuideClick(double x,double y) {
        float ms=macroScale();x/=ms;y/=ms;
        for(int[] h:macroGuideHits)if(x>=h[0]&&x<h[0]+h[2]&&y>=h[1]&&y<h[1]+h[3]) {
            int action=h[4];
            if(action==-1){macroGuideOpen=false;macroNameTyping=false;}
            else if(action==-22){
                var coach=com.lovenote.Lovenote.modules==null?null:
                    com.lovenote.Lovenote.modules.get(com.lovenote.module.impl.SkyBlockCoach.class);
                if(coach!=null){
                    if(!coach.enabled)coach.setEnabled(true);
                    com.lovenote.config.LovenoteConfig.save(true);
                    macroGuideOpen=false;
                    mc.gui.setScreen(new SkyBlockGuideScreen(this));
                }
                return true;
            }
            else if(action<=-20&&action>=-23){
                macroWorkspaceTab=-20-action;macroLibraryMode=macroWorkspaceTab==1;macroGuideScroll=0;
                if(macroWorkspaceTab==2){
                    var c=com.lovenote.Lovenote.modules==null?null:com.lovenote.Lovenote.modules.get(com.lovenote.module.impl.SkyBlockCoach.class);
                    if(c!=null&&!c.enabled)c.setEnabled(true);
                }
                macroFeedback="";macroNameTyping=false;macroGuideTyping=false;
                if(macroWorkspaceTab==3){
                    var assist=com.lovenote.Lovenote.modules==null?null:com.lovenote.Lovenote.modules.get(com.lovenote.module.impl.SchematicAssist.class);
                    schematicFiles=assist==null?java.util.List.of():assist.files();
                    schematicSelected=0;schematicScroll=0;
                }
            }
            else if(action<=-50&&action>=-55){
                var c=com.lovenote.Lovenote.modules==null?null:com.lovenote.Lovenote.modules.get(com.lovenote.module.impl.SkyBlockCoach.class);
                if(c!=null){c.setTrack(new String[]{"START","LEVELS","GARDEN","MINING","COMBAT","COINS"}[-50-action]);com.lovenote.config.LovenoteConfig.save(true);}
            }
            else if(action<=-70&&action>=-72){
                var c=com.lovenote.Lovenote.modules==null?null:com.lovenote.Lovenote.modules.get(com.lovenote.module.impl.SkyBlockCoach.class);
                if(c!=null){if(action==-70)c.advance(-1);else if(action==-71)c.mark(!c.currentDone());else c.advance(1);com.lovenote.config.LovenoteConfig.save(true);}
            }
            else if(action<=-80&&action>=-93){
                var assist=com.lovenote.Lovenote.modules==null?null:com.lovenote.Lovenote.modules.get(com.lovenote.module.impl.SchematicAssist.class);
                if(assist!=null){
                    if(action==-80&&!schematicFiles.isEmpty()){assist.load(schematicFiles.get(Math.min(schematicSelected,schematicFiles.size()-1)));schematicMaterialsExpanded=false;schematicMaterialOffset=0;}
                    else if(action==-81)assist.anchorToPlayer();
                    else if(action==-82)assist.step(-1);
                    else if(action==-83)assist.step(1);
                    else if(action==-84){schematicFiles=assist.files();schematicScroll=0;schematicSelected=0;}
                    else if(action==-85)assist.exportToLitematica();
                    else if(action==-86)schematicMaterialsExpanded=!schematicMaterialsExpanded;
                    else if(action==-87)schematicMaterialOffset=Math.max(0,schematicMaterialOffset-8);
                    else if(action==-88)schematicMaterialOffset=Math.min(Math.max(0,assist.materialTypes()-1),schematicMaterialOffset+8);
                    else if(action==-89)assist.togglePreview();
                    else if(action==-90)assist.rotate();
                    else if(action==-91)assist.moveLayer(-1);
                    else if(action==-92)assist.moveLayer(1);
                    else if(action==-93){assist.unload();schematicMaterialsExpanded=false;schematicMaterialOffset=0;}
                    com.lovenote.config.LovenoteConfig.save(true);
                }
            }
            else if(action<=-1000){schematicSelected=Math.max(0,Math.min(schematicFiles.size()-1,-1000-action));}
            else if(action==-2){macroLibraryMode=false;macroGuideScroll=0;macroFeedback="";macroNameTyping=false;}
            else if(action==-3){macroLibraryMode=true;macroGuideScroll=0;macroFeedback="";}
            else if(action==-4){macroGuideTyping=true;macroNameTyping=false;}
            else if(action==-5){
                var library=com.lovenote.Lovenote.modules==null?null:
                    com.lovenote.Lovenote.modules.get(com.lovenote.module.impl.MacroLibrary.class);
                if(library!=null){
                    var presets=library.presets();
                    macroFeedback=macroTemplateSelected<presets.size() &&
                        library.loadPreset(presets.get(macroTemplateSelected).name())?"LOADED TO SELECTED SLOT":"RECORDER BUSY";
                }else macroFeedback="LIBRARY UNAVAILABLE";
            } else if(action==-9 || action==-10){
                var library=com.lovenote.Lovenote.modules==null?null:
                    com.lovenote.Lovenote.modules.get(com.lovenote.module.impl.MacroLibrary.class);
                boolean started=false;
                if(library!=null){
                    if(action==-10)started=library.startRecording();
                    else {
                        var presets=library.presets();
                        if(macroTemplateSelected>=0&&macroTemplateSelected<presets.size())
                            started=library.playPreset(presets.get(macroTemplateSelected).name());
                    }
                }
                if(started){
                    macroGuideOpen=false;macroGuideTyping=false;macroNameTyping=false;
                    mc.gui.setScreen(null); // Leave GUI immediately; recorder stops on any open screen.
                }else macroFeedback="NEED A WORLD / RECORDER BUSY";
            } else if(action==-6){macroNameTyping=true;macroGuideTyping=false;}
            else if(action==-7)saveMacroFromUi();
            else if(action==-8){
                var library=com.lovenote.Lovenote.modules==null?null:
                    com.lovenote.Lovenote.modules.get(com.lovenote.module.impl.MacroLibrary.class);
                if(library!=null){
                    var presets=library.presets();
                    if(macroTemplateSelected<presets.size() &&
                        library.deleteSaved(presets.get(macroTemplateSelected).name())){
                        macroTemplateSelected=Math.max(0,macroTemplateSelected-1);
                        macroFeedback="SAVED PRESET DELETED";
                        com.lovenote.config.LovenoteConfig.save(true);
                    }
                }
            } else if(action<=-100){macroTemplateSelected=-100-action;macroFeedback="";macroNameTyping=false;}
            else macroGuideSelected=action;
            return true;
        }
        macroGuideTyping=false;macroNameTyping=false;
        return true;
    }

    private void hudSetSlider(HudHit hit,double mx) {
        if(hit.setting instanceof com.lovenote.setting.NumberSetting n) {
            double t=Math.max(0,Math.min(1,(mx-hit.x)/(double)Math.max(1,hit.w)));
            n.set(n.min+t*(n.max-n.min));
        }
    }
    private boolean hudClick(double mx,double my,int button) {
        // Right-click to hide, on either the dock name or the actual widget itself.
        // Hidden widgets remain in the dock and can be restored with left-click.
        if (button == 1) {
            for (int i=hudHits.size()-1;i>=0;i--) {
                HudHit hit=hudHits.get(i);
                if (!hit.contains(mx,my) || hit.module==null || hit.action!=1) continue;
                if(hit.module.enabled)hit.module.setEnabled(false);
                if(hudSelected==hit.module){hudSelected=null;hudDragging=null;hudInspectorOpen=false;}
                return true;
            }
            java.util.List<com.lovenote.module.Module> all=hudModules();
            for(int i=all.size()-1;i>=0;i--) {
                com.lovenote.module.Module m=all.get(i);
                if(!m.enabled||hudNumber(m,"X")==null||hudNumber(m,"Y")==null)continue;
                int bx=hudWidgetX(m),by=hudWidgetY(m);
                double z=com.lovenote.util.HudScale.get(m);
                int ww=Math.max(1,(int)Math.ceil(hudWidgetW(m)*z));
                int hh=Math.max(1,(int)Math.ceil(hudWidgetH(m)*z));
                if(mx>=bx&&mx<bx+ww&&my>=by&&my<by+hh){
                    m.setEnabled(false);
                    if(hudSelected==m){hudSelected=null;hudDragging=null;hudInspectorOpen=false;}
                    return true;
                }
            }
            return true;
        }
        if(button!=0)return true;
        for(int i=hudHits.size()-1;i>=0;i--){
            HudHit hit=hudHits.get(i);if(!hit.contains(mx,my))continue;
            switch(hit.action) {
                case 1 -> {
                    // Clicking a widget shows/selects it; clicking again opens compact settings.
                    boolean again=hudSelected==hit.module;
                    if(!hit.module.enabled)hit.module.toggle();
                    hudSelected=hit.module;hudSettingsScroll=0;
                    hudInspectorOpen=again?!hudInspectorOpen:false;
                }
                case 2 -> {hit.module.toggle();hudSelected=hit.module;hudSettingsScroll=0;}
                case 3 -> {hudSetSlider(hit,mx);hudSlider=(com.lovenote.setting.NumberSetting)hit.setting;}
                case 4 -> {if(hit.setting instanceof com.lovenote.setting.BoolSetting b)b.toggle();}
                case 5 -> hudMoveModule(hit.module,width*.48,height*.42);
                case 6 -> {drainerHudEditor=false;hudDragging=null;hudSlider=null;}
                case 7 -> com.lovenote.spotify.SpotifyManager.I.previous();
                case 8 -> com.lovenote.spotify.SpotifyManager.I.togglePlay();
                case 9 -> com.lovenote.spotify.SpotifyManager.I.next();
                case 10 -> {if(hit.setting instanceof com.lovenote.setting.ModeSetting ms && ms.modes.length>0)ms.setIndex((ms.index()+1)%ms.modes.length);}
                case 11 -> {if(hit.setting instanceof com.lovenote.setting.NumberSetting n)n.set(Math.max(n.min,n.get()-250));}
                case 12 -> {if(hit.setting instanceof com.lovenote.setting.NumberSetting n)n.set(Math.min(n.max,n.get()+250));}
                case 13 -> {if(hit.setting instanceof com.lovenote.setting.NumberSetting n)n.set(0);}
                case 14 -> {com.lovenote.util.HudScale.set(hit.module,
                    .30+Math.max(0,Math.min(1,(mx-hit.x)/Math.max(1,hit.w)))*2.20);
                    hudResizeStartX=mx;
                    hudResizeStartScale=com.lovenote.util.HudScale.get(hit.module);
                    hudResizing=true;}
                case 16 -> {hudInspectorOpen=false;hudSettingsScroll=0;}
                case 15 -> {hudSelected=hit.module;hudResizing=true;
                    hudResizeStartX=mx;hudResizeStartScale=com.lovenote.util.HudScale.get(hit.module);}
            }
            return true;
        }
        java.util.List<com.lovenote.module.Module> mods=hudModules();
        for(int i=mods.size()-1;i>=0;i--) {
            com.lovenote.module.Module m=mods.get(i);
            if(!m.enabled||hudNumber(m,"X")==null||hudNumber(m,"Y")==null)continue;
            int bx=hudWidgetX(m),by=hudWidgetY(m);
            double z=com.lovenote.util.HudScale.get(m);
            int ww=Math.max(1,(int)Math.ceil(hudWidgetW(m)*z));
            int hh=Math.max(1,(int)Math.ceil(hudWidgetH(m)*z));
            if(mx>=bx&&mx<bx+ww&&my>=by&&my<by+hh) {
                hudSelected=m;hudSettingsScroll=0;hudInspectorOpen=false;hudDragging=m;
                if(m instanceof com.lovenote.module.impl.SpotifyHud sp) sp.editorBeginDrag(mx,my);
                else if(m instanceof com.lovenote.module.impl.Lyrics ly) ly.editorBeginDrag(mx,my);
                else {hudDragOffsetX=mx-bx;hudDragOffsetY=my-by;}
                return true;
            }
        }
        return true;
    }
    private double hudDragOffsetX,hudDragOffsetY;
    private boolean hudDrag(double mx,double my) {
        if(hudResizing && hudSelected!=null) {
            // Slider and corner both use a stable initial scale while dragging.
            double change=(mx-hudResizeStartX)/Math.max(70.0,hudWidgetW(hudSelected));
            com.lovenote.util.HudScale.set(hudSelected,hudResizeStartScale+change);
            return true;
        }
        if(hudSlider!=null){
            for(HudHit hit:hudHits)if(hit.setting==hudSlider){hudSetSlider(hit,mx);break;}
            return true;
        }
        if(hudDragging!=null) {
            if(hudDragging instanceof com.lovenote.module.impl.SpotifyHud sp)sp.editorMove(mx,my);
            else if(hudDragging instanceof com.lovenote.module.impl.Lyrics ly)ly.editorMove(mx,my);
            else hudMoveModule(hudDragging,mx-hudDragOffsetX,my-hudDragOffsetY);
            return true;
        }
        return true;
    }
    private boolean configExpanded;
    private boolean profilesExpanded;
    private String profileName = "";
    private boolean profileTyping;
    private int profilePage;
    private String pendingDelete = "";

    public LovenoteModulesScreen(GuiTheme theme) {
        super(theme, Tabs.get().getFirst());
    }

    private static final ItemStack[] RAIL_ICONS = {
            new ItemStack(Items.NETHERITE_SWORD), new ItemStack(Items.BLAZE_POWDER), new ItemStack(Items.FEATHER), new ItemStack(Items.LEATHER_BOOTS),
            new ItemStack(Items.ENDER_EYE), new ItemStack(Items.SPYGLASS), new ItemStack(Items.PAPER),
            new ItemStack(Items.REDSTONE), new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.GOLDEN_APPLE),
            new ItemStack(Items.JUKEBOX), new ItemStack(Items.PAINTING), new ItemStack(Items.COMPARATOR)
    };

    private int pages() { return cats.size() + 3; }
    private boolean isSpecial(int p) { return p >= cats.size(); }
    private String pageLabel(int p) {
        if (p < cats.size()) return cats.get(p).label;
        return p == cats.size() ? "Spotify" : p == cats.size() + 1 ? "Appearance" : "Config";
    }

    private interface Row {
        String title();
        String description();
        int catIndex();
        boolean active();
        void toggle();
        int key();
        void setKey(int k);
        List<Group> groups();
    }

    private record Group(String name, List<Object> settings) {}

    private static final class LRow implements Row {
        final com.lovenote.module.Module m;
        final int cat;
        LRow(com.lovenote.module.Module m, int cat) { this.m = m; this.cat = cat; }
        public String title() { return m.name; }
        public String description() { return m.description; }
        public int catIndex() { return cat; }
        public boolean active() { return m.enabled; }
        public void toggle() { m.toggle(); }
        public int key() { return m.key; }
        public void setKey(int k) { m.key = k; }
        public List<Group> groups() {
            Map<String, List<Object>> by = new LinkedHashMap<>();
            for (com.lovenote.setting.Setting st : m.settings) {
                String g = st instanceof com.lovenote.setting.BoolSetting ? "Toggles"
                        : st instanceof com.lovenote.setting.NumberSetting ? "Values"
                        : st instanceof com.lovenote.setting.ModeSetting ? "Modes" : "Lists";
                by.computeIfAbsent(g, k -> new ArrayList<>()).add(st);
            }
            List<Group> out = new ArrayList<>();
            for (Map.Entry<String, List<Object>> e : by.entrySet()) out.add(new Group(e.getKey(), e.getValue()));
            return out;
        }
    }

    private static final class MRow implements Row {
        final Module m;
        final int cat;
        MRow(Module m, int cat) { this.m = m; this.cat = cat; }
        public String title() { return m.title; }
        public String description() { return m.description; }
        public int catIndex() { return cat; }
        public boolean active() { return m.isActive(); }
        public void toggle() { m.toggle(); }
        public int key() { return m.keybind.getValue(); }
        public void setKey(int k) { m.keybind.set(true, k, 0); }
        public List<Group> groups() {
            List<Group> out = new ArrayList<>();
            for (SettingGroup g : m.settings) {
                List<Object> list = new ArrayList<>();
                for (Setting<?> st : g) list.add(st);
                out.add(new Group(g.name, list));
            }
            return out;
        }
    }

    private int catIndex(com.lovenote.module.Category c) {
        for (int i = 0; i < cats.size(); i++) if (cats.get(i) == c) return i;
        return -1;
    }

    private static com.lovenote.module.Category mapCat(String name) {
        for (com.lovenote.module.Category c : com.lovenote.module.Category.values()) {
            if (c.label.equalsIgnoreCase(name) || c.name().equalsIgnoreCase(name)) return c;
        }
        return name.equalsIgnoreCase("world")
                ? com.lovenote.module.Category.RENDER
                : com.lovenote.module.Category.MISC;
    }

    // Stage 5: remove broken registry-only entries from the default menu.
    // Sources and registration are preserved; Config can reveal them for later ports.
    private static boolean showUnported = false;

    private int unportedCount() {
        int count = 0;
        if (com.lovenote.Lovenote.modules != null)
            for (com.lovenote.module.Module m : com.lovenote.Lovenote.modules.all())
                if (m instanceof com.lovenote.module.UnportedModule && !m.hidden) count++;
        return count;
    }

    private void buildRows() {
        visibleCacheQuery = null;
        visibleCache = List.of();
        allModules.clear();
        java.util.Set<String> exposed = new java.util.HashSet<>();
        java.util.Set<String> meteorNames = new java.util.HashSet<>();
        // The working Meteor version wins over an unported native stub, e.g. Flight.
        for (Module meteor : Modules.get().getAll())
            meteorNames.add(meteor.title.toLowerCase(java.util.Locale.ROOT));
        if (com.lovenote.Lovenote.modules != null) {
            for (com.lovenote.module.Module m : com.lovenote.Lovenote.modules.all()) {
                if (m.hidden) continue;
                String name = m.name.toLowerCase(java.util.Locale.ROOT);
                boolean unported = m instanceof com.lovenote.module.UnportedModule;
                if (unported && (!showUnported || meteorNames.contains(name))) continue;
                int i = m.category == com.lovenote.module.Category.SPOTIFY ? cats.size()
                        : RAGE_MODULES.contains(m.name) ? catIndex(com.lovenote.module.Category.RAGE)
                        : catIndex(m.category);
                if (i >= 0 && exposed.add(name)) allModules.add(new LRow(m, i));
            }
        }
        for (Module meteor : Modules.get().getAll()) {
            String name = meteor.title.toLowerCase(java.util.Locale.ROOT);
            if (exposed.contains(name)) continue;
            int i = catIndex(mapCat(meteor.category.name));
            if (i >= 0 && exposed.add(name)) allModules.add(new MRow(meteor, i));
        }
    }

    @Override
    public void initWidgets() {
        if (sessionStart == 0L) sessionStart = System.currentTimeMillis();

        cats.clear();
        for (com.lovenote.module.Category c : com.lovenote.module.Category.values()) {
            if (c == com.lovenote.module.Category.SPOTIFY || c == com.lovenote.module.Category.THEMES) continue;
            cats.add(c);
        }
        buildRows();

        readLayout();
        vw = Math.max(160, Math.round(width / gs));
        vh = Math.max(120, Math.round(height / gs));

        if (mainX == Integer.MIN_VALUE) { mainX = (vw - winW) / 2; mainY = (vh - winH) / 2; }
        if (panX == Integer.MIN_VALUE) {
            panX = mainX + winW + 8 + PW <= vw - 4 ? mainX + winW + 8 : mainX + winW - PW - 10;
            panY = mainY + 10;
        }
        clampWindows();
        lastNano = System.nanoTime();

        Random r = new Random();
        px = new float[140]; py = new float[140]; pvx = new float[140]; pvy = new float[140];
        for (int i = 0; i < px.length; i++) {
            px[i] = r.nextFloat() * width; py[i] = r.nextFloat() * height;
            pvx[i] = (r.nextFloat() - 0.5f) * 0.25f; pvy[i] = -0.08f - r.nextFloat() * 0.35f;
        }

    }

    private void onSearch() { scrollTarget = 0; scrollShown = 0; tabMs = 0; }

    private static Window windowHandle() { return mc.getWindow(); }

    private EditBox mkField(String hint, int max, String initial) {
        EditBox f = new EditBox(font, 0, 0, 100, 12, Component.literal(hint));
        f.setBordered(false);
        f.setMaxLength(max);
        f.setValue(initial);
        f.setFocused(false);
        return f;
    }

    private void readLayout() {
        // V24: honor saved theme and animation controls. Previously these were
        // reset EVERY frame, making Appearance settings silently ineffective.
        if (LovenoteMenuConfig.style == null || LovenoteMenuConfig.style.isBlank())
            LovenoteMenuConfig.style = "Moon";
        float desired = Mth.clamp(LovenoteMenuConfig.menuScale / 100f, 0.2f, 1.1f);
        float fitX = (width - 28f) / Math.max(1, LovenoteMenuConfig.windowWidth);
        float fitY = (height - 28f) / Math.max(1, LovenoteMenuConfig.windowHeight);
        gs = Math.max(0.1f, Math.min(desired, Math.min(fitX, fitY)));
        winW = Mth.clamp(LovenoteMenuConfig.windowWidth, 460, 900);
        winH = Mth.clamp(LovenoteMenuConfig.windowHeight, 280, 560);
        railW = Mth.clamp(LovenoteMenuConfig.railWidth, 132, 168);
        particleCount = Mth.clamp(LovenoteMenuConfig.particles, 0, 140);
        LovenoteMenuConfig.apply();
    }

    private int contentX() { return LovenoteMenuConfig.style.equals("Moon") ? wx + 20 : (LovenoteMenuConfig.railRight ? wx + 14 : wx + railW + 14); }
    // Moon workspace uses its entire width until a module opens a contextual inspector.
    private int premiumGap() { return 12; }
    private int premiumSettingsW() {
        if (LovenoteMenuConfig.style.equals("Moon")) return Math.min(302, Math.max(205, (int)(winW * 0.39f)));
        return Math.max(180, Math.min(252, (winW - railW - 42) * 40 / 100));
    }
    private int premiumSettingsX() { return wx + winW - (LovenoteMenuConfig.style.equals("Moon") ? 18 : 14) - premiumSettingsW(); }
    private int contentW() {
        if (LovenoteMenuConfig.style.equals("Moon")) return winW - 40;
        return Math.max(140, premiumSettingsX() - premiumGap() - contentX());
    }
    private int railX() { return LovenoteMenuConfig.railRight ? wx + winW - railW : wx; }

    private void clampWindows() {
        mainX = Mth.clamp(mainX, Math.min(0, vw - winW), Math.max(0, vw - winW));
        mainY = Mth.clamp(mainY, Math.min(0, vh - winH), Math.max(0, vh - winH));
        panX = Mth.clamp(panX, 0, Math.max(0, vw - PW));
        panY = Mth.clamp(panY, 0, Math.max(0, vh - PH));
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() { close(); }

    private void close() {
        if (finishClose) { mc.gui.setScreen(null); return; }
        if (!closing) { closing = true; closeMs = 0; }
    }

    // ---------------------------------------------------------------- helpers

    private int a(int c) { return ((int) (((c >>> 24) * ga)) << 24) | (c & 0xFFFFFF); }
    private void fill(GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2, int c) { ctx.fill(x1, y1, x2, y2, a(c)); }
    private void rr(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int c) { LovenoteDraw.rounded(ctx, x, y, w, h, 0, a(c)); }
    private void rr(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int r, int c) { LovenoteDraw.rounded(ctx, x, y, w, h, r, a(c)); }
    private void grad(GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2, int c1, int c2) { LovenoteDraw.gradientH(ctx, x1, y1, x2, y2, a(c1), a(c2)); }
    private void txt(GuiGraphicsExtractor ctx, String s, int x, int y, int c) {
        int cc = a(c); if ((cc >>> 24) < 12) return;
        if (!com.lovenote.util.ModernText.draw(ctx,s,x,y,cc)) ctx.text(font, s, x, y, cc, false);
    }
    private void txtS(GuiGraphicsExtractor ctx, String s, int x, int y, int c) {
        int cc = a(c); if ((cc >>> 24) < 12) return;
        if (!com.lovenote.util.ModernText.draw(ctx,s,x,y,cc)) ctx.text(font, s, x, y, cc, true);
    }
    // Scale only headline text; all existing hitboxes stay in logical UI coordinates.
    private void txtScaled(GuiGraphicsExtractor ctx, String s, int x, int y, float size, int color) {
        Matrix3x2fStack matrix = ctx.pose();
        matrix.pushMatrix();
        matrix.translate(x, y);
        matrix.scale(size, size);
        try { txtS(ctx, s, 0, 0, color); }
        finally { matrix.popMatrix(); }
    }
    private void circle(GuiGraphicsExtractor ctx, int cx, int cy, int r, int c) { LovenoteDraw.circle(ctx, cx, cy, r, a(c)); }

    private float ease(String key, float target, float durMs) {
        // Hover states start unlit, allowing the initial mouse entry to animate.
        float v = anim.getOrDefault(key, key.startsWith("v24-hover:") ? 0f : target);
        // Prior hover animation lasted only ~25% of its declared duration,
        // effectively snapping at 60-144 FPS. Keep hover glass motion visible.
        float effectiveMs = key.startsWith("v24-hover:") ? Math.max(160f, durMs)
                : Math.max(16f, durMs * 0.42f);
        v += (target - v) * (1f - (float) Math.exp(-Math.max(0f, dt) / effectiveMs));
        if (Math.abs(target - v) < 0.004f) v = target;
        anim.put(key, v);
        return v;
    }

    private void addHit(int x, int y, int w, int h, int kind, Row m, Object s, int idx) {
        Hit hit = new Hit();
        hit.x = x; hit.y = y; hit.w = w; hit.h = h; hit.kind = kind; hit.module = m; hit.setting = s; hit.idx = idx;
        hits.add(hit);
    }

    private String keyName(int key) {
        if (key == -1) return "none";
        String n = InputConstants.getKey(new KeyEvent(key, 0, 0)).getDisplayName().getString().toUpperCase();
        return n.length() > 7 ? n.substring(0, 7) : n;
    }

    private void scrollbar(GuiGraphicsExtractor ctx, int x, int top, int bottom, float shown, int max) {
        if (max <= 0) return;
        int trackH = bottom - top;
        int barH = Math.max(18, (int) (trackH * (trackH / (float) (trackH + max))));
        int barY = top + (int) ((trackH - barH) * (shown / max));
        rr(ctx, x, top, 3, trackH, 2, LovenotePalette.alpha(LovenotePalette.track(), (int) (120 * sbAlpha)));
        rr(ctx, x, barY, 3, barH, 2, LovenotePalette.alpha(LovenotePalette.wave(0.1f), (int) (255 * sbAlpha)));
    }

    private float stagger(int index) {
        if (!LovenoteMenuConfig.stagger) return 1f;
        return Mth.clamp((tabMs - index * LovenoteMenuConfig.staggerSpeed) / 170f, 0f, 1f);
    }

    // ---------------------------------------------------------------- settings rows

    private void drawSwitch(GuiGraphicsExtractor ctx, int x, int y, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int accent = LovenotePalette.C2;
        // Keep a smooth state transition without stacking two large rounded tracks.
        rr(ctx, x, y, 24, 11, 6,
                LovenotePalette.lerp(0xBD232A3A, LovenotePalette.alpha(accent, 165), t));
        rr(ctx, x + 2 + Math.round(t * 12), y + 2, 7, 7, 4, 0xFFF7F5FF);
    }

    private int chipsHeight(String[] names, int w) {
        int cx = 0, rows = 1;
        for (String n : names) {
            int cw = font.width(n) + 12;
            if (cx + cw > w && cx > 0) { rows++; cx = 0; }
            cx += cw + 4;
        }
        return rows * 15 + (rows - 1) * 3;
    }

    private int rowH(Object s, int w) {
        if (s instanceof Setting<?> st && !st.isVisible()) return 0;
        if (s instanceof ColorSetting) return expandedColor == s ? 164 : 23;
        if (s instanceof com.lovenote.setting.BoolSetting) return 20;
        if (s instanceof com.lovenote.setting.NumberSetting) return 29;
        if (s instanceof com.lovenote.setting.ModeSetting e) return 14 + chipsHeight(e.modes, w) + 7;
        if (s instanceof com.lovenote.setting.MultiSetting m) return 14 + chipsHeight(m.names, w) + 7;
        if (s instanceof Setting<?> ss) {
            if (ss instanceof BoolSetting || ss instanceof KeybindSetting) return 20;
            if (ss instanceof IntSetting || ss instanceof DoubleSetting) return 29;
            if (ss instanceof EnumSetting<?> e) return 14 + chipsHeight(enumNames(e), w) + 7;
        }
        return 0;
    }

    private static Object[] enumValues(EnumSetting<?> e) {
        Object cur = e.get();
        return cur == null ? new Object[0] : cur.getClass().getEnumConstants();
    }

    private String[] enumNames(EnumSetting<?> e) {
        Object[] v = enumValues(e);
        String[] out = new String[v.length];
        for (int i = 0; i < v.length; i++) out[i] = String.valueOf(v[i]);
        return out;
    }

    @SuppressWarnings("unchecked")
    private static void setEnum(EnumSetting<?> e, Object v) { ((Setting<Object>) e).set(v); }

    private int drawSetting(GuiGraphicsExtractor ctx, Row owner, Object s, int x, int y, int w) {
        if (s instanceof com.lovenote.setting.BoolSetting b) {
            float t = ease(b.hashCode() + "b", b.get() ? 1f : 0f, 150);
            txt(ctx, b.name, x, y + 4, LovenotePalette.lerp(LovenotePalette.dim(), LovenotePalette.text(), t));
            drawSwitch(ctx, x + w - 24, y + 4, t);
            addHit(x - 2, y, w + 4, 18, K_BOOL, owner, b, 0);
            return 20;
        }
        if (s instanceof com.lovenote.setting.NumberSetting n) {
            txt(ctx, n.name, x, y + 2, LovenotePalette.dim());
            String v = n.step >= 1 ? String.valueOf(n.getI()) : String.format("%.2f", n.get());
            txt(ctx, v, x + w - font.width(v), y + 2, LovenotePalette.faint());
            drawSlider(ctx, x, y, w, (float) ((n.get() - n.min) / (n.max - n.min)));
            addHit(x, y + 11, w, 13, K_SLIDER, owner, n, 0);
            return 29;
        }
        if (s instanceof com.lovenote.setting.ModeSetting e) {
            txt(ctx, e.name, x, y + 2, LovenotePalette.dim());
            chips(ctx, e.modes, e.index(), x, y + 14, w, K_MODE, owner, e);
            return 14 + chipsHeight(e.modes, w) + 7;
        }
        if (s instanceof com.lovenote.setting.MultiSetting ms) {
            txt(ctx, ms.name, x, y + 2, LovenotePalette.dim());
            chipsMulti(ctx, ms, x, y + 14, w, owner);
            return 14 + chipsHeight(ms.names, w) + 7;
        }
        if (!(s instanceof Setting<?> st)) return 0;
        Setting<?> s2 = st;
        if (!s2.isVisible()) return 0;
        if (s instanceof ColorSetting colorSetting) {
            return drawColorSetting(ctx, owner, colorSetting, x, y, w);
        }
        if (s instanceof BoolSetting b) {
            float t = ease(b.hashCode() + "b", b.get() ? 1f : 0f, 150);
            txt(ctx, b.name, x, y + 4, LovenotePalette.lerp(LovenotePalette.dim(), LovenotePalette.text(), t));
            drawSwitch(ctx, x + w - 24, y + 4, t);
            addHit(x - 2, y, w + 4, 18, K_BOOL, owner, b, 0);
        } else if (s instanceof IntSetting n) {
            txt(ctx, n.name, x, y + 2, LovenotePalette.dim());
            txt(ctx, String.valueOf(n.get()), x + w - font.width(String.valueOf(n.get())), y + 2, LovenotePalette.faint());
            float t = (float) ((n.get() - n.sliderMin) / (double) (n.sliderMax - n.sliderMin));
            drawSlider(ctx, x, y, w, t);
            addHit(x, y + 11, w, 13, K_SLIDER, owner, n, 0);
        } else if (s instanceof DoubleSetting n) {
            txt(ctx, n.name, x, y + 2, LovenotePalette.dim());
            String val = String.format("%.2f", n.get());
            txt(ctx, val, x + w - font.width(val), y + 2, LovenotePalette.faint());
            float t = (float) ((n.get() - n.sliderMin) / (n.sliderMax - n.sliderMin));
            drawSlider(ctx, x, y, w, t);
            addHit(x, y + 11, w, 13, K_SLIDER, owner, n, 0);
        } else if (s instanceof EnumSetting<?> e) {
            txt(ctx, e.name, x, y + 2, LovenotePalette.dim());
            String[] names = enumNames(e);
            int active = 0;
            Object[] vals = enumValues(e);
            for (int i = 0; i < vals.length; i++) if (vals[i] == e.get()) active = i;
            chips(ctx, names, active, x, y + 14, w, K_MODE, owner, e);
        } else if (s instanceof KeybindSetting k) {
            txt(ctx, k.name, x, y + 4, LovenotePalette.dim());
            String kn = listening == owner ? "..." : keyName(k.get() == null ? -1 : k.get().getValue());
            int kw = font.width(kn) + 16;
            rr(ctx, x + w - kw, y + 2, kw, 16, 8, LovenotePalette.chip());
            txt(ctx, kn, x + w - kw + 8, y + 6, LovenotePalette.text());
            addHit(x + w - kw, y + 2, kw, 16, K_BIND, owner, k, 0);
        }
        return rowH(s, w);
    }

    // Shared color picker for ALL Meteor ColorSettings (Ambience, ESP, particle effects, etc.).
    // Only one expands at once; no permanent large popover is rendered.
    private static String colorHex(meteordevelopment.meteorclient.utils.render.color.SettingColor color) {
        return String.format("#%02X%02X%02X", color.r, color.g, color.b);
    }

    private static int colorARGB(meteordevelopment.meteorclient.utils.render.color.SettingColor color) {
        return ((color.a & 255) << 24) | ((color.r & 255) << 16) | ((color.g & 255) << 8) | (color.b & 255);
    }

    private meteordevelopment.meteorclient.utils.render.color.SettingColor shownColor(ColorSetting setting) {
        var color = setting.get();
        if (dragWin != 4 || dragging == null || dragging.setting != setting) return color;
        var preview = new meteordevelopment.meteorclient.utils.render.color.SettingColor(color);
        switch (dragging.idx) {
            case 0 -> preview.r = colorDragValue;
            case 1 -> preview.g = colorDragValue;
            case 2 -> preview.b = colorDragValue;
            case 3 -> preview.a = colorDragValue;
            default -> { }
        }
        return preview;
    }

    private int drawColorSetting(GuiGraphicsExtractor ctx, Row owner, ColorSetting setting, int x, int y, int w) {
        var color = shownColor(setting);
        boolean open = expandedColor == setting;
        // A compact glassy swatch; click anywhere on the row to open RGBA and hex editing.
        String label = setting.title;
        txt(ctx, trim(label, Math.max(40, w - 109)), x, y + 6, LovenotePalette.text());
        txt(ctx, colorHex(color), x + w - 91, y + 6, LovenotePalette.faint());
        rr(ctx, x + w - 20, y + 2, 20, 17, 5, LovenotePalette.alpha(LovenotePalette.C2, open ? 195 : 75));
        rr(ctx, x + w - 18, y + 4, 16, 13, 4, 0xFF323044);
        rr(ctx, x + w - 18, y + 4, 16, 13, 4, colorARGB(color));
        addHit(x - 2, y, w + 4, 22, K_COLOR_OPEN, owner, setting, 0);
        if (!open) return 23;

        int top = y + 25;
        String[] channels = {"R", "G", "B", "A"};
        int[] values = {color.r, color.g, color.b, color.a};
        for (int i = 0; i < channels.length; i++) {
            int cy = top + i * 23;
            txt(ctx, channels[i], x + 3, cy + 4, LovenotePalette.dim());
            drawSlider(ctx, x + 25, cy - 8, Math.max(28, w - 75), values[i] / 255f);
            String val = String.valueOf(values[i]);
            txt(ctx, val, x + w - font.width(val), cy + 4, LovenotePalette.faint());
            addHit(x + 23, cy, Math.max(30, w - 71), 18, K_COLOR_CHANNEL, owner, setting, i);
        }
        int hexY = top + 94;
        txt(ctx, "HEX", x + 3, hexY + 4, LovenotePalette.dim());
        rr(ctx, x + 28, hexY, w - 29, 18, 5, LovenotePalette.chip());
        String display = hexEditing == setting ? "#" + hexInput + "_" : colorHex(color) + "  EDIT";
        txt(ctx, trim(display, Math.max(24, w - 43)), x + 36, hexY + 5, LovenotePalette.text());
        addHit(x + 28, hexY, w - 29, 18, K_COLOR_HEX, owner, setting, 0);

        int presetY = hexY + 22;
        int visiblePresetCount = w >= 190 ? COLOR_PRESETS.length : 3;
        for (int i = 0; i < visiblePresetCount; i++) {
            int bx = x + 3 + i * 21;
            rr(ctx, bx, presetY, 16, 16, 5, 0xFF343044);
            rr(ctx, bx + 1, presetY + 1, 14, 14, 4, COLOR_PRESETS[i]);
            addHit(bx, presetY, 17, 16, K_COLOR_PRESET, owner, setting, i);
        }
        int end = x + w;
        rr(ctx, end - 79, presetY, 45, 16, 5, color.rainbow ? LovenotePalette.C2 : LovenotePalette.chip());
        txt(ctx, "RAIN", end - 75, presetY + 4, LovenotePalette.text());
        addHit(end - 79, presetY, 45, 16, K_COLOR_RAINBOW, owner, setting, 0);
        rr(ctx, end - 30, presetY, 30, 16, 5, LovenotePalette.chip());
        txt(ctx, "RST", end - 26, presetY + 4, LovenotePalette.text());
        addHit(end - 30, presetY, 30, 16, K_COLOR_RESET, owner, setting, 0);
        return 164;
    }

    private void previewColorSlider(Hit hit, double mx) {
        double percent = Mth.clamp((mx - hit.x) / Math.max(1.0, hit.w), 0.0, 1.0);
        colorDragValue = Mth.clamp((int) Math.round(percent * 255), 0, 255);
    }

    // Commit once on mouse release. Grass/foliage/water settings rebuild world meshes,
    // so applying the value for every mouse-move would cause avoidable lag.
    private void commitColorSlider(Hit hit) {
        if (!(hit.setting instanceof ColorSetting st)) return;
        var c = st.get();
        var updated = new meteordevelopment.meteorclient.utils.render.color.SettingColor(c);
        switch (hit.idx) {
            case 0 -> updated.r = colorDragValue;
            case 1 -> updated.g = colorDragValue;
            case 2 -> updated.b = colorDragValue;
            case 3 -> updated.a = colorDragValue;
            default -> { return; }
        }
        updated.rainbow = false;
        st.set(updated); // Uses Meteor's normal onChanged + persistence path.
    }

    private void applyHexColor() {
        if (hexEditing == null) return;
        String raw = hexInput.startsWith("#") ? hexInput.substring(1) : hexInput;
        if (raw.length() != 6 && raw.length() != 8) return;
        try {
            long packed = Long.parseLong(raw, 16);
            var old = hexEditing.get();
            int r, g, b, a;
            if (raw.length() == 6) {
                r = (int) ((packed >> 16) & 255);
                g = (int) ((packed >> 8) & 255);
                b = (int) (packed & 255);
                a = old.a;
            } else {
                r = (int) ((packed >> 24) & 255);
                g = (int) ((packed >> 16) & 255);
                b = (int) ((packed >> 8) & 255);
                a = (int) (packed & 255);
            }
            hexEditing.set(new meteordevelopment.meteorclient.utils.render.color.SettingColor(r, g, b, a));
            hexEditing = null;
        } catch (NumberFormatException ignored) { /* Remain editing until valid. */ }
    }

    private void drawSlider(GuiGraphicsExtractor ctx, int x, int y, int w, float t) {
        if (w < 7) return;
        t = Mth.clamp(t, 0f, 1f);
        int sy = y + 16;
        int fx = x + Math.round((w - 1) * t);
        // Simple fill tracks greatly reduce emitted GUI quads while retaining a rounded thumb.
        fill(ctx, x, sy, x + w, sy + 4, 0xB52C3246);
        if (fx > x) fill(ctx, x, sy, fx, sy + 4, LovenotePalette.alpha(LovenotePalette.C2, 221));
        rr(ctx, Mth.clamp(fx - 3, x + 1, x + w - 7), sy - 2, 6, 8, 3, 0xFFF6F4FC);
    }

    private void chipsMulti(GuiGraphicsExtractor ctx, com.lovenote.setting.MultiSetting ms, int x, int y, int w, Row owner) {
        int cx = 0, cy = 0;
        for (int i = 0; i < ms.names.length; i++) {
            int cw = font.width(ms.names[i]) + 12;
            if (cx + cw > w && cx > 0) { cx = 0; cy += 18; }
            int rx = x + cx, ry = y + cy;
            float t = ease(ms.hashCode() + ":" + i, ms.get(i) ? 1f : 0f, 150);
            rr(ctx, rx, ry, cw, 15, 6,
                    LovenotePalette.lerp(0xBD1B2132, LovenotePalette.alpha(LovenotePalette.C2, 150), t));
            txt(ctx, ms.names[i], rx + 6, ry + 4, LovenotePalette.lerp(LovenotePalette.dim(), LovenotePalette.text(), t));
            addHit(rx, ry, cw, 15, K_MULTI, owner, ms, i);
            cx += cw + 4;
        }
    }

    private void chips(GuiGraphicsExtractor ctx, String[] names, int active, int x, int y, int w, int kind, Row owner, Object set) {
        int cx = 0, cy = 0;
        for (int i = 0; i < names.length; i++) {
            int cw = font.width(names[i]) + 12;
            if (cx + cw > w && cx > 0) { cx = 0; cy += 18; }
            int rx = x + cx, ry = y + cy;
            float t = ease(set.hashCode() + ":" + i, i == active ? 1f : 0f, 150);
            rr(ctx, rx, ry, cw, 15, 6,
                    LovenotePalette.lerp(0xBD1B2132, LovenotePalette.alpha(LovenotePalette.C2, 165), t));
            txt(ctx, names[i], rx + 6, ry + 4, LovenotePalette.lerp(LovenotePalette.dim(), LovenotePalette.text(), t));
            addHit(rx, ry, cw, 15, kind, owner, set, i);
            cx += cw + 4;
        }
    }

    private int panelTotal(Row m, int w) {
        int total = 0;
        for (Group g : m.groups()) {
            boolean any = false;
            int sum = 0;
            for (Object s : g.settings()) { int h = rowH(s, w); if (h > 0) { any = true; sum += h; } }
            if (any) total += 18 + sum;
        }
        return total;
    }

    // DRainer_STAGE3_PREMIUM_POLISH: compact cards and persistent sidebar labels.
    // ---------------------------------------------------------------- render

    @Override
    public void renderCustom(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        dt = Math.min(100f, (now - lastNano) / 1_000_000f);
        lastNano = now;
        if(drainerHudEditor){hudDraw(ctx,mouseX,mouseY,delta);return;}
        if(macroGuideOpen){macroGuideDraw(ctx);return;}

        readLayout();
        vw = Math.max(160, Math.round(width / gs));
        vh = Math.max(120, Math.round(height / gs));
        clampWindows();

        // renderCustom receives GUI-scaled mouse coordinates already. The scene
        // is then drawn with gs (our menu zoom), so only divide by gs.
        // Dividing by Window.getGuiScale() here a second time broke hover hit tests
        // whenever the game GUI scale was above 1, even though click events worked.
        int mx = (int) (mouseX / gs), my = (int) (mouseY / gs);

        hits.clear();
        pageRects.clear();
        tooltip = null;
        wx = mainX; wy = mainY;
        tabMs += dt;
        sbAlpha += (0.45f - sbAlpha) * 0.12f;

        float animMs = LovenoteMenuConfig.animSpeed;
        int corner = LovenoteMenuConfig.corner;

        float open;
        if (closing) {
            closeMs += dt;
            open = 1f - Math.min(1f, closeMs / Math.max(60f, animMs * 0.7f));
            if (closeMs >= animMs * 0.7f) { finishClose = true; close(); return; }
        } else {
            openMs += dt;
            open = Math.min(1f, openMs / Math.max(60f, animMs));
        }
        float eo = 1f - (1f - open) * (1f - open) * (1f - open);
        animating = open < 0.999f;
        ga = eo;

        ctx.fill(0, 0, width, height, LovenotePalette.alpha(0x000000,
                (int) ((LovenoteMenuConfig.style.equals("Moon") ? 90 : 130) * eo)));
        // Moon HUD: deliberate absence of ambient fog and menu confetti.
        for (int i = 0; i < particleCount && i < px.length; i++) {
            // Frame-rate independent particles: avoid faster animation on high FPS
            // and giant leaps after backgrounding the client.
            float sp = LovenoteMenuConfig.particleSpeed / 100f
                * Math.min(3f, Math.max(0f, dt / 16.6667f));
            px[i] += pvx[i] * sp; py[i] += pvy[i] * sp;
            if (py[i] < 0) { py[i] = height; px[i] = (float) (Math.random() * width); }
            if (px[i] < 0) px[i] = width;
            if (px[i] > width) px[i] = 0;
            int sz = 1 + (i % 2);
            ctx.fill((int) px[i], (int) py[i], (int) px[i] + sz, (int) py[i] + sz,
                    LovenotePalette.alpha(LovenotePalette.skeet(i / (float) Math.max(1, particleCount)), (int) (130 * eo)));
        }

        Matrix3x2fStack ms = ctx.pose();
        ms.pushMatrix();
        ms.scale(gs, gs);
        if (LovenoteMenuConfig.animation.equals("Tilt")) {
            float sc = 0.9f + 0.1f * eo;
            float cx = wx + winW / 2f, cy = wy + winH / 2f;
            ms.translate(cx, cy + (1f - eo) * 14f); ms.scale(sc, sc); ms.translate(-cx, -cy);
        } else if (LovenoteMenuConfig.animation.equals("Scale")) {
            float sc = 0.92f + 0.08f * eo;
            float cx = wx + winW / 2f, cy = wy + winH / 2f;
            ms.translate(cx, cy); ms.scale(sc, sc); ms.translate(-cx, -cy);
        }

        boolean interactive = !animating;
        if (selected != null) panelModule = selected;
        // The inspector slides only 12 logical pixels. Coordinates of controls and
        // hit regions move together so the animation does not break clicking.
        if (v26PreviousSelection != selected) {
            v26PreviousSelection = selected;
            v26DrawerProgress = selected == null ? 1f : 0f;
        } else if (selected != null) {
            v26DrawerProgress = Math.min(1f, v26DrawerProgress + Math.max(0f, dt) / 245f);
        }
        float panelA = ease("panel", selected != null ? 1f : 0f, 220);

        // The Moon layout is a different workspace, not a recoloured three-column skin.
        if (LovenoteMenuConfig.style.equals("Moon")) {
            drawMoonWorkspace(ctx, mx, my, interactive);
            if (selected != null && page != cats.size() + 2 && page != cats.size() + 1)
                drawPremiumSettings(ctx, mx, my, panelA, 6);
        } else {
            drawWindow(ctx, mx, my, interactive, corner);
            if (page != cats.size() + 2) drawPremiumSettings(ctx, mx, my, panelA, corner);
            drawRail(ctx, mx, my, corner);
            drawDrainerStage5Ornaments(ctx);
        }
        if (page == cats.size() + 2 && configExpanded) drawConfigExpanded(ctx);
        // No decorative ripples in the Moon replacement.

        if (listening != null) txtS(ctx, "press a key to bind " + listening.title() + "   (esc = clear)", wx + winW / 2 - 100, wy + winH + 8, 0xFFFFD27A);
        if (tooltip != null) {
            int tw = font.width(tooltip) + 10;
            rr(ctx, mx + 10, my + 12, tw, 16, 5, LovenotePalette.alpha(0x0A0A10, 240));
            txt(ctx, tooltip, mx + 15, my + 16, LovenotePalette.text());
        }
        ms.popMatrix();
    }

    // Drainer ClickGUI-only ambience. Drawn behind particles and window controls.
    private void drawDrainerAtmosphere(GuiGraphicsExtractor ctx, float openAlpha) {
        float time = (System.nanoTime() % 20000000000L) / 1_000_000_000f;
        float pulse = 0.5f + 0.5f * (float) Math.sin(time * 0.8f);
        int w = width;
        int h = height;

        // Restrained violet glow along the outer screen edges.
        for (int i = 0; i < 9; i++) {
            float fade = (1f - i / 9f) * (0.35f + pulse * 0.25f) * openAlpha;
            int alpha = Math.min(24, Math.max(0, (int) (fade * 30)));
            if (alpha == 0) continue;
            int col = LovenotePalette.alpha(LovenotePalette.C2, alpha);
            ctx.fill(i, 0, i + 1, h, col);
            ctx.fill(w - i - 1, 0, w - i, h, col);
            if (i < 5) {
                ctx.fill(0, i, w, i + 1, LovenotePalette.alpha(LovenotePalette.C1, alpha));
            }
        }

        // Tiny moving glints on the top perimeter, away from the logo/search area.
        for (int i = 0; i < 7; i++) {
            float phase = (time * 0.055f + i * 0.173f) % 1f;
            int x = (int) (phase * Math.max(1, w));
            int y = 5 + (i % 3) * 3;
            int alpha = (int) ((35 + 45 * pulse) * openAlpha);
            int c = LovenotePalette.alpha(LovenotePalette.C3, alpha);
            ctx.fill(x, y, x + 2, y + 3, c);
            ctx.fill(Math.max(0, x - 2), y + 1, x + 4, y + 2,
                    LovenotePalette.alpha(LovenotePalette.C2, alpha / 3));
        }
    }
    private void addRipple(int x, int y) {
        if (ripples.size() > 24) ripples.remove(0);
        Ripple r = new Ripple();
        r.x = x; r.y = y; r.t = System.currentTimeMillis();
        ripples.add(r);
    }

    private void drawRipples(GuiGraphicsExtractor ctx) {
        long now = System.currentTimeMillis();
        for (int i = ripples.size() - 1; i >= 0; i--) {
            Ripple r = ripples.get(i);
            float age = (now - r.t) / 420f;
            if (age >= 1f) { ripples.remove(i); continue; }
            int rad = 6 + (int) (age * 34);
            int alpha = (int) ((1f - age) * 110);
            for (int ang = 0; ang < 40; ang++) {
                double an = ang / 40.0 * Math.PI * 2.0;
                int x = r.x + (int) Math.round(Math.cos(an) * rad);
                int y = r.y + (int) Math.round(Math.sin(an) * rad);
                ctx.fill(x, y, x + 2, y + 2, LovenotePalette.alpha(LovenotePalette.wave(0f), alpha));
            }
        }
    }

    // DRAINER_STAGE5_THEME_LANTERNS: small outside-window Minecraft lanterns.
    // These are decorative: no hitboxes, resource dependencies or new keybinds.
    private float lanternAngle = 0f, lanternVelocity = 0f;
    private int lanternPreviousX, lanternPreviousY;
    private boolean lanternPositionReady = false;

    private void drawDrainerStage5Ornaments(GuiGraphicsExtractor ctx) {
        if (!LovenoteMenuConfig.style.equals("Premium")) {
            lanternPositionReady = false;
            return;
        }
        // Fixed-step-independent damped spring. Dragging the GUI imparts momentum.
        float seconds = Mth.clamp(dt / 1000f, 0.001f, 0.05f);
        if (!lanternPositionReady) {
            lanternPreviousX = wx;
            lanternPreviousY = wy;
            lanternPositionReady = true;
        }
        int dx = wx - lanternPreviousX;
        lanternPreviousX = wx;
        lanternPreviousY = wy;
        lanternVelocity += Mth.clamp(-dx * 0.014f, -0.14f, 0.14f);
        lanternVelocity += (-22f * lanternAngle - 8.5f * lanternVelocity) * seconds;
        lanternAngle = Mth.clamp(lanternAngle + lanternVelocity * seconds, -0.36f, 0.36f);
        float time = (System.nanoTime() % 60000000000L) / 1000000000f;
        float idle = (float) Math.sin(time * 0.85f) * 0.012f;
        drawThemeLantern(ctx, wx - 16, wy + 4, lanternAngle + idle, time);
        drawThemeLantern(ctx, wx + winW + 16, wy + 4, lanternAngle - idle, time + 1.6f);
    }

    private void drawThemeLantern(GuiGraphicsExtractor ctx, int x, int y, float angle, float time) {
        Matrix3x2fStack pose = ctx.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.rotate(angle);

        // Forged chain, hanging outside the frame.
        int iron = LovenotePalette.alpha(0x746C80, 240);
        int glint = LovenotePalette.alpha(0xB2A9BC, 195);
        int shadow = LovenotePalette.alpha(0x17131D, 255);
        for (int i = 0; i < 4; i++) {
            int yy = 2 + i * 5;
            fill(ctx, -2, yy, 3, yy + 1, iron);
            fill(ctx, -2, yy + 1, -1, yy + 4, glint);
            fill(ctx, 2, yy + 1, 3, yy + 4, iron);
            fill(ctx, -2, yy + 4, 3, yy + 5, iron);
        }

        // Small recognisable Minecraft-style lantern: dark metal cage and pixel glass.
        int cy = 25;
        int accent = LovenotePalette.C2; // live theme colour, updated by config/presets
        float flicker = 0.87f + 0.08f * (float) Math.sin(time * 3.7f)
                             + 0.05f * (float) Math.sin(time * 7.3f);
        int halo = (int) (21 * flicker);
        circle(ctx, 0, cy + 10, 23, LovenotePalette.alpha(accent, halo / 3));
        circle(ctx, 0, cy + 10, 16, LovenotePalette.alpha(accent, halo));
        circle(ctx, 0, cy + 10, 10, LovenotePalette.alpha(accent, (int)(35 * flicker)));

        fill(ctx, -4, cy - 3, 5, cy - 1, shadow);
        fill(ctx, -3, cy - 4, 4, cy - 3, iron);
        fill(ctx, -6, cy - 1, 7, cy + 2, iron);
        fill(ctx, -7, cy + 2, 8, cy + 17, shadow);
        fill(ctx, -5, cy + 3, 6, cy + 16, LovenotePalette.alpha(accent, 175));
        fill(ctx, -3, cy + 4, 4, cy + 14, LovenotePalette.alpha(accent, 235));
        fill(ctx, -1, cy + 6, 2, cy + 12, LovenotePalette.alpha(0xF2ECFF, 195));
        fill(ctx, -7, cy + 2, -5, cy + 17, iron);
        fill(ctx, 6, cy + 2, 8, cy + 17, iron);
        fill(ctx, -1, cy + 2, 1, cy + 17, LovenotePalette.alpha(0x26202D, 190));
        fill(ctx, -8, cy + 17, 9, cy + 20, iron);
        fill(ctx, -5, cy + 20, 6, cy + 22, shadow);
        fill(ctx, -3, cy + 22, 4, cy + 23, glint);
        pose.popMatrix();
    }

    // DRAINER_STAGE4_CHROME_FRAME: lightweight, time-based premium window trim.
    // This is decorative only and does not affect mouse hitboxes or layout.
    private void drawDrainerChromeFrame(GuiGraphicsExtractor ctx, int corner) {
        if (!LovenoteMenuConfig.style.equals("Premium")) return;

        final int x = wx, y = wy, w = winW, h = winH;
        if (w < 90 || h < 90) return;
        final float t = (System.nanoTime() % 30000000000L) / 1000000000f;
        final float breath = 0.5f + 0.5f * (float) Math.sin(t * 0.8f);
        final int edge = LovenotePalette.alpha(LovenotePalette.C2, 46 + (int) (16 * breath));
        final int silver = LovenotePalette.alpha(LovenotePalette.C3, 68);
        final int shadow = LovenotePalette.alpha(0x080510, 180);
        final int inset = Math.max(5, corner);

        // Double-line chrome border, inset so it doesn't overlap other screens.
        fill(ctx, x + inset, y + 2, x + w - inset, y + 3, silver);
        fill(ctx, x + inset, y + 3, x + w - inset, y + 4, shadow);
        fill(ctx, x + inset, y + h - 4, x + w - inset, y + h - 3, edge);
        fill(ctx, x + 2, y + inset, x + 3, y + h - inset, edge);
        fill(ctx, x + w - 3, y + inset, x + w - 2, y + h - inset, silver);

        // Restrained moving glint across the upper chrome line.
        int travel = Math.max(1, w - inset * 2 - 44);
        float sweep = (t * 0.055f) % 1f;
        int gx = x + inset + (int) (travel * sweep);
        for (int i = 0; i < 22; i++) {
            int fade = 7 + (int) (36 * (1f - Math.abs(i - 10.5f) / 11f));
            fill(ctx, gx + i, y + 2, gx + i + 1, y + 3,
                LovenotePalette.alpha(LovenotePalette.C3, fade));
        }

        // Hairline corner wear: sparse asymmetrical cuts, not an extra frame.
        int wear = LovenotePalette.alpha(LovenotePalette.C3, 53);
        fill(ctx, x + 9, y + 1, x + 23, y + 2, wear);
        fill(ctx, x + w - 27, y + 1, x + w - 13, y + 2, wear);
        fill(ctx, x + 1, y + 11, x + 2, y + 22, wear);
        fill(ctx, x + w - 2, y + h - 26, x + w - 1, y + h - 10, wear);

        // Small forged-metal corner accents, rather than rectangular chains.
        int len = Math.min(19, Math.min(w, h) / 8);
        int accent = LovenotePalette.alpha(LovenotePalette.C1, 70 + (int) (18 * breath));
        fill(ctx, x + inset, y + 5, x + inset + len, y + 6, accent);
        fill(ctx, x + 5, y + inset, x + 6, y + inset + len, accent);
        fill(ctx, x + w - inset - len, y + 5, x + w - inset, y + 6, accent);
        fill(ctx, x + w - 6, y + inset, x + w - 5, y + inset + len, accent);
        fill(ctx, x + inset, y + h - 6, x + inset + len, y + h - 5, accent);
        fill(ctx, x + 5, y + h - inset - len, x + 6, y + h - inset, accent);
        fill(ctx, x + w - inset - len, y + h - 6, x + w - inset, y + h - 5, accent);
        fill(ctx, x + w - 6, y + h - inset - len, x + w - 5, y + h - inset, accent);
    }

    // Premium Drainer x Moon ambient background: time-based and draw-only.
    // Tiny fixed-size shapes, no particle arrays or per-frame allocations.
    // Lightweight ambience: bounded, low-overdraw shapes. No per-card particles.
    private void drawMoonAtmosphere(GuiGraphicsExtractor ctx) {
        final int x = wx, y = wy, w = winW, h = winH;
        if (w < 100 || h < 100) return;
        final double t = System.nanoTime() * 1.0e-9;
        ctx.enableScissor(x + 2, y + 2, x + w - 2, y + h - 2);
        // 10 broad tonal bands instead of 42 per-frame full-width draw calls.
        for (int i = 0; i < 10; i++) {
            int yy = y + h * i / 10;
            int y2 = y + h * (i + 1) / 10;
            ctx.fill(x + 2, yy, x + w - 2, y2,
                    LovenotePalette.alpha(i < 5 ? 0x17132A : 0x24162E, 12));
        }
        // Two slowly drifting, subtle pools (four fills total).
        int gx = x + (int)(w * (0.74 + 0.035 * Math.sin(t * 0.15)));
        int gy = y + (int)(h * (0.30 + 0.035 * Math.cos(t * 0.14)));
        for (int i = 2; i >= 1; i--) {
            int rw = 55 * i, rh = 34 * i;
            ctx.fill(Math.max(x + 2, gx - rw), Math.max(y + 2, gy - rh),
                     Math.min(x + w - 2, gx + rw), Math.min(y + h - 2, gy + rh),
                     LovenotePalette.alpha(0x7F4CC8, 3));
        }
        // 10 tiny ambient points; all positions deterministic.
        for (int i = 0; i < 10; i++) {
            double u = (i * 0.61803398875) % 1.0;
            double v = ((i * 0.41421356237) + t * 0.002) % 1.0;
            int px = x + 9 + (int)(u * (w - 18));
            int py = y + 8 + (int)(v * (h - 16));
            ctx.fill(px, py, px + 1, py + 1,
                    LovenotePalette.alpha(0xC9ADE9, 18));
        }
        ctx.disableScissor();
    }

    // ---------------------------------------------------------------- Moon workspace V2
    // Horizontal navigation + two-column cards + settings drawer. No reserved sidebar
    // or blank third column. Existing module/event/setting handlers are reused.
    // Moon-inspired cockpit: stronger hierarchy and a permanent, compact navigation rail.
    // V24 BENTO CONTROL CENTER: responsive workspace and accurate hit regions.
    // Uses the existing module rows, search, settings panel and macro actions.
    // The renderer never adds or enables a module; it is deliberately UI-only.
    private void drawMoonWorkspace(GuiGraphicsExtractor ctx, int mx, int my, boolean interactive) {
        final int left = wx + 14, right = wx + winW - 14;
        final int ink = 0xFFF4F2FC, sub = 0xFFB2B7C9, muted = 0xFF838AA2;
        final int accent = LovenotePalette.C2;
        // Premium 2026 chassis. A single soft rim instead of stacked rectangles.
        rr(ctx, wx - 2, wy - 2, winW + 4, winH + 4, 19, 0x477B70AF);
        // Respect saved opacity without making text unreadable over bright terrain.
        int paneAlpha = Mth.clamp((int) (208f * LovenoteMenuConfig.opacity / 100f), 150, 230);
        rr(ctx, wx, wy, winW, winH, 17, LovenotePalette.alpha(DrainerSkin.WINDOW, paneAlpha));
        drawMoonAtmosphere(ctx);
        // Controlled highlight in the title area; no full-screen blur passes.
        fill(ctx, wx + 27, wy + 1, wx + winW - 27, wy + 2,
                LovenotePalette.alpha(LovenotePalette.C3, 39));
        fill(ctx, wx + 24, wy + 2, wx + winW - 24, wy + 3, LovenotePalette.alpha(accent, 67));
        // A thin sweeping accent in the title area; motion without a fullscreen shader.
        int headerSweepX = wx + 28 + (int)((winW - 65) *
                (0.5 + 0.5 * Math.sin(System.nanoTime() * 1e-9 * 0.21)));
        fill(ctx, headerSweepX, wy + 2, headerSweepX + 14, wy + 3,
                LovenotePalette.alpha(LovenotePalette.C3, 68));
        // A slow, narrow reflected highlight avoids constant animated redraw geometry.
        int sheenX = wx + 28 + (int) ((winW - 60) * (0.5 + 0.5 * Math.sin(System.nanoTime() * 1e-9 * 0.28)));
        fill(ctx, sheenX, wy + 2, sheenX + 18, wy + 3, 0x36FFFFFF);
        fill(ctx, left, wy + 67, right, wy + 68, 0x294E5E82);

        // Identity and state in a separated top bar.
        // A restrained identity mark, with one illuminated baseline.
        rr(ctx, left + 2, wy + 14, 39, 39, 12, 0x835D4782);
        rr(ctx, left + 3, wy + 15, 37, 37, 11, 0xBC17182C);
        txtS(ctx, "D", left + 16, wy + 27, 0xFFF4DDFF);
        fill(ctx, left + 10, wy + 51, left + 33, wy + 52, LovenotePalette.alpha(accent, 188));
        txtScaled(ctx, "DRAINER", left + 55, wy + 16, 1.40f, ink);
        txt(ctx, "WORKSPACE   /   MINECRAFT 26.2", left + 55, wy + 38, muted);

        final int closeX = right - 29;
        rr(ctx, closeX, wy + 19, 28, 27, 9, 0x752D3043);
        txt(ctx, "x", closeX + 10, wy + 29, 0xFFEADFF3);
        addHit(closeX, wy + 19, 28, 27, K_CLOSE, null, null, 0);
        final int activeCount = selectedCount();
        final String activeLabel = activeCount + " ENABLED";
        final int pillW = Math.max(79, font.width(activeLabel) + 24);
        final int pillX = closeX - pillW - 10;
        rr(ctx, pillX, wy + 20, pillW, 25, 11, 0x88352A51);
        float badgePulse = (float)(0.5 + 0.5 * Math.sin(System.nanoTime() * 1e-9 * 1.8));
        circle(ctx, pillX + 12, wy + 32, 4,
                LovenotePalette.alpha(accent, activeCount > 0 ? 24 + Math.round(18 * badgePulse) : 13));
        circle(ctx, pillX + 12, wy + 32, 2,
                activeCount > 0 ? LovenotePalette.alpha(LovenotePalette.C3, 195 + Math.round(44 * badgePulse)) : 0xFF63677B);
        txt(ctx, activeLabel, pillX + 21, wy + 28, 0xFFE0D1F5);

        // Permanent left navigation. Fixed spacing prevents overlap in compact mode.
        final int navX = left, navY = wy + 81, navW = Math.min(148, Math.max(124, winW / 6 + 10));
        final int navBottom = wy + winH - 63;
        rr(ctx, navX, navY, navW, Math.max(110, navBottom - navY), 16, 0x97131827);
        fill(ctx, navX + navW - 1, navY + 18, navX + navW, navBottom - 18, 0x225E6684);
        txtS(ctx, "WORKSPACES", navX + 13, navY + 13, 0xFF948FB3);
        int navTop = navY + 34;
        int navH = Math.max(18, Math.min(29, (navBottom - navTop - 7) / Math.max(1, pages())));
        // Selected tab glides between rows rather than changing instantly.
        float targetRail = navTop + page * navH;
        if (Float.isNaN(motionRailY)) motionRailY = targetRail;
        motionRailY += (targetRail - motionRailY) * (1f - (float)Math.exp(-Math.max(0f, dt) / 105f));
        if (Math.abs(targetRail - motionRailY) < 0.35f) motionRailY = targetRail;
        for (int i = 0; i < pages(); i++) {
            String label = pageLabel(i);
            int yy = navTop + i * navH;
            int rowHeight = Math.max(17, navH - 3);
            boolean active = i == page && searchText.trim().isEmpty();
            boolean hover = interactive && mx >= navX + 6 && mx < navX + navW - 6
                    && my >= yy && my < yy + rowHeight;
            float a = ease("v24-rail-" + i, active || hover ? 1f : 0f, 165);
            int alpha = Math.min(194, 7 + Math.round(140 * a));
            rr(ctx, navX + 6, yy, navW - 12, rowHeight, 10,
                    (Math.min(176, alpha) << 24) | 0x35294F);
            if (hover && !active) fill(ctx, navX + 14, yy + 2, navX + navW - 14, yy + 3, 0x1DFFFFFF);
            if (active) fill(ctx, navX + 18, yy + 1, navX + navW - 17, yy + 2, 0x20FFFFFF);
            // The sliding selector is drawn once below, above the nav rows.
            int glyphColor = active ? 0xFFE7D4FF : hover ? 0xFFD2C2E8 : 0xFF9799AC;
            // A quiet numeric index reads cleaner than chopped category initials.
            String glyph = i < 9 ? "0" + (i + 1) : Integer.toString(i + 1);
            txt(ctx, glyph, navX + 16, yy + Math.max(5, (rowHeight - 9) / 2), glyphColor);
            txt(ctx, trim(label, navW - 62), navX + 40,
                    yy + Math.max(5, (rowHeight - 9) / 2), active ? ink : hover ? sub : 0xFFC3C9D8);
            pageRects.add(new int[]{navX + 6, yy, navW - 12, rowHeight, i});
        }
        // Draw on top of the animated rows, but leave logical hitboxes fixed.
        if (searchText.trim().isEmpty()) {
            int ny = Math.round(motionRailY);
            int h = Math.max(7, navH - 13);
            int glowAlpha = 28 + (int)(9 * (0.5 + 0.5 * Math.sin(System.nanoTime() * 1e-9 * 1.5)));
            rr(ctx, navX + 6, ny + 3, 3, Math.max(12, navH - 7), 2,
                    LovenotePalette.alpha(accent, glowAlpha));
            rr(ctx, navX + 7, ny + 6, 2, h, 2, LovenotePalette.alpha(accent, 190));
        }

        final int cx = navX + navW + 15;
        final int contentRight = right;
        final int contentW = Math.max(120, contentRight - cx);
        final int headY = wy + 82;
        final String query = searchText.trim().toLowerCase(java.util.Locale.ROOT);
        final String title = query.isEmpty() ? pageLabel(page) : "Search results";
        final int count = visibleModules(query).size();
        // V25.9: layered command deck, not a large labelled rectangle.
        rr(ctx, cx, headY + 1, contentW, 59, 15, 0x641D2238);
        rr(ctx, cx + 1, headY + 2, contentW - 2, 57, 14, 0xB51A1F31);
        fill(ctx, cx + 18, headY + 3, cx + contentW - 18, headY + 4, 0x21FFFFFF);
        float barPulse = (float)(0.5 + 0.5 * Math.sin(System.nanoTime() * 8.0e-10));
        rr(ctx, cx + 14, headY + 14, 3, 30, 2,
                LovenotePalette.alpha(accent, 147 + Math.round(30 * barPulse)));
        txt(ctx, "MODULE LIBRARY  /  " + (query.isEmpty() ? "WORKSPACE" : "SEARCH"),
                cx + 27, headY + 10, 0xFF969EB7);
        txtScaled(ctx, trim(title.toUpperCase(java.util.Locale.ROOT),
                Math.max(50, contentW - Math.max(115, Math.min(226, contentW / 3)) - 84)),
                cx + 26, headY + 27, 1.23f, ink);
        String totalLabel = count + " MODS  /  " + activeCount + " ON";
        txt(ctx, totalLabel, cx + 27, headY + 47,
                LovenotePalette.alpha(LovenotePalette.C3, 176));

        final int searchW = Math.max(115, Math.min(226, contentW / 3));
        final int searchX = contentRight - searchW - 10;
        final int searchY = headY + 15;
        float searchGlow = ease("v254-search-focus", searchFocused ? 1f : 0f, 190);
        rr(ctx, searchX, searchY, searchW, 30, 11,
                LovenotePalette.lerp(0x42576288, LovenotePalette.alpha(accent, 175), searchGlow));
        rr(ctx, searchX + 1, searchY + 1, searchW - 2, 28, 10, 0xBA161D2C);
        fill(ctx, searchX + 10, searchY + 2, searchX + searchW - 10, searchY + 3, 0x16FFFFFF);
        String searchValue = searchText.isEmpty() && !searchFocused ? "Search modules..." : trim(searchText, searchW - 29);
        txt(ctx, searchValue, searchX + 11, searchY + 11, searchText.isEmpty() ? muted : ink);
        if (searchFocused && (System.currentTimeMillis() / 550L) % 2 == 0) {
            int caretX = Math.min(searchX + searchW - 12, searchX + 11 + font.width(trim(searchText, searchW - 29)));
            fill(ctx, caretX, searchY + 8, caretX + 1, searchY + 20, ink);
        }
        addHit(searchX, searchY, searchW, 30, K_SEARCH, null, null, 0);

        moonContentTop = headY + 69;
        final int bottom = wy + winH - 67;
        if (moonContentTop < bottom) {
            int maxRight = selected == null ? contentRight : Math.min(contentRight, premiumSettingsX() - 13);
            int areaW = Math.max(120, maxRight - cx);
            if (!query.isEmpty() || !isSpecial(page) || page == cats.size())
                renderMoonTiles(ctx, mx, my, cx, moonContentTop, areaW, bottom, query, interactive);
            else if (page == cats.size() + 1)
                drawAppearanceWorkspace(ctx, cx, moonContentTop, areaW, bottom);
            else if (page == cats.size() + 2)
                drawConfigHub(ctx, cx, moonContentTop, areaW, bottom);
        }

        // Pinned workflow buttons preserve the old K_GUIDE/K_EDITOR actions.
        fill(ctx, left, wy + winH - 58, right, wy + winH - 57, 0x2C5E6387);
        int dockY = wy + winH - 46;
        float guideHover = ease("v254-guide", interactive && mx >= left && mx < left + 111
                && my >= dockY && my < dockY + 29 ? 1f : 0f, 160);
        rr(ctx, left, dockY, 111, 29, 11,
                LovenotePalette.lerp(0x9F232B3E, 0xCC363252, guideHover));
        txtS(ctx, "Macro Guide", left + 13, dockY + 12, 0xFFE4DDF1);
        addHit(left, dockY, 111, 29, K_GUIDE, null, null, 0);
        float schematicHover = ease("v254-schems", interactive && mx >= left + 119 && mx < left + 226
                && my >= dockY && my < dockY + 29 ? 1f : 0f, 160);
        rr(ctx, left + 119, dockY, 107, 29, 11,
                LovenotePalette.lerp(0xA6383055, 0xD14F3D72, schematicHover));
        txtS(ctx, "Schematics", left + 130, dockY + 12, 0xFFF3E9FF);
        addHit(left + 119, dockY, 107, 29, K_GUIDE, null, null, 3);
        float hudHover = ease("v254-hud", interactive && mx >= left + 234 && mx < left + 333
                && my >= dockY && my < dockY + 29 ? 1f : 0f, 160);
        rr(ctx, left + 234, dockY, 99, 29, 11,
                LovenotePalette.lerp(0x9F232B3E, 0xCC363252, hudHover));
        txtS(ctx, "HUD Editor", left + 246, dockY + 12, 0xFFE4DDF1);
        addHit(left + 234, dockY, 99, 29, K_EDITOR, null, null, 0);
        String stats = mc.getFps() + " FPS  /  " + activeCount + " ON";
        if (right - font.width(stats) > left + 345)
            txt(ctx, stats, right - font.width(stats) - 2, dockY + 12, muted);
    }

    private int selectedCount() {
        int n = 0;
        for (Row row : allModules) if (row.active()) n++;
        return n;
    }

    private static boolean v24IsUnported(Row m) {
        return m instanceof LRow row && row.m instanceof com.lovenote.module.UnportedModule;
    }

    /** Stable tiny labels for the compact card chips; no textures or runtime allocations beyond the text itself. */
    private static String shortModuleMark(String title) {
        if (title == null || title.isEmpty()) return "?";
        char first = Character.toUpperCase(title.charAt(0));
        for (int i = 1; i < title.length(); i++) {
            char c = title.charAt(i);
            if ((Character.isUpperCase(c) && Character.isLowerCase(title.charAt(i - 1)))
                    || (c != ' ' && title.charAt(i - 1) == ' ')) {
                return "" + first + Character.toUpperCase(c);
            }
        }
        return title.length() > 1 ? ("" + first + Character.toUpperCase(title.charAt(1)))
                : String.valueOf(first);
    }

    /**
     * V25.8: lightweight premium-glass card renderer.
     * Fixed positions = stable hitboxes; only colour and a short highlight
     * interpolate with actual frame delta. No multi-layer glow loops, no new
     * textures, and no allocations proportional to hover progress.
     */
    private void renderMoonTiles(GuiGraphicsExtractor ctx, int mx, int my,
                                 int left, int top, int areaW, int bottom,
                                 String q, boolean interactive) {
        List<Row> list = visibleModules(q);
        final int gap = 8, rowH = 49;
        final int columns = selected == null && areaW >= 720 ? 3
                : selected == null && areaW >= 415 ? 2 : 1;
        final int tileW = Math.max(100, (areaW - gap * (columns - 1)) / columns);
        final int rows = (list.size() + columns - 1) / columns;
        maxScroll = Math.max(0, rows * (rowH + gap) - gap - (bottom - top));
        scrollTarget = Mth.clamp(scrollTarget, 0, maxScroll);
        scrollShown += (scrollTarget - scrollShown)
                * (1f - (float) Math.exp(-Math.max(0f, dt) / 65f));
        if (Math.abs(scrollShown - scrollTarget) < 0.2f) scrollShown = scrollTarget;

        ctx.enableScissor(left - 1, top - 1, left + areaW + 2, bottom);
        for (int i = 0; i < list.size(); i++) {
            Row m = list.get(i);
            int x = left + (i % columns) * (tileW + gap);
            int y = top + (i / columns) * (rowH + gap) - Math.round(scrollShown);
            if (y + rowH <= top || y >= bottom) continue;
            boolean active = m.active(), focus = selected == m, unported = v24IsUnported(m);
            boolean hovered = interactive && mx >= x && mx < x + tileW
                    && my >= y && my < y + rowH && my >= top && my < bottom;
            // Stable hitboxes, but spring-soft visual motion in fractional pixels.
            float hover = ease("v24-hover:" + m.title(), hovered ? 1f : 0f, 185);
            float on = ease("v24-on:" + m.title(), active ? 1f : 0f, 205);
            float glow = Math.max(hover, focus ? 0.67f : on * 0.40f);
            final int accent = LovenotePalette.C2;
            // A short stagger when changing categories; only visible cards are drawn.
            float arrive = Mth.clamp((tabMs - (i % 12) * 17f) / 205f, 0f, 1f);
            float reveal = arrive * arrive * (3f - 2f * arrive);
            float hSmooth = hover * hover * (3f - 2f * hover);
            float shift = (1f - reveal) * 5.0f - 2.65f * hSmooth;
            boolean translated = Math.abs(shift) > 0.02f;
            if (translated) { ctx.pose().pushMatrix(); ctx.pose().translate(0f, shift); }

            // One offset depth layer, a fine chrome edge and a translucent body.
            // This is intentionally GPU-cheap compared with a stack of bloom passes.
            rr(ctx, x + 1, y + 3, tileW - 2, rowH - 1, 12,
                    LovenotePalette.alpha(0x060712, 65 + Math.round(35 * hover)));
            rr(ctx, x, y, tileW, rowH, 12,
                    LovenotePalette.lerp(0x57636C92,
                            LovenotePalette.alpha(accent, 162), Math.max(hover * 0.88f, focus ? 0.85f : 0f)));
            rr(ctx, x + 1, y + 1, tileW - 2, rowH - 2, 11,
                    LovenotePalette.lerp(active ? 0xE51A2036 : 0xDB151B2B,
                            0xE82D2B49, hover * 0.93f));
            fill(ctx, x + 14, y + 2, x + tileW - 14, y + 3,
                    LovenotePalette.alpha(0xF8EFFF, 22 + Math.round(41 * glow)));
            if (glow > 0.012f) {
                fill(ctx, x + 2, y + 12, x + 4, y + rowH - 12,
                        LovenotePalette.alpha(accent, Math.round(188 * glow)));
            }
            if (hover > 0.04f) {
                float sweep = (float)(0.5 + 0.5 * Math.sin(System.nanoTime() * 1.6e-9));
                int span = Math.max(1, tileW - 80);
                int shineX = x + 16 + Math.round(span * sweep);
                fill(ctx, shineX, y + 2, Math.min(x + tileW - 14, shineX + 23), y + 3,
                        LovenotePalette.alpha(LovenotePalette.C3, Math.round(112 * hover)));
            }
            // Minimal glyph on an inset lit plate. The plate shares the card's palette.
            rr(ctx, x + 11, y + 11, 27, 27, 9,
                    LovenotePalette.lerp(0x6E343B56,
                            LovenotePalette.alpha(accent, 133), Math.max(on, hover * 0.78f)));
            String glyph = shortModuleMark(m.title());
            txt(ctx, glyph, x + 25 - font.width(glyph) / 2, y + 20,
                    active ? 0xFFF9F5FF : 0xFFE5E8F5);

            final int textX = x + 49;
            final int textWidth = Math.max(38, tileW - 104);
            txtS(ctx, trim(m.title(), textWidth), textX, y + 9,
                    active ? 0xFFFFFFFF : 0xFFF5F5FD);
            String description = unported ? "NOT PORTED TO 26.2" : m.description();
            txt(ctx, trim(description, Math.max(32, tileW - 105)), textX, y + 29,
                    unported ? 0xFFF4B997 : 0xFFAFBBD3);
            if (unported) {
                txt(ctx, "!", x + tileW - 20, y + 12, 0xFFFFC5A1);
            } else {
                int sx = x + tileW - 39, sy = y + 11;
                rr(ctx, sx, sy, 27, 13, 7,
                        LovenotePalette.lerp(0x8B3A4158, LovenotePalette.alpha(accent, 194), on));
                rr(ctx, sx + 1, sy + 1, 25, 11, 6,
                        LovenotePalette.lerp(0xC51B2535, LovenotePalette.alpha(accent, 133), on));
                rr(ctx, sx + 3 + Math.round(on * 12f), sy + 3, 7, 7, 4, 0xFFF7F4FF);
            }
            if (translated) ctx.pose().popMatrix();

            // Hitboxes remain at the original card position. Partial cards are
            // clipped visually and never receive offscreen mouse interactions.
            if (y >= top && y + rowH <= bottom) {
                addHit(x, y, tileW, rowH, K_CARD, m, null, 0);
                if (!unported) addHit(x + tileW - 39, y + 6, 35, 20, K_SWITCH, m, null, 0);
            }
        }
        ctx.disableScissor();
        scrollbar(ctx, left + areaW + 4, top, bottom, scrollShown, maxScroll);
        if (list.isEmpty()) {
            rr(ctx, left, top, areaW, Math.min(bottom - top, 112), 11, 0xCC1A2130);
            txtS(ctx, "NO MATCHES", left + 17, top + 19, 0xFFECE5FB);
            txt(ctx, "Try a different module name or category", left + 17, top + 44, 0xFF929BB1);
        }
    }

    private void drawWindow(GuiGraphicsExtractor ctx, int mx, int my, boolean interactive, int corner) {
        int styleA = LovenoteMenuConfig.style.equals("Moon") ? 0xF4 : LovenoteMenuConfig.style.equals("Premium") ? 0xE8 : LovenoteMenuConfig.style.equals("Classic") ? 0xF8 : LovenoteMenuConfig.style.equals("Liquid") ? 0xCC : 0x8C;
        int alpha = (int) (styleA * LovenoteMenuConfig.opacity / 100.0);

        for (int i = LovenoteMenuConfig.glow; i >= 1; i--) rr(ctx, wx - i, wy - i, winW + i * 2, winH + i * 2, corner + 2, LovenotePalette.alpha(LovenotePalette.C2, 5));
        if (LovenoteMenuConfig.shadow) for (int i = 4; i >= 1; i--) rr(ctx, wx - i, wy + 2, winW + i * 2, winH + i * 2, corner + 2, LovenotePalette.alpha(0x000000, 8));
        rr(ctx, wx - 1, wy - 1, winW + 2, winH + 2, corner + 1,
                LovenoteMenuConfig.style.equals("Moon") ? 0x80515865 : LovenotePalette.alpha(LovenotePalette.C2, 64));
        rr(ctx, wx, wy, winW, winH, corner, LovenotePalette.alpha(LovenotePalette.bg(), alpha));

        // Premium glass: restrained inner chrome rim and violet highlights.
        if (LovenoteMenuConfig.style.equals("Premium")) {
            rr(ctx, wx + 2, wy + 2, winW - 4, winH - 4,
                Math.max(4, corner - 2),
                LovenotePalette.alpha(LovenotePalette.C2, 18));

            rr(ctx, wx + 4, wy + 4, winW - 8, winH - 8,
                Math.max(4, corner - 4),
                LovenotePalette.alpha(LovenotePalette.bg(), 220));

            fill(ctx, wx + corner + 8, wy + 4,
                wx + winW - corner - 8, wy + 5,
                LovenotePalette.alpha(LovenotePalette.C3, 75));
        }
        // Thin neon rim matching the approved Drainer V12 reference; no per-pixel shader.
        if(LovenoteMenuConfig.style.equals("Premium")) {
            fill(ctx,wx+corner,wy+1,wx+winW-corner,wy+2,0x96F295F7);
            fill(ctx,wx+corner+6,wy+3,wx+winW-corner-6,wy+4,0x336D4B9A);
        }
        drawDrainerChromeFrame(ctx, corner);
        if (LovenoteMenuConfig.headerLine) grad(ctx, wx + corner, wy, wx + winW - corner, wy + 1, LovenotePalette.alpha(LovenotePalette.C1, 150), LovenotePalette.alpha(LovenotePalette.C3, 150));
        if (LovenoteMenuConfig.sheen) for (int i = 0; i < 26; i++) {
            int yy = wy + 2 + i * 2;
            if (yy >= wy + winH - 2) break;
            fill(ctx, wx + 2, yy, wx + winW - 2, yy + 2, LovenotePalette.alpha(0xFFFFFF, (int) (10 * (1f - i / 26f))));
        }

        int cx = contentX(), cw = contentW();
        int hdr = wy + 107;
        int ftr = wy + winH - (LovenoteMenuConfig.footer ? 22 : 8);
        fill(ctx, cx - 2, hdr, cx + cw + 2, hdr + 1, LovenotePalette.alpha(LovenotePalette.track(), 150));
        if (LovenoteMenuConfig.footer) fill(ctx, cx - 2, ftr, cx + cw + 2, ftr + 1, LovenotePalette.alpha(LovenotePalette.track(), 150));

        String q = searchText.trim().toLowerCase();
        boolean moduleGrid = !q.isEmpty() || !isSpecial(page) || page == cats.size();

        // Moon-style type-led header replaces the large illustrated client logo.
        if (LovenoteMenuConfig.style.equals("Moon")) {
            txtS(ctx, "drainer", cx, wy + 30, LovenotePalette.text());
            txt(ctx, "CLIENT  /  MODULES", cx, wy + 48, LovenotePalette.faint());
            fill(ctx, cx, wy + 65, cx + cw, wy + 66, 0x53474A56);
        }
        // DRAINER: centred animated logo, above the search bar.
        if (LovenoteMenuConfig.logo) {
            double headerLogoAngle = System.nanoTime() / 1000000000.0
                * Math.PI * 2.0 * 0.18;

            com.lovenote.util.Logos.drawSmoothWide(ctx, cx + (cw - 230) / 2, wy, 230, 76, 0.0);
        }

        int sw = Math.min(200, cw / 2);
        rr(ctx, cx, wy + 78, sw, 18, 9, LovenotePalette.alpha(LovenotePalette.chip(), 210));
        circle(ctx, cx + 11, wy + 87, 4, LovenotePalette.faint());
        circle(ctx, cx + 11, wy + 87, 2, LovenotePalette.alpha(LovenotePalette.bg(), 255));
        fill(ctx, cx + 14, wy + 90, cx + 17, wy + 91, LovenotePalette.faint());
        addHit(cx, wy + 78, sw, 18, K_SEARCH, null, null, 0);
        if (searchText.isEmpty() && !searchFocused) {
            txt(ctx, "Search...", cx + 22, wy + 83, LovenotePalette.faint());
        } else {
            txt(ctx, searchText, cx + 22, wy + 83, LovenotePalette.text());
            if (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
                int caretX = cx + 22 + font.width(searchText);
                fill(ctx, caretX, wy + 82, caretX + 1, wy + 92, LovenotePalette.text());
            }
        }

        String crumb = "drainer";
        int crumbX = cx + sw + 10;
        txt(ctx, crumb, crumbX, wy + 83, LovenotePalette.alpha(LovenotePalette.faint(), 170));
        crumbX += font.width(crumb) + 4;
        txt(ctx, "\u203A", crumbX, wy + 83, LovenotePalette.alpha(LovenotePalette.faint(), 120));
        crumbX += 8;
        txt(ctx, !q.isEmpty() ? "search" : pageLabel(page).toLowerCase(), crumbX, wy + 83, LovenotePalette.wave(0f));

        int bx = wx + winW - 16;
        boolean cHov = interactive && mx >= bx - 4 && mx <= bx + 10 && my >= wy + 10 && my <= wy + 26;
        int closeColor = LovenotePalette.alpha(cHov ? 0xFFEF7777 : 0xBFB2CE, cHov ? 240 : 175);
        for (int i = 0; i < 5; i++) {
            fill(ctx, bx + i, wy + 15 + i, bx + i + 1, wy + 16 + i, closeColor);
            fill(ctx, bx + 4 - i, wy + 15 + i, bx + 5 - i, wy + 16 + i, closeColor);
        }
        addHit(bx - 3, wy + 10, 14, 14, K_CLOSE, null, null, 0);
        rr(ctx, bx - 105, wy + 10, 91, 17, 4, LovenotePalette.alpha(LovenotePalette.chip(), 215));
        txt(ctx, "HUD Editor", bx - 97, wy + 14, LovenotePalette.text());
        addHit(bx - 105, wy + 10, 91, 17, K_EDITOR, null, null, 0);
        rr(ctx, bx - 202, wy + 10, 90, 17, 4, LovenotePalette.alpha(LovenotePalette.chip(), 215));
        txt(ctx, "Macro Guide", bx - 195, wy + 14, LovenotePalette.text());
        addHit(bx - 202, wy + 10, 90, 17, K_GUIDE, null, null, 0);

        float tSlide = Mth.clamp(tabMs / 220f, 0f, 1f);
        float tEase = 1f - (1f - tSlide) * (1f - tSlide);
        int titleY = wy + 116 + (int) ((1f - tEase) * 8f);
        String title = !q.isEmpty() ? "All" : pageLabel(page);
        txtS(ctx, title, cx, titleY, LovenotePalette.text());
        int count = visibleModules(q).size();
        String sub = page == cats.size() + 1 && q.isEmpty() ? LovenotePalette.PRESETS.length + " themes" : page == cats.size() + 2 && q.isEmpty() ? "settings" : count + (count == 1 ? " module" : " modules");
        int subX = cx + font.width(title) + 8;
        if (subX + font.width(sub) < cx + cw) txt(ctx, sub, subX, titleY + 2, LovenotePalette.faint());

        int top = wy + 136, bottom = ftr - 6;
        if (!q.isEmpty() || !isSpecial(page) || page == cats.size()) renderCards(ctx, mx, my, cx, top, cw, bottom, q, interactive, corner);
        else if (page == cats.size() + 1) {
            drawAppearanceWorkspace(ctx, cx, top, cw, bottom);
        } else if (page == cats.size() + 2) {
            drawConfigHub(ctx, cx, top, cw, bottom);
        } else {
            txt(ctx, pageLabel(page) + " page",
                cx + 4, top + 8, LovenotePalette.faint());
        }

        if (LovenoteMenuConfig.footer) drawFooter(ctx, cx, cw, ftr);
    }

    private void drawFooter(GuiGraphicsExtractor ctx, int cx, int cw, int ftr) {
        int y = ftr + 7;
        txt(ctx, "drainer", cx, y, LovenotePalette.faint());
        long secs = (System.currentTimeMillis() - sessionStart) / 1000L;
        txt(ctx, String.format("%02d:%02d", secs / 60, secs % 60), cx + font.width("drainer") + 6, y, LovenotePalette.alpha(LovenotePalette.faint(), 140));
        int enabled = 0;
        for (Row m : allModules) if (m.active()) enabled++;
        String[] seg = { enabled + " on", mc.getFps() + " fps", "ctrl+scroll" };
        int total = 0;
        for (String t : seg) total += font.width(t) + 10;
        total -= 10;
        int x = cx + cw - total;
        for (String t : seg) { if (x > cx + 80) txt(ctx, t, x, y, LovenotePalette.alpha(LovenotePalette.faint(), 160)); x += font.width(t) + 10; }
    }

    private List<Row> visibleModules(String q) {
        if (visibleCacheQuery != null && visibleCachePage == page && visibleCacheQuery.equals(q))
            return visibleCache;
        List<Row> out = new ArrayList<>();
        for (Row m : allModules) {
            if (!q.isEmpty()) {
                if (m.title().toLowerCase(java.util.Locale.ROOT).contains(q)
                        || m.description().toLowerCase(java.util.Locale.ROOT).contains(q)) out.add(m);
            } else if (m.catIndex() == page) out.add(m);
        }
        out.sort((x, y) -> x.title().compareToIgnoreCase(y.title()));
        visibleCache = out;
        visibleCacheQuery = q;
        visibleCachePage = page;
        return out;
    }

    /** Moon-inspired vertical list, deliberately lighter than the old glass card grid.
     *  Uses existing Row toggles and hit kinds, so saved modules/settings survive. */
    private void renderMoonRows(GuiGraphicsExtractor ctx, int mx, int my,
                                int cx, int top, int cw, int bottom,
                                String q, boolean interactive) {
        List<Row> list = visibleModules(q);
        final int rowH = 34, gap = 3;
        maxScroll = Math.max(0, list.size() * (rowH + gap) - (bottom - top));
        scrollTarget = Mth.clamp(scrollTarget, 0, maxScroll);
        scrollShown += (scrollTarget - scrollShown) * (1f - (float) Math.exp(-dt / 45f));
        if (Math.abs(scrollTarget - scrollShown) < 0.2f) scrollShown = scrollTarget;
        ctx.enableScissor(cx - 2, top - 1, cx + cw + 3, bottom);
        for (int i = 0; i < list.size(); i++) {
            Row m = list.get(i);
            int y = top + i * (rowH + gap) - Math.round(scrollShown);
            if (y + rowH < top || y > bottom) continue;
            boolean hovered = interactive && mx >= cx && mx < cx + cw
                    && my >= y && my < y + rowH && my >= top && my < bottom;
            boolean active = m.active();
            boolean focused = selected == m;
            int bg = focused ? 0xFF28232F : hovered ? 0xFF24262D : 0xFF1A1C21;
            rr(ctx, cx, y, cw, rowH, 4, bg);
            if (active) fill(ctx, cx, y + 5, cx + 2, y + rowH - 5,
                    LovenotePalette.alpha(LovenotePalette.C2, 210));
            txt(ctx, trim(m.title(), cw - 94), cx + 10, y + 7,
                    active ? LovenotePalette.text() : LovenotePalette.dim());
            // Tiny activity label, no animated glow, no oversized icon badge.
            txt(ctx, active ? "on" : "off", cx + cw - 66, y + 12,
                    active ? LovenotePalette.C2 : LovenotePalette.faint());
            int sx = cx + cw - 36, sy = y + (rowH - 10) / 2;
            rr(ctx, sx, sy, 27, 10, 5, active ? 0xFF684A88 : 0xFF383B43);
            rr(ctx, sx + (active ? 18 : 2), sy + 2, 6, 6, 3,
                    active ? 0xFFF5F0FA : 0xFFAEB3BD);
            if (y >= top && y + rowH <= bottom) {
                addHit(cx, y, cw, rowH, K_CARD, m, null, 0);
                addHit(sx - 4, sy - 5, 35, 20, K_SWITCH, m, null, 0);
            }
        }
        ctx.disableScissor();
        scrollbar(ctx, cx + cw + 6, top, bottom, scrollShown, maxScroll);
        if (list.isEmpty()) txt(ctx, "No modules found", cx + 8, top + 8, LovenotePalette.faint());
    }

    private void renderCards(GuiGraphicsExtractor ctx, int mx, int my,
                             int cx, int top, int cw, int bottom,
                             String q, boolean interactive, int corner) {
        if (LovenoteMenuConfig.style.equals("Moon")) {
            renderMoonRows(ctx, mx, my, cx, top, cw, bottom, q, interactive);
            return;
        }
        List<Row> list = visibleModules(q);

        int ncols = 1;
        boolean compact = true;
        int gap = 7;
        int cardW = (cw - (ncols - 1) * gap) / ncols;
        int cardH = compact ? 42 : 68;

        int rows = (list.size() + ncols - 1) / ncols;
        maxScroll = Math.max(0, rows * (cardH + gap) - (bottom - top));

        scrollTarget = Mth.clamp(scrollTarget, 0, maxScroll);
        scrollShown += (scrollTarget - scrollShown)
            * (1f - (float) Math.exp(-dt / 40f));

        if (Math.abs(scrollTarget - scrollShown) < 0.2f)
            scrollShown = scrollTarget;

        if (mx > cx - 6 && mx < cx + cw + 12
            && my > top && my < bottom)
            sbAlpha = 1f;

        int cr = Math.max(7, corner);

        ctx.enableScissor(cx - 2, top - 2, cx + cw + 4, bottom);

        for (int i = 0; i < list.size(); i++) {
            Row m = list.get(i);

            int col = i % ncols;
            int x = cx + col * (cardW + gap);
            int y = top + (i / ncols) * (cardH + gap)
                - Math.round(scrollShown);

            if (y + cardH < top - 8 || y > bottom + 8)
                continue;

            float st = stagger(i);
            int sy = y + (int) ((1f - st) * 10f);

            boolean hov = interactive
                && mx >= x && mx < x + cardW
                && my >= sy && my < sy + cardH
                && my >= top && my <= bottom;

            float hv = ease(m.title() + ":h", hov ? 1f : 0f, 150);
            float on = ease(m.title() + ":s", m.active() ? 1f : 0f, 180);
            float sel = ease(m.title() + ":sel",
                selected == m ? 1f : 0f, 180);

            int ly = sy - (int) (hv * LovenoteMenuConfig.hoverLift);
            int accent = LovenotePalette.wave(i * 0.07f);

            // Animated breathing glow
            float pulse = (float) (
                0.5 + 0.5 * Math.sin(
                    System.currentTimeMillis() / 650.0 + i * 0.65
                )
            );

            if (on > 0.01f || hv > 0.01f || sel > 0.01f) {
                int glowAlpha = (int) (
                    22 + 40 * pulse * on + 65 * hv + 75 * sel
                );

                rr(ctx, x - 2, ly - 2,
                    cardW + 4, cardH + 4, cr + 2,
                    LovenotePalette.alpha(accent, Math.min(110, glowAlpha / 2)));
            }

            // Glass card surface
            rr(ctx, x, ly, cardW, cardH, cr,
                LovenotePalette.lerp(
                    LovenotePalette.card(),
                    LovenotePalette.cardHover(), hv));

            // Accent line
            if (on > 0.01f) {
                rr(ctx, x + 12, ly + 1, cardW - 24, 2, 1,
                    LovenotePalette.alpha(accent,
                        (int) (110 * on)));
            }

            // Rounded illuminated icon tile
            int tile = compact ? 20 : 28;
            int ix = x + 10;
            int iy = ly + 7;

            rr(ctx, ix, iy, tile, tile, 8,
                LovenotePalette.alpha(accent,
                    (int) (65 + 70 * on + 40 * hv)));

            rr(ctx, ix + 2, iy + 2,
                tile - 4, tile - 4, 6,
                LovenotePalette.alpha(
                    LovenotePalette.card(), 160));

            // Minimal geometric module emblem
            circle(ctx, ix + tile / 2, iy + tile / 2,
                compact ? 5 : 6,
                LovenotePalette.alpha(accent, 235));

            circle(ctx, ix + tile / 2, iy + tile / 2,
                2, LovenotePalette.card());

            // Module title
            int textX = ix + tile + 9;
            int textW = Math.max(10, cardW - (textX - x) - 52);

            String name = trim(m.title(), textW);

            txt(ctx, name, textX, ly + 9,
                LovenotePalette.text());

            // Description
            if (!compact && LovenoteMenuConfig.descriptions) {
                txt(ctx,
                    trim(m.description(), Math.max(10, cardW - 26)),
                    x + 13, ly + 36,
                    LovenotePalette.dim());
            }

            // Status and animated switch
            String status = m.active() ? "On" : "Off";

            txt(ctx, status, textX, ly + cardH - 16,
                LovenotePalette.lerp(
                    LovenotePalette.faint(), accent, on));

            int switchX = x + cardW - 35;
            int switchY = ly + (cardH - 10) / 2;

            drawSwitch(ctx, switchX, switchY, on);

            // Existing interaction handling
            if (ly >= top && ly + cardH <= bottom) {
                addHit(x, ly, cardW, cardH, K_CARD, m, null, 0);
                addHit(switchX - 4, switchY - 5, 34, 22, K_SWITCH, m, null, 0);
            }

            if (hov && LovenoteMenuConfig.tooltips
                && !name.equals(m.title()))
                tooltip = m.title();
        }

        ctx.disableScissor();

        scrollbar(ctx, cx + cw + 6,
            top, bottom, scrollShown, maxScroll);

        if (list.isEmpty())
            txt(ctx, "No modules found",
                cx + 4, top + 8, LovenotePalette.faint());
    }
    // Drainer Config Hub. Uses existing theme palette and hitbox system.
    private void configAction(int id) {
        switch (id) {
            case 0 -> configExpanded = true;
            case 1 -> configExpanded = false;
            case 2 -> LovenoteMenuConfig.compact = !LovenoteMenuConfig.compact;
            case 3 -> LovenoteMenuConfig.descriptions = !LovenoteMenuConfig.descriptions;
            case 4 -> LovenoteMenuConfig.tooltips = !LovenoteMenuConfig.tooltips;
            case 5 -> LovenoteMenuConfig.logo = !LovenoteMenuConfig.logo;
            case 6 -> LovenoteMenuConfig.shadow = !LovenoteMenuConfig.shadow;
            case 7 -> LovenoteMenuConfig.sheen = !LovenoteMenuConfig.sheen;
            case 8 -> LovenoteMenuConfig.ripples = !LovenoteMenuConfig.ripples;
            case 9 -> LovenoteMenuConfig.menuScale = Mth.clamp(LovenoteMenuConfig.menuScale - 5, 20, 110);
            case 10 -> LovenoteMenuConfig.menuScale = Mth.clamp(LovenoteMenuConfig.menuScale + 5, 20, 110);
            case 11 -> LovenoteMenuConfig.footer = !LovenoteMenuConfig.footer;
            case 12 -> LovenoteMenuConfig.headerLine = !LovenoteMenuConfig.headerLine;
            case 300 -> appearanceTab = 0;
            case 301 -> appearanceTab = 1;
            case 302 -> appearanceTab = 2;
            case 303 -> appearanceTab = 3;
            case 304 -> appearancePresetPage = Math.max(0,appearancePresetPage-1);
            case 305 -> appearancePresetPage = Math.min(2,appearancePresetPage+1);
            case 306 -> { appearanceHexField=1;appearanceHexInput=LovenoteMenuConfig.customPrimary; }
            case 307 -> { appearanceHexField=2;appearanceHexInput=LovenoteMenuConfig.customSecondary; }
            case 308 -> applyAppearanceHex();
            case 309 -> { LovenoteMenuConfig.accentIndex=0;LovenoteMenuConfig.apply(); }
            case 340 -> LovenoteMenuConfig.widgetGlass=!LovenoteMenuConfig.widgetGlass;
            case 341 -> LovenoteMenuConfig.widgetGlow=Mth.clamp(LovenoteMenuConfig.widgetGlow-5,0,100);
            case 342 -> LovenoteMenuConfig.widgetGlow=Mth.clamp(LovenoteMenuConfig.widgetGlow+5,0,100);
            case 343 -> LovenoteMenuConfig.widgetBorder=Mth.clamp(LovenoteMenuConfig.widgetBorder-5,0,160);
            case 344 -> LovenoteMenuConfig.widgetBorder=Mth.clamp(LovenoteMenuConfig.widgetBorder+5,0,160);
            case 345 -> LovenoteMenuConfig.widgetSheen=Mth.clamp(LovenoteMenuConfig.widgetSheen-5,0,130);
            case 346 -> LovenoteMenuConfig.widgetSheen=Mth.clamp(LovenoteMenuConfig.widgetSheen+5,0,130);
            case 347 -> LovenoteMenuConfig.widgetOpacity=Mth.clamp(LovenoteMenuConfig.widgetOpacity-5,25,100);
            case 348 -> LovenoteMenuConfig.widgetOpacity=Mth.clamp(LovenoteMenuConfig.widgetOpacity+5,25,100);
            case 349 -> LovenoteMenuConfig.glow=Mth.clamp(LovenoteMenuConfig.glow-1,0,12);
            case 350 -> LovenoteMenuConfig.glow=Mth.clamp(LovenoteMenuConfig.glow+1,0,12);
            case 351 -> LovenoteMenuConfig.opacity=Mth.clamp(LovenoteMenuConfig.opacity-5,40,100);
            case 352 -> LovenoteMenuConfig.opacity=Mth.clamp(LovenoteMenuConfig.opacity+5,40,100);
            case 353 -> LovenoteMenuConfig.corner=Mth.clamp(LovenoteMenuConfig.corner-1,2,16);
            case 354 -> LovenoteMenuConfig.corner=Mth.clamp(LovenoteMenuConfig.corner+1,2,16);
            case 355 -> LovenoteMenuConfig.shadow=!LovenoteMenuConfig.shadow;
            case 356 -> LovenoteMenuConfig.sheen=!LovenoteMenuConfig.sheen;
            case 357 -> LovenoteMenuConfig.ripples=!LovenoteMenuConfig.ripples;
            case 358 -> LovenoteMenuConfig.stagger=!LovenoteMenuConfig.stagger;
            case 359 -> LovenoteMenuConfig.animation = switch(LovenoteMenuConfig.animation) {
                case "Fade" -> "Slide"; case "Slide" -> "Scale"; case "Scale" -> "Tilt"; default -> "Fade";
            };
            case 360 -> LovenoteMenuConfig.animSpeed=Mth.clamp(LovenoteMenuConfig.animSpeed-50,100,900);
            case 361 -> LovenoteMenuConfig.animSpeed=Mth.clamp(LovenoteMenuConfig.animSpeed+50,100,900);
            case 362 -> LovenoteMenuConfig.hoverLift=Mth.clamp(LovenoteMenuConfig.hoverLift-1,0,8);
            case 363 -> LovenoteMenuConfig.hoverLift=Mth.clamp(LovenoteMenuConfig.hoverLift+1,0,8);
            case 364 -> LovenoteMenuConfig.menuScale=Mth.clamp(LovenoteMenuConfig.menuScale-5,20,110);
            case 365 -> LovenoteMenuConfig.menuScale=Mth.clamp(LovenoteMenuConfig.menuScale+5,20,110);
            case 366 -> LovenoteMenuConfig.compact=!LovenoteMenuConfig.compact;
            case 367 -> LovenoteMenuConfig.descriptions=!LovenoteMenuConfig.descriptions;
            case 368 -> LovenoteMenuConfig.tooltips=!LovenoteMenuConfig.tooltips;
            case 369 -> LovenoteMenuConfig.headerLine=!LovenoteMenuConfig.headerLine;
            case 370 -> LovenoteMenuConfig.fontMode="Clean";
            case 371 -> LovenoteMenuConfig.fontMode="Compact";
            case 372 -> LovenoteMenuConfig.fontMode="Classic";
            case 373 -> LovenoteMenuConfig.footer=!LovenoteMenuConfig.footer;
            case 374 -> LovenoteMenuConfig.fontMode="Segoe UI";
            case 375 -> LovenoteMenuConfig.fontMode="Arial";
            case 376 -> LovenoteMenuConfig.fontMode="Verdana";
            case 377 -> LovenoteMenuConfig.fontMode="Tahoma";
            case 378 -> LovenoteMenuConfig.fontMode="Trebuchet";
            case 379 -> LovenoteMenuConfig.decorativeLogo=!LovenoteMenuConfig.decorativeLogo;
            case 31 -> {
                showUnported = !showUnported;
                buildRows();
                selected = null; panelModule = null;
                scrollTarget = 0; scrollShown = 0;
            }
            case 20 -> { configExpanded = true; profilesExpanded = true; profileTyping = false; }
            case 21 -> { profilesExpanded = false; pendingDelete = ""; }
            case 22 -> { profileTyping = true; profileName = ""; }
            case 23 -> { com.lovenote.config.DrainerProfiles.create(profileName); profileTyping = false; }
            case 24 -> com.lovenote.config.DrainerProfiles.saveActive();
            case 25 -> com.lovenote.config.DrainerProfiles.setAutoSave(!com.lovenote.config.DrainerProfiles.autoSave());
            case 26 -> com.lovenote.config.DrainerProfiles.setAutoLoad(!com.lovenote.config.DrainerProfiles.autoLoad());
            case 27 -> profilePage = Math.max(0, profilePage - 1);
            case 28 -> profilePage++;
            default -> {
                if (id >= 100 && id < 200) {
                    java.util.List<String> names = com.lovenote.config.DrainerProfiles.names();
                    int i = profilePage * 4 + id - 100;
                    if (i < names.size()) com.lovenote.config.DrainerProfiles.load(names.get(i));
                } else if (id >= 200 && id < 300) {
                    java.util.List<String> names = com.lovenote.config.DrainerProfiles.names();
                    int i = profilePage * 4 + id - 200;
                    if (i < names.size()) {
                        String name = names.get(i);
                        if (name.equals(pendingDelete)) { com.lovenote.config.DrainerProfiles.delete(name); pendingDelete = ""; }
                        else pendingDelete = name;
                    }
                }
            }
        }
    }

    private void configButton(GuiGraphicsExtractor ctx, int x, int y, int w, int h, String label, int id) {
        DrainerSkin.control(ctx, x, y, w, h, false);
        txt(ctx, trim(label, w - 14), x + 8, y + (h - 8) / 2, LovenotePalette.text());
        addHit(x, y, w, h, K_CONFIG, null, null, id);
    }

    private void drawConfigHub(GuiGraphicsExtractor ctx, int x, int top, int width, int bottom) {
        int w = Math.max(140, Math.min(width - 16, 380));
        int y = top + 8;
        txt(ctx, "CONFIGURATION", x + 8, y, LovenotePalette.text());
        txt(ctx, "Appearance and client preferences", x + 8, y + 16, LovenotePalette.dim());
        configButton(ctx, x + 8, y + 37, w - 8, 28, "Open expanded settings  >", 0);
        if (y + 102 < bottom) {
            configButton(ctx, x + 8, y + 75, w - 8, 28, "Saved module configurations  >", 20);
        }
        // An explicit opt-in catalogue of legacy entries (not functional ports).
        if (y + 142 < bottom) {
            configButton(ctx, x + 8, y + 113, w - 8, 24,
                (showUnported ? "Hide" : "Show") + " unfinished legacy modules (" + unportedCount() + ")", 31);
        }
        if (y + 205 < bottom) {
            txt(ctx, "ACTIVE THEME", x + 8, y + 179, LovenotePalette.faint());
            int idx = LovenoteMenuConfig.accentIndex;
            String name = idx >= 0 && idx < LovenotePalette.PRESET_NAMES.length
                    ? LovenotePalette.PRESET_NAMES[idx] : "Custom";
            txt(ctx, name, x + 8, y + 197, LovenotePalette.text());
        }
    }

    private void drawConfigExpanded(GuiGraphicsExtractor ctx) {
        if (profilesExpanded) { drawProfiles(ctx); return; }
        // Full-size in-window overlay; no changes to the existing window dimensions.
        int x = wx + railW + 9, y = wy + 104;
        int w = winW - railW - 20, h = winH - 137;
        rr(ctx, x - 2, y - 2, w + 4, h + 4, 12, LovenotePalette.alpha(LovenotePalette.C2, 140));
        rr(ctx, x, y, w, h, 11, LovenotePalette.alpha(LovenotePalette.card(), 250));
        txtS(ctx, "Drainer  /  Config Hub", x + 14, y + 12, LovenotePalette.text());
        configButton(ctx, x + w - 59, y + 8, 45, 22, "Close", 1);
        fill(ctx, x + 12, y + 37, x + w - 12, y + 38, LovenotePalette.alpha(LovenotePalette.C2, 85));
        txt(ctx, "APPEARANCE", x + 14, y + 48, LovenotePalette.wave(0f));
        int colW = (w - 40) / 2;
        String[] labels = {
                "Compact UI", "Descriptions", "Tooltips", "Show logo", "Window shadow",
                "Header sheen", "Click ripples", "Footer", "Header line"
        };
        boolean[] enabled = {
                LovenoteMenuConfig.compact, LovenoteMenuConfig.descriptions, LovenoteMenuConfig.tooltips,
                LovenoteMenuConfig.logo, LovenoteMenuConfig.shadow, LovenoteMenuConfig.sheen,
                LovenoteMenuConfig.ripples, LovenoteMenuConfig.footer, LovenoteMenuConfig.headerLine
        };
        int[] ids = {2, 3, 4, 5, 6, 7, 8, 11, 12};
        for (int i = 0; i < labels.length; i++) {
            int bx = x + 14 + (i % 2) * (colW + 12);
            int by = y + 69 + (i / 2) * 29;
            if (by + 24 >= y + h - 48) break;
            configButton(ctx, bx, by, colW, 24, labels[i] + (enabled[i] ? "  ON" : "  OFF"), ids[i]);
        }
        int sy = y + h - 38;
        txt(ctx, "UI scale  " + LovenoteMenuConfig.menuScale + "%", x + 14, sy + 8, LovenotePalette.text());
        configButton(ctx, x + w - 94, sy, 36, 24, "-", 9);
        configButton(ctx, x + w - 50, sy, 36, 24, "+", 10);
    }

    private void drawProfiles(GuiGraphicsExtractor ctx) {
        int x = wx + railW + 9, y = wy + 104;
        int w = winW - railW - 20, h = winH - 137;
        rr(ctx, x - 2, y - 2, w + 4, h + 4, 12, LovenotePalette.alpha(LovenotePalette.C2, 140));
        rr(ctx, x, y, w, h, 11, LovenotePalette.alpha(LovenotePalette.card(), 250));
        txtS(ctx, "Drainer / Saved Configs", x + 12, y + 12, LovenotePalette.text());
        configButton(ctx, x + w - 60, y + 8, 48, 22, "Back", 21);
        txt(ctx, "Active: " + (com.lovenote.config.DrainerProfiles.active().isEmpty() ? "None" : com.lovenote.config.DrainerProfiles.active()),
                x + 12, y + 41, LovenotePalette.dim());
        int bw = (w - 34) / 2;
        configButton(ctx, x + 12, y + 59, bw, 22, "Auto save: " + (com.lovenote.config.DrainerProfiles.autoSave() ? "ON" : "OFF"), 25);
        configButton(ctx, x + 22 + bw, y + 59, bw, 22, "Auto load: " + (com.lovenote.config.DrainerProfiles.autoLoad() ? "ON" : "OFF"), 26);
        configButton(ctx, x + 12, y + 86, bw, 22, "Save current", 24);
        configButton(ctx, x + 22 + bw, y + 86, bw, 22, "New profile", 22);
        if (profileTyping) {
            txt(ctx, "Name: " + profileName + "_", x + 12, y + 116, LovenotePalette.text());
            configButton(ctx, x + w - 78, y + 111, 66, 22, "Create", 23);
        } else {
            txt(ctx, "Profiles (Load / Delete)", x + 12, y + 117, LovenotePalette.dim());
        }
        java.util.List<String> names = com.lovenote.config.DrainerProfiles.names();
        profilePage = Math.min(profilePage, Math.max(0, (names.size() - 1) / 4));
        int top = y + 141;
        int rowH = Math.min(29, Math.max(23, (h - 190) / 4));
        for (int i = 0; i < 4 && profilePage * 4 + i < names.size(); i++) {
            String n = names.get(profilePage * 4 + i);
            int ry = top + i * rowH;
            if (ry + 22 >= y + h - 28) break;
            txt(ctx, trim(n, w - 142), x + 13, ry + 7, LovenotePalette.text());
            configButton(ctx, x + w - 124, ry, 53, 22, "Load", 100 + i);
            configButton(ctx, x + w - 66, ry, 54, 22, n.equals(pendingDelete) ? "Sure?" : "Delete", 200 + i);
        }
        if (names.size() > 4) {
            configButton(ctx, x + 12, y + h - 27, 32, 20, "<", 27);
            txt(ctx, (profilePage + 1) + " / " + ((names.size() + 3) / 4), x + 51, y + h - 22, LovenotePalette.dim());
            configButton(ctx, x + 91, y + h - 27, 32, 20, ">", 28);
        }
        String status = com.lovenote.config.DrainerProfiles.status;
        if (!status.isEmpty()) txt(ctx, trim(status, w - 154), x + 138, y + h - 22, LovenotePalette.dim());
    }

    private void applyAppearanceHex() {
        if (appearanceHexField==0)return;
        String val=appearanceHexInput.replace("#","").trim();
        if (val.length()!=6) return;
        try { Integer.parseInt(val,16); }
        catch(NumberFormatException e) { return; }
        if (appearanceHexField==1)LovenoteMenuConfig.customPrimary=val;
        else LovenoteMenuConfig.customSecondary=val;
        LovenoteMenuConfig.accentIndex=-1;
        LovenoteMenuConfig.apply();
        appearanceHexField=0;
    }
    private void appearancePair(GuiGraphicsExtractor g,int x,int y,int w,String label,String value,int less,int more) {
        txt(g,label,x+7,y+7,LovenotePalette.text());
        txt(g,value,x+w-111,y+7,LovenotePalette.dim());
        configButton(g,x+w-57,y,24,22,"-",less);
        configButton(g,x+w-29,y,24,22,"+",more);
    }
    private void drawAppearanceWorkspace(GuiGraphicsExtractor g,int x,int top,int width,int bottom) {
        int left=x+8,right=x+width-10;
        txt(g,"CUSTOMISE DRAINER",left,top+5,LovenotePalette.text());
        DrainerSkin.section(g,left,top+17,Math.max(25,right-left));
        String[] tabs={"Colours","Glass","Motion","Typography"};
        int tabW=Math.max(61,Math.min(112,(right-left-18)/4));
        for(int i=0;i<tabs.length;i++){
            int tx=left+i*(tabW+5),ty=top+23;
            boolean tabActive = i == appearanceTab;
            float tabMotion = ease("v254-app-tab-" + i, tabActive ? 1f : 0f, 180);
            DrainerSkin.control(g,tx,ty,tabW,21,tabActive);
            if (tabMotion > 0.02f) {
                fill(g, tx + 9, ty + 19, tx + tabW - 9, ty + 20,
                        LovenotePalette.alpha(LovenotePalette.C2, Math.round(tabMotion * 160)));
            }
            txt(g,tabs[i],tx+7,ty+6,LovenotePalette.lerp(LovenotePalette.dim(), 0xFFFFFFFF, tabMotion));
            addHit(tx,ty,tabW,21,K_CONFIG,null,null,300+i);
        }
        int y=top+57;
        if(appearanceTab==0){
            txt(g,"Accent palette",left,y,LovenotePalette.dim());
            txt(g,"Page "+(appearancePresetPage+1),left+104,y,LovenotePalette.faint());
            configButton(g,right-63,y-5,27,19,"<",304);
            configButton(g,right-31,y-5,27,19,">",305);
            int size=22, gap=7, columns=8;
            int count=LovenotePalette.PRESETS.length;
            for(int k=0;k<24;k++){
                int j=appearancePresetPage*24+k;
                if(j>=count)break;
                int sx=left+(k%columns)*(size+gap),sy=y+22+(k/columns)*(size+gap);
                int color=0xFF000000|LovenotePalette.PRESETS[j][0];
                if(LovenoteMenuConfig.accentIndex==j)rr(g,sx-2,sy-2,size+4,size+4,5,0xBBFFFFFF);
                rr(g,sx,sy,size,size,4,color);
                addHit(sx,sy,size,size,K_ACCENT,null,null,j);
            }
            int sub=y+124;
            txt(g,"Custom HEX accents",left,sub,LovenotePalette.dim());
            String pfx=appearanceHexField==1?appearanceHexInput+"_":LovenoteMenuConfig.customPrimary;
            String sfx=appearanceHexField==2?appearanceHexInput+"_":LovenoteMenuConfig.customSecondary;
            configButton(g,left,sub+17,Math.min(200,(width-30)/2),25,"Primary #"+pfx,306);
            configButton(g,left+Math.min(200,(width-30)/2)+10,sub+17,Math.min(200,(width-30)/2),25,"Secondary #"+sfx,307);
            if(appearanceHexField!=0){
                txt(g,"Type 6 hex characters and press ENTER",left,sub+53,0xFFDAC9E8);
                configButton(g,right-77,sub+48,74,22,"Apply",308);
            }
            txt(g,"Click a swatch or edit both colours. Widgets follow your accent.",left,sub+84,LovenotePalette.faint());
        }else if(appearanceTab==1){
            int w=Math.min(460,width-20);
            configButton(g,left,y,w,24,"Widget glass: "+(LovenoteMenuConfig.widgetGlass?"ON":"OFF"),340);
            int gap=7, colW=Math.max(140,(w-gap)/2), yy=y+32;
            appearancePair(g,left,yy,colW,"Widget glow",LovenoteMenuConfig.widgetGlow+"",341,342);
            appearancePair(g,left+colW+gap,yy,colW,"Widget outline",LovenoteMenuConfig.widgetBorder+"",343,344);yy+=30;
            appearancePair(g,left,yy,colW,"Glass reflection",LovenoteMenuConfig.widgetSheen+"",345,346);
            appearancePair(g,left+colW+gap,yy,colW,"Widget opacity",LovenoteMenuConfig.widgetOpacity+"%",347,348);yy+=30;
            appearancePair(g,left,yy,colW,"Menu glow",String.valueOf(LovenoteMenuConfig.glow),349,350);
            appearancePair(g,left+colW+gap,yy,colW,"Menu opacity",LovenoteMenuConfig.opacity+"%",351,352);yy+=30;
            appearancePair(g,left,yy,colW,"Corner radius",String.valueOf(LovenoteMenuConfig.corner),353,354);
            configButton(g,left+colW+gap,yy,colW,23,"Window shadow: "+(LovenoteMenuConfig.shadow?"ON":"OFF"),355);
        }else if(appearanceTab==2){
            int w=Math.min(460,width-20),gap=7,colW=Math.max(140,(w-gap)/2),yy=y;
            configButton(g,left,yy,colW,24,"Floating decoration: "+(LovenoteMenuConfig.decorativeLogo?"ON":"OFF"),379);
            configButton(g,left+colW+gap,yy,colW,24,"Animation: "+LovenoteMenuConfig.animation,359);yy+=30;
            configButton(g,left,yy,colW,24,"Click ripples: "+(LovenoteMenuConfig.ripples?"ON":"OFF"),357);
            configButton(g,left+colW+gap,yy,colW,24,"Stagger cards: "+(LovenoteMenuConfig.stagger?"ON":"OFF"),358);yy+=30;
            appearancePair(g,left,yy,colW,"Anim. speed",LovenoteMenuConfig.animSpeed+"ms",360,361);
            appearancePair(g,left+colW+gap,yy,colW,"Hover lift",String.valueOf(LovenoteMenuConfig.hoverLift),362,363);yy+=30;
            appearancePair(g,left,yy,colW,"Menu scale",LovenoteMenuConfig.menuScale+"%",364,365);
            configButton(g,left+colW+gap,yy,colW,24,"Compact cards: "+(LovenoteMenuConfig.compact?"ON":"OFF"),366);yy+=30;
            configButton(g,left,yy,w,24,"Module descriptions: "+(LovenoteMenuConfig.descriptions?"ON":"OFF"),367);
        }else{
            txt(g,"Choose an installed system font. No font files are bundled.",left,y,LovenotePalette.dim());
            txt(g,"Drainer logo artwork stays separate.",left,y+17,LovenotePalette.faint());
            int w=Math.max(74,Math.min(110,(width-48)/4));
            String[] names={"Clean", "Compact", "Classic", "Segoe UI", "Arial", "Verdana", "Tahoma", "Trebuchet"};
            int[] choices={370,371,372,374,375,376,377,378};
            for(int i=0;i<names.length;i++){
                int xx=left+(i%4)*(w+6), yy=y+43+(i/4)*32;
                configButton(g,xx,yy,w,26,names[i]+(names[i].equals(LovenoteMenuConfig.fontMode)?" *":""),choices[i]);
            }
            txt(g,"Preview   Smooth text / Drainer 123",left,y+113,LovenotePalette.text());
            txt(g,"OS fonts may be substituted.",left,y+130,LovenotePalette.dim());
            int toggleW=Math.max(80,Math.min(135,(right-left-12)/3));
            configButton(g,left,y+149,toggleW,23,"Tooltips: "+(LovenoteMenuConfig.tooltips?"ON":"OFF"),368);
            configButton(g,left+toggleW+6,y+149,toggleW,23,"Footer: "+(LovenoteMenuConfig.footer?"ON":"OFF"),373);
            configButton(g,left+2*(toggleW+6),y+149,toggleW,23,"Header line: "+(LovenoteMenuConfig.headerLine?"ON":"OFF"),369);
        }
    }

    private void drawThemePresets(GuiGraphicsExtractor ctx,
                                  int x, int top, int width, int bottom) {
        txt(ctx, "APPEARANCE", x + 8, top + 8, LovenotePalette.text());
        txt(ctx, "Choose your accent colour",
            x + 8, top + 25, LovenotePalette.dim());

        int cols = 7;
        int gap = 8;
        int size = Math.min(34, Math.max(18,
            (width - 16 - gap * (cols - 1)) / cols));

        int startX = x + 8;
        int startY = top + 50;

        ctx.enableScissor(x, top, x + width, bottom);

        for (int i = 0; i < LovenotePalette.PRESETS.length; i++) {
            int bx = startX + (i % cols) * (size + gap);
            int by = startY + (i / cols) * (size + gap);

            if (by + size > bottom) break;

            int color = 0xFF000000 | LovenotePalette.PRESETS[i][0];

            if (LovenoteMenuConfig.accentIndex == i) {
                rr(ctx, bx - 2, by - 2, size + 4, size + 4,
                    9, LovenotePalette.alpha(color, 140));
            }

            rr(ctx, bx, by, size, size, 8, color);
            addHit(bx, by, size, size, K_ACCENT, null, null, i);
        }

        ctx.disableScissor();
    }
    private String trim(String s, int w) {
        if (font.width(s) <= w) return s;
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) { if (font.width(b.toString() + c + "..") > w) break; b.append(c); }
        return b + "..";
    }

    private void drawRail(GuiGraphicsExtractor ctx, int mx, int my, int corner) {
        int rx = railX();
        boolean hover = dragWin == 0 && !animating && mx >= rx && mx < rx + railW && my >= wy && my < wy + winH;
        float sb = ease("sb", hover ? 1f : 0f, LovenoteMenuConfig.railSpeed);
        int sbw = railW;
        int sx = LovenoteMenuConfig.railRight ? rx + railW - sbw : rx;

        if (sb > 0.02f) for (int i = 5; i >= 1; i--) rr(ctx, sx + sbw - 2, wy + 4, i * 2 + 2, winH - 8, 8, LovenotePalette.alpha(0x000000, (int) (9 * sb)));
        rr(ctx, sx, wy, sbw, winH, corner,
                LovenoteMenuConfig.style.equals("Moon") ? 0xFB111318 :
                (LovenotePalette.dark ? 0xFA0C0C12 : 0xFAEDEDF4));
        grad(ctx, sx + 2, wy + 8, sx + sbw - 2, wy + 9, LovenotePalette.alpha(LovenotePalette.C1, 90), LovenotePalette.alpha(LovenotePalette.C3, 90));
        fill(ctx, LovenoteMenuConfig.railRight ? sx : sx + sbw - 1, wy + 20,
                LovenoteMenuConfig.railRight ? sx + 1 : sx + sbw, wy + winH - 20,
                LovenotePalette.alpha(LovenotePalette.C2, 55));

        String q = searchText.trim();
        int avail = winH - 56 - 40;
        int rowH = Mth.clamp(avail / Math.max(1, pages()), 18, 25);
        int y = wy + 56;
        int[] ys = new int[pages()];
        for (int i = 0; i < pages(); i++) { ys[i] = y; y += rowH; }

        int targetY = ys[Mth.clamp(page, 0, pages() - 1)];
        pillY = pillY < 0f ? targetY : pillY + (targetY - pillY) * (1f - (float) Math.exp(-dt / 70f));
        if (q.isEmpty()) {
            rr(ctx, sx + 5, Math.round(pillY), sbw - 10, rowH, 4,
                    LovenoteMenuConfig.style.equals("Moon") ? 0xFF292431 : LovenotePalette.alpha(LovenotePalette.C2, 70));
            rr(ctx, sx + 5, Math.round(pillY) + rowH / 2 - 7, 2, 14, 1, LovenotePalette.alpha(LovenotePalette.wave(0f), 230));
        }

        for (int i = 0; i < pages(); i++) {
            int ry = ys[i];
            boolean hov = hover && mx >= sx + 5 && mx < sx + sbw - 5 && my >= ry && my < ry + rowH;
            float hv = ease("pg" + i, hov ? 1f : 0f, 120);
            if (hv > 0.02f && i != page) rr(ctx, sx + 5, ry, sbw - 10, rowH, 6, LovenotePalette.alpha(LovenotePalette.C2, (int) (26 * hv)));
            float slide = (1f - Mth.clamp((tabMs - i * 12f) / 220f, 0f, 1f)) * 10f;
            ItemStack icon = i < RAIL_ICONS.length ? RAIL_ICONS[i] : new ItemStack(Items.PAINTING);
            ctx.item(icon, sx + 13 + (int) slide, ry + (rowH - 16) / 2);
            // Stage 3: persistent category labels and live module counts.
            String label = pageLabel(i);
            int count = 0;
            if (i < cats.size()) for (Row module : allModules) if (module.catIndex() == i) count++;
            String countText = i < cats.size() ? Integer.toString(count) : "";
            int labelX = sx + 35 + (int) slide;
            int countX = sx + sbw - 11 - font.width(countText);
            int labelWidth = Math.max(16, countX - labelX - 5);
            int labelColor = LovenotePalette.lerp(LovenotePalette.dim(), LovenotePalette.text(), Math.max(hv * 0.7f, i == page ? 1f : 0f));
            txt(ctx, trim(label, labelWidth), labelX, ry + (rowH - 8) / 2, labelColor);
            if (!countText.isEmpty()) txt(ctx, countText, countX, ry + (rowH - 8) / 2,
                    LovenotePalette.alpha(LovenotePalette.faint(), i == page ? 215 : 140));
            pageRects.add(new int[]{sx + 5, ry, sbw - 10, rowH, i});
        }
    }

    private void drawPremiumSettings(GuiGraphicsExtractor ctx, int mx, int my, float panelA, int corner) {
        final int baseX = premiumSettingsX();
        final int pw = premiumSettingsW();
        final int py = LovenoteMenuConfig.style.equals("Moon") ? moonContentTop : wy + 112;
        final int ph = LovenoteMenuConfig.style.equals("Moon") ? (wy + winH - 48 - py) : winH - 142;
        if (ph < 80) return;

        // Smoothstep entrance is spatial, not a shader or a per-frame allocation.
        // Everything including scissor and click regions derives from animated px.
        float entrance = Mth.clamp(v26DrawerProgress, 0f, 1f);
        entrance = entrance * entrance * (3f - 2f * entrance);
        final int px = baseX + Math.round((1f - entrance) * 12f);
        final int accent = LovenotePalette.C2;
        final int frameRadius = Math.max(10, Math.min(15, corner + 6));

        // One rounded glass surface plus one thin outline; avoid large overdraw layers.
        rr(ctx, px - 1, py - 1, pw + 2, ph + 2, frameRadius + 1,
                LovenotePalette.alpha(accent, 68));
        rr(ctx, px, py, pw, ph, frameRadius, 0xE0161A29);
        fill(ctx, px + 15, py + 2, px + pw - 15, py + 3, 0x2BFFFFFF);

        Row m = selected;
        if (m == null) {
            txtS(ctx, "INSPECTOR", px + 15, py + 11, LovenotePalette.alpha(accent, 220));
            txt(ctx, "Choose a module to configure", px + 15, py + 37, LovenotePalette.dim());
            panelMax = 0;
            return;
        }

        // Stronger hierarchy: context label, module name and useful description.
        txt(ctx, "MODULE SETTINGS", px + 15, py + 8, LovenotePalette.alpha(LovenotePalette.C3, 183));
        txtS(ctx, trim(m.title(), pw - 92), px + 15, py + 23, 0xFFF3F2FC);
        txt(ctx, trim(m.description(), pw - 30), px + 15, py + 44, LovenotePalette.dim());
        float on = ease(m.title() + ":s", m.active() ? 1f : 0f, 170);
        drawSwitch(ctx, px + pw - 59, py + 24, on);
        addHit(px + pw - 64, py + 19, 34, 21, K_PTOGGLE, m, null, 0);
        // Close action is a quiet circular cross rather than a red danger dot.
        rr(ctx, px + pw - 27, py + 19, 17, 18, 6, 0xB93B344A);
        txt(ctx, "x", px + pw - 21, py + 24, 0xFFE7DEEE);
        addHit(px + pw - 29, py + 18, 20, 21, K_PCLOSE, m, null, 0);
        fill(ctx, px + 13, py + 61, px + pw - 13, py + 62, 0x3A7A7493);

        final int top = py + 68, bottom = py + ph - 11;
        final int settingW = pw - 30;
        final int total = 4 + panelTotal(m, settingW);
        panelMax = Math.max(0, total - (bottom - top));
        panelScrollTarget = Mth.clamp(panelScrollTarget, 0, panelMax);
        panelScrollShown += (panelScrollTarget - panelScrollShown) *
                (1f - (float) Math.exp(-Math.max(0f, dt) / 78f));
        if (Math.abs(panelScrollTarget - panelScrollShown) < 0.2f) panelScrollShown = panelScrollTarget;
        int firstHit = hits.size();
        ctx.enableScissor(px, top, px + pw, bottom);
        int y = top + 5 - Math.round(panelScrollShown);
        boolean first = true;
        for (Group group : m.groups()) {
            boolean any = false;
            for (Object st : group.settings()) if (rowH(st, settingW) > 0) { any = true; break; }
            if (!any) continue;
            if (!first) fill(ctx, px + 15, y - 2, px + pw - 15, y - 1, 0x305C5874);
            first = false;
            y += 5;
            txt(ctx, group.name(), px + 15, y,
                    LovenotePalette.alpha(LovenotePalette.C3, 153));
            y += 14;
            for (Object st : group.settings()) y += drawSetting(ctx, m, st, px + 15, y, settingW);
        }
        ctx.disableScissor();
        for (int i = hits.size() - 1; i >= firstHit; i--) {
            Hit hit = hits.get(i);
            if (hit.y < top || hit.y + hit.h > bottom) hits.remove(i);
        }
        scrollbar(ctx, px + pw - 7, top, bottom, panelScrollShown, panelMax);
    }

    // ---------------------------------------------------------------- input

    private boolean over(Hit h, double mx, double my) { return mx >= h.x && mx <= h.x + h.w && my >= h.y && my <= h.y + h.h; }

    private void applyAccent(int idx) {
        if (idx >= 0 && idx < LovenotePalette.PRESETS.length) LovenoteMenuConfig.accentIndex = idx;
        LovenoteMenuConfig.apply();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if(drainerHudEditor) return hudClick(click.x(),click.y(),click.button());
        if(macroGuideOpen) return macroGuideClick(click.x(),click.y());
        double mx = click.x() / gs, my = click.y() / gs;
        int button = click.button();

        if (listening != null) { listening.setKey(button == 1 ? -1 : GLFW.GLFW_MOUSE_BUTTON_LEFT); listening = null; return true; }
        if (button == 1 && selected != null) { selected = null; panelModule = null; return true; }
        if (button != 0 && button != 1) return super.mouseClicked(click, doubled);

        searchFocused = false;

        if (configExpanded && page == cats.size() + 2) {
            // Expanded panel is modal: underlying module/sidebar controls cannot be clicked.
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit h = hits.get(i);
                if (h.kind == K_CONFIG && over(h, mx, my)) { configAction(h.idx); return true; }
            }
            return true;
        }
        for (int[] r : pageRects) {
            if (mx >= r[0] && mx <= r[0] + r[2] && my >= r[1] && my <= r[1] + r[3]) {
                page = r[4]; configExpanded = false; tabMs = 0; scrollTarget = 0; scrollShown = 0; searchText = "";
                selected = null; panelModule = null; panelScrollTarget = 0;
                addRipple((int) mx, (int) my);
                return true;
            }
        }

        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit hit = hits.get(i);
            if (!over(hit, mx, my)) continue;
            if (hit.kind == K_NONE) continue;
            switch (hit.kind) {
                case K_CARD -> { selected = hit.module; panelModule = hit.module; panelScrollTarget = 0; expandedColor = null; hexEditing = null; }
                case K_SWITCH -> hit.module.toggle();
                case K_BOOL -> {
                    if (hit.setting instanceof BoolSetting b) b.set(!b.get());
                    else if (hit.setting instanceof com.lovenote.setting.BoolSetting b) b.toggle();
                }
                case K_MULTI -> { if (hit.setting instanceof com.lovenote.setting.MultiSetting ms) ms.toggle(hit.idx); }
                case K_SLIDER -> { dragWin = 3; dragging = hit; setSlider(hit, mx); }
                case K_COLOR_OPEN -> {
                    expandedColor = expandedColor == hit.setting ? null : (ColorSetting) hit.setting;
                    hexEditing = null;
                }
                case K_COLOR_CHANNEL -> { dragWin = 4; dragging = hit; previewColorSlider(hit, mx); hexEditing = null; }
                case K_COLOR_HEX -> {
                    ColorSetting col = (ColorSetting) hit.setting;
                    if (hexEditing == col) applyHexColor();
                    else { hexEditing = col; hexInput = colorHex(col.get()).substring(1); }
                }
                case K_COLOR_PRESET -> {
                    var old = ((ColorSetting) hit.setting).get();
                    int rgb = COLOR_PRESETS[hit.idx];
                    ((ColorSetting) hit.setting).set(new meteordevelopment.meteorclient.utils.render.color.SettingColor(
                        (rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, old.a));
                    hexEditing = null;
                }
                case K_COLOR_RESET -> { ((ColorSetting) hit.setting).reset(); hexEditing = null; }
                case K_COLOR_RAINBOW -> {
                    ColorSetting setting = (ColorSetting) hit.setting;
                    var col = new meteordevelopment.meteorclient.utils.render.color.SettingColor(setting.get());
                    col.rainbow = !col.rainbow;
                    setting.set(col);
                    hexEditing = null;
                }
                case K_MODE -> {
                    if (hit.setting instanceof EnumSetting<?> es) {
                        Object[] vals = enumValues(es);
                        if (hit.idx >= 0 && hit.idx < vals.length) setEnum(es, vals[hit.idx]);
                    } else if (hit.setting instanceof com.lovenote.setting.ModeSetting ms) ms.setIndex(hit.idx);
                }
                case K_PTOGGLE -> hit.module.toggle();
                case K_PCLOSE -> { selected = null; panelModule = null; }
                case K_BIND -> { if (hit.module != null) listening = hit.module; }
                case K_ACCENT -> applyAccent(hit.idx);
                case K_CONFIG -> configAction(hit.idx);
                case K_SEARCH -> { searchFocused = true; return true; }
                case K_EDITOR -> enterHudEditor();
                case K_GUIDE -> {macroGuideOpen=true;macroWorkspaceTab=hit.idx==3?3:0;macroLibraryMode=false;macroGuideSelected=0;macroGuideScroll=0;macroFeedback=""; if(macroWorkspaceTab==3) {var assist=com.lovenote.Lovenote.modules==null?null:com.lovenote.Lovenote.modules.get(com.lovenote.module.impl.SchematicAssist.class); schematicFiles=assist==null?java.util.List.of():assist.files(); schematicSelected=0; schematicScroll=0;}}
                case K_CLOSE -> close();
                default -> { }
            }
            return true;
        }

        if (button == 0 && mx >= wx && mx <= wx + winW && my >= wy && my <= wy + 34) {
            dragWin = 1; grabX = (int) mx - wx; grabY = (int) my - wy; return true;
        }
        return super.mouseClicked(click, doubled);
    }

    private void setSlider(Hit h, double mx) {
        double t = Mth.clamp((mx - h.x) / (double) h.w, 0.0, 1.0);
        if (h.setting instanceof IntSetting n) n.set((int) (n.sliderMin + t * (n.sliderMax - n.sliderMin)));
        else if (h.setting instanceof DoubleSetting n) n.set(n.sliderMin + t * (n.sliderMax - n.sliderMin));
        else if (h.setting instanceof com.lovenote.setting.NumberSetting n) n.set(n.min + t * (n.max - n.min));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double dx, double dy) {
        if(drainerHudEditor) return hudDrag(click.x(),click.y());
        if(macroGuideOpen) return true;
        double mx = click.x() / gs, my = click.y() / gs;
        if (dragWin == 1) { mainX = (int) mx - grabX; mainY = (int) my - grabY; clampWindows(); return true; }
        if (dragWin == 2) { panX = (int) mx - grabX; panY = (int) my - grabY; clampWindows(); return true; }
        if (dragWin == 3) {
            if (dragging != null) setSlider(dragging, mx);
            return true;
        }
        if (dragWin == 4) {
            if (dragging != null) previewColorSlider(dragging, mx);
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if(macroGuideOpen)return true;
        if(drainerHudEditor) {
            if(hudDragging instanceof com.lovenote.module.impl.SpotifyHud sp)sp.editorStopDrag();
            if(hudDragging instanceof com.lovenote.module.impl.Lyrics ly)ly.editorStopDrag();
            hudDragging=null;hudSlider=null;hudResizing=false;return true;
        }
        if (dragWin == 4 && dragging != null) commitColorSlider(dragging);
        dragWin = 0; dragging = null; return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double amount) {
        if(macroGuideOpen) {
            if(macroWorkspaceTab==3)schematicScroll=Math.max(0,schematicScroll-(int)Math.signum(amount)*2);
            else macroGuideScroll=Math.max(0,macroGuideScroll-(int)Math.signum(amount)*2);
            return true;
        }
        if(drainerHudEditor) {
            if(mx >= hudInspectorLeft && hudSelected!=null)hudSettingsScroll=Math.max(0,Math.min(hudSelected.settings.size()-1,hudSettingsScroll-(int)Math.signum(amount)));
            else hudListScroll=Math.max(0,hudListScroll-(int)Math.signum(amount)*2);
            return true;
        }
        // Scroll callbacks receive GUI-scaled mouse coordinates, like MouseButtonEvent.
        // Do not divide by Window.guiScale() again: that sent settings scroll into
        // the module list unless the cursor was far outside the actual panel.
        double x = mx / gs, y = my / gs;
        if (InputConstants.isKeyDown(windowHandle(), GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(windowHandle(), GLFW.GLFW_KEY_RIGHT_CONTROL)) {
            LovenoteMenuConfig.menuScale = (int) Mth.clamp(LovenoteMenuConfig.menuScale + amount * 5, 20, 110);
            return true;
        }
        if (selected != null && x >= premiumSettingsX() - 5 && x <= premiumSettingsX() + premiumSettingsW() + 5
                && y >= wy + 105 && y <= wy + winH - 22) {
            panelScrollTarget = Mth.clamp(panelScrollTarget - (float) amount * 24f, 0f, (float) panelMax);
            return true;
        }
        scrollTarget = Mth.clamp(scrollTarget - (float) amount * 28f, 0f, maxScroll);
        sbAlpha = 1f;
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if(macroGuideOpen) {
            if(input.key()==GLFW.GLFW_KEY_ESCAPE){
                if(macroGuideTyping)macroGuideTyping=false;
                else if(macroNameTyping)macroNameTyping=false;
                else macroGuideOpen=false;
            }else if(input.key()==GLFW.GLFW_KEY_BACKSPACE){
                if(macroNameTyping && !macroSaveName.isEmpty())macroSaveName=macroSaveName.substring(0,macroSaveName.length()-1);
                else if(macroGuideTyping && !macroGuideQuery.isEmpty()){
                    macroGuideQuery=macroGuideQuery.substring(0,macroGuideQuery.length()-1);macroGuideScroll=0;
                }
            }else if(input.key()==GLFW.GLFW_KEY_ENTER){
                if(macroNameTyping)saveMacroFromUi();
                macroNameTyping=false;macroGuideTyping=false;
            }
            return true;
        }
        if(drainerHudEditor && input.key()==GLFW.GLFW_KEY_ESCAPE) {
            drainerHudEditor=false;hudDragging=null;hudSlider=null;return true;
        }
        int key = input.key();
        if(appearanceHexField!=0){
            if(key==GLFW.GLFW_KEY_ESCAPE)appearanceHexField=0;
            else if(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER)applyAppearanceHex();
            else if(key==GLFW.GLFW_KEY_BACKSPACE&&!appearanceHexInput.isEmpty())appearanceHexInput=appearanceHexInput.substring(0,appearanceHexInput.length()-1);
            return true;
        }
        if (hexEditing != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) hexEditing = null;
            else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) applyHexColor();
            else if (key == GLFW.GLFW_KEY_BACKSPACE && !hexInput.isEmpty()) hexInput = hexInput.substring(0, hexInput.length() - 1);
            return true;
        }
        if (listening != null) { listening.setKey(key == GLFW.GLFW_KEY_ESCAPE ? -1 : key); listening = null; return true; }
        if (profileTyping && profilesExpanded) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !profileName.isEmpty()) profileName = profileName.substring(0, profileName.length() - 1);
            else if (key == GLFW.GLFW_KEY_ENTER) configAction(23);
            else if (key == GLFW.GLFW_KEY_ESCAPE) profileTyping = false;
            return true;
        }
        if (searchFocused) {
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!searchText.isEmpty()) { searchText = searchText.substring(0, searchText.length() - 1); onSearch(); }
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER) { searchFocused = false; return true; }
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (searchFocused) { searchFocused = false; return true; }
            if (profilesExpanded) { profilesExpanded = false; return true; }
            if (configExpanded) { configExpanded = false; return true; }
            close(); return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (appearanceHexField!=0) {
            int cp=input.codepoint();
            if(appearanceHexInput.length()<6 && Character.digit(cp,16)>=0)appearanceHexInput+=Character.toUpperCase((char)cp);
            return true;
        }
        if (hexEditing != null) {
            int cp = input.codepoint();
            if (hexInput.length() < 8 && Character.digit(cp, 16) >= 0) hexInput += Character.toUpperCase((char) cp);
            return true;
        }
        if(macroGuideOpen){
            int cp=input.codepoint();
            if(macroNameTyping && cp>=32 && cp<127 && macroSaveName.length()<36)macroSaveName+=(char)cp;
            else if(macroGuideTyping && cp>=32 && cp<127 && macroGuideQuery.length()<36){macroGuideQuery+=(char)cp;macroGuideScroll=0;}
            return true;
        }
        if (profileTyping && profilesExpanded) {
            int cp = input.codepoint();
            if (cp >= 32 && cp < 127 && profileName.length() < 32) profileName += (char) cp;
            return true;
        }
        if (searchFocused) {
            int cp = input.codepoint();
            if (cp >= 32 && searchText.length() < 32) { searchText += (char) cp; onSearch(); return true; }
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public void mouseMoved(double mx, double my) {
    }
}
