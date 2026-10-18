package com.lovenote.module.impl;
import com.lovenote.setting.NumberSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
/** Client attack-attempt streak (does not infer server-confirmed hits). */
public final class ComboCounter extends LegacyTextWidget {
    private final NumberSetting resetSeconds = add(new NumberSetting("Reset after (s)", 5, 1, 30, 1));
    private int streak, lastAttackTick, tick;
    private float previousHealth = -1f;
    public ComboCounter() { super("ComboCounter", 0.46, 0.40); describe("Display a streak of consecutive attack attempts; resets on damage or timeout."); }
    @Override public void onAttack(Entity target) { if (mc.player != null && target != null && target != mc.player) { streak++; lastAttackTick = tick; } }
    @Override public void onTick() {
        tick++;
        if (mc.player == null) { streak = 0; previousHealth = -1f; return; }
        float health = mc.player.getHealth();
        if (previousHealth >= 0 && health < previousHealth) streak = 0;
        previousHealth = health;
        if (tick - lastAttackTick > resetSeconds.getI() * 20) streak = 0;
    }
    @Override public void onHud(GuiGraphicsExtractor g, float delta) {
        if (mc.player != null && streak > 1) drawText(g, streak + "x combo (attempts)");
    }
    @Override public void onDisable() { streak = 0; previousHealth = -1f; }
}
