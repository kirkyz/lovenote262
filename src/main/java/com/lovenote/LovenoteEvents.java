package com.lovenote;

import com.lovenote.util.WorldRenderContext;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;

public class LovenoteEvents {

	@EventHandler
	public static void onTick(TickEvent.Post e) {
		if (Lovenote.modules != null) Lovenote.modules.tick();
	}

	@EventHandler
	public static void onRender3D(Render3DEvent e) {
		if (Lovenote.modules == null) return;
		Lovenote.modules.render3D(new WorldRenderContext(e.matrices, MeteorClient.mc.gameRenderer.mainCamera(), e.tickDelta));
	}
}
