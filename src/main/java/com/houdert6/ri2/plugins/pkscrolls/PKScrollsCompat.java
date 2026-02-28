package com.houdert6.ri2.plugins.pkscrolls;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Class which RItems2 can use to interact with ProjectKorraScrolls
 */
public class PKScrollsCompat {
    private static PKScrollsCompat instance;

    PKScrollsCompat() {}

    /**
     * Generates a scroll for the given player
     * @return A scroll, or null if PKScrolls isn't available
     */
    public ItemStack getRandomScrollForPlayer(Player p) {
        return null;
    }

    /**
     * Returns the singleton instance of PKScrollsCompat, creating it if it doesn't exist
     */
    public static PKScrollsCompat instance(Server server) {
        if (instance != null)
            return instance;
        if (server.getPluginManager().isPluginEnabled("ProjectKorraScrolls")) {
            try {
                return instance = (PKScrollsCompat) Class.forName("com.houdert6.ri2.plugins.pkscrolls.PKScrollsCompatIfScrollsInstalledImpl").getDeclaredConstructor(Server.class).newInstance(server);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return instance = new PKScrollsCompat();
    }
}
