package com.lovenote.module;

import com.lovenote.module.impl.GroundFog;
import com.lovenote.module.impl.SkyShader;
import com.lovenote.module.impl.SwingAnimation;
import com.lovenote.module.impl.Velocity;
import com.lovenote.module.impl.Interface;
import com.lovenote.module.impl.LogoWidget;
import com.lovenote.module.impl.FunnyCrosshair;
import com.lovenote.module.impl.SkyExtras;
import com.lovenote.module.impl.CubeField;
import com.lovenote.module.impl.CrystalHalo;
import com.lovenote.module.impl.AuroraVeil;
import com.lovenote.module.impl.NeonGrid;
import com.lovenote.module.impl.RibbonTrail;
import com.lovenote.module.impl.StarSphere;
import com.lovenote.module.impl.SparkBurst;
import com.lovenote.module.impl.PrismOrb;
import com.lovenote.module.impl.Vortex;
import com.lovenote.module.impl.PlasmaShell;
import com.lovenote.module.impl.ArcBeams;
import com.lovenote.module.impl.PrismRain;
import com.lovenote.util.WorldRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ModuleManager {
	private final List<Module> modules = new ArrayList<>();

	public ModuleManager() {
		register(new GroundFog());
		register(new SkyShader());
		// ---- lovenote pack
		register(new Interface()).enabled = true;
		register(new LogoWidget()).enabled = true;
		register(new FunnyCrosshair()).enabled = true;
		register(new SkyExtras());
		register(new CubeField());
		register(new CrystalHalo());
		register(new AuroraVeil());
		register(new NeonGrid());
		register(new RibbonTrail());
		register(new StarSphere());
		register(new SparkBurst());
		register(new PrismOrb());
		register(new Vortex());
		register(new PlasmaShell());
		register(new ArcBeams());
		register(new PrismRain());

		register(new Velocity());
		register(new SwingAnimation());
	}

	private <T extends Module> T register(T m) {
		modules.add(m);
		return m;
	}

	public List<Module> all() { return modules; }

	@SuppressWarnings("unchecked")
	public <T extends Module> T get(Class<T> type) {
		for (Module m : modules) if (type.isInstance(m)) return (T) m;
		return null;
	}

	public List<Module> byCategory(Category c) {
		List<Module> out = new ArrayList<>();
		for (Module m : modules) if (!m.hidden && (m.category == c || m.alsoIn.contains(c))) out.add(m);
		return out;
	}

	public List<Module> search(String query) {
		String q = query.trim().toLowerCase(Locale.ROOT);
		List<Module> out = new ArrayList<>();
		for (Module m : modules) if (!m.hidden && m.matches(q)) out.add(m);
		return out;
	}

	public void tick() {
		for (Module m : modules) if (m.enabled) m.onTick();
	}

	public void render3D(WorldRenderContext ctx) {
		PoseStack stack = ctx.matrixStack();
		for (Module m : modules) if (m.enabled) m.onRender3D(ctx, stack);
	}

	public void hud(GuiGraphicsExtractor ctx, float tickDelta) {
		for (Module m : modules) if (m.enabled) m.onHud(ctx, tickDelta);
	}

	public void attack(Entity target) {
		for (Module m : modules) if (m.enabled) m.onAttack(target);
	}

	public void totemPop(Entity entity) {
		for (Module m : modules) if (m.enabled) m.onTotemPop(entity);
	}
}
