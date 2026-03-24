package com.osrs.client.network;

import com.osrs.protocol.NetworkProto;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Handles incoming packets from the server.
 *
 * Entity positions are tracked in thread-safe maps updated by the Netty I/O
 * thread and read by the LibGDX render thread.  GameScreen drains queued
 * events (combat hits) once per frame.
 */
public class ClientPacketHandler extends SimpleChannelInboundHandler<Object> {

    private static final Logger LOG = LoggerFactory.getLogger(ClientPacketHandler.class);

    // -----------------------------------------------------------------------
    // Entity tracking — updated from WorldState + EntityUpdate packets
    // -----------------------------------------------------------------------

    /** Tile position of every server entity, keyed by entity ID. int[]{x, y} */
    private final Map<Integer, int[]> entityPositions = new ConcurrentHashMap<>();

    /** true = player-controlled entity, false = NPC */
    private final Map<Integer, Boolean> entityIsPlayer = new ConcurrentHashMap<>();

    /** Name of each entity (for future name-plates). */
    private final Map<Integer, String> entityNames = new ConcurrentHashMap<>();

    /** Current health of each entity. int[]{hp, maxHp} */
    private final Map<Integer, int[]> entityHealth = new ConcurrentHashMap<>();

    /** The local player's entity ID (from HandshakeResponse). */
    private int myPlayerId = -1;

    // -----------------------------------------------------------------------
    // Per-frame event queues
    // -----------------------------------------------------------------------

    /** Queued hitsplat events — drained by GameScreen each frame. */
    public static class CombatHitEvent {
        public final int targetId;
        public final int targetX, targetY;
        public final int damage;
        public final boolean hit;
        public final int xpAwarded;

        public CombatHitEvent(int targetId, int targetX, int targetY,
                              int damage, boolean hit, int xpAwarded) {
            this.targetId = targetId;
            this.targetX  = targetX;
            this.targetY  = targetY;
            this.damage   = damage;
            this.hit      = hit;
            this.xpAwarded = xpAwarded;
        }
    }

    private final ConcurrentLinkedQueue<CombatHitEvent> pendingCombatHits =
        new ConcurrentLinkedQueue<>();

    // -----------------------------------------------------------------------
    // Player state
    // -----------------------------------------------------------------------

    private int playerHealth = 10;
    private int playerMaxHealth = 10;
    /** Skill levels: 0=Attack, 1=Strength, 2=Defence, 3=Hitpoints, 4=Ranged, 5=Magic */
    private final int[] skillLevels = {1, 1, 1, 1, 1, 1};
    private boolean leveledUp = false;
    private int leveledUpSkill = -1;

    /** Set when the server sends a PlayerDeath packet; cleared by GameScreen after showing death overlay. */
    private volatile boolean playerDead = false;
    private volatile int deathRespawnX = 50;
    private volatile int deathRespawnY = 50;

    private final NettyClient client;
    private NetworkProto.HandshakeResponse lastHandshakeResponse;

    public ClientPacketHandler(NettyClient client) {
        this.client = client;
    }

