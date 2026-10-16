package com.blokoyun.hooks.axfarmer.transfer;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LootQueueTest {
    @Test void offeredLootLeavesTheProducerListAndIsDeliveredOnce() {
        LootQueue<String, String> queue = new LootQueue<>();
        List<String> first = new ArrayList<>(List.of("bone", "arrow"));
        List<String> second = new ArrayList<>(List.of("string"));
        assertTrue(queue.offer("a", first));
        assertTrue(queue.offer("b", second));
        assertTrue(first.isEmpty(), "AxSpawners must find nothing left to add");
        assertTrue(second.isEmpty());

        List<String> delivered = new ArrayList<>();
        queue.drain((owner, items) -> delivered.add(owner + "=" + items));
        queue.drain((owner, items) -> delivered.add(owner + "=" + items));
        assertEquals(List.of("a=[bone, arrow]", "b=[string]"), delivered);
    }

    @Test void closedQueueLeavesLateLootToTheProducerButKeepsQueuedLoot() {
        LootQueue<String, String> queue = new LootQueue<>();
        queue.offer("a", new ArrayList<>(List.of("bone")));
        queue.close();
        List<String> late = new ArrayList<>(List.of("arrow"));
        assertFalse(queue.offer("b", late));
        assertEquals(List.of("arrow"), late);

        List<String> delivered = new ArrayList<>();
        queue.drain((owner, items) -> delivered.add(owner + "=" + items));
        assertEquals(List.of("a=[bone]"), delivered);
    }

    @Test void readOnlyLootIsNotQueuedAlongsideTheProducerCopy() {
        LootQueue<String, String> queue = new LootQueue<>();
        assertThrows(UnsupportedOperationException.class, () -> queue.offer("a", List.of("bone")));
        queue.drain((owner, items) -> fail("loot would be added twice"));
    }
}
