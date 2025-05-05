package org.bingoUHC_reloaded;

import org.bukkit.scoreboard.Team;
import java.util.*;

public class BingoTeam {
    private final Team bukkitTeam; // 原生队伍
    private final Set<UUID> players = new HashSet<>();
    private final Set<String> collectedItems = new HashSet<>(); // 格式: "x,y"

    public BingoTeam(Team bukkitTeam) {
        this.bukkitTeam = bukkitTeam;
    }

    // 添加玩家
    public void addPlayer(UUID playerId) {
        players.add(playerId);
    }

    // 检查玩家是否在队伍中
    public boolean hasPlayer(UUID playerId) {
        return players.contains(playerId);
    }

    // 记录收集的物品坐标
    public void addCollectedItem(int x, int y) {
        collectedItems.add(x + "," + y);
    }

    // 检查是否收集过某位置的物品
    public boolean hasCollectedItem(int x, int y) {
        return collectedItems.contains(x + "," + y);
    }

    // 获取原生队伍（用于记分板）
    public Team getBukkitTeam() {
        return bukkitTeam;
    }

    public Set<String> getCollectedItems() {
        return this.collectedItems; // 返回收集的物品坐标集合
    }
}