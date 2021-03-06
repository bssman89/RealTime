package me.bizroomba.realtime;

import org.bukkit.GameRule;
import org.bukkit.World;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Functions to carry out the plugin's synchronization features.
 */
public class PluginUtils {

    /**
     * The ratio between a minecraft tick and a real life second.
     */
    public static final double MC_RL_RATIO = 20 / 72d;

    private PluginUtils() {
    }

    /**
     * Synchronizes the gametime of affected worlds to the system time.
     */
    public static void syncWorldsToRealLife() {
        syncWorldsToRealLifeInspected(false);
    }

    /**
     * Updates the plugin's real-life weather cache using openweathermap.org.
     */
    public static void fetchRealLifeWeather() {
        fetchRealLifeWeatherInspected(false);
    }

    /**
     * Synchronizes the gametime of affected worlds to the system time.
     * The inspector is given information created during the process.
     *
     * @param inspect time syncing inspector or null
     */
    public static void syncWorldsToRealLifeInspected(boolean inspect) {
        RealTimePlugin plugin = RealTimePlugin.getInstance();
        LocalDateTime now = LocalDateTime.now();

        if (inspect) PluginCmds.shoutMsg("realtime.mod", "&aSystem time is &e" + now);

        for (SettingsProfile profile : plugin.getSettingsProfiles()) {

            long millis = ChronoUnit.MILLIS.between(profile.getTimeZero(), now);
            long rlt = (long) (Math.floor((millis / 1000d) * MC_RL_RATIO) + 18000);
            long gametime = (long) ((profile.getTimeSpeed() * rlt) + profile.getTimeOffset());

            String cityName = profile.getWeatherCity();
            WeatherState weather = plugin.getRealLifeWeather(cityName);

            if (inspect)
                PluginCmds.shoutMsg("realtime.mod", "&a%s: &e%s &arlt, &e%s &amct", profile.getName(), rlt, gametime);

            for (World affectedWorld : profile.getAffectedLoadedWorlds()) {

                if (profile.isSyncTime() && Optional.ofNullable(affectedWorld.getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE)).orElse(true)) {
                    affectedWorld.setFullTime(gametime);
                }
                if (profile.isSyncWeather() && Optional.ofNullable(affectedWorld.getGameRuleValue(GameRule.DO_WEATHER_CYCLE)).orElse(true)) {
                    weather.applyTo(affectedWorld);
                }
            }

        }
    }

    /**
     * Updates the plugin's real-life weather cache using openweathermap.org.
     * The inspector is given information created during the process.
     *
     * @param inspect weather syncing inspector or null
     */
    public static void fetchRealLifeWeatherInspected(boolean inspect) {
        RealTimePlugin plugin = RealTimePlugin.getInstance();
        String apiKey = plugin.getWeatherApiKey();
        if (apiKey.isEmpty()) {
            return;
        }

        for (SettingsProfile profile : plugin.getSettingsProfiles()) {
            String cityName = profile.getWeatherCity();
            if (cityName.isEmpty()) {
                continue;
            }
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                String json = requestOpenWeatherMapData(apiKey, cityName);
                WeatherState fetchedWeather = parseOpenWeatherMapData(json);

                plugin.getServer().getScheduler().runTask(plugin, () -> {

                    if (inspect) PluginCmds.shoutMsg("realtime.mod", "&a%s: &a%s is &e%s&a, API: '&e%s&a'", profile.getName(), cityName, fetchedWeather, json);

                    plugin.realLifeWeather.put(cityName, fetchedWeather);
                });
            });
        }
    }

    /**
     * Requests the json weather data for a city from api.openweathermap.org.
     * This function is designed to be run asynchronously from spigot.
     *
     * @param apiKey   the api key used to access the weather data
     * @param cityName the city to fetch the weather for
     *
     * @return a json string returned from openweathermap.org or an empty string
     */
    private static String requestOpenWeatherMapData(String apiKey, String cityName) {
        RealTimePlugin plugin = RealTimePlugin.getInstance();
        String protocol = "https://";
        String link = "api.openweathermap.org/data/2.5/weather?q=%s&appid=%s";
        try {
            URL url = new URL(protocol + String.format(link, cityName, apiKey));

            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            int status = con.getResponseCode();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder contents = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                contents.append(inputLine);
            }
            in.close();
            con.disconnect();

            return contents.toString();
        }
        catch (ProtocolException ex) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                PluginCmds.warningMsg("The protocol was invalid: " + ex.getMessage());
            });
        }
        catch (MalformedURLException ex) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                PluginCmds.warningMsg("The URL was malformed: " + protocol + String.format(link, cityName, "*****"));
            });
        }
        catch (IOException ex) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                PluginCmds.warningMsg("There was an IO exception: " + ex.getMessage());
            });
        }
        return "";
    }

    /**
     * Parses a weather state from the json weather data fetched from api.openweathermap.org.
     * This function is designed to be run asynchronously from spigot.
     *
     * @param json the weather data
     *
     * @return the parsed weather state or CLEAR if the data couldn't be parsed
     */
    private static WeatherState parseOpenWeatherMapData(String json) {
        try {
            Object objRoot = new JSONParser().parse(json);
            if (objRoot instanceof JSONObject) {
                JSONObject root = (JSONObject) objRoot;

                Object objWeather = root.get("weather");
                if (objWeather instanceof JSONArray) {
                    JSONArray weather = (JSONArray) objWeather;
                    if (!weather.isEmpty()) {
                        Object objWeatherFirst = weather.get(0);
                        if (objWeatherFirst instanceof JSONObject) {
                            JSONObject weatherFirst = (JSONObject) objWeatherFirst;

                            Object objWeatherMain = weatherFirst.get("main");
                            if (objWeatherMain instanceof String) {
                                String weatherMain = (String) objWeatherMain;
                                return WeatherState.determineFrom(weatherMain);
                            }
                        }
                    }
                }
            }
        }
        catch (ParseException ignored) {
        }
        return WeatherState.CLEAR;
    }
}
