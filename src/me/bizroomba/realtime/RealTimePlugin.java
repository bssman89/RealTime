package me.bizroomba.realtime;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The main class of the RealTime SpigotMC plugin.
 */
public class RealTimePlugin extends JavaPlugin implements Listener {

    Map<String, WeatherState> realLifeWeather = new HashMap<>();

    /**
     * Gets the instance of this plugin.
     *
     * @return the enabled plugin instance
     * @throws IllegalStateException when the plugin is not enabled
     */
    public static RealTimePlugin getInstance() throws IllegalStateException {
        try {
            return (RealTimePlugin) Objects.requireNonNull(Bukkit.getServer().getPluginManager().getPlugin("RealTime"));
        }
        catch (NullPointerException | ClassCastException ex) {
            throw new IllegalStateException("plugin is not enabled");
        }
    }

    /**
     * Registers the plugin's events and refreshes the plugin.
     */
    @Override
    public void onEnable() {
        getLogger().info("Before time began...");
        getServer().getPluginManager().registerEvents(this, this);
        onRefresh();
    }

    /**
     * Saves the config.
     */
    @Override
    public void onDisable() {
        saveConfig();
    }

    /**
     * Reloads the config, creating the default config if it doesn't exist,
     * and reschedules the plugin's tasks.
     */
    public void onRefresh() {
        saveDefaultConfig();
        reloadConfig();

        getServer().getScheduler().cancelTasks(this);

        getServer().getScheduler().runTaskTimer(this, PluginUtils::syncWorldsToRealLife, 1L, 1L);

        if (!getWeatherApiKey().isEmpty()) {
            int ticks = getWeatherFetchPeriod();
            getServer().getScheduler().runTaskTimer(this, PluginUtils::fetchRealLifeWeather, ticks, ticks);
        }

        if (isConfigAutosave()) {
            int ticks = getConfigAutosavePeriod();
            getServer().getScheduler().runTaskTimer(this, this::saveConfig, ticks, ticks);
        }
    }

    /**
     * Executes the plugin's "realtime" command and its subcommands.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        return PluginCmds.doCommand(sender, command, alias, args);
    }

    /**
     * Tab completes the plugin's "realtime" command and its subcommands.
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return PluginCmds.doTabComplete(sender, command, alias, args);
    }

    /**
     * Gets the cached weather state for the chosen city.
     *
     * @return the most recently fetched weather state, or CLEAR if unknown
     */
    public WeatherState getRealLifeWeather(String cityName) {
        return realLifeWeather.getOrDefault(cityName, WeatherState.CLEAR);
    }

    /**
     * Tests if the config should be autosaved.
     *
     * @return true if the config should be autosaved
     */
    public boolean isConfigAutosave() {
        return getConfig().getBoolean("config-autosave", false);
    }

    /**
     * Gets the number of ticks between config autosaves.
     *
     * @return an integer no less than 1200
     */
    public int getConfigAutosavePeriod() {
        int ticks = getConfig().getInt("config-autosave-period");
        if (ticks >= 1200) {
            return ticks;
        }
        return 1200;
    }

    /**
     * Gets the openweathermap.org API key used by the plugin.
     * If it is empty, the plugin will not use the weather sync feature.
     *
     * @return a confidential, alphanumeric api key or empty
     */
    public String getWeatherApiKey() {
        String apiKey = getConfig().getString("weather-api-key");
        if (apiKey != null) {
            return apiKey;
        }
        return "";
    }

    /**
     * Gets the number of ticks between weather syncrhonizations.
     * If this is too often you will max out your api key.
     *
     * @return an integer no less than 1200
     */
    public int getWeatherFetchPeriod() {
        int ticks = getConfig().getInt("fetch-weather-period");
        if (ticks >= 1200) {
            return ticks;
        }
        return 1200;
    }

    /**
     * Gets a list of all the names of worlds being affected by this plugin.
     *
     * @return unmodifiable list of world names
     */
    public List<String> getAllAffectedWorldNames() {
        List<String> affectedWorldNames = new ArrayList<>();
        ConfigurationSection yamlWorlds = getConfig().getConfigurationSection("worlds");
        if (yamlWorlds != null) {
            affectedWorldNames.addAll(yamlWorlds.getKeys(false));
        }
        return Collections.unmodifiableList(affectedWorldNames);
    }

    /**
     * Gets a list of all the currently loaded worlds that are affected by the plugin.
     *
     * @return unmodifiable list of loaded worlds
     */
    public List<World> getAllAffectedLoadedWorlds() {
        List<World> affectedWorlds = new ArrayList<>();
        List<String> affectedWorldNames = getAllAffectedWorldNames();
        for (World world : getServer().getWorlds()) {
            if (affectedWorldNames.contains(world.getName())) {
                affectedWorlds.add(world);
            }
        }
        return Collections.unmodifiableList(affectedWorlds);
    }

    /**
     * Gets the list of plugin settings profiles defined in the config.
     *
     * @return unmodifiable list of settings profiles
     */
    public List<SettingsProfile> getSettingsProfiles() {
        List<SettingsProfile> profiles = new ArrayList<>();
        ConfigurationSection yamlSettings = getConfig().getConfigurationSection("settings");
        if (yamlSettings != null) {
            for (String profileName : yamlSettings.getKeys(false)) {
                profiles.add(new SettingsProfile(profileName));
            }
        }
        return Collections.unmodifiableList(profiles);
    }

    /**
     * Gets the names of the plugin settings profiles defined in the config.
     *
     * @return unmodifiable list of settings profile names
     */
    public List<String> getSettingsProfileNames() {
        List<String> profileNames = new ArrayList<>();
        ConfigurationSection yamlSettings = getConfig().getConfigurationSection("settings");
        if (yamlSettings != null) {
            profileNames.addAll(yamlSettings.getKeys(false));
        }
        return Collections.unmodifiableList(profileNames);
    }

    /**
     * Gets a reference to the settings profile with the given name.
     * If the profile name is empty, the data cannot be saved.
     *
     * @param profileName a profile name that may or may not exist yet in the config, or an empty string
     */
    public SettingsProfile getSettingsProfile(String profileName) {
        return new SettingsProfile(Objects.requireNonNull(profileName));
    }

    /**
     * Gets the name of the settings profile applied to the world by the given name.
     * If the world name is empty, the profile name will be an empty string.
     *
     * @param worldName the name of a loaded or unloaded world.
     *
     * @return a plugin settings profile name or an empty string
     */
    public String getSettingsProfileNameFor(String worldName) {
        String profileName = getConfig().getString("worlds." + worldName);
        if (profileName != null) {
            return profileName;
        }
        return "";
    }

    /**
     * Gets the settings profile applied to the world by the given name.
     * If the world name is empty, the empty settings profile is returned.
     *
     * @param worldName the name of a loaded or unloaded world
     *
     * @return a plugin settings profile
     */
    public SettingsProfile getSettingsProfileFor(String worldName) {
        return new SettingsProfile(getSettingsProfileNameFor(worldName));
    }

    /**
     * Sets the plugin settings profile for the world by the given name.
     * If the profile name is empty, the world will no longer be affected by the plugin.
     *
     * @param worldName   the name of a loaded or unloaded world
     * @param profileName the name of a settings profile, or an empty string
     */
    public void setSettingsProfileFor(String worldName, String profileName) {
        if (!worldName.isEmpty()) {
            if (profileName.isEmpty()) {
                getConfig().set("worlds." + worldName, null);
            }
            else {
                getConfig().set("worlds." + worldName, profileName);
            }
        }
    }
}
