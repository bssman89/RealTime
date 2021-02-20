package me.bizroomba.realtime;

import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A collection of plugin settings that can be applied to different worlds.
 */
public class SettingsProfile {

    private final String name;

    SettingsProfile(String profileName) {
        name = Objects.requireNonNull(profileName);
    }

    private static FileConfiguration getConfig() {
        return RealTimePlugin.getInstance().getConfig();
    }

    /**
     * Gets the name of this profile.
     *
     * @return the profile name, or empty if this is an empty profile
     */
    public String getName() {
        return name;
    }

    /**
     * Gets a list of the names of worlds this profile is applied to.
     *
     * @return unmodifiable list of world names
     */
    public List<String> getAffectedWorldNames() {
        List<String> profileWorldNames = new ArrayList<>();
        ConfigurationSection yamlWorlds = getConfig().getConfigurationSection("worlds");
        if (yamlWorlds != null) {
            for (String worldName : yamlWorlds.getKeys(false)) {
                String profileName = RealTimePlugin.getInstance().getSettingsProfileNameFor(worldName);
                if (profileName.equals(name)) {
                    profileWorldNames.add(worldName);
                }
            }
        }
        return Collections.unmodifiableList(profileWorldNames);
    }

    /**
     * Gets a list of the currently loaded worlds this profile is applied to.
     *
     * @return unmodifiable list of loaded worlds
     */
    public List<World> getAffectedLoadedWorlds() {
        List<World> profileLoadedWorlds = new ArrayList<>();
        List<String> profileWorldNames = getAffectedWorldNames();
        for (World loadedWorld : RealTimePlugin.getInstance().getServer().getWorlds()) {
            if (profileWorldNames.contains(loadedWorld.getName())) {
                profileLoadedWorlds.add(loadedWorld);
            }
        }
        return Collections.unmodifiableList(profileLoadedWorlds);
    }

    /**
     * Tests if worlds with this profile have their time synchronized with the system-time.
     *
     * @return true if syncing time, else false
     */
    public boolean isSyncTime() {
        return getConfig().getBoolean("settings." + name + ".sync-time", false);
    }

    /**
     * Sets if worlds with this profile have their time synchronized with the system-time.
     *
     * @param sync true if syncing time, else false
     */
    public void setSyncTime(boolean sync) {
        if (name.isEmpty()) return;
        getConfig().set("settings." + name + ".sync-time", sync);
    }

    /**
     * Gets the real life date time that corresponds with gametime 0.
     * This defaults to midnight, Jan 1st, 1 AD.
     * Gametime will not go below 0.
     *
     * @return the real life date time of gametime 0
     */
    public LocalDateTime getTimeZero() {
        try {
            String isoTimeZero = getConfig().getString("settings." + name + ".time-zero");
            if (isoTimeZero != null) {
                return LocalDateTime.parse(isoTimeZero, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
        }
        catch (DateTimeParseException ignored) {
        }
        return LocalDateTime.of(1, 1, 1, 0, 0, 0);
    }

    /**
     * Sets the real life date time that corresponds with gametime 0.
     * If null or after the current system time, it will default to midnight, Jan 1st, 1 AD.
     * Gametime will not go below 0.
     *
     * @param timeZero the real life date time of gametime 0.
     */
    public void setTimeZero(LocalDateTime timeZero) {
        if (name.isEmpty()) return;
        String isoTimeZero = null;
        if (timeZero != null && !LocalDateTime.now().isBefore(timeZero)) {
            isoTimeZero = timeZero.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        getConfig().set("settings." + name + ".time-zero", isoTimeZero);
    }

    /**
     * Gets the number of ticks ahead of the system-time worlds with
     * this profile would be when syncrhonized.
     *
     * @return minecraft ticks ahead (can be negative)
     */
    public long getTimeOffset() {
        return getConfig().getLong("settings." + name + ".offset", 0L);
    }

    /**
     * Sets the number of ticks ahead of the system-time worlds with
     * this profile would be when synchronized.
     *
     * @param ticks minecraft ticks ahead (can be negative)
     */
    public void setTimeOffset(long ticks) {
        if (name.isEmpty()) return;
        getConfig().set("settings." + name + ".offset", ticks);
    }

    /**
     * Gets the speed multiplier of the gametime relative to real life.
     *
     * @return a multiplier (can be negative; cannot be zero)
     */
    public double getTimeSpeed() {
        double multiplier = getConfig().getDouble("settings." + name + ".speed", 1.0);
        if (multiplier != 0) {
            return multiplier;
        }
        return 1.0;
    }

    /**
     * Sets the speed multiplier of the gametime relative to real life.
     *
     * @param multiplier a multiplier (can be negative; cannot be zero)
     */
    public void setTimeSpeed(double multiplier) {
        if (name.isEmpty()) return;
        if (multiplier != 0) {
            getConfig().set("settings." + name + ".speed", multiplier);
        }
    }

    /**
     * Tests if worlds with this profile have their weather synchronized with real life.
     *
     * @return true if syncing weather, else false
     */
    public boolean isSyncWeather() {
        return getConfig().getBoolean("settings." + name + ".sync-weather", false);
    }

    /**
     * Sets if worlds with this profile have their weather synchronized with real life.
     *
     * @param sync true if it should sync weather, else false
     */
    public void setSyncWeather(boolean sync) {
        if (name.isEmpty()) return;
        getConfig().set("settings." + name + ".sync-weather", sync);
    }

    /**
     * Gets the real life city with which weather would be synchronized with.
     * If this is empty, it will always be sunny.
     *
     * @return a string of the format: &lt;city&gt;[, &lt;country&gt;]
     */
    public String getWeatherCity() {
        String cityName = getConfig().getString("settings." + name + ".weather-city");
        if (cityName != null) {
            return cityName;
        }
        return "";
    }

    /**
     * Sets the real life city with which weather would be synchronized with.
     * If this is empty, it will always be sunny.
     *
     * @param cityName a string of the format: &lt;city&gt;[, &lt;country&gt;]
     */
    public void setWeatherCity(String cityName) {
        if (name.isEmpty()) return;
        getConfig().set("settings." + name + ".weather-city", cityName);
    }

    /**
     * Copy the settings of this profile to another profile.
     *
     * @param targetProfileName the profile whose settings will be overwritten
     */
    public void copyTo(String targetProfileName) {
        if (!targetProfileName.equals(name)) {
            SettingsProfile target = new SettingsProfile(targetProfileName);
            target.setSyncTime(isSyncTime());
            target.setTimeZero(getTimeZero());
            target.setTimeOffset(getTimeOffset());
            target.setTimeSpeed(getTimeSpeed());
            target.setSyncWeather(isSyncWeather());
            target.setWeatherCity(getWeatherCity());
        }
    }

    /**
     * Clears the data of this settings profile.
     * Any values used by synced worlds will be defaults.
     */
    public void clear() {
        getConfig().set("settings." + name, null);
    }
}
