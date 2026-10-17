package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;

/** Changes the first-person swing animation, item position, size and swing speed. */
public class SwingAnimation extends Module {
        private final ModeSetting style = add(new ModeSetting("Style", "Smash", "Smash", "Spin", "Slide", "Slash", "None"));
        public final NumberSetting speed = add(new NumberSetting("Swing speed", 1.0, 0.5, 2.5, 0.05));
        private final NumberSetting offX = add(new NumberSetting("Item X", 0.0, -0.6, 0.6, 0.02));
        private final NumberSetting offY = add(new NumberSetting("Item Y", 0.0, -0.6, 0.6, 0.02));
        private final NumberSetting offZ = add(new NumberSetting("Item Z", 0.0, -0.6, 0.6, 0.02));
        private final NumberSetting scale = add(new NumberSetting("Item scale", 1.0, 0.5, 1.5, 0.05));

        public SwingAnimation() {
                super("SwingAnimation", Category.VISUALS);
                describe("Changes the first-person swing animation, item position, size and swing speed.");
        }

        public void apply(PoseStack ms, float swing) {
                ms.translate(offX.get(), offY.get(), offZ.get());
                float sc = scale.getF();
                ms.scale(sc, sc, sc);
                if (swing <= 0f) return;
                float s = Mth.sin(swing * (float) Math.PI);
                switch (style.get()) {
                        case "Smash" -> ms.mulPose(Axis.XP.rotationDegrees(-s * 45f));
                        case "Spin" -> ms.mulPose(Axis.ZP.rotationDegrees(swing * 360f));
                        case "Slide" -> ms.translate(0, 0, -s * 0.55);
                        case "Slash" -> {
                                ms.mulPose(Axis.ZP.rotationDegrees(-s * 60f));
                                ms.mulPose(Axis.XP.rotationDegrees(-s * 25f));
                        }
                        default -> {}
                }
        }
}
