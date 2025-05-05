package org.bingoUHC_reloaded;

import org.bingoUHC_reloaded.*;

public class BingoAPI {
    private static BingoUHC_reloaded instance;

    public static void setInstance(BingoUHC_reloaded plugin) {
        if (instance != null) {
            throw new IllegalStateException("BingoUHC instance already set");
        }
        instance = plugin;
    }

    public static BingoUHC_reloaded getInstance() {
        if (instance == null) {
            throw new IllegalStateException("BingoUHC instance not initialized");
        }
        return instance;
    }

    public static GameManager getGameManager() {
        return getInstance().getGameManager();
    }

    public static TeamManager getTeamManager() {
        return getInstance().getTeamManager();
    }

    public static ConfigManager getConfigManager() {
        return getInstance().getConfigManager();
    }

    public static ItemManager getItemManager() {
        return getInstance().getItemManager();
    }

    public static BingoBoard getBingoBoard() {
        return getInstance().getBingoBoard();
    }
}