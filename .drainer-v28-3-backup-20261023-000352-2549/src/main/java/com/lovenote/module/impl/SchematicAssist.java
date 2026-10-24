package com.lovenote.module.impl;

import com.google.gson.JsonObject;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.schematic.SchematicReader;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Fx;
import com.lovenote.util.Vfx;
import com.lovenote.util.WorldRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import org.joml.Matrix4f;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Gfx;
import com.lovenote.util.HudWidgetBounds;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;

/** Read-only local schematic building aide: a relative block checklist with manual placement. */
public final class SchematicAssist extends Module implements HudWidgetBounds {
    private final BoolSetting showHud=add(new BoolSetting("Show build instructions",true));
    private final BoolSetting compactHud=add(new BoolSetting("Compact instructions",true));
    private final NumberSetting hudScale=add(new NumberSetting("Instruction HUD scale",.65,.35,1.20,.05));
    private final BoolSetting show3D=add(new BoolSetting("3D wireframe preview",true));
    private final NumberSetting previewLimit=add(new NumberSetting("Preview block limit",64,8,160,8));
    private final ModeSetting layerMode=add(new ModeSetting("3D layers","Current layer","Current layer","All layers"));
    private int previewRotation,previewLayer;
    private List<Map.Entry<String,Integer>> materialCounts=List.of();
    private final NumberSetting stepSize=add(new NumberSetting("Blocks per next/previous",1,1,100,1));
    private final NumberSetting x=add(new NumberSetting("HUD X",.54,0,.92,.005));
    private final NumberSetting y=add(new NumberSetting("HUD Y",.17,0,.92,.005));
    private SchematicReader.Design design;
    private int index,anchorX,anchorY,anchorZ;
    private String selected="",message="Drop files into the schematic folder";
    private int dx,dy,dw=175,dh=29;
    private boolean bundledFarmsCopied;
    private String cachedMaterialSummary="No schematic loaded";
    /** Built into the jar as read-only resources; copied once per Prism/game instance on first opening Schems. */
    private static final String[] BUILTIN_FARMS = {
        "Garden_Carrots_96.schem",
        "Garden_Melon_96.schem",
        "Garden_NetherWart_96.schem",
        "Garden_Potatoes_96.schem",
        "Garden_Pumpkin_96.schem",
        "Garden_SugarCane_96.schem",
        "Garden_Wheat_96.schem",
        "Starter_MixedCrops_24.schem",
        "Starter_Wheat_16.schem", "Starter_Carrots_16.schem", "Starter_SugarCane_24.schem",
        "Garden_Cocoa_96.schem", "Garden_Cactus_96.schem",
        "Garden_Mushroom_96.schem", "Mega_Melon_Double_192x96.schem",
        "Mega_Pumpkin_Double_192x96.schem", "Mega_NetherWart_Double_192x96.schem"
    };
    private synchronized void ensureBundledFarms() {
        if (bundledFarmsCopied) return;
        try {
            Path dir=folder();
            Files.createDirectories(dir);
            for (String file: BUILTIN_FARMS) {
                Path dest=dir.resolve(file);
                if (Files.exists(dest)) continue; // Never replace a custom or edited schematic.
                try (InputStream source=SchematicAssist.class.getResourceAsStream("/drainer/farms/"+file)) {
                    if (source==null) {message="Missing bundled schematic: "+file;return;}
                    try {Files.copy(source,dest);} catch(FileAlreadyExistsException ignored) { }
                }
            }
            bundledFarmsCopied=true;
        } catch(IOException e) { message="Could not install farm blueprints: "+e.getMessage(); }
    }

