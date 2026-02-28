package com.houdert6.ri2.plugins.pkscrolls;

import com.projectkorra.cozmyc.pkscrolls.ProjectKorraScrolls;
import com.projectkorra.cozmyc.pkscrolls.models.Scroll;
import com.projectkorra.cozmyc.pkscrolls.utils.ScrollItemFactory;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Class that is only ever loaded if ProjectKorraScrolls is enabled, making it safe to use classes and methods from ProjectKorraScrolls
 */
class PKScrollsCompatIfScrollsInstalledImpl extends PKScrollsCompat {
    private final Server server;
    PKScrollsCompatIfScrollsInstalledImpl(Server server) {
        this.server = server;
    }
    @Override
    public ItemStack getRandomScrollForPlayer(Player p) {
        ProjectKorraScrolls scrollsPlugin = (ProjectKorraScrolls) server.getPluginManager().getPlugin("ProjectKorraScrolls");
        if (scrollsPlugin == null)
            return null;
        // Get a random scroll
        Scroll randomScroll = scrollsPlugin.getScrollManager().getRandomScrollForPlayer(p, true, false, false, false);
        return ScrollItemFactory.createScroll(randomScroll);
    }
}
