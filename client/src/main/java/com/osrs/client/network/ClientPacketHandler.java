package com.osrs.client.network;

import com.osrs.protocol.NetworkProto;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
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

    /** Name of each entity. */
    private final Map<Integer, String> entityNames = new ConcurrentHashMap<>();

    /** Combat level of each entity (0 for players). */
    private final Map<Integer, Integer> entityCombatLevels = new ConcurrentHashMap<>();

    /** Definition ID for each NPC/resource entity (0 for players/unknown). */
    private final Map<Integer, Integer> entityDefinitionIds = new ConcurrentHashMap<>();

    /** Current health of each entity. int[]{hp, maxHp} */
    private final Map<Integer, int[]> entityHealth = new ConcurrentHashMap<>();

    /** The local player's entity ID (from HandshakeResponse). */
    private int myPlayerId = -1;

    // -----------------------------------------------------------------------
    // NPC despawn queue — drained by GameScreen each frame
    // -----------------------------------------------------------------------

    /** IDs of NPCs that just died, so GameScreen can remove their visuals. */
    private final ConcurrentLinkedQueue<Integer> despawnedNpcQueue = new ConcurrentLinkedQueue<>();

    // -----------------------------------------------------------------------
    // Per-frame event queues
    // -----------------------------------------------------------------------

    /** Queued hitsplat events — drained by GameScreen each frame. */
    public static class CombatHitEvent {
        public final int targetId;
        public final int targetX, targetY;
        public final int attackerX, attackerY;
        public final int projectileType;   // 0=melee/instant, 1=arrow, future: bolt/spell
        public final int damage;
        public final boolean hit;
        public final int xpAwarded;

        public CombatHitEvent(int targetId, int targetX, int targetY,
                              int attackerX, int attackerY, int projectileType,
                              int damage, boolean hit, int xpAwarded) {
            this.targetId      = targetId;
            this.targetX       = targetX;
            this.targetY       = targetY;
            this.attackerX     = attackerX;
            this.attackerY     = attackerY;
            this.projectileType = projectileType;
            this.damage        = damage;
            this.hit           = hit;
            this.xpAwarded     = xpAwarded;
        }
    }

    private final ConcurrentLinkedQueue<CombatHitEvent> pendingCombatHits =
        new ConcurrentLinkedQueue<>();

    /** Dialogue prompts sent by the server — drained by GameScreen each frame. */
    public static class DialoguePromptEvent {
        public static class DialogueOptionEvent {
            public final int optionId;
            public final String text;

            public DialogueOptionEvent(int optionId, String text) {
                this.optionId = optionId;
                this.text = text;
            }
        }

        public final int npcId;
        public final String npcText;
        public final List<DialogueOptionEvent> options;

        public DialoguePromptEvent(int npcId, String npcText, List<DialogueOptionEvent> options) {
            this.npcId = npcId;
            this.npcText = npcText;
            this.options = options;
        }
    }

    private final ConcurrentLinkedQueue<DialoguePromptEvent> pendingDialoguePrompts =
        new ConcurrentLinkedQueue<>();

    /** Quest updates sent by server — drained by GameScreen each frame. */
    public static class QuestUpdateEvent {
        public enum Status {
            NOT_STARTED,
            IN_PROGRESS,
            COMPLETED
        }

        public static class TaskEvent {
            public final String taskId;
            public final String description;
            public final int currentCount;
            public final int requiredCount;
            public final boolean completed;

            public TaskEvent(String taskId, String description, int currentCount, int requiredCount, boolean completed) {
                this.taskId = taskId;
                this.description = description;
                this.currentCount = currentCount;
                this.requiredCount = requiredCount;
                this.completed = completed;
            }
        }

        public final int questId;
        public final String questName;
        public final String questDescription;
        public final int tasksCompleted;
        public final int tasksTotal;
        public final boolean completed;
        public final boolean miniquest;
        public final int questPointsReward;
        public final int playerTotalQuestPoints;
        public final Status status;
        public final List<TaskEvent> tasks;

        public QuestUpdateEvent(int questId, String questName, String questDescription,
                                int tasksCompleted, int tasksTotal, boolean completed,
                                boolean miniquest, int questPointsReward, int playerTotalQuestPoints,
                                Status status, List<TaskEvent> tasks) {
            this.questId = questId;
            this.questName = questName;
            this.questDescription = questDescription;
            this.tasksCompleted = tasksCompleted;
            this.tasksTotal = tasksTotal;
            this.completed = completed;
            this.miniquest = miniquest;
            this.questPointsReward = questPointsReward;
            this.playerTotalQuestPoints = playerTotalQuestPoints;
            this.status = status;
            this.tasks = tasks;
        }
    }

    private final ConcurrentLinkedQueue<QuestUpdateEvent> pendingQuestUpdates =
        new ConcurrentLinkedQueue<>();

    public static class LogoutEvent {
        public final boolean success;
        public final String message;

        public LogoutEvent(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    private volatile LogoutEvent pendingLogoutEvent;

    public static class SkillingStateEvent {
        public final NetworkProto.SkillingType type;
        public final NetworkProto.SkillingState state;
        public final int targetNpcId;
        public final String message;

        public SkillingStateEvent(NetworkProto.SkillingType type,
                                  NetworkProto.SkillingState state,
                                  int targetNpcId,
                                  String message) {
            this.type = type;
            this.state = state;
            this.targetNpcId = targetNpcId;
            this.message = message == null ? "" : message;
        }
    }

    private final ConcurrentLinkedQueue<SkillingStateEvent> pendingSkillingStates =
        new ConcurrentLinkedQueue<>();

    // -----------------------------------------------------------------------
    // Player state
    // -----------------------------------------------------------------------

    private int playerHealth = 10;
    private int playerMaxHealth = 10;
    /** Skill levels by shared protocol index. */
    private final int[]  skillLevels   = {1,1,1,1,1,1,1,1,1,1, 1,1,1,1,1,1,1,1,1,1, 1,1,1};
    private final long[] skillTotalXp  = new long[23];
    private boolean localPlayerIsMember = false;
    private boolean localPlayerIsAdminToolsEnabled = false;
    private String lastAdminActionMessage = "";
    private boolean lastAdminActionSuccess = false;
    private boolean adminTeleportApplied = false;
    private int adminTeleportX = -1;
    private int adminTeleportY = -1;
    public static final class AdminItemSearchEntry {
        public final int itemId;
        public final String itemName;
        public final int flags;

        public AdminItemSearchEntry(int itemId, String itemName, int flags) {
            this.itemId = itemId;
            this.itemName = itemName == null ? "" : itemName;
            this.flags = flags;
        }
    }
    private final java.util.List<AdminItemSearchEntry> adminItemSearchResults = new java.util.ArrayList<>();
    private String lastAdminItemSearchQuery = "";
    /** Level-up events queued for the render thread. One entry per level gained. */
    public static class LevelUpEvent {
        public final int skillIndex;
        public final int newLevel;
        public LevelUpEvent(int skillIndex, int newLevel) {
            this.skillIndex = skillIndex;
            this.newLevel   = newLevel;
        }
    }

    private final ConcurrentLinkedQueue<LevelUpEvent> pendingLevelUps = new ConcurrentLinkedQueue<>();

    /** XP gain events queued for the render thread to show as floating text. */
    public static class XpDropEvent {
        public final int  skillIndex;
        public final long xpGained;
        public XpDropEvent(int skillIndex, long xpGained) {
            this.skillIndex = skillIndex;
            this.xpGained  = xpGained;
        }
    }

    private final ConcurrentLinkedQueue<XpDropEvent> pendingXpDrops = new ConcurrentLinkedQueue<>();
    private boolean suppressSkillDropsUntilWorldState = true;

    /** Set when the server sends a PlayerDeath packet; cleared by GameScreen after showing death overlay. */
    private volatile boolean playerDead = false;
    private volatile int deathRespawnX = 50;
    private volatile int deathRespawnY = 50;

    // -----------------------------------------------------------------------
    // Inventory state (28 slots)
    // -----------------------------------------------------------------------
    private final int[]    inventoryItemIds    = new int[28];
    private final int[]    inventoryQuantities = new int[28];
    private final String[] inventoryNames      = new String[28];
    private final int[]    inventoryFlags      = new int[28];

    // -----------------------------------------------------------------------
    // Equipment state (11 slots)
    // -----------------------------------------------------------------------
    private final int[]    equipmentItemIds = new int[11];
    private final String[] equipmentNames   = new String[11];
    private final int[] equipBonuses = new int[14]; // indices 0-13: stab_attack...prayer
    /** Current weapon attack range; 1 = melee, 7 = shortbow/magic staff, etc. */
    private volatile int playerAttackRange = 1;
    /** Currently selected auto-cast spell ID (-1 = none). */
    private volatile int selectedSpellId = -1;

    // -----------------------------------------------------------------------
    // Ground items: groundItemId → int[]{itemId, quantity, x, y}
    // -----------------------------------------------------------------------
    private final Map<Integer, int[]>    groundItems     = new ConcurrentHashMap<>();
    private final Map<Integer, String>   groundItemNames = new ConcurrentHashMap<>();

    private final NettyClient client;
    private NetworkProto.HandshakeResponse lastHandshakeResponse;

    public static class BankSlotSnapshot {
        public final int slot;
        public final int tabIndex;
        public final int itemId;
        public final long quantity;
        public final String itemName;
        public final int flags;

        public BankSlotSnapshot(int slot, int tabIndex, int itemId, long quantity, String itemName, int flags) {
            this.slot = slot;
            this.tabIndex = tabIndex;
            this.itemId = itemId;
            this.quantity = quantity;
            this.itemName = itemName == null ? "" : itemName;
            this.flags = flags;
        }
    }

    private final Object bankLock = new Object();
    private final List<BankSlotSnapshot> bankSlots = new ArrayList<>();
    private volatile boolean bankOpen = false;
    private volatile int bankCapacity = 0;

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
            case HANDSHAKE_RESPONSE  -> handleHandshakeResponse(packet.getHandshakeResponse());
            case WORLD_STATE         -> handleWorldState(packet.getWorldState());
            case ENTITY_UPDATE       -> handleEntityUpdate(packet.getEntityUpdate());
            case COMBAT_HIT          -> handleCombatHit(packet.getCombatHit());
            case HEALTH_UPDATE       -> handleHealthUpdate(packet.getHealthUpdate());
            case SKILL_UPDATE           -> handleSkillUpdate(packet.getSkillUpdate());
            case PRAYER_POINTS_UPDATE   -> pendingPrayerPoints.add(new PrayerPointsEvent(
                packet.getPrayerPointsUpdate().getCurrent(),
                packet.getPrayerPointsUpdate().getMaximum()));
            case DIALOGUE_PROMPT     -> handleDialoguePrompt(packet.getDialoguePrompt());
            case QUEST_UPDATE        -> handleQuestUpdate(packet.getQuestUpdate());
            case PLAYER_DEATH        -> handlePlayerDeath(packet.getPlayerDeath());
            case INVENTORY_UPDATE    -> handleInventoryUpdate(packet.getInventoryUpdate());
            case EQUIPMENT_UPDATE    -> handleEquipmentUpdate(packet.getEquipmentUpdate());
            case EQUIPMENT_BONUSES   -> handleEquipmentBonuses(packet.getEquipmentBonuses());
            case GROUND_ITEM_SPAWN   -> handleGroundItemSpawn(packet.getGroundItemSpawn());
            case GROUND_ITEM_DESPAWN -> handleGroundItemDespawn(packet.getGroundItemDespawn());
            case NPC_DESPAWN         -> handleNpcDespawn(packet.getNpcDespawn());
            case NPC_RESPAWN         -> handleNpcRespawn(packet.getNpcRespawn());
            case CHAT_MESSAGE        -> handleChatMessage(packet.getChatMessage());
            case CHAT_BROADCAST      -> handleChatBroadcast(packet.getChatBroadcast());  // public_chat from nearby player
            case LOGOUT_RESPONSE     -> handleLogoutResponse(packet.getLogoutResponse());
            case SKILLING_STATE_UPDATE -> handleSkillingStateUpdate(packet.getSkillingStateUpdate());
            case FRIENDS_LIST_UPDATE -> handleFriendsListUpdate(packet.getFriendsListUpdate());
            case FRIEND_ACTION_RESULT -> handleFriendActionResult(packet.getFriendActionResult());
            case BANK_OPEN -> handleBankOpen(packet.getBankOpen());
            case BANK_CLOSE -> handleBankClose(packet.getBankClose());
            case BANK_SLOT_UPDATE -> handleBankSlotUpdate(packet.getBankSlotUpdate());
            case ADMIN_ACTION_RESULT -> handleAdminActionResult(packet.getAdminActionResult());
            case ADMIN_ITEM_SEARCH_RESULTS -> handleAdminItemSearchResults(packet.getAdminItemSearchResults());
            case ADMIN_TELEPORT_APPLIED -> handleAdminTeleportApplied(packet.getAdminTeleportApplied());
            default -> LOG.debug("Unhandled server message: {}", packet.getPayloadCase());
        }
    }

    // -----------------------------------------------------------------------
    // Handlers
    // -----------------------------------------------------------------------

    private void handleHandshakeResponse(NetworkProto.HandshakeResponse response) {
        LOG.info("Handshake response: success={} message='{}' playerId={} member={} adminToolsEnabled={}",
            response.getSuccess(),
            response.getMessage(),
            response.getPlayerId(),
            response.getIsMember(),
            response.getIsAdminToolsEnabled());
        if (response.getSuccess()) {
            myPlayerId = response.getPlayerId();
            localPlayerIsMember = response.getIsMember();
            localPlayerIsAdminToolsEnabled = response.getIsAdminToolsEnabled();
            // Restore persisted spell selection from server
            selectedSpellId = response.getSelectedSpellId();
        } else {
            localPlayerIsAdminToolsEnabled = false;
        }
        this.lastHandshakeResponse = response;
        client.setHandshakeResponse(response);
    }

    private void handleAdminActionResult(NetworkProto.AdminActionResult result) {
        lastAdminActionSuccess = result.getSuccess();
        lastAdminActionMessage = result.getMessage();
        LOG.info("Admin action result: success={} message='{}'", result.getSuccess(), result.getMessage());
    }

    private void handleAdminItemSearchResults(NetworkProto.AdminItemSearchResults result) {
        synchronized (adminItemSearchResults) {
            adminItemSearchResults.clear();
            for (NetworkProto.AdminItemSearchEntry entry : result.getEntriesList()) {
                adminItemSearchResults.add(new AdminItemSearchEntry(
                    entry.getItemId(),
                    entry.getItemName(),
                    entry.getFlags()
                ));
            }
        }
        lastAdminItemSearchQuery = result.getQuery();
        LOG.info("Admin item search results: query='{}' count={}", result.getQuery(), result.getEntriesCount());
    }

    private void handleAdminTeleportApplied(NetworkProto.AdminTeleportApplied applied) {
        adminTeleportX = applied.getX();
        adminTeleportY = applied.getY();
        adminTeleportApplied = true;
        LOG.info("Admin teleport applied: ({}, {})", adminTeleportX, adminTeleportY);
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
            entityCombatLevels.put(e.getId(), e.getCombatLevel());
            entityDefinitionIds.put(e.getId(), e.getDefinitionId());
            entityHealth.put(e.getId(), new int[]{e.getHealth(), e.getMaxHealth()});
        }
        LOG.info("WorldState received: {} entities loaded", worldState.getEntitiesCount());
        suppressSkillDropsUntilWorldState = false;
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
        if (update.hasIsPlayer()) {
            entityIsPlayer.put(id, update.getIsPlayer());
        }
        if (update.hasName() && !update.getName().isEmpty()) {
            entityNames.put(id, update.getName());
        }
        if (update.hasDefinitionId()) {
            entityDefinitionIds.put(id, update.getDefinitionId());
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
            hit.getAttackerX(), hit.getAttackerY(),
            hit.getProjectileType(),
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
        if (idx >= 0 && idx < skillLevels.length) {
            int oldLevel = skillLevels[idx];
            skillLevels[idx] = update.getNewLevel();
            long newTotalXp = update.getTotalXp();
            if (suppressSkillDropsUntilWorldState) {
                skillTotalXp[idx] = newTotalXp;
            } else {
                long gained = newTotalXp - skillTotalXp[idx];
                if (gained > 0) {
                    pendingXpDrops.add(new XpDropEvent(idx, gained));
                }
                skillTotalXp[idx] = newTotalXp;
                if (update.getLeveledUp()) {
                    int newLevel = update.getNewLevel();
                    // Queue one banner per level gained so multi-level jumps
                    // (e.g. admin XP drop from 40→45) show 5 sequential banners.
                    int levelsGained = Math.max(1, newLevel - oldLevel);
                    for (int lvl = newLevel - levelsGained + 1; lvl <= newLevel; lvl++) {
                        pendingLevelUps.add(new LevelUpEvent(idx, lvl));
                    }
                    LOG.info("Level up! Skill {} → {} ({} level(s))", idx, newLevel, levelsGained);
                }
            }
        }
    }

    private void handleDialoguePrompt(NetworkProto.DialoguePrompt prompt) {
        List<DialoguePromptEvent.DialogueOptionEvent> options = new ArrayList<>();
        for (NetworkProto.DialogueOption option : prompt.getOptionsList()) {
            options.add(new DialoguePromptEvent.DialogueOptionEvent(option.getOptionId(), option.getText()));
        }

        pendingDialoguePrompts.add(new DialoguePromptEvent(prompt.getNpcId(), prompt.getNpcSays(), options));
        LOG.debug("DialoguePrompt npcId={} options={}", prompt.getNpcId(), options.size());
    }

    private void handleQuestUpdate(NetworkProto.QuestUpdate update) {
        List<QuestUpdateEvent.TaskEvent> tasks = new ArrayList<>();
        for (NetworkProto.QuestTaskUpdate task : update.getTasksList()) {
            tasks.add(new QuestUpdateEvent.TaskEvent(
                task.getTaskId(),
                task.getDescription(),
                task.getCurrentCount(),
                task.getRequiredCount(),
                task.getCompleted()
            ));
        }

        QuestUpdateEvent.Status status = switch (update.getStatus()) {
            case QUEST_COMPLETED -> QuestUpdateEvent.Status.COMPLETED;
            case QUEST_IN_PROGRESS -> QuestUpdateEvent.Status.IN_PROGRESS;
            case QUEST_NOT_STARTED, UNRECOGNIZED -> QuestUpdateEvent.Status.NOT_STARTED;
        };

        pendingQuestUpdates.add(new QuestUpdateEvent(
            update.getQuestId(),
            update.getQuestName(),
            update.getQuestDescription(),
            update.getTasksCompleted(),
            update.getTasksTotal(),
            update.getCompleted(),
            update.getMiniquest(),
            update.getQuestPointsReward(),
            update.getPlayerTotalQuestPoints(),
            status,
            tasks
        ));
        LOG.debug("QuestUpdate id={} progress={}/{} complete={}",
            update.getQuestId(), update.getTasksCompleted(), update.getTasksTotal(), update.getCompleted());
    }

    private void handleLogoutResponse(NetworkProto.LogoutResponse response) {
        pendingLogoutEvent = new LogoutEvent(response.getSuccess(), response.getMessage());
        LOG.info("LogoutResponse success={} message='{}'", response.getSuccess(), response.getMessage());
    }

    private void handleSkillingStateUpdate(NetworkProto.SkillingStateUpdate update) {
        pendingSkillingStates.add(new SkillingStateEvent(
            update.getSkillingType(),
            update.getState(),
            update.getTargetNpcId(),
            update.getMessage()
        ));
        LOG.debug("SkillingState type={} state={} target={} message={}",
            update.getSkillingType(), update.getState(), update.getTargetNpcId(), update.getMessage());
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

    /** Name of the given entity, or empty string if unknown. */
    public String getEntityName(int entityId) {
        return entityNames.getOrDefault(entityId, "");
    }

    public int getNpcDefinitionId(int entityId) {
        return entityDefinitionIds.getOrDefault(entityId, 0);
    }

    /** Combat level of the given entity, or 0 if unknown. */
    public int getEntityCombatLevel(int entityId) {
        return entityCombatLevels.getOrDefault(entityId, 0);
    }

    // Returns the ground item ID at the given tile, or null if none.
    public Integer getGroundItemAt(int tileX, int tileY) {
        for (Map.Entry<Integer, int[]> entry : groundItems.entrySet()) {
            int[] item = entry.getValue();
            if (item[2] == tileX && item[3] == tileY) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Returns a snapshot list of {tileX, tileY} for all known ground items.
     * Used by MiniMap for dot rendering.
     */
    public List<int[]> getGroundItemPositions() {
        List<int[]> out = new ArrayList<>();
        for (int[] data : groundItems.values()) {
            out.add(new int[]{ data[2], data[3] });
        }
        return out;
    }

    // Returns an NPC entity ID at the given tile, or null if none.
    public Integer getNpcAt(int tileX, int tileY) {
        for (Map.Entry<Integer, int[]> entry : entityPositions.entrySet()) {
            int id = entry.getKey();
            if (isPlayer(id)) continue;
            if (isResourceNpc(id)) continue;
            int[] pos = entry.getValue();
            if (pos[0] == tileX && pos[1] == tileY) {
                return id;
            }
        }
        return null;
    }

    // Returns true if an NPC is hostile (attacks player on sight / is a combat target).
    public boolean isNpcHostile(int npcId) {
        return getEntityCombatLevel(npcId) > 0;
    }

    // Returns a resource NPC ID at the tile if it is a skilling target, null otherwise.
    public Integer getResourceNpcAt(int tileX, int tileY) {
        for (Map.Entry<Integer, int[]> entry : entityPositions.entrySet()) {
            int id = entry.getKey();
            if (isPlayer(id)) continue;
            if (!isResourceNpc(id)) continue;
            int[] pos = entry.getValue();
            if (pos[0] == tileX && pos[1] == tileY) {
                return id;
            }
        }
        return null;
    }

    // Returns the primary skilling action string for a resource NPC.
    public String getResourcePrimarySkill(int npcId) {
        String name = getEntityName(npcId);
        if ("Oak Tree".equalsIgnoreCase(name)) return "chop";
        if ("Willow Tree".equalsIgnoreCase(name)) return "chop";
        if ("Maple Tree".equalsIgnoreCase(name)) return "chop";
        if ("Yew Tree".equalsIgnoreCase(name)) return "chop";
        if ("Magic Tree".equalsIgnoreCase(name)) return "chop";
        if ("Fishing Spot".equalsIgnoreCase(name)) {
            List<String> actions = getFishingActions(npcId);
            return actions.isEmpty() ? null : actions.get(0);
        }
        if ("Cooking Fire".equalsIgnoreCase(name)) return "cook_at";
        return null;
    }

    public List<String> getFishingActions(int npcId) {
        String name = getEntityName(npcId);
        if (!"Fishing Spot".equalsIgnoreCase(name)) {
            return Collections.emptyList();
        }
        int definitionId = getNpcDefinitionId(npcId);
        if (definitionId == 200) {
            return List.of("fish_net");
        }
        if (definitionId == 201) {
            return List.of("fish_net", "fish_bait");
        }
        if (definitionId == 202) {
            return List.of("fish_lure", "fish_bait");
        }
        if (definitionId == 203) {
            return List.of("fish_cage", "fish_harpoon");
        }
        return List.of("fish_net");
    }

    private boolean isResourceNpc(int npcId) {
        return getResourcePrimarySkill(npcId) != null;
    }

    public int getMyPlayerId()    { return myPlayerId; }
    public int getPlayerHealth()  { return playerHealth; }
    public int getPlayerMaxHealth() { return playerMaxHealth; }
    public int getSkillLevel(int idx) {
        return (idx >= 0 && idx < skillLevels.length) ? skillLevels[idx] : 1;
    }

    /** Drain all queued level-up events for this frame (one per level gained). */
    public List<LevelUpEvent> drainLevelUps() {
        if (pendingLevelUps.isEmpty()) return List.of();
        List<LevelUpEvent> out = new ArrayList<>();
        LevelUpEvent e;
        while ((e = pendingLevelUps.poll()) != null) out.add(e);
        return out;
    }

    public long getSkillTotalXp(int idx) {
        return (idx >= 0 && idx < skillTotalXp.length) ? skillTotalXp[idx] : 0;
    }

    /** Drain all queued XP drop events for this frame. */
    public List<XpDropEvent> drainXpDrops() {
        if (pendingXpDrops.isEmpty()) return List.of();
        List<XpDropEvent> out = new ArrayList<>();
        XpDropEvent e;
        while ((e = pendingXpDrops.poll()) != null) out.add(e);
        return out;
    }

    public List<SkillingStateEvent> drainSkillingStateEvents() {
        if (pendingSkillingStates.isEmpty()) return List.of();
        List<SkillingStateEvent> out = new ArrayList<>();
        SkillingStateEvent e;
        while ((e = pendingSkillingStates.poll()) != null) out.add(e);
        return out;
    }

    /** Drain dialogue prompts queued this frame. */
    public List<DialoguePromptEvent> drainDialoguePrompts() {
        if (pendingDialoguePrompts.isEmpty()) return List.of();
        List<DialoguePromptEvent> out = new ArrayList<>();
        DialoguePromptEvent e;
        while ((e = pendingDialoguePrompts.poll()) != null) out.add(e);
        return out;
    }

    /** Drain quest updates queued this frame. */
    public List<QuestUpdateEvent> drainQuestUpdates() {
        if (pendingQuestUpdates.isEmpty()) return List.of();
        List<QuestUpdateEvent> out = new ArrayList<>();
        QuestUpdateEvent e;
        while ((e = pendingQuestUpdates.poll()) != null) out.add(e);
        return out;
    }

    public LogoutEvent consumeLogoutEvent() {
        LogoutEvent event = pendingLogoutEvent;
        pendingLogoutEvent = null;
        return event;
    }

    /** True if the player just died; GameScreen calls consumePlayerDeath() to acknowledge. */
    public boolean isPlayerDead()   { return playerDead; }
    public int getDeathRespawnX()   { return deathRespawnX; }
    public int getDeathRespawnY()   { return deathRespawnY; }
    public void consumePlayerDeath() { playerDead = false; }

    public NetworkProto.HandshakeResponse getLastHandshakeResponse() {
        return lastHandshakeResponse;
    }

    public boolean isMember() { return localPlayerIsMember; }
    public boolean isAdminToolsEnabled() { return localPlayerIsAdminToolsEnabled; }
    public String getLastAdminActionMessage() {
        return lastAdminActionMessage;
    }
    public boolean wasLastAdminActionSuccessful() {
        return lastAdminActionSuccess;
    }
    public java.util.List<AdminItemSearchEntry> getAdminItemSearchResults() {
        synchronized (adminItemSearchResults) {
            return new java.util.ArrayList<>(adminItemSearchResults);
        }
    }

    public String getLastAdminItemSearchQuery() {
        return lastAdminItemSearchQuery;
    }

    public boolean consumeAdminTeleportApplied() {
        boolean value = adminTeleportApplied;
        adminTeleportApplied = false;
        return value;
    }

    public int getAdminTeleportX() {
        return adminTeleportX;
    }

    public int getAdminTeleportY() {
        return adminTeleportY;
    }

    private void handlePlayerDeath(NetworkProto.PlayerDeath death) {
        deathRespawnX = death.getRespawnX();
        deathRespawnY = death.getRespawnY();
        playerDead = true;
        LOG.info("Player died — respawning at ({}, {})", deathRespawnX, deathRespawnY);
    }

    private void handleInventoryUpdate(NetworkProto.InventoryUpdate update) {
        int slot = update.getSlot();
        if (slot < 0 || slot >= 28) return;
        inventoryItemIds[slot]    = update.getItemId();
        inventoryQuantities[slot] = update.getQuantity();
        inventoryNames[slot]      = update.getItemName();
        inventoryFlags[slot]      = update.getFlags();
        LOG.debug("InventoryUpdate slot={} itemId={} qty={} name={}",
            slot, update.getItemId(), update.getQuantity(), update.getItemName());
    }

    private void handleEquipmentUpdate(NetworkProto.EquipmentUpdate update) {
        int slot = update.getSlot();
        if (slot < 0 || slot >= 11) return;
        equipmentItemIds[slot] = update.getItemId();
        equipmentNames[slot]   = update.getItemName();
        // WEAPON slot (4): sync attack range so the client knows how far to stand
        if (slot == 4) { // EquipmentSlot.WEAPON
            playerAttackRange = Math.max(1, update.getAttackRange());
        }
        LOG.debug("EquipmentUpdate slot={} itemId={} name={} attackRange={}", slot, update.getItemId(), update.getItemName(), playerAttackRange);
    }

    private void handleEquipmentBonuses(NetworkProto.EquipmentBonuses msg) {
        equipBonuses[0]  = msg.getStabAttack();
        equipBonuses[1]  = msg.getSlashAttack();
        equipBonuses[2]  = msg.getCrushAttack();
        equipBonuses[3]  = msg.getMagicAttack();
        equipBonuses[4]  = msg.getRangedAttack();
        equipBonuses[5]  = msg.getStabDefence();
        equipBonuses[6]  = msg.getSlashDefence();
        equipBonuses[7]  = msg.getCrushDefence();
        equipBonuses[8]  = msg.getMagicDefence();
        equipBonuses[9]  = msg.getRangedDefence();
        equipBonuses[10] = msg.getMeleeStrength();
        equipBonuses[11] = msg.getRangedStrength();
        equipBonuses[12] = msg.getMagicDamage();
        equipBonuses[13] = msg.getPrayer();
    }

    private void handleGroundItemSpawn(NetworkProto.GroundItemSpawn spawn) {
        groundItems.put(spawn.getGroundItemId(),
            new int[]{spawn.getItemId(), spawn.getQuantity(), spawn.getX(), spawn.getY()});
        groundItemNames.put(spawn.getGroundItemId(), spawn.getItemName());
        LOG.debug("GroundItemSpawn id={} itemId={} qty={} at ({},{})",
            spawn.getGroundItemId(), spawn.getItemId(), spawn.getQuantity(),
            spawn.getX(), spawn.getY());
    }

    private void handleGroundItemDespawn(NetworkProto.GroundItemDespawn despawn) {
        groundItems.remove(despawn.getGroundItemId());
        groundItemNames.remove(despawn.getGroundItemId());
        LOG.debug("GroundItemDespawn id={}", despawn.getGroundItemId());
    }

    private void handleNpcDespawn(NetworkProto.NpcDespawn despawn) {
        int id = despawn.getNpcId();
        entityPositions.remove(id);
        entityIsPlayer.remove(id);
        entityNames.remove(id);
        entityCombatLevels.remove(id);
        entityDefinitionIds.remove(id);
        entityHealth.remove(id);
        despawnedNpcQueue.add(id);
        LOG.info("NpcDespawn: id={}", id);
    }

    private void handleNpcRespawn(NetworkProto.NpcRespawn respawn) {
        int id = respawn.getNpcId();
        entityPositions.put(id, new int[]{respawn.getX(), respawn.getY()});
        entityIsPlayer.put(id, false);
        entityNames.put(id, respawn.getName());
        entityCombatLevels.put(id, respawn.getCombatLevel());
        entityDefinitionIds.put(id, respawn.getDefinitionId());
        entityHealth.put(id, new int[]{respawn.getHealth(), respawn.getMaxHealth()});
        LOG.info("NpcRespawn: id={} name={} pos=({},{}) hp={}/{}",
            id, respawn.getName(), respawn.getX(), respawn.getY(),
            respawn.getHealth(), respawn.getMaxHealth());
    }

    // -----------------------------------------------------------------------
    // Chat messages from server
    // -----------------------------------------------------------------------

    private final ConcurrentLinkedQueue<String> serverChatMessages = new ConcurrentLinkedQueue<>();

    private void handleChatMessage(NetworkProto.ChatMessage msg) {
        serverChatMessages.add(msg.getText());
        LOG.debug("Server chat: {}", msg.getText());
    }

    private void handleFriendsListUpdate(NetworkProto.FriendsListUpdate msg) {
        List<FriendsListEvent.Entry> entries = new ArrayList<>();
        for (NetworkProto.PlayerEntry friend : msg.getFriendsList()) {
            entries.add(new FriendsListEvent.Entry(friend.getName(), friend.getPlayerId(), friend.getOnline()));
        }
        pendingFriendsListUpdates.add(new FriendsListEvent(entries));
    }

    private void handleFriendActionResult(NetworkProto.FriendActionResult msg) {
        pendingFriendActionResults.add(new FriendActionResultEvent(msg.getSuccess(), msg.getError(), msg.getSequence()));
    }

    private void handleBankOpen(NetworkProto.BankOpen msg) {
        bankCapacity = msg.getCapacity();
        synchronized (bankLock) {
            bankSlots.clear();
            for (NetworkProto.BankSlot slot : msg.getSlotsList()) {
                bankSlots.add(new BankSlotSnapshot(
                    slot.getSlot(),
                    slot.getTabIndex(),
                    slot.getItemId(),
                    slot.getQuantity(),
                    slot.getItemName(),
                    slot.getFlags()
                ));
            }
            bankSlots.sort(java.util.Comparator.comparingInt(slot -> slot.slot));
        }
        bankOpen = true;
        LOG.debug("BankOpen received: capacity={} slots={}", bankCapacity, msg.getSlotsCount());
    }

    private void handleBankClose(NetworkProto.BankClose msg) {
        bankOpen = false;
        bankCapacity = 0;
        synchronized (bankLock) {
            bankSlots.clear();
        }
        LOG.debug("BankClose received: reason={}", msg.getReason());
    }

    private void handleBankSlotUpdate(NetworkProto.BankSlotUpdate msg) {
        synchronized (bankLock) {
            bankSlots.removeIf(slot -> slot.slot == msg.getSlot());
            if (msg.getItemId() > 0 && msg.getQuantity() > 0) {
                bankSlots.add(new BankSlotSnapshot(
                    msg.getSlot(),
                    msg.getTabIndex(),
                    msg.getItemId(),
                    msg.getQuantity(),
                    msg.getItemName(),
                    msg.getFlags()
                ));
            }
            bankSlots.sort(java.util.Comparator.comparingInt(slot -> slot.slot));
        }
        LOG.debug("BankSlotUpdate received: slot={} itemId={} qty={}",
            msg.getSlot(), msg.getItemId(), msg.getQuantity());
    }

    /** Drain server chat messages queued this frame. */
    public List<String> drainServerChatMessages() {
        if (serverChatMessages.isEmpty()) return List.of();
        List<String> out = new ArrayList<>();
        String m;
        while ((m = serverChatMessages.poll()) != null) out.add(m);
        return out;
    }

    // -----------------------------------------------------------------------
    // Public chat broadcasts
    // -----------------------------------------------------------------------

    public static class PrayerPointsEvent {
        public final int current, maximum;
        public PrayerPointsEvent(int current, int maximum) {
            this.current = current;
            this.maximum = maximum;
        }
    }

    public static class FriendsListEvent {
        public static class Entry {
            public final String name;
            public final long playerId;
            public final boolean online;

            public Entry(String name, long playerId, boolean online) {
                this.name = name;
                this.playerId = playerId;
                this.online = online;
            }
        }

        public final List<Entry> entries;

        public FriendsListEvent(List<Entry> entries) {
            this.entries = entries;
        }
    }

    public static class FriendActionResultEvent {
        public final boolean success;
        public final String error;
        public final long sequence;

        public FriendActionResultEvent(boolean success, String error, long sequence) {
            this.success = success;
            this.error = error;
            this.sequence = sequence;
        }
    }

    private final ConcurrentLinkedQueue<PrayerPointsEvent> pendingPrayerPoints =
        new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<FriendsListEvent> pendingFriendsListUpdates =
        new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<FriendActionResultEvent> pendingFriendActionResults =
        new ConcurrentLinkedQueue<>();
    public List<PrayerPointsEvent> drainPrayerPoints() {
        if (pendingPrayerPoints.isEmpty()) return List.of();
        List<PrayerPointsEvent> out = new ArrayList<>();
        PrayerPointsEvent e;
        while ((e = pendingPrayerPoints.poll()) != null) out.add(e);
        return out;
    }

    public List<FriendsListEvent> drainFriendsListUpdates() {
        if (pendingFriendsListUpdates.isEmpty()) return List.of();
        List<FriendsListEvent> out = new ArrayList<>();
        FriendsListEvent e;
        while ((e = pendingFriendsListUpdates.poll()) != null) out.add(e);
        return out;
    }

    public List<FriendActionResultEvent> drainFriendActionResults() {
        if (pendingFriendActionResults.isEmpty()) return List.of();
        List<FriendActionResultEvent> out = new ArrayList<>();
        FriendActionResultEvent e;
        while ((e = pendingFriendActionResults.poll()) != null) out.add(e);
        return out;
    }

    /** A public chat message from a nearby player. */
    public static class ChatBroadcastEvent {
        public final int    senderId;
        public final String senderName;
        public final String text;
        public final int    x, y;

        public ChatBroadcastEvent(int senderId, String senderName, String text, int x, int y) {
            this.senderId   = senderId;
            this.senderName = senderName;
            this.text       = text;
            this.x          = x;
            this.y          = y;
        }
    }

    private final ConcurrentLinkedQueue<ChatBroadcastEvent> pendingChatBroadcasts =
        new ConcurrentLinkedQueue<>();

    private void handleChatBroadcast(NetworkProto.ChatBroadcast msg) {
        pendingChatBroadcasts.add(new ChatBroadcastEvent(
            msg.getSenderId(), msg.getSenderName(), msg.getText(), msg.getX(), msg.getY()));
        LOG.debug("ChatBroadcast from {}: {}", msg.getSenderName(), msg.getText());
    }

    /** Drain all public chat broadcasts queued this frame. */
    public List<ChatBroadcastEvent> drainChatBroadcasts() {
        if (pendingChatBroadcasts.isEmpty()) return List.of();
        List<ChatBroadcastEvent> out = new ArrayList<>();
        ChatBroadcastEvent e;
        while ((e = pendingChatBroadcasts.poll()) != null) out.add(e);
        return out;
    }

    /** Drain the IDs of NPCs that died this frame so GameScreen can clear their visuals. */
    public List<Integer> drainDespawnedNpcs() {
        if (despawnedNpcQueue.isEmpty()) return List.of();
        List<Integer> out = new ArrayList<>();
        Integer id;
        while ((id = despawnedNpcQueue.poll()) != null) out.add(id);
        return out;
    }

    // -----------------------------------------------------------------------
    // Accessors for inventory / equipment / ground items
    // -----------------------------------------------------------------------

    public int   getInventoryItemId(int slot)    { return (slot >= 0 && slot < 28) ? inventoryItemIds[slot]    : 0; }
    public int   getInventoryQuantity(int slot)  { return (slot >= 0 && slot < 28) ? inventoryQuantities[slot] : 0; }
    public String getInventoryName(int slot)     { return (slot >= 0 && slot < 28) ? inventoryNames[slot]      : ""; }
    public int   getInventoryFlags(int slot)     { return (slot >= 0 && slot < 28) ? inventoryFlags[slot]      : 0; }

    public int    getEquipmentItemId(int slot)   { return (slot >= 0 && slot < 11) ? equipmentItemIds[slot] : 0; }
    public String getEquipmentName(int slot)     { return (slot >= 0 && slot < 11) ? equipmentNames[slot]   : ""; }
    public int[] getEquipBonuses() { return equipBonuses; }
    public int   getPlayerAttackRange()          { return playerAttackRange; }
    public int   getSelectedSpellId()            { return selectedSpellId; }
    public void  setSelectedSpellId(int id)      { this.selectedSpellId = id; }

    /** Snapshot of all ground items — safe to read on the render thread. */
    public Map<Integer, int[]>  getGroundItems()     { return groundItems; }
    public Map<Integer, String> getGroundItemNames() { return groundItemNames; }
    public boolean isBankOpen() { return bankOpen; }
    public int getBankCapacity() { return bankCapacity; }
    public List<BankSlotSnapshot> getBankSlots() {
        synchronized (bankLock) {
            return Collections.unmodifiableList(new ArrayList<>(bankSlots));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOG.error("Exception in client packet handler", cause);
        ctx.close();
    }
}
