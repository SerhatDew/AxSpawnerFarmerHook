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
            return false;
        }
        boolean storageChanged = false;
        for (Iterator<ItemStack> iterator = items.iterator(); iterator.hasNext();) {
            ItemStack item = iterator.next();
            if (item == null || item.getAmount() <= 0 || item.getType().isAir()) {
                continue;
            }
            // AxSpawners multiplies the event's amount in Spawner.add, not before the event.
            if (spawnerAmount > Long.MAX_VALUE / item.getAmount()) {
                continue;
            }
            long generated = item.getAmount() * spawnerAmount;
            int offered = (int) Math.min(generated, Integer.MAX_VALUE);
            int accepted = farmers.collect(location, item, offered);
            if (accepted == 0) {
                continue;
            }
            if (accepted < 0 || accepted > offered) {
                throw new IllegalStateException("uxmFarmer returned an invalid collected amount: " + accepted);
            }
            long remaining = generated - accepted;
            int units = (int) (remaining / spawnerAmount);
            long residue = remaining % spawnerAmount;
            if (residue != 0 && usedSlots <= maxSlots) {
                // A partial farmer capacity need not be divisible by the stacked spawner count.
                // Store this exact residue the way Spawner.add would; the whole units still use it.
                ItemStack key = item.clone();
                key.setAmount(1);
                long toStore = residue;
                if (usedSlots == maxSlots) {
                    // At the slot limit AxSpawners only tops up the item's last partial stack.
                    long current = storage.getOrDefault(key, 0L);
                    int stackSize = key.getMaxStackSize();
                    toStore = Math.min(residue, Math.ceilDiv(current, stackSize) * stackSize - current);
                }
                if (toStore > 0) {
                    storage.merge(key, toStore, Math::addExact);
                    storageChanged = true;
                }
            }
            if (units == 0) {
                iterator.remove();
            } else {
                item.setAmount(units);
            }
        }
        return storageChanged;
    }
}
