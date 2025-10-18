package me.yusufkerem;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.UUID;

public class CatsmpCommand implements CommandExecutor {

    private final CATSMPMC plugin;

    public CatsmpCommand(CATSMPMC plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "CATSMP Commands:");
            sender.sendMessage(ChatColor.YELLOW + "/catsmp upgrade" + ChatColor.WHITE + " - Opens upgrade menu");
            sender.sendMessage(ChatColor.YELLOW + "/catsmp checkbp [player]" + ChatColor.WHITE + " - Check Blood Points");
            sender.sendMessage(ChatColor.YELLOW + "/catsmp addbp <player>" + ChatColor.WHITE + " - Adds 1 Blood Point (Admin)");
            sender.sendMessage(ChatColor.YELLOW + "/catsmp removebp <player>" + ChatColor.WHITE + " - Removes 1 Blood Point (Admin)");
            sender.sendMessage(ChatColor.YELLOW + "/catsmp setbp <player> <amount>" + ChatColor.WHITE + " - Sets Blood Points (Admin)");
            return true;
        }

        // ---------------------- /catsmp checkbp ----------------------
        if (args[0].equalsIgnoreCase("checkbp")) {
            if (args.length == 1) {
                // Self check
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Console must specify a player!");
                    return true;
                }
                Player player = (Player) sender;
                UUID uuid = player.getUniqueId();
                int bp = plugin.getPlayerData().getInt(uuid + ".bloodpoints", 0);
                player.sendMessage(ChatColor.GOLD + "You currently have " + ChatColor.YELLOW + bp + "/5" + ChatColor.GOLD + " Blood Points.");
                return true;
            } else {
                // Check others
                if (!sender.hasPermission("catsmp.checkbp.others")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to check other players!");
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found!");
                    return true;
                }

                UUID targetUUID = target.getUniqueId();
                int bp = plugin.getPlayerData().getInt(targetUUID + ".bloodpoints", 0);
                sender.sendMessage(ChatColor.GOLD + target.getName() + " has " + ChatColor.YELLOW + bp + "/5" + ChatColor.GOLD + " Blood Points.");
                return true;
            }
        }

        // ---------------------- /catsmp addbp ----------------------
        if (args[0].equalsIgnoreCase("addbp")) {
            if (!sender.hasPermission("catsmp.addbp")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /catsmp addbp <player>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found!");
                return true;
            }

            UUID targetUUID = target.getUniqueId();
            int currentBP = plugin.getPlayerData().getInt(targetUUID + ".bloodpoints", 0);

            if (currentBP >= 5) {
                sender.sendMessage(ChatColor.YELLOW + target.getName() + " already has the maximum of 5 Blood Points!");
                return true;
            }

            int newBP = Math.min(currentBP + 1, 5);
            plugin.getPlayerData().set(targetUUID + ".bloodpoints", newBP);
            plugin.savePlayerData();

            sender.sendMessage(ChatColor.GREEN + "Added 1 Blood Point to " + target.getName() + " (" + newBP + "/5)");
            target.sendMessage(ChatColor.GOLD + "You gained a Blood Point! (" + newBP + "/5)");
            return true;
        }

        // ---------------------- /catsmp removebp ----------------------
        if (args[0].equalsIgnoreCase("removebp")) {
            if (!sender.hasPermission("catsmp.removebp")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /catsmp removebp <player>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found!");
                return true;
            }

            UUID targetUUID = target.getUniqueId();
            int currentBP = plugin.getPlayerData().getInt(targetUUID + ".bloodpoints", 0);

            if (currentBP <= 0) {
                sender.sendMessage(ChatColor.YELLOW + target.getName() + " already has 0 Blood Points!");
                return true;
            }

            int newBP = Math.max(currentBP - 1, 0);
            plugin.getPlayerData().set(targetUUID + ".bloodpoints", newBP);
            plugin.savePlayerData();

            sender.sendMessage(ChatColor.GREEN + "Removed 1 Blood Point from " + target.getName() + " (" + newBP + "/5)");
            target.sendMessage(ChatColor.RED + "You lost a Blood Point! (" + newBP + "/5)");
            return true;
        }

        // ---------------------- /catsmp setbp ----------------------
        if (args[0].equalsIgnoreCase("setbp")) {
            if (!sender.hasPermission("catsmp.setbp")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                return true;
            }

            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /catsmp setbp <player> <amount>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found!");
                return true;
            }

            int amount;
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Amount must be a number!");
                return true;
            }

            if (amount < 0 || amount > 5) {
                sender.sendMessage(ChatColor.RED + "Amount must be between 0 and 5!");
                return true;
            }

            UUID targetUUID = target.getUniqueId();
            plugin.getPlayerData().set(targetUUID + ".bloodpoints", amount);
            plugin.savePlayerData();

            sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s Blood Points to " + amount + "/5");
            target.sendMessage(ChatColor.GOLD + "Your Blood Points have been set to " + amount + "/5");
            return true;
        }

        // ---------------------- /catsmp upgrade ----------------------
        if (args[0].equalsIgnoreCase("upgrade")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command!");
                return true;
            }

            Player player = (Player) sender;
            plugin.openUpgradeMenu(player);
            return true;
        }

        return false;
    }
}
