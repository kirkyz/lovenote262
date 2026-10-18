package com.lovenote.module.impl;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.NumberSetting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
/** Automatically eats a hotbar food item in permitted play environments. */
public final class AutoEat extends Module {
    private final NumberSetting hunger=add(new NumberSetting("Hunger",14,1,19,1));
    private int previousSlot=-1;
    private boolean eating, originalUseDown;
    public AutoEat(){super("AutoEat",Category.MISC);describe("Eat available hotbar food below selected hunger threshold.");}
    @Override public void onTick(){
        if(mc.player==null||mc.level==null||mc.gui.screen()!=null){stop();return;}
        if(eating){
            if(mc.player.getFoodData().getFoodLevel()>hunger.getI() || !mc.player.isUsingItem())stop();
            return;
        }
        if(mc.player.getFoodData().getFoodLevel()>hunger.getI()||mc.options.keyUse.isDown())return;
        var inv=mc.player.getInventory();
        for(int i=0;i<9;i++){
            ItemStack stack=inv.getItem(i);
            if(stack.isEmpty()||!stack.has(DataComponents.FOOD))continue;
            previousSlot=inv.getSelectedSlot(); originalUseDown=mc.options.keyUse.isDown();
            inv.setSelectedSlot(i);mc.options.keyUse.setDown(true);eating=true;return;
        }
    }
    private void stop(){
        if(eating && mc.options != null && !originalUseDown)mc.options.keyUse.setDown(false);
        if(eating && previousSlot>=0 && mc.player!=null)mc.player.getInventory().setSelectedSlot(previousSlot);
        previousSlot=-1;eating=false;
    }
    @Override public void onDisable(){stop();}
}
