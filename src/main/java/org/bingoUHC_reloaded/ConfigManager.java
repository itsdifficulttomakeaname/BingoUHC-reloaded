package org.bingoUHC_reloaded;

import com.google.gson.*;
import org.bukkit.*;
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

    public ConfigManager(BingoUHC_reloaded plugin) {
        this.plugin = plugin;
        this.config = (YamlConfiguration) plugin.getConfig();
    }

    public void loadConfigs() throws Exception {
        loadMainConfig();
        loadLanguageConfig();
        loadItemConfig();
    }

    private void loadMainConfig() throws Exception {
        File configFile = new File(plugin.getDataFolder(), "config.yml");

        // 如果配置文件不存在，或者是一个空文件，就从资源中复制
        if (!configFile.exists() || configFile.length() == 0) {
            // 确保插件数据目录存在
            plugin.getDataFolder().mkdirs();

            // 从 JAR 资源中复制 config.yml
            try (InputStream in = plugin.getResource("config.yml")) {
                if (in == null) {
                    throw new FileNotFoundException("默认 config.yml 未在插件 JAR 中找到！");
                }
                Files.copy(in, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                plugin.getLogger().info("已从资源文件复制默认 config.yml");
            }
        }

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