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

/** Faithful port of the old client's LovenoteScreen, on vanilla GuiGraphicsExtractor. */
public class LovenoteModulesScreen extends TabScreen {
    private static final int PW = 208, PH = 320, ACC_COLS = 11, RAIL_MIN = 46;
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
            K_PTOGGLE = 6, K_PCLOSE = 7, K_BIND = 8, K_ACCENT = 9, K_EDITOR = 10, K_CLOSE = 12, K_SEARCH = 13, K_MULTI = 5, K_CONFIG = 14, K_GUIDE = 15;

    private static final class Hit {
        int x, y, w, h, kind, idx;
        Row module;
        Object setting;
    }

    private static final class Ripple { int x, y; long t; }

    private final List<com.lovenote.module.Category> cats = new ArrayList<>();
    private final List<Row> allModules = new ArrayList<>();
    private final List<Hit> hits = new ArrayList<>();
    private final List<int[]> pageRects = new ArrayList<>();
    private final List<Ripple> ripples = new ArrayList<>();
    private final Map<String, Float> anim = new HashMap<>();

    private int wx, wy, winW = 600, winH = 372, railW = 118;
    private float gs = 1f;
    private int vw, vh;
    private float dt, ga = 1f, openMs, closeMs, tabMs, scrollTarget, scrollShown, panelScrollTarget, panelScrollShown;
    private float pillY = -1f;
    private int maxScroll, panelMax;
    private boolean closing, finishClose, animating;
    private long lastNano;
    private Row selected, panelModule, listening;
    private Hit dragging;
    private int dragWin, grabX, grabY;
    private boolean settingFields;
    private String tooltip;
    private float sbAlpha = 0.45f;

