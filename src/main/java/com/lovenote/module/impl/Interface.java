package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Logos;
import com.lovenote.util.Ui;

/** Master style module. Every UI/HUD surface reads from here. */
public class Interface extends Module {
	public static Interface INSTANCE;

	public final ModeSetting accent = add(new ModeSetting("Accent", "Lovenote", Colors.PRESET_NAMES));
	public final ModeSetting theme = add(new ModeSetting("Theme", "Dark", "Dark", "Light"));
	public final BoolSetting animated = add(new BoolSetting("Animated accent", true));

	public final ModeSetting bgTint = add(new ModeSetting("Panel tint", "Dark", "Dark", "Black", "Accent"));
	public final NumberSetting bgOpacity = add(new NumberSetting("Panel opacity", 225, 0, 255, 5));
	public final BoolSetting gradientPanel = add(new BoolSetting("Panel gradient", true));
	public final BoolSetting glass = add(new BoolSetting("Glass sheen", true));
	public final ModeSetting pillStyle = add(new ModeSetting("Corner style", "Pill", "Pill", "Rounded"));
	public final NumberSetting panelRadius = add(new NumberSetting("Corner radius", 7, 0, 18, 1));
	public final NumberSetting widgetScale = add(new NumberSetting("Widget scale", 1.0, 0.6, 1.8, 0.05));

	public final BoolSetting accentBorder = add(new BoolSetting("Accent border", true));
	public final NumberSetting borderWidth = add(new NumberSetting("Border width", 1, 0, 3, 1));
	public final NumberSetting borderAlpha = add(new NumberSetting("Border opacity", 70, 0, 255, 5));
	public final BoolSetting shadowOn = add(new BoolSetting("Drop shadow", true));
	public final NumberSetting shadowSize = add(new NumberSetting("Shadow size", 4, 0, 12, 1));
	public final NumberSetting shadowAlpha = add(new NumberSetting("Shadow opacity", 80, 0, 255, 5));
	public final BoolSetting glowOn = add(new BoolSetting("Accent glow", true));
	public final NumberSetting glowSize = add(new NumberSetting("Glow size", 5, 0, 14, 1));
	public final NumberSetting glowAlpha = add(new NumberSetting("Glow opacity", 45, 0, 200, 5));

	public final BoolSetting textShadow = add(new BoolSetting("Text shadow", true));

	public final BoolSetting menuFx = add(new BoolSetting("Menu effects", true));
	public final BoolSetting menuFxMeteorOnly = add(new BoolSetting("Menu only", true));
	public final BoolSetting rippleOn = add(new BoolSetting("Edge ripple", true));
	public final NumberSetting rippleEdge = add(new NumberSetting("Ripple reach", 26, 4, 120, 2));
	public final NumberSetting rippleSize = add(new NumberSetting("Ripple size", 54, 12, 180, 2));
	public final NumberSetting rippleLife = add(new NumberSetting("Ripple life (ms)", 720, 200, 2500, 20));
	public final NumberSetting rippleAlpha = add(new NumberSetting("Ripple opacity", 85, 10, 255, 5));
	public final NumberSetting rippleWidth = add(new NumberSetting("Ripple width", 2, 1, 6, 1));
	public final BoolSetting menuVignette = add(new BoolSetting("Vignette", true));
	public final BoolSetting menuScanlines = add(new BoolSetting("Scanlines", false));
	public final NumberSetting menuGrain = add(new NumberSetting("Grain", 0, 0, 900, 25));
	public final BoolSetting menuEdgeGlow = add(new BoolSetting("Edge glow", true));
	public final NumberSetting menuEdgeGlowAlpha = add(new NumberSetting("Edge glow opacity", 70, 0, 200, 5));

	public final ModeSetting logoStyle = add(new ModeSetting("Logo", "Monogram", Logos.STYLES));
	public final NumberSetting logoSize = add(new NumberSetting("Logo size", 16, 6, 96, 1));
	public final NumberSetting logoSpin = add(new NumberSetting("Logo spin", 1.0, 0.0, 6.0, 0.05));
	public final BoolSetting logoInWidgets = add(new BoolSetting("Logo in widgets", true));
	public final ModeSetting logoColour = add(new ModeSetting("Logo colour", "Accent", "Accent", "White", "Rainbow", "Original"));
	public final NumberSetting logoCropTop = add(new NumberSetting("Logo crop top %", 24, 0, 60, 1));
	public final NumberSetting logoCropBottom = add(new NumberSetting("Logo crop bottom %", 0, 0, 60, 1));
	public final NumberSetting logoZoom = add(new NumberSetting("Logo zoom", 1.0, 0.5, 1.8, 0.05));
	public final BoolSetting logoKeyBlack = add(new BoolSetting("Logo key out black", true));
	public final BoolSetting logoInMenu = add(new BoolSetting("Logo in menu", true));
	public final ModeSetting logoMenuCorner = add(new ModeSetting("Logo corner", "Top Left", "Top Left", "Top Right", "Bottom Left", "Bottom Right"));
	public final NumberSetting logoMenuSize = add(new NumberSetting("Menu logo size", 24, 10, 72, 1));
	public final NumberSetting logoMenuSpin = add(new NumberSetting("Menu logo spin", 0.7, 0.0, 4.0, 0.05));

	public final BoolSetting hudFrame = add(new BoolSetting("Widget frame", true));
	public final BoolSetting hudAccentBar = add(new BoolSetting("Widget accent bar", true));
	public final NumberSetting hudPadding = add(new NumberSetting("Widget padding", 5, 1, 14, 1));

	public Interface() {
		super("Interface", Category.THEMES);
		describe("Master style: accent, glass, glow, ripples, logo, HUD frame.");
		alsoIn.add(Category.HUD);
		INSTANCE = this;
		// Default activation is handled by ModuleManager after construction.
		Logos.setStyle(logoStyle.index());
		Ui.push(this);
	}

	@Override
	public void onEnable() { Ui.push(this); }

	@Override
	public void onTick() {
		Logos.setStyle(logoStyle.index());
		Ui.push(this);
	}
}
