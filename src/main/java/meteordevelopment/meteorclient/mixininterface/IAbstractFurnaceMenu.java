/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/lovenote262).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixininterface;

import net.minecraft.world.item.ItemStack;

public interface IAbstractFurnaceMenu {
    boolean meteor$canSmelt(ItemStack itemStack);
}
