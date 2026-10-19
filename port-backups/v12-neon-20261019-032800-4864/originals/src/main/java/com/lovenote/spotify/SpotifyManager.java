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
                catch (Exception e) { connected = false; hasTrack = false; playing = false; status = "Bridge error: " + e.getMessage();
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
    private void poll() throws Exception {
        JsonObject o = execute();
        connected = true;
        if (!o.has("found") || !o.get("found").getAsBoolean()) {
            hasTrack = false; status = "Open Spotify and play a track"; return;
        }
        title = str(o, "title"); artist = str(o, "artist"); album = str(o, "album");
        playing = o.has("playing") && o.get("playing").getAsBoolean();
        progressMs = o.has("progressMs") ? o.get("progressMs").getAsLong() : 0;
        durationMs = o.has("durationMs") ? o.get("durationMs").getAsLong() : 0;
        progressStamp = System.currentTimeMillis();
        progressNanos = System.nanoTime();
        hasTrack = !title.isBlank();
        status = hasTrack ? "Connected" : "No track available";
        String cover = str(o, "cover");
        if (!cover.equals(lastCover)) {
            lastCover = cover;
            if (cover.isEmpty()) Minecraft.getInstance().execute(() -> { artTexture = null; });
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
    public long progress() {
        long sinceMs = Math.max(0L, (System.nanoTime() - progressNanos) / 1_000_000L);
        // Use the last bridge position as an anchor; never advance while paused.
        long p = progressMs + (playing ? Math.min(1500L, sinceMs) : 0);
        return Math.max(0, durationMs > 0 ? Math.min(durationMs, p) : p);
    }
    private void command(String... args) {
        Thread t = new Thread(() -> { try { execute(args); poll(); }
            catch (Exception e) { status = "Control error: " + e.getMessage(); } }, "Drainer-Media-Control");
        t.setDaemon(true); t.start();
    }
    public void togglePlay() { command("toggle"); }
    public void next() { command("next"); }
    public void previous() { command("previous"); }
    public void seek(long ms) { command("seek", Long.toString(ms)); }
}
