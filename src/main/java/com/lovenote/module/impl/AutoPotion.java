package com.lovenote.module.impl;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.NumberSetting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
/** Uses one splash potion from hotbar below health threshold; potion effect is not predicted. */
public final class AutoPotion extends Module {
    private final NumberSetting health=add(new NumberSetting("Use below health",8,1,20,1));
    private final NumberSetting cooldown=add(new NumberSetting("Cooldown (seconds)",12,3,120,1));
    private int ticks,restoreSlot=-1;
    public AutoPotion(){super("AutoPotion",Category.HYPIXEL);alsoIn.add(Category.DONUT);describe("Uses a splash potion at low health. Check the potion effect before enabling.");}
    @Override public void onTick(){
        if(mc.player==null||mc.gameMode==null){restoreSlot=-1;return;}
        if(restoreSlot>=0){mc.player.getInventory().setSelectedSlot(restoreSlot);restoreSlot=-1;}
        if(ticks>0){ticks--;return;}
        if(mc.gui.screen()!=null||mc.player.getHealth()>health.get())return;
        var inv=mc.player.getInventory();
        for(int i=0;i<9;i++)if(inv.getItem(i).is(Items.SPLASH_POTION)){
            restoreSlot=inv.getSelectedSlot();inv.setSelectedSlot(i);
            mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);
            ticks=cooldown.getI()*20;
            return;
        }
    }
    @Override public void onDisable(){
        if(restoreSlot>=0 && mc.player!=null)mc.player.getInventory().setSelectedSlot(restoreSlot);
        restoreSlot=-1;
    }
}
