package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Gfx;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;

/** Compact cached local terrain minimap. No external mapping downloads. */
public final class RegionMap extends Module {
    private final NumberSetting x = add(new NumberSetting("X", .82, 0, .95, .01));
    private final NumberSetting y = add(new NumberSetting("Y", .10, 0, .90, .01));
    private final NumberSetting radius = add(new NumberSetting("Radius (blocks)", 56, 16, 128, 8));
    private final BoolSetting showPlayers = add(new BoolSetting("Nearby player dots", true));
    private final BoolSetting showCoords = add(new BoolSetting("Coordinates", false));
    private static final int CELLS = 29, PIXEL = 3, MAP_SIZE = CELLS * PIXEL;
    private final int[] pixels = new int[CELLS * CELLS];
    private int lastX = Integer.MIN_VALUE, lastZ = Integer.MIN_VALUE, lastRadius = -1;
    private Object lastLevel;
    private long lastUpdate;

    public RegionMap() {
        super("RegionMap", Category.HUD);
        describe("Compact overhead terrain minimap using loaded surface blocks; cached for performance.");
    }

    @Override public void onDisable() { lastLevel = null; }

    private void rebuild() {
        if (mc.player == null || mc.level == null) return;
        int px = mc.player.getBlockX(), pz = mc.player.getBlockZ(), r = radius.getI();
        long now = System.currentTimeMillis();
        if (lastLevel == mc.level && lastX == px && lastZ == pz && lastRadius == r && now - lastUpdate < 1800) return;
        if (lastLevel == mc.level && Math.abs(lastX - px) <= 1 && Math.abs(lastZ - pz) <= 1 && lastRadius == r && now - lastUpdate < 900) return;
        lastLevel = mc.level; lastX = px; lastZ = pz; lastRadius = r; lastUpdate = now;
        for (int iz = 0; iz < CELLS; iz++) {
            for (int ix = 0; ix < CELLS; ix++) {
                int wx = px + Math.round((ix - CELLS / 2f) * r / (CELLS / 2f));
                int wz = pz + Math.round((iz - CELLS / 2f) * r / (CELLS / 2f));
                BlockPos check = new BlockPos(wx, mc.player.getBlockY(), wz);
                if (!mc.level.hasChunkAt(check)) { pixels[iz * CELLS + ix] = 0xFF161A28; continue; }
                int h = mc.level.getHeight(Heightmap.Types.WORLD_SURFACE, wx, wz) - 1;
                BlockPos top = new BlockPos(wx, h, wz);
                int rgb = mc.level.getBlockState(top).getMapColor(mc.level, top).col;
                int v = Math.max(-25, Math.min(28, (h - mc.player.getBlockY()) * 3));
                int rr = Math.max(0, Math.min(255, ((rgb >> 16) & 255) + v));
                int gg = Math.max(0, Math.min(255, ((rgb >> 8) & 255) + v));
                int bb = Math.max(0, Math.min(255, (rgb & 255) + v));
                pixels[iz * CELLS + ix] = 0xFF000000 | rr << 16 | gg << 8 | bb;
            }
        }
    }

    @Override public void onHud(GuiGraphicsExtractor g, float delta) {
        if (mc.player == null || mc.level == null) return;
        rebuild();
        int w = MAP_SIZE + 12, h = MAP_SIZE + (showCoords.get() ? 31 : 20);
        int px = Math.max(0, Math.min((int)(x.get() * Gfx.guiW()), Math.max(0, Gfx.guiW() - w)));
        int py = Math.max(0, Math.min((int)(y.get() * Gfx.guiH()), Math.max(0, Gfx.guiH() - h)));
        Ui.widgetCard(g, px, py, w, h, 215);
        Ui.text(g, "MINIMAP", px + 6, py + 4, Ui.textDim);
        int ox = px + 6, oy = py + 16;
        for (int iz = 0; iz < CELLS; iz++) {
            for (int ix = 0; ix < CELLS; ix++) {
                Ui.rect(g, ox + ix * PIXEL, oy + iz * PIXEL, PIXEL, PIXEL, pixels[iz * CELLS + ix]);
            }
        }
        int c = CELLS / 2 * PIXEL + PIXEL / 2;
        Ui.rect(g, ox + c - 2, oy + c - 2, 5, 5, 0xFF121521);
        Ui.rect(g, ox + c - 1, oy + c - 1, 3, 3, Ui.accent1);
        if (showPlayers.get()) {
            for (Player player : mc.level.players()) {
                if (player == mc.player) continue;
                double dx = player.getX() - mc.player.getX(), dz = player.getZ() - mc.player.getZ();
                int mx = c + (int) Math.round(dx / radius.get() * MAP_SIZE / 2d);
                int mz = c + (int) Math.round(dz / radius.get() * MAP_SIZE / 2d);
                if (mx > 2 && mz > 2 && mx < MAP_SIZE - 3 && mz < MAP_SIZE - 3)
                    Ui.rect(g, ox + mx - 1, oy + mz - 1, 3, 3, Ui.accent2);
            }
        }
        if (showCoords.get()) {
            Ui.text(g, mc.player.getBlockX() + "  /  " + mc.player.getBlockZ(), px + 6, py + MAP_SIZE + 19, Ui.textDim);
        }
    }
}
