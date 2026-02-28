package com.houdert6.ri2.plugins.papi;

import org.bukkit.Server;
import org.bukkit.entity.Player;

/**
 * Class which RItems2 can use to interact with PlaceholderAPI
 */
public class PlaceholderAPICompat {
    private static PlaceholderAPICompat instance;

    PlaceholderAPICompat() {}

    /**
     * Replaces all placeholders in the given template using whatever PlaceholderAPI placeholders are available
     */
    public String fillInPlaceholders(String template, Player player) {
        return template;
    }

    /**
     * Returns the singleton instance of PlaceholderAPICompat, creating it if it doesn't exist
     */
    public static PlaceholderAPICompat instance(Server server) {
        if (instance != null)
            return instance;
        if (server.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                return instance = (PlaceholderAPICompat) Class.forName("com.houdert6.ri2.plugins.papi.PAPICompatIfPAPIIsInstalledImpl").getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return instance = new PlaceholderAPICompat();
    }
}
