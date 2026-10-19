package com.lovenote.spotify;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/** Windows media-session bridge for Drainer. No Spotify API key required. */
public final class SpotifyManager {
    public static final SpotifyManager I = new SpotifyManager();
    public volatile String title = "", artist = "", album = "", status = "Starting bridge";
    public volatile boolean connected, hasTrack, playing;
    public volatile long progressMs, durationMs, progressStamp;
    private volatile long progressNanos;
    private volatile String clockTrack = "";
    // The bridge may report the same (or older) position for several polls. The
    // HUD clock must not be re-anchored backwards for each of those samples.
    private long lastRawProgress = -1L;
    private int rewindSamples;
    private long rewindAnchor;
    private volatile long requestedSeek = -1L;
    private volatile long requestedSeekUntil;

    public volatile Identifier artTexture;
    private volatile String lastCover = "";
    private volatile boolean started;
    private SpotifyManager() {}

    private Path helper() {
        Path cfg = FabricLoader.getInstance().getConfigDir().toAbsolutePath().normalize();
        Path[] paths = {cfg.resolve("lovenote-media-bridge.exe"),
                cfg.resolve("drainer-media-bridge.exe"),
                Path.of(System.getProperty("user.dir", "."), "config", "lovenote-media-bridge.exe")};
        for (Path p : paths) if (Files.isRegularFile(p)) return p;
        return paths[0];
    }

