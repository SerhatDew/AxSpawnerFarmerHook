package com.blokoyun.farmerhook;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

final class LootTransfer {
    private final FarmerCollection farmers;

    LootTransfer(FarmerCollection farmers) {
        this.farmers = farmers;
    }

    boolean collect(Location location, long spawnerAmount, List<ItemStack> items, Map<ItemStack, Long> storage,
                    long usedSlots, long maxSlots) {
        if (spawnerAmount <= 0) {
            items.clear();
            return false;
        }
        boolean storageChanged = false;
        for (Iterator<ItemStack> iterator = items.iterator(); iterator.hasNext();) {
            ItemStack item = iterator.next();
            if (item == null || item.getAmount() <= 0 || item.getType().isAir()) {
                iterator.remove();
                continue;
            }
            // AxSpawners multiplies the event's amount in Spawner.add, not before the event.
            if (spawnerAmount > Long.MAX_VALUE / item.getAmount()) {
                iterator.remove();
                continue;
            }
            long generated = item.getAmount() * spawnerAmount;
            int offered = (int) Math.min(generated, Integer.MAX_VALUE);
            int accepted = location == null ? 0 : farmers.collect(location, item, offered);
            if (accepted < 0 || accepted > offered) {
                throw new IllegalStateException("uxmFarmer returned an invalid collected amount: " + accepted);
            }
            long remaining = generated - accepted;
            if (remaining != 0 && usedSlots <= maxSlots) {
                // Use one exact addition and the batch's original slot state, just like Spawner.add.
                ItemStack key = item.clone();
                key.setAmount(1);
                long toStore = remaining;
                if (usedSlots == maxSlots) {
                    // At the slot limit AxSpawners only tops up the item's last partial stack.
                    long current = storage.getOrDefault(key, 0L);
                    int stackSize = key.getMaxStackSize();
                    toStore = Math.min(remaining, (stackSize - current % stackSize) % stackSize);
                }
                if (toStore > 0) {
                    storage.merge(key, toStore, Math::addExact);
                    storageChanged = true;
                }
            }
            iterator.remove();
        }
        return storageChanged;
    }
}
