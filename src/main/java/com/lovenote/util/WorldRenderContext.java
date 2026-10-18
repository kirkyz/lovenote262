package com.lovenote.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;

public class WorldRenderContext {
	private final PoseStack matrixStack;
	private final Camera camera;
	private final float tickDelta;

	public WorldRenderContext(PoseStack matrixStack, Camera camera, float tickDelta) {
		this.matrixStack = matrixStack;
		this.camera = camera;
		this.tickDelta = tickDelta;
	}

	public PoseStack matrixStack() { return matrixStack; }
	public Camera camera() { return camera; }
	public float tickDelta() { return tickDelta; }
}