    public SchematicAssist(){super("SchematicAssist",Category.HUD);alsoIn.add(Category.HYPIXEL);alsoIn.add(Category.RENDER);
        describe("Read .schem, .litematic and .nbt into a manual block-by-block build checklist; no auto-placement. Use Macros > Schematics.");}
    public static Path folder(){return meteordevelopment.meteorclient.MeteorClient.FOLDER.toPath().resolve("schematics");}
    public synchronized List<String> files(){
        ensureBundledFarms();
        try{
            Files.createDirectories(folder());
            try(var stream=Files.list(folder())){
                return stream.filter(Files::isRegularFile).map(p->p.getFileName().toString())
                    .filter(s->{String t=s.toLowerCase(Locale.ROOT);return t.endsWith(".schem")||t.endsWith(".litematic")||t.endsWith(".nbt");})
                    .sorted(String.CASE_INSENSITIVE_ORDER).limit(150).toList();
            }
        }catch(IOException ex){message="Folder: "+ex.getMessage();return List.of();}
    }
    public synchronized String message(){return message;}
    public synchronized String file(){return selected;}
    public synchronized boolean loaded(){return design!=null;}
    /** Clear the active Drainer blueprint, overlay, material counts, and 3D preview.
     * This does not manage placements opened independently inside Litematica. */
    public synchronized void unload(){
        design=null;selected="";index=0;
        previewRotation=0;previewLayer=0;
        materialCounts=List.of();cachedMaterialSummary="No schematic loaded";
        message="Schematic unloaded (Drainer preview cleared)";
    }
    public synchronized int total(){return design==null?0:design.blocks().size();}
    public synchronized int currentIndex(){return index;}
    public synchronized String format(){return design==null?"":design.type();}
    public synchronized String topMaterials(){return cachedMaterialSummary;}
    public synchronized int stepSize(){return stepSize.getI();}
    public synchronized int rotation(){return previewRotation;}
    public synchronized int layer(){return previewLayer;}
    public synchronized boolean wireframeEnabled(){return show3D.get();}
    public synchronized void rotate(){previewRotation=(previewRotation+90)%360;message="Blueprint rotated "+previewRotation+" degrees";}
    public synchronized void moveLayer(int step){
        if(design!=null){previewLayer=Math.max(0,Math.min(Math.max(0,design.height()-1),previewLayer+step));
            message="Preview layer "+(previewLayer+1)+" / "+design.height();}
    }
    public synchronized void togglePreview(){show3D.set(!show3D.get());}
    public synchronized int materialTypes(){return materialCounts.size();}
    public synchronized String materialLine(int index){
        if(index<0||index>=materialCounts.size())return "";
        var e=materialCounts.get(index);
        return e.getKey().replace("minecraft:","").replace('_',' ')+"  x"+e.getValue();
    }
    private static String canonical(String block){int i=block.indexOf('[');return i<0?block:block.substring(0,i);}
    private static List<Map.Entry<String,Integer>> countMaterials(SchematicReader.Design input){
        Map<String,Integer> counts=new HashMap<>();
        for(var block:input.blocks())counts.merge(canonical(block.name()),1,Integer::sum);
        return counts.entrySet().stream().sorted((a,b)->Integer.compare(b.getValue(),a.getValue()))
            .map(e->Map.entry(e.getKey(),e.getValue())).toList();
    }
    private static String summarizeMaterials(SchematicReader.Design input){
        Map<String,Integer> counts=new HashMap<>();
        for(var block:input.blocks()){
            String type=block.name();int at=type.indexOf('[');if(at>=0)type=type.substring(0,at);
            counts.merge(type,1,Integer::sum);
        }
        return counts.entrySet().stream().sorted((a,b)->Integer.compare(b.getValue(),a.getValue())).limit(3)
            .map(e->e.getKey()+" x"+e.getValue()).reduce((a,b)->a+" / "+b).orElse("Empty");
    }
    /** User-triggered copy for Litematica's full world hologram; does not inject or bundle that mod. */
    public synchronized boolean exportToLitematica(){
        if(selected.isBlank()){message="Load a schematic first";return false;}
        try{
            Path from=folder().resolve(selected).normalize();
            if(!from.getParent().equals(folder()))throw new IOException("Invalid path");
            Path into=mc.gameDirectory.toPath().resolve("schematics");
            Files.createDirectories(into);
            Path dest=into.resolve(selected);
            if(!Files.exists(dest))Files.copy(from,dest);
            message="Ready in Litematica's schematics folder; load via Litematica";
            return true;
        }catch(IOException|RuntimeException ex){message="Copy failed: "+ex.getMessage();return false;}
    }
    public synchronized String dimensions(){return design==null?"":design.width()+" x "+design.height()+" x "+design.depth();}
    public synchronized boolean load(String filename){
        try{
            Path f=folder().resolve(filename).normalize();
            if(!f.getParent().equals(folder())||!Files.isRegularFile(f))throw new IOException("Invalid file path");
            SchematicReader.Design next=SchematicReader.read(f);
            if(next.blocks().isEmpty())throw new IOException("Schematic contains no non-air blocks");
            design=next;selected=filename;index=0;previewRotation=0;previewLayer=0;
            materialCounts=countMaterials(next);cachedMaterialSummary=summarizeMaterials(next);
            if(mc.player!=null){anchorX=mc.player.blockPosition().getX();anchorY=mc.player.blockPosition().getY();anchorZ=mc.player.blockPosition().getZ();}
            message="Loaded "+next.blocks().size()+" blocks; anchor at player position";
            if(!enabled)setEnabled(true);
            return true;
        }catch(IOException|RuntimeException ex){message="Load failed: "+ex.getMessage();return false;}
    }
    public synchronized void anchorToPlayer(){if(mc.player!=null){anchorX=mc.player.blockPosition().getX();anchorY=mc.player.blockPosition().getY();anchorZ=mc.player.blockPosition().getZ();message="Anchor moved to your current block";}else message="Join a world to set an anchor";}
    public synchronized void step(int delta){
        if(design!=null&&!design.blocks().isEmpty())index=Math.max(0,Math.min(design.blocks().size()-1,index+delta*stepSize.getI()));
    }
    public synchronized String nextInstructions(){
        if(design==null)return "Select a file and press Load";
        SchematicReader.Block b=design.blocks().get(index);
        int[] pos=transformed(b);
        return b.name()+" @ "+pos[0]+", "+pos[1]+", "+pos[2];
    }
    @Override public synchronized void saveExtra(JsonObject o){o.addProperty("fileV16",selected);o.addProperty("indexV16",index);o.addProperty("anchorXV16",anchorX);o.addProperty("anchorYV16",anchorY);o.addProperty("anchorZV16",anchorZ);o.addProperty("rotationV19",previewRotation);o.addProperty("layerV19",previewLayer);}
    @Override public synchronized void loadExtra(JsonObject o){
        if(o.has("fileV16")){String f=o.get("fileV16").getAsString();if(!f.isBlank()&&files().contains(f)){
            if(load(f)){
                if(o.has("indexV16") && design!=null)
                    index=Math.max(0,Math.min(design.blocks().size()-1,o.get("indexV16").getAsInt()));
                if(o.has("anchorXV16"))anchorX=o.get("anchorXV16").getAsInt();
                if(o.has("anchorYV16"))anchorY=o.get("anchorYV16").getAsInt();
                if(o.has("anchorZV16"))anchorZ=o.get("anchorZV16").getAsInt();
                if(o.has("rotationV19"))previewRotation=Math.floorMod(o.get("rotationV19").getAsInt()/90,4)*90;
                if(o.has("layerV19"))previewLayer=Math.max(0,Math.min(design.height()-1,o.get("layerV19").getAsInt()));
            }
        }}
    }
    @Override public int editorX(){return dx;}
    @Override public int editorY(){return dy;}
    @Override public int editorWidth(){return dw;}
    @Override public int editorHeight(){return dh;}

