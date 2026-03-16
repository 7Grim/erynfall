package com.osrs.server.world;

import java.util.*;

/**
 * Breadth-First Search (BFS) pathfinding for OSRS-style movement.
 * Finds shortest path from start tile to target tile.
 */
public class Pathfinding {
    
    private static final int MAX_PATH_LENGTH = 256; // Prevent infinite paths
    
    public static class Tile {
        public int x, y;
        
        public Tile(int x, int y) {
            this.x = x;
            this.y = y;
        }
        
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Tile)) return false;
            Tile t = (Tile) o;
            return x == t.x && y == t.y;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }
    
    private final TileMap tileMap;
    
    public Pathfinding(TileMap tileMap) {
        this.tileMap = tileMap;
    }
    
    /**
     * Find shortest path from start to target using BFS.
     * Returns list of tiles to walk, or empty list if unreachable.
     */
    public List<Tile> findPath(int startX, int startY, int targetX, int targetY) {
        if (startX == targetX && startY == targetY) {
            return new ArrayList<>(); // Already at target
        }
        
        if (!tileMap.isWalkable(targetX, targetY)) {
            return new ArrayList<>(); // Target is not walkable
        }
        
        Queue<Tile> queue = new LinkedList<>();
        Set<Tile> visited = new HashSet<>();
        Map<Tile, Tile> parent = new HashMap<>();
        
        Tile start = new Tile(startX, startY);
        Tile target = new Tile(targetX, targetY);
        
        queue.add(start);
        visited.add(start);
        
        while (!queue.isEmpty()) {
            Tile current = queue.poll();
            
            if (current.equals(target)) {
                // Reconstruct path
                return reconstructPath(parent, target);
            }
            
            // Check all 4 adjacent tiles (cardinal directions)
            Tile[] neighbors = {
                new Tile(current.x + 1, current.y),     // East
                new Tile(current.x - 1, current.y),     // West
                new Tile(current.x, current.y + 1),     // North
                new Tile(current.x, current.y - 1)      // South
            };
            
            for (Tile neighbor : neighbors) {
                if (visited.contains(neighbor)) {
                    continue;
                }
                
                if (!tileMap.isWalkable(neighbor.x, neighbor.y)) {
                    continue;
                }
                
                visited.add(neighbor);
                parent.put(neighbor, current);
                queue.add(neighbor);
            }
        }
        
        // No path found
        return new ArrayList<>();
    }
    
    /**
     * Reconstruct path from parent map.
     */
    private List<Tile> reconstructPath(Map<Tile, Tile> parent, Tile target) {
        List<Tile> path = new ArrayList<>();
        Tile current = target;
        
        while (parent.containsKey(current)) {
            path.add(0, current);
            current = parent.get(current);
        }
        
        // Limit path length (prevent DoS)
        if (path.size() > MAX_PATH_LENGTH) {
            path = path.subList(0, MAX_PATH_LENGTH);
        }
        
        return path;
    }
    
    /**
     * Check if path exists (without returning full path).
     */
    public boolean canReach(int startX, int startY, int targetX, int targetY) {
        return !findPath(startX, startY, targetX, targetY).isEmpty() ||
               (startX == targetX && startY == targetY);
    }
}
