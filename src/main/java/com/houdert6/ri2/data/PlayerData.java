package com.houdert6.ri2.data;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class PlayerData implements ConfigurationSerializable {
    final int tier;
    final Map<?, ?> tierReqProgress;

    PlayerData(int tier, Map<?, ?> tierReqProgress) {
        this.tier = tier;
        this.tierReqProgress = tierReqProgress;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("tier", tier);
        if (tierReqProgress != null) {
            map.put("next-tier-progress", tierReqProgress);
        }
        return map;
    }
    public static PlayerData deserialize(Map<String, Object> map) {
        int tier = map.getOrDefault("tier", 0) instanceof Integer i ? i : 0;
        Map<?, ?> tierReqProgress = map.get("next-tier-progress") instanceof Map<?,?> nextTierProgMap ? nextTierProgMap : null;
        return new PlayerData(tier, tierReqProgress);
    }
}
