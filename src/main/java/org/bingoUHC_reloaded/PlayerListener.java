package org.bingoUHC_reloaded;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PlayerListener implements Listener {
    private final BingoUHC_reloaded plugin;

    public PlayerListener(BingoUHC_reloaded plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 服务器人数已满
        if (plugin.getServer().getOnlinePlayers().size() >= plugin.getConfigManager().getMaxPlayers()) {
            plugin.getServer().getScheduler().runTask(plugin, () ->
                    player.kickPlayer("Server is full"));
            return;
        }

        switch (plugin.getGameManager().getGameState()) {
            case 0: case 1:
                handleLobbyJoin(player);
                break;
            case 2: case 3: case 4:
                handleMidGameJoin(player);
                break;
            case 5:
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        player.kickPlayer("Server is restarting"));
                break;
        }
    }

    private void handleLobbyJoin(Player player) {
        // 传送到大厅世界
        World world = plugin.getServer().getWorld(plugin.getConfigManager().getLobbyWorld());
        if (world == null) {
            world = plugin.getServer().getWorlds().get(0);
            plugin.getLogger().warning("Lobby world not found, using default world");
        }

        player.teleport(world.getSpawnLocation());
        player.getInventory().clear();
        player.setGameMode(GameMode.SURVIVAL);
        player.setExhaustion(1);

        // 给予队伍选择器
        ItemStack teamSelector = plugin.getItemManager().createTeamSelector();
        plugin.getServer().getScheduler().runTask(plugin, () ->
                player.getInventory().setItem(1, teamSelector));
    }

    private void handleMidGameJoin(Player player) {
        // 中途加入的玩家
        if (!plugin.getTeamManager().hasTeam(player.getUniqueId())) {
            player.setGameMode(GameMode.SPECTATOR);
            plugin.getTeamManager().assignRandomTeam(player);
            player.sendMessage(plugin.getConfigManager().translateMessage("join-halfway"));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 在等待阶段移除退出的玩家
        if (plugin.getGameManager().getGameState() <= 1) {
            plugin.getTeamManager().removePlayer(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();

        // 只检查位置变化，忽略头部转动
        if (to == null || (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ())) {
            return;
        }

        switch (plugin.getGameManager().getGameState()) {
            case 0: case 1: case 2:
                // 防止掉出世界
                if (to.getY() < player.getWorld().getMinHeight()) {
                    player.teleport(player.getWorld().getSpawnLocation());
                }
                break;

            case 3:
                handleTerrainObservingMove(player, to);
                break;

            case 4:
                handleGameMove(player);
                break;
        }
    }

    private void handleTerrainObservingMove(Player player, Location to) {
        int team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        Location teamLoc = plugin.getTeamManager().getTeamLocation(team);

        // 限制玩家移动
        if (!to.toVector().equals(teamLoc.toVector())) {
            Location newLoc = teamLoc.clone();
            newLoc.setYaw(player.getLocation().getYaw());
            newLoc.setPitch(player.getLocation().getPitch());
            player.teleport(newLoc);
        }
    }

    private void handleGameMove(Player player) {
        // 检查是否需要移除缓降效果
        PotionEffect effect = player.getPotionEffect(PotionEffectType.SLOW_FALLING);
        if (effect != null && effect.isInfinite() && player.getFallDistance() == 0) {
            Location below = player.getLocation().clone();
            below.setY(below.getY() - 1);

            if (below.getBlock().getType() != Material.AIR &&
                    below.getBlock().getType() != Material.BARRIER) {
                player.removePotionEffect(PotionEffectType.SLOW_FALLING);
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (plugin.getGameManager().getGameState() == 4) {
            Player player = event.getPlayer();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                // 重新给予缓降效果
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.SLOW_FALLING,
                        PotionEffect.INFINITE_DURATION,
                        1, false, false, false));

                // 传送到队伍位置
                if (player.getBedSpawnLocation() == null) {
                    int team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
                    player.teleport(plugin.getTeamManager().getTeamLocation(team));
                }
            });
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        // 在非游戏阶段取消物品丢弃
        if (plugin.getGameManager().getGameState() == 0 ||
                plugin.getGameManager().getGameState() == 1 ||
                plugin.getGameManager().getGameState() == 5) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (plugin.getGameManager().getGameState() <= 1) {
            if (plugin.getItemManager().isTeamSelector(event.getMainHandItem()) ||
                    plugin.getItemManager().isTeamSelector(event.getOffHandItem())) {
                event.setCancelled(true);
            }
        }
    }
}