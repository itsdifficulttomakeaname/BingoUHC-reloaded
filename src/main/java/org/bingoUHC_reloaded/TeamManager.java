package org.bingoUHC_reloaded;

import org.bukkit.*;
import org.bukkit.entity.Player;
import java.util.*;

public class TeamManager {
    private final BingoUHC_reloaded plugin;
    private final Map<UUID, Integer> playerTeams = new HashMap<>();
    private final Map<Integer, Location> teamLocations = new HashMap<>();
    private final Map<Integer, List<Material>> teamDisappearanceRecorder = new HashMap<>();

    public TeamManager(BingoUHC_reloaded plugin) {
        this.plugin = plugin;
        initializeTeamRecorders();
    }

    private void initializeTeamRecorders() {
        teamDisappearanceRecorder.put(1, new ArrayList<>());
        teamDisappearanceRecorder.put(2, new ArrayList<>());
        teamDisappearanceRecorder.put(4, new ArrayList<>());
        teamDisappearanceRecorder.put(8, new ArrayList<>());
    }

    public void assignRandomTeam(Player player) {
        int team = (int) Math.pow(2, new Random().nextInt(4));
        playerTeams.put(player.getUniqueId(), team);
        player.sendMessage(plugin.getConfigManager().translateMessage("random-teamed", getTeamDisplayName(team)));
    }

    public String getTeamDisplayName(int team) {
        return plugin.getConfigManager().translateMessage(switch (team) {
            case 1 -> "red-team";
            case 2 -> "yellow-team";
            case 4 -> "green-team";
            case 8 -> "blue-team";
            default -> "no-team";
        });
    }

    public void setTeamLocation(int team, Location location) {
        teamLocations.put(team, location);
    }

    public Location getTeamLocation(int team) {
        return teamLocations.get(team);
    }

    public int getPlayerTeam(UUID playerId) {
        return playerTeams.getOrDefault(playerId, 0);
    }

    private void handleDisappearance(int team, Material material) {
        plugin.getServer().getOnlinePlayers().forEach(p -> {
            if (getPlayerTeam(p.getUniqueId()) == team) {
                p.getInventory().remove(material);
            }
        });

        plugin.getServer().getOnlinePlayers().forEach(p ->
                p.sendMessage(plugin.getConfigManager().translateMessage("item-disappeared",
                        getTeamDisplayName(team),
                        plugin.getConfigManager().translateItemName(material.getItemTranslationKey(), p.getLocale()))));
    }

    public boolean isTeammate(Player p1, Player p2) {
        return getPlayerTeam(p1.getUniqueId()) == getPlayerTeam(p2.getUniqueId());
    }

    public String getTeamMembers(int team) {
        StringBuilder members = new StringBuilder();
        playerTeams.forEach((uuid, t) -> {
            if (t == team) {
                Player p = plugin.getServer().getPlayer(uuid);
                if (p != null) members.append(p.getName()).append(" ");
            }
        });
        return members.toString().trim();
    }

    public boolean hasTeam(UUID uuid) {
        return playerTeams.containsKey(uuid);
    }

    public void removePlayer(UUID uuid) {
        playerTeams.remove(uuid);
    }

    public void setPlayerTeam(UUID uuid, int team) {
        playerTeams.put(uuid, team);
    }


    // 修复弃用的 getTranslationKey() 方法
    public String getTeamColorCode(int team) {
        return switch (team) {
            case 1 -> "&4";
            case 2 -> "&e";
            case 4 -> "&a";
            case 8 -> "&3";
            default -> "&f";
        };
    }

    /**
     * 记录队伍收集的物品（用于消失模式）
     */
    public void recordCollectedItem(int team, Material material) {
        List<Material> collected = teamDisappearanceRecorder.get(team);
        collected.add(material);

        // 检查是否达到消失阈值
        if (collected.size() >= 6) {
            Material toRemove = collected.remove(0);
            removeCollectedItem(team, toRemove);
        }
    }

    private void removeCollectedItem(int team, Material material) {
        // 从所有该队伍玩家的背包中移除物品
        /*
            playerTeams.forEach((uuid, t) -> {
                if (t == team) {
                    Player player = plugin.getServer().getPlayer(uuid);
                    if (player != null) {
                        player.getInventory().remove(material);
                    }
                }
            });
        */
        // 从面板状态中移除
        plugin.getBingoBoard().removeItemFromSlots(team, material);

        // 广播消息
        String message = plugin.getConfigManager().translateMessage(
                "item-disappeared",
                getTeamDisplayName(team),
                TranslationHelper.getItemName(material, null) // 使用服务器默认语言
        );
        plugin.getServer().broadcastMessage(message);
    }
}