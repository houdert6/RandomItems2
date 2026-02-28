package com.houdert6.ri2.plugins.papi;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

/**
 * Class that is only ever loaded if PlaceholderAPI is enabled, making it safe to use classes and methods from PAPI
 */
class PAPICompatIfPAPIIsInstalledImpl extends PlaceholderAPICompat {
    @Override
    public String fillInPlaceholders(String template, Player player) {
        return PlaceholderAPI.setPlaceholders(player, template);
    }
}
