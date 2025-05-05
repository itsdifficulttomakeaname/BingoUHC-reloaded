package org.bingoUHC_reloaded;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bingoUHC_reloaded.GameState;
import java.util.*;

public class GameManager {
    private final BingoUHC_reloaded plugin;
    private GameState gameState;
    private int state = 0;
    private int mode = 0;
    private double timer = -1;
    private double maxTimer = -1;
    private BukkitTask resetTimer;
    private boolean resetState = true;
    private final Set<UUID> resetVotes = new HashSet<>();
    private final Map<UUID, Integer> rtpCooldownPlayers = new HashMap<>();
    private final Map<UUID, Integer> voteDifficulties = new HashMap<>();

    public GameManager(BingoUHC_reloaded plugin) {
        this.plugin = plugin;
    }

    public void perTick() {
        try {
            switch (state) {
                case 0: handleWaitingState(); break;
                case 1: handlePreparingState(); break;
                case 2: handlePanelObservingState(); break;
                case 3: handleTerrainObservingState(); break;
                case 4: handleGameRunningState(); break;
                case 5: handleGameEndState(); break;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Game tick error: " + e.getMessage());
            e.printStackTrace();
            plugin.getServer().shutdown();
        }
    }

    private void handleWaitingState() {
        if (plugin.getServer().getOnlinePlayers().size() >= plugin.getConfigManager().getMinPlayers()) {
            plugin.getServer().getOnlinePlayers().forEach(p ->
                    p.sendMessage(plugin.getConfigManager().translateMessage("start-preparing")));
            maxTimer = timer = plugin.getConfigManager().getPreparingTime();
            state = 1;
        }
    }

    private void handlePreparingState() {
        if (plugin.getServer().getOnlinePlayers().size() < plugin.getConfigManager().getMinPlayers()) {
            plugin.getServer().getOnlinePlayers().forEach(p ->
                    p.sendMessage(plugin.getConfigManager().translateMessage("stop-preparing")));
            state = 0;
        } else if (timer > 0) {
            updateTimer();
            timer -= 0.05;
        } else {
            startPanelObserving();
        }
    }

    private void updateTimer() {
        plugin.getServer().getOnlinePlayers().forEach(p -> {
            p.setLevel((int) timer);
            p.setExp((float) (timer / maxTimer));
            if (plugin.getConfigManager().shouldRemind(timer)) {
                p.sendMessage(plugin.getConfigManager().translateMessage("timer-reminder", String.valueOf((int) timer)));
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            }
        });
    }

    private void startPanelObserving() {
        maxTimer = timer = plugin.getConfigManager().getPanelObservingTime();
        plugin.getBingoBoard().initializeBoard();
        plugin.getServer().getOnlinePlayers().forEach(p -> {
            p.sendMessage(plugin.getConfigManager().translateMessage("observing-panel-started", String.valueOf((int) maxTimer)));
            p.getInventory().setItemInOffHand(plugin.getBingoBoard().getPanelMap());
        });
        state = 2;
    }

    /**
     * 结束游戏主逻辑
     * @param winningTeam 获胜队伍ID（0表示平局）
     */
    public void endGame(int winningTeam) {
        // 1. 异步执行避免卡顿主线程
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // 2. 播放结束动画和音效
                playEndEffects(winningTeam);

                // 4. 延迟后执行服务器重启
                Thread.sleep(10000); // 10秒后重启
                restartServer();
            } catch (Exception e) {
                plugin.getLogger().severe("游戏结束流程异常: " + e.getMessage());
                restartServer(); // 异常时强制重启
            }
        });
    }

    /**
     * 播放结束特效和消息
     */
    private void playEndEffects(int team) throws InterruptedException {
        String color = getTeamColor(team); // 获取队伍颜色代码
        String teamName = plugin.getTeamManager().getTeamDisplayName(team);

        // 1. 全局广播获胜消息
        plugin.getServer().broadcastMessage(
                plugin.getConfigManager().translateMessage(
                        team != 0 ? "game-win" : "game-draw",
                        teamName
                )
        );

        // 2. 播放龙吼音效
        plugin.getServer().getOnlinePlayers().forEach(p -> {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 1.0f, 1.0f);
        });

        // 3. 播放BINGO动画（原代码逻辑）
        playEndAnimation(color);
    }

    /**
     * 服务器重启逻辑
     */
    private void restartServer() {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            // BungeeCord专用传送逻辑（无需单服兼容）
            sendAllToBungeeLobby();

            // 执行重启命令
            plugin.getConfigManager().restartServer();
        });
    }

    /**
     * 通过BungeeCord将所有玩家传送到大厅
     */
    private void sendAllToBungeeLobby() {
        String lobbyName = plugin.getConfig().getString("bungee-mode.lobby-name", "lobby");

        @SuppressWarnings("UnstableApiUsage")
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(lobbyName);

        byte[] data = out.toByteArray();

        plugin.getServer().getOnlinePlayers().forEach(player -> {
            try {
                player.sendPluginMessage(plugin, "BungeeCord", data);
            } catch (Exception e) {
                player.kickPlayer("正在返回大厅...");
            }
        });
    }

    private String getTeamColor(int team) {
        return switch (team) {
            case 1 -> "&4";
            case 2 -> "&e";
            case 4 -> "&a";
            case 8 -> "&3";
            default -> "&f";
        };
    }

    private String formatGameTime() {
        int hours = (int) timer / 3600;
        int minutes = ((int) timer - (3600 * hours)) / 60;
        int seconds = (int) timer % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void sendEndMessages(int team, String time) {
        plugin.getServer().getOnlinePlayers().forEach(p -> {
            p.sendMessage(team != 0 ?
                    plugin.getConfigManager().translateMessage("game-win", plugin.getTeamManager().getTeamDisplayName(team)) :
                    plugin.getConfigManager().translateMessage("game-draw"));
            p.sendMessage(plugin.getConfigManager().translateMessage("end-game-timer", time));
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 1, 1);
        });
    }

    private void playEndAnimation(String color) throws InterruptedException {
        String[] frames = {
                color + "N",
                color + "B        I        N        G        O",
                color + "B       I       N       G       O",
                color + "B      I      N      G      O",
                color + "B     I     N     G     O",
                color + "B    I    N    G    O",
                color + "B   I   N   G   O",
                color + "B  I  N  G  O",
                color + "B I N G O",
                color + "BINGO"
        };

        for (String frame : frames) {
            plugin.getServer().getOnlinePlayers().forEach(p ->
                    p.sendTitle(frame, "", 0, 150, 0));
            Thread.sleep(150);
        }
        Thread.sleep(7000);
    }

    public void cleanup() {
        if (resetTimer != null) resetTimer.cancel();
        resetVotes.clear();
        rtpCooldownPlayers.clear();
        voteDifficulties.clear();
    }

    private void handlePanelObservingState() {
        if (timer > 0) {
            updateTimer();
            timer -= 0.05;
        } else {
            startTerrainObserving();
        }
    }

    private void startTerrainObserving() {
        maxTimer = timer = plugin.getConfigManager().getTerrainObservingTime();
        plugin.getServer().getOnlinePlayers().forEach(p -> {
            p.sendMessage(plugin.getConfigManager().translateMessage("observing-terrain-started",
                    String.valueOf((int)maxTimer)));

            int team = plugin.getTeamManager().getPlayerTeam(p.getUniqueId());
            Location loc = generateTeamLocation(team);
            plugin.getTeamManager().setTeamLocation(team, loc);
            p.teleport(loc);
        });
        state = 3;
    }

    private void handleTerrainObservingState() {
        if (timer > 0) {
            updateTimer();
            timer -= 0.05;
        } else {
            startGame();
        }
    }

    private void startGame() {
        plugin.getServer().getWorlds().forEach(w ->
                w.setGameRule(GameRule.KEEP_INVENTORY, true));

        plugin.getServer().getOnlinePlayers().forEach(p -> {
            p.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOW_FALLING,
                    PotionEffect.INFINITE_DURATION,
                    1, false, false, false));

            // 给予初始工具
            p.getInventory().addItem(plugin.getItemManager().getDefaultTools());
        });
        state = 4;
    }

    private void handleGameRunningState() {
        timer += 0.05;
        updateActionBarTimer();
        checkItemCollections();
        checkBingo();
    }

    private void handleGameEndState() {
        // 1. 防止重复触发结束逻辑
        if (timer < 0) return;

        // 2. 重置计时器标记
        timer = -1;

        // 3. 确定获胜队伍（示例逻辑，实际应根据游戏规则计算）
        int winningTeam = determineWinningTeam();

        // 4. 调用游戏结束主逻辑
        endGame(winningTeam);

        // 5. 执行清理操作
        cleanup();
    }

    /**
     * 确定获胜队伍（示例实现）
     */
    private int determineWinningTeam() {
        // 实际项目中应根据游戏规则计算获胜队伍
        // 这里简化为检查哪个队伍先完成Bingo
        for (int team : new int[]{1, 2, 4, 8}) {
            if (plugin.getBingoBoard().isTeamBingo(team)) {
                return team;
            }
        }
        return 0; // 0表示平局
    }

    // 重置相关方法
    public boolean hasResetVote(UUID uuid) {
        return resetVotes.contains(uuid);
    }

    public boolean isResetState() {
        return resetState;
    }

    public void addResetVote(UUID uuid) {
        resetVotes.add(uuid);
    }

    public int getResetVotesCount() {
        return resetVotes.size();
    }

    public void cancelResetTimer() {
        if (resetTimer != null) {
            resetTimer.cancel();
        }
    }

    public void startResetTimer() {
        resetTimer = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getServer().getOnlinePlayers().forEach(p ->
                    p.sendMessage(plugin.getConfigManager().translateMessage("vote-end-timeout")));
            resetVotes.clear();
            resetState = false;
            resetTimer = plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> resetState = true,
                    20L * plugin.getConfigManager().getVoteEndCooldown());
        }, 20L * plugin.getConfigManager().getVoteEndTime());
    }

    // 随机传送相关方法
    public boolean isOnRtpCooldown(UUID uuid) {
        return rtpCooldownPlayers.containsKey(uuid);
    }

    public int getRtpCooldown(UUID uuid) {
        return rtpCooldownPlayers.getOrDefault(uuid, 0);
    }

    public void setRtpCooldown(UUID uuid, int cooldown) {
        rtpCooldownPlayers.put(uuid, cooldown);
    }

    public boolean randomTeleport(Player player) {
        World world = player.getWorld();
        int range = plugin.getConfigManager().getRtpRange();
        int attempts = plugin.getConfigManager().getRtpAttempts();

        for (int i = 0; i < attempts; i++) {
            int x = player.getLocation().getBlockX() + (int)(Math.random() * range * 2) - range;
            int z = player.getLocation().getBlockZ() + (int)(Math.random() * range * 2) - range;
            Location loc = new Location(world, x, 0, z);
            loc.setY(world.getHighestBlockYAt(loc) + 2);

            if (isValidRtpLocation(loc)) {
                player.teleport(loc);
                return true;
            }
        }
        return false;
    }

    private boolean isValidRtpLocation(Location loc) {
        return !plugin.getConfigManager().getRtpBlacklistedBiomes()
                .contains(loc.getBlock().getBiome().name());
    }

    // 难度投票
    public void setVoteDifficulty(UUID uuid, int difficulty) {
        voteDifficulties.put(uuid, difficulty);
    }

    /**
     * 为队伍生成随机出生位置
     */
    private Location generateTeamLocation(int team) {
        World world = plugin.getServer().getWorlds().get(0);
        int spawnRange = plugin.getConfigManager().getSpawnRange();
        int offset = plugin.getConfigManager().getObservingYOffset();

        // 根据队伍决定生成区域
        int x = 0, z = 0;
        switch (team) {
            case 1: // 红队 - 左上
                x = -spawnRange + new Random().nextInt(spawnRange/2);
                z = -spawnRange + new Random().nextInt(spawnRange/2);
                break;
            case 2: // 黄队 - 右上
                x = new Random().nextInt(spawnRange/2);
                z = -spawnRange + new Random().nextInt(spawnRange/2);
                break;
            case 4: // 绿队 - 左下
                x = -spawnRange + new Random().nextInt(spawnRange/2);
                z = new Random().nextInt(spawnRange/2);
                break;
            case 8: // 蓝队 - 右下
                x = new Random().nextInt(spawnRange/2);
                z = new Random().nextInt(spawnRange/2);
                break;
        }

        Location loc = new Location(world, x, 0, z);
        loc.setY(world.getHighestBlockYAt(loc) + offset);

        // 放置屏障方块
        Location barrierLoc = loc.clone();
        barrierLoc.setY(barrierLoc.getY() - 1);
        barrierLoc.getBlock().setType(Material.BARRIER);

        return loc;
    }

    /**
     * 更新所有玩家的动作条计时器
     */
    private void updateActionBarTimer() {
        int totalSeconds = (int) timer;
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        String timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        String message = plugin.getConfigManager().translateMessage("game-timer", timeString);

        plugin.getServer().getOnlinePlayers().forEach(player -> {
            player.spigot().sendMessage(
                    net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message)
            );
        });
    }

    /**
     * 检查玩家是否收集了面板物品
     */
    private void checkItemCollections() {
        plugin.getServer().getOnlinePlayers().forEach(player -> {
            int team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
            if (team == 0) return; // 观战者不检查

            // 检查背包中所有物品
            for (ItemStack item : player.getInventory().getContents()) {
                if (item == null || item.getType() == Material.AIR) continue;

                // 跳过默认工具
                if (item.hasItemMeta() &&
                        item.getItemMeta().hasCustomModelData() &&
                        item.getItemMeta().getCustomModelData() == 128) {
                    continue;
                }

                // 检查是否匹配面板物品
                Material material = item.getType();
                if (plugin.getBingoBoard().checkItemCollection(player, material)) {
                    // 处理消失模式
                    if (plugin.getGameManager().hasDisappearanceMode()) {
                        plugin.getTeamManager().recordCollectedItem(team, material);
                    }
                }
            }
        });
    }

    /**
     * 检查是否有队伍完成了Bingo
     */
    private void checkBingo() {
        for (int team : new int[]{1, 2, 4, 8}) {
            if (plugin.getBingoBoard().isTeamBingo(team)) {
                // 检查垄断模式
                if (plugin.getGameManager().hasMonopolizationMode()) {
                    int collectedCount = plugin.getBingoBoard().getCollectedCount(team);
                    if (collectedCount >= 13) { // 垄断模式需要收集至少13个
                        endGame(team);
                        return;
                    }
                } else {
                    // 普通Bingo规则
                    endGame(team);
                    return;
                }
            }
        }
    }

    public boolean hasDisappearanceMode() {
        return (mode & 2) != 0;
    }

    public boolean hasMonopolizationMode() {
        return (mode & 1) != 0;
    }

    // Getter/Setter方法...
    public int getGameState() { return state; }
    public void setGameState(GameState state) {
        this.gameState = state;

        if (state == GameState.PREPARING) {
            // 清除所有玩家的队伍选择器
            Bukkit.getOnlinePlayers().forEach(player -> {
                PlayerInventory inventory = player.getInventory();
                // 遍历背包所有物品
                for (ItemStack item : inventory.getContents()) {
                    if (isTeamSelectorItem(item)) {
                        inventory.remove(item); // 移除物品
                    }
                }
            });
        }
    }
    public int getMode() { return mode; }
    public void setMode(int mode) { this.mode = mode; }

    private boolean isTeamSelectorItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        // 检查名称或NBT标签
        return meta.getDisplayName().equals(ChatColor.BLUE + "队伍选择器") ||
                meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "team_selector"), PersistentDataType.STRING);
    }
}