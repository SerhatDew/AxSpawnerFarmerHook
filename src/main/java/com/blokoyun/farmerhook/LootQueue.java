package com.blokoyun.farmerhook;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;

/**
 * Hands loot generated off the main thread over to the main thread without blocking the producer.
 */
final class LootQueue<S, T> {
    private record Batch<S, T>(S owner, List<T> items) {}

    private final Object lock = new Object();
    private final ConcurrentLinkedQueue<Batch<S, T>> batches = new ConcurrentLinkedQueue<>();
    private boolean open = true;

    /**
     * Moves every element out of {@code items} into the queue. Returns {@code false} and leaves
     * {@code items} untouched once the queue is closed.
     */
    boolean offer(S owner, List<T> items) {
        synchronized (lock) {
            if (!open) {
                return false;
            }
            List<T> taken = new ArrayList<>(items);
            // Clear before queueing: if the source list is read-only nothing is queued twice.
            items.clear();
            batches.add(new Batch<>(owner, taken));
            return true;
        }
    }

    void drain(BiConsumer<S, List<T>> delivery) {
        for (Batch<S, T> batch; (batch = batches.poll()) != null;) {
            delivery.accept(batch.owner(), batch.items());
        }
    }

    /** Rejects further offers; batches already queued stay available to {@link #drain}. */
    void close() {
        synchronized (lock) {
            open = false;
        }
    }
}
