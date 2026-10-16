package com.blokoyun.hooks.axfarmer;

import com.blokoyun.hooks.axfarmer.config.HookConfig;
import com.blokoyun.hooks.axfarmer.integration.AxSpawnerApi;
import com.blokoyun.hooks.axfarmer.integration.AxLootDelivery;
import com.blokoyun.hooks.axfarmer.integration.FarmerCollection;
import com.blokoyun.hooks.axfarmer.listener.AxLootListener;
import com.blokoyun.hooks.axfarmer.transfer.LootTransfer;
import com.uxplima.uxmfarmer.FarmerPlugin;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import com.blokoyun.hooks.common.logging.LifecycleLogger;

public final class AxSpawnerFarmerHook extends JavaPlugin {
    private final LifecycleLogger lifecycle = new LifecycleLogger(this);
    private AxLootListener listener;
    private BukkitTask ticker;

    private boolean startupComplete;
    private boolean startupFailed;

    @Override
    public void onEnable() {
        long startedNanos = System.nanoTime();
        startupComplete = false;
        startupFailed = false;
        lifecycle.startup();
        try {
            saveDefaultConfig();
            HookConfig config = HookConfig.load(this);
            lifecycle.info("Yapılandırma yüklendi.");
            FarmerPlugin farmer = findFarmer();
            lifecycle.info("Bağımlılıklar hazır: AxSpawners, uxmFarmer.");
            if (config.collectionEnabled()) {
                AxSpawnerApi axApi = new AxSpawnerApi();
                LootTransfer transfer = new LootTransfer(new FarmerCollection(farmer));
                AxLootDelivery delivery = new AxLootDelivery(farmer, axApi, transfer, lifecycle);
                lifecycle.info("API bağlantıları kuruldu.");
                listener = new AxLootListener(farmer::isEnabled, delivery);
                getServer().getPluginManager().registerEvents(listener, this);
                ticker = getServer().getScheduler().runTaskTimer(this, listener::drain, 1L, 1L);
                lifecycle.info("Eşya aktarımı etkinleştirildi.");
            } else {
                lifecycle.inactive("Eşya aktarımı yapılandırmadan kapalı; dinleyici ve görev başlatılmadı.");
            }
            startupComplete = true;
            if (config.collectionEnabled()) {
                lifecycle.started(startedNanos);
            }
        } catch (RuntimeException | LinkageError ex) {
            startupFailed = true;
            lifecycle.startupFailed(ex);
            cleanupOwnedResources();
            throw ex;
        }
    }

    @Override
    public void onDisable() {
        long startedNanos = System.nanoTime();
        lifecycle.shutdown();
        boolean clean = cleanupOwnedResources();
        if (clean && startupComplete && !startupFailed) {
            lifecycle.stopped(startedNanos);
        } else if (!clean) {
            lifecycle.shutdownFailed();
        } else {
            lifecycle.stoppedBeforeReady();
        }
    }

    private FarmerPlugin findFarmer() {
        if (!(getServer().getPluginManager().getPlugin("uxmFarmer") instanceof FarmerPlugin candidate)
            || !candidate.isEnabled()) {
            throw new IllegalStateException("uxmFarmer bulunamadı veya etkin değil.");
        }
        return candidate;
    }

    private boolean cleanupOwnedResources() {
        boolean clean = true;
        AxLootListener listenerToClose = listener;
        listener = null;
        if (listenerToClose != null) {
            try {
                listenerToClose.close();
                lifecycle.info("Yeni aktarım kabulü durduruldu.");
            } catch (RuntimeException | LinkageError ex) {
                clean = false;
                lifecycle.failure("Aktarım kuyruğu yeni girişlere kapatılamadı.", ex);
            }
        }
        BukkitTask tickerToCancel = ticker;
        ticker = null;
        if (listenerToClose != null) {
            try {
                HandlerList.unregisterAll(listenerToClose);
            } catch (RuntimeException | LinkageError ex) {
                clean = false;
                lifecycle.failure("Eşya dinleyicisi kaldırılamadı.", ex);
            }
        }
        if (tickerToCancel != null) {
            try {
                tickerToCancel.cancel();
                lifecycle.info("Aktarım görevi durduruldu.");
            } catch (RuntimeException | LinkageError ex) {
                clean = false;
                lifecycle.failure("Aktarım görevi durdurulamadı.", ex);
            }
        }
        if (listenerToClose != null) {
            try {
                listenerToClose.drain();
                lifecycle.info("Bekleyen aktarımlar tamamlandı.");
            } catch (RuntimeException | LinkageError ex) {
                clean = false;
                lifecycle.failure("Bekleyen aktarımlar tamamlanamadı.", ex);
            }
        }
        return clean;
    }

}
