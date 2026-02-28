package com.houdert6.ri2.data.itemtiers;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.EntityType;

import java.util.List;
import java.util.Random;

/**
 * A type of random item
 * @param type type of random item
 * @param tag if nonnull then {@code type} is ignored and the type is picked randomly from this tag
 * @param enchantTier enchant tier from 1-7
 * @param entities entities a spawner can spawn
 * @param minEntities min entities on a spawner
 * @param maxEntities max entities on a spawner
 * @param minCount min item count
 * @param maxCount max item count
 * @param isAll if the item is the "all" type
 * @param isScroll if the item is a scroll from ProjectKorraScrolls
 */
public record RandomItem(Material type, Tag<Material> tag, int enchantTier, List<EntityType> entities, int minEntities, int maxEntities, int minCount, int maxCount, boolean isAll, boolean isScroll) {
    private static final Random RANDOM = new Random();

    /**
     * Converts a Bukkit Material to a RandomItem
     */
    public static RandomItem of(Material material) {
        return new RandomItem(material, null, 0, List.of(), 1, 1, 1, 1, false, false);
    }

    /**
     * Returns the type of this random item. If the random item was constructed from an item tag, a random value from that tag is returned every time
     * @see #frozen()
     */
    @Override
    public Material type() {
        return tag == null ? type : tag.getValues().stream().skip(RANDOM.nextInt(Math.max(tag.getValues().size(), 1))).findFirst().orElse(Material.AIR);
    }

    /**
     * Returns a "frozen" random item whose {@link #type()} is guaranteed to remain consistent across multiple calls.
     * If the item was constructed from an item tag, a random item from the tag is picked and a new random item of that type is returned.
     * If the random item was constructed in any other way then this method returns itself.
     * @see #type()
     */
    public RandomItem frozen() {
        return tag == null ? this : new RandomItem(type(), null, enchantTier, entities, minEntities, maxEntities, minCount, maxCount, isAll, isScroll);
    }
}
