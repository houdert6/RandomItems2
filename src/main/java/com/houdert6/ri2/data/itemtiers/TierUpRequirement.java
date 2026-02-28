package com.houdert6.ri2.data.itemtiers;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.function.Function;

/**
 * A requirement for tiering up
 */
public abstract class TierUpRequirement {
    /**
     * Checks if the given Material can fulfill the tier-up requirement
     */
    public abstract boolean matches(Material type);

    /**
     * A consistent key that can be used to identify this tier-up requirement
     */
    public abstract String key();

    /**
     * Replaces the given placeholder in the template with a human-readable form of this requirement's item(s), and returns it as a {@link Component}
     * @param preprocessor A preprocessor called on the given template at some point after all instances of the given placeholder are removed from the string. The placeholder's value is not guaranteed to have been added yet, and the preprocessor may potentially be called on multiple times on different split up parts of the template string. This is mainly used by RandomItems2 for adding PlaceholderAPI support and may be null
     */
    public abstract Component fillInPlaceholder(String template, Function<String, String> preprocessor, String placeholder);

    /**
     * Checks if two tier-up requirements are equal.
     * Two tier-up requirements are considered equal if the have the same {@link #key()}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TierUpRequirement tierUpReq) {
            return key().equals(tierUpReq.key());
        }
        return super.equals(obj);
    }
}
