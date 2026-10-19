package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Gfx;
import com.lovenote.util.HudWidgetBounds;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** Spinning client logo next to your name, with optional live info. */
public class LogoWidget extends Module implements HudWidgetBounds {
    // Minecraft loads this texture from the mod JAR and caches it on the GPU.
    // Avoid generating a procedural logo atlas during HUD rendering.
    // One texture per choice, supplied as a transparent PNG in the mod resources.
    // The user's original Drainer image remains the DEFAULT "Image" selection.
    private static final String[] LOGO_NAMES = {"Image", "Classic", "Crest", "Heart", "Monogram", "Star",
        "Crystal", "Infinity", "Sparkle", "Crown", "Wing", "Moon", "Rune", "Thorns", "Portal", "Orbit", "Diamond", "Nebula"};
    private static final String[] LOGO_FILES = {"original", "classic", "crest", "heart", "monogram", "star",
        "crystal", "infinity", "sparkle", "crown", "wing", "moon", "rune", "thorns", "portal", "orbit", "diamond", "nebula"};
    private static final Identifier[] ICONS = new Identifier[LOGO_FILES.length];
    static { for (int i = 0; i < LOGO_FILES.length; i++) ICONS[i] = Identifier.fromNamespaceAndPath(
        "lovenote", "textures/gui/drainer_logos/" + LOGO_FILES[i] + ".png"); }
    private int renderX, renderY, renderW = 80, renderH = 20;
    @Override public int editorX() { return renderX; }
    @Override public int editorY() { return renderY; }
    @Override public int editorWidth() { return renderW; }
    @Override public int editorHeight() { return renderH; }
    private final ModeSetting rendering = add(new ModeSetting(
            "Rendering", "Performance", "Performance", "Animated"));
	private final ModeSetting style = add(new ModeSetting("Logo", "Image", LOGO_NAMES));
	private final NumberSetting size = add(new NumberSetting("Logo size", 16, 6, 96, 1));
    // A separate reserved slot prevents Logo size from changing the frame's dimensions.
    // Large artwork can extend beyond its reserved area by choice; increase Logo space to avoid overlap.
    private final NumberSetting logoSpace = add(new NumberSetting("Logo space (frame)", 24, 12, 96, 1));
	private final NumberSetting hudScale = add(new NumberSetting("HUD scale", 0.8, 0.4, 1.4, 0.05));
	private final NumberSetting spin = add(new NumberSetting("Spin speed", 1.0, 0.0, 6.0, 0.05));
	private final ModeSetting spinMode = add(new ModeSetting("Spin", "Static", "Spin", "Pulse", "Spin + Pulse", "Static"));
	private final NumberSetting x = add(new NumberSetting("X", 0.012, 0.0, 0.95, 0.002));
	private final NumberSetting y = add(new NumberSetting("Y", 0.012, 0.0, 0.95, 0.002));
	private final BoolSetting frame = add(new BoolSetting("Frame", false));
	private final NumberSetting pad = add(new NumberSetting("Padding", 3, 1, 12, 1));
	private final BoolSetting showName = add(new BoolSetting("Show name", true));
	private final ModeSetting nameText = add(new ModeSetting("Name", "lovenote", "lovenote", "Lovenote", "Custom"));
	private final ModeSetting customName = add(new ModeSetting("Custom", "lovenote", "lovenote", "Custom"));
	private final ModeSetting nameColour = add(new ModeSetting("Name colour", "Gradient", "Gradient", "Accent", "White"));
	private final BoolSetting info = add(new BoolSetting("Show info", true));
	private final BoolSetting showFps = add(new BoolSetting("FPS", true));
	private final BoolSetting showTime = add(new BoolSetting("Time", true));
	private final BoolSetting showServer = add(new BoolSetting("Server", false));
	private final NumberSetting gap = add(new NumberSetting("Gap", 4, 0, 18, 1));

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
		int sz = Math.max(6, Math.min(96, size.getI()));
        int slot = Math.max(12, Math.min(96, logoSpace.getI()));

		String name = label();
		String infoStr = info.get() ? buildInfo() : "";
		int nameW = showName.get() ? Ui.textW(name) : 0;
		int infoW = infoStr.isEmpty() ? 0 : Ui.textW(infoStr);
		int inner = slot + (nameW > 0 ? gap.getI() + nameW : 0) + (infoW > 0 ? gap.getI() + infoW : 0);

        int p = pad.getI();
        int bw = inner + p * 2, bh = Math.max(slot, 11) + p * 2;
        // Uniform resizing of the whole widget (including its text), not just the icon.
        float sc = Math.max(0.4f, Math.min(1.4f, hudScale.getF()));
        int actualW = (int) Math.ceil(bw * sc), actualH = (int) Math.ceil(bh * sc);
        px = Math.max(0, Math.min(px, Math.max(0, Gfx.guiW() - actualW - 2)));
        py = Math.max(0, Math.min(py, Math.max(0, Gfx.guiH() - actualH - 2)));
        renderX=px; renderY=py; renderW=actualW; renderH=actualH;
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(px, py);
        pose.scale(sc, sc);
        if (frame.get()) Ui.widgetCard(g, 0, 0, bw, bh, 204);

        int tx = p;
        // Performance / Image retains the existing GPU-cached texture path.
        // Procedural styles are only baked on selection change and then cached.
        boolean moving = rendering.is("Animated") && !spinMode.is("Static");
        boolean rotating = moving && (spinMode.is("Spin") || spinMode.is("Spin + Pulse"));
        float pulse = moving && (spinMode.is("Pulse") || spinMode.is("Spin + Pulse"))
                ? (float)(1.0 + 0.055 * Math.sin(t * Math.max(.05, spin.get()) * 2.0)) : 1f;
        float iconX = tx + slot * .5f;
        float iconY = bh * .5f;
        var logoPose = g.pose();
        logoPose.pushMatrix();
        logoPose.translate(iconX, iconY);
        if (pulse != 1f) logoPose.scale(pulse, pulse);
        // GPU-cached texture in BOTH Performance and Animated mode.
        // Changing the Logo setting selects a different PNG immediately.
        int choice = Math.max(0, Math.min(ICONS.length - 1, style.index()));
        if (rotating) logoPose.scale((float)Math.cos(angle), 1f);
        Gfx.blit(g, ICONS[choice], -sz/2, -sz/2,
                0f, 0f, sz, sz, 128, 128, 128, 128);
        logoPose.popMatrix();
        tx += slot + gap.getI();

        if (showName.get()) {
            int col = nameColour.is("White") ? 0xFFFFFFFF
                    : nameColour.is("Accent") ? Colors.C1
                    : Colors.skeet((float) ((t * 0.25) % 1.0));
            Ui.text(g, name, tx, (bh - 8) / 2, col);
            tx += nameW + gap.getI();
        }
        if (infoW > 0) Ui.text(g, infoStr, tx, (bh - 8) / 2, Ui.textDim);
        pose.popMatrix();
    }

	private String buildInfo() {
		StringBuilder sb = new StringBuilder();
		if (showFps.get()) sb.append(mc.getFps()).append(" fps");
		if (showTime.get()) {
			if (sb.length() > 0) sb.append("  ");
			long secs = (System.currentTimeMillis() / 1000L) % 86400L;
			sb.append(String.format("%02d:%02d", secs / 3600, (secs % 3600) / 60));
		}
		if (showServer.get()) {
            if (sb.length() > 0) sb.append("  ");
            sb.append(mc.getCurrentServer() == null ? "Singleplayer" : mc.getCurrentServer().ip);
        }
		return sb.toString();
	}
}
