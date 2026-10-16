package com.blokoyun.hooks.axfarmer.config;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/** Validated, immutable settings read once during startup. */
public record HookConfig(int configVersion, boolean collectionEnabled) {
    public static final int CURRENT_VERSION = 1;

    public HookConfig {
        if (configVersion != CURRENT_VERSION) {
            throw new IllegalArgumentException("Desteklenmeyen config-version: " + configVersion
                + "; beklenen: " + CURRENT_VERSION + ".");
        }
    }

    public static HookConfig load(JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        File file = new File(plugin.getDataFolder(), "config.yml");
        YamlConfiguration disk = new YamlConfiguration();
        disk.options().parseComments(true);
        try {
            // Resource defaults must not hide a legacy user value on disk.
            disk.load(file);
            HookConfig result = read(disk);
            boolean changed = false;
            if (!disk.contains("config-version")) {
                disk.set("config-version", CURRENT_VERSION);
                changed = true;
            }
            if (disk.contains("collect-spawner-drops")) {
                disk.set("collection.enabled", result.collectionEnabled());
                disk.set("collect-spawner-drops", null);
                changed = true;
            }
            if (changed) {
                disk.save(file);
            }
            return result;
        } catch (IOException | InvalidConfigurationException ex) {
            throw new IllegalStateException("config.yml okunamadı veya taşınamadı.", ex);
        }
    }

    public static HookConfig read(FileConfiguration config) {
        Object rawVersion = config.get("config-version");
        int version = readVersion(rawVersion);
        if (config.contains("collection") && !config.isConfigurationSection("collection")) {
            throw invalid("collection", config.get("collection"), "bir YAML bölümü");
        }
        boolean enabled = config.contains("collection.enabled")
            ? readBoolean(config, "collection.enabled")
            : config.contains("collect-spawner-drops") ? readBoolean(config, "collect-spawner-drops") : true;
        return new HookConfig(version, enabled);
    }

    private static int readVersion(Object raw) {
        if (raw == null) {
            return CURRENT_VERSION;
        }
        if (raw instanceof Integer value) {
            return value;
        }
        if (raw instanceof Long value && value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
            return value.intValue();
        }
        throw invalid("config-version", raw, "bir tam sayı");
    }

    private static boolean readBoolean(FileConfiguration config, String path) {
        Object raw = config.get(path);
        if (raw instanceof Boolean value) {
            return value;
        }
        throw invalid(path, raw, "true veya false");
    }

    private static IllegalArgumentException invalid(String path, Object value, String expected) {
        return new IllegalArgumentException("Geçersiz " + path + " değeri '" + value
            + "'; beklenen: " + expected + ".");
    }
}
