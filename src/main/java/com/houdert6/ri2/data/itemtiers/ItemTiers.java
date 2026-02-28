package com.houdert6.ri2.data.itemtiers;

import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ItemTiers {
    private final List<Tier> tiers = new ArrayList<>();
    /**
     * true if warnings were emitted while loading item tiers
     */
    public final boolean hadWarnings;

    /**
     * Reads any item tier data from the given configuration, and uses it to construct an ItemTiers instance
     * @param hasScrolls If ProjectKorraScrolls is enabled on the server and "scroll" is a valid item type
     * @param warningLogger A logger to log any warnings to
     */
    public ItemTiers(FileConfiguration config, boolean hasScrolls, Logger warningLogger) {
        int currentTierForLogging = 1;
        boolean hadWarnings = false;
        for (Map<?,?> tier : config.getMapList("item-tiers")) {
            WeightedList.Builder<RandomItem> itemsInTier = new WeightedList.Builder<>();
            if (tier.get("items") instanceof List<?> items) {
                for (Object entry : items) {
                    // Each item should either be a string, or a Map, and should be converted into a new random item
                    String type;
                    List<EntityType> entities;
                    int minEntities;
                    int maxEntities;
                    int enchantTier;
                    int minCount;
                    int maxCount;
                    int weight;
                    if (entry instanceof String itemId) {
                        // Entry is an item id string
                        type = itemId;
                        entities = List.of();
                        minEntities = 1;
                        maxEntities = 1;
                        enchantTier = 0;
                        minCount = 1;
                        maxCount = 1;
                        weight = 5;
                    } else if (entry instanceof Map<?,?> itemData) {
                        // Entry contains more data about the item
                        if (!(itemData.get("type") instanceof String itemType)) {
                            warningLogger.warning("Found item in tier " + currentTierForLogging + " without item type, skipping.");
                            hadWarnings = true;
                            continue; // Skip this item if it has no type
                        }
                        type = itemType;
                        if (itemData.get("entities") instanceof List<?> entityNameList) {
                            entities = new ArrayList<>();
                            for (Object entityEntry : entityNameList) {
                                if (entityEntry instanceof String entityName) {
                                    try {
                                        entities.add(EntityType.valueOf(entityName.toUpperCase()));
                                        continue;
                                    } catch (IllegalArgumentException ignore) {
                                    }
                                }
                                warningLogger.log(Level.WARNING, "found invalid entry in entities list: {0}", entityEntry);
                                hadWarnings = true;
                            }
                        } else {
                            entities = List.of();
                        }
                        if (itemData.get("min-entities") instanceof Number num) {
                            minEntities = num.intValue();
                            if (minEntities < 0) {
                                warningLogger.log(Level.WARNING, "min-entities for item in tier {0} is negative ({1}), defaulting to 1", new Object[]{currentTierForLogging, minEntities});
                                minEntities = 1;
                                hadWarnings = true;
                            }
                        } else {
                            minEntities = 1;
                        }
                        if (itemData.get("max-entities") instanceof Number num) {
                            maxEntities = num.intValue();
                            if (maxEntities < 0) {
                                warningLogger.log(Level.WARNING, "max-entities for item in tier {0} is negative ({1}), defaulting to min-entities", new Object[]{currentTierForLogging, maxEntities});
                                maxEntities = minEntities;
                                hadWarnings = true;
                            }
                        } else {
                            maxEntities = minEntities;
                        }
                        if (itemData.get("enchant-tier") instanceof Number num) {
                            enchantTier = Math.clamp(num.longValue(), 0, 7);
                        } else {
                            enchantTier = 0;
                        }
                        if (itemData.get("min-count") instanceof Number num) {
                            minCount = num.intValue();
                            if (minCount < 0) {
                                warningLogger.log(Level.WARNING, "min-count for item in tier {0} is negative ({1}), defaulting to 1", new Object[]{currentTierForLogging, minCount});
                                minCount = 1;
                                hadWarnings = true;
                            }
                        } else {
                            minCount = 1;
                        }
                        if (itemData.get("max-count") instanceof Number num) {
                            maxCount = num.intValue();
                            if (maxCount < 0) {
                                warningLogger.log(Level.WARNING, "max-count for item in tier {0} is negative ({1}), defaulting to min-count", new Object[]{currentTierForLogging, maxCount});
                                maxCount = 1;
                                hadWarnings = true;
                            }
                        } else {
                            maxCount = minCount;
                        }
                        if (itemData.get("weight") instanceof Number num) {
                            weight = num.intValue();
                            if (weight < 1) {
                                warningLogger.log(Level.WARNING, "weight for item in tier {0} is less than 1 ({1}), when item weights must be at least 1. Defaulting to 5", new Object[]{currentTierForLogging, weight});
                                weight = 5;
                                hadWarnings = true;
                            }
                        } else {
                            weight = 5;
                        }
                    } else {
                        warningLogger.log(Level.WARNING, "found invalid entry in items list: {0}", entry);
                        hadWarnings = true;
                        continue;
                    }
                    // Now convert this item data into an item (mainly just processing the item type)
                    type = type.toLowerCase();
                    if (type.equals("all")) {
                        itemsInTier.add(new RandomItem(Material.AIR, null, enchantTier, entities, Math.min(minEntities, maxEntities), Math.max(maxEntities, minEntities), Math.min(minCount, maxCount), Math.max(maxCount, minCount), true, false), weight);
                    } else if (type.startsWith("#")) {
                        NamespacedKey tagId = NamespacedKey.fromString(type.substring(1));
                        if (tagId == null) {
                            warningLogger.log(Level.WARNING, "Invalid tag syntax: {0}", type);
                            hadWarnings = true;
                            continue;
                        }
                        Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_ITEMS, tagId, Material.class);
                        if (tag == null) {
                            warningLogger.log(Level.WARNING, "Invalid tag: {0}", type);
                            hadWarnings = true;
                            continue;
                        }
                        // Add the tag as a possible random item
                        itemsInTier.add(new RandomItem(null, tag, enchantTier, entities, Math.min(minEntities, maxEntities), Math.max(maxEntities, minEntities), Math.min(minCount, maxCount), Math.max(maxCount, minCount), false, false), weight);
                    } else if (hasScrolls && type.equals("scroll")) {
                        itemsInTier.add(new RandomItem(Material.PAPER, null, enchantTier, entities, Math.min(minEntities, maxEntities), Math.max(maxEntities, minEntities), Math.min(minCount, maxCount), Math.max(maxCount, minCount), false, true), weight);
                    } else {
                        NamespacedKey itemId = NamespacedKey.fromString(type);
                        if (itemId == null) {
                            warningLogger.log(Level.WARNING, "Invalid item id syntax: {0}", type);
                            hadWarnings = true;
                            continue;
                        }
                        Material material = Registry.MATERIAL.get(itemId);
                        if (material == null) {
                            warningLogger.log(Level.WARNING, "Non-existent item in config for tier {0}: {1}", new Object[] {currentTierForLogging, type});
                            hadWarnings = true;
                            continue;
                        }
                        itemsInTier.add(new RandomItem(material, null, enchantTier, entities, Math.min(minEntities, maxEntities), Math.max(maxEntities, minEntities), Math.min(minCount, maxCount), Math.max(maxCount, minCount), false, false), weight);
                    }
                }
            }
            Map<TierUpRequirement, Integer> nextTierReq = null;
            if (tier.get("next-tier-requires") instanceof Map<?,?> tierReqMap) {
                for (Object keyObj : tierReqMap.keySet()) {
                    if (!(keyObj instanceof String key)) {
                        warningLogger.log(Level.WARNING, "invalid key for next-tier-requires somehow isn't a string: {0}", keyObj);
                        hadWarnings = true;
                        continue;
                    }
                    if (!(tierReqMap.get(keyObj) instanceof Number count)) {
                        warningLogger.log(Level.WARNING, "count for next tier requirement of tier {0} for item {1} is not a number: {2}", new Object[] {currentTierForLogging, key, tierReqMap.get(keyObj)});
                        hadWarnings = true;
                        continue;
                    }
                    TierUpRequirement req;
                    if (key.startsWith("#")) {
                        // Requirement is an item tag
                        NamespacedKey tagId = NamespacedKey.fromString(key.substring(1));
                        if (tagId == null) {
                            warningLogger.log(Level.WARNING, "Invalid tag syntax: {0}", key);
                            hadWarnings = true;
                            continue;
                        }
                        Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_ITEMS, tagId, Material.class);
                        req = new ItemTagTierUpRequirement(tag);
                    } else {
                        // Requirement is just an item
                        NamespacedKey itemId = NamespacedKey.fromString(key);
                        if (itemId == null) {
                            warningLogger.log(Level.WARNING, "Invalid item id syntax: {0}", key);
                            hadWarnings = true;
                            continue;
                        }
                        Material material = Registry.MATERIAL.get(itemId);
                        if (material == null) {
                            warningLogger.log(Level.WARNING, "Non-existent item in next-tier-requires for tier {0}: {1}", new Object[] {currentTierForLogging, key});
                            hadWarnings = true;
                            continue;
                        }
                        req = new SingleMaterialTierUpRequirement(material);
                    }
                    // Add the requirement to the map
                    if (nextTierReq == null) {
                        nextTierReq = new HashMap<>();
                    }
                    if (nextTierReq.containsKey(req)) {
                        // Just merge the two duplicate requirements
                        nextTierReq.put(req, nextTierReq.get(req) + count.intValue());
                    } else {
                        nextTierReq.put(req, count.intValue());
                    }
                }
            }
            tiers.add(new Tier(itemsInTier.build(), nextTierReq));
            currentTierForLogging++;
        }
        this.hadWarnings = hadWarnings;
    }

    /**
     * Returns a list of all random item types in the given numeric tier
     * @return item types in the tier, or an empty list if out of range.
     */
    public List<RandomItem> itemsInTier(int tier) {
        if (tier < 0 || tier >= tiers.size()) {
            return List.of();
        }
        return tiers.get(tier).items;
    }
    /**
     * Returns the requirements needed to progress from the given tier to the next one
     * @return map of requirements to how many are items needed if progression is possible, or null if it's not
     */
    public Map<TierUpRequirement, Integer> getNextTierRequirements(int tier) {
        if (tier < 0 || tier >= tiers.size()) {
            return null;
        }
        return tiers.get(tier).tierUpRequirements;
    }

    /**
     * The total number of configured tiers
     */
    public int numTiers() {
        return tiers.size();
    }

    private static class Tier {
        private final List<RandomItem> items;
        private final Map<TierUpRequirement, Integer> tierUpRequirements;

        private Tier(List<RandomItem> items, Map<TierUpRequirement, Integer> tierUpRequirements) {
            this.items = items;
            this.tierUpRequirements = tierUpRequirements == null ? null : Collections.unmodifiableMap(tierUpRequirements);
        }
    }
}