    public synchronized void start() {
        if (started) return;
        started = true;
        Thread t = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try { poll(); Thread.sleep(850); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                catch (Exception e) { clearPlayback("Bridge error: " + e.getMessage());
                    try { Thread.sleep(2500); } catch (InterruptedException x) { Thread.currentThread().interrupt(); } }
            }
        }, "Drainer-Spotify-Poll");
        t.setDaemon(true);
        t.start();
    }

    private JsonObject execute(String... args) throws Exception {
        Path exe = helper();
        if (!Files.isRegularFile(exe)) throw new IllegalStateException("EXE missing: " + exe);
        String[] command = new String[args.length + 1];
        command[0] = exe.toString();
        System.arraycopy(args, 0, command, 1, args.length);
        Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
        String line;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            line = r.readLine();
        }
        if (!p.waitFor(8, TimeUnit.SECONDS)) { p.destroyForcibly(); throw new IllegalStateException("Helper timeout"); }
        if (line == null || !line.startsWith("{")) throw new IllegalStateException("Invalid helper response");
        JsonObject result = JsonParser.parseString(line).getAsJsonObject();
        if (result.has("error")) throw new IllegalStateException(result.get("error").getAsString());
        return result;
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }
    private synchronized void clearPlayback(String why) {
        connected = false;
        hasTrack = false;
        playing = false;
        title = ""; artist = ""; album = "";
        clockTrack = "";
        progressMs = 0; durationMs = 0;
        progressNanos = System.nanoTime();
        progressStamp = System.currentTimeMillis();
        lastRawProgress = -1;
        rewindSamples = 0;
        requestedSeek = -1;
        lastCover = "";
        artTexture = null;
        status = why;
    }

    private void poll() throws Exception {
        JsonObject o = execute();
        if (!o.has("found") || !o.get("found").getAsBoolean()) {
            clearPlayback("Open Spotify and play a track");
            connected = true;
            return;
        }
        String freshTitle = str(o,"title"), freshArtist = str(o,"artist");
        boolean freshPlaying = o.has("playing") && o.get("playing").getAsBoolean();
        long reported = Math.max(0, o.has("progressMs") ? o.get("progressMs").getAsLong() : 0);
        long freshDuration = Math.max(0, o.has("durationMs") ? o.get("durationMs").getAsLong() : 0);
        String identity = freshTitle + "|" + freshArtist + "|" + freshDuration;
        long now = System.nanoTime();
        synchronized (this) {
            boolean changed = !identity.equals(clockTrack) || !hasTrack;
            boolean previousPlaying = playing;
            long predicted = progress();
            long anchor = reported;
            // UI-initiated seeks receive a brief acknowledgement window. An old
            // bridge sample must never snap the playhead back immediately.
            if (!changed && requestedSeek >= 0 && now < requestedSeekUntil) {
                if (Math.abs(reported - requestedSeek) <= 850) {
                    requestedSeek = -1;
                } else {
                    anchor = predicted;
                }
            } else if (!changed && freshPlaying && previousPlaying) {
                long rawDrop = lastRawProgress < 0 ? 0 : lastRawProgress - reported;
                if (rawDrop > 1500 && predicted - reported > 1200) {
                    // A genuine backwards seek shows a drop in raw player time.
                    rewindSamples = 1;
                    rewindAnchor = reported;
                    anchor = predicted;
                } else if (rewindSamples > 0 && reported <= rewindAnchor + 2500
                        && predicted - reported > 1200) {
                    // Confirm a seek on the following poll, not a single stale read.
                    anchor = reported;
                    rewindSamples = 0;
                } else {
                    rewindSamples = 0;
                    // The expected clock is authoritative through small backwards
                    // and repeated media-session samples; accept obvious forwards seeks.
                    anchor = reported > predicted + 850 ? reported : predicted;
                }
            } else if (!changed && !freshPlaying && !previousPlaying) {
                // Paused sessions should remain frozen, even if bridge samples drift.
                anchor = Math.abs(reported - predicted) > 1500 ? reported : predicted;
            } else if (!changed && !freshPlaying) {
                anchor = Math.max(predicted, reported);
            }
            if (freshDuration > 0) anchor = Math.min(anchor, freshDuration);
            clockTrack = identity;
            connected = true;
            title = freshTitle; artist = freshArtist; album = str(o,"album");
            playing = freshPlaying;
            progressMs = Math.max(0, anchor);
            durationMs = freshDuration;
            progressStamp = System.currentTimeMillis();
            progressNanos = now;
            lastRawProgress = reported;
            hasTrack = !freshTitle.isBlank();
            status = hasTrack ? "Connected" : "No track available";
            if (changed) { rewindSamples = 0; requestedSeek = -1; }
            if (requestedSeek >= 0 && now >= requestedSeekUntil) requestedSeek = -1;
        }
        String cover = str(o, "cover");
        if (!cover.equals(lastCover)) {
            lastCover = cover;
            if (cover.isEmpty()) Minecraft.getInstance().execute(() -> artTexture = null);
            else {
                try {
                    byte[] bytes = Base64.getDecoder().decode(cover);
                    if (bytes.length <= 2_000_000) {
                        NativeImage img = NativeImage.read(new ByteArrayInputStream(bytes));
                        Minecraft.getInstance().execute(() -> {
                            Identifier id = Identifier.fromNamespaceAndPath("lovenote", "gen/spotify_art");
                            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "drainer_album", img));
                            artTexture = id;
                        });
                    }
                } catch (Exception ignored) { artTexture = null; }
            }
        }
    }

    /** Monotonic estimate between bridge samples; pauses do not advance lyrics. */
    public synchronized long progress() {
        if (!hasTrack) return 0;
        long sinceMs = Math.max(0L, (System.nanoTime() - progressNanos) / 1_000_000L);
        long value = progressMs + (playing ? sinceMs : 0);
        return Math.max(0, durationMs > 0 ? Math.min(durationMs, value) : value);
    }
    private void command(String... args) {
        Thread t = new Thread(() -> { try { execute(args); poll(); }
            catch (Exception e) { status = "Control error: " + e.getMessage(); } }, "Drainer-Media-Control");
        t.setDaemon(true); t.start();
    }
    public void togglePlay() { command("toggle"); }
    public void next() { command("next"); }
    public void previous() { command("previous"); }
    public synchronized void seek(long ms) {
        long target = Math.max(0, durationMs > 0 ? Math.min(durationMs, ms) : ms);
        requestedSeek = target;
        requestedSeekUntil = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        progressMs = target;
        progressNanos = System.nanoTime();
        rewindSamples = 0;
        command("seek", Long.toString(target));
    }
}
