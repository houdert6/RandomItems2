# RandomItems2
RandomItems2 is a highly configurable random items plugin

## How it works
Every few seconds, a random item will appear in the air (in the form of an [Ominous Item Spawner](https://minecraft.wiki/w/Ominous_Item_Spawner)).
Every possible random item is decided by the player's current Random Item tier. Each player starts at Tier 1, but can progress to higher tiers by depositing resources into a **Tier-Up Zone** created with `/ri2 tierupzone`.

## Commands
|     Command     |        Permission       |                         Description                         |
| --------------- | ----------------------- | ----------------------------------------------------------- |
| /ri2 help       | randomitems2.command    | Displays a help message                                     |
| /ri2 exempt     | randomitems2.exempt     | Toggles if random items will spawn near a specified player  |
| /ri2 toggle     | randomitems2.toggle     | Toggles if random items will spawn near you                 |
| /ri2 reload     | randomitems2.reload     | Reloads all plugin configurations                           |
| /ri2 give       | randomitems2.give       | Can be used to give players special items                   |
| /ri2 spawner    | randomitems2.spawner    | Can be used to give players custom mob spawners             |
| /ri2 tier       | randomitems2.tier       | Can be used to view or change a player's item tier          |
| /ri2 tierupzone | randomitems2.tierupzone | Can be used to create a new tier-up zone in the world       |

## Compatibility with other plugins
RandomItems2 has built-in support for [ProjectKorra Scrolls](https://github.com/CozmycDev/ProjectKorraScrolls) and [PlaceholderAPI](https://placeholderapi.com).
If **ProjectKorra Scrolls** is installed on your server, then random scrolls can be included as a random item type in the `item-tiers` config.
If **PlaceholderAPI** is installed, then every message in `messages.yml` supports using PlaceholderAPI placeholders.
Of course, neither of these plugins are necessary for running a server with RandomItems2 :)

## Default Configuration
```yml
#############################################
#  RANDOM        ____    ___                #
#               |  _ \  |_ _|               #
#               | |_) |  | |                #
#               |  _ <   | |                #
#               |_| \_\ |___|        ITEMS  #
#############################################

# RandomItems2 - Main Plugin Configuration


# ==== GENERAL OPTIONS ====
# Random items will spawn near players in these worlds:
worlds: []

# Random items are spawned near the player after a random amount of time within the below interval (both inclusive).
# Times are given in ticks (1 second = 20 ticks)
min-give-time: 200
max-give-time: 400

# An interval for the amount of time it takes for a spawned item to actually "pop" into the world. In other words, how long it takes the item to manifest
# Times are given in ticks (1 second = 20 ticks)
min-pop-time: 100
max-pop-time: 140

# This is a list of players who will not receive random items
# This list may be configured manually but is usually updated by the in-game `exempt` and `toggle` commands.
# This list may contain player usernames or UUIDs. The in-game commands will use UUIDs.
exempt: []

# ==== ITEM GENERATION

# Item tiers provide a progression for players to receive increasingly advanced sets of random items.
# To configure the plugin to always give entirely random items, replace the below with the following:
#item-tiers:
#  -
#    items:
#      - all
item-tiers:
  -
    # All the items that can generate around players in this tier
    items:
      # You can use the item ID of any item
      - stick
      # Or a minecraft item tag [see: https://minecraft.wiki/w/Item_tag_(Java_Edition)]
      - "#logs"
      # Another form of item ID, with an explicit `minecraft:` namespace
      - "minecraft:stone"
      - cobblestone
      # If the plugin ProjectKorraScrolls is installed you can use the following item type, which will spawn as any random scroll the player could otherwise find in chests:
      # - scroll
    # Items the player needs to deposit into a tier-up zone to progress to the next tier
    # In the below example, the player needs to deposit a wooden axe and 3 sticks to progress to from tier 1 to tier 2
    next-tier-requires:
      wooden_axe: 1
      stick: 3
  - items:
      - "#logs"
      - stick
      # Items can be added in shorthand as above, but can also be added as a yml object like below for additional configuration
      - type: stone
        # The maximum amount of the item that can spawn in the generated stack. By default, this is 1
        # Item counts will always be limited to the maximum stack size of that item
        max-count: 10
        # The weight of an item determines how frequently it spawns. It is a number that is at least 1 which determines how likely an item is to spawn.
        # The default weight assigned to items if not specified is 5, so that a scale from 1-10 may be used to conveniently make items more common/rare than the default, however there's nothing limiting the weight from being set above 10
        weight: 10
      - cobblestone
      - type: wooden_axe
        # Enchantment tiers influence how an item is randomly enchanted. By default, items aren't randomly enchanted.
        # All possible values for enchantment tiers are:
        # Tier 1: 45% chance that a tool will have any random enchant with a level of 1
        # Tier 2: 35% chance that a tool will have a valid random enchant with a valid level
        # Tier 3: 25% chance that a tool will have a valid random enchant with a level of up to 10
        # Tier 4: Same as tier 3 but without curses
        # Tier 5: Same as tier 4 but with a 45% chance that it has a second valid random enchant with a level of up to 10
        # Tier 6: Same as tier 5 but with a 5% chance of a non-tool item having any enchant with a level up to 5
        # Tier 7: Same as tier 6 but with  15% chance for the non-tool to have an enchant up to level 10
        enchant-tier: 2
      - type: wooden_pickaxe
        enchant-tier: 2
      - type: wooden_sword
        enchant-tier: 2
    next-tier-requires:
      stone_sword: 4
      # Like with random items, you can use an item tag. In this case, it means the player must deposit 3 of any pickaxe
      "#pickaxes": 3
      cobblestone: 64
      stone: 10
  - items:
      - "#logs"
      - stick
      - type: stone
        max-count: 10
        # You can also configure the minimum amount of the item that will spawn. If max and min counts are the same, the item will always spawn with that quantity.
        # By default, the minimum count is 1
        min-count: 10
      - cobblestone
      - iron_ore
      - type: stone_axe
        enchant-tier: 3
      - type: stone_pickaxe
        enchant-tier: 3
      - type: stone_sword
        enchant-tier: 3
      - type: spawner
        # You can set the entities that a mob spawner can potentially come with when generated
        # The entities list may include entities from the "default-spawner-excludes" configuration option, bypassing it
        # You can also set an entity list on a hopper minecart to turn it into a spawner minecart
        # If not specified, a spawner block can generate with any vanilla entity that isn't in "default-spawner-excludes" by default, but a hopper minecart will not become a spawner minecart
        entities:
          - zombie
          - skeleton
          - spider
        # Spawners can cycle between multiple entities. You can set the minimum and maximum number of entities a spawner can spawn
        min-entities: 1
        max-entities: 2
    next-tier-requires:
      iron_ingot: 10
  - items:
      # Special item type that includes all vanilla items
      - type: all
        enchant-tier: 3
        # You can still specify min and max entities for "all", as well as an entity list, and it will affect any spawners generated
        min-entities: 2
        max-entities: 2

# Items that should not be included in the item type "all"
all-excludes:
  - ender_dragon_spawn_egg
  - wither_spawn_egg

# Mobs not included in spawners when they don't have an entities list set
default-spawner-excludes:
  - ender_dragon
  - wither

# ==== MISC OPTIONS ====

# Configures the way Random Shears act when used. If an invalid value is used this will default to "damage-spammable"
# damage-spammable - Entities will take a heart of damage when sheared. The shears will not perform their usual action (e.g., removing wool from a sheep). Entities will drop a random item when right clicked with shears, even if they are under no-damage-ticks and can't take damage. This mode encourages spam clicking shearable mobs for maximum output of random items.
# damage - Like spammable damage, entities will take a heart of damage when sheared and drop a random item. Unlike spammable damage, items will not drop when used during no damage ticks.
# cooldown - Right clicking a shearable entity drops a random item with a cooldown specified below.
# shear - Right clicking a shearable entity shears the entity but drops a random item instead. Mooshrooms will still drop mushrooms.
# disabled - Random shears act like normal shears and don't drop random items.
random-shears-behavior: damage-spammable

# If the random shears are set to "cooldown" above, this is how long the player must wait before shearing the same mob again.
# Values less than or equal to 0 allow spam-shearing the same mob over and over for random items with no cooldown.
# This value is set in ticks. (20 seconds = 1 tick)
random-shears-cooldown: 0

# If true, shulker boxes won't be filled with random items
disable-random-shulker-boxes: false
```

## Messages
Every message in the plugin can be customized in `messages.yml`, with many available placeholders for you to use.

### Default `messages.yml`
```yml
#############################################
#  RANDOM        ____    ___                #
#               |  _ \  |_ _|               #
#               | |_) |  | |                #
#               |  _ <   | |                #
#               |_| \_\ |___|        ITEMS  #
#############################################

# RandomItems2 - Messages

# If PlaceholderAPI is installed, then every message listed below supports placeholders from PlaceholderAPI on top of the listed supported placeholders
# PlaceholderAPI placeholders are always parsed as the player who ran the command or performed the action triggering the message, even when a command has a target

# A prefix that is prepended to all other messages in this file, except for the help message below, and a few other message options whose comments state they aren't prefixed
prefix: "&7[&cRandomItems2&7] &b"

# The help message displayed when running /ritems2 or /ritems2 help
# Each item in the help message is its own line
# Available placeholders: %alias% - the alias of the /randomitems2 command that the player ran (e.g., ritems2, ri2, randomitems2)
help-message:
  - "&6RandomItems 2.0 Help"
  - "&b/%alias% &6help: Displays this help message"
  - message: "&b/%alias% &6exempt &c[player]&b: Toggles if random items will spawn near a player"
    # The line will only display if the user has the specified permission
    permission: randomitems2.exempt
  - message: "&b/%alias% &6toggle&b: Toggles if random items will spawn near you"
    permission: randomitems2.toggle
  - message: "&b/%alias% &6reload&b: Reloads all plugin configurations"
    permission: randomitems2.reload
  - message: "&b/%alias% &6give &c<player> <item> [amount]&b: Can be used to give players special items"
    permission: randomitems2.give
  - message: "&b/%alias% &6spawner &c<player> <entity>[,<entities>...] [amount]&b: Can be used to give players custom mob spawners"
    permission: randomitems2.spawner
  - message: "&b/%alias% &6tier &c<player> [new tier]&b: Can be used to view or change a player's item tier."
    permission: randomitems2.tier
  - "&bPlugin made by &6houdert6"

# ==== GENERIC COMMAND ERRORS ====

# Message sent when a player uses a sub-command improperly
# Available placeholders:
#  - %alias%: the alias of the /randomitems2 command that the player ran
#  - %subcmd%: the subcommand the player tried running
#  - %params%: the parameters the command takes
usage: "&cUsage: &b/%alias% &6%subcmd% &c%params%"

# Message sent when the player tries to do something they don't have permission for (e.g., "You don't have permission to toggle the spawning of random items near you")
# Available placeholders:
#  - %action% - the action the player tried to do (e.g., "exempt players from receiving random items")
#  - %subcmd% - the subcommand the player tried running
no-permission: "&cYou don't have permission to %action%"

# Sent if a command is run that does not have a player specified by an argument, if the command expects such an argument when not run by a player
# Available placeholders:
#  - %action% - the action the player tried to do
#  - %subcmd% - the subcommand the player tried running
command-needs-player: "&cYou must either run this command as a player or specify a player"

# Sent if a non-player (console, cmd block) tries running a command that must be run by a player
# Available placeholders:
#  - %action% - the action the user tried to do
#  - %subcmd% - the subcommand the user tried running
must-be-player: "&cThis command must be run by a player"

# Sent if an invalid value is passed to a parameter expecting a player in a command
# Available placeholders:
#  - %action% - the action the player tried to do
#  - %subcmd% - the subcommand the player tried running
#  - %player% - the invalid player
player-not-found: "&cPlayer not found: &6%player%"

# Sent if an invalid number is specified for a command expecting a number
# Available placeholders:
#  - %action% - the action the player tried to do
#  - %subcmd% - the subcommand the player tried running
#  - %number% - the invalid number
invalid-number: "&6%number% &cis not a valid number"

# Sent if the player tries running a subcommand that doesn't exist
# Available placeholders:
#  - %player% - the player/user
#  - %subcmd% - the subcommand the user tried running
unknown-subcommand: "&cUnknown subcommand: &6%subcmd%"

# ==== EXEMPT AND TOGGLE ====

# Sent when /randomitems2 exempt is used to toggle on random items for a player
# Available placeholders: %player% - the name of the affected player
enable-items-other: "Random items will spawn around %player% again"

# Sent when /randomitems2 toggle is used to toggle on random items for the player running the command
# Available placeholders: %player% - the player's username
enable-items-self: "Random items will spawn around you again"

# Sent when /randomitems2 exempt is used to toggle off random items for a player
# Available placeholders: %player% - the name of the affected player
disable-items-other: "Random items will no longer spawn around %player%"

# Sent when /randomitems2 toggle is used to toggle off random items for the player running the command
# Available placeholders: %player% - the player's username
disable-items-self: "Random items will no longer spawn around you"

# ==== CONFIG RELOAD ====

# Sent when /randomitems2 reload is used
# No available placeholders
config-reload: "Reloaded!"

# Sent when /randomitems2 reload is used and RandomItems2 printed warnings to the console as part of the reload process because of invalid config data.
# No available placeholders
config-reload-with-warnings: "Reloaded &e(with warnings)"

# Sent if some component of RandomItems2 that depends on player data fails to access it
# No available placeholders
error-accessing-playerdata: "&cAn error is preventing the plugin from accessing player data."

# ==== GIVE ====

# Sent when a non-existent special item is specified in /randomitems2 give
# Available placeholders: %item% - the invalid item
invalid-item: "&cInvalid item: &6%item%"

# Sent if some items from /randomitems2 give didn't fit in the player's inventory
# Available placeholders:
#  - %player% - the player who received the items
#  - %amount% - the number of items given
#  - %lost% - number of items that didn't fit and were lost
#  - %item% - the name of the given item
no-space-for-some-items: "&cSome items didn't fit in the player's inventory and were lost!"

# Sent when /randomitems2 give is successful
# Available placeholders:
#  - %player% - the player who received the items
#  - %amount% - the number of items given
#  - %item% - the name of the given item
gave-items: "Gave &c%player% &6%amount% &bof &6%item%"

# ==== SPAWNER ====

# Sent when an invalid entity appears in the list of entities specified in /randomitems2 spawner
# Available placeholders: %entity% - the invalid entity
invalid-entity: "&cInvalid entity: &6%entity%"

# Sent if some spawners from /randomitems2 spawner didn't fit in the player's inventory
# Available placeholders:
#  - %player% - the player who received the spawners
#  - %amount% - the number of spawners given
#  - %lost% - number of spawners that didn't fit and were lost
no-space-for-some-spawners: "&cSome items didn't fit in the player's inventory and were lost!"

# Sent when /randomitems2 spawner is successful (when more than one entity is specified)
# Available placeholders:
#  - %player% - the player who received the spawner(s)
#  - %amount% - the number of spawners given
#  - %item% - the word "spawner" with the correct pluralization depending on if there's more than one given
#  - %entities% - a comma-seperated list of the entities the given spawner will spawn
gave-multi-entity-spawners: "Gave &c%player% &6%amount% &bmulti-entity %item%"

# Sent when /randomitems2 spawner is successful (when only one entity is specified)
# Available placeholders:
#  - %player% - the player who received the spawner(s)
#  - %amount% - the number of spawners given
#  - %item% - the word "spawner" with the correct pluralization depending on if there's more than one given
#  - %entity% - the entity the spawner will spawn
gave-spawners: "Gave &c%player% &6%amount% &b%item% of type &6%entity%"

# ==== TIER COMMAND ====

# The output of using /randomitems2 tier to see someone's item tier.
# Available placeholders:
#  - %player% - the player whose tier was checked
#  - %tier% - the player's current item tier
tier-output: "&c%player%'s &btier is &6%tier%"

# Sent when someone tries using /randomitems2 tier with a number less than 1
# Available placeholders:
#  - %player% - the player who the command attempted to change the tier of
#  - %tier% - the invalid tier
tier-too-low: "&cThe new tier must be ≥ 1"

# Sent when someone tries using /randomitems2 tier with too high a tier
# Available placeholders:
#  - %player% - the player who the command attempted to change the tier of
#  - %tier% - the invalid tier
#  - %maxtier% - the maximum allowed value for a tier (based on how many item-tiers there are in the configuration)
tier-too-high: "&cThe maximum tier on this server is &6%maxtier%"

# The output of using /randomitems2 tier to set someone's item tier.
# Available placeholders:
#  - %player% - the player whose tier was changed
#  - %tier% - the player's new item tier
tier-set: "Set &c%player%&b's tier to &r%tier%"

# ==== TIER UP ====

# The text displayed above a tier-up zone. Not prefixed by the prefix at the top of this file.
# No available placeholders
tier-up-zone-title: "&aRight click to deposit items"

# Sent when a player creates a new tier up zone with /randomitems2 tierupzone
# No available placeholders
tier-up-zone-created: "Tier up zone created!"

# Sent when a player right clicks a tier-up zone and deposits some items. Not prefixed by the prefix at the top of this file.
# Available placeholders:
#  - %item% - the deposited item
#  - %amount% - how much of the item was deposited
deposit-actionbar: "&aDeposited %amount%x &2%item%"

# Title and subtitle sent when a new tier is unlocked via a tier-up zone. Not prefixed by the prefix at the top of this file.
# Available placeholders:
#  - %oldtier% - the player's former tier
#  - %newtier% - the player's new tier
tier-up-title: "&9New Tier!"
tier-up-subtitle: "&bYou're now receiving &9Tier %newtier% &bitems!"

# Sent in chat when a player tiers up via a tier-up zone
# Available placeholders:
#  - %oldtier% - the player's former tier
#  - %newtier% - the player's new tier
tier-up-chat: "You've tiered up to &9%newtier%&b!"

# Sent in chat by a tier-up zone to tell the player what items they need to tier up
# The header is sent before all the lines with the requirements are sent
# Available placeholders:
#  - %oldtier% - the player's current tier
#  - %newtier% - the tier the player would rank up to upon depositing all required items
tier-up-requirements-header: "To tier up to Tier %newtier%, you need to deposit:"

# The format of each line detailing a requirement to tier up. Not prefixed by the prefix at the top of this file.
# Available placeholders:
#  - %deposited% - how much of the item the player has already deposited
#  - %required% - the total amount of the item the player needs
#  - %remaining% - the amount of the item the player still needs to deposit
#  - %item% - the item or type of item required
tier-up-requirements-line: "&c » &a%deposited%&8/&b%required% &6%item%"
```
