package com.blokoyun.hooks.axfarmer.integration;

import com.artillexstudios.axspawners.spawner.Spawner;
import com.blokoyun.hooks.axfarmer.listener.AxLootListener;
import com.blokoyun.hooks.axfarmer.transfer.LootTransfer;
import com.blokoyun.hooks.common.logging.LifecycleLogger;
import com.uxplima.uxmfarmer.FarmerPlugin;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

/** Applies a captured batch using live Farmer rules and AxSpawners storage. */
public final class AxLootDelivery implements AxLootListener.Delivery {
    private final FarmerPlugin farmer;
    private final AxSpawnerApi axApi;
    private final LootTransfer transfer;
    private final LifecycleLogger lifecycle;

    public AxLootDelivery(FarmerPlugin farmer, AxSpawnerApi axApi, LootTransfer transfer,
                          LifecycleLogger lifecycle) {
        this.farmer = farmer;
        this.axApi = axApi;
        this.transfer = transfer;
        this.lifecycle = lifecycle;
    }

    @Override
    public void deliver(Spawner spawner, long amount, List<ItemStack> items) {
        if (spawner.isRemoved()) {
            return;
        }
        long usedSlots = spawner.getUsedSlots();
        long maxSlots = spawner.getMaxSlots();
        boolean storageChanged = false;
        try {
            Location at = farmer.isEnabled() ? axApi.location(spawner) : null;
            storageChanged = transfer.collect(at, amount, items, spawner.getStoredItems(), usedSlots, maxSlots);
        } catch (RuntimeException ex) {
            lifecycle.failure("Eşya aktarımı başarısız; kalan eşyalar AxSpawners deposuna aktarılıyor.", ex);
            // Earlier entries may have changed storage before the failing entry.
            storageChanged = true;
            transfer.collect(null, amount, items, spawner.getStoredItems(), usedSlots, maxSlots);
        } finally {
            if (storageChanged) {
                spawner.updateGuis();
            }
        }
    }
}
