package me.yusufkerem;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

public class CATSMPMC extends JavaPlugin {
    private File dataFile;
    private FileConfiguration data;

    @Override
    public void onEnable() {
        getLogger().info("CATSMPMC enabling...");

        // Create data file
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            getDataFolder().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Could not create data.yml!");
                e.printStackTrace();
            }
        }

        // Load configuration safely
        data = YamlConfiguration.loadConfiguration(dataFile);

        // Register command
        getCommand("catsmp").setExecutor(new CatsmpCommand(this));

        getLogger().info("CATSMPMC enabled successfully!");
    }

    @Override
    public void onDisable() {
        saveData();
        getLogger().info("CATSMPMC disabled.");
    }

    public FileConfiguration getData() {
        return data;
    }

    public void saveData() {
        if (data != null && dataFile != null) {
            try {
                data.save(dataFile);
                getLogger().info("Saved data.yml successfully.");
            } catch (IOException e) {
                getLogger().severe("Failed to save data.yml: " + e.getMessage());
            }
        } else {
            getLogger().warning("Skipped saving data.yml (data was null).");
        }
    }

    public void openUpgradeMenu(Player player) {
        // TODO: Implement upgrade menu functionality
        player.sendMessage("§6Upgrade menu is not yet implemented!");
    }
}
