package org.bingoUHC_reloaded;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.*;


import java.awt.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class BingoBoard {
    private final BingoUHC_reloaded plugin;
    private final Material[][] panelItems = new Material[5][5];
    private final BufferedImage[][] panelTextures = new BufferedImage[5][5];
    private final int[][] slotStates = new int[5][5];
    private MapView panel;
    private static final byte[] TEAM_COLORS = {
            MapPalette.matchColor(Color.RED),    // 1 << 0 (红)
            MapPalette.matchColor(Color.YELLOW), // 1 << 1 (黄)
            MapPalette.matchColor(Color.GREEN),  // 1 << 2 (绿)
            MapPalette.matchColor(Color.BLUE)    // 1 << 3 (蓝)
    };
    private static final int ITEM_SPACING = 25; // 物品间距（像素）
    private static final int ITEM_OFFSET = 12;  // 起始偏移量（居中）

    public BingoBoard(BingoUHC_reloaded plugin) {
        this.plugin = plugin;
    }

    private List<BingoTeam> getCollectedTeams(int gridX, int gridY) {
        return plugin.getTeamManager().getTeams().stream()
                .filter(team -> team.hasCollectedItem(gridX, gridY))
                .collect(Collectors.toList());
    }

    public void initializeBoard() {
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                panelItems[j][i] = plugin.getItemManager().getRandomUsableMaterial();
                panelTextures[j][i] = plugin.getItemManager().loadItemTexture(panelItems[j][i]);
                slotStates[j][i] = 0;
            }
        }
        panel = plugin.getServer().createMap(plugin.getServer().getWorlds().get(0));
        resetMapRenderer();
    }

    public ItemStack getPanelMap() {
        ItemStack map = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) map.getItemMeta();
        meta.setMapView(panel);
        map.setItemMeta(meta);
        return map;
    }

    public void resetMapRenderer() {
        panel.getRenderers().clear();
        panel.addRenderer(new MapRenderer() {
            private BufferedImage image = createBoardImage();

            @Override
            public void render(MapView view, MapCanvas canvas, Player player) {
                canvas.drawImage(0, 0, image);
            }

            private BufferedImage createBoardImage() {
                BufferedImage image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = image.createGraphics();

                // 绘制背景
                try {
                    BufferedImage bg = plugin.getItemManager().loadImage("textures/background.png");
                    if (bg != null) g.drawImage(bg, 0, 0, null);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load background: " + e.getMessage());
                }

                // 绘制格子状态
                for (int i = 0; i < 5; i++) {
                    for (int j = 0; j < 5; j++) {
                        drawSlotState(g, j, i);
                        drawItemTexture(g, j, i);
                    }
                }

                g.dispose();
                return image;
            }

            private void fillQuadrant(Graphics2D g, int centerX, int centerY, int radius, int quadrant) {
                for (int dx = 0; dx < radius; dx++) {
                    for (int dy = 0; dy < radius; dy++) {
                        int targetX, targetY;
                        switch (quadrant) {
                            case 0: // 左上
                                targetX = centerX - dx;
                                targetY = centerY - dy;
                                break;
                            case 1: // 右上
                                targetX = centerX + dx;
                                targetY = centerY - dy;
                                break;
                            case 2: // 左下
                                targetX = centerX - dx;
                                targetY = centerY + dy;
                                break;
                            case 3: // 右下
                                targetX = centerX + dx;
                                targetY = centerY + dy;
                                break;
                            default:
                                continue;
                        }
                        g.fillRect(targetX, targetY, 1, 1);
                    }
                }
            }

            private void drawSlotState(Graphics2D g, int x, int y) {
                int state = slotStates[x][y];
                if (state == 0) return;

                int centerX = x * 24 + 12; // 格子中心X
                int centerY = y * 24 + 12; // 格子中心Y
                int radius = 10; // 象限半径

                for (int i = 0; i < 4; i++) {
                    if ((state & (1 << i)) != 0) {
                        g.setColor(new Color(TEAM_COLORS[i]));
                        fillQuadrant(g, centerX, centerY, radius, i);
                    }
                }
            }

            private void drawItemTexture(Graphics2D g, int x, int y) {
                BufferedImage texture = panelTextures[x][y];
                if (texture != null) {
                    g.drawImage(texture, x * 24 + 8, y * 24 + 8, null);
                }
            }
        });
    }

    public boolean checkItemCollection(Player player, Material material) {
        int teamIndex = plugin.getTeamManager().getPlayerTeamIndex(player.getUniqueId());
        if (teamIndex == -1) return false;

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                if (panelItems[j][i] == material) {
                    BingoTeam team = plugin.getTeamManager().getTeams().get(teamIndex);
                    if (!team.hasCollectedItem(j, i)) {
                        team.addCollectedItem(j, i);
                        slotStates[j][i] |= (1 << teamIndex); // 更新位掩码
                        resetMapRenderer();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isTeamBingo(int team) {
        // 检查所有可能的宾果线
        for (int i = 0; i < 5; i++) {
            // 检查行
            if (checkLine(team, i, 0, 0, 1)) return true;
            // 检查列
            if (checkLine(team, 0, i, 1, 0)) return true;
        }
        // 检查对角线
        return checkLine(team, 0, 0, 1, 1) || checkLine(team, 4, 0, -1, 1);
    }

    private boolean checkLine(int team, int startX, int startY, int dx, int dy) {
        for (int i = 0; i < 5; i++) {
            if ((slotStates[startX + i * dx][startY + i * dy] & team) == 0) {
                return false;
            }
        }
        return true;
    }

    public Material getItemAt(int x, int y) {
        return panelItems[x][y];
    }

    /**
     * 获取队伍已收集的物品数量
     */
    public int getCollectedCount(int team) {
        int count = 0;
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                if ((slotStates[i][j] & team) != 0) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * 从指定队伍的面板状态中移除特定物品
     */
    public void removeItemFromSlots(int team, Material material) {
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                if (panelItems[i][j] == material) {
                    slotStates[i][j] &= ~team; // 使用位运算移除该队伍的标记
                }
            }
        }
        resetMapRenderer(); // 更新地图渲染
    }

    private BingoItem getRandomItemWithWeight(List<BingoItem> items) {
        int totalWeight = items.stream().mapToInt(BingoItem::getWeight).sum();
        int random = new Random().nextInt(totalWeight);
        int current = 0;

        for (BingoItem item : items) {
            current += item.getWeight();
            if (random < current) {
                return item;
            }
        }

        return items.get(0); // 默认返回第一个
    }
}