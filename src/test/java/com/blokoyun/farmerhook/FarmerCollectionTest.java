package com.blokoyun.farmerhook;

import com.uxplima.uxmfarmer.FarmerPlugin;
import com.uxplima.uxmfarmer.config.CollectedMaterials;
import com.uxplima.uxmfarmer.entity.Farmer;
import com.uxplima.uxmfarmer.entity.enums.FarmerSetting;
import com.uxplima.uxmfarmer.entity.farmerdata.CollectedItem;
import io.papermc.paper.datacomponent.DataComponentTypes;
import java.util.EnumMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class FarmerCollectionTest {
    private static PaperRegistries registries;

    @BeforeAll static void initializePaperRegistries() {
        registries = new PaperRegistries();
    }

    @AfterAll static void releaseRegistries() {
        registries.close();
    }

    private FarmerPlugin plugin;
    private Farmer farmer;
    private ItemStack item;
    private Location at;
    private FarmerCollection collection;
    private CollectedMaterials.CollectedMaterial product;

    @BeforeEach void setup() {
        plugin = mock(FarmerPlugin.class, RETURNS_DEEP_STUBS);
        farmer = mock(Farmer.class);
        item = mock(ItemStack.class);
        at = new Location(mock(World.class), 1, 64, 1);
        when(plugin.getConfiguration().getWorldManagement().isAllowedInWorld(at.getWorld())).thenReturn(true);
        when(plugin.getRegionHook().getFastFarmer(at)).thenReturn(farmer);
        when(farmer.isFarmerEnabled()).thenReturn(true);
        when(farmer.isSettingEnabled(FarmerSetting.COLLECT_NATURAL_DROPS)).thenReturn(true);
        when(farmer.getCollectedItems()).thenReturn(new EnumMap<>(Material.class));
        when(item.getType()).thenReturn(Material.BONE);
        product = mock(CollectedMaterials.CollectedMaterial.class);
        when(product.getModelData()).thenReturn(-1);
        var products = new EnumMap<Material, CollectedMaterials.CollectedMaterial>(Material.class);
        products.put(Material.BONE, product);
        when(plugin.getCollectedMaterials().getCollectedMaterialsForPerformance()).thenReturn(products);
        collection = new FarmerCollection(plugin);
    }

    @Test void disallowedWorldDoesNotConsumeLoot() {
        when(plugin.getConfiguration().getWorldManagement().isAllowedInWorld(at.getWorld())).thenReturn(false);
        assertEquals(0, collection.collect(at, item, 40));
        verify(plugin.getStorageService(), never()).addProduct(any(), any(), anyInt(), anyBoolean());
    }

    @Test void regionWithoutFarmerDoesNotConsumeLoot() {
        when(plugin.getRegionHook().getFastFarmer(at)).thenReturn(null);
        assertEquals(0, collection.collect(at, item, 40));
    }

    @Test void farmerNaturalDropSwitchIsRespected() {
        when(farmer.isSettingEnabled(FarmerSetting.COLLECT_NATURAL_DROPS)).thenReturn(false);
        assertEquals(0, collection.collect(at, item, 40));
    }

    @Test void missingRequiredModelDoesNotConvertIntoPlainMaterial() {
        when(product.getModelData()).thenReturn(321);
        assertEquals(0, collection.collect(at, item, 40));
    }

    @Test void unexpectedModelDoesNotConvertIntoPlainMaterial() {
        when(item.hasData(DataComponentTypes.CUSTOM_MODEL_DATA)).thenReturn(true);
        assertEquals(0, collection.collect(at, item, 40));
    }

    @Test void disabledProductDoesNotConsumeLoot() {
        CollectedItem stock = mock(CollectedItem.class);
        farmer.getCollectedItems().put(Material.BONE, stock);
        assertEquals(0, collection.collect(at, item, 40));
        verify(plugin.getStorageService(), never()).addProduct(any(), any(), anyInt(), anyBoolean());
    }

    @Test void acceptedLootGoesThroughUxmFarmerStorageWithProduction() {
        when(plugin.getStorageService().addProduct(farmer, Material.BONE, 40, true)).thenReturn(25);
        assertEquals(25, collection.collect(at, item, 40));
    }

    @Test void bonusFromTheCollectEventIsNotSubtractedFromSpawnerLoot() {
        when(plugin.getStorageService().addProduct(farmer, Material.BONE, 40, true)).thenReturn(55);
        assertEquals(40, collection.collect(at, item, 40));
    }

    @Test void namedLootFollowsTheCustomItemSetting() {
        when(item.hasItemMeta()).thenReturn(true);
        when(item.hasData(DataComponentTypes.CUSTOM_NAME)).thenReturn(true);
        when(plugin.getStorageService().addProduct(farmer, Material.BONE, 40, true)).thenReturn(40);
        assertEquals(0, collection.collect(at, item, 40));
        verify(plugin.getStorageService(), never()).addProduct(any(), any(), anyInt(), anyBoolean());

        when(plugin.getGeneralSettings().isCollectCustomItems()).thenReturn(true);
        assertEquals(40, collection.collect(at, item, 40));
    }
}
