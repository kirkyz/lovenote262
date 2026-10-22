package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Gfx;
import com.lovenote.util.HudGlass;
import com.lovenote.util.HudWidgetBounds;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Claude-inspired four-slot durability display backed by real armour slots. */
public final class ArmorHud extends Module implements HudWidgetBounds {
    private final BoolSetting offhand = add(new BoolSetting("Off hand", true));
    private final NumberSetting x = add(new NumberSetting("X", 0.58, 0, .98, .005));
    private final NumberSetting y = add(new NumberSetting("Y", 0.82, 0, .98, .005));
    private final NumberSetting opacity = add(new NumberSetting("Panel opacity", 205, 30, 255, 5));
    private final BoolSetting frame = add(new BoolSetting("Frame", true));
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final String[] LABELS = {"HELM", "CHEST", "LEGS", "BOOTS"};
    private int px, py;
    private static final int W = 244, H = 83;

    public ArmorHud() {
        super("ArmorHud", Category.HUD);
        describe("Real armor durability in four minimalist draggable glass slots.");
    }
    @Override public int editorX() {return px;}
    @Override public int editorY() {return py;}
    @Override public int editorWidth() {return W;}
    @Override public int editorHeight() {return H;}

    @Override public void onHud(GuiGraphicsExtractor g, float delta) {
        if (mc.player == null) return;
        px = Math.max(0, Math.min((int)(x.get()*Gfx.guiW()), Math.max(0,Gfx.guiW()-W)));
        py = Math.max(0, Math.min((int)(y.get()*Gfx.guiH()), Math.max(0,Gfx.guiH()-H)));
        if(frame.get()) HudGlass.card(g, px,py,W,H,opacity.getI());
        Ui.text(g, "ARMOUR  /  EQUIPMENT", px + 13,py + 10,HudGlass.text());
        if(offhand.get()) Ui.text(g,mc.player.getOffhandItem().isEmpty()?"OFF —":"OFF +",
                px + W - 50,py + 10,HudGlass.muted());
        for(int i=0;i<SLOTS.length;i++) {
            ItemStack stack=mc.player.getItemBySlot(SLOTS[i]);
            int bx=px+13+i*58, by=py+28;
            HudGlass.tile(g,bx,by,51,44,!stack.isEmpty());
            Ui.text(g,LABELS[i],bx+6,by+7,HudGlass.muted());
            double health=1;
            String label="EMPTY";
            if(!stack.isEmpty()) {
                if(stack.isDamageableItem() && stack.getMaxDamage()>0) {
                    health=Math.max(0,Math.min(1,(double)(stack.getMaxDamage()-stack.getDamageValue())/stack.getMaxDamage()));
                    label=(int)Math.round(health*100)+"%";
                } else label="OK";
            } else health=0;
            Ui.text(g,label,bx+6,by+21,HudGlass.text());
            HudGlass.progress(g,bx+5,by+37,41,3,health,health<.25?0xFFF1757A:HudGlass.accent());
        }
    }
}
