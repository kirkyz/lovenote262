
package com.lovenote;

import com.lovenote.config.LovenoteConfig;
import com.lovenote.module.Module;
import com.lovenote.module.impl.SpotifyHud;
import com.lovenote.module.impl.Lyrics;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import org.lwjgl.glfw.GLFW;
import com.lovenote.util.Fx;
import com.lovenote.util.WorldRenderContext;
import com.mojang.blaze3d.platform.InputConstants;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;

public class LovenoteEvents {
    private static boolean loaded;

    private static int GfxWidth(Minecraft mc) {return com.lovenote.util.Gfx.guiW();}
    private static int GfxHeight(Minecraft mc) {return com.lovenote.util.Gfx.guiH();}
    @EventHandler
    public static void onTick(TickEvent.Post e) {
        if (Lovenote.modules == null) return;

        if (!loaded) {
            loaded = true;
            LovenoteConfig.load();
        }

        Minecraft mc = MeteorClient.mc;

        if (mc.gui.screen() == null && mc.level != null) {
            for (Module m : Lovenote.modules.all()) {
                if (m.key < 0) {
                    m.keyDown = false;
                    continue;
                }

                boolean down = InputConstants.isKeyDown(mc.getWindow(), m.key);
                if (down && !m.keyDown) m.toggle();
                m.keyDown = down;
            }
        }

        if(mc.gui.screen() != null) {
            boolean down=GLFW.glfwGetMouseButton(mc.getWindow().handle(),GLFW.GLFW_MOUSE_BUTTON_LEFT)==GLFW.GLFW_PRESS;
            double mx=mc.mouseHandler.xpos()*GfxWidth(mc)/Math.max(1,mc.getWindow().getWidth());
            double my=mc.mouseHandler.ypos()*GfxHeight(mc)/Math.max(1,mc.getWindow().getHeight());
            for(Module m:Lovenote.modules.all()) {
                if(m instanceof SpotifyHud hud)hud.move(mx,my,down);
                if(m instanceof Lyrics lyrics)lyrics.move(mx,my,down);
            }
        }
        Lovenote.modules.tick();
        LovenoteConfig.tick();
    }

    // Interact while a screen with a visible cursor is open (e.g. chat/inventory).
    @EventHandler
    public static void onMediaClick(MouseClickEvent e) {
        if (Lovenote.modules == null || Minecraft.getInstance().gui.screen() == null || e.button()!=0) return;
        boolean down=e.action==KeyAction.Press;
        for(Module m:Lovenote.modules.all()) {
            if(m instanceof SpotifyHud hud && hud.click(e.click.x(),e.click.y(),down)) {e.cancel();return;}
            if(m instanceof Lyrics lyrics && lyrics.click(e.click.x(),e.click.y(),down)) {e.cancel();return;}
        }
    }

    @EventHandler
    public static void onRender3D(Render3DEvent e) {
        if (Lovenote.modules == null) return;

        Fx.setMatrices(e.matrices);
        Lovenote.modules.render3D(
            new WorldRenderContext(
                e.matrices,
                MeteorClient.mc.gameRenderer.mainCamera(),
                e.tickDelta
            )
        );
    }

    @EventHandler
    public static void onRender2D(Render2DEvent e) {
        if (Lovenote.modules == null) return;

        Lovenote.modules.hud(e.graphics, e.tickDelta);
    }
}
