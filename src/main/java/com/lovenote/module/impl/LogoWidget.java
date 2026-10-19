package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Gfx;
import com.lovenote.util.Logos;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** Spinning client logo next to your name, with optional live info. */
public class LogoWidget extends Module {
    // Minecraft loads this texture from the mod JAR and caches it on the GPU.
    // Avoid generating a procedural logo atlas during HUD rendering.
    private static final Identifier FAST_ICON = Identifier.fromNamespaceAndPath(
            "lovenote", "textures/gui/drainer_hud_icon.png");
    private final ModeSetting rendering = add(new ModeSetting(
            "Rendering", "Performance", "Performance", "Animated"));
	private final ModeSetting style = add(new ModeSetting("Logo", "Image", Logos.STYLES));
	private final NumberSetting size = add(new NumberSetting("Logo size", 20, 6, 128, 1));
	private final NumberSetting spin = add(new NumberSetting("Spin speed", 1.0, 0.0, 6.0, 0.05));
	private final ModeSetting spinMode = add(new ModeSetting("Spin", "Static", "Spin", "Pulse", "Spin + Pulse", "Static"));
	private final NumberSetting x = add(new NumberSetting("X", 0.012, 0.0, 0.95, 0.002));
	private final NumberSetting y = add(new NumberSetting("Y", 0.012, 0.0, 0.95, 0.002));
	private final BoolSetting frame = add(new BoolSetting("Frame", false));
	private final NumberSetting pad = add(new NumberSetting("Padding", 4, 1, 16, 1));
	private final BoolSetting showName = add(new BoolSetting("Show name", true));
	private final ModeSetting nameText = add(new ModeSetting("Name", "lovenote", "lovenote", "Lovenote", "Custom"));
	private final ModeSetting customName = add(new ModeSetting("Custom", "lovenote", "lovenote", "Custom"));
	private final ModeSetting nameColour = add(new ModeSetting("Name colour", "Gradient", "Gradient", "Accent", "White"));
	private final BoolSetting info = add(new BoolSetting("Show info", true));
	private final BoolSetting showFps = add(new BoolSetting("FPS", true));
	private final BoolSetting showTime = add(new BoolSetting("Time", true));
	private final BoolSetting showServer = add(new BoolSetting("Server", false));
	private final NumberSetting gap = add(new NumberSetting("Gap", 5, 0, 24, 1));

	public LogoWidget() {
		super("LogoWidget", Category.HUD);
		describe("Spinning logo and client name.");
		// Default activation is handled by ModuleManager after construction.
	}

	private String label() {
		String configured = nameText.is("Custom") ? customName.get() : nameText.get();
		return configured.equalsIgnoreCase("lovenote") ? "drainer" : configured;
	}

	@Override
	public void onHud(GuiGraphicsExtractor g, float td) {
		if (mc.player == null) return;
		// No Logos.setStyle / setTint / Ui.push here: those can cause the
		// shared procedural texture atlas to be invalidated each frame.

		int px = (int) (x.get() * Gfx.guiW());
		int py = (int) (y.get() * Gfx.guiH());

		double t = System.nanoTime() / 1_000_000_000.0;
		double angle = 0;
		if (rendering.is("Animated") &&
                (spinMode.is("Spin") || spinMode.is("Spin + Pulse"))) {
            angle = t * spin.get() * Math.PI * 2.0;
        }
		int sz = Math.max(12, Math.min(24, size.getI())); // Fixed dimensions: no resize animation.

		String name = label();
		String infoStr = info.get() ? buildInfo() : "";
		int nameW = showName.get() ? Ui.textW(name) : 0;
		int infoW = infoStr.isEmpty() ? 0 : Ui.textW(infoStr);
		int inner = sz + (nameW > 0 ? gap.getI() + nameW : 0) + (infoW > 0 ? gap.getI() + infoW : 0);

		int p = pad.getI();
		int bw = inner + p * 2, bh = Math.max(sz, 11) + p * 2;
		// Reposition the entire widget, not just the image, so the label stays visible.
		px = Math.max(0, Math.min(px, Math.max(0, Gfx.guiW() - bw - 2)));
		py = Math.max(0, Math.min(py, Math.max(0, Gfx.guiH() - bh - 2)));
		if (frame.get()) {
			Ui.rrect(g, px, py, bw, bh, 5, 0xE5111020);
			Ui.rect(g, px + 6, py + bh - 2, Math.max(0, bw - 12), 1, 0xFF8F638F);
		}

		int tx = px + p;
		if (rendering.is("Performance")) {
            // Static, prepacked resource: no procedural texture baking/upload.
            Gfx.blit(g, FAST_ICON, tx, py + (bh - sz) / 2,
                    0f, 0f, sz, sz, 64, 64, 64, 64);
        } else {
            // Legacy animated shapes are preserved as an opt-in alternative.
            Logos.setTint(3);
            Logos.keyBlack = false;
            Logos.draw(g, tx, py + (bh - sz) / 2, sz, angle, style.index());
        }
		tx += sz + gap.getI();

		if (showName.get()) {
			int col = nameColour.is("White") ? 0xFFFFFFFF
					: nameColour.is("Accent") ? Colors.C1
					: Colors.skeet((float) ((t * 0.25) % 1.0));
			Ui.text(g, name, tx, py + (bh - 8) / 2, col);
			tx += nameW + gap.getI();
		}
		if (infoW > 0) Ui.text(g, infoStr, tx, py + (bh - 8) / 2, Ui.textDim);
	}

	private String buildInfo() {
		StringBuilder sb = new StringBuilder();
		if (showFps.get()) sb.append(mc.getFps()).append(" fps");
		if (showTime.get()) {
			if (sb.length() > 0) sb.append("  ");
			long secs = (System.currentTimeMillis() / 1000L) % 86400L;
			sb.append(String.format("%02d:%02d", secs / 3600, (secs % 3600) / 60));
		}
		if (showServer.get() && mc.getCurrentServer() != null) {
			if (sb.length() > 0) sb.append("  ");
			sb.append(mc.getCurrentServer().ip);
		}
		return sb.toString();
	}
}
