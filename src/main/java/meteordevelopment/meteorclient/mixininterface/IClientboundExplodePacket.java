/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/lovenote262).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixininterface;

public interface IClientboundExplodePacket {
    void meteor$setVelocityX(float velocity);

    void meteor$setVelocityY(float velocity);

    void meteor$setVelocityZ(float velocity);
}
