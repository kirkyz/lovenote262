package com.lovenote.util;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Reflection bridge for members reshuffled by the 26.x retained-mode GUI rewrite. */
public final class Gfx {
	private Gfx() {}

	private static boolean resolved;
	private static Method mText, mBlit, mGuiW, mGuiH;
	private static Object pipeline;

	public static void resolve() {
		if (resolved) return;
		resolved = true;

		Object font = null;
		try {
			Minecraft mc = Minecraft.getInstance();
			if (mc != null) font = mc.font;
		} catch (Throwable ignored) {}

		for (Method m : GuiGraphicsExtractor.class.getMethods()) {
			String n = m.getName();
			Class<?>[] p = m.getParameterTypes();
			if (mText == null && font != null && (n.equals("text") || n.equals("drawString"))
					&& p.length >= 5 && p[0].isInstance(font)
					&& p[1] == String.class && p[2] == int.class && p[3] == int.class && p[4] == int.class) {
				mText = m;
			}
			if (mBlit == null && n.equals("blit") && p.length == 12
					&& named(p[0], "RenderPipeline") && named(p[1], "Identifier", "ResourceLocation")) {
				mBlit = m;
			}
		}

		String[] pipes = {
				"net.minecraft.client.renderer.RenderPipelines",
				"net.minecraft.client.gl.RenderPipelines",
				"com.mojang.blaze3d.pipeline.RenderPipelines"
		};
		for (String cn : pipes) {
			if (pipeline != null) break;
			try { pipeline = Class.forName(cn).getField("GUI_TEXTURED").get(null); }
			catch (Throwable ignored) {}
		}

		try { mGuiW = Window.class.getMethod("getGuiScaledWidth"); } catch (Throwable ignored) {}
		try { mGuiH = Window.class.getMethod("getGuiScaledHeight"); } catch (Throwable ignored) {}

		System.out.println("[lovenote] gfx bridge: text=" + (mText != null)
				+ " blit=" + (mBlit != null) + " pipeline=" + (pipeline != null)
				+ " guiSize=" + (mGuiW != null));
	}

	private static boolean named(Class<?> c, String... names) {
		String s = c.getSimpleName();
		for (String n : names) if (s.equals(n)) return true;
		return false;
	}

	public static void text(GuiGraphicsExtractor g, String s, int x, int y, int col) {
		resolve();
		if (mText == null) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc == null || mc.font == null) return;
		try {
			Class<?>[] p = mText.getParameterTypes();
			if (p.length >= 6) {
				Object extra = p[5] == boolean.class ? Boolean.TRUE
						: p[5] == int.class ? Integer.valueOf(0) : null;
				if (extra != null) mText.invoke(g, mc.font, s, x, y, col, extra);
				else mText.invoke(g, mc.font, s, x, y, col);
			} else {
				mText.invoke(g, mc.font, s, x, y, col);
			}
		} catch (Throwable ignored) {}
	}

	public static void blit(GuiGraphicsExtractor g, Identifier tex, int x, int y, float u, float v,
							int w, int h, int rw, int rh, int tw, int th) {
		resolve();
		if (mBlit == null || pipeline == null) return;
		try {
			Class<?>[] p = mBlit.getParameterTypes();
			Object uu = p[4] == float.class ? (Object) u : (Object) (int) u;
			Object vv = p[5] == float.class ? (Object) v : (Object) (int) v;
			mBlit.invoke(g, pipeline, tex, x, y, uu, vv, w, h, rw, rh, tw, th);
		} catch (Throwable ignored) {}
	}

	public static int guiW() {
		resolve();
		Minecraft mc = Minecraft.getInstance();
		if (mc == null) return 320;
		try {
			if (mGuiW != null) return (int) mGuiW.invoke(mc.getWindow());
			return mc.getWindow().getWidth() / Math.max(1, mc.getWindow().getGuiScale());
		} catch (Throwable t) { return 320; }
	}

	public static int guiH() {
		resolve();
		Minecraft mc = Minecraft.getInstance();
		if (mc == null) return 240;
		try {
			if (mGuiH != null) return (int) mGuiH.invoke(mc.getWindow());
			return mc.getWindow().getHeight() / Math.max(1, mc.getWindow().getGuiScale());
		} catch (Throwable t) { return 240; }
	}

	public static File gameDir() {
		try {
			Minecraft mc = Minecraft.getInstance();
			if (mc != null) {
				Field f = Minecraft.class.getField("gameDirectory");
				Object o = f.get(mc);
				if (o instanceof File file) return file;
			}
		} catch (Throwable ignored) {}
		return new File(".");
	}
}
