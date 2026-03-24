package com.osrs.shared;

/**
 * Base class for all entities in the game world.
 * This is shared between server and client.
 */
public class Entity {
    
    protected int id;
    protected String name;
    protected int x;
    protected int y;
    protected int facing;      // 0-7 (N, NE, E, SE, S, SW, W, NW)
    protected int spriteId;
    protected int animationFrame;
    protected int health;
    protected int maxHealth;
    
    public Entity(int id, String name, int x, int y) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.facing = 0;
        this.spriteId = 0;
        this.animationFrame = 0;
        this.health = 100;
        this.maxHealth = 100;
    }
    
    public int getId() { return id; }
    public String getName() { return name; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getFacing() { return facing; }
    public int getSpriteId() { return spriteId; }
    public int getAnimationFrame() { return animationFrame; }
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    public void setFacing(int facing) {
        this.facing = Math.max(0, Math.min(7, facing));
    }
    
    public void setSpriteId(int spriteId) {
        this.spriteId = spriteId;
    }
    
    public void setAnimationFrame(int frame) {
        this.animationFrame = frame;
    }
    
    public void setHealth(int health) {
        this.health = Math.max(0, Math.min(maxHealth, health));
    }

    public void setMaxHealth(int maxHealth) {
        this.maxHealth = Math.max(1, maxHealth);
        this.health = this.maxHealth;
    }
}
