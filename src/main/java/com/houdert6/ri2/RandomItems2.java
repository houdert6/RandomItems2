package com.houdert6.ri2;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;

import com.google.common.collect.ImmutableList;
import com.houdert6.ri2.data.PlayerData;
import com.houdert6.ri2.data.itemtiers.ItemTiers;
import com.houdert6.ri2.data.PlayerDataManager;
import com.houdert6.ri2.data.itemtiers.RandomItem;
import com.houdert6.ri2.plugins.papi.PlaceholderAPICompat;
import com.houdert6.ri2.plugins.pkscrolls.PKScrollsCompat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.OminousItemSpawner;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;

import com.google.common.collect.Lists;

import com.houdert6.ri2.commands.CommandRItem;
import com.houdert6.ri2.events.RandomItemsEvents;
import org.bukkit.scheduler.BukkitTask;

public class RandomItems2 extends JavaPlugin {
    public static final List<Material> SWORDS = Lists.newArrayList(Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD);
    public static final List<Material> AXES = Lists.newArrayList(Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE);
    public static final List<Material> HOES = Lists.newArrayList(Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE, Material.GOLDEN_HOE, Material.DIAMOND_HOE, Material.NETHERITE_HOE);
    public static final List<Material> PICKAXES = Lists.newArrayList(Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE, Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE);
    public static final List<Material> SHOVELS = Lists.newArrayList(Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL, Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL);
    public static final List<Material> HELMETS = Lists.newArrayList(Material.LEATHER_HELMET, Material.CHAINMAIL_HELMET, Material.IRON_HELMET, Material.GOLDEN_HELMET, Material.DIAMOND_HELMET, Material.NETHERITE_HELMET, Material.TURTLE_HELMET);
    public static final List<Material> CHESTPLATES = Lists.newArrayList(Material.LEATHER_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE, Material.IRON_CHESTPLATE, Material.GOLDEN_CHESTPLATE, Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE);
    public static final List<Material> LEGGINGS = Lists.newArrayList(Material.LEATHER_LEGGINGS, Material.CHAINMAIL_LEGGINGS, Material.IRON_LEGGINGS, Material.GOLDEN_LEGGINGS, Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS);
    public static final List<Material> BOOTS = Lists.newArrayList(Material.LEATHER_HELMET, Material.CHAINMAIL_HELMET, Material.IRON_HELMET, Material.GOLDEN_HELMET, Material.DIAMOND_HELMET, Material.NETHERITE_HELMET);

	public Component prefix = LegacyComponentSerializer.legacySection().deserialize("§7[§cRandomItems2§7] §b");
	public Random random = new Random();
	public BukkitTask currentItemSpawnTask = null;
    private FileConfiguration messagesConfig;
    private int currentItemSpawnTaskId = 0;
    private ItemTiers itemTiers;
    private PlayerDataManager playerData;

	@Override
	public void onEnable() {
		this.saveConfigYML();
        ConfigurationSerialization.registerClass(PlayerData.class);
		this.getServer().getPluginManager().registerEvents(new RandomItemsEvents(this), this);
		this.getCommand("randomitems").setExecutor(new CommandRItem(this));
		this.getCommand("randomitems").setTabCompleter(new CommandRItem(this));
		this.reloadConfigRI();
        try {
            reloadMessagesConfig();
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Failed to load messages.yml", e);
        }
	}
	@Override
	public void onLoad() {
		super.onLoad();
	}
	@Override
	public void onDisable() {
		if (this.currentItemSpawnTask != null) {
			this.currentItemSpawnTask.cancel();
		}
	}

    /**
     * Helper method to decide when to next spawn items
     */
    private int nextItemTime() {
        int time1 = getConfig().getInt("min-give-time", 200);
        int time2 = getConfig().getInt("max-give-time", 400);
        return random.nextInt(Math.min(time1, time2), Math.max(time1, time2) + 1);
    }
    private int nextItemPopTime() {
        int time1 = getConfig().getInt("min-pop-time", 100);
        int time2 = getConfig().getInt("max-pop-time", 140);
        return random.nextInt(Math.min(time1, time2), Math.max(time1, time2) + 1);
    }
	
