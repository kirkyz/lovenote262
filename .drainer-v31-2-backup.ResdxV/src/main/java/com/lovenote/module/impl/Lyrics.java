package com.lovenote.module.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.spotify.SpotifyManager;
import com.lovenote.util.Gfx;
import com.lovenote.util.HudGlass;
import com.lovenote.util.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Stable, uniformly scaled LRCLIB lyric widget. Word timing is estimated from line timing. */
public final class Lyrics extends Module {
    private static Lyrics instance;
    public static Lyrics instance() { return instance; }
    private final NumberSetting x = add(new NumberSetting("X", .66, 0, .98, .005));
    private final NumberSetting y = add(new NumberSetting("Y", .75, 0, .98, .005));
    private final NumberSetting scale = add(new NumberSetting("Scale", .95, .25, 1.6, .05));
    private final NumberSetting timingOffset = add(new NumberSetting("Timing offset (ms)", 0, -10000, 10000, 50));
    private final NumberSetting maxHold = add(new NumberSetting("Max lyric hold (ms)", 5500, 2000, 12000, 250));
    private final BoolSetting oneLine = add(new BoolSetting("Single line", false));
    private final BoolSetting hideIdle = add(new BoolSetting("Hide when idle", true));
    private final NumberSetting idleDelay = add(new NumberSetting("Hide after pause (s)", 15, 0, 120, 5));
    private final BoolSetting karaoke = add(new BoolSetting("Word highlight", true));
    private volatile String track = "", status = "Searching lyrics...";
    private volatile List<Line> lines = List.of();
    // Optional *unsynchronised* fallback, not represented as fake timestamps.
    private volatile List<String> plainLines = List.of();
    private volatile boolean fallbackAttempted;
    private static final String FALLBACK_URI = "https://api.lyrics.ovh/v1/";
    private volatile long nextRetryAt;
    private volatile int fetchAttempts;
    private volatile boolean fetchInProgress;
    private volatile long fetchStartedNanos;
    // A generation prevents a late A->B->A response from overwriting newer lyrics.
    private volatile long lyricGeneration;
    private volatile long trackDurationBucket = -1L;
    private static final HttpClient LYRICS_HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(6)).followRedirects(HttpClient.Redirect.NORMAL).build();
    // Transient 503/429 errors must never permanently lock out a track.
    private static final long MAX_RETRY_MS = 180_000L;
    // Shared circuit-breaker: do not make two requests for every song while
    // LRCLIB responds with 503 / 429, especially during playlist skipping.
    private static volatile long providerRetryAt;
    private static volatile String providerStatus = "Lyrics provider unavailable — retrying";
    private enum ExactResult { FOUND, NOT_FOUND, RETRY_LATER }
    // At most one LRCLIB request in flight and one pending, even during rapid skipping.
    // Avoids accumulating network threads and requests as the Spotify track changes.
    private static final ThreadPoolExecutor LYRICS_WORKER = new ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1),
        r -> { Thread t = new Thread(r, "Drainer-Lyrics"); t.setDaemon(true); return t; },
        new ThreadPoolExecutor.AbortPolicy());
    private record Line(long ms, String text) {}
    private int px, py, w, h;
    private boolean dragging;
    private double dx, dy;

    public Lyrics() {
        super("Lyrics", Category.SPOTIFY);
        instance = this;
        describe("LRCLIB line-synced lyrics with estimated word highlighting; adjust Timing offset for your bridge.");
    }
    @Override public void onEnable() { SpotifyManager.I.start(); }
    private static long durationBucket(long ms) {
        return ms > 0 ? Math.round(ms / 5000.0) : 0L;
    }
    private boolean isCurrent(String key, long generation) {
        return (enabled || SpotifyHud.isIntegratedLyricsActive())
            && generation == lyricGeneration && key.equals(track);
    }
    @Override public void onTick() {
        SpotifyManager m = SpotifyManager.I;
        if (!enabled && !SpotifyHud.isIntegratedLyricsActive()) return;
        if (!m.hasTrack) {
            if (!track.isEmpty()) lyricGeneration++;
            track = ""; trackDurationBucket = -1L;
            lines = List.of(); plainLines = List.of(); fallbackAttempted = false; fetchAttempts = 0;
            nextRetryAt = 0; fetchInProgress = false;
            return;
        }
        String key = m.title + "|" + m.artist;
        long bucket = durationBucket(m.durationMs);
        if (!key.equals(track) || bucket != trackDurationBucket) {
            lyricGeneration++;
            track = key; trackDurationBucket = bucket;
            lines = List.of(); plainLines = List.of(); fallbackAttempted = false; status = "Finding timed lyrics...";
            fetchAttempts = 0; nextRetryAt = 0; fetchInProgress = false;
            // A successful LRCLIB response is stored under Minecraft's config folder.
            // Reading it once per track avoids unnecessary requests during outages.
            List<Line> cached = loadCached(key, m.durationMs);
            if (cached.isEmpty()) cached = loadManual(m.title, m.artist);
            if (!cached.isEmpty()) { lines = cached; status = ""; }
            else {
                List<String> offlinePlain=loadPlainCached(key);
                if (!offlinePlain.isEmpty()) {
                    plainLines=offlinePlain; fallbackAttempted=true; status="Plain lyrics (timed search pending)";
                }
            }
        }
        // DiscardOldestPolicy drops queued work on rapid track skipping; if a
        // queued task is dropped, recover instead of leaving this track stuck.
        if (fetchInProgress && System.nanoTime() - fetchStartedNanos > 30_000_000_000L)
            fetchInProgress = false;
        // A plain fallback is NOT a final result: continue searching for accurate timed lines.
        // Previously cached/plain lyrics short-circuited LRCLIB forever for this track.
        if (fetchInProgress || !lines.isEmpty()) return;
        long now=System.currentTimeMillis();
        if(now<providerRetryAt){
            status=providerStatus;
            // Do not block *all* songs just because LRCLIB is down.
            // Use a separate provider once, showing plainly unsynchronised lyrics.
            if (!fallbackAttempted) {
                fallbackAttempted = true;
                fetchInProgress = true;
                fetchStartedNanos = System.nanoTime();
                String title=m.title, artist=m.artist;
                long generation=lyricGeneration;
                try {
                    LYRICS_WORKER.execute(() -> {
                        try { loadPlainFallback(key,title,artist,generation); }
                        finally { if (isCurrent(key,generation)) fetchInProgress=false; }
                    });
                } catch (java.util.concurrent.RejectedExecutionException busy) {
                    fetchInProgress=false;
                    fallbackAttempted=false;
                    nextRetryAt=System.currentTimeMillis()+1500;
                }
            }
            return;
        }
        if(now<nextRetryAt)return;
        fetchInProgress = true;
        fetchStartedNanos = System.nanoTime();
        fetchAttempts++;
        nextRetryAt = System.currentTimeMillis() + retryDelay(fetchAttempts, 0L);
        String title=m.title,artist=m.artist;
        long duration=m.durationMs;
        long generation = lyricGeneration;
        try {
            LYRICS_WORKER.execute(() -> {
                try {
                    if (isCurrent(key, generation)) fetch(key,title,artist,duration,generation);
                } finally { if (isCurrent(key,generation)) fetchInProgress=false; }
            });
        } catch (java.util.concurrent.RejectedExecutionException busy) {
            // Rapid track skipping can saturate the worker. Retry promptly instead
            // of leaving the lyrics window blank for the old 30-second watchdog.
            fetchInProgress=false;
            nextRetryAt=System.currentTimeMillis()+1500;
        }
    }
    /** Let SpotifyHud drive lyric updates when Lyrics Only is not enabled. */
    public void refreshForEmbedded() {
        if (SpotifyHud.isIntegratedLyricsActive() && !enabled) onTick();
    }
    private static Path plainCachePath(String key) throws Exception {
        byte[] hash=MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
        StringBuilder name=new StringBuilder();
        for(byte b:hash) name.append(String.format(Locale.ROOT,"%02x",b&255));
        return FabricLoader.getInstance().getConfigDir().resolve("drainer-lyrics-cache")
            .resolve("plain-"+name+".txt");
    }
    private static List<String> loadPlainCached(String key){
        try {
            Path path=plainCachePath(key);
            if(!Files.isRegularFile(path)||Files.size(path)>70_000L)return List.of();
            return List.copyOf(Files.readAllLines(path,StandardCharsets.UTF_8));
        } catch(Exception ignored){return List.of();}
    }
    private static void savePlainCached(String key,List<String> data){
        try {
            Path path=plainCachePath(key);Files.createDirectories(path.getParent());
            Files.write(path,data,StandardCharsets.UTF_8);
        } catch(Exception ignored){}
    }

    /** Another public provider may have plain text even when LRCLIB is busy.
     *  Never pretend these lines have accurate timing, and never retry in a loop.
     */
    private void loadPlainFallback(String key, String title, String artist, long generation) {
        if (!isCurrent(key,generation) || title.isBlank() || artist.isBlank()) return;
        try {
            String url = FALLBACK_URI + URLEncoder.encode(artist,StandardCharsets.UTF_8).replace("+","%20")
                + "/" + URLEncoder.encode(title,StandardCharsets.UTF_8).replace("+","%20");
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "Drainer-Lyrics/7 (Minecraft HUD)")
                .timeout(Duration.ofSeconds(7)).GET().build();
            HttpResponse<String> res = LYRICS_HTTP.send(req,HttpResponse.BodyHandlers.ofString());
            if(res.statusCode()!=200 || res.body().length()>120_000) return;
            JsonObject json=JsonParser.parseString(res.body()).getAsJsonObject();
            if(!json.has("lyrics") || json.get("lyrics").isJsonNull()) return;
            String raw=json.get("lyrics").getAsString();
            java.util.List<String> plain = new java.util.ArrayList<>();
            for(String line:raw.split("\\R")) {
                String trimmed=line.trim();
                if(!trimmed.isEmpty() && trimmed.length()<300 && !trimmed.startsWith("Paroles de ")) plain.add(trimmed);
                if(plain.size()>=160) break;
            }
            if(!plain.isEmpty() && isCurrent(key,generation) && lines.isEmpty()) {
                plainLines=List.copyOf(plain);
                status="Plain lyrics (not synced)";
                savePlainCached(key,plain);
            }
        } catch (Exception ignored) { /* Secondary provider is best-effort. */ }
    }

    private static String norm(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }
    /** LRCLIB plainLyrics has no timestamps; keep real line order without faking sync. */
    private static List<String> parsePlain(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        List<String> output = new ArrayList<>();
        for (String row : raw.split("\\R")) {
            String clean = row.strip();
            if (!clean.isEmpty() && clean.length() <= 300) output.add(clean);
            if (output.size() >= 160) break;
        }
        return output;
    }
    private static List<Line> parse(String raw) {
        List<Line> out = new ArrayList<>();
        for (String row : raw.split("\\R")) {
            // Some LRC files repeat timestamps before a single lyric line.
            int offset=0;
            java.util.List<Long> stamps=new ArrayList<>();
            while (offset<row.length() && row.charAt(offset)=='[') {
                int close=row.indexOf(']',offset);
                if (close<0)break;
                String stamp=row.substring(offset+1,close);
                String[] parts=stamp.split(":");
                if (parts.length!=2)break;
                try {
                    long millis=Math.round((Integer.parseInt(parts[0])*60+Double.parseDouble(parts[1]))*1000);
                    if(millis>=0)stamps.add(millis);
                } catch (NumberFormatException ignored) {break;}
                offset=close+1;
            }
            String words=row.substring(offset).trim();
            if (!words.isEmpty()) for(long ms:stamps) out.add(new Line(ms,words));
        }
        out.sort(Comparator.comparingLong(Line::ms));
        return out;
    }
    private static long retryDelay(int attempt, long serverDelayMs) {
        // 10, 20, 40, 80, 160, 180 seconds; Retry-After is respected up to 5 min.
        long delay = Math.min(MAX_RETRY_MS, 10_000L << Math.min(5, Math.max(0, attempt - 1)));
        return Math.min(300_000L, Math.max(delay, serverDelayMs));
    }
    private static long retryAfterMs(HttpResponse<?> response) {
        String header = response.headers().firstValue("Retry-After").orElse("").trim();
        if (header.isEmpty()) return 0;
        try { return Math.min(300_000L, Math.max(0, Long.parseLong(header)) * 1000); }
        catch (NumberFormatException ignored) {
            try { return Math.min(300_000L, Math.max(0,
                ZonedDateTime.parse(header, DateTimeFormatter.RFC_1123_DATE_TIME)
                    .toInstant().toEpochMilli() - System.currentTimeMillis())); }
            catch (Exception ignoredDate) { return 0; }
        }
    }
    private static Path cacheFile(String key, long duration) throws Exception {
        // Rounding prevents a minor bridge duration change from creating duplicate cache keys.
        long seconds = Math.max(0L, Math.round(duration / 5000.0) * 5);
        byte[] sha = MessageDigest.getInstance("SHA-256").digest(
            (key + "|" + seconds).getBytes(StandardCharsets.UTF_8));
        StringBuilder name = new StringBuilder();
        for (byte b : sha) name.append(String.format(Locale.ROOT, "%02x", b & 0xff));
        return FabricLoader.getInstance().getConfigDir().resolve("drainer-lyrics-cache")
            .resolve(name + ".lrc");
    }
    /** Optional offline timed lyric files (Artist - Title.lrc) in config/drainer-lyrics-manual. */
    private static List<Line> loadManual(String title,String artist) {
        try {
            String file=(artist+" - "+title).replaceAll("[\\/:*?\"<>|\\p{Cntrl}]", "_").trim();
            Path dir=FabricLoader.getInstance().getConfigDir().resolve("drainer-lyrics-manual");
            Path path=dir.resolve(file+".lrc");
            if(!Files.isRegularFile(path)||Files.size(path)>200_000L)return List.of();
            return List.copyOf(parse(Files.readString(path,StandardCharsets.UTF_8)));
        } catch(Exception ignored){return List.of();}
    }
    private static List<Line> loadCached(String key, long duration) {
        try {
            Path path = cacheFile(key, duration);
            if (!Files.isRegularFile(path) || Files.size(path) > 200_000L) return List.of();
            return List.copyOf(parse(Files.readString(path, StandardCharsets.UTF_8)));
        } catch (Exception ignored) { return List.of(); }
    }
    private static void saveCached(String key, long duration, String lrc) {
        if (lrc == null || lrc.length() > 160_000) return;
        try {
            Path path = cacheFile(key, duration);
            Files.createDirectories(path.getParent());
            Files.writeString(path, lrc, StandardCharsets.UTF_8);
        } catch (Exception ignored) { /* cache is optional on locked/read-only folders */ }
    }
    private void providerBackoff(String key,long generation,int http,HttpResponse<?> response){
        long delay=retryDelay(fetchAttempts,retryAfterMs(response));
        providerRetryAt=Math.max(providerRetryAt,System.currentTimeMillis()+delay);
        providerStatus=http==429?"Lyrics rate limited — retrying later"
            :"Lyrics provider busy ("+http+") — retrying later";
        if(isCurrent(key,generation)){
            status=providerStatus;
            nextRetryAt=Math.max(nextRetryAt,providerRetryAt);
        }
    }
    /** Try the exact recording; a 503/429 must NOT trigger a second search request. */
    private ExactResult tryExact(String key, String title, String artist, long duration, long generation) {
        try {
            String url = "https://lrclib.net/api/get?track_name="
                + URLEncoder.encode(title, StandardCharsets.UTF_8)
                + "&artist_name=" + URLEncoder.encode(artist, StandardCharsets.UTF_8);
            if(duration>0)url += "&duration=" + Math.round(duration/1000.0);
            HttpRequest req=HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "Drainer-Lyrics/6.1 (Minecraft HUD)")
                .timeout(Duration.ofSeconds(9)).GET().build();
            HttpResponse<String> response=LYRICS_HTTP.send(req,HttpResponse.BodyHandlers.ofString());
            int http=response.statusCode();
            if(http==429 || http>=500){providerBackoff(key,generation,http,response);return ExactResult.RETRY_LATER;}
            if(http!=200)return ExactResult.NOT_FOUND;
            JsonObject o=JsonParser.parseString(response.body()).getAsJsonObject();
            if(!o.has("syncedLyrics") || o.get("syncedLyrics").isJsonNull()) {
                if (isCurrent(key,generation) && o.has("plainLyrics")
                    && !o.get("plainLyrics").isJsonNull()) {
                    List<String> plain = parsePlain(o.get("plainLyrics").getAsString());
                    if (!plain.isEmpty()) {
                        plainLines=List.copyOf(plain);
                        savePlainCached(key,plain);
                        status="Plain lyrics (no timestamps)";
                    }
                }
                return ExactResult.NOT_FOUND;
            }
            String raw=o.get("syncedLyrics").getAsString();
            List<Line> result=parse(raw);
            if(result.isEmpty()||!isCurrent(key,generation))return ExactResult.NOT_FOUND;
            if(duration>0 && o.has("duration") && !o.get("duration").isJsonNull()
                && Math.abs(duration/1000.0-o.get("duration").getAsDouble())>18)return ExactResult.NOT_FOUND;
            lines=List.copyOf(result);status="";
            saveCached(key,duration,raw);
            return ExactResult.FOUND;
        } catch(Exception ignored){return ExactResult.NOT_FOUND;}
    }
    private void fetch(String key, String title, String artist, long duration, long generation) {
        try {
            // Look up the exact recording before broad search: fewer mismatches and
            // fewer 503 errors when only the search endpoint is overloaded.
            if (!isCurrent(key,generation)) return;
            ExactResult exact=tryExact(key,title,artist,duration,generation);
            if(exact!=ExactResult.NOT_FOUND || !isCurrent(key,generation))return;
            String url = "https://lrclib.net/api/search?track_name=" + URLEncoder.encode(title, StandardCharsets.UTF_8)
                + "&artist_name=" + URLEncoder.encode(artist, StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "Drainer-Lyrics/6.0 (Minecraft HUD)")
                .timeout(Duration.ofSeconds(9)).GET().build();
            HttpResponse<String> response = LYRICS_HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            int http = response.statusCode();
            // Search can fail while the official exact-track endpoint still works.
            if (http == 429 || http>=500) {
                providerBackoff(key,generation,http,response);
                return;
            }
            if (http != 200) {
                if (isCurrent(key,generation)) {
                    status = "Lyrics request returned HTTP " + http;
                    // Unknown 4xx requests are usually not transient; avoid flooding the API.
                    nextRetryAt = System.currentTimeMillis() + 300_000L;
                }
                if(!fallbackAttempted && isCurrent(key,generation)) {
                    fallbackAttempted=true;loadPlainFallback(key,title,artist,generation);
                }
                return;
            }
            var arr = JsonParser.parseString(response.body()).getAsJsonArray();
            List<Line> best = List.of(); String bestRaw = "";
            List<String> bestPlain = List.of(); double bestPlainScore = Double.MAX_VALUE;
            double bestScore = Double.MAX_VALUE;
            for (var element : arr) {
                JsonObject o = element.getAsJsonObject();
                String candidateRaw = o.has("syncedLyrics") && !o.get("syncedLyrics").isJsonNull()
                    ? o.get("syncedLyrics").getAsString() : "";
                List<Line> parsed = parse(candidateRaw);
                List<String> plainCandidate = o.has("plainLyrics") && !o.get("plainLyrics").isJsonNull()
                    ? parsePlain(o.get("plainLyrics").getAsString()) : List.of();
                if (parsed.isEmpty() && plainCandidate.isEmpty()) continue;
                String candidateTitle = o.has("trackName") ? o.get("trackName").getAsString() : "";
                String candidateArtist = o.has("artistName") ? o.get("artistName").getAsString() : "";
                double score = 0;
                if (!norm(title).equals(norm(candidateTitle))) score += 50;
                if (!norm(artist).equals(norm(candidateArtist))) score += 25;
                if (duration > 0 && o.has("duration") && !o.get("duration").isJsonNull()) {
                    double diff = Math.abs(duration / 1000.0 - o.get("duration").getAsDouble());
                    score += diff > 8 ? 100 + diff : diff;
                }
                if (duration > 0 && o.has("duration") && !o.get("duration").isJsonNull()
                    && Math.abs(duration / 1000.0 - o.get("duration").getAsDouble()) > 18) continue;
                if (!parsed.isEmpty() && score < bestScore) { bestScore = score; best = parsed; bestRaw = candidateRaw; }
                if (!plainCandidate.isEmpty() && score < bestPlainScore) {
                    bestPlainScore = score; bestPlain = plainCandidate;
                }
            }
            if (isCurrent(key,generation)) {
                lines = List.copyOf(best);
                if (best.isEmpty() && !bestPlain.isEmpty()) {
                    plainLines = List.copyOf(bestPlain);
                    savePlainCached(key,bestPlain);
                }
                status = best.isEmpty() ? (plainLines.isEmpty()
                    ? "No lyrics found for this recording" : "Plain lyrics (no timestamps)") : "";
                if (best.isEmpty()) nextRetryAt = System.currentTimeMillis() + 90_000L;
                else saveCached(key, duration, bestRaw);
            }
            if(best.isEmpty() && !fallbackAttempted && isCurrent(key,generation)) {
                fallbackAttempted=true;
                if (plainLines.isEmpty()) loadPlainFallback(key,title,artist,generation);
            }
        } catch (Exception ex) {
            if (isCurrent(key,generation)) {
                String reason = ex.getMessage();
                if (reason == null || reason.isBlank()) reason = ex.getClass().getSimpleName();
                if (reason.length() > 32) reason = reason.substring(0, 32);
                status = "Lyrics connection error — retrying...";
                nextRetryAt = System.currentTimeMillis() + retryDelay(fetchAttempts, 0);
            }
            if(!fallbackAttempted && isCurrent(key,generation)) {
                fallbackAttempted=true;
                loadPlainFallback(key,title,artist,generation);
            }
        }
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
    private void drawWords(GuiGraphicsExtractor g, Line line, long start, long end, long pos, int textX, int textY, int limit) {
        String s = fit(line.text, limit);
        if (s.isEmpty()) return;
        if (!karaoke.get() || end <= start) { Ui.text(g, s, textX, textY, HudGlass.accent()); return; }
        double fraction=Math.max(0,Math.min(1,(double)
            (pos-start)/(end-start)));
        // Estimated character progression from line timestamps, not true per-word timing.
        // Two draw calls prevent expensive one-character-at-a-time rendering.
        int split=(int)Math.round(s.length()*fraction);
        int cursor=textX;
        if(split>0) {
            String lit=s.substring(0,split);
            Ui.text(g,lit,cursor,textY,HudGlass.accent());
            cursor+=Ui.textW(lit);
        }
        if(split<s.length()) Ui.text(g,s.substring(split),cursor,textY,HudGlass.muted());
    }
    /**
     * Draw the currently cached lyric lines inside SpotifyHud's pose.
     * No second track title, no network work in rendering, and no invented timings.
     */
    public void renderEmbedded(GuiGraphicsExtractor g, int x, int y, int width, int height) {
        SpotifyManager m = SpotifyManager.I;
        Ui.text(g,"LYRICS",x+2,y,HudGlass.accent());
        boolean matching = m.hasTrack && track.equals(m.title + "|" + m.artist)
            && trackDurationBucket == durationBucket(m.durationMs);
        if (!m.hasTrack) {
            Ui.text(g,"Play music to view lyrics",x+2,y+42,HudGlass.muted());
            return;
        }
        boolean ended = !m.playing && m.durationMs > 0 && m.progress() >= m.durationMs-180;
        if (ended) {
            Ui.text(g,"Playback finished",x+2,y+42,HudGlass.muted());
            return;
        }
        List<Line> timed = matching ? lines : List.of();
        List<String> plain = matching ? plainLines : List.of();
        int maxWidth = Math.max(30,width-6);
        if (!timed.isEmpty()) {
            long position = Math.max(0,m.progress()+timingOffset.getI());
            int current = -1;
            for (int i=0;i<timed.size();i++) {
                if (timed.get(i).ms<=position) current=i;
                else break;
            }
            if (current<0) {
                Ui.text(g,"Waiting for first lyric...",x+2,y+42,HudGlass.muted());
                return;
            }
            long start = timed.get(current).ms;
            long next = current+1 < timed.size() ? timed.get(current+1).ms
                : Math.max(start+3000,m.durationMs);
            long holdEnd = start+maxHold.getI();
            if (current>0) Ui.text(g,fit(timed.get(current-1).text,maxWidth),x+2,y+24,HudGlass.muted());
            if (position>holdEnd) Ui.text(g,"♫",x+2,y+44,HudGlass.accent());
            else drawWords(g,timed.get(current),start,Math.min(next,holdEnd),position,
                x+2,y+44,maxWidth);
            if (current+1<timed.size()) Ui.text(g,fit(timed.get(current+1).text,maxWidth),x+2,y+64,HudGlass.muted());
        } else if (!plain.isEmpty()) {
            Ui.text(g,"PLAIN · NOT SYNCED",x+width-Ui.textW("PLAIN · NOT SYNCED")-4,
                y,HudGlass.muted());
            // Untimed lyrics are a text preview, not a fabricated karaoke timeline.
            int idx = 0;
            if (idx>0) Ui.text(g,fit(plain.get(idx-1),maxWidth),x+2,y+24,HudGlass.muted());
            Ui.text(g,fit(plain.get(idx),maxWidth),x+2,y+44,HudGlass.text());
            if(idx+1<plain.size()) Ui.text(g,fit(plain.get(idx+1),maxWidth),x+2,y+64,HudGlass.muted());
        } else {
            Ui.text(g,fit(matching ? status : "Finding synced lyrics...",maxWidth),
                x+2,y+42,HudGlass.muted());
        }
    }

    @Override public void onHud(GuiGraphicsExtractor g, float delta) {
        if (SpotifyHud.isIntegratedLyricsActive()) return; // Prevent duplicate floating music panels.
        SpotifyManager m = SpotifyManager.I;
        // Media sessions commonly retain track metadata after the player has
        // stopped at the end. Do not leave the final lyric onscreen indefinitely.
        boolean finished = m.hasTrack && !m.playing && m.durationMs > 0
            && m.progress() >= m.durationMs - 180;
        boolean editing = net.minecraft.client.Minecraft.getInstance().gui.screen() instanceof
            meteordevelopment.meteorclient.gui.screens.LovenoteModulesScreen screen && screen.isDrainerHudEditor();
        boolean idle = m.idleFor(idleDelay.getI() * 1000L);
        if ((!m.hasTrack || finished || idle) && hideIdle.get() && !editing) return;
        double sc = Math.max(.25, Math.min(1.6, scale.get()));
        boolean single = oneLine.get();
        int bw = single ? 306 : 345, bh = single ? 77 : 130;
        w=(int)Math.round(bw*sc); h=(int)Math.round(bh*sc);
        px=Math.max(0,Math.min((int)(x.get()*Gfx.guiW()),Math.max(0,Gfx.guiW()-w)));
        py=Math.max(0,Math.min((int)(y.get()*Gfx.guiH()),Math.max(0,Gfx.guiH()-h)));
        var pose = g.pose();
        pose.pushMatrix(); pose.translate(px,py); pose.scale((float)sc,(float)sc);
        HudGlass.card(g,0,0,bw,bh,215);
        // Live track metadata and musical icon; never use fake timed lines.
        HudGlass.tile(g,12,11,29,29,false);
        Ui.text(g,"♫",21,20,HudGlass.accent());
        Ui.text(g,"SYNCED LYRICS",51,9,HudGlass.accent());
        Ui.text(g,HudGlass.fit(m.hasTrack ? m.title : "Waiting for music",bw-68),
                51,22,HudGlass.text());
        Ui.text(g,HudGlass.fit(m.hasTrack ? m.artist : "Media bridge",bw-68),
                51,36,HudGlass.muted());
        g.fill(13,52,bw-13,53,0x267C83B6);
        // A track switch invalidates lyrics even between ticks.
        boolean matching = m.hasTrack && track.equals(m.title + "|" + m.artist)
            && trackDurationBucket == durationBucket(m.durationMs);
        List<Line> ls = matching ? lines : List.of();
        List<String> pl = matching ? plainLines : List.of();
        int activeY=single?60:88;
        int limit=bw-27;
        if(ls.isEmpty() && !pl.isEmpty()) {
            // Never fake timing for plain lyrics; leave manual/timed providers in charge.
            int idx = 0;
            if(!single && idx>0)
                Ui.text(g,fit(pl.get(idx-1),limit),14,65,HudGlass.muted());
            Ui.text(g,fit(pl.get(idx),limit),14,activeY,HudGlass.text());
            if(!single && idx+1<pl.size())
                Ui.text(g,fit(pl.get(idx+1),limit),14,108,HudGlass.muted());
            // Fallback lyric provider has no timings; don't imply karaoke sync.
            Ui.text(g,"PLAIN",bw-47,8,HudGlass.muted());
        } else if(ls.isEmpty()) {
            Ui.text(g,fit(!m.hasTrack ? "Play a track to preview lyrics"
                : !matching ? "Finding synced lyrics..." : status,limit),14,activeY,HudGlass.muted());
        } else {
            long pos=Math.max(0,m.progress()+timingOffset.getI());
            int idx=-1;
            for(int i=0;i<ls.size();i++) if(ls.get(i).ms<=pos)idx=i;else break;
            if(idx<0) {
                Ui.text(g,"Waiting for first lyric...",14,activeY,HudGlass.muted());
            } else {
                long start=ls.get(idx).ms;
                long holdEnd=start+maxHold.getI();
                long next=idx+1<ls.size()?ls.get(idx+1).ms:Math.max(start+3000,m.durationMs);
                long end=Math.min(next,holdEnd);
                if(!single && idx>0)
                    Ui.text(g,fit(ls.get(idx-1).text,limit),14,65,HudGlass.muted());
                if(pos>holdEnd) Ui.text(g,"♫",14,activeY,HudGlass.accent());
                else drawWords(g,ls.get(idx),start,end,pos,14,activeY,limit);
                if(!single && idx+1<ls.size())
                    Ui.text(g,fit(ls.get(idx+1).text,limit),14,108,HudGlass.muted());
            }
        }
        pose.popMatrix();
    }
}
