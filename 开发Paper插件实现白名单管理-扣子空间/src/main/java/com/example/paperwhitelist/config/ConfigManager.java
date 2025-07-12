package com.example.paperwhitelist.config;

import com.example.paperwhitelist.PaperWhitelistPlugin;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private final PaperWhitelistPlugin plugin;
    private final File configFile;
    private Map<String, Object> config;

    public ConfigManager(PaperWhitelistPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
    }

    public void loadConfig() {
        // Create data folder if not exists
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        // Create default config if not exists
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }

        // Load config from file
        try (InputStream input = new FileInputStream(configFile)) {
            Yaml yaml = new Yaml(new CustomClassLoaderConstructor(getClass().getClassLoader()));
            config = yaml.load(input);
            
            // Ensure config is not null
            if (config == null) {
                config = new HashMap<>();
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load config file: " + e.getMessage());
            config = new HashMap<>();
        }
    }

    public void saveConfig() {
        try (FileWriter writer = new FileWriter(configFile)) {
            Yaml yaml = new Yaml();
            yaml.dump(config, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save config file: " + e.getMessage());
        }
    }

    public String getString(String path, String defaultValue) {
        String[] keys = path.split("\\.");
        Map<String, Object> current = config;
        
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            if (!current.containsKey(key)) {
                return defaultValue;
            }
            
            if (i == keys.length - 1) {
                Object value = current.get(key);
                return value != null ? value.toString() : defaultValue;
            }
            
            if (current.get(key) instanceof Map) {
                current = (Map<String, Object>) current.get(key);
            } else {
                return defaultValue;
            }
        }
        
        return defaultValue;
    }

    public int getInt(String path, int defaultValue) {
        try {
            String value = getString(path, String.valueOf(defaultValue));
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String path, boolean defaultValue) {
        String value = getString(path, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }

    public void setValue(String path, Object value) {
        String[] keys = path.split("\\.");
        Map<String, Object> current = config;
        
        for (int i = 0; i < keys.length - 1; i++) {
            String key = keys[i];
            if (!current.containsKey(key) || !(current.get(key) instanceof Map)) {
                current.put(key, new HashMap<String, Object>());
            }
            current = (Map<String, Object>) current.get(key);
        }
        
        current.put(keys[keys.length - 1], value);
        saveConfig();
    }
}