    private int[] transformed(SchematicReader.Block block){
        int bx=block.x(),bz=block.z();
        return switch(previewRotation){
            case 90 -> new int[]{anchorX-bz,anchorY+block.y(),anchorZ+bx};
            case 180 -> new int[]{anchorX-bx,anchorY+block.y(),anchorZ-bz};
            case 270 -> new int[]{anchorX+bz,anchorY+block.y(),anchorZ-bx};
            default -> new int[]{anchorX+bx,anchorY+block.y(),anchorZ+bz};
        };
    }
    /** Lightweight native 3D outlines. For the complete full-block hologram use Litematica. */
    @Override public synchronized void onRender3D(WorldRenderContext ctx,PoseStack stack){
        if(!show3D.get()||design==null||mc.player==null||mc.level==null)return;
        Matrix4f mat=stack.last().pose();
        var cam=ctx.camera().position();
        int drawn=0,limit=VisualOptimizer.budget(previewLimit.getI(),8);
        MeshBuilder mesh=Fx.colorQuads();
        int total=design.blocks().size();
        // Iterate a small neighborhood of the current checklist index, never the full
        // multi-plot blueprint (tens of thousands of blocks) on each render frame.
        int[] dx={0,1,1,0},dz={0,0,1,1};
        int start=Math.max(0,index-96),end=Math.min(total,start+3600);
        int stride=Math.max(1,(end-start)/Math.max(limit*8,1));
        for(int i=start;i<end&&drawn<limit;i+=stride){
            SchematicReader.Block b=design.blocks().get(i);
            if(layerMode.is("Current layer")&&b.y()!=previewLayer)continue;
            int[] xyz=transformed(b);
            double x=xyz[0],y=xyz[1],z=xyz[2];
            if(Math.abs(x-cam.x)>38||Math.abs(y-cam.y)>30||Math.abs(z-cam.z)>38)continue;
            int tint=(i==index)?0xFFE9D0FF:0xFFB387DE;
            int c=Colors.alpha(tint,(i==index)?235:115);
            // Twelve edges of each ghost block, in absolute world coordinates.
            for(int ey=0;ey<=1;ey++){
                for(int k=0;k<4;k++){
                    int j=(k+1)%4;
                    Vfx.tube(mesh,mat,x+dx[k],y+ey,z+dz[k],x+dx[j],y+ey,z+dz[j],0.012f,c,c,cam.x,cam.y,cam.z);
                }
            }
            for(int k=0;k<4;k++){
                Vfx.tube(mesh,mat,x+dx[k],y,z+dz[k],x+dx[k],y+1,z+dz[k],0.012f,c,c,cam.x,cam.y,cam.z);
            }
            drawn++;
        }
        if(drawn>0)Fx.draw();
    }

