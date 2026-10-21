package com.lovenote.util;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import meteordevelopment.meteorclient.gui.themes.lovenote.LovenoteMenuConfig;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

/** System-font glyph rasterization. Uses the installed OS font; no font files are bundled. */
public final class ModernText {
    private ModernText() {}
    private static final class Texture {
        final Identifier id;
        final int width, height;
        Texture(Identifier id, int width, int height) {
            this.id=id;this.width=width;this.height=height;
        }
    }
    private static final Map<String, Texture> cache = new LinkedHashMap<>(128, .75f, true);
    // FontMetrics construction was happening on every widget width query.
    // A bounded width cache greatly reduces AWT allocations in the HUD/menu.
    private static final Map<String, Integer> widthCache = new LinkedHashMap<>(256, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
            return size() > 768;
        }
    };
    private static long sequence;
    private static boolean available = true;
    private static String lastMode = "";
    private static Font awt() {
        String mode = LovenoteMenuConfig.fontMode;
        int size = "Compact".equals(mode) ? 15 : 17;
        String face = switch (mode) {
            case "Arial" -> "Arial";
            case "Verdana" -> "Verdana";
            case "Tahoma" -> "Tahoma";
            case "Trebuchet" -> "Trebuchet MS";
            default -> "Segoe UI";
        };
        return new Font(face, Font.PLAIN, size);
    }
    public static boolean enabled() {
        return available && !"Classic".equals(LovenoteMenuConfig.fontMode);
    }
    public static int width(String text) {
        if (text == null || text.isEmpty()) return 0;
        String key = LovenoteMenuConfig.fontMode + "|" + text;
        Integer cached = widthCache.get(key);
        if (cached != null) return cached;
        int measured;
        try {
            var graphics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
            try {
                graphics.setFont(awt());
                measured = Math.max(0, (int) Math.ceil(graphics.getFontMetrics().stringWidth(text) / 2.0));
            } finally { graphics.dispose(); }
        } catch (Throwable t) { measured = text.length() * 6; }
        widthCache.put(key, measured);
        return measured;
    }
    private static void evictOld() {
        while (cache.size() > 550) {
            var it = cache.entrySet().iterator();
            if (!it.hasNext()) break;
            Texture old=it.next().getValue();it.remove();
            try {
                // TextureManager.release is not consistently named across 26.x mappings.
                for (var m : Minecraft.getInstance().getTextureManager().getClass().getMethods()) {
                    if (m.getName().equals("release") && m.getParameterCount()==1 && m.getParameterTypes()[0].isInstance(old.id)) {
                        m.invoke(Minecraft.getInstance().getTextureManager(), old.id); break;
                    }
                }
            } catch (Throwable ignored) {}
        }
    }
    public static boolean draw(GuiGraphicsExtractor g, String str, int x, int y, int argb) {
        if (!enabled() || str==null || str.isEmpty()) return false;
        if (str.length()>145) return false;
        // FPS counters change almost every frame. Rasterising one GPU texture
        // per distinct FPS string caused cache churn and visible frame spikes.
        // Use Minecraft's native text path for these rapidly changing labels.
        if (str.contains(" FPS")) return false;
        try {
            RenderSystem.assertOnRenderThread();
            String mode = LovenoteMenuConfig.fontMode;
            if (!mode.equals(lastMode)) { cache.clear(); widthCache.clear(); lastMode=mode; }
            // Cache colour in 16-shade steps to avoid per-frame textures for animated accents.
            int color = (argb&0xF0F0F0F0) | 0x0F0F0F0F;
            String key = mode + "|" + Integer.toHexString(color) + "|" + str;
            Texture texture = cache.get(key);
            if (texture==null) {
                Font face=awt();
                BufferedImage probe=new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);
                Graphics2D measure=probe.createGraphics();measure.setFont(face);
                int w=Math.max(2,measure.getFontMetrics().stringWidth(str)+4);
                int h=Math.max(2,measure.getFontMetrics().getHeight()+3);
                int ascent=measure.getFontMetrics().getAscent();measure.dispose();
                if (w>1300 || h>80) return false;
                BufferedImage raster=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
                Graphics2D painter=raster.createGraphics();
                painter.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                painter.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
                painter.setFont(face);
                painter.setColor(new Color(color,true));
                painter.drawString(str,2,ascent+1);
                painter.dispose();
                NativeImage pixels=new NativeImage(w,h,false);
                for (int py=0;py<h;py++) for(int px=0;px<w;px++) {
                    int v=raster.getRGB(px,py);
                    int abgr=(v&0xFF00FF00)|((v>>>16)&255)|((v&255)<<16);
                    pixels.setPixelABGR(px,py,abgr);
                }
                Identifier id=Identifier.fromNamespaceAndPath("lovenote","generated/ui_text/"+(++sequence));
                Minecraft.getInstance().getTextureManager().register(id,new DynamicTexture(()->"drainer_text",pixels));
                texture=new Texture(id,w,h);cache.put(key,texture);evictOld();
            }
            int drawW=Math.max(1,Math.round(texture.width*.5f)), drawH=Math.max(1,Math.round(texture.height*.5f));
            Gfx.blit(g,texture.id,x-1,y-2,0f,0f,drawW,drawH,texture.width,texture.height,texture.width,texture.height);
            return true;
        } catch (Throwable t) {
            available=false;
            System.out.println("[Drainer] Modern font unavailable; reverting to Minecraft text: "+t);
            return false;
        }
    }
}
