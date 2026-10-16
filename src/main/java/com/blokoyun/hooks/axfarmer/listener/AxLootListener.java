package com.blokoyun.hooks.axfarmer.listener;

import com.artillexstudios.axspawners.api.events.AxSpawnersLootGenerateEvent;
import com.artillexstudios.axspawners.spawner.Spawner;
import com.blokoyun.hooks.axfarmer.transfer.LootQueue;
import java.util.List;
import java.util.function.BooleanSupplier;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

/** Captures AxSpawners loot and hands asynchronous batches to the owning plugin. */
public final class AxLootListener implements Listener {
    @FunctionalInterface
    public interface Delivery {
        void deliver(Spawner spawner, long amount, List<ItemStack> items);
    }

    private record Source(Spawner spawner, long amount) {}

    private final LootQueue<Source, ItemStack> pending = new LootQueue<>();
    private final BooleanSupplier farmerEnabled;
    private final Delivery delivery;
    private volatile boolean accepting = true;

    public AxLootListener(BooleanSupplier farmerEnabled, Delivery delivery) {
        this.farmerEnabled = farmerEnabled;
        this.delivery = delivery;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLoot(AxSpawnersLootGenerateEvent event) {
        if (!accepting || !farmerEnabled.getAsBoolean()) {
            return;
        }
        Source source = new Source(event.getSpawner(), event.getSpawner().getAmount());
        if (Bukkit.isPrimaryThread()) {
            delivery.deliver(source.spawner(), source.amount(), event.getItems());
            return;
        }
        pending.offer(source, event.getItems());
    }

    public void close() {
        accepting = false;
        pending.close();
    }

    public void drain() {
        pending.drain((source, items) -> delivery.deliver(source.spawner(), source.amount(), items));
    }
}
