package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.util.WorldRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * V55: compatibility stub for saved Drainer configuration and existing mixins.
 * Custom skybox rendering is delegated to Nuit (separate Fabric mod).
 * This class deliberately never claims, recolors, or hides Minecraft's sky.
 */
public final class SkyShader extends Module {
    public SkyShader() {
        super("SkyShader", Category.RENDER);
        describe("V55: old Drainer skies retired. Configure new skyboxes with Nuit resource packs.");
    }

    public boolean active() { return false; }
    public boolean vanillaClouds() { return true; }
    public boolean hideVanillaSun() { return false; }
    public boolean hideVanillaMoon() { return false; }

    @Override public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
        // Intentionally empty: Nuit owns skybox composition.
    }
}