    private float[] px, py, pvx, pvy;
    private int particleCount = 45;
    private String searchText = "";
    private boolean searchFocused;
    // Drainer-native HUD editor: independent of Meteor's HUD implementation.
    private boolean macroGuideOpen;
    private int macroGuideSelected, macroGuideScroll;
    private int macroTemplateSelected;
    private boolean macroLibraryMode, macroGuideTyping;
    private String macroGuideQuery="", macroFeedback="";
    private final java.util.List<int[]> macroGuideHits = new java.util.ArrayList<>();
    private boolean drainerHudEditor;
    private com.lovenote.module.Module hudSelected;
    private com.lovenote.module.Module hudDragging;
    private com.lovenote.setting.NumberSetting hudSlider;
    private int hudListScroll, hudSettingsScroll;
    private boolean hudResizing;
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
        hudHits.add(new HudHit(x,y,w,h,action,m,st));
    }
    private void enterHudEditor() {
        drainerHudEditor=true;
        hudSelected=com.lovenote.Lovenote.modules.get(com.lovenote.module.impl.SpotifyHud.class);
        hudDragging=null;hudSlider=null;hudListScroll=0;hudSettingsScroll=0;
    }
    private int hudWidgetX(com.lovenote.module.Module m) {
        if(m instanceof com.lovenote.module.impl.SpotifyHud sp)return sp.editorX();
        if(m instanceof com.lovenote.module.impl.Lyrics lr)return lr.editorX();
        var setting=hudNumber(m,"X");return setting==null?-1:(int)(setting.get()*width);
    }
    private int hudWidgetY(com.lovenote.module.Module m) {
        if(m instanceof com.lovenote.module.impl.SpotifyHud sp)return sp.editorY();
        if(m instanceof com.lovenote.module.impl.Lyrics lr)return lr.editorY();
        var setting=hudNumber(m,"Y");return setting==null?-1:(int)(setting.get()*height);
    }
    private int hudWidgetW(com.lovenote.module.Module m) {
        if(m instanceof com.lovenote.module.impl.SpotifyHud sp)return sp.editorWidth();
        if(m instanceof com.lovenote.module.impl.Lyrics lr)return lr.editorWidth();
        return 160;
    }
    private int hudWidgetH(com.lovenote.module.Module m) {
        if(m instanceof com.lovenote.module.impl.SpotifyHud sp)return sp.editorHeight();
        if(m instanceof com.lovenote.module.impl.Lyrics lr)return lr.editorHeight();
        return 30;
    }
    private void hudMoveModule(com.lovenote.module.Module m,double x,double y) {
        var sx=hudNumber(m,"X");var sy=hudNumber(m,"Y");
        if(sx!=null)sx.set(Math.max(0,Math.min(1,x/Math.max(1,width))));
        if(sy!=null)sy.set(Math.max(0,Math.min(1,y/Math.max(1,height))));
    }
    // V12: real on-screen editor. Compact glass controls; HUD remains visible for direct manipulation.
    private void hudDraw(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta) {
        ga=1f;
        hudHits.clear();
        g.fill(0,0,width,height,0x21080716);
        // Avoid a full-screen modal: render the actual HUD as an editable, softly tinted preview.
        if(com.lovenote.Lovenote.modules!=null) {
            for(com.lovenote.module.Module m:hudModules())if(m.enabled)
                com.lovenote.util.HudScale.draw(m,g,delta);
        }
        int top=11;
        neon(g,12,top,204,31,12,0xEB110D23,0xC06C46A2);
        txtS(g,"DRAINER  /  HUD",24,top+10,0xFFFFD8F7);
        txt(g,"ESC TO CLOSE",224,top+19,0xFFBBA3CF);
        // Library dock: ghosted, close to the bottom of the screen as in the original editor.
        java.util.List<com.lovenote.module.Module> mods=hudModules();
        int dockX=12,dockH=56,dockY=height-dockH-12;
        int inspectorW=Math.min(230,Math.max(176,width/4));
        int dockW=Math.max(110,width-inspectorW-40);
        neon(g,dockX,dockY,dockW,dockH,10,0xC4141026,0x904E3371);
        txt(g,"WIDGETS  •  CLICK TO SELECT  •  DRAG TO MOVE",dockX+12,dockY+8,0xFFD3A9E3);
        int chipY=dockY+26,chipX=dockX+11,chipH=22,gap=5;
        int maxVisible=dockX+dockW-10;
        hudListScroll=Math.max(0,Math.min(Math.max(0,mods.size()-1),hudListScroll));
        g.enableScissor(dockX+8,chipY-2,maxVisible,dockY+dockH-4);
        for(int i=hudListScroll;i<mods.size();i++) {
            var m=mods.get(i);
            int chipW=Math.max(49,Math.min(132,font.width(m.name)+30));
            if(chipX+chipW>maxVisible)break;
            boolean sel=m==hudSelected;
            neon(g,chipX,chipY,chipW,chipH,8,sel?0xE142244D:0xA8221A32,
                 sel?0xCFE69CF7:0x4048356A);
            txt(g,(m.enabled?"● ":"○ ")+m.name,chipX+7,chipY+7,
                sel?0xFFFFE8FB:m.enabled?0xFFDEC8F2:0xFF9783AD);
            hudHit(chipX,chipY,chipW,chipH,1,m,null);
            chipX+=chipW+gap;
        }
        g.disableScissor();
        // Properties inspector: compact, independently scrollable, with universal per-widget scale.
        int px=width-inspectorW-12;
        int py=11,ph=Math.max(170,Math.min(height-dockH-34,292));
        neon(g,px,py,inspectorW,ph,12,0xE7131023,0xA0603987);
        txtS(g,"WIDGET SETTINGS",px+13,py+12,0xFFFFE0F8);
        if(hudSelected==null) {
            txt(g,"Choose a widget below",px+13,py+49,0xFFB69DC9);
        } else {
            String title=hudSelected.name;
            while(title.length()>5&&font.width(title)>inspectorW-27)title=title.substring(0,title.length()-2);
            txt(g,title,px+12,py+35,0xFFF5B3EC);
            neon(g,px+inspectorW-45,py+53,32,18,8,
                 hudSelected.enabled?0xFF773E90:0xFF33243F,0x70674A9B);
            txt(g,hudSelected.enabled?"ON":"OFF",px+inspectorW-40,py+59,0xFFFFE4FA);
            hudHit(px+inspectorW-45,py+53,32,18,2,hudSelected,null);
            txt(g,"SIZE",px+13,py+80,0xFFE4BDD9);
            double current=com.lovenote.util.HudScale.get(hudSelected);
            txt(g,String.format(java.util.Locale.ROOT,"%d%%",(int)Math.round(current*100)),
                 px+inspectorW-50,py+80,0xFFFFD6FB);
            int sliderX=px+13,sliderY=py+96,sliderW=inspectorW-26;
            rr(g,sliderX,sliderY,sliderW,3,2,0xFF3D3156);
            int activeW=(int)Math.round(sliderW*(current-.30)/2.20);
            rr(g,sliderX,sliderY,Math.max(2,activeW),3,2,0xFFF599EF);
            rr(g,sliderX+activeW-3,sliderY-3,8,9,4,0xFFECADFA);
            hudHit(sliderX,sliderY-8,sliderW,20,14,hudSelected,null);
            txt(g,"30%",sliderX,sliderY+12,0xFF9983B5);
            txt(g,"250%",px+inspectorW-40,sliderY+12,0xFF9983B5);
            fill(g,px+12,py+133,px+inspectorW-12,py+134,0x785B3974);
            int y=py+143, end=py+ph-40, lineW=inspectorW-25;
            int settingIndex=0;
            g.enableScissor(px+5,y-4,px+inspectorW-6,Math.max(y,end));
            for(com.lovenote.setting.Setting st:hudSelected.settings) {
                if(settingIndex++<hudSettingsScroll)continue;
                if(y+30>end)break;
                if(st instanceof com.lovenote.setting.NumberSetting n) {
                    String label=st.name;
                    while(label.length()>5&&font.width(label)>lineW-48)label=label.substring(0,label.length()-2);
                    txt(g,label,px+13,y,0xFFE6D5EF);
                    txt(g,String.format(java.util.Locale.ROOT,"%.2f",n.get()),px+inspectorW-48,y,0xFFF2B9EF);
                    rr(g,px+13,y+15,lineW,3,2,0xFF493653);
                    int fw=(int)(lineW*Math.max(0,Math.min(1,(n.get()-n.min)/Math.max(.001,n.max-n.min))));
                    if(fw>0)rr(g,px+13,y+15,fw,3,2,0xFFE9A1EB);
                    hudHit(px+13,y+10,lineW,15,3,hudSelected,st);
                    y+=30;
                } else if(st instanceof com.lovenote.setting.BoolSetting b) {
                    txt(g,st.name,px+13,y+5,0xFFDFCCE4);
                    txt(g,b.get()?"ON":"OFF",px+inspectorW-37,y+5,b.get()?0xFFEEA8E8:0xFF9587A7);
                    hudHit(px+13,y,lineW,24,4,hudSelected,st);
                    y+=26;
                } else if(st instanceof com.lovenote.setting.ModeSetting mode) {
                    String value=mode.modes.length==0?"None":mode.modes[Math.max(0,Math.min(mode.modes.length-1,mode.index()))];
                    txt(g,st.name,px+13,y,0xFFD2B2DC);
                    txt(g,value+"  ›",px+13,y+12,0xFFF5D3F6);
                    hudHit(px+13,y,lineW,27,10,hudSelected,st);
                    y+=30;
                }
            }
            g.disableScissor();
            neon(g,px+12,py+ph-31,inspectorW-24,21,7,0xFF3D2850,0x905E4287);
            txt(g,"CENTER",px+20,py+ph-25,0xFFFFD9F5);
            hudHit(px+12,py+ph-31,inspectorW-24,21,5,hudSelected,null);
        }
        // Selected widget outline + resize handle (drag lower-right corner).
        if(hudSelected!=null&&hudSelected.enabled) {
            int bx=hudWidgetX(hudSelected),by=hudWidgetY(hudSelected);
            if(bx>=0&&by>=0) {
                double sc=com.lovenote.util.HudScale.get(hudSelected);
                int ww=(int)Math.round(hudWidgetW(hudSelected)*sc);
                int hh=(int)Math.round(hudWidgetH(hudSelected)*sc);
                // Minimal glow passes so enabling the HUD editor doesn't tank FPS.
                rr(g,bx-2,by-2,ww+4,hh+4,8,0x704F2A75);
                fill(g,bx,by,bx+ww,by+1,0xFFE8ACFA);
                fill(g,bx,by+hh-1,bx+ww,by+hh,0xFFE8ACFA);
                fill(g,bx,by,bx+1,by+hh,0xFFE8ACFA);
                fill(g,bx+ww-1,by,bx+ww,by+hh,0xFFE8ACFA);
                neon(g,bx+ww-7,by+hh-7,12,12,4,0xFFE9B9F2,0xFFAC51DA);
                hudHit(bx+ww-9,by+hh-9,18,18,15,hudSelected,null);
            }
        }
        txt(g,"SCROLL DOCK TO BROWSE  •  DRAG CORNER TO RESIZE",18,dockY-14,0xFFB7A2C8);
    }

    private void neon(GuiGraphicsExtractor g,int x,int y,int w,int h,int r,int fill,int edge) {
        if(w<=2||h<=2)return;
        rr(g,x-2,y-2,w+4,h+4,r+2,(edge&0x00FFFFFF)|0x16000000);
        rr(g,x-1,y-1,w+2,h+2,r+1,edge);
        rr(g,x,y,w,h,r,fill);
        rr(g,x+1,y+1,w-2,h-2,Math.max(2,r-1),0x251A1234);
    }
    // V12: Macro Guide and real (four-template) native Macro Library share one small glass window.
    private void macroGuideDraw(GuiGraphicsExtractor g) {
        macroGuideHits.clear();
        g.fill(0,0,width,height,0x54080510);
        int boxW=Math.min(width-42,670),boxH=Math.min(height-42,370);
        int boxX=(width-boxW)/2,boxY=(height-boxH)/2;
        neon(g,boxX,boxY,boxW,boxH,13,0xEF100E1F,0xC57E3DAD);
        txtS(g,"DRAINER  /  MACROS",boxX+17,boxY+12,0xFFFFDAFA);
        int tabX=boxX+Math.max(205,boxW-269);
        for(int i=0;i<2;i++) {
            int xx=tabX+i*86;
            boolean active=(i==1)==macroLibraryMode;
            neon(g,xx,boxY+9,80,23,7,active?0xE050245D:0xBD22182F,active?0xBFE89DF8:0x503E315F);
            txt(g,i==0?"GUIDE":"LIBRARY",xx+12,boxY+17,active?0xFFFFE8FF:0xFFBFA4CC);
            macroGuideHits.add(new int[]{xx,boxY+9,80,23,i==0?-2:-3});
        }
        int xClose=boxX+boxW-28;
        txtS(g,"×",xClose,boxY+15,0xFFECCCEF);
        macroGuideHits.add(new int[]{xClose-6,boxY+8,26,25,-1});
        neon(g,boxX+16,boxY+42,Math.min(262,boxW/2),23,8,0xB21D172F,0x72533178);
        String placeholder="SEARCH  "+(macroLibraryMode?"TEMPLATES":"GUIDES")+"...";
        String query=macroGuideQuery.isEmpty()?placeholder:macroGuideQuery;
        txt(g,query,boxX+26,boxY+50,macroGuideQuery.isEmpty()?0xFF9A85B1:0xFFF3DFF8);
        macroGuideHits.add(new int[]{boxX+16,boxY+42,Math.min(262,boxW/2),23,-4});
        txt(g,macroLibraryMode?"4 DEMO PRESETS • SINGLE PLAYER TESTING":"REFERENCE ENTRIES • INFORMATION ONLY",
            boxX+18,boxY+72,0xFFA68AB9);
        int paneX=boxX+16,paneY=boxY+90,paneW=Math.max(148,Math.min(215,boxW/3)),paneH=boxH-107,rowH=29;
        neon(g,paneX,paneY,paneW,paneH,10,0xE0181426,0x56483063);
        var entries=com.lovenote.module.MacroGuideCatalog.ALL;
        String[] templates={"Walk 2s","Left and right","Jump once","Stand still"};
        java.util.List<Integer> indices=new java.util.ArrayList<>();
        String q=macroGuideQuery.toLowerCase(java.util.Locale.ROOT).trim();
        int count=macroLibraryMode?templates.length:entries.size();
        for(int i=0;i<count;i++) {
            String name=macroLibraryMode?templates[i]:entries.get(i).name();
            if(q.isEmpty() || name.toLowerCase(java.util.Locale.ROOT).contains(q)
                    || (!macroLibraryMode && entries.get(i).group().toLowerCase(java.util.Locale.ROOT).contains(q))) indices.add(i);
        }
        int rows=Math.max(1,(paneH-13)/rowH);
        macroGuideScroll=Math.max(0,Math.min(Math.max(0,indices.size()-rows),macroGuideScroll));
        g.enableScissor(paneX+3,paneY+4,paneX+paneW-3,paneY+paneH-5);
        for(int row=macroGuideScroll;row<indices.size()&&row<macroGuideScroll+rows;row++) {
            int i=indices.get(row),yy=paneY+8+(row-macroGuideScroll)*rowH;
            boolean selected=macroLibraryMode?i==macroTemplateSelected:i==macroGuideSelected;
            neon(g,paneX+6,yy-2,paneW-12,rowH-3,7,
                selected?0xC045285A:0x90231933,selected?0x94C475E9:0x264B3561);
            String name=macroLibraryMode?templates[i]:entries.get(i).name();
            while(name.length()>5&&font.width(name)>paneW-26)name=name.substring(0,name.length()-2);
            txt(g,name,paneX+14,yy+6,selected?0xFFFFE8FB:0xFFD1BEDF);
            macroGuideHits.add(new int[]{paneX+6,yy-2,paneW-12,rowH-3,macroLibraryMode?-10-i:i});
        }
        g.disableScissor();
        int detailX=paneX+paneW+11,detailW=boxX+boxW-16-detailX;
        neon(g,detailX,paneY,detailW,paneH,10,0xEC181328,0x634E3575);
        int tx=detailX+14,maxWidth=Math.max(65,detailW-28);
        g.enableScissor(detailX+5,paneY+5,detailX+detailW-5,paneY+paneH-7);
        if(macroLibraryMode){
            String name=templates[Math.max(0,Math.min(templates.length-1,macroTemplateSelected))];
            txtS(g,name,tx,paneY+14,0xFFFFD9F5);
            txt(g,"RECORDER TEMPLATE",tx,paneY+38,0xFFA97CBD);
            fill(g,tx,paneY+55,tx+maxWidth,paneY+56,0x8055386C);
            String detail=switch(name){
                case "Walk 2s" -> "Moves forward for 40 ticks.";
                case "Left and right" -> "Moves left for 40 ticks, then right for 40 ticks.";
                case "Jump once" -> "Waits, jumps, then stops.";
                default -> "Records a short idle sequence for local testing.";
            };
            macroTextBlock(g,"HOW IT WORKS",detail,tx,paneY+68,maxWidth);
            macroTextBlock(g,"ABOUT", "These are real native demo templates. Farming and mining names without recorded routes are references only.",tx,paneY+132,maxWidth);
            int loadY=paneY+paneH-43;
            neon(g,tx,loadY,Math.max(90,maxWidth),26,8,0xDE733987,0xBFD98EE9);
            txt(g,"LOAD INTO RECORDER",tx+10,loadY+8,0xFFFFE4FA);
            macroGuideHits.add(new int[]{tx,loadY,Math.max(90,maxWidth),26,-5});
            if(!macroFeedback.isEmpty())txt(g,macroFeedback,tx,loadY-15,0xFFBCE8C8);
        }else if(!entries.isEmpty()) {
            var entry=entries.get(Math.max(0,Math.min(entries.size()-1,macroGuideSelected)));
            String name=entry.name();
            while(name.length()>5&&font.width(name)>maxWidth)name=name.substring(0,name.length()-2);
            txtS(g,name,tx,paneY+14,0xFFFFCEE9);
            txt(g,entry.group().toUpperCase(java.util.Locale.ROOT),tx,paneY+37,0xFFC39ADB);
            fill(g,tx,paneY+55,tx+maxWidth,paneY+56,0x8055386C);
            int yy=paneY+65;
            yy=macroTextBlock(g,"WHAT IT DOES",entry.what(),tx,yy,maxWidth);
            yy=macroTextBlock(g,"HOW IT WORKS",entry.how(),tx,yy,maxWidth);
            macroTextBlock(g,"HOW TO USE",entry.usage(),tx,yy,maxWidth);
        }
        g.disableScissor();
    }

    private int macroTextBlock(GuiGraphicsExtractor g,String label,String value,int x,int y,int maxWidth) {
        txt(g,label,x,y,0xFFC18BD9);y+=17;
        StringBuilder line=new StringBuilder();
        for(String word:value.split(" ")) {
            String candidate=line.isEmpty()?word:line+" "+word;
            if(!line.isEmpty() && font.width(candidate)>maxWidth) {
                txt(g,line.toString(),x,y,0xFFE8DAEE);y+=12;line=new StringBuilder(word);
            } else line=new StringBuilder(candidate);
        }
        if(!line.isEmpty()){txt(g,line.toString(),x,y,0xFFE8DAEE);y+=12;}
        return y+17;
    }
    private boolean macroGuideClick(double x,double y) {
        for(int[] h:macroGuideHits) if(x>=h[0]&&x<h[0]+h[2]&&y>=h[1]&&y<h[1]+h[3]) {
            int action=h[4];
            if(action==-1)macroGuideOpen=false;
            else if(action==-2){macroLibraryMode=false;macroGuideScroll=0;macroFeedback="";}
            else if(action==-3){macroLibraryMode=true;macroGuideScroll=0;macroFeedback="";}
            else if(action==-4)macroGuideTyping=true;
            else if(action==-5) {
                var library=com.lovenote.Lovenote.modules.get(com.lovenote.module.impl.MacroLibrary.class);
                String[] templates={"Walk 2s","Left and right","Jump once","Stand still"};
                if(library!=null)macroFeedback=library.loadPreset(templates[macroTemplateSelected])?"LOADED":"RECORDER BUSY";
                else macroFeedback="LIBRARY UNAVAILABLE";
            }else if(action<=-10){macroTemplateSelected=Math.max(0,Math.min(3,-10-action));macroFeedback="";}
            else macroGuideSelected=action;
            return true;
        }
        macroGuideTyping=false;
        return true;
    }

    private void hudSetSlider(HudHit hit,double mx) {
        if(hit.setting instanceof com.lovenote.setting.NumberSetting n) {
            double t=Math.max(0,Math.min(1,(mx-hit.x)/(double)Math.max(1,hit.w)));
            n.set(n.min+t*(n.max-n.min));
        }
    }
    private boolean hudClick(double mx,double my,int button) {
        if(button!=0)return true;
        for(int i=hudHits.size()-1;i>=0;i--){
            HudHit hit=hudHits.get(i);if(!hit.contains(mx,my))continue;
            switch(hit.action) {
                case 1 -> {hudSelected=hit.module;hudSettingsScroll=0;}
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
            if(mx>=bx&&mx<bx+hudWidgetW(m)&&my>=by&&my<by+hudWidgetH(m)) {
                hudSelected=m;hudSettingsScroll=0;hudDragging=m;
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
        return p == cats.size() ? "Spotify" : p == cats.size() + 1 ? "Themes" : "Config";
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

    private void buildRows() {
        allModules.clear();
        for (com.lovenote.module.Module m : com.lovenote.Lovenote.modules.all()) {
            int i = m.category == com.lovenote.module.Category.SPOTIFY ? cats.size()
                    : RAGE_MODULES.contains(m.name) ? catIndex(com.lovenote.module.Category.RAGE)
                    : catIndex(m.category);
            if (i >= 0) allModules.add(new LRow(m, i));
        }
        for (Module m : Modules.get().getAll()) {
            int i = catIndex(mapCat(m.category.name));
            if (i >= 0) allModules.add(new MRow(m, i));
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

    private int contentX() { return LovenoteMenuConfig.railRight ? wx + 14 : wx + railW + 14; }
    // DRainer_STAGE2_THREE_COLUMN: fixed space for settings, even when no module is selected.
    private int premiumGap() { return 12; }
    private int premiumSettingsW() { return Math.max(180, Math.min(252, (winW - railW - 42) * 40 / 100)); }
    private int premiumSettingsX() { return wx + winW - 14 - premiumSettingsW(); }
    private int contentW() { return Math.max(140, premiumSettingsX() - premiumGap() - contentX()); }
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
        ctx.text(font, s, x, y, cc, false);
    }
    private void txtS(GuiGraphicsExtractor ctx, String s, int x, int y, int c) {
        int cc = a(c); if ((cc >>> 24) < 12) return;
        ctx.text(font, s, x, y, cc, true);
    }
    private void circle(GuiGraphicsExtractor ctx, int cx, int cy, int r, int c) { LovenoteDraw.circle(ctx, cx, cy, r, a(c)); }

    private float ease(String key, float target, float durMs) {
        float v = anim.getOrDefault(key, target);
        v += (target - v) * (1f - (float) Math.exp(-dt / Math.max(8f, durMs * 0.25f)));
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
        rr(ctx, x, y, 22, 10, 5, LovenotePalette.lerp(LovenotePalette.track(), LovenotePalette.C2, t));
        if (t > 0.02f) rr(ctx, x + 2, y + 1, 18, 8, 4, LovenotePalette.alpha(LovenotePalette.C2, (int) (255 * t)));
        rr(ctx, x + 1 + (int) (t * 12), y + 1, 8, 8, 4, 0xFFFFFFFF);
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

    private void drawSlider(GuiGraphicsExtractor ctx, int x, int y, int w, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int sy = y + 16;
        fill(ctx, x, sy, x + w, sy + 3, LovenotePalette.track());
        int fx = x + (int) (w * t);
        grad(ctx, x, sy, Math.max(x + 1, fx), sy + 3, LovenotePalette.C1, LovenotePalette.skeet(t));
        rr(ctx, Mth.clamp(fx - 3, x, x + w - 6), sy - 3, 6, 9, 3, 0xFFFFFFFF);
    }

    private void chipsMulti(GuiGraphicsExtractor ctx, com.lovenote.setting.MultiSetting ms, int x, int y, int w, Row owner) {
        int cx = 0, cy = 0;
        for (int i = 0; i < ms.names.length; i++) {
            int cw = font.width(ms.names[i]) + 12;
            if (cx + cw > w && cx > 0) { cx = 0; cy += 18; }
            int rx = x + cx, ry = y + cy;
            float t = ease(ms.hashCode() + ":" + i, ms.get(i) ? 1f : 0f, 150);
            rr(ctx, rx, ry, cw, 15, 3, LovenotePalette.lerp(LovenotePalette.chip(), LovenotePalette.C2, t));
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
            rr(ctx, rx, ry, cw, 15, 3, LovenotePalette.lerp(LovenotePalette.chip(), LovenotePalette.C2, t));
            if (t > 0.02f) rr(ctx, rx + 2, ry + 1, cw - 4, 13, 3, LovenotePalette.alpha(LovenotePalette.C2, (int) (255 * t)));
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

        double s = mc.getWindow().getGuiScale();
        int mx = (int) (mouseX / s), my = (int) (mouseY / s);

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

        ctx.fill(0, 0, width, height, LovenotePalette.alpha(0x000000, (int) (130 * eo)));
        drawDrainerAtmosphere(ctx, eo);
        for (int i = 0; i < particleCount && i < px.length; i++) {
            float sp = LovenoteMenuConfig.particleSpeed / 100f;
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
        float panelA = ease("panel", selected != null ? 1f : 0f, 220);

        drawWindow(ctx, mx, my, interactive, corner);
        if (page != cats.size() + 2) drawPremiumSettings(ctx, mx, my, panelA, corner);
        drawRail(ctx, mx, my, corner);
        drawDrainerStage5Ornaments(ctx);
        if (page == cats.size() + 2 && configExpanded) drawConfigExpanded(ctx);
        drawRipples(ctx);

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
    private void drawWindow(GuiGraphicsExtractor ctx, int mx, int my, boolean interactive, int corner) {
        int styleA = LovenoteMenuConfig.style.equals("Premium") ? 0xE8 : LovenoteMenuConfig.style.equals("Classic") ? 0xF8 : LovenoteMenuConfig.style.equals("Liquid") ? 0xCC : 0x8C;
        int alpha = (int) (styleA * LovenoteMenuConfig.opacity / 100.0);

        for (int i = LovenoteMenuConfig.glow; i >= 1; i--) rr(ctx, wx - i, wy - i, winW + i * 2, winH + i * 2, corner + 2, LovenotePalette.alpha(LovenotePalette.C2, 5));
        if (LovenoteMenuConfig.shadow) for (int i = 4; i >= 1; i--) rr(ctx, wx - i, wy + 2, winW + i * 2, winH + i * 2, corner + 2, LovenotePalette.alpha(0x000000, 8));
        rr(ctx, wx - 1, wy - 1, winW + 2, winH + 2, corner + 1, LovenotePalette.alpha(LovenotePalette.C2, 64));
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
            txt(ctx, "Search...", cx + 22, wy + 142, LovenotePalette.faint());
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
        rr(ctx, bx - 105, wy + 10, 91, 17, 5, LovenotePalette.alpha(LovenotePalette.chip(), 215));
        txt(ctx, "HUD Editor", bx - 97, wy + 14, LovenotePalette.text());
        addHit(bx - 105, wy + 10, 91, 17, K_EDITOR, null, null, 0);
        rr(ctx, bx - 202, wy + 10, 90, 17, 5, LovenotePalette.alpha(LovenotePalette.chip(), 215));
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
            drawThemePresets(ctx, cx, top, cw, bottom);
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
        List<Row> out = new ArrayList<>();
        for (Row m : allModules) {
            if (!q.isEmpty()) { if (m.title().toLowerCase().contains(q) || m.description().toLowerCase().contains(q)) out.add(m); }
            else if (m.catIndex() == page) out.add(m);
        }
        out.sort((x, y) -> x.title().compareToIgnoreCase(y.title()));
        return out;
    }

    private void renderCards(GuiGraphicsExtractor ctx, int mx, int my,
                             int cx, int top, int cw, int bottom,
                             String q, boolean interactive, int corner) {
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
        rr(ctx, x, y, w, h, 7, LovenotePalette.alpha(LovenotePalette.C2, 65));
        rr(ctx, x + 1, y + 1, w - 2, h - 2, 6, LovenotePalette.card());
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
        if (y + 165 < bottom) {
            txt(ctx, "ACTIVE THEME", x + 8, y + 142, LovenotePalette.faint());
            int idx = LovenoteMenuConfig.accentIndex;
            String name = idx >= 0 && idx < LovenotePalette.PRESET_NAMES.length
                    ? LovenotePalette.PRESET_NAMES[idx] : "Custom";
            txt(ctx, name, x + 8, y + 160, LovenotePalette.text());
            fill(ctx, x + 8, y + 178, x + w, y + 180, LovenotePalette.C1);
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
        rr(ctx, sx, wy, sbw, winH, corner, LovenotePalette.dark ? 0xFA0C0C12 : 0xFAEDEDF4);
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
            rr(ctx, sx + 5, Math.round(pillY), sbw - 10, rowH, 6, LovenotePalette.alpha(LovenotePalette.C2, 70));
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
        int px = premiumSettingsX();
        int pw = premiumSettingsW();
        int py = wy + 112;
        int ph = winH - 142;
        if (ph < 80) return;
        rr(ctx, px - 1, py - 1, pw + 2, ph + 2, corner, LovenotePalette.alpha(LovenotePalette.C2, 65));
        rr(ctx, px, py, pw, ph, corner, LovenotePalette.alpha(LovenotePalette.card(), 238));
        Row m = selected;
        if (m == null) {
            txtS(ctx, "Module Settings", px + 12, py + 13, LovenotePalette.text());
            fill(ctx, px + 10, py + 36, px + pw - 10, py + 37, LovenotePalette.alpha(LovenotePalette.track(), 170));
            txt(ctx, "Select a module", px + 12, py + 52, LovenotePalette.faint());
            txt(ctx, "to edit its settings", px + 12, py + 68, LovenotePalette.dim());
            panelMax = 0;
            return;
        }
        txtS(ctx, trim(m.title(), pw - 84), px + 12, py + 11, LovenotePalette.text());
        txt(ctx, trim(m.description(), pw - 24), px + 12, py + 25, LovenotePalette.faint());
        float on = ease(m.title() + ":s", m.active() ? 1f : 0f, 170);
        drawSwitch(ctx, px + pw - 64, py + 12, on);
        addHit(px + pw - 67, py + 8, 28, 18, K_PTOGGLE, m, null, 0);
        circle(ctx, px + pw - 17, py + 17, 4, 0xFFEF4444);
        addHit(px + pw - 23, py + 10, 13, 14, K_PCLOSE, m, null, 0);
        fill(ctx, px + 10, py + 38, px + pw - 10, py + 39, LovenotePalette.alpha(LovenotePalette.track(), 170));
        int top = py + 44, bottom = py + ph - 10;
        int settingW = pw - 26;
        int total = 4 + panelTotal(m, settingW);
        panelMax = Math.max(0, total - (bottom - top));
        panelScrollTarget = Mth.clamp(panelScrollTarget, 0, panelMax);
        panelScrollShown += (panelScrollTarget - panelScrollShown) * (1f - (float) Math.exp(-dt / 40f));
        if (Math.abs(panelScrollTarget - panelScrollShown) < 0.2f) panelScrollShown = panelScrollTarget;
        int firstHit = hits.size();
        ctx.enableScissor(px, top, px + pw, bottom);
        int y = top + 4 - Math.round(panelScrollShown);
        boolean first = true;
        for (Group g : m.groups()) {
            boolean any = false;
            for (Object st : g.settings()) if (rowH(st, settingW) > 0) { any = true; break; }
            if (!any) continue;
            if (!first) fill(ctx, px + 13, y - 2, px + pw - 13, y - 1, LovenotePalette.alpha(LovenotePalette.track(), 120));
            first = false;
            y += 4;
            txt(ctx, g.name(), px + 13, y, LovenotePalette.alpha(LovenotePalette.faint(), 190));
            y += 14;
            for (Object st : g.settings()) y += drawSetting(ctx, m, st, px + 13, y, settingW);
        }
        ctx.disableScissor();
        // Scissoring does not clip hitboxes: remove invisible controls from the input list.
        for (int i = hits.size() - 1; i >= firstHit; i--) {
            Hit h = hits.get(i);
            if (h.y < top || h.y + h.h > bottom) hits.remove(i);
        }
        scrollbar(ctx, px + pw - 6, top, bottom, panelScrollShown, panelMax);
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
                page = r[4]; configExpanded = false; tabMs = 0; scrollTarget = 0; scrollShown = 0; searchText = ""; addRipple((int) mx, (int) my);
                return true;
            }
        }

        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit hit = hits.get(i);
            if (!over(hit, mx, my)) continue;
            if (hit.kind == K_NONE) continue;
            switch (hit.kind) {
                case K_CARD -> { selected = hit.module; panelModule = hit.module; panelScrollTarget = 0; }
                case K_SWITCH -> hit.module.toggle();
                case K_BOOL -> {
                    if (hit.setting instanceof BoolSetting b) b.set(!b.get());
                    else if (hit.setting instanceof com.lovenote.setting.BoolSetting b) b.toggle();
                }
                case K_MULTI -> { if (hit.setting instanceof com.lovenote.setting.MultiSetting ms) ms.toggle(hit.idx); }
                case K_SLIDER -> { dragWin = 3; dragging = hit; setSlider(hit, mx); }
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
                case K_GUIDE -> {macroGuideOpen=true;macroLibraryMode=false;macroGuideSelected=0;macroGuideScroll=0;macroFeedback="";}
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
        dragWin = 0; dragging = null; return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double amount) {
        if(macroGuideOpen) {
            macroGuideScroll=Math.max(0,macroGuideScroll-(int)Math.signum(amount)*2);
            return true;
        }
        if(drainerHudEditor) {
            if(mx >= width-Math.min(230,Math.max(176,width/4))-12 && hudSelected!=null)hudSettingsScroll=Math.max(0,Math.min(hudSelected.settings.size()-1,hudSettingsScroll-(int)Math.signum(amount)));
            else hudListScroll=Math.max(0,hudListScroll-(int)Math.signum(amount)*2);
            return true;
        }
        double s = mc.getWindow().getGuiScale();
        double x = mx / s / gs, y = my / s / gs;
        if (InputConstants.isKeyDown(windowHandle(), GLFW.GLFW_KEY_LEFT_CONTROL)) {
            LovenoteMenuConfig.menuScale = (int) Mth.clamp(LovenoteMenuConfig.menuScale + amount * 5, 20, 110);
            return true;
        }
        if (selected != null && x >= premiumSettingsX() && x <= premiumSettingsX() + premiumSettingsW() && y >= wy + 112 && y <= wy + winH - 30) {
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
            if(input.key()==GLFW.GLFW_KEY_ESCAPE){if(macroGuideTyping)macroGuideTyping=false;else macroGuideOpen=false;}
            else if(input.key()==GLFW.GLFW_KEY_BACKSPACE && macroGuideTyping && !macroGuideQuery.isEmpty()){
                macroGuideQuery=macroGuideQuery.substring(0,macroGuideQuery.length()-1);macroGuideScroll=0;
            }else if(input.key()==GLFW.GLFW_KEY_ENTER)macroGuideTyping=false;
            return true;
        }
        if(drainerHudEditor && input.key()==GLFW.GLFW_KEY_ESCAPE) {
            drainerHudEditor=false;hudDragging=null;hudSlider=null;return true;
        }
        int key = input.key();
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
        if(macroGuideOpen){
            int cp=input.codepoint();
            if(macroGuideTyping && cp>=32 && cp<127 && macroGuideQuery.length()<36){macroGuideQuery+=(char)cp;macroGuideScroll=0;}
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
