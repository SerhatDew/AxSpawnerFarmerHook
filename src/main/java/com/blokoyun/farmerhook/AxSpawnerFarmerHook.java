package com.blokoyun.farmerhook;

import com.artillexstudios.axspawners.api.events.AxSpawnersLootGenerateEvent;
import com.artillexstudios.axspawners.spawner.Spawner;
import com.uxplima.uxmfarmer.FarmerPlugin;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class AxSpawnerFarmerHook extends JavaPlugin implements Listener {
    private final LootQueue<Spawner, ItemStack> pending = new LootQueue<>();
    private FarmerPlugin farmer;
    private LootTransfer transfer;
    private MethodHandle location;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        boolean collectSpawnerDrops = getConfig().getBoolean("collect-spawner-drops", true);
        farmer = (FarmerPlugin) getServer().getPluginManager().getPlugin("uxmFarmer");
        if (farmer == null || !farmer.isEnabled()) {
            getLogger().severe("uxmFarmer must be enabled before this hook.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        try {
            // BCommons is loaded and relocated by AxSpawners at runtime, not bundled in its API jar.
            Method accessor = Spawner.class.getMethod("getLocation");
            Method unwrap = accessor.getReturnType().getMethod("get");
            location = MethodHandles.filterReturnValue(
                MethodHandles.publicLookup().unreflect(accessor),
                MethodHandles.publicLookup().unreflect(unwrap)
            ).asType(MethodType.methodType(Location.class, Spawner.class));
        } catch (ReflectiveOperationException ex) {
            getLogger().log(Level.SEVERE, "Cannot bind to the AxSpawners 1.12.1 location API.", ex);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        transfer = new LootTransfer(new FarmerCollection(farmer));
        if (collectSpawnerDrops) {
            getServer().getPluginManager().registerEvents(this, this);
            getServer().getScheduler().runTaskTimer(this, () -> pending.drain(this::deliver), 1L, 1L);
        }
        getLogger().info("Hook ready; collect-spawner-drops=" + collectSpawnerDrops);
    }

    @Override
    public void onDisable() {
        // Listeners stay registered until after onDisable returns; late loot is left to AxSpawners.
        pending.close();
        pending.drain(this::deliver);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLoot(AxSpawnersLootGenerateEvent event) {
        if (!farmer.isEnabled()) {
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            collect(event.getSpawner(), event.getItems());
            return;
        }
        // AxSpawners adds this list right after the event on its ticker thread, and only after
        // the whole tick may it start the next one. Take the loot out of the list so that add is a
        // no-op, then finish it on the next server tick instead of stalling the ticker.
        pending.offer(event.getSpawner(), event.getItems());
    }

    private void deliver(Spawner spawner, List<ItemStack> items) {
        try {
            collect(spawner, items);
        } catch (RuntimeException ex) {
            getLogger().log(Level.SEVERE, "Spawner loot transfer failed; the rest goes to AxSpawners storage.", ex);
        }
        if (!items.isEmpty()) {
            spawner.add(items);
        }
    }

    private void collect(Spawner spawner, List<ItemStack> items) {
        if (!farmer.isEnabled() || spawner.isRemoved()) {
            return;
        }
        Location at;
        try {
            at = (Location) location.invokeExact(spawner);
        } catch (Throwable ex) {
            throw new IllegalStateException("Cannot resolve AxSpawners location", ex);
        }
        // Mutate the list in place: AxSpawners 1.12.1 adds the event's own list, not event.setItems.
        if (transfer.collect(at, spawner.getAmount(), items, spawner.getStoredItems(),
            spawner.getUsedSlots(), spawner.getMaxSlots())) {
            spawner.updateGuis();
        }
    }
}