	public boolean reloadConfigRI() {
		if (this.currentItemSpawnTask != null) {
			this.currentItemSpawnTask.cancel();
		}
        itemTiers = new ItemTiers(getConfig(), getServer().getPluginManager().isPluginEnabled("ProjectKorraScrolls"), getLogger());
        reloadPlayerData();
		currentItemSpawnTask = getServer().getScheduler().runTaskLater(this, this.new ItemRunnable(++currentItemSpawnTaskId), nextItemTime());
        return itemTiers.hadWarnings;
	}

    /**
     * Reloads messages.yml, creating the file if needed, and throws an exception if something goes wrong.
     */
    public void reloadMessagesConfig() throws Exception {
        this.messagesConfig = new YamlConfiguration();
        File messagesConfigFile = new File(getDataFolder(), "messages.yml");
        if (!messagesConfigFile.exists()) {
            saveResource("messages.yml", false);
        }
        messagesConfig.load(messagesConfigFile);
        prefix = LegacyComponentSerializer.legacyAmpersand().deserialize(PlaceholderAPICompat.instance(getServer()).fillInPlaceholders(messagesConfig.getString("prefix", "&7[&cRandomItems2&7] &b"), null));
        if (!prefix.children().isEmpty()) {
            prefix = prefix.style(prefix.children().getLast().style()); // So that the color code at the end of the prefix is used
        }
    }

