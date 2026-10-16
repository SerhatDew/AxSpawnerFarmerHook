package com.blokoyun.hooks.axfarmer.config;

import java.nio.file.Files;
import java.nio.file.Path;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HookConfigTest {
    @TempDir Path directory;

    @Test void legacyDisabledCollectionSurvivesResourceDefaultsAndDiskMigration() throws Exception {
        Path file = directory.resolve("config.yml");
        Files.writeString(file, "collect-spawner-drops: false\ncustom-value: retained\n");
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        YamlConfiguration resource = new YamlConfiguration();
        resource.set("collection.enabled", true);
        when(plugin.getConfig()).thenReturn(resource);

        assertFalse(HookConfig.load(plugin).collectionEnabled());
        YamlConfiguration migrated = YamlConfiguration.loadConfiguration(file.toFile());
        assertEquals(1, migrated.getInt("config-version"));
        assertFalse(migrated.getBoolean("collection.enabled"));
        assertFalse(migrated.contains("collect-spawner-drops"));
        assertEquals("retained", migrated.getString("custom-value"));
        assertFalse(HookConfig.load(plugin).collectionEnabled());
    }

    @Test void currentValueWinsWhenBothKeysExist() throws Exception {
        Path file = directory.resolve("config.yml");
        Files.writeString(file, "config-version: 1\ncollection:\n  enabled: false\ncollect-spawner-drops: true\n");
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        assertFalse(HookConfig.load(plugin).collectionEnabled());
        assertFalse(YamlConfiguration.loadConfiguration(file.toFile()).contains("collect-spawner-drops"));
    }

    @Test void invalidConfigIsNotRewritten() throws Exception {
        Path file = directory.resolve("config.yml");
        String contents = "collection:\n  enabled: perhaps\ncollect-spawner-drops: false\n";
        Files.writeString(file, contents);
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        assertThrows(IllegalArgumentException.class, () -> HookConfig.load(plugin));
        assertEquals(contents, Files.readString(file));
    }

    @Test void invalidStructureAndFutureSchemaAreRejected() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("collection", false);
        assertThrows(IllegalArgumentException.class, () -> HookConfig.read(config));
        config.set("collection", null);
        config.set("config-version", 2);
        assertThrows(IllegalArgumentException.class, () -> HookConfig.read(config));
        config.set("config-version", "1");
        assertThrows(IllegalArgumentException.class, () -> HookConfig.read(config));
    }
}
