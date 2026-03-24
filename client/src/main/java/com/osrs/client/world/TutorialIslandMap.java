package com.osrs.client.world;

/**
 * Generates the Tutorial Island tile layout as a 104×104 int array.
 *
 * Tile type constants (same integers used by IsometricRenderer):
 *   GRASS=0, PATH=1, WATER=2, SAND=3, WALL=4
 */
public class TutorialIslandMap {

    public static final int GRASS = 0;
    public static final int PATH  = 1;
    public static final int WATER = 2;
    public static final int SAND  = 3;
    public static final int WALL  = 4;

    public static final int WIDTH  = 104;
    public static final int HEIGHT = 104;

    public static int[][] generate() {
        int[][] map = new int[WIDTH][HEIGHT];

        // --- Base: all grass ---
        // (default int[] is 0 = GRASS, so nothing to do)

        // --- Water border: outer 5 tiles ---
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (x < 5 || x >= WIDTH - 5 || y < 5 || y >= HEIGHT - 5) {
                    map[x][y] = WATER;
                }
            }
        }

        // --- Sandy beach: 3-tile ring inside water ---
        for (int x = 5; x < WIDTH - 5; x++) {
            for (int y = 5; y < HEIGHT - 5; y++) {
                if (x < 8 || x >= WIDTH - 8 || y < 8 || y >= HEIGHT - 8) {
                    map[x][y] = SAND;
                }
            }
        }

        // --- Main cross paths (3 tiles wide) ---
        // Horizontal: y = 49..51
        for (int x = 8; x < WIDTH - 8; x++) {
            map[x][49] = PATH;
            map[x][50] = PATH;
            map[x][51] = PATH;
        }
        // Vertical: x = 49..51
        for (int y = 8; y < HEIGHT - 8; y++) {
            map[49][y] = PATH;
            map[50][y] = PATH;
            map[51][y] = PATH;
        }

        // --- Guide's area (NE of centre) ---
        // Small hut with PATH floor; NPC 1 stands just west of it at (52,48)
        addBuilding(map, 57, 41, 9, 8);
        // Connecting path spur east from main vertical path
        for (int x = 52; x <= 57; x++) {
            map[x][45] = PATH;
            map[x][46] = PATH;
        }

        // --- Combat training yard (SE of centre) ---
        // NPC 2 is at (55,45) — keep the yard away at (55,57)
        addBuilding(map, 54, 56, 8, 7);
        for (int y = 52; y <= 56; y++) {
            map[56][y] = PATH;
            map[57][y] = PATH;
        }

        // --- Pond in NW quadrant ---
        for (int x = 29; x <= 36; x++) {
            for (int y = 26; y <= 33; y++) {
                map[x][y] = WATER;
            }
        }
        // Sand ring around pond
        for (int x = 27; x <= 38; x++) {
            for (int y = 24; y <= 35; y++) {
                if (map[x][y] == GRASS) map[x][y] = SAND;
            }
        }

        // --- Small island dock in SW water ---
        for (int x = 14; x <= 20; x++) {
            map[x][80] = PATH; // dock planks
            map[x][81] = PATH;
        }

        return map;
    }

    /** Fills a rectangular building: WALL outline + PATH interior. */
    private static void addBuilding(int[][] map, int x0, int y0, int w, int h) {
        for (int x = x0; x < x0 + w; x++) {
            for (int y = y0; y < y0 + h; y++) {
                if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) continue;
                boolean border = (x == x0 || x == x0 + w - 1 || y == y0 || y == y0 + h - 1);
                map[x][y] = border ? WALL : PATH;
            }
        }
    }
}
