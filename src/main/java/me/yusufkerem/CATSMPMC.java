package me.yusufkerem;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CATSMPMC extends JavaPlugin implements Listener {

    public void saveData() {
    }

    // ----- enums & constants -----
    public enum Role {
        PASSIVE, AGGRESSIVE
    }

    private static final int MAX_BP = 5;
    private static final long DASH_COOLDOWN_MS = 5000L; // 5 seconds

    // ability caps
    private static final int MAX_CLAWS = 2; // Strength up to II
    private static final int MAX_RABIES = 5; // up to 25% (5 * 5%)
    private static final int MAX_PROTECTIVE = 3; // up to 30% (3 * 10%)
    private static final int MAX_PURRING = 2; // Regen up to II
    private static final int MAX_HEALTHKITTY = 5; // up to +10 HP (5 hearts)
    private static final int MAX_ZOOM = 3; // up to 12 blocks (3*4)

    // items used in GUI (simple)
    private static final String UPGRADE_INV_TITLE = ChatColor.DARK_PURPLE + "CATSMP Upgrade Menu";

    // ----- runtime data -----
    private File dataFile;
    private FileConfiguration data;

    private final Random random = new Random();

    // in-memory caches (for quick access)
    // not strictly necessary but convenient
    private final Map<UUID, Role> roles = new HashMap<>();
    private final Map<UUID, Integer> bloodPoints = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> abilityLevels = new HashMap<>();
    private final Map<UUID, Boolean> zoomComboEnabled = new HashMap<>();
    private final Map<UUID, Long> lastDash = new HashMap<>();

    @Override
    public void onEnable() {
        getLogger().info("CATSMPMC enabling...");

        // create data file if needed
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            getDataFolder().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Could not create data.yml: " + e.getMessage());
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);

        // load existing saved data into memory
        loadAllData();

        // register command executor for catsmp and zoom and role
        Objects.requireNonNull(getCommand("catsmp")).setExecutor((sender, command, label, args) -> {
            return handleCatsmpCommand(sender, command.getName(), args);
        });
        Objects.requireNonNull(getCommand("zoom")).setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player p) {
                triggerDash(p);
                return true;
            }
            sender.sendMessage("Only players can use /zoom");
            return true;
        });
        Objects.requireNonNull(getCommand("zoomcombo")).setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player p) {
                boolean cur = zoomComboEnabled.getOrDefault(p.getUniqueId(), true);
                zoomComboEnabled.put(p.getUniqueId(), !cur);
                p.sendMessage(ChatColor.GREEN + "Sneak+Jump combo zoom is now: "
                        + (!cur ? ChatColor.AQUA + "enabled" : ChatColor.RED + "disabled"));
                savePlayer(p.getUniqueId());
                return true;
            }
            sender.sendMessage("Only players can toggle zoom combo");
            return true;
        });
        Objects.requireNonNull(getCommand("role")).setExecutor((sender, command, label, args) -> {
            if (args.length >= 3 && args[0].equalsIgnoreCase("set") && sender.hasPermission("catsmp.admin")) {
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found!");
                    return true;
                }
                Role r = args[2].equalsIgnoreCase("passive") ? Role.PASSIVE : Role.AGGRESSIVE;
                roles.put(target.getUniqueId(), r);
                // ensure data keys exist
                bloodPoints.putIfAbsent(target.getUniqueId(), 3);
                abilityLevels.putIfAbsent(target.getUniqueId(), new HashMap<>());
                zoomComboEnabled.putIfAbsent(target.getUniqueId(), true);
                applyAbilities(target);
                savePlayer(target.getUniqueId());
                sender.sendMessage(ChatColor.GREEN + "Set role of " + target.getName() + " to " + r.name());
                return true;
            }
            sender.sendMessage(ChatColor.YELLOW + "Usage: /role set <player> <passive|aggressive> (admin only)");
            return true;
        });

        // register events
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("CATSMPMC enabled!");
    }

    @Override
    public void onDisable() {
        // save all player data
        saveAllData();
        getLogger().info("CATSMPMC disabled and data saved.");
    }

    // -------------------------
    // Data load/save
    // -------------------------
    private void loadAllData() {
        if (data == null)
            return;

        ConfigurationSection playersSection = data.getConfigurationSection("players");
        if (playersSection == null)
            return;

        for (String uuidStr : playersSection.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                String roleStr = data.getString("players." + uuidStr + ".role", "PASSIVE");
                roles.put(uuid, Role.valueOf(roleStr));
                int bp = data.getInt("players." + uuidStr + ".bloodpoints", 3);
                bloodPoints.put(uuid, Math.min(bp, MAX_BP));

                // abilities
                Map<String, Integer> map = new HashMap<>();
                if (data.contains("players." + uuidStr + ".abilities")) {
                    ConfigurationSection abilities = data.getConfigurationSection("players." + uuidStr + ".abilities");
                    for (String ability : abilities.getKeys(false)) {
                        int lvl = abilities.getInt(ability, 0);
                        map.put(ability, lvl);
                    }
                }
                abilityLevels.put(uuid, map);

                zoomComboEnabled.put(uuid, data.getBoolean("players." + uuidStr + ".zoomcombo", true));
            } catch (IllegalArgumentException ex) {
                getLogger().warning("Bad UUID in data.yml: " + uuidStr);
            }
        }
    }

    private void saveAllData() {
        if (data == null)
            data = new YamlConfiguration();
        // clear players section
        data.set("players", null);

        for (UUID uuid : roles.keySet()) {
            String path = "players." + uuid.toString();
            data.set(path + ".role", roles.getOrDefault(uuid, Role.PASSIVE).name());
            data.set(path + ".bloodpoints", bloodPoints.getOrDefault(uuid, 3));
            data.set(path + ".zoomcombo", zoomComboEnabled.getOrDefault(uuid, true));

            Map<String, Integer> map = abilityLevels.getOrDefault(uuid, new HashMap<>());
            for (Map.Entry<String, Integer> e : map.entrySet()) {
                data.set(path + ".abilities." + e.getKey(), e.getValue());
            }
        }

        try {
            data.save(dataFile);
        } catch (IOException e) {
            getLogger().severe("Failed to save data.yml: " + e.getMessage());
        }
    }

    private void savePlayer(UUID uuid) {
        if (data == null)
            data = new YamlConfiguration();
        String path = "players." + uuid.toString();
        data.set(path + ".role", roles.getOrDefault(uuid, Role.PASSIVE).name());
        data.set(path + ".bloodpoints", bloodPoints.getOrDefault(uuid, 3));
        data.set(path + ".zoomcombo", zoomComboEnabled.getOrDefault(uuid, true));
        Map<String, Integer> map = abilityLevels.getOrDefault(uuid, new HashMap<>());
        data.set(path + ".abilities", null); // clear then repopulate
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            data.set(path + ".abilities." + e.getKey(), e.getValue());
        }
        try {
            data.save(dataFile);
        } catch (IOException e) {
            getLogger().severe("Failed to save player data for " + uuid + ": " + e.getMessage());
        }
    }

    // -------------------------
    // Commands
    // -------------------------
    private boolean handleCatsmpCommand(org.bukkit.command.CommandSender sender, String name, String[] args) {
        // /catsmp ...
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GREEN + "CATSMP Commands:");
            sender.sendMessage(ChatColor.YELLOW + "/catsmp upgrade" + ChatColor.WHITE + " - Open upgrade GUI");
            sender.sendMessage(ChatColor.YELLOW + "/catsmp checkbp [player]" + ChatColor.WHITE + " - Check BP");
            sender.sendMessage(ChatColor.YELLOW + "/catsmp addbp <player>" + ChatColor.WHITE + " - Add BP (admin)");
            sender.sendMessage(
                    ChatColor.YELLOW + "/catsmp removebp <player>" + ChatColor.WHITE + " - Remove BP (admin)");
            sender.sendMessage(
                    ChatColor.YELLOW + "/catsmp setbp <player> <amt>" + ChatColor.WHITE + " - Set BP (admin)");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("upgrade")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can open the upgrade GUI.");
                return true;
            }
            Player p = (Player) sender;
            openUpgradeMenu(p);
            return true;
        }

        if (sub.equals("checkbp")) {
            if (args.length == 1) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Console must specify a player: /catsmp checkbp <player>");
                    return true;
                }
                Player p = (Player) sender;
                int bp = bloodPoints.getOrDefault(p.getUniqueId(), 3);
                p.sendMessage(ChatColor.GOLD + "You have " + ChatColor.AQUA + bp + ChatColor.GOLD + "/" + MAX_BP
                        + " Blood Points.");
                return true;
            } else {
                if (!sender.hasPermission("catsmp.checkbp.others")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to check others' BP.");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }
                int bp = bloodPoints.getOrDefault(target.getUniqueId(), 3);
                sender.sendMessage(ChatColor.GOLD + target.getName() + " has " + ChatColor.AQUA + bp + ChatColor.GOLD
                        + "/" + MAX_BP + " Blood Points.");
                return true;
            }
        }

        if (sub.equals("addbp")) {
            if (!sender.hasPermission("catsmp.addbp")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /catsmp addbp <player>");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }
            UUID uuid = target.getUniqueId();
            int cur = bloodPoints.getOrDefault(uuid, 3);
            if (cur >= MAX_BP) {
                sender.sendMessage(ChatColor.YELLOW + target.getName() + " already has max BP (" + MAX_BP + ").");
                return true;
            }
            int next = Math.min(cur + 1, MAX_BP);
            bloodPoints.put(uuid, next);
            savePlayer(uuid);
            sender.sendMessage(
                    ChatColor.GREEN + "Added 1 BP to " + target.getName() + " (" + next + "/" + MAX_BP + ")");
            target.sendMessage(ChatColor.GOLD + "You received 1 Blood Point! (" + next + "/" + MAX_BP + ")");
            return true;
        }

        if (sub.equals("removebp")) {
            if (!sender.hasPermission("catsmp.removebp")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /catsmp removebp <player>");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }
            UUID uuid = target.getUniqueId();
            int cur = bloodPoints.getOrDefault(uuid, 3);
            if (cur <= 0) {
                sender.sendMessage(ChatColor.YELLOW + target.getName() + " already has 0 BP.");
                return true;
            }
            int next = Math.max(cur - 1, 0);
            bloodPoints.put(uuid, next);
            savePlayer(uuid);
            sender.sendMessage(
                    ChatColor.GREEN + "Removed 1 BP from " + target.getName() + " (" + next + "/" + MAX_BP + ")");
            target.sendMessage(ChatColor.RED + "You lost 1 Blood Point! (" + next + "/" + MAX_BP + ")");
            return true;
        }

        if (sub.equals("setbp")) {
            if (!sender.hasPermission("catsmp.setbp")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission.");
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /catsmp setbp <player> <amount>");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }
            int amount;
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "Amount must be a number.");
                return true;
            }
            if (amount < 0 || amount > MAX_BP) {
                sender.sendMessage(ChatColor.RED + "Amount must be between 0 and " + MAX_BP);
                return true;
            }
            UUID uuid = target.getUniqueId();
            bloodPoints.put(uuid, amount);
            savePlayer(uuid);
            sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s BP to " + amount + "/" + MAX_BP);
            target.sendMessage(ChatColor.GOLD + "Your Blood Points were set to " + amount + "/" + MAX_BP);
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Unknown subcommand.");
        return true;
    }

    // -------------------------
    // Player join / role assign / apply abilities
    // -------------------------
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();

        // if not in saved data -> new player; random role and starting BP 3
        if (!roles.containsKey(uuid)) {
            Role assigned = random.nextBoolean() ? Role.PASSIVE : Role.AGGRESSIVE;
            roles.put(uuid, assigned);
        }
        bloodPoints.putIfAbsent(uuid, 3);
        abilityLevels.putIfAbsent(uuid, new HashMap<>());
        zoomComboEnabled.putIfAbsent(uuid, true);

        // apply abilities to the player
        applyAbilities(p);

        // ensure saved data exists
        savePlayer(uuid);
    }

    // -------------------------
    // Death handling for BP gain/loss
    // -------------------------
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Player killer = victim.getKiller();
        if (killer != null && killer.getUniqueId() != null) {
            UUID vUuid = victim.getUniqueId();
            UUID kUuid = killer.getUniqueId();

            int vBP = bloodPoints.getOrDefault(vUuid, 0);
            if (vBP > 0) {
                int kBP = bloodPoints.getOrDefault(kUuid, 0);
                bloodPoints.put(kUuid, Math.min(kBP + 1, MAX_BP));
                killer.sendMessage(
                        ChatColor.GOLD + "You gained 1 Blood Point! (" + bloodPoints.get(kUuid) + "/" + MAX_BP + ")");
            }
            // victim loses 1 (down to 0)
            bloodPoints.put(vUuid, Math.max(vBP - 1, 0));

            // save both
            savePlayer(kUuid);
            savePlayer(vUuid);
        }
    }

    // -------------------------
    // Combat: Claws & Rabies & Protective Fur damage reduction
    // -------------------------
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player attacker && e.getEntity() instanceof Player victim) {
            UUID a = attacker.getUniqueId();
            Role role = roles.getOrDefault(a, Role.AGGRESSIVE);

            // Aggressive abilities only
            if (role == Role.AGGRESSIVE) {
                Map<String, Integer> levels = abilityLevels.getOrDefault(a, new HashMap<>());

                // Claws -> Strength effect already applied permanently via applyAbilities()
                // Rabies -> chance to apply Wither III for 2 seconds
                int rabies = levels.getOrDefault("rabies", 0);
                if (rabies > 0) {
                    int chance = Math.min(rabies * 5, 25); // 5% per level
                    if (random.nextInt(100) < chance) {
                        victim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 2, true, false, true)); // 2s
                                                                                                                     // Wither
                                                                                                                     // III
                        attacker.sendMessage(ChatColor.DARK_RED + "Rabies triggered!");
                    }
                }
            }
        }
    }

    // custom damage reduction for Protective Fur (aggressive)
    @EventHandler
    public void onEntityDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player victim))
            return;
        UUID uuid = victim.getUniqueId();
        Role role = roles.getOrDefault(uuid, Role.PASSIVE);
        if (role != Role.AGGRESSIVE)
            return;

        Map<String, Integer> levels = abilityLevels.getOrDefault(uuid, new HashMap<>());
        int protLvl = levels.getOrDefault("protectivefur", 0);
        if (protLvl <= 0)
            return;

        // reduce damage by 10% per level (max 30%)
        double reduce = Math.min(protLvl * 0.10, 0.30);
        double orig = e.getDamage();
        double newDamage = orig * (1.0 - reduce);
        e.setDamage(newDamage);
    }

    // -------------------------
    // No fall damage for cats
    // -------------------------
    @EventHandler
    public void onFall(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player && e.getCause() == EntityDamageEvent.DamageCause.FALL) {
            e.setCancelled(true);
        }
    }

    // -------------------------
    // Upgrade GUI
    // -------------------------
    public void openUpgradeMenu(Player p) {
        Role role = roles.getOrDefault(p.getUniqueId(), Role.PASSIVE);
        Inventory inv = Bukkit.createInventory(null, 9, UPGRADE_INV_TITLE);

        // build items depending on role
        List<ItemStack> items = new ArrayList<>();

        if (role == Role.PASSIVE) {
            items.add(makeGuiItem(Material.POTION, ChatColor.AQUA + "Purring (Regen)",
                    "Permanent Regeneration. Max " + MAX_PURRING));
            items.add(makeGuiItem(Material.APPLE, ChatColor.GREEN + "Health Kitty",
                    "Increase max hearts. Max " + MAX_HEALTHKITTY));
            items.add(makeGuiItem(Material.RABBIT_FOOT, ChatColor.LIGHT_PURPLE + "Zoom (Dash)",
                    "Dash forward. +4 blocks per level. Max " + MAX_ZOOM));
        } else {
            // aggressive
            items.add(makeGuiItem(Material.IRON_AXE, ChatColor.RED + "Claws", "Permanent Strength. Max " + MAX_CLAWS));
            items.add(makeGuiItem(Material.POISONOUS_POTATO, ChatColor.DARK_PURPLE + "Rabies",
                    "Chance to Wither III on hit. +5% per level. Max " + MAX_RABIES));
            items.add(makeGuiItem(Material.LEATHER, ChatColor.GRAY + "Protective Fur",
                    "Permanent damage reduction. +10% per level. Max " + MAX_PROTECTIVE));
        }

        // put items into inventory
        int slot = 1;
        for (ItemStack is : items) {
            inv.setItem(slot, is);
            slot += 2;
        }

        p.openInventory(inv);
    }

    private ItemStack makeGuiItem(Material m, String name, String loreText) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Collections.singletonList(ChatColor.GRAY + loreText));
            it.setItemMeta(meta);
        }
        return it;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        // Make sure they are clicking inside the upgrade menu
        if (event.getView().getTitle().equals(UPGRADE_INV_TITLE)) {
            event.setCancelled(true); // Prevent taking items

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta())
                return;

            String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            UUID uuid = player.getUniqueId();

            // Access your data file
            FileConfiguration data = this.data; // assuming you have 'data' already
            String path = "players." + uuid + ".";

            // Ability upgrade handling
            if (name.equalsIgnoreCase("Speed Boost")) {
                int level = data.getInt(path + "speedboost", 0);
                if (level < 5) {
                    data.set(path + "speedboost", level + 1);
                    player.sendMessage(ChatColor.GREEN + "Speed Boost upgraded to level " + (level + 1) + "!");
                } else {
                    player.sendMessage(ChatColor.RED + "Speed Boost is already maxed out!");
                }
            }

            if (name.equalsIgnoreCase("Fireball")) {
                int level = data.getInt(path + "fireball", 0);
                if (level < 5) {
                    data.set(path + "fireball", level + 1);
                    player.sendMessage(ChatColor.GOLD + "Fireball upgraded to level " + (level + 1) + "!");
                } else {
                    player.sendMessage(ChatColor.RED + "Fireball is already maxed out!");
                }
            }

            // etc. for claws, health kitty, purring, rabies

            try {
                data.save(dataFile);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Refresh menu
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(this, () -> openUpgradeMenu(player), 5L);
        }
    }

    // -------------------------
    // Apply abilities to player (auto apply on join and after upgrades)
    // -------------------------
    private void applyAbilities(Player p) {
        UUID uuid = p.getUniqueId();
        Role role = roles.getOrDefault(uuid, Role.PASSIVE);
        Map<String, Integer> lvlMap = abilityLevels.getOrDefault(uuid, new HashMap<>());

        // First remove effects we may have added earlier to avoid stacking
        p.removePotionEffect(PotionEffectType.REGENERATION);
        p.removePotionEffect(PotionEffectType.STRENGTH);
        p.removePotionEffect(PotionEffectType.WITHER); // typically applied transiently
        // Note: we handle protective fur with event-based damage reduction (no potion)

        if (role == Role.PASSIVE) {
            // Purring -> Regen
            int purr = Math.min(lvlMap.getOrDefault("purring", 0), MAX_PURRING);
            if (purr > 0) {
                int amp = Math.max(0, purr - 1); // level 1 => amp 0, level 2 => amp 1
                p.addPotionEffect(
                        new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, amp, true, false, true));
            }

            // Health Kitty -> Max HP increase
            int healthLvl = Math.min(lvlMap.getOrDefault("healthkitty", 0), MAX_HEALTHKITTY);
            AttributeInstance maxHealth = p.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null) {
                double base = 20.0;
                double extra = 2.0 * healthLvl; // each level +2 HP (1 heart)
                maxHealth.setBaseValue(base + extra);
                if (p.getHealth() > maxHealth.getValue())
                    p.setHealth(maxHealth.getValue());
                else
                    p.setHealth(Math.min(p.getHealth(), maxHealth.getValue()));
            }

            // Zoom -> (no ongoing potion) - handled on dash trigger
        } else {
            // Aggressive
            int clawsLvl = Math.min(lvlMap.getOrDefault("claws", 0), MAX_CLAWS);
            if (clawsLvl > 0) {
                int amp = Math.max(0, clawsLvl - 1); // 1->amp0, 2->amp1
                p.addPotionEffect(
                        new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, amp, true, false, true));
            }

            // Protective Fur -> handled in damage event (no potion effect)
            // Rabies -> applied on hit (no persistent effect here)
        }
    }

    // -------------------------
    // Dash / Zoom logic
    // -------------------------
    private void triggerDash(Player p) {
        UUID uuid = p.getUniqueId();
        int zoomLvl = Math.min(abilityLevels.getOrDefault(uuid, new HashMap<>()).getOrDefault("zoom", 0), MAX_ZOOM);
        if (zoomLvl <= 0) {
            p.sendMessage(ChatColor.RED + "You have not upgraded Zoom yet.");
            return;
        }

        long now = System.currentTimeMillis();
        long last = lastDash.getOrDefault(uuid, 0L);
        if (now - last < DASH_COOLDOWN_MS) {
            long left = (DASH_COOLDOWN_MS - (now - last)) / 1000;
            p.sendMessage(ChatColor.YELLOW + "Zoom is on cooldown. " + left + "s remaining.");
            return;
        }

        lastDash.put(uuid, now);
        // distance = 4 * level; cap already by MAX_ZOOM
        double dist = Math.min(4.0 * zoomLvl, 12.0);
        Vector dir = p.getLocation().getDirection().clone().normalize().multiply(dist);
        // small upward push so player doesn't immediately hit ground if they dash on
        // ground
        dir.setY(Math.max(0.2, dir.getY()));
        p.setVelocity(dir);
        p.sendMessage(ChatColor.AQUA + "Zoom!");
    }

    // attempt to detect sneak + jump -> PlayerToggleSneakEvent is fired when
    // toggling sneak
    // we'll check whether player's upward velocity is positive (just jumped) when
    // toggled into sneak
    @EventHandler
    public void onSneakToggle(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        boolean comboEnabled = zoomComboEnabled.getOrDefault(p.getUniqueId(), true);
        if (!comboEnabled)
            return;

        // only consider when starting to sneak
        if (!p.isSneaking()) {
            // p.isSneaking() reflects new state already; we need to check if they are now
            // sneaking
            // But to be robust, we'll check if player velocity upwards is positive (recent
            // jump)
            Vector vel = p.getVelocity();
            if (vel.getY() > 0.0) {
                // they just jumped and are now sneaking -> treat as combo
                triggerDash(p);
            }
        }
    }

    // -------------------------
    // Helpers: get / set defaults for players
    // -------------------------
    private void ensurePlayerDefaults(UUID uuid) {
        roles.putIfAbsent(uuid, random.nextBoolean() ? Role.PASSIVE : Role.AGGRESSIVE);
        bloodPoints.putIfAbsent(uuid, 3);
        abilityLevels.putIfAbsent(uuid, new HashMap<>());
        zoomComboEnabled.putIfAbsent(uuid, true);
    }

    // -------------------------
    // Utility: save single player's data (wraps savePlayer)
    // -------------------------
    // (already provided above: savePlayer(UUID uuid))

    // -------------------------
    // Helper to get player's BP (external usage)
    // -------------------------
    public int getBP(UUID uuid) {
        return bloodPoints.getOrDefault(uuid, 3);
    }

    // -------------------------
    // Utility: applying upgrades via commands (if you want CLI upgrade
    // implementation later)
    // -------------------------
    public boolean trySpendBP(UUID uuid) {
        int cur = bloodPoints.getOrDefault(uuid, 3);
        if (cur <= 0)
            return false;
        bloodPoints.put(uuid, cur - 1);
        savePlayer(uuid);
        return true;
    }

    // -------------------------
    // Extra safety: save all players periodically? (optional)
    // -------------------------
    public FileConfiguration getData() {
        return this.data; // assuming 'data' is your custom config FileConfiguration
    }

}
