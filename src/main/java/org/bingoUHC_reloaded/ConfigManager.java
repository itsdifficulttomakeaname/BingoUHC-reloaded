package org.bingoUHC_reloaded;

import com.google.gson.*;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class ConfigManager {
    private final BingoUHC_reloaded plugin;
    private final YamlConfiguration langConfig = new YamlConfiguration();
    private final YamlConfiguration itemConfig = new YamlConfiguration();
    private final YamlConfiguration config;
    private final File itemsFile; // 声明 itemsFile
    private final List<BingoItem> items = new ArrayList<>(); // 声明 items

    public ConfigManager(BingoUHC_reloaded plugin) {
        this.plugin = plugin;
        this.config = (YamlConfiguration) plugin.getConfig();
        this.itemsFile = new File(plugin.getDataFolder(), "items.yml"); // 初始化 itemsFile
    }

    public void loadConfigs() throws Exception {
        loadMainConfig();
        loadLanguageConfig();
        loadItemConfig();
    }

    public void loadItems() {
        FileConfiguration config = YamlConfiguration.loadConfiguration(itemsFile);
        ConfigurationSection itemsSection = config.getConfigurationSection("items");

        if(itemsSection != null) {
            items.clear();
            for(String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSec = itemsSection.getConfigurationSection(key);
                Material material = Material.matchMaterial(itemSec.getString("material"));
                String name = itemSec.getString("name");
                int weight = itemSec.getInt("weight", 1); // 默认权重为1

                if(material != null) {
                    items.add(new BingoItem(material, name, weight));
                }
            }
        }
    }

    public List<BingoItem> getItems() {
        return items;
    }

    private void loadMainConfig() throws Exception {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        plugin.getLogger().info("配置文件路径: " + configFile.getAbsolutePath());

        // 优先检查磁盘文件是否存在
        if (!configFile.exists()) {
            plugin.getLogger().info("未找到配置文件，正在从JAR资源复制...");
            plugin.getDataFolder().mkdirs(); // 确保目录存在

            // 从JAR资源复制到磁盘
            try (InputStream in = plugin.getResource("config.yml")) {
                if (in == null) {
                    throw new FileNotFoundException("JAR中未找到 config.yml");
                }
                Files.copy(in, configFile.toPath());
                plugin.getLogger().info("配置文件复制完成");
            } catch (IOException e) {
                plugin.getLogger().severe("复制配置文件失败: " + e.getMessage());
                throw e;
            }
        }

        // 加载磁盘配置文件
        config.load(configFile);
        plugin.getLogger().info("配置文件加载成功");

        // 加载配置
        YamlConfiguration defConfig = new YamlConfiguration();
        try (InputStream stream = plugin.getResource("config.yml")) {
            defConfig.load(new InputStreamReader(stream));
        }

        // 检查配置版本
        if (config.getInt("config-version", -1) != defConfig.getInt("config-version", 0)) {
            updateConfig(defConfig);
        }
    }

    private void updateConfig(YamlConfiguration defConfig) {
        defConfig.getKeys(true).forEach(key -> {
            if (!config.contains(key)) {
                config.set(key, defConfig.get(key));
            }
        });
        try {
            config.save(new File(plugin.getDataFolder(), "config.yml"));
            plugin.getLogger().warning("Updated config.yml, please check new options");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to update config.yml: " + e.getMessage());
        }
    }

    private void loadLanguageConfig() throws Exception {
        plugin.saveResource("lang.yml", false);
        File langFile = new File(plugin.getDataFolder(), "lang.yml");
        YamlConfiguration defLang = new YamlConfiguration();
        try (InputStream stream = plugin.getResource("lang.yml")) {
            defLang.load(new InputStreamReader(stream));
        }

        langConfig.load(langFile);
        if (langConfig.getInt("config-version", -1) != defLang.getInt("config-version", 0)) {
            updateLanguageConfig(defLang, langFile);
        }
    }

    private void updateLanguageConfig(YamlConfiguration defLang, File langFile) {
        defLang.getKeys(true).forEach(key -> {
            if (!langConfig.contains(key)) {
                langConfig.set(key, defLang.get(key));
            }
        });
        try {
            langConfig.save(langFile);
            plugin.getLogger().warning("Updated lang.yml, please check translations");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to update lang.yml: " + e.getMessage());
        }
    }

    private void loadItemConfig() throws Exception {
        plugin.saveResource("items.yml", plugin.getConfig().getBoolean("auto-rewrite-items", false));
        itemConfig.load(new File(plugin.getDataFolder(), "items.yml"));
    }

    public String translateMessage(String key, String... replacements) {
        String message = langConfig.getString(key, "Missing translation: " + key);
        for (int i = 0; i < replacements.length; i++) {
            message = message.replace("%var" + i + "%", replacements[i]);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String translateItemName(String translationKey, String locale) {
        try {
            String langFile = (locale == null || locale.isEmpty()) ? "en_us" : locale.toLowerCase();
            InputStream stream = plugin.getResource("translations/" + langFile + ".json");
            if (stream == null) stream = plugin.getResource("translations/en_us.json");

            JsonObject json = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();
            return json.get(translationKey).getAsString();
        } catch (Exception e) {
            plugin.getLogger().severe("Translation error: " + e.getMessage());
            return "missing lang";
        }
    }

    public List<String> getItemList() {
        return itemConfig.getStringList("items");
    }

    public Set<String> getLimitedItemGroups() {
        return Objects.requireNonNull(itemConfig.getConfigurationSection("limited-items")).getKeys(false);
    }

    public List<String> getLimitedItems(String group) {
        return itemConfig.getStringList("limited-items." + group + ".items");
    }

    public int getItemLimit(String group) {
        return itemConfig.getInt("limited-items." + group + ".max-limit");
    }

    public Set<String> getDifficultItems() {
        return Objects.requireNonNull(itemConfig.getConfigurationSection("item-difficulties")).getKeys(false);
    }

    public int getItemDifficulty(String item) {
        return itemConfig.getInt("item-difficulties." + item);
    }

    public int getMinPlayers() { return config.getInt("min-players"); }
    public int getMaxPlayers() { return config.getInt("max-players"); }
    public double getPreparingTime() { return config.getDouble("preparing-time"); }
    public double getPanelObservingTime() { return config.getDouble("observing-panel-time"); }
    // 其他配置获取方法...

    public void restartServer() {
        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
                config.getString("bungee-mode.restart-command", "restart"));
    }

    public boolean shouldRemind(double time) {
        return config.getDoubleList("timer-reminder").contains(time);
    }

    public boolean allowTeamDamage() {
        return config.getBoolean("allow-teammate-damage", false);
    }

    public int getRtpCooldown() {
        return config.getInt("random-teleport-cooldown", 300);
    }

    public int getRtpRange() {
        return config.getInt("random-teleport-range", 1000);
    }

    public int getRtpAttempts() {
        return config.getInt("random-teleport-try-limit", 10);
    }

    public List<String> getRtpBlacklistedBiomes() {
        return config.getStringList("random-teleport-blacklist-biomes");
    }

    public void setLobbyWorld(String worldName) {
        config.set("waiting-world", worldName);
        plugin.saveConfig();
    }

    public String getLobbyWorld() {
        return config.getString("waiting-world", "world");
    }

    public int getVoteEndTime() {
        return config.getInt("vote-end-time", 30);
    }

    public int getVoteEndCooldown() {
        return config.getInt("vote-end-cooldown", 60);
    }

    public double getTerrainObservingTime() {
        return config.getDouble("observing-terrain-time", 60.0);
    }

    public int getSpawnRange() {
        return config.getInt("spawn-range", 500);
    }

    public int getObservingYOffset() {
        return config.getInt("observing-terrain-y-offset", 2);
    }
}