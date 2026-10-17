package com.lovenote.util;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class RotationUtil {
        private RotationUtil() {}

        /** {yaw, pitch} needed for 'from' to look at the upper body of 'to'. */
        public static float[] to(Entity from, Entity to) {
                Vec3 eye = from.getEyePosition();
                Vec3 p = to.position().add(0, to.getBbHeight() * 0.6, 0);
                double dx = p.x - eye.x, dy = p.y - eye.y, dz = p.z - eye.z;
                double flat = Math.sqrt(dx * dx + dz * dz);
                float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
                float pitch = (float) -Math.toDegrees(Math.atan2(dy, flat));
                return new float[]{Mth.wrapDegrees(yaw), Mth.clamp(pitch, -90f, 90f)};
        }
}
