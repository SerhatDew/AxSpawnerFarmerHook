package com.blokoyun.farmerhook;

import com.artillexstudios.axspawners.api.events.AxSpawnersLootGenerateEvent;
import com.artillexstudios.axspawners.spawner.Spawner;
import com.uxplima.uxmfarmer.FarmerPlugin;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class AxSpawnerFarmerHook extends JavaPlugin implements Listener {
    private FarmerPlugin farmer;
    private LootTransfer transfer;
    private MethodHandle location;
    private boolean collectSpawnerDrops;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        collectSpawnerDrops = getConfig().getBoolean("collect-spawner-drops", true);
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
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Hook ready; collect-spawner-drops=" + collectSpawnerDrops);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLoot(AxSpawnersLootGenerateEvent event) {
        if (!collectSpawnerDrops || !farmer.isEnabled()) {
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            collect(event);
            return;
        }
        // Do not let AxSpawners.add race the farmer transaction or consume the old list.
        Future<Void> task = getServer().getScheduler().callSyncMethod(this, () -> {
            collect(event);
            return null;
        });
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    task.get();
                    return;
                } catch (InterruptedException ex) {
                    // A timed-out Ax ticker can interrupt its worker. The transaction may already
                    // have committed: wait for the same task, never abandon it and duplicate loot.
                    interrupted = true;
                } catch (ExecutionException ex) {
                    throw new IllegalStateException("Spawner loot transfer failed", ex.getCause());
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void collect(AxSpawnersLootGenerateEvent event) {
        Spawner spawner = event.getSpawner();
        if (!collectSpawnerDrops || !farmer.isEnabled() || spawner.isRemoved()) {
            return;
        }
        Location at;
        try {
            at = (Location) location.invokeExact(spawner);
        } catch (Throwable ex) {
            throw new IllegalStateException("Cannot resolve AxSpawners location", ex);
        }
        // Mutate the original list: AxSpawners 1.12.1 does not read event.setItems afterwards.
        if (transfer.collect(at, spawner.getAmount(), event.getItems(), spawner.getStoredItems(),
            spawner.getUsedSlots(), spawner.getMaxSlots())) {
            spawner.updateGuis();
        }
    }
}
