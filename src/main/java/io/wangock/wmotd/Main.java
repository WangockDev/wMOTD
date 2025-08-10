package io.wangock.wmotd;

import io.wangock.wmotd.commands.WMOTDCommand;
import io.wangock.wmotd.config.ConfigManager;
import io.wangock.wmotd.hooks.PlaceholderHook;
import io.wangock.wmotd.hooks.GeoHook;
import io.wangock.wmotd.hooks.WeatherHook;
import io.wangock.wmotd.listeners.JoinListener;
import io.wangock.wmotd.listeners.PingListener;
import io.wangock.wmotd.managers.IconManager;
import io.wangock.wmotd.managers.MOTDManager;
import io.wangock.wmotd.managers.MiniMessageFormatter;
import io.wangock.wmotd.managers.PlayerTracker;
import io.wangock.wmotd.managers.ScheduleManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;

public final class Main extends JavaPlugin {

    private static Main instance;

    private ConfigManager configManager;
    private MiniMessageFormatter miniMessageFormatter;
    private PlaceholderHook placeholderHook;
    private WeatherHook weatherHook;
    private GeoHook geoHook;
    private MOTDManager motdManager;
    private IconManager iconManager;
    private ScheduleManager scheduleManager;
    private PlayerTracker playerTracker;

    private Instant serverStartInstant;

    public static Main getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        this.serverStartInstant = Instant.now();

        // Ensure default files and data dirs
        saveDefaultConfig();
        saveResource("templates.yml", false);
        saveResource("messages.yml", false);
        getDataFolder().mkdirs();

        this.configManager = new ConfigManager(this);
        this.configManager.loadAll();

        this.miniMessageFormatter = new MiniMessageFormatter(this);
        this.placeholderHook = new PlaceholderHook(this);
        this.weatherHook = new WeatherHook(this);
        this.playerTracker = new PlayerTracker(this);
        this.geoHook = new GeoHook(this);
        this.scheduleManager = new ScheduleManager(this, weatherHook);
        this.iconManager = new IconManager(this);
        this.motdManager = new MOTDManager(this, miniMessageFormatter, placeholderHook, scheduleManager, iconManager, playerTracker);

        // Register listeners
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new PingListener(this, motdManager, iconManager, geoHook), this);
        pm.registerEvents(new JoinListener(playerTracker), this);

        // Register command
        PluginCommand cmd = getCommand("wmotd");
        if (cmd != null) {
            WMOTDCommand executor = new WMOTDCommand(this, motdManager, iconManager);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        // Start hooks/services
        this.weatherHook.start();
        this.iconManager.start();
        this.scheduleManager.start();
    }

    @Override
    public void onDisable() {
        if (this.weatherHook != null) this.weatherHook.stop();
        if (this.iconManager != null) this.iconManager.stop();
        if (this.scheduleManager != null) this.scheduleManager.stop();
        if (this.configManager != null) this.configManager.saveState();
    }

    public ConfigManager getConfigManager() { return configManager; }
    public MiniMessageFormatter getMiniMessageFormatter() { return miniMessageFormatter; }
    public PlaceholderHook getPlaceholderHook() { return placeholderHook; }
    public WeatherHook getWeatherHook() { return weatherHook; }
    public GeoHook getGeoHook() { return geoHook; }
    public MOTDManager getMotdManager() { return motdManager; }
    public IconManager getIconManager() { return iconManager; }
    public ScheduleManager getScheduleManager() { return scheduleManager; }
    public PlayerTracker getPlayerTracker() { return playerTracker; }
    public Instant getServerStartInstant() { return serverStartInstant; }
}


