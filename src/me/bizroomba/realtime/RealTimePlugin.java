package me.bizroomba.realtime;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.TimeSkipEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class RealTimePlugin extends JavaPlugin implements Listener {

    private static final LocalDateTime TIME_ZERO = LocalDateTime.of(1, 1, 1, 0, 0, 0);
    private static final double MC_RL_RATIO = 20 / 72d;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        getServer().getScheduler().runTaskTimer(this, this::syncGameTime, 1L, 1L);
        getServer().getScheduler().runTaskTimer(this, this::saveConfig, 24000L, 24000L);
        getLogger().info("Before time began...");
    }

    @Override
    public void onDisable() {
        saveConfig();
    }

    @EventHandler
    public void onTimeSkip(TimeSkipEvent event) {
        boolean applySkip = false;
        if (event.getSkipReason() == TimeSkipEvent.SkipReason.COMMAND) {
            if (getConfig().getBoolean("allow-time-skip-commands", false)) {
                applySkip = true;
            }
        }
        else if (event.getSkipReason() == TimeSkipEvent.SkipReason.NIGHT_SKIP) {
            if (getConfig().getBoolean("allow-time-skip-sleeping", false)) {
                applySkip = true;
            }
        }
        if (applySkip) {
            setWorldTimeOffset(event.getWorld(), event.getSkipAmount());
        }
    }

    public void syncGameTime() {
        long millis = ChronoUnit.MILLIS.between(TIME_ZERO, LocalDateTime.now());
        long rlt = (long) (Math.floor((millis / 1000d) * MC_RL_RATIO) + 18000);

        for (World syncedWorld : getSyncedWorlds()) {
            double m = getWorldTimeMultiplier(syncedWorld);
            double b = getWorldTimeOffset(syncedWorld);
            syncedWorld.setFullTime((long) ((m * rlt) + b));
        }
    }

    public double getWorldTimeMultiplier(World world) {
        return getConfig().getDouble("multipliers." + world.getName(), 1.0);
    }

    public long getWorldTimeOffset(World world) {
        return getConfig().getLong("offsets." + world.getName(), 0L);
    }

    public void setWorldTimeOffset(World world, long offset) {
        if (getSyncedWorlds().contains(world)) {
            getConfig().set("offsets." + world.getName(), offset);
        }
    }

    public List<World> getSyncedWorlds() {
        List<World> syncedWorlds;
        if (!getConfig().isList("worlds")) {
            syncedWorlds = getServer().getWorlds();
        } else {
            syncedWorlds = new ArrayList<>();
            for (String worldName : getConfig().getStringList("worlds")) {
                World world = getServer().getWorld(worldName);
                if (world != null) {
                    syncedWorlds.add(world);
                }
            }
        }
        return Collections.unmodifiableList(syncedWorlds);
    }
}
