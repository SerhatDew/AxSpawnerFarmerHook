package com.blokoyun.hooks.axfarmer.integration;

import com.artillexstudios.axspawners.spawner.Spawner;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import org.bukkit.Location;

/** Binds the AxSpawners location API once instead of reflecting for every loot event. */
public final class AxSpawnerApi {
    private final MethodHandle location;

    public AxSpawnerApi() {
        try {
            Method accessor = Spawner.class.getMethod("getLocation");
            Method unwrap = accessor.getReturnType().getMethod("get");
            location = MethodHandles.filterReturnValue(
                MethodHandles.publicLookup().unreflect(accessor),
                MethodHandles.publicLookup().unreflect(unwrap)
            ).asType(MethodType.methodType(Location.class, Spawner.class));
        } catch (ReflectiveOperationException | RuntimeException ex) {
            throw new IllegalStateException("Cannot bind to the AxSpawners location API.", ex);
        }
    }

    public Location location(Spawner spawner) {
        try {
            return (Location) location.invokeExact(spawner);
        } catch (Throwable ex) {
            throw new IllegalStateException("Cannot resolve AxSpawners location.", ex);
        }
    }
}
