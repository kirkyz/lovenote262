package com.lovenote.module.impl;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
/** Plays a client-side sound for attack attempts, not verified hits. */
public final class HitSound extends Module {
    private final ModeSetting sound=add(new ModeSetting("Sound","Orb","Orb","Chime","Ding"));
    private final NumberSetting pitch=add(new NumberSetting("Pitch",1.4,.5,2.0,.1));
    private final NumberSetting volume=add(new NumberSetting("Volume",.8,.1,1.0,.1));
    public HitSound(){super("HitSound",Category.VISUALS);describe("Selectable local sound effect when attacking a living entity.");}
    @Override public void onAttack(Entity target){
        if(!(target instanceof LivingEntity)||mc.player==null)return;
        var event=sound.is("Ding")?SoundEvents.ARROW_HIT_PLAYER:sound.is("Chime")?SoundEvents.AMETHYST_BLOCK_CHIME:SoundEvents.EXPERIENCE_ORB_PICKUP;
        mc.player.playSound(event,volume.getF(),pitch.getF());
    }
}
