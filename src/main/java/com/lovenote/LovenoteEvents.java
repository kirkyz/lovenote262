
package com.lovenote;

import com.lovenote.config.LovenoteConfig;
import com.lovenote.module.Module;
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

        Lovenote.modules.tick();
        LovenoteConfig.tick();
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
