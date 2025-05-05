package org.bingoUHC_reloaded;

import org.bukkit.Material;

public class BingoItem {
    private final Material material;
    private final String displayName;
    private final int weight;

    public BingoItem(Material material, String displayName, int weight) {
        this.material = material;
        this.displayName = displayName;
        this.weight = weight;
    }

    // Getter 方法
    public Material getMaterial() {
        return material;
    }

    public String getName() {
        return displayName;
    }

    public int getWeight() {
        return weight;
    }

    // 可以添加toString方法方便调试
    @Override
    public String toString() {
        return "BingoItem{" +
                "material=" + material +
                ", displayName='" + displayName + '\'' +
                ", weight=" + weight +
                '}';
    }
}