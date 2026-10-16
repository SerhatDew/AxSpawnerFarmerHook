package com.blokoyun.hooks.axfarmer.transfer;

import com.blokoyun.hooks.axfarmer.integration.FarmerCollection;
import com.blokoyun.hooks.axfarmer.integration.PaperRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LootTransferTest {
    private static PaperRegistries registries;

    @BeforeAll static void initializePaperRegistries() {
        registries = new PaperRegistries();
    }

    @AfterAll
    static void releaseRegistries() {
        registries.close();
    }

    @Test void atTheSlotLimitTheRemainderOnlyTopsUpTheLastStack() {
        assertEquals(64L, storeRemainderOnto62(1, 1));
    }


    @Test void overTheSlotLimitTheRemainderIsDroppedLikeAxSpawners() {
        assertEquals(62L, storeRemainderOnto62(2, 1));
    }

    @Test void partialFarmerDepositMustNotLoseWholeUnitsWhenResidueCrossesSlotLimit() {
        FarmerCollection farmer = mock(FarmerCollection.class);
        when(farmer.collect(any(), any(), anyInt())).thenReturn(13);
        ItemStack item = mock(ItemStack.class);
        AtomicInteger amount = new AtomicInteger(10);
        when(item.getType()).thenReturn(Material.BONE);
        when(item.getAmount()).thenAnswer(call -> amount.get());
        doAnswer(call -> { amount.set(call.getArgument(0)); return null; }).when(item).setAmount(anyInt());
        ItemStack key = mock(ItemStack.class);
        when(key.getMaxStackSize()).thenReturn(64);
        when(item.clone()).thenReturn(key);
        var storage = new HashMap<ItemStack, Long>();
        var items = new ArrayList<>(List.of(item));
        var transfer = new LootTransfer(farmer);
        transfer.collect(new Location(null, 0, 64, 0), 10, items, storage, 0, 1);
        assertTrue(items.isEmpty(), "AxSpawners must not add these units a second time");
        assertEquals(87L, storage.get(key), "100 generated - 13 collected in one initially empty batch");
    }

    /** 4 items x 10 spawners, farmer takes 13: exactly 27 remain for storage. */
    private long storeRemainderOnto62(long usedSlots, long maxSlots) {
        FarmerCollection farmer = mock(FarmerCollection.class);
        when(farmer.collect(any(), any(), anyInt())).thenReturn(13);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.BONE);
        when(item.getAmount()).thenReturn(4);
        ItemStack key = mock(ItemStack.class);
        when(key.getMaxStackSize()).thenReturn(64);
        when(item.clone()).thenReturn(key);
        Map<ItemStack, Long> storage = new HashMap<>();
        storage.put(key, 62L);
        new LootTransfer(farmer).collect(new Location(null, 0, 64, 0), 10,
            new ArrayList<>(List.of(item)), storage, usedSlots, maxSlots);
        return storage.get(key);
    }

    @Test void stackedLootIsCollectedOnceWithoutBeingAddedAgainByAxSpawners() {
        run(4, 10, 40, 40, 0);
    }

    @Test void nonDivisibleCapacityRetainsTheExactStackedRemainder() {
        run(4, 10, 13, 13, 27);
    }

    @Test void rejectedLootGoesToAxSpawnersStorage() {
        run(4, 10, 0, 0, 40);
    }

    @Test void virtualCountsAboveIntegerRangeDoNotOverflowFarmerStorage() {
        run(2, 3_000_000_000L, Integer.MAX_VALUE, Integer.MAX_VALUE, 3_852_516_353L);
    }

    @Test void overflowedSpawnerMultiplicationDoesNotCreateAFarmerDeposit() {
        run(2, Long.MAX_VALUE, 100, 0, 0);
    }

    private void run(int unitAmount, long spawners, int capacity, long expectedFarmer, long expectedSpawner) {
        FarmerCollection farmer = mock(FarmerCollection.class);
        AtomicInteger farmerStock = new AtomicInteger();
        when(farmer.collect(any(), any(), anyInt())).thenAnswer(call -> {
            int accepted = Math.min(call.<Integer>getArgument(2), capacity - farmerStock.get());
            farmerStock.addAndGet(accepted);
            return accepted;
        });
        AtomicInteger unitCount = new AtomicInteger(unitAmount);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.BONE);
        when(item.getAmount()).thenAnswer(call -> unitCount.get());
        doAnswer(call -> { unitCount.set(call.getArgument(0)); return null; }).when(item).setAmount(anyInt());
        ItemStack normalized = mock(ItemStack.class);
        when(normalized.getMaxStackSize()).thenReturn(64);
        when(item.clone()).thenReturn(normalized);
        List<ItemStack> loot = new ArrayList<>(List.of(item));
        Map<ItemStack, Long> storage = new HashMap<>();
        new LootTransfer(farmer).collect(new Location(null, 0, 64, 0), spawners, loot, storage, 0, Long.MAX_VALUE);
        assertEquals(expectedFarmer, farmerStock.get());
        assertTrue(loot.isEmpty(), "the producer must not add an already handled batch");
        long stored = storage.values().stream().mapToLong(Long::longValue).sum();
        assertEquals(expectedSpawner, stored);
        if (spawners <= Long.MAX_VALUE / unitAmount) {
            assertEquals((long) unitAmount * spawners, farmerStock.get() + stored);
        }
    }
}
