package io.wangock.wmotd.hooks;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.wangock.wmotd.Main;
import org.bukkit.Bukkit;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class GeoHook {
    private final Main plugin;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private boolean enabled;
    private long ttlSeconds;

    public GeoHook(Main plugin) {
        this.plugin = plugin;
        reloadFromConfig();
    }

    public void reloadFromConfig() {
        var cfg = plugin.getConfigManager().getConfig();
        this.enabled = cfg.getBoolean("geoip.enabled", false);
        this.ttlSeconds = cfg.getLong("geoip.ttl-seconds", 3600);
    }

    public String getCountryCached(String ip) {
        if (!enabled || ip == null || ip.isEmpty()) return null;
        CacheEntry entry = cache.get(ip);
        if (entry != null && Instant.now().isBefore(entry.expiresAt)) {
            return entry.country;
        }
        // schedule async fetch; return last known or null
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> fetch(ip));
        return entry != null ? entry.country : null;
    }

    private void fetch(String ip) {
        try {
            String url = "https://ipwho.is/" + ip;
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                JsonObject obj = JsonParser.parseString(resp.body()).getAsJsonObject();
                if (obj.has("success") && obj.get("success").getAsBoolean()) {
                    String code = obj.get("country_code").getAsString();
                    cache.put(ip, new CacheEntry(code, Instant.now().plusSeconds(ttlSeconds)));
                }
            }
        } catch (Exception ignored) {}
    }

    private static class CacheEntry {
        final String country;
        final Instant expiresAt;
        CacheEntry(String c, Instant t) { this.country = c; this.expiresAt = t; }
    }
}


