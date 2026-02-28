package com.houdert6.ri2.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.houdert6.ri2.data.RandomShearsBehavior;
import com.houdert6.ri2.data.itemtiers.RandomItem;
import com.houdert6.ri2.data.itemtiers.SingleMaterialTierUpRequirement;
import com.houdert6.ri2.data.itemtiers.TierUpRequirement;
import com.houdert6.ri2.plugins.papi.PlaceholderAPICompat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Container;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.entity.minecart.SpawnerMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.event.player.PlayerRespawnEvent.RespawnReason;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTables;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.common.collect.ImmutableList;

import com.houdert6.ri2.RandomItems2;

import static com.houdert6.ri2.data.RandomShearsBehavior.*;

public class RandomItemsEvents implements Listener {
	public RandomItems2 plugin;
	public RandomItemsEvents(RandomItems2 plugin) {
		this.plugin = plugin;
	}
	@EventHandler
	public void onShear(PlayerShearEntityEvent event) {
		Player player = event.getPlayer();
		if (plugin.getConfig().getStringList("worlds").contains(player.getWorld().getName())) {
			ItemStack item = event.getItem();
			if (plugin.getAction(item) instanceof String action && action.equals("randomshears")) {
                RandomShearsBehavior behavior = RandomShearsBehavior.fromConfig(plugin.getConfig().getString("random-shears-behavior", "damage-spammable"), RandomShearsBehavior.SPAMMABLE);
                if (behavior == DISABLED) {
                    return; // Feature not enabled in config
                }
                if (plugin.playerDataManager() == null) {
                    return; // Player data manager not present
                }
				event.setCancelled(true);
                NamespacedKey cooldownKey = new NamespacedKey(plugin, "random_shears_cooldown");
                if (behavior == DAMAGE && event.getEntity() instanceof LivingEntity living && living.getNoDamageTicks() > 0) {
                    return; // Random shears aren't in spammable mode and the entity is under no damage ticks
                }
                if (behavior == COOLDOWN && event.getEntity().getPersistentDataContainer().getOrDefault(cooldownKey, PersistentDataType.LONG, 0L) > System.currentTimeMillis()) {
                    return; // On cooldown
                }
				ItemStack ritem = this.plugin.getRandomItem(plugin.itemTiers().itemsInTier(plugin.playerDataManager().getTier(player)), player);
				player.getWorld().dropItem(event.getEntity().getLocation(), ritem);
				if (event.getEntity() instanceof LivingEntity living && behavior.isDamage()) {
					living.damage(2, player);
				} else if (behavior == SHEAR) {
                    switch (event.getEntity()) {
                        case Sheep sheep -> sheep.setSheared(true);
                        case Bogged bogged -> bogged.setSheared(true);
                        case Snowman snowman -> snowman.setDerp(true);
                        default -> event.setCancelled(false); // There is a Paper API for shearing mooshrooms but that just acts like a normal shear anyway and this is a shear event, so it's simpler to uncancel the event
                    }
                } else if (behavior == COOLDOWN) {
                    int cooldown = plugin.getConfig().getInt("random-shears-cooldown");
                    if (cooldown != 0) {
                        event.getEntity().getPersistentDataContainer().set(cooldownKey, PersistentDataType.LONG, System.currentTimeMillis() + (cooldown * 50L)); // 50ms per tick
                    }
                }
				player.getWorld().playSound(event.getEntity().getLocation(), Sound.ENTITY_ENDER_EYE_DEATH, 10, 1);
			}
		}
	}
	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		if (event.getBlock().getType() == Material.SPAWNER) { // Randomized spawners work outside of RandomItems worlds
			PersistentDataContainer cont = event.getItemInHand().getItemMeta().getPersistentDataContainer();
			if (cont.has(new NamespacedKey(plugin, "spawner"), PersistentDataType.STRING)) {
				EntityType entity = EntityType.valueOf(cont.get(new NamespacedKey(plugin, "spawner"), PersistentDataType.STRING));
				CreatureSpawner state = (CreatureSpawner) event.getBlock().getState();
				new BukkitRunnable() {
					@Override
					public void run() {
						state.setSpawnedType(entity);
						state.update();
					}
				}.runTaskLater(this.plugin, 1);
			} else if (cont.has(new NamespacedKey(plugin, "multispawner"), PersistentDataType.LIST.dataContainers())) {
				List<PersistentDataContainer> multiSpawn = cont.get(new NamespacedKey(plugin, "multispawner"), PersistentDataType.LIST.dataContainers());
				EntityType[] toSpawn = new EntityType[multiSpawn.size()];
				for (int i = 0; i < multiSpawn.size(); i++) {
					// multiSpawn is basically just an array of "spawner" tag containers
					toSpawn[i] = EntityType.valueOf(multiSpawn.get(i).get(new NamespacedKey(plugin, "spawner"), PersistentDataType.STRING));
				}
                CreatureSpawner state = (CreatureSpawner) event.getBlock().getState();
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        state.setSpawnedType(toSpawn[0]); // Will default to spawning pigs for the first spawn unless specified explicitly
                        for (EntityType type : toSpawn) {
                            if (type.getEntityClass() == null) {
                                plugin.getLogger().warning("Cannot add spawn for entity type " + type.getKey().getKey() + " because the type has no entity class");
                                continue;
                            }
                            EntitySnapshot snapshot = event.getBlock().getWorld().createEntity(event.getBlock().getLocation(), type.getEntityClass()).createSnapshot();
                            if (snapshot == null) {
                                plugin.getLogger().warning("Cannot add spawn for entity type " + type.getKey().getKey() + " because an entity snapshot could not be created");
                                continue;
                            }
                            state.addPotentialSpawn(snapshot, 1, null);
                            state.update();
                        }
                    }
                }.runTaskLater(this.plugin, 1);
			}
		} else {
			ItemMeta meta = event.getItemInHand().getItemMeta();
			if (meta.hasDisplayName() && meta.getDisplayName().trim().startsWith("§3§oLoot Container")) {
				if (event.getBlock().getState() instanceof Container) {
					new BukkitRunnable() {
						@Override
						public void run() {
							Container block = (Container) event.getBlock().getState();
							LootTables.DESERT_PYRAMID.getLootTable().fillInventory(block.getInventory(), RandomItemsEvents.this.plugin.random, new LootContext.Builder(event.getBlock().getLocation()).build());
							block.update();
						}
					}.runTaskLater(this.plugin, 1);
				}
			}
		}
	}
	@EventHandler
	public void onFishCaught(PlayerFishEvent event) {
		if (event.getState() == State.CAUGHT_FISH) {
			Player player = event.getPlayer();
			ItemStack fishingrod = player.getInventory().getItem(EquipmentSlot.HAND);
			if (fishingrod.getType() != Material.FISHING_ROD) {
				fishingrod = player.getInventory().getItem(EquipmentSlot.OFF_HAND);
			}
			if (plugin.getConfig().getStringList("worlds").contains(player.getWorld().getName())) {
                if (event.getCaught() != null && event.getCaught() instanceof Item caughtItem && plugin.playerDataManager() != null) {
                    if (plugin.getAction(fishingrod) instanceof String action && action.equals("randomfishingrod")) {
                        ArrayList<RandomItem> items = new ArrayList<>(plugin.itemTiers().itemsInTier(plugin.playerDataManager().getTier(player)));
                        for (int i = 0; i < 50; i++) {
                            items.addAll(ImmutableList.of(RandomItem.of(Material.OAK_LOG), RandomItem.of(Material.SPRUCE_LOG), RandomItem.of(Material.BIRCH_LOG), RandomItem.of(Material.JUNGLE_LOG), RandomItem.of(Material.ACACIA_LOG), RandomItem.of(Material.DARK_OAK_LOG), RandomItem.of(Material.IRON_INGOT), RandomItem.of(Material.GOLD_INGOT), RandomItem.of(Material.DIAMOND)));
                        }
                        ItemStack randomitem = this.plugin.getRandomItem(items, player);
                        randomitem.setAmount(this.plugin.random.nextInt(45) + 5);
                        caughtItem.setItemStack(randomitem);
                        player.getWorld().createExplosion(event.getHook().getLocation().add(0, 1, 0), 100f, true);
                    } else if (plugin.getAction(fishingrod) instanceof String action && action.equals("chillfishingrod")) {
                        // A chill fishing rod is a random fishing rod that doesn't affect generated item types, stack sizes, and doesn't explode
                        ItemStack randomitem = this.plugin.getRandomItem(this.plugin.itemTiers().itemsInTier(this.plugin.playerDataManager().getTier(player)), player);
                        caughtItem.setItemStack(randomitem);
                    }
                }
			}
		}
	}
	/**
	 * This event prevents anvils in RandomItems worlds from reducing the level of an enchantment on an item if the book contains an enchantment level over the maximum level for that particular item. Basically, allows the level 10 random enchanted books to actually work
	 */
	@EventHandler
	public void onAnvilUse(PrepareAnvilEvent event) {
		//plugin.getLogger().log(Level.INFO, "Reached 1");
		Location location = event.getInventory().getLocation();
		if (location != null && this.plugin.getConfig().getStringList("worlds").contains(location.getWorld().getName())) {
			//plugin.getLogger().log(Level.INFO, "Reached 2");
			ItemStack first = event.getInventory().getItem(0);
			ItemStack second = event.getInventory().getItem(1);
			if (second.getType() == Material.ENCHANTED_BOOK) {
				//plugin.getLogger().log(Level.INFO, "Reached 3");
				EnchantmentStorageMeta meta = (EnchantmentStorageMeta) second.getItemMeta();
				ItemStack result = event.getResult();
				if (result != null) {
					//plugin.getLogger().log(Level.INFO, "Reached 4");
					boolean changed = false;
					ItemMeta resultMeta = result.getItemMeta();
					for (Enchantment ench : resultMeta.getEnchants().keySet()) {
						if (meta.hasStoredEnchant(ench) && resultMeta.getEnchantLevel(ench) < meta.getStoredEnchantLevel(ench) && !first.containsEnchantment(ench)) {
							resultMeta.removeEnchant(ench);
							resultMeta.addEnchant(ench, meta.getStoredEnchantLevel(ench), true);
							changed = true;
						}
					}
					if (changed) {
						//plugin.getLogger().log(Level.INFO, "Reached 5");
						result.setItemMeta(resultMeta);
						event.setResult(result);
					}
				}
			}
		}
	}
	@EventHandler
	public void onEntityPlace(EntityPlaceEvent event) {
		if (event.getEntityType() == EntityType.HOPPER_MINECART) {
			PersistentDataContainer cont = event.getPlayer().getInventory().getItemInMainHand().getItemMeta().getPersistentDataContainer();
			if (cont.has(new NamespacedKey(plugin, "spawner"), PersistentDataType.STRING)) {
				event.setCancelled(true);
				if (event.getPlayer().getGameMode() == GameMode.SURVIVAL || event.getPlayer().getGameMode() == GameMode.ADVENTURE) {
					if (event.getPlayer().getInventory().getItemInMainHand().getAmount() == 1) {
						event.getPlayer().getInventory().setItemInMainHand(new ItemStack(Material.AIR));
					} else {
						event.getPlayer().getInventory().getItemInMainHand().setAmount(event.getPlayer().getInventory().getItemInMainHand().getAmount() - 1);
					}
				}
				EntityType entity = EntityType.valueOf(cont.get(new NamespacedKey(plugin, "spawner"), PersistentDataType.STRING));
				Entity espawner = event.getBlock().getWorld().spawnEntity(event.getBlock().getLocation(), EntityType.SPAWNER_MINECART);
				if (espawner instanceof SpawnerMinecart) {
					SpawnerMinecart spawner = (SpawnerMinecart)espawner;
					new BukkitRunnable() {
						@Override
						public void run() {
							spawner.setSpawnedType(entity);
						}
					}.runTaskLater(this.plugin, 1);
				}
			} else if (cont.has(new NamespacedKey(plugin, "multispawner"), PersistentDataType.LIST.dataContainers())) {
				event.setCancelled(true);
				if (event.getPlayer().getGameMode() == GameMode.SURVIVAL || event.getPlayer().getGameMode() == GameMode.ADVENTURE) {
					if (event.getPlayer().getInventory().getItemInMainHand().getAmount() == 1) {
						event.getPlayer().getInventory().setItemInMainHand(new ItemStack(Material.AIR));
					} else {
						event.getPlayer().getInventory().getItemInMainHand().setAmount(event.getPlayer().getInventory().getItemInMainHand().getAmount() - 1);
					}
				}
				List<PersistentDataContainer> multiSpawn = cont.get(new NamespacedKey(plugin, "multispawner"), PersistentDataType.LIST.dataContainers());
				EntityType[] toSpawn = new EntityType[multiSpawn.size()];
				for (int i = 0; i < multiSpawn.size(); i++) {
					// multiSpawn is basically just an array of "spawner" tag containers
					toSpawn[i] = EntityType.valueOf(multiSpawn.get(i).get(new NamespacedKey(plugin, "spawner"), PersistentDataType.STRING));
				}
                Entity espawner = event.getBlock().getWorld().spawnEntity(event.getBlock().getLocation(), EntityType.SPAWNER_MINECART);
                if (espawner instanceof SpawnerMinecart) {
                    SpawnerMinecart spawner = (SpawnerMinecart)espawner;
                    spawner.setSpawnedType(toSpawn[0]);
                    for (EntityType type : toSpawn) {
                        if (type.getEntityClass() == null) {
                            plugin.getLogger().warning("Cannot add spawn for entity type " + type.getKey().getKey() + " because the type has no entity class");
                            continue;
                        }
                        EntitySnapshot snapshot = event.getBlock().getWorld().createEntity(event.getBlock().getLocation(), type.getEntityClass()).createSnapshot();
                        if (snapshot == null) {
                            plugin.getLogger().warning("Cannot add spawn for entity type " + type.getKey().getKey() + " because an entity snapshot could not be created");
                            continue;
                        }
                        spawner.addPotentialSpawn(snapshot, 1, null);
                    }
                }
			}
		} else if (event.getEntityType() == EntityType.MINECART && (event.getPlayer().getInventory().getItemInMainHand().getItemMeta().getDisplayName().equals("§bMinecart of Randomness") || event.getPlayer().getInventory().getItemInMainHand().getItemMeta().getDisplayName().equals("Minecart of Randomness"))) {
			Entity eminecart = event.getEntity();
			if (eminecart instanceof Minecart) {
				ItemStack rand;
                Material[] materials = Material.values();
				do {
					rand = new ItemStack(materials[plugin.random.nextInt(materials.length)]);
				} while (!rand.getType().isBlock());
				((Minecart)eminecart).setDisplayBlockData(rand.getType().createBlockData());
			}
		}
	}
	@EventHandler
	public void onTeleport(PlayerTeleportEvent event) {
		if (event.getCause() == TeleportCause.END_PORTAL && plugin.getConfig().getStringList("worlds").contains(event.getPlayer().getWorld().getName())) {
			World world = this.plugin.getServer().getWorld(this.plugin.getConfig().getString("endworld", ""));
			if (world != null) {
				event.getPlayer().teleport(world.getSpawnLocation(), TeleportCause.NETHER_PORTAL);
				event.setCancelled(true);
			}
		}
	}
	@EventHandler
	public void onRespawn(PlayerRespawnEvent event) {
		if (event.getRespawnReason() == RespawnReason.END_PORTAL) {
			if (event.getPlayer().getWorld().getName().equals(this.plugin.getConfig().getString("endworld", ""))) {
				World world = this.plugin.getServer().getWorld(this.plugin.getConfig().getString("retworld", ""));
				if (world != null) {
					new BukkitRunnable() {
						@Override
						public void run() {
							event.getPlayer().teleport(world.getSpawnLocation(), TeleportCause.NETHER_PORTAL);
						}
					}.runTaskLater(this.plugin, 1);
				}
			} 
		}
	}

    @EventHandler
    public void onInteractWithEntity(PlayerInteractEntityEvent event) {
        // Check if the player right clicked a tier-up interaction
        if (event.getRightClicked() instanceof Interaction && event.getRightClicked().getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "").equals("tierup")) {
            if (plugin.playerDataManager() != null) {
                Map<TierUpRequirement, Integer> tierUpReqs = plugin.itemTiers().getNextTierRequirements(plugin.playerDataManager().getTier(event.getPlayer()));
                if (tierUpReqs == null) {
                    return; // No way to progress from this tier
                }
                ItemStack heldItem = event.getPlayer().getInventory().getItemInMainHand();
                for (Map.Entry<TierUpRequirement, Integer> req : tierUpReqs.entrySet()) {
                    // Check if the player's current held item may be used to progress
                    if (req.getKey().matches(heldItem.getType())) {
                        int reqCount = req.getValue();
                        int currentCount = plugin.playerDataManager().getProgressToNextTier(event.getPlayer(), req.getKey());
                        int itemsToTake = reqCount - currentCount;
                        if (itemsToTake > 0) {
                            Material itemType = heldItem.getType();
                            // There's still progress to be had with this item
                            int deposit;
                            if (itemsToTake >= heldItem.getAmount()) {
                                deposit = heldItem.getAmount();
                                plugin.playerDataManager().progress(event.getPlayer(), req.getKey(), deposit);
                                event.getPlayer().getInventory().setItemInMainHand(null);
                            } else {
                                deposit = itemsToTake;
                                plugin.playerDataManager().progress(event.getPlayer(), req.getKey(), deposit);
                                heldItem.setAmount(heldItem.getAmount() - itemsToTake);
                                event.getPlayer().getInventory().setItemInMainHand(heldItem);
                            }
                            // Play a deposit sound
                            event.getPlayer().playSound(event.getRightClicked().getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 1, 1);
                            // Deposit tooltip
                            String tooltipTemplate = plugin.getMessagesConfig().getString("deposit-actionbar", "&aDeposited %amount%x &2%item%");
                            event.getPlayer().sendActionBar(SingleMaterialTierUpRequirement.itemPlaceholder(tooltipTemplate.replaceAll("%amount%", Integer.toString(deposit)), template -> PlaceholderAPICompat.instance(plugin.getServer()).fillInPlaceholders(template, event.getPlayer()), "%item%", itemType));
                            // Check for a tier up and return
                            checkTierUp(event.getPlayer(), tierUpReqs);
                            return;
                        }
                    }
                }
                // Player hasn't deposited a valid item so play a sound and send a help msg
                event.getPlayer().playSound(event.getRightClicked().getLocation(), Sound.ENTITY_ENDER_EYE_DEATH, 1, 1);
                sendTierUpHelp(event.getPlayer());
            }
        }
    }


    private void checkTierUp(Player player, Map<TierUpRequirement, Integer> tierUpReqs) {
        boolean hasTieredUp = true;
        for (Map.Entry<TierUpRequirement, Integer> req : tierUpReqs.entrySet()) {
            if (plugin.playerDataManager().getProgressToNextTier(player, req.getKey()) < req.getValue()) {
                hasTieredUp = false; // Tier up req > player's progress
            }
        }
        if (hasTieredUp) {
            int currentTierData = plugin.playerDataManager().getTier(player);
            int newTierData = currentTierData + 1;
            plugin.playerDataManager().setTier(player, newTierData); // Set the player to the new tier
            // These below variables are incremented because they're always saved as 0-indexed but displayed as 1-indexed. They're final because they're captured by a lambda below
            final int currentTier = currentTierData + 1;
            final int newTier = newTierData + 1;
            // Play a sound to congratulate the player
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 10, 1.5f);
            // Send a title and subtitle
            String title = plugin.getMessagesConfig().getString("tier-up-title", "&9New Tier!");
            title = title.replace("%oldtier%", Integer.toString(currentTier)).replace("%newtier%", Integer.toString(newTier));
            title = PlaceholderAPICompat.instance(plugin.getServer()).fillInPlaceholders(title, player);
            String subtitle = plugin.getMessagesConfig().getString("tier-up-subtitle", "&bYou're now receiving &9Tier %newtier% &bitems!");
            subtitle = subtitle.replace("%oldtier%", Integer.toString(currentTier)).replace("%newtier%", Integer.toString(newTier));
            subtitle = PlaceholderAPICompat.instance(plugin.getServer()).fillInPlaceholders(subtitle, player);
            player.showTitle(Title.title(LegacyComponentSerializer.legacyAmpersand().deserialize(title), LegacyComponentSerializer.legacyAmpersand().deserialize(subtitle)));
            // Send a chat msg
            player.sendMessage(plugin.getMsg("tier-up-chat", "You've tiered up to &9%newtier%&b!", player, template -> template.replace("%oldtier%", Integer.toString(currentTier)).replace("%newtier%", Integer.toString(newTier))));
            // Tell players how to get to their next tier
            sendTierUpHelp(player);
        }
    }
    // A message listing tier up requirements
    private void sendTierUpHelp(Player player) {
        int tier = plugin.playerDataManager().getTier(player);
        Map<TierUpRequirement, Integer> tierUpReqs = plugin.itemTiers().getNextTierRequirements(tier);
        if (tierUpReqs != null) {
            player.sendMessage(plugin.getMsg("tier-up-requirements-header", "To tier up to Tier %newtier%, you need to deposit:", player, template -> template.replace("%oldtier%", Integer.toString(tier + 1)).replace("%newtier%", Integer.toString(tier + 2))));
            // Send each line
            for (Map.Entry<TierUpRequirement, Integer> req : tierUpReqs.entrySet()) {
                String template = plugin.getMessagesConfig().getString("tier-up-requirements-line", "&c » &a%deposited%&8/&b%required% &6%item%");
                int deposited = plugin.playerDataManager().getProgressToNextTier(player, req.getKey());
                template = template.replace("%deposited%", Integer.toString(deposited)).replace("%required%", Integer.toString(req.getValue())).replace("%remaining%", Integer.toString(Math.max(req.getValue() - deposited, 0)));
                player.sendMessage(req.getKey().fillInPlaceholder(template, templateForPAPI -> PlaceholderAPICompat.instance(plugin.getServer()).fillInPlaceholders(templateForPAPI, player), "%item%"));
            }
        }
    }
}
