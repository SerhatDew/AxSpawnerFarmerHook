package com.blokoyun.farmerhook;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.*;

final class PaperRegistries implements AutoCloseable {
    private final MockedStatic<RegistryAccess> service;

    @SuppressWarnings({"unchecked", "rawtypes", "removal"})
    PaperRegistries() {
        service = mockStatic(RegistryAccess.class);
        RegistryAccess access = mock(RegistryAccess.class);
        service.when(RegistryAccess::registryAccess).thenReturn(access);
        when(access.getRegistry(any(Class.class))).thenAnswer(call -> registry(false));
        when(access.getRegistry(any(RegistryKey.class))).thenAnswer(call ->
            registry(call.getArgument(0).equals(RegistryKey.DATA_COMPONENT_TYPE)));
    }

    private static Object registry(boolean components) {
        Map<NamespacedKey, Object> entries = new HashMap<>();
        return Proxy.newProxyInstance(Registry.class.getClassLoader(), new Class<?>[]{Registry.class},
            (proxy, method, args) -> {
                if (components && method.getName().equals("get")) {
                    NamespacedKey key = (NamespacedKey) args[0];
                    return entries.computeIfAbsent(key, k -> Proxy.newProxyInstance(
                        DataComponentType.class.getClassLoader(),
                        new Class<?>[]{DataComponentType.Valued.class, DataComponentType.NonValued.class},
                        (value, operation, arguments) -> switch (operation.getName()) {
                            case "getKey" -> k;
                            case "hashCode" -> k.hashCode();
                            case "equals" -> value == arguments[0];
                            case "isPersistent" -> true;
                            default -> null;
                        }));
                }
                return null;
            });
    }

    @Override public void close() {
        service.close();
    }
}
