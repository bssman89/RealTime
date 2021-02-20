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
        receiver.sendMessage(formatMsg("&f[&bReal&6Time&f]&r " + message, formatterObjs));
    }

    /**
     * Formats and logs the given message to the console at the info level with blue text.
     *
     * @param message       the debugging message
     * @param formatterObjs optional objects to format into the message
     */
    public static void infoMsg(String message, Object... formatterObjs) {
        RealTimePlugin.getInstance().getLogger().info(formatMsg(message, formatterObjs));
    }

    /**
     * Formats and logs the given message to the console at the warning level with red text.
     *
     * @param message       the debugging message
     * @param formatterObjs optional objects to format into the message
     */
    public static void warningMsg(String message, Object... formatterObjs) {
        RealTimePlugin.getInstance().getLogger().warning(formatMsg(message, formatterObjs));
    }

    /**
     * Searches the argument list for a pair of double or single quotes.
     * Any arguments within this pair are joined into a single argument by spaces
     * and the quotes are removed.
     *
     * @param args command arguments
     * @return quote-aware command arguments
     */
    public static String[] asQuoteAwareArgs(String[] args) {

        int startQuoteIdx = -1;
        int endQuoteIdx = -1;
        boolean isDoubleQuote = true;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (startQuoteIdx < 0) {

                if (arg.startsWith("\"")) {
                    startQuoteIdx = i;
                }
                else if (arg.startsWith("'")) {
                    startQuoteIdx = i;
                    isDoubleQuote = false;
                }
            }
            if (startQuoteIdx >= 0) {

                if (arg.endsWith("\"") && isDoubleQuote) {
                    endQuoteIdx = i;
                    break;
                }
                else if (arg.endsWith("'") && !isDoubleQuote) {
                    endQuoteIdx = i;
                    break;
                }
            }
        }
        if (startQuoteIdx < 0 || endQuoteIdx < 0) {
            return args;
        }
        else {
            List<String> newArgsList = new ArrayList<>();
            newArgsList.addAll(Arrays.asList(args).subList(0, startQuoteIdx));
            StringBuilder quotedTerm = new StringBuilder();
            for (int i = startQuoteIdx; i <= endQuoteIdx; i++) {
                quotedTerm.append(args[i]);
                if (i < endQuoteIdx) {
                    quotedTerm.append(' ');
                }
            }
            newArgsList.add(quotedTerm.substring(1, quotedTerm.length() - 1));
            newArgsList.addAll(Arrays.asList(args).subList(endQuoteIdx + 1, args.length));
            return newArgsList.toArray(new String[0]);
        }
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
                    pluginHelp += "&b/realtime syncworld <world> [<profile>] &7begin syncing the chosen world\n";
                    pluginHelp += "&b/realtime forgetworld <world> &7stop syncing the chosen world\n";
                    pluginHelp += "&b/realtime updatetime [<profile>] &7fetches and updates the rl time\n";
                    pluginHelp += "&b/realtime updateweather [<profile>] &7fetches and updates the rl weather\n";
                    pluginHelp += "&b/realtime getsynctime [<profile>] &7get whether time is being synced\n";
                    pluginHelp += "&b/realtime gettimezero [<profile>] &7get the rl time of gametime 0\n";
                    pluginHelp += "&b/realtime gettimeoffset [<profile>] &7get the ticks ahead gametime is from rl\n";
                    pluginHelp += "&b/realtime gettimespeed [<profile>] &7get the speed multiplier of gametime from rl\n";
                    pluginHelp += "&b/realtime getsyncweather [<profile>] &7get whether weather is being synced\n";
                    pluginHelp += "&b/realtime getweathercity [<profile>] &7get the rl city that weather is synced to\n";
                    pluginHelp += "&b/realtime setsynctime (true|false) [<profile>] &7set whether time is being synced\n";
                    pluginHelp += "&b/realtime settimezero <datetime> [<profile>] &7set the rl time of gametime 0\n";
                    pluginHelp += "&b/realtime settimeoffset <ticks> [<profile>] &7set the ticks ahead gametime is from rl\n";
                    pluginHelp += "&b/realtime settimespeed <multiplier> [<profile>] &7set the speed multiplier of gametime from rl\n";
                    pluginHelp += "&b/realtime setsyncweather (true|false) [<profile>] &7set whether weather is being synced\n";
                    pluginHelp += "&b/realtime setweathercity <\"city...\"> [<profile>] &7set the rl city that weather is synced to\n";
                }
                if (sender.hasPermission("realtime.admin")) {
                    pluginHelp += "&b/realtime reloadconfig &7reload the plugin's config, loosing any unsaved changes\n";
                    pluginHelp += "&b/realtime saveconfig &7saves any changes to the plugin's config";
                }
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', pluginHelp));
            }
            else if (args[0].equalsIgnoreCase("syncworld")) {
                if (!sender.hasPermission("realtime.mod")) {
                    chatMsg(sender, "&cYou don't have permission to do that");
                }
                else if (args.length == 2 || args.length == 3) {
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
                else if (args.length == 2) {
                    String worldName = args[1];
                    plugin.setSettingsProfileFor(worldName, "");
                    chatMsg(sender, "&aNo longer syncing " + worldName);
                }
                else {
                    chatMsg(sender, "&6/realtime forgetworld <world>");
                }
            }
            else if (args[0].equalsIgnoreCase("updatetime")) {
                if (!sender.hasPermission("realtime.mod")) {
                    chatMsg(sender, "&cYou don't have permission to do that");
                }
                else if (args.length == 1 || args.length == 2) {
                    String profileName = args.length == 2 ? args[1] : "default";
                    PluginUtils.syncWorldsToRealLifeInspected(sender);
                }
                else {
                    chatMsg(sender, "&6/realtime updatetime [<profile>]");
                }
            }
            else if (args[0].equalsIgnoreCase("updateweather")) {
                if (!sender.hasPermission("realtime.mod")) {
                    chatMsg(sender, "&cYou don't have permission to do that");
                }
                else if (args.length == 1 || args.length == 2) {
                    String profileName = args.length == 2 ? args[1] : "default";
                    PluginUtils.fetchRealLifeWeatherInspected(sender);
                }
                else {
                    chatMsg(sender, "&6/realtime updateweather [<profile>]");
                }
            }
            else if (args[0].equalsIgnoreCase("getsynctime")) {
                if (!sender.hasPermission("realtime.mod")) {
                    chatMsg(sender, "&cYou don't have permission to do that");
                }
                else if (args.length == 1 || args.length == 2) {
                    String profileName = args.length == 2 ? args[1] : "default";
                    boolean isSyncTime = plugin.getSettingsProfile(profileName).isSyncTime();
                    chatMsg(sender, "Got " + profileName + ".sync-time: " + isSyncTime);
                }
                else {
                    chatMsg(sender, "&6/realtime getsynctime [<profile>]");
                }
            }
            else if (args[0].equalsIgnoreCase("gettimezero")) {
                if (!sender.hasPermission("realtime.mod")) {
                    chatMsg(sender, "&cYou don't have permission to do that");
                }
                else if (args.length == 1 || args.length == 2) {
                    String profileName = args.length == 2 ? args[1] : "default";
                    LocalDateTime timeZero = plugin.getSettingsProfile(profileName).getTimeZero();
                    chatMsg(sender, "Got " + profileName + ".time-zero: " + timeZero.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                }
                else {
                    chatMsg(sender, "&6/realtime gettimezero [<profile>]");
                }
            }
            else if (args[0].equalsIgnoreCase("gettimeoffset")) {
                if (!sender.hasPermission("realtime.mod")) {
                    chatMsg(sender, "&cYou don't have permission to do that");
                }
                else if (args.length == 1 || args.length == 2) {
                    String profileName = args.length == 2 ? args[1] : "default";
                    long offset = plugin.getSettingsProfile(profileName).getTimeOffset();
                    chatMsg(sender, "Got " + profileName + ".time-offset: " + offset);
                }
                else {
                    chatMsg(sender, "&6/realtime gettimeoffset [<profile>]");
                }
            }
            else if (args[0].equalsIgnoreCase("gettimespeed")) {
                if (!sender.hasPermission("realtime.mod")) {
                    chatMsg(sender, "&cYou don't have permission to do that");
                }
                else if (args.length == 1 || args.length == 2) {
                    String profileName = args.length == 2 ? args[1] : "default";
                    double speed = plugin.getSettingsProfile(profileName).getTimeSpeed();
                    chatMsg(sender, "Got " + profileName + ".time-speed: " + speed);
                }
                else {
                    chatMsg(sender, "&6/realtime gettimespeed [<profile>]");
                }
            }
            else if (args[0].equalsIgnoreCase("getsyncweather")) {
                if (!sender.hasPermission("realtime.mod")) {
                    chatMsg(sender, "&cYou don't have permission to do that");
                }
                else if (args.length == 1 || args.length == 2) {
                    String profileName = args.length == 2 ? args[1] : "default";
                    boolean isSyncWeather = plugin.getSettingsProfile(profileName).isSyncWeather();
                    chatMsg(sender, "Got " + profileName + ".sync-weather: " + isSyncWeather);
                }
                else {
                    chatMsg(sender, "&6/realtime getsyncweather [<profile>]");
                }
            }
            else if (args[0].equalsIgnoreCase("getweathercity")) {
                if (!sender.hasPermission("realtime.mod")) {
                    chatMsg(sender, "&cYou don't have permission to do that");
                }
                else if (args.length == 1 || args.length == 2) {
                    String profileName = args.length == 2 ? args[1] : "default";
                    String cityName = plugin.getSettingsProfile(profileName).getWeatherCity();
                    chatMsg(sender, "Got " + profileName + ".weather-city: " + cityName);
                }
                else {
                    chatMsg(sender, "&6/realtime getweathercity [<profile>]");
                }
            }
            else if (args[0].equalsIgnoreCase("setsynctime")) {
                if (!sender.hasPermission("realtime.mod")) {
                    chatMsg(sender, "&cYou don't have permission to do that");
                }
                else if (args.length == 2 || args.length == 3) {
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
                    chatMsg(sender, "&6/realtime setsynctime true|false [<profile>]");
                }
            }
            else if (args[0].equalsIgnoreCase("settimezero")) {
                if (!sender.hasPermission("realtime.mod")) {
                    chatMsg(sender, "&cYou don't have permission to do that");
                }
                else if (args.length == 2 || args.length == 3) {
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
                    chatMsg(sender, "&6/realtime settimezero <time-zero> [<profile>]");
                    chatMsg(sender, "&6Time Zero should be in the ISO date-time format");
                }
            }
            else if (args[0].equalsIgnoreCase("settimeoffset")) {
                if (!sender.hasPermission("realtime.mod")) {
                    chatMsg(sender, "&cYou don't have permission to do that");
                }
                else if (args.length == 2 || args.length == 3) {
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
                    chatMsg(sender, "&6/realtime settimeoffset <ticks> [<profile>]");
                    chatMsg(sender, "&6Ticks should be an integer");
                }
            }
            else if (args[0].equalsIgnoreCase("settimespeed")) {
                if (!sender.hasPermission("realtime.mod")) {
                    chatMsg(sender, "&cYou don't have permission to do that");
                }
                else if (args.length == 2 || args.length == 3) {
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
                    chatMsg(sender, "&6/realtime settimespeed <multiplier> [<profile>]");
                    chatMsg(sender, "&6Multiplier should be a non-zero real number");
                }
            }
            else if (args[0].equalsIgnoreCase("setsyncweather")) {
                if (!sender.hasPermission("realtime.mod")) {
                    chatMsg(sender, "&cYou don't have permission to do that");
                }
                else if (args.length == 2 || args.length == 3) {
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
                    chatMsg(sender, "&6/realtime setsyncweather true|false [<profile>]");
                }
            }
            else if (args[0].equalsIgnoreCase("setweathercity")) {
                String[] cityArgs = asQuoteAwareArgs(args);

                if (!sender.hasPermission("realtime.mod")) {
                    chatMsg(sender, "&cYou don't have permission to do that");
                }
                else if (cityArgs.length >= 2) {
                    String cityName = cityArgs[1];
                    String profileName = cityArgs.length == 3 ? cityArgs[2] : "default";

                    if (cityName.contains("&") || cityName.contains("?") || cityName.contains("/")) {
                        chatMsg(sender, "&cCity contains invalid characters");
                    }
                    else {
                        plugin.getSettingsProfile(profileName).setWeatherCity(cityName);
                        chatMsg(sender, "&aSet " + profileName + ".weather-city: " + cityName);
                    }
                }
                else {
                    chatMsg(sender, "&6/realtime setweathercity <city> [<profile>]");
                    chatMsg(sender, "&6City should quoted if it contains spaces");
                }
            }
            else if (args[0].equalsIgnoreCase("reloadconfig")) {
                if (!sender.hasPermission("realtime.admin")) {
                    chatMsg(sender, "&cYou don't have permission to do that");
                }
                else if (args.length == 1) {
                    plugin.onRefresh();
                    chatMsg(sender, "&aReloaded the plugin's configuration");
                }
                else {
                    chatMsg(sender, "&6/realtime reloadconfig");
                }
            }
            else if (args[0].equalsIgnoreCase("saveconfig")) {
                if (!sender.hasPermission("realtime.admin")) {
                    chatMsg(sender, "&cYou don't have permission to do that");
                }
                else if (args.length == 1) {
                    plugin.saveConfig();
                    chatMsg(sender, "&aSaved the plugin's configuration");
                }
                else {
                    chatMsg(sender, "&6/realtime saveconfig");
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
                    if ("updatetime".startsWith(args[0])) options.add("updatetime");
                    if ("updateweather".startsWith(args[0])) options.add("updateweather");
                    if ("getsynctime".startsWith(args[0])) options.add("getsynctime");
                    if ("gettimezero".startsWith(args[0])) options.add("gettimezero");
                    if ("gettimeoffset".startsWith(args[0])) options.add("gettimeoffset");
                    if ("gettimespeed".startsWith(args[0])) options.add("gettimespeed");
                    if ("getsyncweather".startsWith(args[0])) options.add("getsyncweather");
                    if ("getweathercity".startsWith(args[0])) options.add("getweathercity");
                    if ("setsynctime".startsWith(args[0])) options.add("setsynctime");
                    if ("settimezero".startsWith(args[0])) options.add("settimezero");
                    if ("settimeoffset".startsWith(args[0])) options.add("settimeoffset");
                    if ("settimespeed".startsWith(args[0])) options.add("settimespeed");
                    if ("setsyncweather".startsWith(args[0])) options.add("setsyncweather");
                    if ("setweathercity".startsWith(args[0])) options.add("setweathercity");
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
                    else if (args[0].equalsIgnoreCase("setsynctime") || args[0].equalsIgnoreCase("setsyncweather")) {
                        if ("true".startsWith(args[1])) options.add("true");
                        if ("false".startsWith(args[1])) options.add("false");
                    }
                    else if (args[0].equalsIgnoreCase("updatetime")
                            || args[0].equalsIgnoreCase("updateweather")
                            || args[0].equalsIgnoreCase("getsynctime")
                            || args[0].equalsIgnoreCase("gettimezero")
                            || args[0].equalsIgnoreCase("gettimeoffset")
                            || args[0].equalsIgnoreCase("gettimespeed")
                            || args[0].equalsIgnoreCase("getsyncweather")
                            || args[0].equalsIgnoreCase("getweathercity")) {
                        for (String profileName : RealTimePlugin.getInstance().getSettingsProfileNames()) {
                            if (profileName.startsWith(args[1])) options.add(profileName);
                        }
                    }
                }
            }
            else if (args.length == 3) {
                if (sender.hasPermission("realtime.mod")) {
                    if (args[0].equalsIgnoreCase("setsynctime")
                            || args[0].equalsIgnoreCase("settimezero")
                            || args[0].equalsIgnoreCase("settimeoffset")
                            || args[0].equalsIgnoreCase("settimespeed")
                            || args[0].equalsIgnoreCase("setsyncweather")
                            || args[0].equalsIgnoreCase("setweathercity")) {
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