    // -----------------------------------------------------------------------
    // Packet routing
    // -----------------------------------------------------------------------

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof NetworkProto.ServerMessage)) {
            LOG.warn("Unknown message type: {}", msg.getClass().getSimpleName());
            return;
        }
        NetworkProto.ServerMessage packet = (NetworkProto.ServerMessage) msg;
        switch (packet.getPayloadCase()) {
            case HANDSHAKE_RESPONSE -> handleHandshakeResponse(packet.getHandshakeResponse());
            case WORLD_STATE        -> handleWorldState(packet.getWorldState());
            case ENTITY_UPDATE      -> handleEntityUpdate(packet.getEntityUpdate());
            case COMBAT_HIT         -> handleCombatHit(packet.getCombatHit());
            case HEALTH_UPDATE      -> handleHealthUpdate(packet.getHealthUpdate());
            case SKILL_UPDATE       -> handleSkillUpdate(packet.getSkillUpdate());
            case PLAYER_DEATH       -> handlePlayerDeath(packet.getPlayerDeath());
            default -> LOG.debug("Unhandled server message: {}", packet.getPayloadCase());
        }
    }

    // -----------------------------------------------------------------------
    // Handlers
    // -----------------------------------------------------------------------

    private void handleHandshakeResponse(NetworkProto.HandshakeResponse response) {
        myPlayerId = response.getPlayerId();
        LOG.info("Handshake: playerId={} success={} msg={}",
            myPlayerId, response.getSuccess(), response.getMessage());
        this.lastHandshakeResponse = response;
        client.setConnectedLatch();
    }

    /**
     * WorldState: initial snapshot sent on login. Populates entity map with
     * all NPCs and any other connected players.
     */
    private void handleWorldState(NetworkProto.WorldState worldState) {
        for (NetworkProto.Entity e : worldState.getEntitiesList()) {
            entityPositions.put(e.getId(), new int[]{e.getX(), e.getY()});
            entityIsPlayer.put(e.getId(), e.getIsPlayer());
            entityNames.put(e.getId(), e.getName());
            entityHealth.put(e.getId(), new int[]{e.getHealth(), e.getMaxHealth()});
        }
        LOG.info("WorldState received: {} entities loaded", worldState.getEntitiesCount());
    }

    /**
     * EntityUpdate: live delta sent whenever any entity moves.
     * Only the fields present in the proto oneof are applied.
     */
    private void handleEntityUpdate(NetworkProto.EntityUpdate update) {
        int id = update.getEntityId();
        if (update.hasX() && update.hasY()) {
            entityPositions.put(id, new int[]{update.getX(), update.getY()});
        }
        if (update.hasHealth()) {
            int[] hp = entityHealth.getOrDefault(id, new int[]{100, 100});
            hp[0] = update.getHealth();
            entityHealth.put(id, hp);
        }
        LOG.debug("EntityUpdate id={} pos=({},{})", id,
            update.hasX() ? update.getX() : '?', update.hasY() ? update.getY() : '?');
    }

    private void handleCombatHit(NetworkProto.CombatHit hit) {
        // Update NPC health from the embedded target_health field
        int[] hp = entityHealth.getOrDefault(hit.getTargetId(), new int[]{100, 100});
        hp[0] = hit.getTargetHealth();
        entityHealth.put(hit.getTargetId(), hp);

        pendingCombatHits.add(new CombatHitEvent(
            hit.getTargetId(),
            hit.getTargetX(), hit.getTargetY(),
            hit.getDamage(), hit.getHit(),
            hit.getXpAwarded()
        ));
        LOG.debug("CombatHit: npc={} pos=({},{}) dmg={} hit={}",
            hit.getTargetId(), hit.getTargetX(), hit.getTargetY(),
            hit.getDamage(), hit.getHit());
    }

    private void handleHealthUpdate(NetworkProto.HealthUpdate update) {
        int id = update.getEntityId();
        int[] hp = new int[]{update.getHealth(), update.getMaxHealth()};
        entityHealth.put(id, hp);
        if (id == myPlayerId) {
            playerHealth    = update.getHealth();
            playerMaxHealth = update.getMaxHealth();
        }
        LOG.debug("HealthUpdate entity={} hp={}/{}", id, hp[0], hp[1]);
    }

    private void handleSkillUpdate(NetworkProto.SkillUpdate update) {
        int idx = update.getSkillIndex();
        if (idx >= 0 && idx < skillLevels.length) skillLevels[idx] = update.getNewLevel();
        if (update.getLeveledUp()) {
            leveledUp = true;
            leveledUpSkill = idx;
            LOG.info("LEVEL UP! Skill {} → level {}", idx, update.getNewLevel());
        }
    }

    // -----------------------------------------------------------------------
    // Accessors for GameScreen (called from render thread)
    // -----------------------------------------------------------------------

    /** Drain all pending combat hits for this frame. */
    public List<CombatHitEvent> drainCombatHits() {
        if (pendingCombatHits.isEmpty()) return List.of();
        List<CombatHitEvent> out = new ArrayList<>();
        CombatHitEvent e;
        while ((e = pendingCombatHits.poll()) != null) out.add(e);
        return out;
    }

    /** Current tile position of the given entity, or null if unknown. */
    public int[] getEntityPosition(int entityId) {
        return entityPositions.get(entityId);
    }

    /** All entity positions (snapshot — safe for iteration on render thread). */
    public Map<Integer, int[]> getEntityPositions() { return entityPositions; }

    /** true if the entity is a player, false if NPC. */
    public boolean isPlayer(int entityId) {
        return Boolean.TRUE.equals(entityIsPlayer.get(entityId));
    }

    /** Health of the given entity as {hp, maxHp}, or {100,100} if unknown. */
    public int[] getEntityHealth(int entityId) {
        return entityHealth.getOrDefault(entityId, new int[]{100, 100});
    }

    public int getMyPlayerId()    { return myPlayerId; }
    public int getPlayerHealth()  { return playerHealth; }
    public int getPlayerMaxHealth() { return playerMaxHealth; }
    public int getSkillLevel(int idx) {
        return (idx >= 0 && idx < skillLevels.length) ? skillLevels[idx] : 1;
    }

    public boolean consumeLevelUp() { boolean v = leveledUp; leveledUp = false; return v; }
    public int getLeveledUpSkill()  { return leveledUpSkill; }

    /** True if the player just died; GameScreen calls consumePlayerDeath() to acknowledge. */
    public boolean isPlayerDead()   { return playerDead; }
    public int getDeathRespawnX()   { return deathRespawnX; }
    public int getDeathRespawnY()   { return deathRespawnY; }
    public void consumePlayerDeath() { playerDead = false; }

    public NetworkProto.HandshakeResponse getLastHandshakeResponse() {
        return lastHandshakeResponse;
    }

    private void handlePlayerDeath(NetworkProto.PlayerDeath death) {
        deathRespawnX = death.getRespawnX();
        deathRespawnY = death.getRespawnY();
        playerDead = true;
        LOG.info("Player died — respawning at ({}, {})", deathRespawnX, deathRespawnY);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOG.error("Exception in client packet handler", cause);
        ctx.close();
    }
}
