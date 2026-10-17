package com.lovenote.util;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public final class Gl {

	private Gl() {}

	public static RenderType quads() { return RenderTypes.debugQuads(); }
	public static RenderType lines() { return RenderTypes.lines(); }
	public static RenderType tex(Identifier texture) { return RenderTypes.blockScreenEffect(texture); }
	public static RenderType texDepth(Identifier texture) { return RenderTypes.entityTranslucentEmissive(texture); }

	public static float skyAngle(Level world, float tickDelta) {
		if (world == null) return 0.0f;
		double d = Mth.frac(world.getLevelData().getGameTime() / 24000.0D);
		double e = 0.25D - Math.cos(d * Math.PI * 2.0D) / 2.0D;
		return (float) ((d * 2.0D + e) / 3.0D);
	}

	public static float skyAngleRadians(Level world, float tickDelta) {
		return skyAngle(world, tickDelta) * (float) (Math.PI * 2.0D);
	}
}
