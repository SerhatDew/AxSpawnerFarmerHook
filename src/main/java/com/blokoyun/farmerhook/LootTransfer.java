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
            if (residue != 0) {
                // A partial farmer capacity need not be divisible by the stacked spawner count.
                // Keep this exact residue separately; the remaining whole units still use AxSpawners.add.
                ItemStack key = item.clone();
                key.setAmount(1);
                long current = storage.getOrDefault(key, 0L);
                int stackSize = key.getMaxStackSize();
                long partialSpace = current % stackSize == 0 ? 0 : stackSize - current % stackSize;
                long freeSlots = Math.max(0, maxSlots - usedSlots);
                long room = freeSlots > (Long.MAX_VALUE - partialSpace) / stackSize
                    ? Long.MAX_VALUE : freeSlots * stackSize + partialSpace;
                long toStore = usedSlots > maxSlots ? 0 : Math.min(residue, room);
                if (toStore > 0) {
                    storage.merge(key, toStore, Math::addExact);
                    usedSlots += Math.ceilDiv(current + toStore, stackSize) - Math.ceilDiv(current, stackSize);
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
