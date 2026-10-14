package com.blokoyun.farmerhook;

import com.uxplima.uxmfarmer.FarmerPlugin;
import com.uxplima.uxmfarmer.config.CollectedMaterials;
import com.uxplima.uxmfarmer.entity.Farmer;
import com.uxplima.uxmfarmer.entity.enums.FarmerSetting;
import com.uxplima.uxmfarmer.entity.farmerdata.CollectedItem;
import com.uxplima.uxmfarmer.utils.ItemUtils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

final class FarmerCollection {
    private final FarmerPlugin plugin;

    FarmerCollection(FarmerPlugin plugin) {
        this.plugin = plugin;
    }

    int collect(Location location, ItemStack item, int amount) {
        if (amount <= 0 || location == null || location.getWorld() == null
            || !plugin.getConfiguration().getWorldManagement().isAllowedInWorld(location.getWorld())) {
            return 0;
        }
        Farmer farmer = plugin.getRegionHook().getFastFarmer(location);
        if (farmer == null || !farmer.isFarmerEnabled()
            || !farmer.isSettingEnabled(FarmerSetting.COLLECT_NATURAL_DROPS)) {
            return 0;
        }
        Material material = item.getType();
        CollectedMaterials.CollectedMaterial product = plugin.getCollectedMaterials()
            .getCollectedMaterialsForPerformance().get(material);
        if (product == null) {
            return 0;
        }
        if (item.hasItemMeta() && !plugin.getGeneralSettings().isCollectCustomItems()
            && (item.hasData(DataComponentTypes.CUSTOM_NAME) || ItemUtils.hasAnyEnchant(item) || ItemUtils.hasLore(item))) {
            return 0;
        }
        int model = product.getModelData();
        if (model != -1) {
            if (!item.hasItemMeta() || !item.hasData(DataComponentTypes.CUSTOM_MODEL_DATA)) {
                return 0;
            }
            CustomModelData data = item.getData(DataComponentTypes.CUSTOM_MODEL_DATA);
            if (data == null || data.floats().isEmpty() || data.floats().getFirst().intValue() != model) {
                return 0;
            }
        } else if (item.hasData(DataComponentTypes.CUSTOM_MODEL_DATA)) {
            return 0;
        }
        CollectedItem stock = farmer.getCollectedItems().get(material);
        if (stock != null && !stock.isEnabled()) {
            return 0;
        }
        // Preserve uxmFarmer's collection event, statistics, production and auto-sell behavior.
        // An external event handler can grant bonus products; never subtract those from Ax loot.
        int added = plugin.getStorageService().addProduct(farmer, material, amount, true);
        return Math.max(0, Math.min(amount, added));
    }
}
