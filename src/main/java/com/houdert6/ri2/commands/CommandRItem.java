package com.houdert6.ri2.commands;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;

import com.houdert6.ri2.plugins.papi.PlaceholderAPICompat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.StringUtil;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import com.houdert6.ri2.RandomItems2;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class CommandRItem implements TabExecutor {
	private final RandomItems2 instance;
	public final Map<String, ItemStack> customItems = new HashMap<>();
	
	public CommandRItem(RandomItems2 instanceIn) {
		this.instance = instanceIn;
		this.addCustomItems();
	}
	private void addCustomItems() {
		// Add the random shears to the custom items list (the list used by /randomitems give)
		ItemStack randomshears = new ItemStack(Material.SHEARS);
		ItemMeta meta = randomshears.getItemMeta();
		meta.addAttributeModifier(Attribute.MOVEMENT_SPEED, new AttributeModifier(new NamespacedKey(instance, "shears_mainhand_movement"), -0.5, Operation.ADD_SCALAR, EquipmentSlotGroup.HAND));
		meta.setDisplayName("§cRandom Shears");
		meta.addEnchant(Enchantment.MENDING, 1, true);
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		meta.setUnbreakable(true);
		randomshears.setItemMeta(meta);
        instance.setAction(randomshears, "randomshears");
		customItems.put("randomshears", randomshears);
		ItemStack randomfishingrod = new ItemStack(Material.FISHING_ROD);
		meta = randomfishingrod.getItemMeta();
		meta.addAttributeModifier(Attribute.MOVEMENT_SPEED, new AttributeModifier(new NamespacedKey(instance, "random_fishing_rod_speed"), -0.25, Operation.ADD_SCALAR, EquipmentSlotGroup.HAND));
		meta.setDisplayName("§cRandom Fishing Rod");
		meta.addEnchant(Enchantment.MENDING, 1, true);
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		meta.setUnbreakable(true);
		meta.setLore(ImmutableList.of("", "§7When Used:", "§9+ You will receive random items", "§9+ You have a greater chance of getting logs, ingots and diamonds", "§c- The area you fish in will explode every time you catch something"));
		randomfishingrod.setItemMeta(meta);
        instance.setAction(randomfishingrod, "randomfishingrod");
		customItems.put("randomfishingrod", randomfishingrod);
        ItemStack chillfishingrod = new ItemStack(Material.FISHING_ROD);
        meta.setDisplayName("§cRandom Fishing Rod");
        meta.setLore(ImmutableList.of("", "§7When Used:", "§9+ You will receive random items"));
        chillfishingrod.setItemMeta(meta);
        instance.setAction(chillfishingrod, "chillfishingrod");
        customItems.put("chillfishingrod", chillfishingrod);
		ItemStack randMinecart = new ItemStack(Material.MINECART);
		meta = randMinecart.getItemMeta();
		meta.setDisplayName("§bMinecart of Randomness");
		meta.setLore(ImmutableList.of("§6Who knows what's inside..."));
		randMinecart.setItemMeta(meta);
		customItems.put("minecartofrandomness", randMinecart);
	}

    /**
     * Constructs a component from a message in messages.yml, prefixed with instance.prefix
     * @param config The config path in messages.yml
     * @param def A default template if none is found in messages.yml
*      @param sender A player used for PlaceholderAPI placeholders
     * @param messageStringProcessor A preprocessor for message templates. Can be used to alter the template from the config before it's converted to a component
     */
    private Component getMsg(String config, String def, CommandSender sender, Function<String, String> messageStringProcessor) {
        return instance.getMsg(config, def, sender instanceof Player player ? player : null, messageStringProcessor);
    }
    /**
     * Generates a component for a generic command usage message for a command with no parameters
     */
    private Component usageMsg(String label, CommandSender player, String subcmd) {
        return usageMsg(label, player, subcmd, "");
    }
    /**
     * Generates a component for a generic command usage message
     */
    private Component usageMsg(String label, CommandSender player, String subcmd, String params) {
        return getMsg("usage", "&cUsage: &b/%alias% &6%subcmd% &c%params%", player, template ->template.replaceAll("%alias%", label).replaceAll("%subcmd%", subcmd).replaceAll("%params%", params));
    }
    /**
     * Generates a component for a generic no permission message
     * @param action the action to tell the player they don't have permission for
     */
    private Component noPermissionMsg(String action, CommandSender player, String subcmd) {
        return getMsg("no-permission", "&cYou don't have permission to %action%", player, template ->template.replaceAll("%action%", action).replaceAll("%subcmd%", subcmd));
    }

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length == 0 || args[0].equals("help")) {
			if (args.length > 1) {
				sender.sendMessage(usageMsg(label, sender, "help"));
				return true;
			}
            if (instance.getMessagesConfig().isList("help-message")) {
                List<?> lines = instance.getMessagesConfig().getList("help-message");
                instance.getLogger().info(String.join(", ", instance.getMessagesConfig().getKeys(true)));
                for (Object o : lines) {
                    String line;
                    if (o instanceof String str) {
                        line = str;
                    } else if (o instanceof Map<?,?> obj) {
                        if (obj.get("message") instanceof String str) {
                            line = str;
                            if (obj.get("permission") instanceof String perm && !sender.hasPermission(perm))
                                continue; // User doesn't have this line's permission
                        } else {
                            continue; // No line specified in object
                        }
                    } else {
                        continue; // Some weird data type in the help message
                    }
                    // Convert the line template to the final component
                    Component msg = LegacyComponentSerializer.legacyAmpersand().deserialize(PlaceholderAPICompat.instance(instance.getServer()).fillInPlaceholders(line.replaceAll("%alias%", label), sender instanceof Player p ? p : null));
                    sender.sendMessage(msg); // Send the line of the help message
                }
            } else {
                // Hardcoded fallback that should match the behaviour of the default config
                sender.sendMessage(ChatColor.GOLD + "RandomItems 2.0 Help");
                sender.sendMessage(ChatColor.AQUA + " - /" + label + " §6help§b: Displays this help message");
                if (sender.hasPermission("randomitems2.exempt")) {
                    sender.sendMessage(ChatColor.AQUA + " - /" + label + " §6exempt §c[player]§b: Toggles if random items will spawn near a player");
                }
                if (sender.hasPermission("randomitems2.toggle")) {
                    sender.sendMessage(ChatColor.AQUA + " - /" + label + " §6toggle§b: Toggles if random items will spawn near you");
                }
                if (sender.hasPermission("randomitems2.reload")) {
                    sender.sendMessage(ChatColor.AQUA + " - /" + label + " §6reload§b: Reloads all plugin configurations");
                }
                if (sender.hasPermission("randomitems2.give")) {
                    sender.sendMessage(ChatColor.AQUA + " - /" + label + " §6give §c<player> <item> [amount]§b: Can be used to give players special items");
                }
                if (sender.hasPermission("randomitems2.spawner")) {
                    sender.sendMessage(ChatColor.AQUA + " - /" + label + " §6spawner §c<player> <entity>[,<entities>...] [amount]§b: Can be used to give players custom mob spawners");
                }
                if (sender.hasPermission("randomitems2.tier")) {
                    sender.sendMessage(ChatColor.AQUA + " - /" + label + " §6tier §c<player> [new tier]§b: Can be used to view or change a player's item tier.");
                }
                sender.sendMessage(ChatColor.AQUA + " - Plugin made by " + ChatColor.GOLD + "houdert6");
            }
			return true;
		}
		if (args[0].equals("exempt")) {
            if (!sender.hasPermission("randomitems2.exempt")) {
                sender.sendMessage(noPermissionMsg("exempt players from receiving random items", sender, "exempt"));
                return true;
            }
			if (args.length > 2) {
                sender.sendMessage(usageMsg(label, sender, "exempt", "[player]"));
				return true;
			}
			String player = "";
			if (args.length == 2) {
				player = args[1];
			} else {
				if (!(sender instanceof Player)) {
					sender.sendMessage(getMsg("command-needs-player", "&cYou must either run this command as a player or specify a player", sender, template ->template.replaceAll("%action%", "exempt players from receiving random items").replaceAll("%subcmd%", "exempt")));
					return true;
				}
				player = sender.getName();
			}
            Player target = instance.getServer().getPlayer(player);
            if (target != null) {
                if (toggleRandomItems(target.getUniqueId())) {
                    sender.sendMessage(getMsg("enable-items-other", "Random items will spawn around %player% again", sender, template ->template.replaceAll("%player%", target.getName())));
                } else {
                    sender.sendMessage(getMsg("disable-items-other", "Random items will no longer spawn around %player%", sender, template ->template.replaceAll("%player%", target.getName())));
                }
            } else {
                String invalidPlayer = player; // For capturing
				sender.sendMessage(getMsg("player-not-found", "&cPlayer not found: &6%player%", sender, template ->template.replaceAll("%action%", "exempt players from receiving random items").replaceAll("%subcmd%", "exempt").replaceAll("%player%", invalidPlayer)));
			}
			return true;
		}
        if (args[0].equals("toggle")) {
            if (!sender.hasPermission("randomitems2.toggle")) {
                sender.sendMessage(noPermissionMsg("toggle the spawning of random items near you", sender, "toggle"));
                return true;
            }
            if (args.length != 1) {
                sender.sendMessage(usageMsg(label, sender, "toggle"));
                return true;
            }
            if (!(sender instanceof Player p)) {
                sender.sendMessage(getMsg("must-be-player", "&cThis command must be run by a player", sender, template ->template.replaceAll("%action%", "toggle the spawning of random items near you").replaceAll("%subcmd%", "toggle")));
                return true;
            }
            if (toggleRandomItems(p.getUniqueId())) {
                sender.sendMessage(getMsg("enable-items-self", "Random items will spawn around you again", sender, template ->template.replaceAll("%player%", p.getName())));
            } else {
                sender.sendMessage(getMsg("disable-items-self", "Random items will no longer spawn around you", sender, template ->template.replaceAll("%player%", p.getName())));
            }
            return true;
        }
		if (args[0].equals("reload")) {
            if (!sender.hasPermission("randomitems2.reload")) {
                sender.sendMessage(noPermissionMsg("reload the plugin config", sender, "reload"));
                return true;
            }
			if (args.length != 1) {
				sender.sendMessage(usageMsg(label, sender, "reload"));
				return true;
			}
			instance.reloadConfig();
			boolean hadWarnings = instance.reloadConfigRI();
            try {
                instance.reloadMessagesConfig();
            } catch (Exception e) {
                instance.getLogger().log(Level.WARNING, "Failed to load messages.yml", e);
                // Using a messages.yml message makes no sense for an error about messages.yml failing to load
                sender.sendMessage(instance.prefix.append(Component.text("Something went wrong reloading messages.yml, check console for more info.").color(NamedTextColor.RED)));
                return true;
            }
            if (hadWarnings) {
                sender.sendMessage(getMsg("config-reload-with-warnings", "Reloaded &e(with warnings)", sender, null));
            } else {
                sender.sendMessage(getMsg("config-reload", "Reloaded!", sender, null));
            }
			return true;
		}
		if (args[0].equals("give")) {
            if (!sender.hasPermission("randomitems2.give")) {
                sender.sendMessage(noPermissionMsg("give special items", sender, "give"));
                return true;
            }
			if (args.length != 3 && args.length != 4) {
				sender.sendMessage(usageMsg(label, sender, "give", "<player> <item> [amount]"));
				return true;
			}
			Player player = this.instance.getServer().getPlayer(args[1]);
			if (player == null) {
                sender.sendMessage(getMsg("player-not-found", "&cPlayer not found: &6%player%", sender, template ->template.replaceAll("%action%", "give special items").replaceAll("%subcmd%", "give").replaceAll("%player%", args[1])));
				return true;
			}
			if (!this.customItems.containsKey(args[2])) {
                sender.sendMessage(getMsg("invalid-item", "&cInvalid item: &6%item%", sender, template ->template.replaceAll("%item%", args[2])));
				return true;
			}
			ItemStack item = new ItemStack(this.customItems.get(args[2]));
			int amount = 1;
			if (args.length == 4) {
				try {
					amount = Integer.parseInt(args[3]);
				} catch (NumberFormatException e) {
					sender.sendMessage(getMsg("invalid-number", "&6%number% &cis not a valid number", sender, template ->template.replaceAll("%action%", "give special items").replaceAll("%subcmd%", "give").replaceAll("%number%", args[3])));
					return true;
				}
			}
			item.setAmount(amount);
			HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(item);
            sender.sendMessage(getMsg("gave-items", "Gave &c%player% &6%amount% &bof &6%item%", sender, template ->template.replaceAll("%player%", args[1]).replaceAll("%amount%", args.length == 4 ? args[3] : "1").replaceAll("%item%", args[2])));
			if (!remaining.isEmpty()) {
				sender.sendMessage(getMsg("no-space-for-some-items", "&cSome items didn't fit in the player's inventory and were lost!", sender, template ->{
                    int lost = 0;
                    for (ItemStack stack : remaining.values()) {
                        lost += stack.getAmount();
                    }
                    return template.replaceAll("%player%", args[1]).replaceAll("%amount%", args.length == 4 ? args[3] : "1").replaceAll("%lost%", Integer.toString(lost)).replaceAll("%item%", args[2]);
                }));
			}
			return true;
		}
		if (args[0].equals("spawner")) {
            if (!sender.hasPermission("randomitems2.spawner")) {
                sender.sendMessage(noPermissionMsg("give custom spawners", sender, "spawner"));
                return true;
            }
			if (args.length != 3 && args.length != 4) {
				sender.sendMessage(usageMsg(label, sender, "spawner", "<player> <entity>[,<entities>...] [amount]"));
				return true;
			}
			Player player = this.instance.getServer().getPlayer(args[1]);
			if (player == null) {
                sender.sendMessage(getMsg("player-not-found", "&cPlayer not found: &6%player%", sender, template ->template.replaceAll("%action%", "give custom spawners").replaceAll("%subcmd%", "spawner").replaceAll("%player%", args[1])));
				return true;
			}
			EntityType[] entities = EntityType.values();
			List<String> entityNames = Lists.newArrayList(ImmutableList.copyOf(entities).stream().map((EntityType entity) -> entity.name().toLowerCase()).toArray(String[]::new));
			entityNames.addAll(Lists.newArrayList(ImmutableList.copyOf(entities).stream().map((EntityType entity) -> entity.name().toLowerCase() + "_minecart").toArray(String[]::new)));
			String[] specifiedEntities = args[2].split(",");
			if (specifiedEntities.length > 1) { // Multi-entity spawner
				List<EntityType> toSpawn = new ArrayList<>();
				boolean minecart = false; // If the spawner should be a spawner minecart; true if any of the entity choices specify so
				for (String specified : specifiedEntities) {
					if (!entityNames.contains(specified)) {
                        String invalidEntity = specified; // For capturing
						sender.sendMessage(getMsg("invalid-entity", "&cInvalid entity: &6%entity%", sender, template ->template.replaceAll("%entity%", invalidEntity)));
						return true;
					}
					if (specified.endsWith("_minecart")) {
						specified = specified.substring(0, specified.length() - "_minecart".length());
						minecart = true;
					}
					toSpawn.add(EntityType.valueOf(specified.toUpperCase()));
				}
				ItemStack item = new ItemStack(minecart ? Material.HOPPER_MINECART : Material.SPAWNER);
				ItemMeta meta = item.getItemMeta();
				PersistentDataContainer[] entityData = new PersistentDataContainer[toSpawn.size()];
				for (int i = 0; i < entityData.length; i++) {
					PersistentDataContainer spawnData = meta.getPersistentDataContainer().getAdapterContext().newPersistentDataContainer();
					spawnData.set(NamespacedKey.fromString("spawner", this.instance), PersistentDataType.STRING, toSpawn.get(i).name());
					entityData[i] = spawnData;
				}
				meta.getPersistentDataContainer().set(NamespacedKey.fromString("multispawner", this.instance), PersistentDataType.TAG_CONTAINER_ARRAY, entityData);
				meta.setLore(ImmutableList.copyOf(toSpawn.stream().map((type) -> "§7§oSpawns: §c" + type.name()).toArray(String[]::new)));
				// Item giving code from above
				item.setItemMeta(meta);
				int amount = 1;
				if (args.length == 4) {
					try {
						amount = Integer.parseInt(args[3]);
					} catch (NumberFormatException e) {
                        sender.sendMessage(getMsg("invalid-number", "&6%number% &cis not a valid number", sender, template ->template.replaceAll("%action%", "give custom spawners").replaceAll("%subcmd%", "spawner").replaceAll("%number%", args[3])));
						instance.getLogger().log(Level.WARNING, e.toString());
						return true;
					}
				}
				item.setAmount(amount);
				HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(item);
                final int capturedAmount = amount; // For capturing in a lambda below
                sender.sendMessage(getMsg("gave-multi-entity-spawners", "Gave &c%player% &6%amount% &bmulti-entity %item%", sender, template ->template.replaceAll("%player%", args[1]).replaceAll("%amount%", args.length == 4 ? args[3] : "1").replaceAll("%item%", capturedAmount == 1 ? "spawner" : "spawners").replaceAll("%entities%", String.join(", ", specifiedEntities))));
				if (!remaining.isEmpty()) {
                    sender.sendMessage(getMsg("no-space-for-some-spawners", "&cSome items didn't fit in the player's inventory and were lost!", sender, template ->{
                        int lost = 0;
                        for (ItemStack stack : remaining.values()) {
                            lost += stack.getAmount();
                        }
                        return template.replaceAll("%player%", args[1]).replaceAll("%amount%", args.length == 4 ? args[3] : "1").replaceAll("%lost%", Integer.toString(lost));
                    }));
                }
				return true;
			}
			if (!entityNames.contains(args[2])) {
                sender.sendMessage(getMsg("invalid-entity", "&cInvalid entity: &6%entity%", sender, template ->template.replaceAll("%entity%", args[2])));
				return true;
			}
			ItemStack item = new ItemStack(Material.SPAWNER);
			ItemMeta meta = item.getItemMeta();
			if (args[2].endsWith("_minecart")) {
				item = new ItemStack(Material.HOPPER_MINECART);
				meta = item.getItemMeta();
				EntityType toSpawn = EntityType.valueOf(args[2].substring(0, args[2].length() - "_minecart".length()).toUpperCase());
				meta.getPersistentDataContainer().set(NamespacedKey.fromString("spawner", this.instance), PersistentDataType.STRING, toSpawn.name());
				meta.setLore(ImmutableList.copyOf(new String[] {"§7§oSpawns: §c" + toSpawn.name()}));
				meta.setDisplayName("§4Spawner Minecart");
			} else {
				//ItemMeta meta = item.getItemMeta();
				EntityType toSpawn = EntityType.valueOf(args[2].toUpperCase());
				meta.getPersistentDataContainer().set(NamespacedKey.fromString("spawner", this.instance), PersistentDataType.STRING, toSpawn.name());
				meta.setLore(ImmutableList.copyOf(new String[] {"§7§oSpawns: §c" + toSpawn.name()}));
			}
			item.setItemMeta(meta);
			int amount = 1;
			if (args.length == 4) {
				try {
					amount = Integer.parseInt(args[3]);
				} catch (NumberFormatException e) {
                    sender.sendMessage(getMsg("invalid-number", "&6%number% &cis not a valid number", sender, template ->template.replaceAll("%action%", "give custom spawners").replaceAll("%subcmd%", "spawner").replaceAll("%number%", args[3])));
					instance.getLogger().log(Level.WARNING, e.toString());
					return true;
				}
			}
			item.setAmount(amount);
			HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(item);
            final int capturedAmount = amount; // For capturing in a lambda below
            sender.sendMessage(getMsg("gave-spawners", "Gave &c%player% &6%amount% &b%item% of type &6%entity%", sender, template ->template.replaceAll("%player%", args[1]).replaceAll("%amount%", Integer.toString(capturedAmount)).replaceAll("%item%", (capturedAmount == 1 ? "spawner" : "spawners")).replaceAll("%entity%", args[2])));
			if (!remaining.isEmpty()) {
                sender.sendMessage(getMsg("no-space-for-some-spawners", "&cSome items didn't fit in the player's inventory and were lost!", sender, template ->{
                    int lost = 0;
                    for (ItemStack stack : remaining.values()) {
                        lost += stack.getAmount();
                    }
                    return template.replaceAll("%player%", args[1]).replaceAll("%amount%", args.length == 4 ? args[3] : "1").replaceAll("%lost%", Integer.toString(lost));
                }));
            }
			return true;
		}
        if (args[0].equals("tier")) {
            if (!sender.hasPermission("randomitems2.tier")) {
                sender.sendMessage(noPermissionMsg("view/change players' item tiers", sender, "tier"));
                return true;
            }
            if (args.length != 2 && args.length != 3) {
                sender.sendMessage(usageMsg(label, sender, "tier", "<player> [new tier]"));
                return true;
            }
            Player player = this.instance.getServer().getPlayer(args[1]);
            if (player == null) {
                sender.sendMessage(getMsg("player-not-found", "&cPlayer not found: &6%player%", sender, template ->template.replaceAll("%action%", "view/change players' item tiers").replaceAll("%subcmd%", "tier").replaceAll("%player%", args[1])));
                return true;
            }
            if (instance.playerDataManager() == null) {
                sender.sendMessage(getMsg("error-accessing-playerdata", "&cAn error is preventing the plugin from accessing player data.", sender, null));
                return true;
            }
            if (args.length == 2) {
                int tier = instance.playerDataManager().getTier(player) + 1; // Tiers are stored as 0-indexed but inputted and outputted as 1-indexed
                sender.sendMessage(getMsg("tier-output", "&c%player%'s &btier is &6%tier%", sender, template ->template.replaceAll("%player%", args[1]).replaceAll("%tier%", Integer.toString(tier))));
                return true;
            } else {
                try {
                    int newTier = Integer.parseInt(args[2]);
                    if (newTier < 1) {
                        sender.sendMessage(getMsg("tier-too-low", "&cThe new tier must be ≥ 1", sender, template ->template.replaceAll("%player%", args[1]).replaceAll("%tier%", Integer.toString(newTier))));
                        return true;
                    }
                    if (newTier > instance.itemTiers().numTiers()) {
                        sender.sendMessage(getMsg("tier-too-high", "&cThe maximum tier on this server is &6%maxtier%", sender, template ->template.replaceAll("%player%", args[1]).replaceAll("%tier%", Integer.toString(newTier)).replaceAll("%maxtier%", Integer.toString(instance.itemTiers().numTiers()))));
                        return true;
                    }
                    instance.playerDataManager().setTier(player, newTier - 1);
                    sender.sendMessage(getMsg("tier-set", "Set &c%player%&b's tier to &r%tier%", sender, template ->template.replaceAll("%player%", args[1]).replaceAll("%tier%", Integer.toString(newTier))));
                    return true;
                } catch (NumberFormatException e) {
                    sender.sendMessage(getMsg("invalid-number", "&6%number% &cis not a valid number", sender, template ->template.replaceAll("%action%", "view/change players' item tiers").replaceAll("%subcmd%", "tier").replaceAll("%number%", args[2])));
                    instance.getLogger().log(Level.WARNING, e.toString());
                    return true;
                }
            }
        }
        if (args[0].equals("tierupzone")) {
            if (!sender.hasPermission("randomitems2.tierupzone")) {
                sender.sendMessage(noPermissionMsg("create a tier-up zone", sender, "tierupzone"));
                return true;
            }
            if (args.length != 1) {
                sender.sendMessage(usageMsg(label, sender, "tierupzone"));
                return true;
            }
            if (!(sender instanceof Player p)) {
                sender.sendMessage(getMsg("must-be-player", "&cThis command must be run by a player", sender, template ->template.replaceAll("%action%", "create a tier-up zone").replaceAll("%subcmd%", "tierupzone")));
                return true;
            }
            // Hitbox for tier-up zone
            Interaction tierUpInteraction = p.getWorld().spawn(p.getLocation(), Interaction.class);
            tierUpInteraction.setInteractionWidth(2);
            tierUpInteraction.setInteractionHeight(2);
            tierUpInteraction.setResponsive(true); // Right click plays an animation
            tierUpInteraction.getPersistentDataContainer().set(new NamespacedKey(instance, "action"), PersistentDataType.STRING, "tierup");
            // Block model for tier-up zone
            BlockDisplay tierUpModel = p.getWorld().spawn(p.getLocation(), BlockDisplay.class);
            tierUpModel.setBlock(Material.CHEST.createBlockData());
            tierUpModel.setBillboard(Display.Billboard.VERTICAL);
            tierUpModel.setTransformation(new Transformation(new Vector3f(-1, 0, -1), new Quaternionf(0, 0, 0, 1), new Vector3f(2, 2, 2), new Quaternionf(0, 0, 0, 1)));
            tierUpModel.setRotation(0, 0);
            // Text
            TextDisplay tierUpTitle = p.getWorld().spawn(p.getLocation().add(0, 2, 0), TextDisplay.class);
            tierUpTitle.text(LegacyComponentSerializer.legacyAmpersand().deserialize(PlaceholderAPICompat.instance(instance.getServer()).fillInPlaceholders(instance.getMessagesConfig().getString("tier-up-zone-title", "&aRight click to deposit items"), p)));
            tierUpTitle.setBillboard(Display.Billboard.VERTICAL);
            tierUpTitle.setRotation(0, 0);
            sender.sendMessage(getMsg("tier-up-zone-created", "Tier up zone created!", sender, null));
            return true;
        }
        sender.sendMessage(getMsg("unknown-subcommand", "&cUnknown subcommand: &6%subcmd%", sender, template ->template.replaceAll("%player%", sender.getName()).replaceAll("%subcmd%", args[0])));
		return true;
	}

    /**
     * Toggles if the given player receives random items.
     * @return if the player will now receive items
     */
    private boolean toggleRandomItems(UUID playerId) {
        List<String> exemptions = instance.getConfig().getStringList("exempt");
        boolean ret;
        if (exemptions.contains(playerId.toString())) {
            exemptions.remove(playerId.toString());
            ret = true;
        } else {
            exemptions.add(playerId.toString());
            ret = false;
        }
        instance.getConfig().set("exempt", exemptions);
        instance.saveConfig();
        return ret;
    }

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
		List<String> complete = Lists.newArrayList();
		String[] commands = {"exempt", "toggle", "reload", "give", "spawner", "tier", "tierupzone"};
		if (args.length == 1) {
			complete.add("help"); // All players who have access to the plugin command have /randomitems help
			for (String cmd : commands) {
                if (sender.hasPermission("randomitems2." + cmd)) {
                    complete.add(cmd);
                }
            }
		} else if (args.length == 2) {
            if ((args[0].equals("exempt") || args[0].equals("give") || args[0].equals("spawner") || args[0].equals("tier")) && sender.hasPermission("randomitems2." + args[0])) {
				complete.addAll(ImmutableList.copyOf(this.instance.getServer().getOnlinePlayers().stream().map((Player player) -> player.getName()).toArray(String[]::new)));
			}
		} else if (args.length == 3) {
			if (args[0].equals("give") && sender.hasPermission("randomitems2." + args[0])) {
				complete.addAll(this.customItems.keySet());
			} else if (args[0].equals("spawner") && sender.hasPermission("randomitems2." + args[0])) {
				EntityType[] entities = EntityType.values();
				List<String> entityNames = Lists.newArrayList(ImmutableList.copyOf(entities).stream().map((EntityType entity) -> entity.name().toLowerCase()).toArray(String[]::new));
				entityNames.addAll(Lists.newArrayList(ImmutableList.copyOf(entities).stream().map((EntityType entity) -> entity.name().toLowerCase() + "_minecart").toArray(String[]::new)));
				complete.addAll(entityNames);
			}
		}
		
		List<String> completer = new ArrayList<String>();
		StringUtil.copyPartialMatches(args[args.length - 1], complete, completer);
		return completer;
	}
}
