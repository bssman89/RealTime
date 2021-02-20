package me.bizroomba.realtime;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginDescriptionFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Functions for executing and tab completing the plugin's commands.
 */
public class PluginCmds {

    private PluginCmds() {
    }

    /**
     * Tries to format the given objects into the message and replaces ampersands with the chat color symbol.
     * If there was a formatting exception, the message and objects are printed seperately instead.
     *
     * @param message       the message to optionally format and translate alternate color codes for
     * @param formatterObjs the optional formatter objects
     *
     * @return the formatted message
     */
    public static String formatMsg(String message, Object... formatterObjs) {
        try {
            return ChatColor.translateAlternateColorCodes('&', String.format(message, formatterObjs));
        }
        catch (IllegalFormatException ex) {
            return String.format("%s %% (%s)",
                    ChatColor.translateAlternateColorCodes('&', message),
                    Arrays.stream(formatterObjs).map(Objects::toString).collect(Collectors.joining(", ")));
        }
    }

    /**
     * Formats the given message and sends it to the receiver with the plugin's chat prefix.
     *
     * @param receiver      the command sender that will receive this message
     * @param message       the message with optional ampersand color codes
     * @param formatterObjs optional objects to format into the message
     */
    public static void chatMsg(CommandSender receiver, String message, Object... formatterObjs) {
        receiver.sendMessage(formatMsg("&f[&9Real&6Time&f]&r " + message, formatterObjs));
    }

    /**
     * Formats and logs the given message to the console at the info level with blue text.
     *
     * @param message       the debugging message
     * @param formatterObjs optional objects to format into the message
     */
    public static void debugMsg(String message, Object... formatterObjs) {
        RealTimePlugin.getInstance().getLogger().info("\\e[0;34m" + formatMsg(message, formatterObjs) + "\\e[0;37m");
    }

    /**
     * Formats and logs the given message to the console at the warning level with red text.
     *
     * @param message       the debugging message
     * @param formatterObjs optional objects to format into the message
     */
    public static void warningMsg(String message, Object... formatterObjs) {
        RealTimePlugin.getInstance().getLogger().warning("\\e[0;31m" + formatMsg(message, formatterObjs) + "\\e[0;37m");
    }