    public void reloadPlayerData() {
        playerData = null;
        File dataFolder = new File(getDataFolder(), "data");
        if (!dataFolder.exists()) {
            try {
                Files.createDirectory(dataFolder.toPath());
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "error reloading player data", e);
                return;
            }
        }
        File playerDataFile = new File(dataFolder, "playerdata.yml");
        FileConfiguration playerData = new YamlConfiguration();
        if (playerDataFile.exists()) {
            try {
                playerData.load(playerDataFile);
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "error reloading player data", e);
                playerData = null;
            }
        }
        this.playerData = playerData == null ? null : new PlayerDataManager(playerData, playerDataFile);
    }
    public FileConfiguration getMessagesConfig() {
        return this.messagesConfig;
    }
	
	public void saveConfigYML() {
		this.saveDefaultConfig();
	}
    public PlayerDataManager playerDataManager() {
        return playerData;
    }
    public ItemTiers itemTiers() {
        return itemTiers;
    }
	/**
	 * Checks if the given item can be enchanted
	 * @param item The item to check
	 * @return If the item has an entry in the RandomItems valid enchantment lists (any enchantable vanilla item)
	 */
	public static boolean isTool(Material item) {
		return SWORDS.contains(item) || AXES.contains(item) || HOES.contains(item) || PICKAXES.contains(item) || SHOVELS.contains(item) || HELMETS.contains(item) || CHESTPLATES.contains(item) || LEGGINGS.contains(item) || BOOTS.contains(item) || item == Material.BOW || item == Material.COMPASS || item == Material.CROSSBOW || item == Material.FISHING_ROD || item == Material.SHEARS || item == Material.ELYTRA || item == Material.SHIELD || item == Material.TRIDENT || item == Material.FLINT_AND_STEEL;
	}

    /**
     * Enchants a stack
     * @param stack the item stack
     * @param chance percentage from 1-100 representing the chance of the item being enchanted
     * @param tool whether to validate it being a tool
     * @param doValidTypes enchants the item with only valid types of enchantments
     * @param doValidLevels enchants the item with only valid enchantment levels. Ignores `maxLevel`
     * @param filterCurses makes sure the item isn't enchanted with one of the two curses
     * @param maxLevel maximum level of the enchantment
     * @return {@code true} if an enchantment was added
     */
    public boolean enchantStack(ItemStack stack, int chance, boolean tool, boolean doValidTypes, boolean doValidLevels, boolean filterCurses, int maxLevel) {
        if (random.nextInt(100) < chance) {
            // Either not checking for tools or it is a tool
            if (!tool || isTool(stack.getType())) {
                ItemMeta meta = stack.getItemMeta();
                if (doValidTypes) {
                    List<Enchantment> enchants = Lists.newArrayList(Registry.ENCHANTMENT.iterator());
                    Collections.shuffle(enchants);
                    for (Enchantment enchant : enchants) {
                        if (filterCurses && enchant.isCursed()) {
                            continue;
                        }
                        if (enchant.canEnchantItem(stack)) {
                            meta.addEnchant(enchant, this.random.nextInt(doValidLevels ? enchant.getMaxLevel() : maxLevel) + 1, true);
                            stack.setItemMeta(meta);
                            return true;
                        }
                    }
                } else {
                    List<Enchantment> enchants = Lists.newArrayList(Registry.ENCHANTMENT.iterator());
                    Enchantment enchant = enchants.get(this.random.nextInt(enchants.size()));
                    meta.addEnchant(enchant, this.random.nextInt(doValidLevels ? enchant.getMaxLevel() : maxLevel) + 1, true);
                    stack.setItemMeta(meta);
                    return true;
                }
            }
        }
        return false;
    }
    private boolean idListContainsNamespacedKey(List<String> ids, NamespacedKey searchKey) {
        for (String id : ids) {
            NamespacedKey idKey = NamespacedKey.fromString(id);
            if (idKey == null) {
                getLogger().log(Level.WARNING, "id {0} is not a valid id", id);
            } else {
                if (idKey.getNamespace().equals(searchKey.getNamespace()) && idKey.getKey().equals(searchKey.getKey())) {
                    return true;
                }
            }
        }
        return false;
    }
    /**
     * Constructs a component from a message in messages.yml, prefixed with {@link #prefix}
     * @param config The config path in messages.yml
     * @param def A default template if none is found in messages.yml
     * @param player A player used for PlaceholderAPI placeholders
     * @param messageStringProcessor A preprocessor for message templates. Can be used to alter the template from the config before it's converted to a component
     */
    public Component getMsg(String config, String def, Player player, Function<String, String> messageStringProcessor) {
        String template = getMessagesConfig().getString(config, def);
        if (messageStringProcessor != null) {
            template = messageStringProcessor.apply(template);
        }
        template = PlaceholderAPICompat.instance(getServer()).fillInPlaceholders(template, player);
        return prefix.append(LegacyComponentSerializer.legacyAmpersand().deserialize(template));
    }
	/**
	 * Generates a random item
	 * @param randomItemTypes A list of possible random item types to generate from
	 * @return A random item
	 */
	public ItemStack getRandomItem(List<RandomItem> randomItemTypes) {
        return getRandomItem(randomItemTypes, null, true);
    }
    /**
     * Generates a random item
     * @param randomItemTypes A list of possible random item types to generate from
     * @param player The player who this random item is for. Used only for scroll generation by ProjectKorraScrolls as pretty much all item data in RandomItems2 is determined by {@link RandomItem}
     * @return A random item
     */
    public ItemStack getRandomItem(List<RandomItem> randomItemTypes, Player player) {
        return getRandomItem(randomItemTypes, player, true);
    }

    /**
     * Generates a random item
     * @param randomItemTypes A list of possible random item types to generate from
     * @param player The player who this random item is for. Used only for scroll generation by ProjectKorraScrolls as pretty much all item data in RandomItems2 is determined by {@link RandomItem}, so this is pretty safe to leave as null
     * @param fillShulkerBoxes Whether to fill shulker boxes, if the configuration allows it (mainly just used to avoid recursively filling shulkers until a stack overflow)
     * @return A random item
     */
    private ItemStack getRandomItem(List<RandomItem> randomItemTypes, Player player, boolean fillShulkerBoxes) {
        boolean includeVanillaMaterials = randomItemTypes.stream().anyMatch(RandomItem::isAll);
		Material[] items = Material.values();
		Enchantment[] enchants = Enchantment.values();
		EntityType[] entities = EntityType.values();
		PotionType[] potions = PotionType.values();
		List<Material> edit = Lists.newArrayList(items);
        if (includeVanillaMaterials) {
            // Exclude the excludes list in the config
            for (String exclude : getConfig().getStringList("all-excludes")) {
                NamespacedKey excludeKey = NamespacedKey.fromString(exclude);
                if (excludeKey != null) {
                    if (!edit.removeIf(m -> m.getKey().getNamespace().equals(excludeKey.getNamespace()) && m.getKey().getKey().equals(excludeKey.getKey()))) {
                        getLogger().log(Level.WARNING, "exclude key {0} did not match any items", exclude);
                    }
                } else {
                    getLogger().log(Level.WARNING, "exclude key {0} is invalid", exclude);
                }
            }
            // Edit the item list to add duplicate items for things with variants
            for (int i = 1; i < enchants.length; i++) { // int i = 1 because the list already includes one book
                edit.add(Material.ENCHANTED_BOOK);
            }
            for (int i = 1 + Set.copyOf(getConfig().getStringList("default-spawner-excludes")).size(); i < entities.length; i++) { // int i = 1 because the list already includes one spawner, and also accounting for excluded spawners
                edit.add(Material.SPAWNER);
                edit.add(Material.HOPPER_MINECART); // Used as a placeholder for spawner minecarts. Hopper minecarts themselves are still obtainable
            }
            for (int i = 1; i < potions.length; i++) {
                edit.add(Material.POTION);
                edit.add(Material.SPLASH_POTION);
                edit.add(Material.LINGERING_POTION);
                edit.add(Material.TIPPED_ARROW);
                edit.add(Material.SUSPICIOUS_STEW);
            }
        }

		// Generate initial item
		RandomItem item = randomItemTypes.get(this.random.nextInt(randomItemTypes.size())).frozen();
        // If the item is of type "all", then replace it with one from the "edit" list
        boolean all = item.isAll();
        if (all) {
            if (!includeVanillaMaterials) throw new AssertionError("Somehow an \"all\" item was picked when none were detected");
            item = new RandomItem(edit.get(random.nextInt(edit.size())), null, item.enchantTier(), item.entities(), item.minEntities(), item.maxEntities(), item.minCount(), item.maxCount(), false, false);
        }
		// Ensure the material is an item
		while (!item.type().isItem()) {
            if (all) {
                // Redo picking from "edit" if the item picked is all
                item = new RandomItem(edit.get(random.nextInt(edit.size())), null, item.enchantTier(), item.entities(), item.minEntities(), item.maxEntities(), item.minCount(), item.maxCount(), false, false);
            } else {
                // Redo picking from the random item set
                item = randomItemTypes.get(this.random.nextInt(randomItemTypes.size())).frozen();
            }
		}
        ItemStack itemstack;
        if (item.isScroll() && player == null) {
            while (item.isScroll() || !item.type().isItem()) {
                // Redo picking from the random item set until the
                item = randomItemTypes.get(this.random.nextInt(randomItemTypes.size())).frozen();
            }
        }
        if (item.isScroll()) {
            itemstack = PKScrollsCompat.instance(getServer()).getRandomScrollForPlayer(player);
        } else {
            itemstack = new ItemStack(item.type(), Math.min(this.random.nextInt(item.minCount(), item.maxCount() + 1), item.type().getMaxStackSize()));
        }
		ItemMeta meta = itemstack.getItemMeta();
		// Enchanted books get random enchants here
		if (item.type() == Material.ENCHANTED_BOOK) {
			if (meta instanceof EnchantmentStorageMeta) {
				((EnchantmentStorageMeta) meta).addStoredEnchant(enchants[this.random.nextInt(enchants.length)], random.nextInt(9) + 1, true);

			}
		}
        itemstack.setItemMeta(meta);
        // Enchant based on the configured enchant tier
        switch (item.enchantTier()) {
            case 1 -> enchantStack(itemstack, 45, true, false, false, false, 1);
            case 2 -> enchantStack(itemstack, 35, true, true, true, false, -1);
            case 3 -> enchantStack(itemstack, 25, true, true, false, false, 10);
            case 4 -> enchantStack(itemstack, 25, true, true, false, true, 10);
            case 5 -> {
                if (enchantStack(itemstack, 25, true, true, false, true, 10)) {
                    enchantStack(itemstack, 45, true, true, false, true, 10);
                }
            }
            case 6 -> {
                if (enchantStack(itemstack, 25, true, true, false, true, 10)) {
                    enchantStack(itemstack, 45, true, true, false, true, 10);
                } else if (!isTool(item.type())) {
                    enchantStack(itemstack, 5, false, false, false, true, 5);
                }
            }
            case 7 -> {
                if (enchantStack(itemstack, 25, true, true, false, true, 10)) {
                    enchantStack(itemstack, 45, true, true, false, true, 10);
                } else if (!isTool(item.type())) {
                    enchantStack(itemstack, 15, false, false, false, true, 10);
                }
            }
        }
        meta = itemstack.getItemMeta();
		// This code makes spawners get random entities
		if (item.type() == Material.SPAWNER) {
            int numEntities = random.nextInt(item.minEntities(), item.maxEntities() + 1);
			if (item.entities().size() != 1 && numEntities > 1) {
				ArrayList<EntityType> toSpawn = new ArrayList<>();
				List<PersistentDataContainer> entityData = new ArrayList<>();
				for (int i = 0; i < numEntities; i++) {
                    if (item.entities().isEmpty()) {
                        toSpawn.add(entities[this.random.nextInt(entities.length)]);
                    } else {
                        toSpawn.add(item.entities().get(this.random.nextInt(item.entities().size())));
                    }
					while (item.entities().isEmpty() && idListContainsNamespacedKey(getConfig().getStringList("default-spawner-excludes"), toSpawn.get(i).getKey())) {
                        // If the initially selected random entity is an excluded entity, then change it
                        if (item.entities().isEmpty()) {
                            toSpawn.set(i, entities[this.random.nextInt(entities.length)]);
                        }
					}
					PersistentDataContainer spawnData = meta.getPersistentDataContainer().getAdapterContext().newPersistentDataContainer();
					spawnData.set(NamespacedKey.fromString("spawner", this), PersistentDataType.STRING, toSpawn.get(i).name());
					entityData.add(spawnData);
				}
				meta.getPersistentDataContainer().set(NamespacedKey.fromString("multispawner", this), PersistentDataType.LIST.dataContainers(), entityData);
				meta.setLore(ImmutableList.copyOf(toSpawn.stream().map((type) -> "§7§oSpawns: §c" + type.name()).toArray(String[]::new)));
			} else {
				EntityType toSpawn = entities[this.random.nextInt(entities.length)];
				while (toSpawn == EntityType.WITHER || toSpawn == EntityType.ENDER_DRAGON) {
					toSpawn = entities[this.random.nextInt(entities.length)];
				}
				meta.getPersistentDataContainer().set(NamespacedKey.fromString("spawner", this), PersistentDataType.STRING, toSpawn.name());
				meta.setLore(ImmutableList.copyOf(new String[] {"§7§oSpawns: §c" + toSpawn.name()}));
			}
		}
		if (item.type() == Material.POTION || item.type() == Material.SPLASH_POTION || item.type() == Material.LINGERING_POTION || item.type() == Material.TIPPED_ARROW) {
			PotionType type = potions[this.random.nextInt(potions.length)];
			if (meta instanceof PotionMeta potionMeta) {
				potionMeta.setBasePotionType(type);
			}
		}
		if (item.type() == Material.SUSPICIOUS_STEW) {
			PotionType type = potions[this.random.nextInt(potions.length)];
			boolean upgrade = type.getKey().getKey().startsWith("strong_");
			boolean extend = type.getKey().getKey().startsWith("long_");
			if (meta instanceof SuspiciousStewMeta) {
				SuspiciousStewMeta smeta = (SuspiciousStewMeta) meta;
				if (type.getEffectType() != null) {
					smeta.addCustomEffect(type.getEffectType().createEffect((extend ? 120 : 60) * 20, upgrade ? 40 : 20), true);
				}
			}
		}
        if (meta instanceof BlockStateMeta && !getConfig().getBoolean("disable-random-shulker-boxes", false) && fillShulkerBoxes) {
            BlockState state = ((BlockStateMeta) meta).getBlockState();
            if (state instanceof ShulkerBox) {
                int a = this.random.nextInt(15);
                for (int i = 0; i < a; i++) {
                    ((ShulkerBox) state).getInventory().addItem(this.getRandomItem(randomItemTypes, player, false)); // Avoid recursively filling shulkers
                }
            }
            ((BlockStateMeta) meta).setBlockState(state);
        }
		if (item.type() == Material.HOPPER_MINECART) {
			if (!item.entities().isEmpty()) { // If this should be a spawner minecart
                int numEntities = random.nextInt(item.minEntities(), item.maxEntities() + 1);
				if (item.entities().size() > 1 && numEntities > 1) {
					ArrayList<EntityType> toSpawnList = new ArrayList<>();
					List<PersistentDataContainer> entityData = new ArrayList<>();
                    for (int i = 0; i < numEntities; i++) {
                        toSpawnList.add(item.entities().get(this.random.nextInt(item.entities().size())));
                        while (idListContainsNamespacedKey(getConfig().getStringList("default-spawner-excludes"), toSpawnList.get(i).getKey())) {
                            // If the initially selected random entity is an excluded entity, then change it
                            toSpawnList.set(i, item.entities().get(this.random.nextInt(item.entities().size())));
                        }
						PersistentDataContainer spawnData = meta.getPersistentDataContainer().getAdapterContext().newPersistentDataContainer();
						spawnData.set(NamespacedKey.fromString("spawner", this), PersistentDataType.STRING, toSpawnList.get(i).name());
						entityData.add(spawnData);
					}
					meta.getPersistentDataContainer().set(NamespacedKey.fromString("multispawner", this), PersistentDataType.LIST.dataContainers(), entityData);
					meta.setLore(ImmutableList.copyOf(toSpawnList.stream().map((type) -> "§7§oSpawns: §c" + type.name()).toArray(String[]::new)));
				} else {
                    EntityType toSpawn = item.entities().get(this.random.nextInt(item.entities().size()));
					meta.getPersistentDataContainer().set(NamespacedKey.fromString("spawner", this), PersistentDataType.STRING, toSpawn.name());
					meta.setLore(ImmutableList.copyOf(new String[] {"§7§oSpawns: §c" + toSpawn.name()}));
					meta.setDisplayName("§4Spawner Minecart");
				}
			}
		}
		itemstack.setItemMeta(meta);
		return itemstack;
	}

    /**
     * Gets the action on the specified item
     * @param item The item to get the action of
     * @return The action on the item, or null if there is none
     */
    public String getAction(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().get(new NamespacedKey(this, "action"), PersistentDataType.STRING);
    }
    /**
     * Sets the action on the specified item
     * @param item The item to set the action of
     * @param action The action to put onto the item, or null to remove it
     */
    public void setAction(ItemStack item, String action) {
        if (item == null || !(item.getItemMeta() instanceof ItemMeta meta))
            return;
        if (action == null) {
            meta.getPersistentDataContainer().remove(new NamespacedKey(this, "action"));
        } else {
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "action"), PersistentDataType.STRING, action);
        }
        item.setItemMeta(meta);
    }

	public class ItemRunnable implements Runnable {
        private final int id;
        public ItemRunnable(int id) {
            this.id = id;
        }

		@Override
		public void run() {
            if (id != currentItemSpawnTaskId) {
                return;
            }
            try {
                List<String> worlds = RandomItems2.this.getConfig().getStringList("worlds");
                List<String> exemptions = RandomItems2.this.getConfig().getStringList("exempt");
                for (Player player : RandomItems2.this.getServer().getOnlinePlayers()) {
                    if (!worlds.contains(player.getWorld().getName()) || isExempt(exemptions, player)) {
                        continue;
                    }
                    if (playerData == null)
                        continue;
                    ItemStack item = RandomItems2.this.getRandomItem(itemTiers.itemsInTier(playerData.getTier(player)), player);
                    Location randomSpawnPos = null;
                    for (int tries = 0; tries < 10 && (randomSpawnPos == null || !player.getWorld().getBlockAt(randomSpawnPos).isEmpty()); tries++) {
                        randomSpawnPos = player.getLocation().add(randSign() * random.nextDouble(6), random.nextDouble(4) + 2, randSign() * random.nextDouble(6));
                    }
                    if (player.getWorld().getBlockAt(randomSpawnPos).isEmpty()) {
                        // Found a good spawn point
                        player.getWorld().spawn(randomSpawnPos, OminousItemSpawner.class, spawner -> {
                            spawner.setItem(item);
                            spawner.setSpawnItemAfterTicks(nextItemPopTime());
                        });
                    }
                }
            } finally {
                currentItemSpawnTask = getServer().getScheduler().runTaskLater(RandomItems2.this, RandomItems2.this.new ItemRunnable(id), nextItemTime());
            }
		}

        /**
         * Randomly picks 1 or -1
         */
        private double randSign() {
            return random.nextBoolean() ? -1d : 1d;
        }
        private boolean isExempt(List<String> exemptions, Player p) {
            if (exemptions.contains(p.getUniqueId().toString())) {
                return true;
            }
            for (String exemption : exemptions) {
                if (exemption.equalsIgnoreCase(p.getName())) {
                    return true;
                }
            }
            return false;
        }
	}
}
