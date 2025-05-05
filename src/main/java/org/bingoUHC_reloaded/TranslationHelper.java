package org.bingoUHC_reloaded;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.md_5.bungee.api.ChatColor;

public class TranslationHelper {

    public static String getItemName(Material material, String lang) {
        // 创建临时物品获取本地化名称
        ItemStack temp = new ItemStack(material);
        ItemMeta meta = temp.getItemMeta();

        // 英文名称作为fallback
        String defaultName = formatMaterialKey(material);

        if (meta != null) {
            // 获取本地化名称 (依赖客户端语言)
            String localized = meta.getDisplayName();
            if (!localized.isEmpty() && !localized.equals(defaultName)) {
                return ChatColor.RESET + localized;
            }
        }
        return defaultName;
    }

    private static String formatMaterialKey(Material material) {
        String key = material.getKey().getKey();
        return ChatColor.RESET + key.replace('_', ' ');
    }
}