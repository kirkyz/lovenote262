package com.lovenote.module.impl.ported;

import com.google.gson.JsonObject;
import com.lovenote.Lovenote;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.module.UnportedModule;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.BlockUtil;
import com.lovenote.util.Gfx;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Independent, Drainer-native adaptations of the submitted Donut/Hypixel module ideas.
 * Nothing from the incompatible Meteor-addon entrypoint is installed here.
 * Server-specific and experimental modules default to OFF.
 */
public final class DrainerServerMacros {
    private DrainerServerMacros() {}

    /** Command modules share one cooldown and never run from GUI screens. */
    private abstract static class CommandModule extends Module {
        protected final NumberSetting seconds = add(new NumberSetting("Interval (seconds)", 30, 3, 600, 1));
        private int ticks;
        CommandModule(String title, Category group, String desc) {super(title,group); describe(desc);}
        protected abstract String command();
        @Override public void onEnable(){ticks = 10;}
        @Override public void onTick(){
            if(mc.player==null || mc.player.connection==null || mc.gui.screen()!=null) return;
            if(--ticks>0)return;
            ticks=seconds.getI()*20;
            String cmd=command();
            if(cmd!=null && !cmd.isBlank() && cmd.length()<=180)
                mc.player.connection.sendCommand(cmd.startsWith("/")?cmd.substring(1):cmd);
        }
    }
    public static final class DonutAutoBuy extends CommandModule {
        private final ModeSetting item = add(new ModeSetting("Item", "diamond", "diamond", "emerald", "totem", "obsidian", "gold", "iron"));
        private final NumberSetting amount = add(new NumberSetting("Quantity",1,1,64,1));
        public DonutAutoBuy(){super("DonutAutoBuy", Category.DONUT,"Issues a configured /buy command. Check prices and the server syntax before use."); seconds.set(45);}
        @Override protected String command(){return "buy "+item.get()+" "+amount.getI();}
    }
    public static final class DonutAutoShard extends CommandModule {
        public DonutAutoShard(){super("DonutAutoShard",Category.DONUT,"Runs the shard-conversion command at an interval."); seconds.set(60);}
        @Override protected String command(){return "shard all";}
    }
    public static final class DonutAutoWarp extends CommandModule {
        private final ModeSetting destination=add(new ModeSetting("Destination","market","market","spawn","home"));
        public DonutAutoWarp(){super("DonutAutoWarp",Category.DONUT,"Issues configured /warp command. The location must exist on the server.");seconds.set(60);}
        @Override protected String command(){return "warp "+destination.get();}
    }
    public static final class DonutAutoRekit extends CommandModule {
        public DonutAutoRekit(){super("DonutAutoRekit",Category.DONUT,"Requests the server kit on a timer, subject to server cooldowns.");seconds.set(120);}
        @Override protected String command(){return "kit";}
    }
    public static final class DonutAutoLog extends Module {
        private final NumberSetting health=add(new NumberSetting("Health threshold",8,1,20,1));
        private final NumberSetting delay=add(new NumberSetting("Confirm ticks",3,1,40,1));
        private int low;
        public DonutAutoLog(){super("DonutAutoLog",Category.DONUT);describe("Disconnect locally after sustained low health. Does not guarantee your character's safety.");}
        @Override public void onTick(){
            if(mc.player==null || mc.player.connection==null){low=0;return;}
            if(mc.player.getHealth()>health.get()){low=0;return;}
            if(++low>=delay.getI()){
                setEnabled(false);
                mc.getConnection().getConnection().disconnect(Component.literal("Drainer auto-log: low health"));
            }
        }
        @Override public void onDisable(){low=0;}
    }
    public static final class DonutPingComp extends Module {
        private final NumberSetting maxPing=add(new NumberSetting("Warning ping (ms)",150,40,1000,10));
        private final BoolSetting hud=add(new BoolSetting("Show warning HUD",true));
        private int ping,check;
        public DonutPingComp(){super("DonutPingComp",Category.DONUT); describe("Displays a high-latency warning; does not alter network packets.");}
        @Override public void onTick(){
            if(mc.player==null||mc.getConnection()==null){ping=0;return;}
            if(--check>0)return;check=20;
            var info=mc.getConnection().getPlayerInfo(mc.player.getUUID());
            ping=info==null?0:info.getLatency();
        }
        @Override public void onHud(GuiGraphicsExtractor g,float delta){
            if(hud.get() && mc.player!=null && ping>maxPing.getI()){
                String s="DR  |  HIGH PING: "+ping+" ms";
                Ui.pill(g,8,44,Ui.textW(s)+14,20);
                Ui.text(g,s,15,50,0xFFFFA8C0);
            }
        }
    }
    public static final class DonutSpawnerDetect extends Module {
        private final NumberSetting radius=add(new NumberSetting("Search radius",8,2,16,1));
        private int ticks,count;
        public DonutSpawnerDetect(){super("DonutSpawnerDetect",Category.DONUT);describe("Counts spawner blocks in a small loaded area. Scan limited to once every 2 seconds.");}
        @Override public void onTick(){
            if(mc.player==null||mc.level==null){count=0;return;}
            if(--ticks>0)return;ticks=40;
            int r=radius.getI(),total=0;
            BlockPos origin=mc.player.blockPosition();
            for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++)for(int y=-r;y<=r;y++){
                BlockPos pos=origin.offset(x,y,z);
                if(!mc.level.getChunkSource().hasChunk(pos.getX()>>4,pos.getZ()>>4))continue;
                if(mc.level.getBlockState(pos).is(Blocks.SPAWNER))total++;
            }
            count=total;
        }
        @Override public void onHud(GuiGraphicsExtractor g,float delta){
            if(mc.player==null || count<=0)return;
            String s="DR  |  Spawners nearby: "+count;
            Ui.pill(g,8,67,Ui.textW(s)+14,20);
            Ui.text(g,s,15,73,0xFFE9C7FF);
        }
    }
    public static final class DonutAutoFarm extends Module {
        private final NumberSetting interval=add(new NumberSetting("Interval (ticks)",5,2,40,1));
        private int timer;
        public DonutAutoFarm(){super("DonutAutoFarm",Category.DONUT);describe("Breaks the crop under your crosshair; does not automatically replant or navigate.");}
        @Override public void onTick(){
            if(mc.player==null||mc.level==null||mc.gameMode==null||mc.gui.screen()!=null)return;
            if(--timer>0)return;timer=interval.getI();
            if(!(mc.hitResult instanceof BlockHitResult hit))return;
            var type=mc.level.getBlockState(hit.getBlockPos()).getBlock();
            if(type!=Blocks.WHEAT && type!=Blocks.CARROTS && type!=Blocks.POTATOES && type!=Blocks.BEETROOTS && type!=Blocks.NETHER_WART)return;
            mc.gameMode.continueDestroyBlock(hit.getBlockPos(),hit.getDirection());
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
    }
    /** Covers both submitted DonutChestSteal and DonutAutoSellChest: their behavior was identical. */
    public static final class DonutChestTransfer extends Module {
        private final NumberSetting interval=add(new NumberSetting("Interval (ticks)",6,3,40,1));
        private int timer;
        public DonutChestTransfer(){super("DonutChestTransfer",Category.DONUT);describe("Quick-moves one item stack from an opened chest per interval.");}
        @Override public void onTick(){
            if(mc.player==null||mc.gameMode==null||!(mc.player.containerMenu instanceof ChestMenu menu) || mc.gui.screen()==null)return;
            if(--timer>0)return;timer=interval.getI();
            int chestSlots=menu.getRowCount()*9;
            for(int i=0;i<chestSlots;i++){
                if(menu.slots.get(i).getItem().isEmpty())continue;
                mc.gameMode.handleContainerInput(menu.containerId,i,0,ContainerInput.QUICK_MOVE,mc.player);
                return;
            }
        }
    }
    public static final class DonutAutoCraft extends Module {
        private final NumberSetting interval=add(new NumberSetting("Interval (ticks)",8,4,80,1));
        private final ModeSetting result=add(new ModeSetting("Result","Diamond block","Diamond block","Gold block","Iron block","Emerald block"));
        private int timer;
        public DonutAutoCraft(){super("DonutAutoCraft",Category.DONUT);describe("Takes the chosen result from a prepared crafting grid; it does not fill recipes.");}
        @Override public void onTick(){
            if(mc.player==null||mc.gameMode==null||mc.gui.screen()==null)return;
            if(!(mc.player.containerMenu instanceof net.minecraft.world.inventory.CraftingMenu menu))return;
            if(--timer>0)return;timer=interval.getI();
            ItemStack stack=menu.slots.get(0).getItem();
            var expected=switch(result.get()){
                case "Gold block" -> Items.GOLD_BLOCK;
                case "Iron block" -> Items.IRON_BLOCK;
                case "Emerald block" -> Items.EMERALD_BLOCK;
                default -> Items.DIAMOND_BLOCK;
            };
            if(!stack.isEmpty() && stack.is(expected))
                mc.gameMode.handleContainerInput(menu.containerId,0,0,ContainerInput.QUICK_MOVE,mc.player);
        }
    }
    public static final class DonutLoadout extends Module {
        private final ModeSetting profile=add(new ModeSetting("Profile","Travel","Travel","Economy","Farming","Combat"));
        public DonutLoadout(){super("DonutLoadout",Category.DONUT);describe("Applies a Drainer-native module profile once, without touching placeholders.");}
        @Override public void onEnable(){
            if(Lovenote.modules==null){setEnabled(false);return;}
            switch(profile.get()){
                case "Travel" -> {toggle("AutoSprint",true);toggle("DonutAutoWarp",false);toggle("DonutAutoFarm",false);}
                case "Economy" -> {toggle("AutoSell",true);toggle("DonutAutoBuy",false);toggle("DonutAutoFarm",false);}
                case "Farming" -> {toggle("DonutAutoFarm",true);toggle("AutoSell",false);toggle("AutoSprint",false);}
                case "Combat" -> {toggle("DonutAutoLog",true);toggle("DonutAutoFarm",false);toggle("AutoSell",false);}
            }
            setEnabled(false);
        }
        private void toggle(String name,boolean enabled){
            for(Module m:Lovenote.modules.all())if(m.name.equals(name) && m!=this && !(m instanceof UnportedModule)){
                m.setEnabled(enabled);break;
            }
        }
    }
    /**
     * Supported-block bridging helper. Uses the existing 26.2 Meteor inventory
     * synchronisation and placement geometry rather than manually changing slots.
     * Requests normal block placement; it does not bypass server checks.
     */
    public static final class HypixelScaffold extends Module {
        private final NumberSetting interval = add(new NumberSetting("Interval (ticks)", 2, 1, 20, 1));
        private final ModeSetting placement = add(new ModeSetting("Placement", "Ahead", "Ahead", "Underfoot"));
        private final NumberSetting lookAhead = add(new NumberSetting("Look ahead (blocks)", 0.65, 0, 1.5, 0.05));
        private final BoolSetting autoSwitch = add(new BoolSetting("Auto-switch blocks", true));
        private final BoolSetting rotate = add(new BoolSetting("Rotate to placement", true));
        private final BoolSetting onlyMoving = add(new BoolSetting("Only while moving", false));
        private final BoolSetting holdUse = add(new BoolSetting("Require use key", false));
        private final BoolSetting debugHud = add(new BoolSetting("Show status HUD", false));
        private int timer;
        private String status = "Ready";

        public HypixelScaffold() {
            super("HypixelScaffold", Category.HYPIXEL);
            alsoIn.add(Category.RAGE);
            describe("Bridging helper using supported 26.2 placement and synced hotbar; requires ordinary server-permitted placements.");
        }

        @Override public void onEnable() { timer = 0; status = "Ready"; }
        @Override public void onDisable() { timer = 0; status = "Ready"; }

        private static boolean solidHotbarBlock(ItemStack stack, net.minecraft.client.multiplayer.ClientLevel level, BlockPos pos) {
            if (stack.isEmpty() || !(stack.getItem() instanceof net.minecraft.world.item.BlockItem item)) return false;
            net.minecraft.world.level.block.Block block = item.getBlock();
            if (block instanceof net.minecraft.world.level.block.FallingBlock) return false;
            return net.minecraft.world.level.block.Block.isShapeFullBlock(
                block.defaultBlockState().getCollisionShape(level, pos));
        }

        @Override public void onTick() {
            if (mc.player == null || mc.level == null || mc.gameMode == null || mc.gui.screen() != null) return;
            var velocity = mc.player.getDeltaMovement();
            if (onlyMoving.get() && velocity.x * velocity.x + velocity.z * velocity.z < 0.0005) {
                status = "Not moving";
                return;
            }
            if (holdUse.get() && !mc.options.keyUse.isDown()) { status = "Hold use key"; return; }
            if (timer-- > 0) return;
            timer = interval.getI() - 1;

            BlockPos feet = mc.player.blockPosition().below();
            var blocks = meteordevelopment.meteorclient.utils.player.InvUtils.findInHotbar(
                s -> solidHotbarBlock(s, mc.level, feet));
            if (!blocks.found()) { status = "No solid blocks in hotbar"; return; }
            if (!autoSwitch.get() && blocks.getHand() == null) { status = "Select a block"; return; }

            double leadX = placement.is("Ahead") ? Math.max(-1.5, Math.min(1.5, velocity.x * 3.0)) * lookAhead.get() : 0;
            double leadZ = placement.is("Ahead") ? Math.max(-1.5, Math.min(1.5, velocity.z * 3.0)) * lookAhead.get() : 0;
            BlockPos wanted = BlockPos.containing(mc.player.getX() + leadX,
                    mc.player.getY() - 0.65, mc.player.getZ() + leadZ);
            int y = feet.getY();

            // Prefer the path ahead but recover onto a supported block near the feet.
            // Never fabricate a hit face or send air-place attempts.
            java.util.ArrayList<BlockPos> candidates = new java.util.ArrayList<>();
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                BlockPos candidate = new BlockPos(wanted.getX() + dx, y, wanted.getZ() + dz);
                if (candidate.distSqr(feet) > 5.0) continue;
                candidates.add(candidate);
            }
            candidates.sort(java.util.Comparator.comparingDouble(p -> p.distSqr(wanted)));

            for (BlockPos candidate : candidates) {
                if (!mc.level.getChunkSource().hasChunk(candidate.getX() >> 4, candidate.getZ() >> 4)) continue;
                ItemStack selectedStack = blocks.isOffhand() ? mc.player.getOffhandItem()
                    : mc.player.getInventory().getItem(blocks.slot());
                var blockItem = (net.minecraft.world.item.BlockItem) selectedStack.getItem();
                if (!meteordevelopment.meteorclient.utils.world.BlockUtils.canPlaceBlock(candidate, true, blockItem.getBlock())) continue;
                var side = meteordevelopment.meteorclient.utils.world.BlockUtils.getPlaceSide(candidate);
                if (side == null) continue;
                BlockPos neighbor = candidate.relative(side);
                if (mc.player.getEyePosition().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(neighbor)) > 25.0) continue;
                boolean sent = meteordevelopment.meteorclient.utils.world.BlockUtils.place(
                    candidate, blocks, rotate.get(), 50, true, true);
                if (sent) { status = "Placement requested"; return; }
            }
            status = "No supported block in reach";
        }

        @Override public void onHud(GuiGraphicsExtractor g, float delta) {
            if (!debugHud.get() || mc.player == null) return;
            String s = "Scaffold: " + status;
            Ui.pill(g, 8, 112, Ui.textW(s) + 14, 20);
            Ui.text(g, s, 15, 118, 0xFFCEC3DD);
        }
    }
    public static final class HypixelNoSlow extends Module {
        public HypixelNoSlow(){super("HypixelNoSlow",Category.HYPIXEL);alsoIn.add(Category.RAGE);describe("Experimental: requests sprint while using an item; does not bypass server slowdowns.");}
        @Override public void onTick(){
            if(mc.player==null||mc.gui.screen()!=null)return;
            if(mc.player.isUsingItem() && mc.options.keyUp.isDown())mc.player.setSprinting(true);
        }
    }
    public static final class HypixelAntiVoid extends Module {
        private final NumberSetting threshold=add(new NumberSetting("Minimum Y",0,-64,128,1));
        public HypixelAntiVoid(){super("HypixelAntiVoid",Category.HYPIXEL);alsoIn.add(Category.MOVEMENT);describe("Experimental local upward impulse when falling below Y threshold. No server rescue guarantee.");}
        @Override public void onTick(){
            if(mc.player==null||mc.level==null||mc.gui.screen()!=null)return;
            if(mc.player.getY() < threshold.get() && mc.player.getDeltaMovement().y<-.25){
                var v=mc.player.getDeltaMovement();mc.player.setDeltaMovement(v.x,.42,v.z);
            }
        }
    }
    /** One shared emergency-use implementation instead of duplicated HypixelAutoGap + DonutSpawnGuard. */
    public static final class EmergencyGap extends Module {
        private final NumberSetting health=add(new NumberSetting("Use below health",12,1,20,1));
        private int previousSlot=-1;
        private boolean using, originalUse;
        public EmergencyGap(){super("EmergencyGap",Category.RAGE);alsoIn.add(Category.DONUT);alsoIn.add(Category.HYPIXEL);describe("Attempts to eat a golden apple when health is low. Single shared implementation.");}
        @Override public void onTick(){
            if(mc.player==null||mc.level==null||mc.gui.screen()!=null){stop();return;}
            if(using){if(mc.player.getHealth()>health.get()||!mc.player.isUsingItem())stop();return;}
            if(mc.player.getHealth()>health.get()||mc.options.keyUse.isDown())return;
            var inv=mc.player.getInventory();
            for(int i=0;i<9;i++){
                ItemStack stack=inv.getItem(i);
                if(!stack.is(Items.GOLDEN_APPLE) && !stack.is(Items.ENCHANTED_GOLDEN_APPLE))continue;
                previousSlot=inv.getSelectedSlot();originalUse=mc.options.keyUse.isDown();
                inv.setSelectedSlot(i);mc.options.keyUse.setDown(true);using=true;return;
            }
        }
        private void stop(){
            if(using && mc.options!=null && !originalUse)mc.options.keyUse.setDown(false);
            if(using && mc.player!=null&&previousSlot>=0)mc.player.getInventory().setSelectedSlot(previousSlot);
            using=false;previousSlot=-1;
        }
        @Override public void onDisable(){stop();}
    }
    /** Shared native offhand refill for both Hypixel and Donut; uses bundled Meteor inventory helper. */
    public static final class OffhandTotem extends Module {
        private final NumberSetting interval=add(new NumberSetting("Check interval (ticks)",10,4,40,1));
        private int ticks,spare;
        public OffhandTotem(){super("OffhandTotem",Category.RAGE);alsoIn.add(Category.DONUT);alsoIn.add(Category.HYPIXEL);describe("Moves a spare totem into your offhand. Uses existing client inventory transaction handling.");}
        @Override public void onTick(){
            if(mc.player==null||mc.gameMode==null||mc.gui.screen()!=null)return;
            if(--ticks>0)return;ticks=interval.getI();
            if(mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING))return;
            var found=meteordevelopment.meteorclient.utils.player.InvUtils.find(Items.TOTEM_OF_UNDYING);
            spare=found.count();
            if(found.found()) meteordevelopment.meteorclient.utils.player.InvUtils.move().from(found.slot()).toOffhand();
        }
        @Override public void onHud(GuiGraphicsExtractor g,float delta){
            if(mc.player==null || mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING))return;
            String s="DR  |  Totem missing  (spares: "+spare+")";
            Ui.pill(g,8,90,Ui.textW(s)+14,20);
            Ui.text(g,s,15,96,0xFFFFB8BD);
        }
    }
    public static final class ServerMobAura extends Module {
        private final NumberSetting range=add(new NumberSetting("Range",3,2,4.5,.1));
        private final NumberSetting interval=add(new NumberSetting("Attack interval (ticks)",6,3,20,1));
        private int ticks;
        public ServerMobAura(){super("ServerMobAura",Category.RAGE);alsoIn.add(Category.DONUT);alsoIn.add(Category.HYPIXEL);describe("One shared nearby-mob attack implementation for both tabs; not an anti-cheat bypass.");}
        @Override public void onTick(){
            if(mc.player==null||mc.level==null||mc.gameMode==null||mc.gui.screen()!=null)return;
            if(--ticks>0)return;ticks=interval.getI();
            Entity closest=null;double best=range.get()*range.get();
            for(Entity entity:mc.level.entitiesForRendering()){
                if(entity==mc.player|| !(entity instanceof LivingEntity living)||!living.isAlive()||entity instanceof Player)continue;
                double distance=mc.player.distanceToSqr(entity);
                if(distance<best){closest=entity;best=distance;}
            }
            if(closest!=null){mc.gameMode.attack(mc.player,closest);mc.player.swing(InteractionHand.MAIN_HAND);}
        }
    }
    /** V11: Named recorded slots, replay state, bounded recordings and shared key ownership.
     *  Only movement-key samples are captured. No clicks, chat commands, packets or aiming.
     */
    public static final class MacroRecorder extends Module {
        private final BoolSetting recording=add(new BoolSetting("Recording",false));
        private final BoolSetting playback=add(new BoolSetting("Playback",false));
        private final BoolSetting loop=add(new BoolSetting("Loop replay",false));
        private final BoolSetting clear=add(new BoolSetting("Clear selected slot",false));
        private final ModeSetting slot=add(new ModeSetting("Recording slot","Slot 1","Slot 1","Slot 2","Slot 3","Slot 4","Slot 5"));
        private final NumberSetting maxSeconds=add(new NumberSetting("Max seconds",45,5,120,5));
        private final NumberSetting stopHealth=add(new NumberSetting("Stop below health",6,0,20,1));
        private final BoolSetting recordLook=add(new BoolSetting("Record camera direction",true));
        private final BoolSetting recordMouse=add(new BoolSetting("Record attack/use",true));
        private final com.lovenote.macro.MacroTape[] tapes = {
            new com.lovenote.macro.MacroTape(),new com.lovenote.macro.MacroTape(),
            new com.lovenote.macro.MacroTape(),new com.lovenote.macro.MacroTape(),
            new com.lovenote.macro.MacroTape()};
        private int playIndex, activeSlot=-1;
        private boolean wasRecording, wasPlaying;
        private net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> startedDimension;
        public MacroRecorder(){super("MacroRecorder",Category.MISC);
            alsoIn.add(Category.HYPIXEL); alsoIn.add(Category.DONUT);
            describe("Record/replay input in permitted worlds; disabled on Hypixel. Stops on menus, death, dimension changes and low health.");}
        public boolean isBusy(){return wasRecording || wasPlaying || recording.get() || playback.get();}
        /** Used by native MacroLibrary. Overwrites the selected slot, never executes automatically. */
        public void loadTemplate(int[] frames){loadTemplate(frames,null,null);}
        public void loadTemplate(int[] frames,float[] yaw,float[] pitch){
            if(isBusy())return;
            tapes[slot.index()].setFrom(frames,yaw,pitch);
            playIndex=0;
        }
        /** Server-side rules: do not record or replay gameplay on Hypixel. */
        private boolean onHypixel(){
            if(mc.getCurrentServer()==null)return false;
            String server=mc.getCurrentServer().ip.toLowerCase(java.util.Locale.ROOT);
            return server.equals("hypixel.net") || server.startsWith("hypixel.net:") ||
                server.endsWith(".hypixel.net") || server.matches(".+\\.hypixel\\.net:\\d+");
        }
        /** Called by the Library: arm recording/playback before dismissing the GUI. */
        public boolean queueRecording(){
            if(mc.player==null || mc.level==null || isBusy() || onHypixel())return false;
            if(!enabled)setEnabled(true);
            stopAll();recording.set(true);return true;
        }
        public boolean queuePlayback(){
            if(mc.player==null || mc.level==null || isBusy() || tape().isEmpty() || onHypixel())return false;
            if(!enabled)setEnabled(true);
            stopAll();playback.set(true);return true;
        }
        public void halt(){stopAll();}
        /** An independent snapshot; library saves never modify the live recording. */
        public int[] selectedFrames() {
            com.lovenote.macro.MacroTape current = tape();
            int[] values = new int[current.size()];
            for (int i = 0; i < values.length; i++) values[i] = current.get(i);
            return values;
        }
        public float[] selectedYaw(){
            float[] out=new float[tape().size()];
            for(int i=0;i<out.length;i++)out[i]=tape().getYaw(i);
            return out;
        }
        public float[] selectedPitch(){
            float[] out=new float[tape().size()];
            for(int i=0;i<out.length;i++)out[i]=tape().getPitch(i);
            return out;
        }
        public String selectedSlot() { return slot.get(); }
        private com.lovenote.macro.MacroTape tape(){return tapes[slot.index()];}
        private void stopReplay(){
            com.lovenote.macro.MacroPlayback.release(mc,this);
            wasPlaying=false; playback.set(false); playIndex=0; activeSlot=-1; startedDimension=null;
        }
        private void stopAll(){
            stopReplay(); recording.set(false); wasRecording=false;
            com.lovenote.macro.MacroPlayback.stopRecording();
        }
        @Override public void onEnable(){stopAll();}
        @Override public void onTick(){
            if(onHypixel()){stopAll();return;}
            if(clear.get()){
                stopAll();tape().clear();clear.set(false);return;
            }
            if(mc.player==null || mc.level==null || mc.gui.screen()!=null || !mc.player.isAlive()){
                stopAll();return;
            }
            if((wasPlaying || wasRecording) && activeSlot!=slot.index()){stopAll();return;}
            if(wasPlaying && (mc.player.getHealth()<stopHealth.get()
                    || startedDimension!=mc.level.dimension())) {stopAll();return;}
            if(recording.get()){
                if(wasPlaying)stopReplay();
                if(!wasRecording){tape().clear();wasRecording=true;activeSlot=slot.index();}
                if(tape().size()>=Math.min(com.lovenote.macro.MacroTape.MAX_TICKS,maxSeconds.getI()*20)){
                    recording.set(false);wasRecording=false;
                    com.lovenote.macro.MacroPlayback.stopRecording();return;
                }
                int mask=(mc.options.keyUp.isDown()?1:0)|(mc.options.keyDown.isDown()?2:0)
                    |(mc.options.keyLeft.isDown()?4:0)|(mc.options.keyRight.isDown()?8:0)
                    |(mc.options.keyJump.isDown()?16:0)|(mc.options.keyShift.isDown()?32:0)
                    |(recordMouse.get()&&mc.options.keyAttack.isDown()?64:0)
                    |(recordMouse.get()&&mc.options.keyUse.isDown()?128:0);
                tape().append(mask,recordLook.get()?mc.player.getYRot():Float.NaN,
                    recordLook.get()?mc.player.getXRot():Float.NaN);
                com.lovenote.macro.MacroPlayback.recordStatus(slot.get(),tape().size());
                return;
            }
            if(wasRecording){wasRecording=false;com.lovenote.macro.MacroPlayback.stopRecording();}
            if(!playback.get() || tape().isEmpty()){
                if(wasPlaying)stopReplay();
                playback.set(false);return;
            }
            if(!wasPlaying){
                if(!com.lovenote.macro.MacroPlayback.tryClaim(this)){
                    playback.set(false);return;
                }
                playIndex=0;wasPlaying=true;activeSlot=slot.index();startedDimension=mc.level.dimension();
            }
            if(playIndex>=tape().size()){
                if(loop.get())playIndex=0;
                else {stopReplay();return;}
            }
            int i=playIndex++;
            int mask=tape().get(i);
            com.lovenote.macro.MacroPlayback.apply(mc,this,slot.get(),mask,playIndex,tape().size(),
                tape().getYaw(i),tape().getPitch(i));
        }
        @Override public void onDisable(){stopAll();}
        @Override public void saveExtra(JsonObject o){
            com.google.gson.JsonArray arr=new com.google.gson.JsonArray();
            for(var tape:tapes)arr.add(tape.toJson());
            o.add("macroSlotsV11",arr);
            com.google.gson.JsonArray angles=new com.google.gson.JsonArray();
            for(var tape:tapes)angles.add(tape.rotationsToJson());
            o.add("macroAnglesV15",angles);
        }
        @Override public void loadExtra(JsonObject o){
            stopAll();
            for(var tape:tapes)tape.clear();
            if(o.has("macroSlotsV11") && o.get("macroSlotsV11").isJsonArray()){
                var a=o.getAsJsonArray("macroSlotsV11");
                for(int i=0;i<Math.min(tapes.length,a.size());i++)tapes[i].fromJson(a.get(i));
                if(o.has("macroAnglesV15") && o.get("macroAnglesV15").isJsonArray()){
                    var angles=o.getAsJsonArray("macroAnglesV15");
                    for(int i=0;i<Math.min(tapes.length,angles.size());i++)tapes[i].loadRotations(angles.get(i));
                }
            } else if(o.has("macroSamples")){
                // Migrate the existing V10.1 recording to Slot 1.
                try{
                    for(String p:o.get("macroSamples").getAsString().split(",")){
                        if(tapes[0].size()>=com.lovenote.macro.MacroTape.MAX_TICKS)break;
                        try {tapes[0].append(Integer.parseInt(p));}catch(NumberFormatException ignored){}
                    }
                }catch(RuntimeException ignored){}
            }
        }
    }

}
