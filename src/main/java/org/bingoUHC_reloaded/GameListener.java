package org.bingoUHC_reloaded;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class GameListener implements Listener {
    private final BingoUHC_reloaded plugin;

    public GameListener(BingoUHC_reloaded plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        // 在非游戏阶段取消伤害
        switch (plugin.getGameManager().getGameState()) {
            case 0: case 1: case 2: case 3:
                event.setCancelled(true);
                break;
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // 使用 allowTeamDamage() 方法
        if (plugin.getGameManager().getGameState() == 4) {
            boolean sameTeam = plugin.getTeamManager().isTeammate(
                    (Player)event.getEntity(),
                    (Player)event.getDamager());
            event.setCancelled(sameTeam && !plugin.getConfigManager().allowTeamDamage());
        }
    }

    @EventHandler
    public void onEntityExhaustion(EntityExhaustionEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        // 在非游戏阶段取消饥饿消耗
        switch (plugin.getGameManager().getGameState()) {
            case 0: case 1: case 2: case 3:
                event.setCancelled(true);
                break;
        }
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        // 在非游戏阶段取消物品拾取
        switch (plugin.getGameManager().getGameState()) {
            case 0: case 1: case 2: case 3: case 5:
                event.setCancelled(true);
                break;
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // 在游戏阶段不处理
        if (plugin.getGameManager().getGameState() > 3) return;

        event.setCancelled(true);
        ItemStack item = event.getItem();

        // 处理队伍选择器点击
        if (item != null && plugin.getItemManager().isTeamSelector(item)) {
            openTeamSelectionGUI(event.getPlayer());
        }
    }

    private void openTeamSelectionGUI(Player player) {
        Inventory gui = plugin.getServer().createInventory(
                player, 27, plugin.getConfigManager().translateMessage("team-selector"));

        gui.setItem(10, plugin.getItemManager().createTeamItem(Material.RED_WOOL, 1));
        gui.setItem(12, plugin.getItemManager().createTeamItem(Material.YELLOW_WOOL, 2));
        gui.setItem(14, plugin.getItemManager().createTeamItem(Material.GREEN_WOOL, 4));
        gui.setItem(16, plugin.getItemManager().createTeamItem(Material.BLUE_WOOL, 8));
        gui.setItem(22, plugin.getItemManager().createTeamItem(Material.WHITE_WOOL, 0));

        player.openInventory(gui);
    }

    private ItemStack createTeamItem(Material material, int team) {
        ItemStack item = new ItemStack(material);
        item.setItemMeta(plugin.getItemManager().createTeamItemMeta(material, team));
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // 只处理队伍选择GUI
        if (!event.getView().getTitle().equals(
                plugin.getConfigManager().translateMessage("team-selector"))) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        // 处理队伍选择
        switch (event.getCurrentItem().getType()) {
            case WHITE_WOOL:
                plugin.getTeamManager().removePlayer(player.getUniqueId());
                player.sendMessage(plugin.getConfigManager().translateMessage(
                        "team-selected",
                        plugin.getConfigManager().translateMessage("no-team")));
                break;

            case RED_WOOL:
                plugin.getTeamManager().setPlayerTeam(player.getUniqueId(), 1);
                player.sendMessage(plugin.getConfigManager().translateMessage(
                        "team-selected",
                        plugin.getConfigManager().translateMessage("red-team")));
                break;

            case YELLOW_WOOL:
                plugin.getTeamManager().setPlayerTeam(player.getUniqueId(), 2);
                player.sendMessage(plugin.getConfigManager().translateMessage(
                        "team-selected",
                        plugin.getConfigManager().translateMessage("yellow-team")));
                break;

            case GREEN_WOOL:
                plugin.getTeamManager().setPlayerTeam(player.getUniqueId(), 4);
                player.sendMessage(plugin.getConfigManager().translateMessage(
                        "team-selected",
                        plugin.getConfigManager().translateMessage("green-team")));
                break;

            case BLUE_WOOL:
                plugin.getTeamManager().setPlayerTeam(player.getUniqueId(), 8);
                player.sendMessage(plugin.getConfigManager().translateMessage(
                        "team-selected",
                        plugin.getConfigManager().translateMessage("blue-team")));
                break;
        }

        player.closeInventory();
    }
}