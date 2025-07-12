package com.example.paperwhitelist;

import com.example.paperwhitelist.config.ConfigManager;
import com.example.paperwhitelist.database.DatabaseManager;
import com.example.paperwhitelist.web.WebServer;
import org.bukkit.plugin.java.JavaPlugin;

public class PaperWhitelistPlugin extends JavaPlugin {
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private WebServer webServer;

    @Override
    public void onEnable() {
        // Initialize configuration manager
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        // Initialize database manager
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();

        // Initialize and start web server
        webServer = new WebServer(this);
        webServer.start();

        getLogger().info("PaperWhitelistPlugin has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        // Stop web server
        if (webServer != null) {
            webServer.stop();
        }

        // Close database connections
        if (databaseManager != null) {
            databaseManager.close();
        }

        getLogger().info("PaperWhitelistPlugin has been disabled successfully!");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}