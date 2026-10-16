package com.blokoyun.hooks.axfarmer.transfer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;

/** Hands asynchronous loot batches to the server thread without blocking AxSpawners. */
public final class LootQueue<S, T> {
    private record Batch<S, T>(S owner, List<T> items) {}

    private final Object lock = new Object();
    private final ConcurrentLinkedQueue<Batch<S, T>> batches = new ConcurrentLinkedQueue<>();
    private boolean open = true;

    /** Moves every element out of items, or leaves it untouched after close. */
    public boolean offer(S owner, List<T> items) {
        synchronized (lock) {
            if (!open) {
                return false;
            }
            List<T> taken = new ArrayList<>(items);
            items.clear();
            batches.add(new Batch<>(owner, taken));
            return true;
        }
    }

    public void drain(BiConsumer<S, List<T>> delivery) {
        for (Batch<S, T> batch; (batch = batches.poll()) != null;) {
            delivery.accept(batch.owner(), batch.items());
        }
    }

    /** Stops producers; batches already queued remain available to drain. */
    public void close() {
        synchronized (lock) {
            open = false;
        }
    }
}
