/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/lovenote262).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixininterface;

public interface IServerboundMovePlayerPacket {
    int meteor$getTag();

    void meteor$setTag(int tag);
}
