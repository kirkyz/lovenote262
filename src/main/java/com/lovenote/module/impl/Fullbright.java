package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Client-local night vision without knowingly replacing real player effects. */
public final class Fullbright extends Module {
    private Object injectedPlayer;
    private MobEffectInstance injectedEffect;

    public Fullbright() {
        super("Fullbright", Category.VISUALS);
        describe("Local night-vision brightness (visual effect only).");
    }

    @Override public void onTick() {
        if (mc.player == null) {
            injectedPlayer = null;
            injectedEffect = null;
            return;
        }
        if (mc.player != injectedPlayer) {
            injectedPlayer = mc.player;
            injectedEffect = null;
        }
        MobEffectInstance current = mc.player.getEffect(MobEffects.NIGHT_VISION);
        // The player may already have a legitimate night vision effect.
        if (current != null && current != injectedEffect) return;
        if (current == null || current.getDuration() < 260) {
            mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0, false, false, false));
            injectedEffect = mc.player.getEffect(MobEffects.NIGHT_VISION);
        }
    }

    @Override public void onDisable() {
        if (mc.player != null && mc.player == injectedPlayer && injectedEffect != null
                && mc.player.getEffect(MobEffects.NIGHT_VISION) == injectedEffect) {
            mc.player.removeEffect(MobEffects.NIGHT_VISION);
        }
        injectedPlayer = null;
        injectedEffect = null;
    }
}
