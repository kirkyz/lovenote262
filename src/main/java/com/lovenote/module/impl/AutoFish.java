package com.lovenote.module.impl;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.NumberSetting;
import meteordevelopment.meteorclient.mixin.FishingHookAccessor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Items;
/** Line-state fishing, using the project's existing hook accessor. */
public final class AutoFish extends Module {
    private final BoolSetting autoCast=add(new BoolSetting("Auto cast",true));
    private final BoolSetting autoSwitch=add(new BoolSetting("Hotbar rod switch",false));
    private final NumberSetting cooldown=add(new NumberSetting("Recast wait (ticks)",20,5,100,1));
    private final NumberSetting biteDelay=add(new NumberSetting("Reel delay (ticks)",4,1,15,1));
    private int ticks;
    private int reelIn=-1;
    public AutoFish(){super("AutoFish",Category.HYPIXEL);alsoIn.add(Category.DONUT);describe("Fishes with a rod; recasts after bites. Uses the client fishing-hook accessor, no server bypass.");}
    @Override public void onEnable(){ticks=0;reelIn=-1;}
    @Override public void onTick(){
        if(mc.player==null||mc.gameMode==null||mc.gui.screen()!=null){reelIn=-1;return;}
        if(ticks>0)ticks--;
        if(!isHoldingRod() && !selectRod())return;
        FishingHook hook=mc.player.fishing;
        if(hook==null){
            reelIn=-1;
            if(autoCast.get()&&ticks<=0){mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);ticks=cooldown.getI();}
            return;
        }
        if(reelIn>=0){
            if(--reelIn<=0 && ticks<=0){mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);reelIn=-1;ticks=cooldown.getI();}
            return;
        }
        if(hook.currentState==FishingHook.FishHookState.BOBBING
           && ((FishingHookAccessor)hook).meteor$hasCaughtFish())reelIn=biteDelay.getI();
    }
    private boolean isHoldingRod(){return mc.player.getMainHandItem().is(Items.FISHING_ROD);}
    private boolean selectRod(){
        if(!autoSwitch.get())return false;
        var inv=mc.player.getInventory();
        for(int i=0;i<9;i++)if(inv.getItem(i).is(Items.FISHING_ROD)){
            inv.setSelectedSlot(i);return true;
        }
        return false;
    }
}
