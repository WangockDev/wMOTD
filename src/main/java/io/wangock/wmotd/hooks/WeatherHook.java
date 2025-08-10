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

public class WeatherHook {
    private final Main plugin;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private volatile boolean night;
    private volatile int weatherCode;
    private volatile Instant lastUpdate = Instant.EPOCH;

    private int intervalSeconds;
    private double latitude;
    private double longitude;
    private boolean enabled;

    public WeatherHook(Main plugin) {
        this.plugin = plugin;
        reloadFromConfig();
    }

    public void start() {
        if (!enabled) return;
        long ticks = Math.max(20L, intervalSeconds * 20L);
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::refresh, 1L, ticks);
    }

    public void stop() {
        // No persistent async tasks stored; Paper will cancel on disable
    }

    public void reloadFromConfig() {
        var cfg = plugin.getConfigManager().getConfig();
        this.enabled = cfg.getBoolean("weather.enabled", false);
        this.intervalSeconds = cfg.getInt("weather.update-interval-seconds", 600);
        this.latitude = cfg.getDouble("weather.latitude", 55.7558);
        this.longitude = cfg.getDouble("weather.longitude", 37.6173);
    }

    public boolean isNight() {
        return night;
    }

    private void refresh() {
        if (!enabled) return;
        try {
            // Using Open-Meteo: day/night and weather code
            String url = "https://api.open-meteo.com/v1/forecast?latitude=" + latitude + "&longitude=" + longitude + "&current=is_day,weather_code";
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonObject obj = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonObject current = obj.getAsJsonObject("current");
                if (current != null && current.has("is_day")) {
                    int isDay = current.get("is_day").getAsInt();
                    this.night = isDay == 0;
                }
                if (current != null && current.has("weather_code")) {
                    this.weatherCode = current.get("weather_code").getAsInt();
                }
                this.lastUpdate = Instant.now();
            }
        } catch (Exception ignored) { }
    }
    
    public String getWeatherCategory() {
        int code = this.weatherCode;
        if (code == 0) return "clear";
        if (code >= 1 && code <= 3) return "cloudy";
        if (code == 45 || code == 48) return "cloudy";
        if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) return "rain";
        if ((code >= 71 && code <= 77) || (code == 85 || code == 86)) return "snow";
        if (code >= 95 && code <= 99) return "thunder";
        return night ? "night" : "clear";
    }
}


