package com.osrs.server.world;

/** A dropped item lying on the ground. Tracks visibility and despawn timing. */
public class GroundItem {
    // Ground items are public to killer for 60s (15360 ticks), then all for 2min, then despawn at 3min total
    public static final long OWNER_ONLY_TICKS = 15_360L;
    public static final long DESPAWN_TICKS    = 46_080L;

    private final int groundItemId;  // server-assigned unique ID
    private final int itemId;
    private int quantity;
    private final int x, y;
    private final int ownerPlayerId; // player who gets exclusive visibility first; -1 = public
    private final long spawnTick;

    public GroundItem(int groundItemId, int itemId, int quantity, int x, int y,
                      int ownerPlayerId, long spawnTick) {
        this.groundItemId  = groundItemId;
        this.itemId        = itemId;
        this.quantity      = quantity;
        this.x             = x;
        this.y             = y;
        this.ownerPlayerId = ownerPlayerId;
        this.spawnTick     = spawnTick;
    }

    public int  getGroundItemId()  { return groundItemId; }
    public int  getItemId()        { return itemId; }
    public int  getQuantity()      { return quantity; }
    public void setQuantity(int q) { this.quantity = q; }
    public int  getX()             { return x; }
    public int  getY()             { return y; }
    public int  getOwnerPlayerId() { return ownerPlayerId; }
    public long getSpawnTick()     { return spawnTick; }

    public boolean isPublic(long currentTick) {
        return ownerPlayerId < 0 || (currentTick - spawnTick) >= OWNER_ONLY_TICKS;
    }

    public boolean isDespawned(long currentTick) {
        return (currentTick - spawnTick) >= DESPAWN_TICKS;
    }
}
