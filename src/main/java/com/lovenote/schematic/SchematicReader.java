package com.lovenote.schematic;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Size-limited, read-only NBT importer. Supports Sponge .schem, Litematica .litematic, vanilla .nbt. */
public final class SchematicReader {
    private SchematicReader() { }
    public record Block(int x,int y,int z,String name) { }
    public record Design(String name,int width,int height,int depth,List<Block> blocks,String type) { }
    private static final int MAX_SOURCE=24*1024*1024, MAX_BYTES=90*1024*1024, MAX_BLOCKS=500_000;
    private static Map<String,Object> compound(Object v){return v instanceof Map<?,?> m?(Map<String,Object>)m:Map.of();}
    private static List<Object> list(Object v){return v instanceof List<?> l?(List<Object>)l:List.of();}
    private static int number(Object value,int def){return value instanceof Number n?n.intValue():def;}
    private static String str(Object v){return v instanceof String s?s:"";}
    private static int n(Map<String,Object> map,String key){return number(map.get(key),0);}
    private static String blockName(Object paletteEntry){
        String s=str(compound(paletteEntry).get("Name"));return s.isEmpty()?"minecraft:air":s;
    }
    private static boolean filled(String block){return !block.equals("minecraft:air")&&!block.equals("air")&&!block.equals("minecraft:void_air")&&!block.equals("minecraft:cave_air");}
    private static int safeVolume(int x,int y,int z) throws IOException {
        if(x<1||y<1||z<1||x>2048||y>2048||z>2048||(long)x*y*z>MAX_BLOCKS)
            throw new IOException("Schematic dimensions exceed safety limit (500,000 blocks)");
        return x*y*z;
    }
    public static Design read(Path path) throws IOException {
        long fileSize=Files.size(path);
        if(fileSize==0||fileSize>MAX_SOURCE)throw new IOException("Schematic file must be below 24 MB");
        String ext=path.getFileName().toString().toLowerCase(Locale.ROOT);
        if(!ext.endsWith(".schem")&&!ext.endsWith(".litematic")&&!ext.endsWith(".nbt"))throw new IOException("Unsupported file extension");
        Object parsed;
        try(BufferedInputStream source=new BufferedInputStream(Files.newInputStream(path))){
            source.mark(2);int a=source.read(),b=source.read();source.reset();
            InputStream stream=(a==31&&b==139)?new GZIPInputStream(source):source;
            try(DataInputStream in=new DataInputStream(new CappedInputStream(stream,MAX_BYTES))){
                if(in.readUnsignedByte()!=10)throw new IOException("Not an NBT compound");
                in.readUTF();parsed=readPayload(in,10,0);
            }
        }
        Map<String,Object> root=compound(parsed);
        if(root.containsKey("Schematic"))root=compound(root.get("Schematic"));
        if(ext.endsWith(".litematic"))return litematic(root,path.getFileName().toString());
        if(ext.endsWith(".schem"))return sponge(root,path.getFileName().toString());
        return vanilla(root,path.getFileName().toString());
    }
    private static Design sponge(Map<String,Object> root,String name)throws IOException {
        Map<String,Object> blocks=compound(root.get("Blocks"));
        Map<String,Object> rawPalette=compound(root.containsKey("Palette")?root.get("Palette"):blocks.get("Palette"));
        Object data=root.containsKey("BlockData")?root.get("BlockData"):blocks.get("Data");
        int width=n(root,"Width"),height=n(root,"Height"),depth=n(root,"Length");
        int size=safeVolume(width,height,depth);
        if(!(data instanceof byte[] bytes))throw new IOException("Missing Sponge BlockData");
        Map<Integer,String> palette=new HashMap<>();
        for(var entry:rawPalette.entrySet())palette.put(number(entry.getValue(),-1),entry.getKey());
        List<Block> out=new ArrayList<>();int p=0;
        for(int index=0;index<size;index++){
            int id=0,shift=0;
            while(true){if(p>=bytes.length||shift>28)throw new IOException("Invalid Sponge varint block data");int v=bytes[p++]&255;id|=(v&127)<<shift;if((v&128)==0)break;shift+=7;}
            String type=palette.getOrDefault(id,"minecraft:air");
            if(filled(type))out.add(new Block(index%width,index/(width*depth),index/width%depth,type));
        }
        return new Design(name,width,height,depth,List.copyOf(out),"Sponge .schem");
    }
    private static Design litematic(Map<String,Object> root,String name)throws IOException {
        Map<String,Object> regions=compound(root.get("Regions"));
        if(regions.isEmpty())throw new IOException("No Litematica regions");
        List<Block> out=new ArrayList<>();int mw=1,mh=1,md=1;
        for(var regionEntry:regions.entrySet()){
            Map<String,Object> region=compound(regionEntry.getValue());
            Map<String,Object> dimension=compound(region.get("Size"));
            int sx=n(dimension,"x"),sy=n(dimension,"y"),sz=n(dimension,"z");
            int w=Math.abs(sx),h=Math.abs(sy),d=Math.abs(sz);
            int count=safeVolume(w,h,d);
            Map<String,Object> position=compound(region.get("Position"));
            int originX=n(position,"x"),originY=n(position,"y"),originZ=n(position,"z");
            List<Object> sourcePalette=list(region.get("BlockStatePalette"));
            if(sourcePalette.isEmpty()||!(region.get("BlockStates") instanceof long[] states))throw new IOException("Missing Litematica palette/states");
            int bits=Math.max(2,32-Integer.numberOfLeadingZeros(sourcePalette.size()-1));
            long mask=(1L<<bits)-1;
            for(int i=0;i<count;i++){
                long bit=(long)i*bits;int word=(int)(bit>>>6);int bitIndex=(int)(bit&63);
                if(word>=states.length)throw new IOException("Truncated Litematica block states");
                long id=(states[word]>>>bitIndex)&mask;
                if(bitIndex+bits>64){if(word+1>=states.length)throw new IOException("Truncated palette bits");id|=(states[word+1]<<(64-bitIndex))&mask;}
                if(id>=sourcePalette.size())continue;
                String type=blockName(sourcePalette.get((int)id));
                if(filled(type)){
                    int x=i%w,y=i/(w*d),z=(i/w)%d;
                    out.add(new Block(originX+(sx<0?-x:x),originY+(sy<0?-y:y),originZ+(sz<0?-z:z),type));
                    if(out.size()>MAX_BLOCKS)throw new IOException("Too many placed blocks");
                }
            }
            mw=Math.max(mw,Math.abs(originX)+w);mh=Math.max(mh,Math.abs(originY)+h);md=Math.max(md,Math.abs(originZ)+d);
        }
        return new Design(name,mw,mh,md,List.copyOf(out),"Litematica .litematic");
    }
    private static Design vanilla(Map<String,Object> root,String name)throws IOException {
        List<Object> rawSize=list(root.get("size"));
        if(rawSize.size()<3)throw new IOException("Vanilla structure has no size");
        int w=number(rawSize.get(0),0),h=number(rawSize.get(1),0),d=number(rawSize.get(2),0);safeVolume(w,h,d);
        List<Object> palette=list(root.get("palette"));
        if(palette.isEmpty()){List<Object> palettes=list(root.get("palettes"));if(!palettes.isEmpty())palette=list(palettes.get(0));}
        List<Block> out=new ArrayList<>();
        for(Object obj:list(root.get("blocks"))){
            Map<String,Object> b=compound(obj);List<Object> pos=list(b.get("pos"));if(pos.size()<3)continue;
            int state=n(b,"state");if(state<0||state>=palette.size())continue;
            String type=blockName(palette.get(state));if(filled(type))out.add(new Block(number(pos.get(0),0),number(pos.get(1),0),number(pos.get(2),0),type));
        }
        return new Design(name,w,h,d,List.copyOf(out),"Vanilla .nbt");
    }
    private static Object readPayload(DataInputStream in,int type,int depth)throws IOException {
        if(depth>40)throw new IOException("NBT nesting too deep");
        return switch(type){
            case 1 -> in.readByte();case 2 -> in.readShort();case 3 -> in.readInt();case 4 -> in.readLong();
            case 5 -> in.readFloat();case 6 -> in.readDouble();
            case 7 -> {int n=in.readInt();require(n,MAX_BYTES);byte[] a=new byte[n];in.readFully(a);yield a;}
            case 8 -> in.readUTF();
            case 9 -> {int subtype=in.readUnsignedByte();int n=in.readInt();require(n,MAX_BLOCKS*4);List<Object> list=new ArrayList<>(Math.min(n,100000));for(int i=0;i<n;i++)list.add(readPayload(in,subtype,depth+1));yield list;}
            case 10 -> {Map<String,Object> map=new LinkedHashMap<>();int count=0;while(true){int subtype=in.readUnsignedByte();if(subtype==0)break;if(++count>100000)throw new IOException("NBT compound too large");String key=in.readUTF();map.put(key,readPayload(in,subtype,depth+1));}yield map;}
            case 11 -> {int n=in.readInt();require(n,MAX_BLOCKS*4);int[] a=new int[n];for(int i=0;i<n;i++)a[i]=in.readInt();yield a;}
            case 12 -> {int n=in.readInt();require(n,MAX_BLOCKS*4);long[] a=new long[n];for(int i=0;i<n;i++)a[i]=in.readLong();yield a;}
            default -> throw new IOException("Unsupported NBT tag "+type);
        };
    }
    private static void require(int len,int max)throws IOException{if(len<0||len>max)throw new IOException("NBT size exceeds allowed limit");}
    private static final class CappedInputStream extends FilterInputStream {
        int remaining;CappedInputStream(InputStream stream,int limit){super(stream);remaining=limit;}
        @Override public int read()throws IOException{if(remaining==0)throw new IOException("NBT decompression limit exceeded");int r=super.read();if(r!=-1)remaining--;return r;}
        @Override public int read(byte[] b,int off,int len)throws IOException{if(remaining==0)throw new IOException("NBT decompression limit exceeded");int r=in.read(b,off,Math.min(len,remaining));if(r>0)remaining-=r;return r;}
    }
}
