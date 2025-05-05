package org.bingoUHC_reloaded;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.*;

public class CommandHandler implements CommandExecutor, TabCompleter {
    private final BingoUHC_reloaded plugin;
    private GameManager gameManager;
    private final List<String> COMMANDS = Arrays.asList(
            "help", "tp", "reset", "rtp", "kill", "players", "panel", "setlobby", "vd");

    public CommandHandler(BingoUHC_reloaded plugin) {
        this.plugin = plugin;
        plugin.getCommand("bingo").setExecutor(this);
        plugin.getCommand("bingo").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command");
            return true;
        }

        String subCmd = args.length > 0 ? args[0].toLowerCase() : "help";
        switch (subCmd) {
            case "help": return handleHelp(player);
            case "tp": return handleTeleport(player, args);
            case "reset": return handleReset(player, args);
            case "rtp": return handleRandomTeleport(player);
            case "kill": return handleKill(player);
            case "players": return handlePlayers(player);
            case "panel": return handlePanel(player);
            case "setlobby": return handleSetLobby(player);
            case "vd": return handleVoteDifficulty(player, args);
            case "check": return handleCheckCommand(sender, args);
            case "join": return handleRegisterCommand((Player) sender);
            case "quit": handleUnregisterCommand((Player) sender);
            default:
                player.sendMessage(plugin.getConfigManager().translateMessage("unknown-command"));
                return true;
        }
    }

    private boolean handleHelp(Player player) {
        player.sendMessage(plugin.getConfigManager().translateMessage("help-message"));
        return true;
    }

    private boolean handleTeleport(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(plugin.getConfigManager().translateMessage("unknown-command"));
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(plugin.getConfigManager().translateMessage("player-non-exists", args[1]));
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().translateMessage("cannot-self-teleport"));
            return true;
        }

        if (!plugin.getTeamManager().isTeammate(player, target)) {
            player.sendMessage(plugin.getConfigManager().translateMessage("player-team-different", args[1]));
            return true;
        }

        player.teleport(target);
        player.sendMessage(plugin.getConfigManager().translateMessage(
                "player-teleported", player.getName(), target.getName()));
        target.sendMessage(plugin.getConfigManager().translateMessage(
                "player-teleported", player.getName(), target.getName()));
        return true;
    }

    private boolean handleReset(Player player, String[] args) {
        // 强制重置
        if (args.length == 2 && args[1].equals("-force")) {
            if (!player.hasPermission("bingo.force-reset")) {
                player.sendMessage(plugin.getConfigManager().translateMessage("no-permission"));
                return true;
            }
            plugin.getGameManager().endGame(0);
            return true;
        }

        // 投票重置
        switch (plugin.getGameManager().getGameState()) {
            case 0: case 1: case 5:
                player.sendMessage(plugin.getConfigManager().translateMessage("vote-end-disabled"));
                break;

            case 2: case 3: case 4:
                if (plugin.getGameManager().hasResetVote(player.getUniqueId())) {
                    player.sendMessage(plugin.getConfigManager().translateMessage("already-voted"));
                    return true;
                }

                if (!plugin.getGameManager().isResetState()) {
                    player.sendMessage(plugin.getConfigManager().translateMessage("vote-end-disabled"));
                    return true;
                }

                plugin.getGameManager().addResetVote(player.getUniqueId());
                int votes = plugin.getGameManager().getResetVotesCount();
                int needed = plugin.getServer().getOnlinePlayers().size();

                plugin.getServer().getOnlinePlayers().forEach(p -> p.sendMessage(
                        plugin.getConfigManager().translateMessage("vote-end", player.getName(), votes + "/" + needed)));

                // 检查是否所有玩家都投票了
                if (votes == needed) {
                    plugin.getGameManager().cancelResetTimer();
                    plugin.getGameManager().endGame(0);
                }

                // 第一次投票时启动计时器
                if (votes == 1) {
                    plugin.getGameManager().startResetTimer();
                }
                break;
        }
        return true;
    }

    private boolean handleRandomTeleport(Player player) {
        // 检查冷却时间
        if (plugin.getGameManager().isOnRtpCooldown(player.getUniqueId())) {
            int cooldown = plugin.getGameManager().getRtpCooldown(player.getUniqueId());
            player.sendMessage(plugin.getConfigManager().translateMessage(
                    "random-teleport-cooldown", String.valueOf(cooldown)));
            return true;
        }

        if (plugin.getGameManager().randomTeleport(player)) {
            player.sendMessage(plugin.getConfigManager().translateMessage(
                    "random-teleported", player.getName()));
            plugin.getGameManager().setRtpCooldown(player.getUniqueId(),
                    plugin.getConfigManager().getRtpCooldown());
            return true;
        } else {
            player.sendMessage(plugin.getConfigManager().translateMessage("cannot-random-teleport"));
            return false;
        }
    }

    private boolean handleKill(Player player) {
        if (plugin.getGameManager().getGameState() == 4) {
            player.setHealth(0);
            return true;
        }
        return false;
    }

    private boolean handlePlayers(Player player) {
        String red = plugin.getTeamManager().getTeamMembers(1);
        String yellow = plugin.getTeamManager().getTeamMembers(2);
        String green = plugin.getTeamManager().getTeamMembers(4);
        String blue = plugin.getTeamManager().getTeamMembers(8);

        player.sendMessage(plugin.getConfigManager().translateMessage(
                "check-players", red, yellow, green, blue));
        return true;
    }

    private boolean handlePanel(Player player) {
        if (plugin.getGameManager().getGameState() >= 2) {
            ItemStack panel = plugin.getBingoBoard().getPanelMap();
            player.getInventory().addItem(panel);
            return true;
        }
        return false;
    }

    private boolean handleSetLobby(Player player) {
        if (!player.hasPermission("bingo.set-lobby")) {
            player.sendMessage(plugin.getConfigManager().translateMessage("no-permission"));
            return true;
        }

        try {
            Location loc = player.getLocation();
            plugin.getConfigManager().setLobbyWorld(loc.getWorld().getName());
            loc.getWorld().setSpawnLocation(loc);
            player.sendMessage(plugin.getConfigManager().translateMessage("lobby-set-success"));
            return true;
        } catch (Exception e) {
            player.sendMessage(plugin.getConfigManager().translateMessage("unexpected-error"));
            return false;
        }
    }

    private boolean handleVoteDifficulty(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(plugin.getConfigManager().translateMessage("unknown-command"));
            return true;
        }

        if (plugin.getGameManager().getGameState() <= 1) {
            try {
                int vote = Integer.parseInt(args[1]);
                if (vote < 0 || vote > 100) throw new NumberFormatException();

                plugin.getGameManager().setVoteDifficulty(player.getUniqueId(), vote);
                player.sendMessage(plugin.getConfigManager().translateMessage("difficulty-voted"));
                return true;
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getConfigManager().translateMessage("unknown-command"));
                return false;
            }
        }
        return false;
    }

    private boolean handleCheckCommand(CommandSender sender, String[] args) {
        if(!(sender instanceof Player)) {
            sender.sendMessage("只有玩家可以执行此命令");
            return true;
        }

        if(args.length != 3) {
            sender.sendMessage("用法: /bingo check <x> <y>");
            return true;
        }

        try {
            int x = Integer.parseInt(args[1]);
            int y = Integer.parseInt(args[2]);

            if(x < 1 || x > 5 || y < 1 || y > 5) {
                sender.sendMessage("坐标必须在1-5之间");
                return true;
            }

            BingoItem item = gameManager.getBingoBoard().get(x-1).get(y-1);
            sender.sendMessage(String.format("坐标(%d,%d)的物品是: %s", x, y, item.getName()));

        } catch (NumberFormatException e) {
            sender.sendMessage("坐标必须是数字");
        }

        return true;
    }

    private boolean handleRegisterCommand(Player player) {
        gameManager.registerPlayer(player);
        player.sendMessage(ChatColor.GREEN + "你已成功报名宾果游戏!");
        return true;
    }

    private boolean handleUnregisterCommand(Player player) {
        gameManager.unregisterPlayer(player);
        player.sendMessage(ChatColor.RED + "你已退出宾果游戏");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // 主命令补全
            for (String command : COMMANDS) {
                if (command.startsWith(args[0].toLowerCase())) {
                    completions.add(command);
                }
            }
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "tp":
                    // 传送命令补全队友
                    plugin.getServer().getOnlinePlayers().forEach(p -> {
                        if (p != sender && sender instanceof Player &&
                                plugin.getTeamManager().isTeammate((Player)sender, p)) {
                            completions.add(p.getName());
                        }
                    });
                    break;

                case "reset":
                    // 重置命令-force补全
                    if (sender.hasPermission("bingo.force-reset")) {
                        completions.add("-force");
                    }
                    break;

                case "check":
                    // 检查命令坐标补全
                    completions.add("<x>");
                    break;

                case "vd":
                    // 难度投票补全
                    completions.add("<0-100>");
                    break;
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("check")) {
            // 检查命令y坐标补全
            completions.add("<y>");
        }

        return completions;
    }
}