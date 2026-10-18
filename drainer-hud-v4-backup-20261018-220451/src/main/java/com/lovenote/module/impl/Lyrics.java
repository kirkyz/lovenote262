package com.lovenote.module.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.spotify.SpotifyManager;
import com.lovenote.util.Gfx;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Optional independent compact lyrics panel; LRCLIB data is fetched only when enabled. */
public final class Lyrics extends Module {
    private final NumberSetting x = add(new NumberSetting("X", .66, 0, .98, .005));
    private final NumberSetting y = add(new NumberSetting("Y", .75, 0, .98, .005));
    private final NumberSetting scale = add(new NumberSetting("Scale", 1, .35, 1.6, .05));
    private final BoolSetting oneLine = add(new BoolSetting("Single line", true));
    private final BoolSetting hideIdle = add(new BoolSetting("Hide when idle", true));
    private volatile String track = "", status = "Searching lyrics...";
    private volatile List<Line> lines = List.of();
    private record Line(long ms, String text) {}
    private int px,py,w,h;
    private boolean dragging;
    private double dx,dy;
    public Lyrics() { super("Lyrics", Category.HUD); describe("Independent, movable synced lyrics from LRCLIB (internet required)."); }
    @Override public void onEnable() { SpotifyManager.I.start(); }
    @Override public void onTick() {
        SpotifyManager m=SpotifyManager.I;
        if(!enabled || !m.hasTrack) return;
        String key=m.title+"|"+m.artist;
        if(key.equals(track)) return;
        track=key;lines=List.of();status="Searching lyrics...";
        Thread t=new Thread(()->fetch(key,m.title,m.artist),"Drainer-Lyrics");t.setDaemon(true);t.start();
    }
    private void fetch(String key,String title,String artist) {
        try {
            String url="https://lrclib.net/api/search?track_name="+URLEncoder.encode(title,StandardCharsets.UTF_8)+"&artist_name="+URLEncoder.encode(artist,StandardCharsets.UTF_8);
            HttpRequest req=HttpRequest.newBuilder(URI.create(url)).header("User-Agent","Drainer-MediaHUD/3.0").timeout(java.time.Duration.ofSeconds(8)).build();
            String body=HttpClient.newHttpClient().send(req,HttpResponse.BodyHandlers.ofString()).body();
            var arr=JsonParser.parseString(body).getAsJsonArray();
            List<Line> parsed=new ArrayList<>();
            for(var element:arr) {
                JsonObject o=element.getAsJsonObject();
                if(!o.has("syncedLyrics") || o.get("syncedLyrics").isJsonNull()) continue;
                for(String row:o.get("syncedLyrics").getAsString().split("\\R")) {
                    if(!row.startsWith("[") || row.indexOf(']')<0) continue;
                    int end=row.indexOf(']');
                    String[] parts=row.substring(1,end).split(":");
                    if(parts.length!=2) continue;
                    try {long ms=(long)((Integer.parseInt(parts[0])*60+Double.parseDouble(parts[1]))*1000);String txt=row.substring(end+1).trim();if(!txt.isEmpty())parsed.add(new Line(ms,txt));}catch(NumberFormatException ignored){}
                }
                if(!parsed.isEmpty())break;
            }
            if(track.equals(key)) {lines=List.copyOf(parsed);status=parsed.isEmpty()?"No synced lyrics found":"";}
        } catch(Exception e) {if(track.equals(key))status="Lyrics unavailable";}
    }
    public boolean click(double mx,double my,boolean down) {
        if(!enabled)return false;
        if(!down){dragging=false;return false;}
        if(mx<px||mx>px+w||my<py||my>py+h)return false;
        dragging=true;dx=mx-px;dy=my-py;return true;
    }
    public void move(double mx,double my,boolean down){
        if(!down){dragging=false;return;}
        if(dragging){x.set(Math.max(0,mx-dx)/Math.max(1,Gfx.guiW()));y.set(Math.max(0,my-dy)/Math.max(1,Gfx.guiH()));}
    }
    @Override public void onHud(GuiGraphicsExtractor g,float delta){
        SpotifyManager m=SpotifyManager.I;
        if(!m.hasTrack&&hideIdle.get())return;
        double sc=scale.get()*Math.max(.5,Ui.scale);
        w=(int)((oneLine.get()?210:250)*sc);h=(int)((oneLine.get()?36:67)*sc);
        px=Math.max(0,Math.min((int)(x.get()*Gfx.guiW()),Math.max(0,Gfx.guiW()-w)));
        py=Math.max(0,Math.min((int)(y.get()*Gfx.guiH()),Math.max(0,Gfx.guiH()-h)));
        Ui.rrect(g,px-2,py-2,w+4,h+4,7,0x503D1B60);
        Ui.rrect(g,px,py,w,h,6,0xE9100B19);
        Ui.rect(g,px+7,py+2,w-14,1,Ui.accent1);
        List<Line> ls=lines;
        if(ls.isEmpty()) {Ui.text(g,status,px+10,py+Math.max(9,h/2-5),0xFFBBA7C8);return;}
        long pos=m.progress();int idx=0;
        for(int i=0;i<ls.size();i++)if(ls.get(i).ms<=pos)idx=i;else break;
        if(oneLine.get()) Ui.text(g,clip(ls.get(idx).text,34),px+10,py+Math.max(9,h/2-5),Ui.accent1);
        else for(int off=-1;off<=1;off++) {int n=idx+off;if(n>=0&&n<ls.size())Ui.text(g,clip(ls.get(n).text,39),px+10,py+10+(off+1)*16,off==0?Ui.accent1:0xFF8D7A9D);}
    }
    private static String clip(String s,int n){return s.length()>n?s.substring(0,n-3)+"...":s;}
}
