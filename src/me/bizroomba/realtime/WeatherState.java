package me.bizroomba.realtime;

import org.bukkit.World;

/**
 * The possible weather states of a world.
 */
public enum WeatherState {
    CLEAR {
        @Override
        public void applyTo(World world) {
            world.setStorm(false);
            world.setThundering(false);
        }
    },
    RAIN {
        @Override
        public void applyTo(World world) {
            world.setThundering(false);
            world.setStorm(true);
        }
    },
    THUNDER {
        @Override
        public void applyTo(World world) {
            world.setStorm(true);
            world.setThundering(true);
        }
    };

    /**
     * Applies this weather state to the chosen world.
     *
     * @param world a loaded world
     */
    public abstract void applyTo(World world);

    /**
     * Determins the weather state from a weather description.
     * The word "thunder" signals THUNDER.
     * The words "rain", "shower", or "snow" signal RAIN.
     * Anything left will be CLEAR.
     *
     * @param weatherDescription a weather description text
     *
     * @return the most likely weather state represented by the text
     */
    public static WeatherState determineFrom(String weatherDescription) {
        String desc = weatherDescription.toLowerCase();
        if (desc.contains("thunder")) {
            return THUNDER;
        }
        else if (desc.contains("rain")
                || desc.contains("shower")
                || desc.contains("drizzle")
                || desc.contains("snow")) {
            return RAIN;
        }
        else {
            return CLEAR;
        }
    }
}