    private static String truncate(String s,int available){
        if (Ui.textW(s)<=available)return s;
        while(!s.isEmpty()&&Ui.textW(s+"…")>available)s=s.substring(0,s.length()-1);
        return s+"…";
    }
    @Override public synchronized void onHud(GuiGraphicsExtractor g,float delta){
        if(mc.player==null||!showHud.get()||design==null)return;
        boolean compact=compactHud.get();
        int boxW=compact?258:290, boxH=compact?44:68;
        float sc=(float)hudScale.get();
        dw=Math.max(1,Math.min(Gfx.guiW()-4,Math.round(boxW*sc)));
        dh=Math.max(1,Math.min(Gfx.guiH()-4,Math.round(boxH*sc)));
        dx=Math.max(0,Math.min((int)(x.get()*Gfx.guiW()),Gfx.guiW()-dw-2));
        dy=Math.max(0,Math.min((int)(y.get()*Gfx.guiH()),Gfx.guiH()-dh-2));
        SchematicReader.Block b=design.blocks().get(index);
        String raw=b.name().replace("minecraft:","");
        int at=raw.indexOf('[');
        int[] pos=transformed(b);
        String coords=pos[0]+" / "+pos[1]+" / "+pos[2];
        int nameWidth=compact?Math.max(50,boxW-Ui.textW(coords)-25):boxW-17;
        String name=truncate(at>=0?raw.substring(0,at):raw,nameWidth);
        var pose=g.pose();
        pose.pushMatrix();pose.translate(dx,dy);pose.scale(sc,sc);
        Ui.pill(g,0,0,boxW,boxH);
        Ui.text(g,"SCHEM  "+(index+1)+" / "+design.blocks().size(),8,6,0xFFCDA8F0);
        Ui.text(g,name,8,compact?21:24,0xFFF8E6FF);
        if(compact)Ui.text(g,coords,boxW-Ui.textW(coords)-8,21,0xFFD9C9EB);
        else Ui.text(g,"Place at  "+coords,8,44,0xFFD9C9EB);
        pose.popMatrix();
    }
}
