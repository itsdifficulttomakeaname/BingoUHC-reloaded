package org.bingoUHC_reloaded;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class ItemManager {
    private final BingoUHC_reloaded plugin;
    private final int TEAM_SELECTOR_MODEL_DATA = 256;
    private final int DEFAULT_TOOL_MODEL_DATA = 128;
    private final List<Material> usableMaterials = new ArrayList<>();
    private final Map<List<Material>, Integer> limitedMaterials = new HashMap<>();
    private final Map<Material, Integer> itemDifficulties = new HashMap<>();
    private final Map<Integer, List<Material>> difficultyItems = new HashMap<>();
    private final Map<Material, BufferedImage> textureCache = new HashMap<>();

    public ItemManager(BingoUHC_reloaded plugin) {
        this.plugin = plugin;
        initializeDifficultyItems();
    }

    public ItemStack createTeamSelector() {
        ItemStack item = new ItemStack(Material.WHITE_WOOL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().translateMessage("team-selector"));
            meta.setCustomModelData(TEAM_SELECTOR_MODEL_DATA);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createTeamItem(Material material, int team) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(TEAM_SELECTOR_MODEL_DATA);
            meta.setDisplayName(getTeamDisplayName(team));

            // 添加队伍成员到Lore
            if (material != Material.WHITE_WOOL) {
                List<String> lore = new ArrayList<>();
                List<String> members = Collections.singletonList(plugin.getTeamManager().getTeamMembers(team));
                for (String member : members) {
                    lore.add(ChatColor.WHITE + member);
                }
                meta.setLore(lore);
            }

            item.setItemMeta(meta);
        }
        return item;
    }

    private String getTeamDisplayName(int team) {
        return plugin.getConfigManager().translateMessage(switch (team) {
            case 1 -> "red-team";
            case 2 -> "yellow-team";
            case 4 -> "green-team";
            case 8 -> "blue-team";
            default -> "no-team";
        });
    }

    public boolean isTeamSelector(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return (item.getType() == Material.WHITE_WOOL ||
                item.getType() == Material.RED_WOOL ||
                item.getType() == Material.YELLOW_WOOL ||
                item.getType() == Material.GREEN_WOOL ||
                item.getType() == Material.BLUE_WOOL) &&
                meta.hasCustomModelData() &&
                meta.getCustomModelData() == TEAM_SELECTOR_MODEL_DATA;
    }

    public ItemMeta createTeamItemMeta(Material material, int team) {
        ItemMeta meta = plugin.getServer().getItemFactory().getItemMeta(material);
        meta.setCustomModelData(TEAM_SELECTOR_MODEL_DATA);
        meta.setDisplayName(getTeamDisplayName(team));

        if (material != Material.WHITE_WOOL) {
            List<String> lore = new ArrayList<>();
            List<String> members = Collections.singletonList(plugin.getTeamManager().getTeamMembers(team));
            for (String member : members) {
                lore.add(ChatColor.WHITE + member);
            }
            meta.setLore(lore);
        }

        return meta;
    }

    public ItemStack[] getDefaultTools() {
        ConfigurationSection toolsSection = plugin.getConfig().getConfigurationSection("default-tools");
        if (toolsSection == null || toolsSection.getKeys(false).isEmpty()) {
            return new ItemStack[0];
        }

        List<ItemStack> tools = new ArrayList<>();
        for (String toolKey : toolsSection.getKeys(false)) {
            try {
                ItemStack tool = createToolFromConfig(toolsSection.getConfigurationSection(toolKey));
                if (tool != null) {
                    tools.add(tool);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("创建工具 " + toolKey + " 失败: " + e.getMessage());
            }
        }
        return tools.toArray(new ItemStack[0]);
    }

    private ItemStack createToolFromConfig(ConfigurationSection config) {
        if (config == null) return null;

        // 获取基础属性
        Material material = Material.matchMaterial(config.getString("material", ""));
        if (material == null) return null;

        ItemStack tool = new ItemStack(material);
        ItemMeta meta = tool.getItemMeta();
        if (meta == null) return null;

        // 设置显示名称
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                config.getString("name", material.name())));

        // 设置Lore
        List<String> lore = new ArrayList<>();
        for (String line : config.getStringList("lore")) {
            lore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(lore);

        // 设置耐久度
        if (meta instanceof Damageable && config.getInt("durability", 0) > 0) {
            int maxDurability = material.getMaxDurability();
            int damage = maxDurability - config.getInt("durability");
            ((Damageable) meta).setDamage(Math.max(0, Math.min(damage, maxDurability)));
        }

        // 添加附魔
        for (String enchantStr : config.getStringList("enchantments")) {
            try {
                String[] parts = enchantStr.split(" ");
                Enchantment enchant = Enchantment.getByKey(NamespacedKey.minecraft(parts[0].toLowerCase()));
                if (enchant != null) {
                    int level = parseRomanNumeral(parts.length > 1 ? parts[1] : "I");
                    meta.addEnchant(enchant, level, true);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("无效的附魔配置: " + enchantStr);
            }
        }

        tool.setItemMeta(meta);
        return tool;
    }

    private int parseRomanNumeral(String roman) {
        // 简单实现罗马数字转换
        return switch (roman.toUpperCase()) {
            case "I" -> 1;
            case "II" -> 2;
            case "III" -> 3;
            case "IV" -> 4;
            case "V" -> 5;
            case "VI" -> 6;
            case "VII" -> 7;
            case "VIII" -> 8;
            case "IX" -> 9;
            case "X" -> 10;
            case "L" -> 50;
            case "C" -> 100;
            case "D" -> 500;
            case "M" -> 1000;
            case "CCLV" -> 255; // 特殊处理UNBREAKING CCLV
            default -> 1;
        };
    }

    public String getItemDisplayName(Material material, Player player) {
        return TranslationHelper.getItemName(material,
                player != null ? player.getLocale() : "en_us");
    }

    /**
     * 从items.yml加载所有物品配置
     */
    public void loadItems() {
        try {
            File itemFile = new File(plugin.getDataFolder(), "items.yml");
            YamlConfiguration itemConfig = YamlConfiguration.loadConfiguration(itemFile);

            // 加载基础物品列表
            usableMaterials.clear();
            for (String itemName : itemConfig.getStringList("items")) {
                try {
                    usableMaterials.add(Material.valueOf(itemName));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("无效的物品类型: " + itemName);
                }
            }

            // 加载受限物品组
            limitedMaterials.clear();
            ConfigurationSection limitedSection = itemConfig.getConfigurationSection("limited-items");
            if (limitedSection != null) {
                for (String group : limitedSection.getKeys(false)) {
                    List<Material> materials = new ArrayList<>();
                    for (String itemName : itemConfig.getStringList("limited-items." + group + ".items")) {
                        try {
                            materials.add(Material.valueOf(itemName));
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("受限物品组 " + group + " 包含无效物品: " + itemName);
                        }
                    }
                    int limit = itemConfig.getInt("limited-items." + group + ".max-limit", 1);
                    limitedMaterials.put(materials, limit);
                }
            }

            // 加载物品难度
            itemDifficulties.clear();
            difficultyItems.values().forEach(List::clear);
            ConfigurationSection difficultySection = itemConfig.getConfigurationSection("item-difficulties");
            if (difficultySection != null) {
                for (String itemName : difficultySection.getKeys(false)) {
                    try {
                        Material material = Material.valueOf(itemName);
                        int difficulty = itemConfig.getInt("item-difficulties." + itemName, 2);
                        itemDifficulties.put(material, difficulty);
                        difficultyItems.computeIfAbsent(difficulty, k -> new ArrayList<>()).add(material);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("难度配置包含无效物品: " + itemName);
                    }
                }
            }

            plugin.getLogger().info("已加载 " + usableMaterials.size() + " 个可用物品");
        } catch (Exception e) {
            plugin.getLogger().severe("加载物品配置失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeDifficultyItems() {
        for (int i = 0; i <= 4; i++) {
            difficultyItems.put(i, new ArrayList<>());
        }
    }

    public Material getRandomUsableMaterial() {
        if (usableMaterials.isEmpty()) {
            throw new IllegalStateException("没有可用的物品");
        }
        return usableMaterials.get(new Random().nextInt(usableMaterials.size()));
    }

    public List<Material> getUsableMaterials() {
        return Collections.unmodifiableList(usableMaterials);
    }

    /**
     * 加载物品纹理图片
     * @param material 要加载的材质
     * @return 物品的纹理图像，如果加载失败返回null
     */
    public BufferedImage loadItemTexture(Material material) {
        // 检查缓存
        if (textureCache.containsKey(material)) {
            return textureCache.get(material);
        }

        // 从插件资源加载
        String texturePath = "textures/" + material.name().toLowerCase() + ".png";
        try (InputStream stream = plugin.getResource(texturePath)) {
            if (stream != null) {
                BufferedImage image = ImageIO.read(stream);
                textureCache.put(material, image);
                return image;
            }
        } catch (IOException e) {
            plugin.getLogger().warning("无法加载物品纹理: " + material + " - " + e.getMessage());
        }

        // 返回默认的红色错误纹理
        return createErrorTexture();
    }

    /**
     * 通用图片加载方法
     * @param path 图片路径 (相对于插件资源目录)
     * @return 加载的图像，失败时返回错误纹理
     */
    public BufferedImage loadImage(String path) {
        try (InputStream stream = plugin.getResource(path)) {
            if (stream != null) {
                return ImageIO.read(stream);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("无法加载图片: " + path + " - " + e.getMessage());
        }
        return createErrorTexture();
    }

    /**
     * 创建错误提示纹理
     */
    private BufferedImage createErrorTexture() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = image.createGraphics();
        try {
            g.setColor(java.awt.Color.RED);
            g.fillRect(0, 0, 16, 16);
            g.setColor(java.awt.Color.WHITE);
            g.drawString("?", 4, 12);
        } finally {
            g.dispose();
        }
        return image;
    }

    /**
     * 预加载所有物品纹理
     */
    public void preloadTextures() {
        plugin.getLogger().info("正在预加载物品纹理...");
        int loaded = 0;

        for (Material material : usableMaterials) {
            if (loadItemTexture(material) != null) {
                loaded++;
            }
        }

        plugin.getLogger().info("已预加载 " + loaded + "/" + usableMaterials.size() + " 个物品纹理");
    }
}