package com.lovenote.skyblock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.Scoreboard;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Read-only SkyBlock sidebar and optional, rate-limited public Hypixel API snapshots.
 * No key in chat, logs, player config JSON, or outgoing URLs. Never sends commands or inputs. */
public final class SkyBlockIntel {
    public record Snapshot(boolean hypixel, String area, String quest, int level,
                           String profile, String apiStatus, double bank, long updatedAt,
                           List<String> sidebar, String market) {}
    private static volatile Snapshot latest = new Snapshot(false, "Unknown", "Unavailable", -1,
        "Not linked", "API key not configured", -1, 0, List.of(), "Not checked");
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
    private static final AtomicBoolean busy = new AtomicBoolean(false);
    private static final AtomicBoolean marketBusy = new AtomicBoolean(false);
    private static volatile String apiProfile = "Not linked", apiStatus = "API key not configured";
    private static volatile double apiBank = -1;
    private static volatile long apiUpdated;
    private static volatile String market = "Not checked";
    private static long lastPoll, lastMarketPoll;
    private static UUID lastUUID;
    private static int ticks;
    private static final Pattern LEVEL = Pattern.compile("(?i)(?:skyblock\\s+level|sb\\s+level|level)\\s*:?\\s*(\\d{1,3})");
    // Hypixel has changed its sidebar location symbols more than once. Also handle
    // icon-prefixed names (e.g. the skull next to Spider's Den) without making up an area.
    private static final String[] AREAS = {
        "Spider's Den", "The Garden", "Garden", "Private Island", "Hub", "Village",
        "Dwarven Mines", "Crystal Hollows", "Deep Caverns", "Gold Mine", "Coal Mine",
        "The End", "Crimson Isle", "Dungeon Hub", "The Park", "Park",
        "Jerry's Workshop", "The Farming Islands", "Rift", "The Rift", "Barn",
        "Mushroom Desert", "Graveyard", "Wilderness", "High Level", "Forest",
        "Ruins", "Colosseum", "Mountain", "Bank", "Farm"
    };
    private static String findArea(List<String> rows) {
        for (String row: rows) {
            String clean=row.trim();
            // The canonical location marker works even for unknown future area names.
            int pos=clean.indexOf('⏣');
            if(pos>=0){
                String area=clean.substring(pos+1).trim();
                if(!area.isBlank())return area;
            }
            // Strip icon glyphs/punctuation from the start and match a whole known name.
            // Avoid matching casual quest prose mentioning an island name.
            String candidate=clean.replaceFirst("^[^\\p{L}\\p{N}]+", "").trim();
            for(String known:AREAS) if(candidate.equalsIgnoreCase(known)) return known;
        }
        return "Unknown";
    }
    private SkyBlockIntel() {}

    public static Snapshot get() { return latest; }

    public static void tick(Minecraft mc) {
        if (++ticks % 20 != 0) return;  // one sidebar read a second, never every render frame
        if (mc.player == null || mc.level == null) {
            latest = new Snapshot(false, "Unknown", "Unavailable", -1, "Not linked", "Disconnected", -1,0,List.of(),"Not checked");
            return;
        }
        String host = mc.getCurrentServer() == null ? "" : mc.getCurrentServer().ip.toLowerCase(Locale.ROOT);
        boolean hypixel = host.equals("hypixel.net") || host.endsWith(".hypixel.net") ||
            host.startsWith("hypixel.net:") || host.matches(".+\\.hypixel\\.net:\\d+");
        List<String> rows = readSidebar(mc);
        String area = findArea(rows), quest = "Not detected";
        int level = -1;
        for (String row : rows) {
            String clean = row.strip();
            if (clean.contains("⏣")) {
                int n = clean.indexOf('⏣');
                String found = clean.substring(n + 1).trim();
                if (!found.isBlank()) area = found;
            }
            Matcher m = LEVEL.matcher(clean);
            if (m.find()) try { level = Integer.parseInt(m.group(1)); } catch (NumberFormatException ignored) {}
        }
        for (int i=0; i<rows.size(); i++) {
            String row = rows.get(i).toLowerCase(Locale.ROOT);
            if (row.contains("objective:") || row.contains("quest:") || row.contains("commission:")) {
                String value = rows.get(i).replaceFirst("(?i)^.*?(objective|quest|commission)\\s*:\\s*", "").trim();
                if (value.isBlank() && i+1<rows.size()) value = rows.get(i+1);
                if (!value.isBlank()) { quest=value; break; }
            }
        }
        // Empty sidebar is not proof of the player's quests being complete.
        if (!hypixel) { area = "Not on Hypixel"; quest = "Join SkyBlock for live status"; }
        UUID playerUUID = mc.player.getUUID();
        if (!playerUUID.equals(lastUUID)) {
            lastUUID=playerUUID;apiProfile="Not linked";apiStatus="API key not configured";apiBank=-1;apiUpdated=0;lastPoll=0;
        }
        if (hypixel) { pollProfile(playerUUID, false); pollMarket(false); }
        latest = new Snapshot(hypixel, area, quest, level, apiProfile, apiStatus, apiBank,
            apiUpdated, rows, market);
    }

    public static void refresh() {
        if (lastUUID != null) { lastPoll=0; pollProfile(lastUUID,true); }
        lastMarketPoll=0;pollMarket(true);
    }

