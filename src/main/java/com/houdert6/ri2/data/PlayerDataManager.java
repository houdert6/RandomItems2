package com.houdert6.ri2.data;

import com.houdert6.ri2.data.itemtiers.TierUpRequirement;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Class for reading and manipulating saved player data
 */
public class PlayerDataManager {
    private final FileConfiguration playerData;
    private final File playerDataFile;

    public PlayerDataManager(FileConfiguration playerDataConfig, File playerDataFile) {
        this.playerData = playerDataConfig;
        this.playerDataFile = playerDataFile;
    }

    /**
     * Gets the current item tier of the given player
     */
    public int getTier(Player p) {
        return getTier(p.getUniqueId());
    }
    /**
     * Gets the current item tier of the given player
     */
    public int getTier(UUID playerId) {
        PlayerData data = getPlayerData(playerId);
        return data.tier;
    }

    /**
     * Sets the item tier of the given player to the specified new tier
     */
    public void setTier(Player p, int newTier) {
        setTier(p.getUniqueId(), newTier);
    }
    /**
     * Sets the item tier of the given player to the specified new tier
     */
    public void setTier(UUID playerId, int newTier) {
        PlayerData newData = new PlayerData(newTier, null); // Clear tier req progress upon changing tiers
        setPlayerData(playerId, newData);
    }

    /**
     * Gets the player's progress towards the next tier for the given tier requirement
     */
    public int getProgressToNextTier(Player p, TierUpRequirement req) {
        return getProgressToNextTier(p.getUniqueId(), req);
    }
    /**
     * Gets the player's progress towards the next tier for the given tier requirement
     */
    public int getProgressToNextTier(UUID uuid, TierUpRequirement req) {
        Map<?,?> tierReqMap = getPlayerData(uuid).tierReqProgress;
        if (tierReqMap == null)
            return 0;
        return tierReqMap.get(req.key()) instanceof Integer progress ? progress : 0;
    }

    /**
     * Makes the player progress towards the given tier requirement by the given amount
     */
    public void progress(Player p, TierUpRequirement req, int amount) {
        progress(p.getUniqueId(), req, amount);
    }
    /**
     * Makes the player progress towards the given tier requirement by the given amount
     */
    public void progress(UUID uuid, TierUpRequirement req, int amount) {
        PlayerData data = getPlayerData(uuid);
        HashMap<Object, Object> nextTierProgress = new HashMap<>(data.tierReqProgress == null ? Map.of() : data.tierReqProgress);
        int currentProgress = nextTierProgress.getOrDefault(req.key(), 0) instanceof Integer progress ? progress : 0;
        nextTierProgress.put(req.key(), currentProgress + amount);
        PlayerData newData = new PlayerData(data.tier, nextTierProgress);
        setPlayerData(uuid, newData);
    }

    // Some private helper methods to get and save player data
    private PlayerData getPlayerData(UUID uuid) {
        return playerData.getObject(uuid.toString(), PlayerData.class, new PlayerData(0, null));
    }
    private void setPlayerData(UUID uuid, PlayerData newData) {
        playerData.set(uuid.toString(), newData);
        try {
            playerData.save(playerDataFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
