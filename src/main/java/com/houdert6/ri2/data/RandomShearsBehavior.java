package com.houdert6.ri2.data;

/**
 * An enum that represents all the different ways random shears may work
 */
public enum RandomShearsBehavior {
    /**
     * Entities will take a heart of damage when sheared. The shears will not perform their usual action (e.g., removing wool from a sheep).
     * Entities will drop a random item when right clicked with shears, even if they are under no-damage-ticks and can't take damage. This mode encourages spam clicking shearable mobs for maximum output of random items.
     */
    SPAMMABLE("damage-spammable"),
    /**
     * Like spammable damage, entities will take a heart of damage when sheared and drop a random item. Unlike spammable damage, items will not drop when used during no damage ticks.
     */
    DAMAGE("damage"),
    /**
     * Right clicking a shearable entity drops a random item with a cooldown.
     */
    COOLDOWN("cooldown"),
    /**
     * Right clicking a shearable entity shears the entity but drops a random item instead.
     */
    SHEAR("shear"),
    /**
     * Random shears act like normal shears and don't drop random items.
     */
    DISABLED("disabled");

    private String configValue;

    RandomShearsBehavior(String configValue) {
        this.configValue = configValue;
    }

    /**
     * If the behavior operates by dealing damage to the sheared entity
     */
    public boolean isDamage() {
        return this == SPAMMABLE || this == DAMAGE;
    }

    /**
     * Returns the enum value of a shear behaviour in the RandomItems2 config
     */
    public static RandomShearsBehavior fromConfig(String configValue) {
        for (RandomShearsBehavior behavior : values()) {
            if (behavior.configValue.equals(configValue)) {
                return behavior;
            }
        }
        return null;
    }

    /**
     * Returns the enum value of a shear behaviour in the RandomItems2 config
     * @param def default behavior if the config value is unknown
     */
    public static RandomShearsBehavior fromConfig(String configValue, RandomShearsBehavior def) {
        RandomShearsBehavior behavior = fromConfig(configValue);
        return behavior == null ? def : behavior;
    }
}
