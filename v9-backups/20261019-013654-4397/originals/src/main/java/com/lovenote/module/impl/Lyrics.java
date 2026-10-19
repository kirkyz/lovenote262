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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Stable, uniformly scaled LRCLIB lyric widget. Word timing is estimated from line timing. */
public final class Lyrics extends Module {
    private final NumberSetting x = add(new NumberSetting("X", .66, 0, .98, .005));
    private final NumberSetting y = add(new NumberSetting("Y", .75, 0, .98, .005));
    private final NumberSetting scale = add(new NumberSetting("Scale", .8, .35, 1.6, .05));
    private final NumberSetting timingOffset = add(new NumberSetting("Timing offset (ms)", 0, -10000, 10000, 50));
    private final BoolSetting oneLine = add(new BoolSetting("Single line", true));
    private final BoolSetting hideIdle = add(new BoolSetting("Hide when idle", true));
    private final BoolSetting karaoke = add(new BoolSetting("Word highlight", true));
    private volatile String track = "", status = "Searching lyrics...";
    private volatile List<Line> lines = List.of();
    private record Line(long ms, String text) {}
    private int px, py, w, h;
    private boolean dragging;
    private double dx, dy;

    public Lyrics() {
        super("Lyrics", Category.SPOTIFY);
        describe("LRCLIB line-synced lyrics with estimated word highlighting; adjust Timing offset for your bridge.");
    }
    @Override public void onEnable() { SpotifyManager.I.start(); }
    @Override public void onTick() {
        SpotifyManager m = SpotifyManager.I;
        if (!enabled) return;
        if (!m.hasTrack) { track = ""; lines = List.of(); return; }
        String key = m.title + "|" + m.artist;
        if (key.equals(track)) return;
        track = key; lines = List.of(); status = "Searching lyrics...";
        Thread t = new Thread(() -> fetch(key, m.title, m.artist, m.durationMs), "Drainer-Lyrics");
        t.setDaemon(true);
        t.start();
    }
    private static String norm(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }
    private static List<Line> parse(String raw) {
        List<Line> out = new ArrayList<>();
        for (String row : raw.split("\\R")) {
            int end = row.indexOf(']');
            if (!row.startsWith("[") || end < 0) continue;
            String[] p = row.substring(1, end).split(":");
            if (p.length != 2) continue;
            try {
                long ms = Math.round((Integer.parseInt(p[0]) * 60 + Double.parseDouble(p[1])) * 1000);
                String words = row.substring(end+1).trim();
                if (!words.isEmpty()) out.add(new Line(ms, words));
            } catch (NumberFormatException ignored) { }
        }
        out.sort(Comparator.comparingLong(Line::ms));
        return out;
    }
    private void fetch(String key, String title, String artist, long duration) {
        try {
            String url = "https://lrclib.net/api/search?track_name=" + URLEncoder.encode(title, StandardCharsets.UTF_8)
                + "&artist_name=" + URLEncoder.encode(artist, StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).header("User-Agent", "Drainer-Lyrics/5.0")
                .timeout(java.time.Duration.ofSeconds(9)).build();
            String body = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString()).body();
            var arr = JsonParser.parseString(body).getAsJsonArray();
            List<Line> best = List.of(); double bestScore = Double.MAX_VALUE;
            for (var element : arr) {
                JsonObject o = element.getAsJsonObject();
                if (!o.has("syncedLyrics") || o.get("syncedLyrics").isJsonNull()) continue;
                List<Line> parsed = parse(o.get("syncedLyrics").getAsString());
                if (parsed.isEmpty()) continue;
                String candidateTitle = o.has("trackName") ? o.get("trackName").getAsString() : "";
                String candidateArtist = o.has("artistName") ? o.get("artistName").getAsString() : "";
                double score = 0;
                if (!norm(title).equals(norm(candidateTitle))) score += 50;
                if (!norm(artist).equals(norm(candidateArtist))) score += 25;
                if (duration > 0 && o.has("duration") && !o.get("duration").isJsonNull()) {
                    double diff = Math.abs(duration / 1000.0 - o.get("duration").getAsDouble());
                    score += diff > 8 ? 100 + diff : diff;
                }
                if (score < bestScore) { bestScore = score; best = parsed; }
            }
            if (track.equals(key)) { lines = List.copyOf(best); status = best.isEmpty() ? "No timed lyrics found" : ""; }
        } catch (Exception ex) { if (track.equals(key)) status = "Lyrics unavailable"; }
    }
    public boolean contains(double mx, double my) { return enabled && mx >= px && mx < px+w && my >= py && my < py+h; }
    public int editorX() { return px; }
    public int editorY() { return py; }
    public int editorWidth() { return w; }
    public int editorHeight() { return h; }
    public void editorBeginDrag(double mx, double my) { dragging = true; dx = mx-px; dy = my-py; }
    public void editorStopDrag() { dragging = false; }
    public void editorMove(double mx, double my) {
        if (!dragging) return;
        x.set(Math.max(0, Math.min(Gfx.guiW()-w, mx-dx)) / Math.max(1, Gfx.guiW()));
        y.set(Math.max(0, Math.min(Gfx.guiH()-h, my-dy)) / Math.max(1, Gfx.guiH()));
    }
    public boolean click(double mx, double my, boolean down) {
        if (!down) { editorStopDrag(); return false; }
        if (!contains(mx,my)) return false;
        editorBeginDrag(mx,my); return true;
    }
    public void move(double mx, double my, boolean down) { if (down) editorMove(mx,my); else editorStopDrag(); }
    private static String fit(String s, int max) {
        if (s == null) return "";
        if (Ui.textW(s) <= max) return s;
        while (!s.isEmpty() && Ui.textW(s + "…") > max) s=s.substring(0,s.length()-1);
        return s + "…";
    }
    private void drawWords(GuiGraphicsExtractor g, Line line, long start, long end, int textX, int textY, int limit) {
        String s = fit(line.text, limit);
        if (s.isEmpty()) return;
        if (!karaoke.get() || end <= start) { Ui.text(g, s, textX, textY, 0xFFF0B8DD); return; }
        String[] words = s.split(" ");
        int weighted = 0;
        for (String word : words) weighted += Math.max(2, word.length());
        double elapsed = Math.max(0, Math.min(1, (double) (SpotifyManager.I.progress()+timingOffset.getI()-start) / (end-start)));
        int budget = (int) Math.round(elapsed * weighted), used = 0, cursor = textX;
        for (int i=0; i<words.length; i++) {
            String token = words[i] + (i == words.length-1 ? "" : " ");
            used += Math.max(2, words[i].length());
            Ui.text(g, token, cursor, textY, used <= budget ? 0xFFFFA9DB : 0xFF8C7C9C);
            cursor += Ui.textW(token);
        }
    }
    @Override public void onHud(GuiGraphicsExtractor g, float delta) {
        SpotifyManager m = SpotifyManager.I;
        if (!m.hasTrack && hideIdle.get()) return;
        double sc = Math.max(.35, Math.min(1.6, scale.get()));
        boolean single = oneLine.get();
        int bw = single ? 324 : 352, bh = single ? 62 : 110;
        w=(int)Math.round(bw*sc); h=(int)Math.round(bh*sc);
        px=Math.max(0,Math.min((int)(x.get()*Gfx.guiW()),Math.max(0,Gfx.guiW()-w)));
        py=Math.max(0,Math.min((int)(y.get()*Gfx.guiH()),Math.max(0,Gfx.guiH()-h)));
        var pose = g.pose();
        pose.pushMatrix(); pose.translate(px, py); pose.scale((float)sc,(float)sc);
        Ui.rrect(g,-1,-1,bw+2,bh+2,9,0x80523C61);
        Ui.rrect(g,0,0,bw,bh,8,0xEE110E1D);
        Ui.rect(g,12,5,bw-24,2,0xFFC68CE0);
        Ui.text(g,"LIVE LYRICS",13,13,0xFFB595CB);
        List<Line> ls = lines;
        if (ls.isEmpty()) {
            Ui.text(g,fit(status,bw-28),13,35,0xFFC6ACC9);
        } else {
            long pos = Math.max(0, m.progress()+timingOffset.getI());
            int idx = -1;
            for (int i=0; i<ls.size(); i++) if (ls.get(i).ms <= pos) idx = i; else break;
            if (idx < 0) {
                Ui.text(g, "Waiting for first line...",13,35,0xFF9B899F);
            } else {
                long start=ls.get(idx).ms;
                long end=idx+1<ls.size()?ls.get(idx+1).ms:Math.max(start+3000,m.durationMs);
                drawWords(g,ls.get(idx),start,end,13,single?36:49,bw-28);
                if (!single) {
                    if (idx>0) Ui.text(g,fit(ls.get(idx-1).text,bw-28),13,31,0xFF776F88);
                    if (idx+1<ls.size()) Ui.text(g,fit(ls.get(idx+1).text,bw-28),13,72,0xFF867B96);
                }
            }
        }
        pose.popMatrix();
    }
}
