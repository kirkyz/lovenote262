package com.lovenote.module;

import com.lovenote.module.impl.GroundFog;
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