    /**
     * Executes the plugin's commands.
     */
    public static boolean doCommand(CommandSender sender, Command command, String alias, String[] args) {
        RealTimePlugin plugin = RealTimePlugin.getInstance();

        if (command.equals(plugin.getCommand("realtime"))) {
            if (args.length == 0) {
                PluginDescriptionFile info = plugin.getDescription();
                String pluginHelp = "";
                pluginHelp += "&6--====[ &e" + info.getFullName() + " &6]====--\n";
                pluginHelp += "&a" + info.getDescription() + "\n";
                pluginHelp += "&eBy: " + String.join(", ", info.getAuthors()) + "\n";
                pluginHelp += "&d" + info.getWebsite() + "\n";
                pluginHelp += "&f-------------------------\n";
                if (sender.hasPermission("realtime.mod")) {
                    pluginHelp += "&b/realtime syncworld <world> [<profile>] &7begin syncing the chosen world";
                    pluginHelp += "&b/realtime forgetworld <world> &7stop syncing the chosen world";
                    pluginHelp += "&b/realtime synctime <bool> [<profile>] &7set whether time is being synced";
                    pluginHelp += "&b/realtime timezero <datetime> [<profile>] &7set the rl time of gametime 0";
                    pluginHelp += "&b/realtime timeoffset <ticks> [<profile>] &7set the ticks ahead gametime is from rl";
                    pluginHelp += "&b/realtime timespeed <multiplier> [<profile>] &7set the speed multiplier of gametime from rl";
                    pluginHelp += "&b/realtime syncweather <bool> [<profile>] &7set whether weather is being synced";
                    pluginHelp += "&b/realtime weathercity <city> [<profile>] &7set the rl city that weather is synced to";
                }
                if (sender.hasPermission("realtime.admin")) {
                    pluginHelp += "&b/realtime reloadconfig &7reload the plugin's config";
                    pluginHelp += "&b/realtime toggledebugging &7print a debug message to console";
                }
                chatMsg(sender, pluginHelp);
            }
            else if (args[0].equalsIgnoreCase("syncworld")) {
                if (!sender.hasPermission("realtime.mod")) {
                    chatMsg(sender, "&cYou don't have permission to do that");
                }
                if (args.length == 2 || args.length == 3) {
                    String worldName = args[1];
                    String profileName = args.length == 3 ? args[2] : "default";
                    plugin.setSettingsProfileFor(worldName, profileName);
                    chatMsg(sender, "&aNow syncing " + worldName);
                }
                else {
                    chatMsg(sender, "&6/realtime syncworld <world> [<profile>]");
                }
            }
            else if (args[0].equalsIgnoreCase("forgetworld")) {
                if (!sender.hasPermission("realtime.mod")) {
                    chatMsg(sender, "&cYou don't have permission to do that");
                }
                if (args.length == 2) {
                    String worldName = args[1];
                    plugin.setSettingsProfileFor(worldName, "");
                    chatMsg(sender, "&aNo longer syncing " + worldName);
                }
                else {
                    chatMsg(sender, "&6/realtime forgetworld <world>");
                }
            }
            else if (args[0].equalsIgnoreCase("synctime")) {
                if (!sender.hasPermission("realtime.mod")) {
                    chatMsg(sender, "&cYou don't have permission to do that");
                }
                if (args.length == 2 || args.length == 3) {
                    String boolName = args[1];
                    String profileName = args.length == 3 ? args[2] : "default";

                    if (boolName.equalsIgnoreCase("true") || boolName.equalsIgnoreCase("false")) {
                        boolean state = Boolean.parseBoolean(boolName);
                        plugin.getSettingsProfile(profileName).setSyncTime(state);
                        chatMsg(sender, "&aSet " + profileName + ".sync-time: " + state);
                    }
                    else {
                        chatMsg(sender, "&cInvalid boolean: " + args[1]);
                    }
                }
                else {
                    chatMsg(sender, "&6/realtime synctime true|false [<profile>]");
                }
            }
            else if (args[0].equalsIgnoreCase("timezero")) {
                if (!sender.hasPermission("realtime.mod")) {
                    chatMsg(sender, "&cYou don't have permission to do that");
                }
                if (args.length == 2 || args.length == 3) {
                    String isoTimeZero = args[1];
                    String profileName = args.length == 3 ? args[2] : "default";

                    doSetTimeZero:
                    {
                        LocalDateTime timeZero;
                        try {
                            timeZero = LocalDateTime.parse(isoTimeZero, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                            if (!LocalDateTime.now().isAfter(timeZero)) {
                                chatMsg(sender, "&cTime zero must be in the past");
                                break doSetTimeZero;
                            }
                        }
                        catch (IllegalArgumentException ex) {
                            chatMsg(sender, "&cTime zero was not formatted correctly");
                            break doSetTimeZero;
                        }
                        plugin.getSettingsProfile(profileName).setTimeZero(timeZero);
                        chatMsg(sender, "&aSet " + profileName + ".time-zero: " + isoTimeZero);
                    }
                }
                else {
                    chatMsg(sender, "&6/realtime timezero <time-zero> [<profile>]");
                    chatMsg(sender, "&6Time Zero should be in the ISO date-time format");
                }
            }
            else if (args[0].equalsIgnoreCase("timeoffset")) {
                if (!sender.hasPermission("realtime.mod")) {
                    chatMsg(sender, "&cYou don't have permission to do that");
                }
                if (args.length == 2 || args.length == 3) {
                    String offsetName = args[1];
                    String profileName = args.length == 3 ? args[2] : "default";

                    doSetOffset:
                    {
                        long offset;
                        try {
                            offset = Long.parseLong(offsetName);
                        }
                        catch (NumberFormatException ex) {
                            chatMsg(sender, "&cTicks must be an integer");
                            break doSetOffset;
                        }
                        plugin.getSettingsProfile(profileName).setTimeOffset(offset);
                        chatMsg(sender, "&aSet " + profileName + ".time-offset: " + offsetName);
                    }
                }
                else {
                    chatMsg(sender, "&6/realtime timeoffset <ticks> [<profile>]");
                    chatMsg(sender, "&6Ticks should be an integer");
                }
            }
            else if (args[0].equalsIgnoreCase("timespeed")) {
                if (!sender.hasPermission("realtime.mod")) {
                    chatMsg(sender, "&cYou don't have permission to do that");
                }
                if (args.length == 2 || args.length == 3) {
                    String speedName = args[1];
                    String profileName = args.length == 3 ? args[2] : "default";

                    doSetSpeed:
                    {
                        double speed;
                        try {
                            speed = Double.parseDouble(speedName);
                            if (speed == 0) {
                                chatMsg(sender, "&cMultiplier cannot be zero");
                                break doSetSpeed;
                            }
                        }
                        catch (NumberFormatException ex) {
                            chatMsg(sender, "&cMultiplier must be a real number");
                            break doSetSpeed;
                        }
                        plugin.getSettingsProfile(profileName).setTimeSpeed(speed);
                        chatMsg(sender, "&aSet " + profileName + ".time-speed: " + speedName);
                    }
                }
                else {
                    chatMsg(sender, "&6/realtime timespeed <multiplier> [<profile>]");
                    chatMsg(sender, "&6Multiplier should be a non-zero real number");
                }
            }
            else if (args[0].equalsIgnoreCase("syncweather")) {
                if (!sender.hasPermission("realtime.mod")) {
                    chatMsg(sender, "&cYou don't have permission to do that");
                }
                if (args.length == 2 || args.length == 3) {
                    String boolName = args[1];
                    String profileName = args.length == 3 ? args[2] : "default";

                    if (boolName.equalsIgnoreCase("true") || boolName.equalsIgnoreCase("false")) {
                        boolean state = Boolean.parseBoolean(boolName);
                        plugin.getSettingsProfile(profileName).setSyncWeather(state);
                        chatMsg(sender, "&aSet " + profileName + ".sync-weather: " + state);
                    }
                    else {
                        chatMsg(sender, "&cInvalid boolean: " + args[1]);
                    }
                }
                else {
                    chatMsg(sender, "&6/realtime syncweather true|false [<profile>]");
                }
            }
            else if (args[0].equalsIgnoreCase("weathercity")) {
                if (!sender.hasPermission("realtime.mod")) {
                    chatMsg(sender, "&cYou don't have permission to do that");
                }
                if (args.length == 2 || args.length == 3) {
                    String cityName = args[1];
                    String profileName = args.length == 3 ? args[2] : "default";

                    if (cityName.contains("&") || cityName.contains("?") || cityName.contains("/")) {
                        chatMsg(sender, "&cCity contains invalid characters");
                    }
                    else {
                        plugin.getSettingsProfile(profileName).setWeatherCity(cityName);
                        chatMsg(sender, "&aSet " + profileName + ".weather-city: " + cityName);
                    }
                }
                else {
                    chatMsg(sender, "&6/realtime weathercity <city> [<profile>]");
                    chatMsg(sender, "&6City should be title cased, in the form: <city>[, <country>]");
                }
            }
            else if (args[0].equalsIgnoreCase("reloadconfig")) {
                if (!sender.hasPermission("realtime.admin")) {
                    chatMsg(sender, "&cYou don't have permission to do that");
                }
                if (args.length == 1) {
                    plugin.onRefresh();
                    chatMsg(sender, "&aReloaded the plugin's configuration");
                }
                else {
                    chatMsg(sender, "&6/realtime reloadconfig");
                }
            }
            else if (args[0].equalsIgnoreCase("toggledebugging")) {
                if (!sender.hasPermission("realtime.admin")) {
                    chatMsg(sender, "&cYou don't have permission to do that");
                }
                if (args.length == 1) {

                    plugin.debugMode = !plugin.debugMode;

                    chatMsg(sender, "&aTurned debugging mode " + (plugin.debugMode ? "on" : "off"));
                }
                else {
                    chatMsg(sender, "&6/realtime toggledebugging");
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Tab completes the plugin's commands.
     */
    public static List<String> doTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (command.equals(RealTimePlugin.getInstance().getCommand("realtime"))) {
            if (args.length == 1) {
                if (sender.hasPermission("realtime.mod")) {
                    if ("syncworld".startsWith(args[0])) options.add("syncworld");
                    if ("forgetworld".startsWith(args[0])) options.add("forgetworld");
                    if ("synctime".startsWith(args[0])) options.add("synctime");
                    if ("timezero".startsWith(args[0])) options.add("timezero");
                    if ("timeoffset".startsWith(args[0])) options.add("timeoffset");
                    if ("timespeed".startsWith(args[0])) options.add("timespeed");
                    if ("syncweather".startsWith(args[0])) options.add("syncweather");
                    if ("weathercity".startsWith(args[0])) options.add("weathercity");
                }
                if (sender.hasPermission("realtime.admin")) {
                    if ("reloadconfig".startsWith(args[0])) options.add("reloadconfig");
                    if ("toggledebugging".startsWith(args[0])) options.add("toggledebugging");
                }
            }
            else if (args.length == 2) {
                if (sender.hasPermission("realtime.mod")) {
                    if (args[0].equalsIgnoreCase("syncworld") || args[0].equalsIgnoreCase("forgetworld")) {
                        for (World world : RealTimePlugin.getInstance().getServer().getWorlds()) {
                            String worldName = world.getName();
                            if (worldName.startsWith(args[1])) {
                                options.add(worldName);
                            }
                        }
                    }
                    else if (args[0].equalsIgnoreCase("synctime") || args[0].equalsIgnoreCase("syncweather")) {
                        if ("true".startsWith(args[1])) options.add("true");
                        if ("false".startsWith(args[1])) options.add("false");
                    }
                }
            }
            else if (args.length == 3) {
                if (sender.hasPermission("realtime.mod")) {
                    if (args[0].equalsIgnoreCase("synctime")
                            || args[0].equalsIgnoreCase("timezero")
                            || args[0].equalsIgnoreCase("timeoffset")
                            || args[0].equalsIgnoreCase("timespeed")
                            || args[0].equalsIgnoreCase("syncweather")
                            || args[0].equalsIgnoreCase("weathercity")) {
                        for (String profileName : RealTimePlugin.getInstance().getSettingsProfileNames()) {
                            if (profileName.startsWith(args[2])) options.add(profileName);
                        }
                    }
                }
            }
        }
        return options;
    }
}
