package org.bingoUHC_reloaded;

import org.bukkit.plugin.java.JavaPlugin;

public final class BingoUHC_reloaded extends JavaPlugin {
    private static BingoUHC_reloaded instance;
    private GameManager gameManager;
    private TeamManager teamManager;
    private ConfigManager configManager;
    private ItemManager itemManager;
    private BingoBoard bingoBoard;
    private CommandHandler commandHandler;
    private PlayerListener playerListener;
    private GameListener gameListener;

    @Override
    public void onLoad() {
        instance = this;
        BingoAPI.setInstance(this);
    }

    @Override
    public void onEnable() {
        // 初始化管理器
        this.configManager = new ConfigManager(this);
        this.itemManager = new ItemManager(this);
        this.teamManager = new TeamManager(this);
        this.bingoBoard = new BingoBoard(this);
        this.gameManager = new GameManager(this);

        // 初始化处理器
        this.commandHandler = new CommandHandler(this);
        this.playerListener = new PlayerListener(this);
        this.gameListener = new GameListener(this);

        // 加载配置
        try {
            configManager.loadConfigs();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        itemManager.loadItems();
        itemManager.preloadTextures();

        // 启动游戏循环
        getServer().getScheduler().runTaskTimer(this, gameManager::perTick, 1L, 1L);

        // 检查BungeeCord配置
        if (!getConfig().isSet("bungee-mode.lobby-name")) {
            getLogger().warning("未配置bungee-mode.lobby-name，将使用默认值'lobby'");
            getConfig().set("bungee-mode.lobby-name", "lobby");
        }

        // 注册BungeeCord通道
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
    }

    @Override
    public void onDisable() {
        gameManager.cleanup();
    }

    public static BingoUHC_reloaded getInstance() { return instance; }
    public GameManager getGameManager() { return gameManager; }
    public TeamManager getTeamManager() { return teamManager; }
    public ConfigManager getConfigManager() { return configManager; }
    public ItemManager getItemManager() { return itemManager; }
    public BingoBoard getBingoBoard() { return bingoBoard; }
    public CommandHandler getCommandHandler() { return commandHandler; }
}