    private static List<String> readSidebar(Minecraft mc) {
        try {
            Scoreboard board = mc.level.getScoreboard();
            Objective objective = board.getDisplayObjective(DisplaySlot.SIDEBAR);
            if (objective == null) return List.of();
            List<String> output = new ArrayList<>();
            output.add(clean(objective.getDisplayName().getString()));
            // Both the display component and fallback score-holder are read-only.
            for (PlayerScoreEntry entry : board.listPlayerScores(objective)) {
                String raw = entry.display()!=null ? entry.display().getString() : entry.owner();
                String s=clean(raw);
                if (!s.isBlank() && !s.startsWith("#")) output.add(s);
            }
            return Collections.unmodifiableList(output);
        } catch (RuntimeException e) { return List.of(); }
    }
    private static String clean(String s) {
        if (s == null) return "";
        return s.replaceAll("(?i)§[0-9A-FK-ORX]", "").trim();
    }

    private static Path settingsFile() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("lovenote262/skyblock-api.properties");
    }
    private static String key() {
        try {
            Path path=settingsFile();if(!Files.isRegularFile(path))return "";
            Properties props=new Properties();
            try(var reader=Files.newBufferedReader(path)){props.load(reader);}
            String value=props.getProperty("api-key", "").trim();
            return value.matches("[A-Za-z0-9-]{16,128}") ? value : "";
        } catch (Exception ignored){ return ""; }
    }
    private static void pollProfile(UUID uuid,boolean force) {
        long now=System.currentTimeMillis();
        if (!force && now-lastPoll<300_000) return;
        if (!busy.compareAndSet(false,true)) return;
        lastPoll=now;
        String secret=key();
        if(secret.isEmpty()) { apiStatus="API key not configured";busy.set(false);return; }
        String playerID=uuid.toString().replace("-","");
        HttpRequest request=HttpRequest.newBuilder(URI.create("https://api.hypixel.net/v2/skyblock/profiles?uuid="+playerID))
            .timeout(Duration.ofSeconds(10)).header("API-Key",secret).header("User-Agent","DrainerSkyBlockCoach/16")
            .GET().build();
        // Network on an asynchronous HTTP worker, never the Minecraft tick/render thread.
        HTTP.sendAsync(request,HttpResponse.BodyHandlers.ofString()).whenComplete((response,error)->{
            try {
                if (error!=null){apiStatus="Network unavailable";return;}
                if(response.statusCode()==429){apiStatus="Rate limited; retry later";return;}
                if(response.statusCode()==403){apiStatus="Invalid or restricted API key";return;}
                if(response.statusCode()!=200){apiStatus="API returned HTTP "+response.statusCode();return;}
                JsonObject root=JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray profiles=root.has("profiles") && root.get("profiles").isJsonArray() ? root.getAsJsonArray("profiles") : null;
                if(profiles==null){apiStatus="No accessible profiles";return;}
                JsonObject selected=null;
                for(JsonElement el:profiles){
                    if(!el.isJsonObject())continue;
                    JsonObject p=el.getAsJsonObject();
                    if(selected==null || (p.has("selected") && p.get("selected").getAsBoolean()))selected=p;
                    if(p.has("selected")&&p.get("selected").getAsBoolean())break;
                }
                if(selected==null){apiStatus="No profiles returned";return;}
                apiProfile = selected.has("cute_name") ? selected.get("cute_name").getAsString() : "Unnamed";
                if(selected.has("banking")&&selected.get("banking").isJsonObject()){
                    JsonObject banking=selected.getAsJsonObject("banking");
                    apiBank=banking.has("balance")?banking.get("balance").getAsDouble():-1;
                } else apiBank=-1;
                apiUpdated=System.currentTimeMillis();apiStatus="Linked (read-only snapshot)";
            } catch (RuntimeException e){apiStatus="Profile format unavailable";}
            finally {busy.set(false);}
        });
    }
    private static void pollMarket(boolean force) {
        long now=System.currentTimeMillis();if(!force && now-lastMarketPoll<300_000)return;
        if(!marketBusy.compareAndSet(false,true))return;
        lastMarketPoll=now;
        HttpRequest request=HttpRequest.newBuilder(URI.create("https://api.hypixel.net/v2/skyblock/bazaar"))
            .timeout(Duration.ofSeconds(10)).header("User-Agent","DrainerSkyBlockCoach/16").GET().build();
        HTTP.sendAsync(request,HttpResponse.BodyHandlers.ofString()).whenComplete((response,error)->{
            try {
                if(error!=null || response.statusCode()!=200){market="Bazaar unavailable";return;}
                JsonObject products=JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonObject("products");
                if(products==null){market="Bazaar data missing";return;}
                String[] goods={"ENCHANTED_SUGAR_CANE","ENCHANTED_CARROT","ENCHANTED_POTATO","ENCHANTED_NETHER_STALK"};
                double highest=-1;String chosen="";
                for(String id:goods){
                    if(!products.has(id))continue;
                    JsonObject p=products.getAsJsonObject(id),quick=p.getAsJsonObject("quick_status");
                    if(quick==null || !quick.has("sellPrice"))continue;
                    double price=quick.get("sellPrice").getAsDouble();
                    if(price>highest){highest=price;chosen=id.replace("ENCHANTED_","").replace('_',' ');}
                }
                market=highest>=0?String.format(Locale.ROOT,"%s sellPrice %.1f/item",chosen,highest):"No tracked crops available";
            } catch(RuntimeException e){market="Bazaar format changed";}
            finally {marketBusy.set(false);}
        });
    }
}
