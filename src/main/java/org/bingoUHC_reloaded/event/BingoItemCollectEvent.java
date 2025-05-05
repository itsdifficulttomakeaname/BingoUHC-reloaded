package org.bingoUHC_reloaded.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public class BingoItemCollectEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final ItemStack item;

    public BingoItemCollectEvent(Player player, ItemStack item) {
        this.player = player;
        this.item = item;
    }

    // Bukkit 事件必要方法
    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    // Getter 方法
    public Player getPlayer() {
        return player;
    }

    public ItemStack getItem() {
        return item;
    }
}