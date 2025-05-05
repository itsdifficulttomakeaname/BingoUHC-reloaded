package org.bingoUHC_reloaded;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

public class BingoUtils {
    public static boolean hasSplitPart(int number, int part) {
        if (number < part) return false;
        int highestPower = highestPowerOfTwo(number);
        return highestPower == part || hasSplitPart(number - highestPower, part);
    }

    private static int highestPowerOfTwo(int number) {
        int power = 1;
        while (power * 2 <= number) {
            power *= 2;
        }
        return power;
    }

    public static Set<Integer> getSplitParts(int number) {
        Set<Integer> parts = new LinkedHashSet<>();
        int mask = 1;
        for (int i = 0; i < 31; i++) {
            if ((number & mask) != 0) {
                parts.add(mask);
            }
            mask <<= 1;
        }
        return parts;
    }

    public static void fillMapPixels(int startX, int startY, int endX, int endY,
                                     BufferedImage image, Color color) {
        for (int y = startY; y <= endY; y++) {
            for (int x = startX; x <= endX; x++) {
                image.setRGB(x, y, color.getRGB());
            }
        }
    }

    public static String formatTime(double seconds) {
        int hours = (int) (seconds / 3600);
        int minutes = (int) ((seconds % 3600) / 60);
        int secs = (int) (seconds % 60);
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    public static boolean isBingoLine(int[][] grid, int team, int startX, int startY, int dx, int dy) {
        for (int i = 0; i < 5; i++) {
            int x = startX + i * dx;
            int y = startY + i * dy;
            if (x < 0 || x >= 5 || y < 0 || y >= 5 || (grid[x][y] & team) == 0) {
                return false;
            }
        }
        return true;
    }
}