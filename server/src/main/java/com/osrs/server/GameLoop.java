package com.osrs.server;

import com.osrs.protocol.NetworkProto;
import com.osrs.server.combat.CombatEngine;
import com.osrs.server.database.DatabaseManager;
import com.osrs.server.database.PlayerRepository;
import com.osrs.server.network.NettyServer;
import com.osrs.server.network.PlayerSession;
import com.osrs.server.quest.Quest;
import com.osrs.server.quest.QuestManager;
import com.osrs.server.world.GroundItem;
import com.osrs.server.world.World;
import com.osrs.shared.ItemDefinition;
import com.osrs.shared.CombatStyle;
import com.osrs.shared.EquipmentSlot;
import com.osrs.shared.NPC;
import com.osrs.shared.Player;
import com.osrs.shared.SkillingAction;
import com.osrs.shared.WoodcuttingRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 256-tick game loop running on a dedicated thread.
 * 
 * Each tick (3.9ms) processes:
 * 1. Player input (movement, combat commands)
 * 2. Entity updates (positions, animations)
 * 3. Combat calculations
 * 4. Skill progression
 * 5. Delta synchronization
 */
public class GameLoop {
    
    private static final Logger LOG = LoggerFactory.getLogger(GameLoop.class);
    
    private final long tickIntervalNs;
    private volatile boolean running = false;
    private Thread loopThread;
    private long tickCount = 0;
    
    private final World world;
    private final NettyServer nettyServer;
    private final CombatEngine combatEngine;

    // NPC wander: per-NPC tick counter for next random step
    // At 256 Hz, 200-450 ticks ≈ 0.78–1.76 seconds between steps (OSRS-like cadence)
    private static final int WANDER_MIN = 200;
    private static final int WANDER_MAX = 450;

    // Autosave every 60 seconds (256 Hz × 60 = 15,360 ticks)
    private static final long AUTOSAVE_INTERVAL = 15_360L;
    private final Map<Integer, Long> npcNextWanderTick = new HashMap<>();
    private final Random random = new Random();

    // Prayer drain: 1 prayer point every 9,240 server ticks (~36 s) — OSRS tier-1 rate.
    private static final long PRAYER_DRAIN_TICKS = 9_240L;

    // NPC combat movement: 1 tile per ~0.6s = 154 ticks at 256 Hz
    private static final int NPC_MOVE_SPEED   = 154;
    // NPC attack speed: 4 OSRS ticks = 2.4s = 615 server ticks
    private static final int NPC_ATTACK_SPEED = 615;

    // NPC de-aggro chase limit: wander_radius + 5 tiles beyond spawn
    private static final int NPC_CHASE_EXTRA  = 5;

    // PID rotation: every 100-150 OSRS ticks (15400-23100 server ticks)
    // OSRS: PID re-randomizes each cycle; lower PID = higher priority in simultaneous actions
    private static final long PID_ROTATE_MIN = 15400L;
    private static final long PID_ROTATE_MAX = 23100L;
    private long nextPidRotateTick = 0;

    // Server tick ↔ OSRS tick conversion: 256 Hz × 0.6 s/tick = 153.6 ≈ 154 server ticks per OSRS tick
    private static final int OSRS_TICKS_TO_SERVER = 154;
    // Fixed chop attempt interval: 4 OSRS ticks per roll (OSRS standard)
    private static final int CHOP_ATTEMPT_INTERVAL = WoodcuttingRegistry.CHOP_ATTEMPT_OSRS_TICKS * OSRS_TICKS_TO_SERVER;

    // Fishing (MVP first slice)
    private static final int SMALL_FISHING_NET_ITEM_ID = 303;
    private static final int RAW_SHRIMPS_ITEM_ID = 317;
    private static final long SHRIMPS_XP = 100L;  // 10.0 XP stored as tenths

    // Cooking (MVP first slice)
    private static final int COOKED_SHRIMPS_ITEM_ID = 315;
    private static final int BURNT_SHRIMPS_ITEM_ID = 7954;
    private static final long COOKED_SHRIMPS_XP = 300L;  // 30.0 XP stored as tenths
    
    public GameLoop(long tickIntervalNs, World world, NettyServer nettyServer) {
        this.tickIntervalNs = tickIntervalNs;
        this.world = world;
        this.nettyServer = nettyServer;
        this.combatEngine = new CombatEngine(world.getItemDefs());
    }
    
    public void start() {
        LOG.info("Starting game loop (interval: {} ns)", tickIntervalNs);
        validateTreeVariantConfiguration();
        running = true;
        
        loopThread = new Thread(this::run, "GameLoop");
        loopThread.setDaemon(false);
        loopThread.start();
    }
    
    public void stop() {
        LOG.info("Stopping game loop at tick {}", tickCount);
        running = false;
        
        try {
            loopThread.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void run() {
        long lastTickNs = System.nanoTime();
        long logIntervalTicks = 256; // Log once per second
        
        while (running) {
            try {
                // Process tick
                processTick();
                
                tickCount++;
                
                // Log every second
                if (tickCount % logIntervalTicks == 0) {
                    LOG.info("Tick {} (uptime: {} sec)", tickCount, tickCount / 256);
                }
                
                // Sleep until next tick
                long now = System.nanoTime();
                long tickDuration = now - lastTickNs;
                long sleepNs = tickIntervalNs - tickDuration;
                
                if (sleepNs > 0) {
                    Thread.sleep(sleepNs / 1_000_000, (int) (sleepNs % 1_000_000));
                } else {
                    // Tick overran; log warning but don't sleep
                    if (tickDuration > tickIntervalNs * 1.1) {
                        LOG.warn("Tick {} took {} ms (expected {:.1f} ms)", 
                            tickCount, 
                            tickDuration / 1_000_000.0, 
                            tickIntervalNs / 1_000_000.0);
                    }
                }
                
                lastTickNs = System.nanoTime();
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.info("Game loop interrupted");
                break;
            } catch (Exception e) {
                LOG.error("Error in game loop tick {}", tickCount, e);
            }
        }
        
        LOG.info("Game loop exited at tick {}", tickCount);
    }
    
    /**
     * 6-stage tick processing (EXACT ORDER MATTERS for determinism)
     * See EXHAUSTIVE_DEVELOPMENT_ROADMAP.md for stage definitions
     */
    private void processTick() {
        try {
            // Expose current tick to ServerPacketHandler for pickup scheduling
            nettyServer.setCurrentTick(tickCount);

            // Stage 1: Input dequeue (dequeue packets from Netty)
            // Currently handled by ServerPacketHandler directly

            // PID rotation: re-randomize player priorities every 100-150 OSRS ticks
            if (tickCount >= nextPidRotateTick) {
                rotatePids();
            }

            // Stage 2: Movement — NPC wander + player pathfinding
            updateNPCWander();
            processNPCCombat();  // NPC follow & retaliate when aggroed

            // Stage 3: Combat calculation
            processCombat();

            // Stage 4: Skill progression (XP awards, level-ups)
            processSkillingActions();
            updateSkills();
            processPrayerDrain();

            // Stage 5: Loot generation — tick ground item visibility and despawn
            tickGroundItems();

            // Stage 5b: NPC respawn timers
            processNPCRespawns();

            // Stage 5c: Pending item pickups (3-OSRS-tick animation delay)
            processPendingPickups();

            // Stage 6: Autosave — persist all online players every 60 seconds
            if (tickCount > 0 && tickCount % AUTOSAVE_INTERVAL == 0 && DatabaseManager.isHealthy()) {
                for (Player p : world.getPlayers().values()) {
                    PlayerRepository.savePlayer(p);
                    PlayerRepository.saveInventory(p);
                    PlayerRepository.saveBank(p);
                    PlayerRepository.saveEquipment(p);
                    PlayerSession ps = getSessionForPlayer(p.getId());
                    if (ps != null) {
                        PlayerRepository.saveQuestProgress(p, ps.getQuestManager());
                    }
                }
                LOG.info("Autosave complete — {} player(s) saved", world.getPlayers().size());
            }

        } catch (Exception e) {
            LOG.error("Error in tick {}", tickCount, e);
        }
    }

    /**
     * Execute all scheduled item pickups whose animation delay has elapsed.
     * OSRS: 3-tick (1.8 s) animation; item enters inventory + despawns after that.
     */
    private void processPendingPickups() {
        for (World.PendingPickup pp : world.drainDuePickups(tickCount)) {
            Player player = pp.player;
            GroundItem gi  = world.getGroundItem(pp.groundItemId);

            if (gi == null) {
                // Item despawned between scheduling and execution
                sendChatMessageToPlayer(pp.ctx, "Too late — it's gone!", 1);
                continue;
            }

            // Re-check inventory (may have filled up during the delay)
            ItemDefinition def = world.getItemDef(gi.getItemId());
            if (def.stackable) {
                // Try to stack
                for (int i = 0; i < 28; i++) {
                    if (player.getInventoryItemId(i) == gi.getItemId()) {
                        int newQty = player.getInventoryQuantity(i) + gi.getQuantity();
                        player.setInventoryItem(i, gi.getItemId(), newQty);
                        world.removeGroundItem(gi.getGroundItemId());
                        sendInventorySlot(pp.ctx, player, i);
                        broadcastGroundItemDespawn(gi.getGroundItemId());
                        LOG.info("Player {} picked up {} x{} (stacked into slot {})",
                            player.getId(), def.name, gi.getQuantity(), i);
                        gi = null;
                        break;
                    }
                }
                if (gi == null) continue;
            }

            int slot = player.getFirstEmptySlot();
            if (slot < 0) {
                sendChatMessageToPlayer(pp.ctx,
                    "Your inventory is too full to pick up the " + def.name + ".", 1);
                continue;
            }

            player.setInventoryItem(slot, gi.getItemId(), gi.getQuantity());
            world.removeGroundItem(gi.getGroundItemId());
            sendInventorySlot(pp.ctx, player, slot);
            broadcastGroundItemDespawn(gi.getGroundItemId());
            LOG.info("Player {} picked up {} x{} into slot {}", player.getId(), def.name, gi.getQuantity(), slot);
        }
    }

    private void sendInventorySlot(io.netty.channel.ChannelHandlerContext ctx, Player player, int slot) {
        sendInventorySlot(ctx.channel(), player, slot);
    }

    private void sendInventorySlot(io.netty.channel.Channel channel, Player player, int slot) {
        int itemId = player.getInventoryItemId(slot);
        int qty    = player.getInventoryQuantity(slot);
        ItemDefinition def = world.getItemDef(itemId);
        channel.writeAndFlush(NetworkProto.ServerMessage.newBuilder()
            .setInventoryUpdate(NetworkProto.InventoryUpdate.newBuilder()
                .setSlot(slot)
                .setItemId(itemId)
                .setQuantity(qty)
                .setItemName(itemId > 0 ? def.name : "")
                .setFlags(itemId > 0 ? def.getFlags() : 0))
            .build());
    }

    private void broadcastGroundItemDespawn(int groundItemId) {
        nettyServer.broadcastToAll(NetworkProto.ServerMessage.newBuilder()
            .setGroundItemDespawn(NetworkProto.GroundItemDespawn.newBuilder()
                .setGroundItemId(groundItemId))
            .build());
    }

    private void sendChatMessageToPlayer(io.netty.channel.ChannelHandlerContext ctx, String text, int type) {
        sendChatMessageToPlayer(ctx.channel(), text, type);
    }

    private void sendChatMessageToPlayer(io.netty.channel.Channel channel, String text, int type) {
        channel.writeAndFlush(NetworkProto.ServerMessage.newBuilder()
            .setChatMessage(NetworkProto.ChatMessage.newBuilder()
                .setText(text).setType(type))
            .build());
    }

    private void sendPositionCorrection(io.netty.channel.Channel channel, Player player) {
        channel.writeAndFlush(NetworkProto.ServerMessage.newBuilder()
            .setEntityUpdate(NetworkProto.EntityUpdate.newBuilder()
                .setEntityId(player.getId())
                .setX(player.getX())
                .setY(player.getY()))
            .build());
    }
    
    /**
     * Process queued player input commands.
     * TODO: Dequeue from player sessions.
     */
    private void processPlayerInput() {
        // Placeholder: input is currently processed by ServerPacketHandler
        // In a full implementation, we'd dequeue command packets here
    }
    
    /**
     * NPC wander: each NPC with wanderRadius > 0 takes one random step per
     * WANDER_MIN..WANDER_MAX ticks (≈0.8–1.8 s), staying within their radius
     * of their spawn point. Broadcasts EntityUpdate to all clients on move.
     * Skips NPCs that are dead, in combat, or in dialogue.
     */
    private void updateNPCWander() {
        for (NPC npc : world.getNPCs().values()) {
            if (npc.isDead() || npc.isInCombat() || npc.isInDialogue() || npc.getWanderRadius() <= 0) continue;
            // Keep guide/instructor style NPCs stable; non-combat NPCs should not
            // drift around while players attempt to start dialogue.
            if (npc.getCombatLevel() <= 0) continue;
            // Also freeze if any player is currently attacking this NPC (even before aggro)
            if (isBeingAttackedByPlayer(npc.getId())) continue;
            // Keep dialogue/trade style NPC interactions stable: when a player is already
            // adjacent, do not take a wander step away before interaction packet lands.
            if (isAnyPlayerAdjacent(npc)) continue;

            long next = npcNextWanderTick.getOrDefault(npc.getId(), 0L);
            if (tickCount < next) continue;

            // Schedule next step
            npcNextWanderTick.put(npc.getId(),
                tickCount + WANDER_MIN + random.nextInt(WANDER_MAX - WANDER_MIN));

            // Try up to 8 random directions; pick first valid tile within radius
            int[] dx = {0, 1, 0, -1, 1, 1, -1, -1};
            int[] dy = {1, 0, -1, 0, 1, -1, 1, -1};
            int start = random.nextInt(8);
            for (int i = 0; i < 8; i++) {
                int dir = (start + i) % 8;
                int nx = npc.getX() + dx[dir];
                int ny = npc.getY() + dy[dir];
                // Must be within wander radius of spawn AND within map bounds AND walkable
                int distX = Math.abs(nx - npc.getSpawnX());
                int distY = Math.abs(ny - npc.getSpawnY());
                if (distX > npc.getWanderRadius() || distY > npc.getWanderRadius()) continue;
                if (nx < 0 || nx >= 104 || ny < 0 || ny >= 104) continue;
                if (!world.canWalkTo(nx, ny)) continue;

                npc.setPosition(nx, ny);

                NetworkProto.ServerMessage update = NetworkProto.ServerMessage.newBuilder()
                    .setEntityUpdate(NetworkProto.EntityUpdate.newBuilder()
                        .setEntityId(npc.getId())
                        .setX(nx)
                        .setY(ny))
                    .build();
                nettyServer.broadcastToAll(update);
                break;
            }
        }
    }

    /**
     * NPC combat AI: for every NPC that has been aggroed (combatTarget >= 0):
     * - If target player gone / dead → clear combat target.
     * - If player escaped beyond spawn + wander_radius + NPC_CHASE_EXTRA → de-aggro.
     * - If in melee range (Chebyshev ≤ 1) → attack every NPC_ATTACK_SPEED ticks.
     * - Otherwise → step one tile toward player every NPC_MOVE_SPEED ticks.
     * NPCs in dialogue are skipped (frozen).
     */
    private void processNPCCombat() {
        for (NPC npc : world.getNPCs().values()) {
            if (npc.isDead() || !npc.isInCombat() || npc.isInDialogue()) continue;

            // Non-combat NPCs (guides, bankers, etc.) should never chase/attack.
            if (npc.getCombatLevel() <= 0) {
                npc.setCombatTarget(-1);
                continue;
            }

            Player target = world.getPlayer(npc.getCombatTarget());
            if (target == null || target.getHealth() <= 0) {
                npc.setCombatTarget(-1);
                continue;
            }

            // De-aggro if player fled beyond chase limit from spawn
            int chaseLimit = npc.getWanderRadius() + NPC_CHASE_EXTRA;
            int playerFromSpawnX = Math.abs(target.getX() - npc.getSpawnX());
            int playerFromSpawnY = Math.abs(target.getY() - npc.getSpawnY());
            if (Math.max(playerFromSpawnX, playerFromSpawnY) > chaseLimit) {
                npc.setCombatTarget(-1);
                LOG.debug("NPC {} de-aggroed: player {} fled beyond chase range", npc.getId(), target.getId());
                continue;
            }

            int chebyshev = Math.max(
                Math.abs(npc.getX() - target.getX()),
                Math.abs(npc.getY() - target.getY())
            );

            if (chebyshev <= npc.getAttackRange()) {
                // In attack range — attack
                if (tickCount - npc.getLastAttackTick() < NPC_ATTACK_SPEED) continue;

                CombatEngine.HitResult result = combatEngine.calculateHit(npc, target, tickCount);
                npc.setLastAttackTick(tickCount);

                int newTargetHealth = target.getHealth();
                if (result.hit) {
                    newTargetHealth = Math.max(0, target.getHealth() - result.damage);
                    target.setHealth(newTargetHealth);
                    LOG.debug("NPC {} hit player {} for {} (HP now {})",
                        npc.getId(), target.getId(), result.damage, newTargetHealth);
                } else {
                    LOG.debug("NPC {} missed player {}", npc.getId(), target.getId());
                }

                // Broadcast CombatHit (attacker = NPC, target = player)
                nettyServer.broadcastToAll(NetworkProto.ServerMessage.newBuilder()
                    .setCombatHit(NetworkProto.CombatHit.newBuilder()
                        .setAttackerId(npc.getId())
                        .setTargetId(target.getId())
                        .setDamage(result.damage)
                        .setHit(result.hit)
                        .setXpAwarded(0)
                        .setAttackerHealth(npc.getHealth())
                        .setTargetHealth(newTargetHealth)
                        .setTargetX(target.getX())
                        .setTargetY(target.getY()))
                    .build());

                // Broadcast HealthUpdate for the player
                nettyServer.broadcastToAll(NetworkProto.ServerMessage.newBuilder()
                    .setHealthUpdate(NetworkProto.HealthUpdate.newBuilder()
                        .setEntityId(target.getId())
                        .setHealth(newTargetHealth)
                        .setMaxHealth(target.getMaxHealth()))
                    .build());

                if (newTargetHealth <= 0) {
                    npc.setCombatTarget(-1);
                    LOG.info("NPC {} killed player {}", npc.getId(), target.getId());
                    respawnPlayer(target);
                } else if (target.isAutoRetaliate() && target.getCombatTarget() < 0) {
                    target.setCombatTarget(npc.getId());
                }

            } else {
                // Out of range — step one tile along a valid BFS path.
                if (tickCount - npc.getLastPathfindTick() < NPC_MOVE_SPEED) continue;

                List<com.osrs.server.world.Pathfinding.Tile> path = world.findPath(
                    npc.getX(), npc.getY(), target.getX(), target.getY());
                if (!path.isEmpty()) {
                    com.osrs.server.world.Pathfinding.Tile step = path.get(0);
                    int nx = step.x;
                    int ny = step.y;

                    npc.setPosition(nx, ny);
                    npc.setLastPathfindTick(tickCount);

                    nettyServer.broadcastToAll(NetworkProto.ServerMessage.newBuilder()
                        .setEntityUpdate(NetworkProto.EntityUpdate.newBuilder()
                            .setEntityId(npc.getId())
                            .setX(nx)
                            .setY(ny))
                        .build());
                }
            }
        }
    }

    /**
     * Process all active combats.
     * Players are sorted by PID (lower = higher priority) to implement OSRS PID priority.
     * Each player in combat checks attack range before rolling hit/miss/damage every 4 ticks.
     * Out-of-range players maintain their combat target (enabling kiting) but do not fire.
     */
    private void processCombat() {
        // Sort players by PID — lower PID acts first (OSRS priority system)
        List<Player> players = new ArrayList<>(world.getPlayers().values());
        players.sort((a, b) -> Long.compare(a.getPid(), b.getPid()));

        for (Player player : players) {
            if (!player.isInCombat()) {
                continue;
            }

            NPC target = world.getNPC(player.getCombatTarget());
            if (target == null || target.isDead()) {
                player.setCombatTarget(-1);
                LOG.debug("Player {} combat ended (target dead/gone)", player.getId());
                continue;
            }

            // Attack range check: if the player is out of weapon range, skip the attack but
            // keep the combat target set — this is what enables kiting. The client walks the
            // player toward the NPC (or away to kite), and attacks resume once back in range.
            int dist = Math.max(
                Math.abs(player.getX() - target.getX()),
                Math.abs(player.getY() - target.getY())
            );
            if (dist > player.getAttackRange()) {
                continue;
            }

            // OSRS default weapon speed: 4 OSRS-ticks (2.4 seconds) = 615 server ticks at 256 Hz
            int attackSpeed = 615;
            if (tickCount - player.getLastAttackTick() < attackSpeed) {
                continue;
            }

            CombatEngine.HitResult result = combatEngine.calculateHit(player, target, tickCount);

            int newTargetHealth = target.getHealth();
            if (result.hit) {
                newTargetHealth = Math.max(0, target.getHealth() - result.damage);
                target.setHealth(newTargetHealth);
                LOG.debug("Player {} hit NPC {} for {} (HP now {})",
                    player.getId(), target.getId(), result.damage, newTargetHealth);

                // Award XP per OSRS combat style (only on actual damage)
                if (result.damage > 0) {
                    awardCombatXp(player, result.damage);

                    // Aggro NPC on first damaging hit
                    if (!target.isInCombat()) {
                        target.setCombatTarget(player.getId());
                        LOG.info("NPC {} aggroed by player {} (took {} damage)",
                            target.getId(), player.getId(), result.damage);
                    }
                }
            } else {
                LOG.debug("Player {} missed NPC {}", player.getId(), target.getId());
            }

            player.setLastAttackTick(tickCount);

            // Broadcast CombatHit to all clients
            NetworkProto.ServerMessage combatMsg = NetworkProto.ServerMessage.newBuilder()
                .setCombatHit(NetworkProto.CombatHit.newBuilder()
                    .setAttackerId(player.getId())
                    .setTargetId(target.getId())
                    .setDamage(result.damage)
                    .setHit(result.hit)
                    .setXpAwarded(0) // XP sent separately via SkillUpdate
                    .setAttackerHealth(player.getHealth())
                    .setTargetHealth(newTargetHealth)
                    .setTargetX(target.getX())
                    .setTargetY(target.getY()))
                .build();
            nettyServer.broadcastToAll(combatMsg);

            // Broadcast HealthUpdate for the NPC
            NetworkProto.ServerMessage healthMsg = NetworkProto.ServerMessage.newBuilder()
                .setHealthUpdate(NetworkProto.HealthUpdate.newBuilder()
                    .setEntityId(target.getId())
                    .setHealth(newTargetHealth)
                    .setMaxHealth(target.getMaxHealth()))
                .build();
            nettyServer.broadcastToAll(healthMsg);

            // NPC dies
            if (newTargetHealth <= 0) {
                killNPC(target, player);
            }
        }
    }
    
    /**
     * Handles NPC death: marks the NPC as dead, clears all player combat targets
     * pointing at it, spawns loot for the killer, and broadcasts NpcDespawn so
     * every client removes the model. Schedules respawn for the future.
     */
    private void killNPC(NPC npc, Player killer) {
        LOG.info("NPC {} ({}) killed by player {}", npc.getId(), npc.getName(), killer.getId());

        // Mark dead and schedule respawn
        npc.setDead(true);
        npc.setHealth(0);
        npc.setCombatTarget(-1);
        npc.setInDialogue(false);
        npc.setDialoguePlayer(-1);
        npc.setRespawnAtTick(tickCount + npc.getRespawnDelayTicks());

        // Clear any player who was fighting this NPC
        for (Player p : world.getPlayers().values()) {
            if (p.getCombatTarget() == npc.getId()) {
                p.setCombatTarget(-1);
            }
        }

        // Spawn loot for the killer
        spawnLoot(killer, npc);
        updateKillQuestObjectives(killer, npc.getDefinitionId());

        // Tell all clients to remove the NPC model
        nettyServer.broadcastToAll(NetworkProto.ServerMessage.newBuilder()
            .setNpcDespawn(NetworkProto.NpcDespawn.newBuilder()
                .setNpcId(npc.getId()))
            .build());
    }

    private void updateKillQuestObjectives(Player killer, int npcDefinitionId) {
        PlayerSession killerSession = getSessionForPlayer(killer.getId());
        if (killerSession == null || killerSession.getQuestManager() == null) {
            return;
        }

        QuestManager questManager = killerSession.getQuestManager();
        for (Quest quest : questManager.getQuests().values()) {
            for (Quest.Task task : quest.tasks) {
                if (task.type != Quest.TaskType.KILL || task.completed || task.targetEntityId != npcDefinitionId) {
                    continue;
                }

                boolean wasComplete = (quest.status == Quest.QuestStatus.COMPLETED);
                boolean finished = questManager.addTaskProgress(quest.id, task.id, 1);
                Quest updated = questManager.getQuest(quest.id);
                sendQuestUpdate(killerSession, updated);

                boolean isNowComplete = updated.allTasksCompleted()
                    && updated.status == Quest.QuestStatus.COMPLETED;
                if (!wasComplete && isNowComplete) {
                    if (updated.totalRewardXp > 0) {
                        sendSkillUpdate(killer, updated.rewardSkillIndex, (long) updated.totalRewardXp * 10);
                    }
                    sendChatMessageToPlayer(killerSession.getChannel(),
                        "You have completed: " + updated.name + "! You earned "
                            + updated.totalRewardXp + " XP.", 3);
                    PlayerRepository.saveQuestProgress(
                        killerSession.getPlayer(), killerSession.getQuestManager());
                }

                if (finished) {
                    LOG.info("Player {} completed kill objective '{}' for quest {}",
                        killer.getId(), task.id, quest.id);
                }
            }
        }
    }

    private void updateSkillQuestObjectives(PlayerSession session,
                                            Quest.TaskType type,
                                            int targetId) {
        if (session == null || session.getQuestManager() == null) return;
        QuestManager questManager = session.getQuestManager();
        for (Quest quest : questManager.getQuests().values()) {
            for (Quest.Task task : quest.tasks) {
                if (task.type != type || task.completed
                        || task.targetEntityId != targetId) continue;

                boolean wasComplete = (quest.status == Quest.QuestStatus.COMPLETED);
                questManager.addTaskProgress(quest.id, task.id, 1);
                Quest updated = questManager.getQuest(quest.id);
                sendQuestUpdate(session, updated);

                boolean isNowComplete = updated.allTasksCompleted()
                    && updated.status == Quest.QuestStatus.COMPLETED;
                if (!wasComplete && isNowComplete) {
                    if (updated.totalRewardXp > 0) {
                        sendSkillUpdate(session.getPlayer(),
                            updated.rewardSkillIndex, (long) updated.totalRewardXp * 10);
                    }
                    sendChatMessageToPlayer(session.getChannel(),
                        "You have completed: " + updated.name + "! You earned "
                            + updated.totalRewardXp + " XP.", 3);
                    PlayerRepository.saveQuestProgress(
                        session.getPlayer(), session.getQuestManager());
                }
            }
        }
    }

    private void sendQuestUpdate(PlayerSession session, Quest quest) {
        if (session == null || quest == null) {
            return;
        }

        QuestManager questManager = session.getQuestManager();
        if (questManager == null) {
            return;
        }

        NetworkProto.QuestUpdate.Builder update = NetworkProto.QuestUpdate.newBuilder()
            .setQuestId(quest.id)
            .setQuestName(quest.name)
            .setTasksCompleted(quest.getCompletedTaskCount())
            .setTasksTotal(quest.tasks.size())
            .setCompleted(quest.allTasksCompleted())
            .setQuestDescription(quest.description == null ? "" : quest.description)
            .setMiniquest(quest.miniquest)
            .setQuestPointsReward(quest.questPointsReward)
            .setPlayerTotalQuestPoints(questManager.getTotalQuestPoints())
            .setStatus(switch (quest.status) {
                case COMPLETED -> NetworkProto.QuestStatus.QUEST_COMPLETED;
                case IN_PROGRESS -> NetworkProto.QuestStatus.QUEST_IN_PROGRESS;
                case NOT_STARTED -> NetworkProto.QuestStatus.QUEST_NOT_STARTED;
            });

        for (Quest.Task task : quest.tasks) {
            int current = questManager.getTaskProgress(quest.id, task.id);
            update.addTasks(NetworkProto.QuestTaskUpdate.newBuilder()
                .setTaskId(task.id)
                .setDescription(task.description == null ? task.id : task.description)
                .setCurrentCount(current)
                .setRequiredCount(Math.max(1, task.quantity))
                .setCompleted(task.completed));
        }

        session.getChannel().writeAndFlush(NetworkProto.ServerMessage.newBuilder()
            .setQuestUpdate(update)
            .build());

        if (DatabaseManager.isHealthy() && session.getPlayer() != null) {
            PlayerRepository.saveQuestProgress(session.getPlayer(), questManager);
        }
    }

    private PlayerSession getSessionForPlayer(int playerId) {
        for (PlayerSession session : nettyServer.getSessions().values()) {
            if (!session.isAuthenticated() || session.getPlayer() == null) continue;
            if (session.getPlayer().getId() == playerId) return session;
        }
        return null;
    }

    /**
     * Drains 1 prayer point from every online player who has an active prayer,
     * at the rate of 1 point per PRAYER_DRAIN_TICKS server ticks (~36 s).
     * Deactivates all prayers and notifies the player when points reach 0.
     */
    private void processPrayerDrain() {
        if (tickCount == 0 || tickCount % PRAYER_DRAIN_TICKS != 0) return;
        for (PlayerSession ps : nettyServer.getSessions().values()) {
            if (!ps.isAuthenticated() || ps.getPlayer() == null) continue;
            Player p = ps.getPlayer();
            if (!p.hasAnyActivePrayer() || p.getPrayerPoints() <= 0) continue;
            p.setPrayerPoints(p.getPrayerPoints() - 1);
            io.netty.channel.Channel ch = ps.getChannel();
            if (ch == null) continue;
            ch.writeAndFlush(NetworkProto.ServerMessage.newBuilder()
                .setPrayerPointsUpdate(NetworkProto.PrayerPointsUpdate.newBuilder()
                    .setCurrent(p.getPrayerPoints())
                    .setMaximum(p.getMaxPrayerPoints()))
                .build());
            if (p.getPrayerPoints() == 0) {
                p.deactivateAllPrayers();
                ch.writeAndFlush(NetworkProto.ServerMessage.newBuilder()
                    .setChatMessage(NetworkProto.ChatMessage.newBuilder()
                        .setText("You have run out of Prayer points.")
                        .setType(1))
                    .build());
            }
        }
    }

    /**
     * Checks all dead NPCs and respawns any whose timer has expired.
     * Sends NpcRespawn to all clients so they re-add the entity.
     */
    private void processNPCRespawns() {
        for (NPC npc : world.getNPCs().values()) {
            if (!npc.isDead()) continue;
            if (tickCount < npc.getRespawnAtTick()) continue;

            // Reset to full health at spawn point
            npc.setDead(false);
            npc.setHealth(npc.getMaxHealth());
            npc.setPosition(npc.getSpawnX(), npc.getSpawnY());
            npc.setCombatTarget(-1);
            npc.setRespawnAtTick(-1);

            LOG.info("NPC {} ({}) respawned at ({}, {})",
                npc.getId(), npc.getName(), npc.getSpawnX(), npc.getSpawnY());

            // Send full entity data so clients can re-add it
            nettyServer.broadcastToAll(NetworkProto.ServerMessage.newBuilder()
                .setNpcRespawn(NetworkProto.NpcRespawn.newBuilder()
                    .setNpcId(npc.getId())
                    .setName(npc.getName())
                    .setX(npc.getSpawnX())
                    .setY(npc.getSpawnY())
                    .setHealth(npc.getMaxHealth())
                    .setMaxHealth(npc.getMaxHealth())
                    .setCombatLevel(npc.getCombatLevel()))
                .build());
        }
    }

    /**
     * Returns true if any player currently has this NPC set as their combat target.
     * Used to freeze NPC wandering the moment a player begins attacking,
     * even before the first damaging hit lands (which triggers full aggro/retaliation).
     */
    private boolean isBeingAttackedByPlayer(int npcId) {
        for (Player p : world.getPlayers().values()) {
            if (p.getCombatTarget() == npcId) return true;
        }
        return false;
    }

    private boolean isAnyPlayerAdjacent(NPC npc) {
        for (Player p : world.getPlayers().values()) {
            int chebyshev = Math.max(
                Math.abs(p.getX() - npc.getX()),
                Math.abs(p.getY() - npc.getY())
            );
            if (chebyshev <= 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * Teleport the player to the world spawn point (Lumbridge) and restore full HP.
     * Broadcasts a PlayerDeath packet to only that player's session, and an EntityUpdate
     * (new position) to all clients so they see the player teleport away.
     */
    private void respawnPlayer(Player player) {
        int spawnX = world.getSpawnX();
        int spawnY = world.getSpawnY();

        // --- OSRS death drop: keep 3 most valuable items, drop the rest ---
        int deathX = player.getX();
        int deathY = player.getY();

        // Collect occupied slots as [slot, itemId, qty, storeValue]
        List<int[]> occupied = new ArrayList<>();
        for (int s = 0; s < 28; s++) {
            int itemId = player.getInventoryItemId(s);
            if (itemId == 0) continue;
            int qty = player.getInventoryQuantity(s);
            ItemDefinition def = world.getItemDef(itemId);
            occupied.add(new int[]{s, itemId, qty, def != null ? def.storeValue : 1});
        }

        // Sort descending by storeValue; tie-break ascending by slot index
        occupied.sort((a, b) -> a[3] != b[3] ? b[3] - a[3] : a[0] - b[0]);

        // Top 3 slots are kept
        Set<Integer> keptSlots = new HashSet<>();
        for (int i = 0; i < Math.min(3, occupied.size()); i++) {
            keptSlots.add(occupied.get(i)[0]);
        }

        // Drop everything else as immediately-public ground items
        for (int[] entry : occupied) {
            int slot   = entry[0];
            int itemId = entry[1];
            int qty    = entry[2];
            if (keptSlots.contains(slot)) continue;

            // ownerPlayerId = -1 -> public immediately (no owner-only window)
            GroundItem gi = world.spawnGroundItem(itemId, qty, deathX, deathY, -1, tickCount);
            ItemDefinition def = world.getItemDef(itemId);
            String name = def != null ? def.name : "Item";
            nettyServer.broadcastToAll(NetworkProto.ServerMessage.newBuilder()
                .setGroundItemSpawn(NetworkProto.GroundItemSpawn.newBuilder()
                    .setGroundItemId(gi.getGroundItemId())
                    .setItemId(gi.getItemId())
                    .setQuantity(gi.getQuantity())
                    .setX(gi.getX()).setY(gi.getY())
                    .setItemName(name))
                .build());

            // Clear slot and notify the dying player's client
            player.setInventoryItem(slot, 0, 0);
            nettyServer.sendToPlayer(player.getId(), NetworkProto.ServerMessage.newBuilder()
                .setInventoryUpdate(NetworkProto.InventoryUpdate.newBuilder()
                    .setSlot(slot)
                    .setItemId(0)
                    .setQuantity(0)
                    .setItemName("")
                    .setFlags(0))
                .build());
        }

        // Reset state
        player.setHealth(player.getMaxHealth());
        player.setPosition(spawnX, spawnY);
        player.setCombatTarget(-1);

        // Notify the dying player (death screen + respawn coords)
        nettyServer.sendToPlayer(player.getId(), NetworkProto.ServerMessage.newBuilder()
            .setPlayerDeath(NetworkProto.PlayerDeath.newBuilder()
                .setRespawnX(spawnX)
                .setRespawnY(spawnY))
            .build());

        // Broadcast full HP restore so all clients update health bar
        nettyServer.broadcastToAll(NetworkProto.ServerMessage.newBuilder()
            .setHealthUpdate(NetworkProto.HealthUpdate.newBuilder()
                .setEntityId(player.getId())
                .setHealth(player.getMaxHealth())
                .setMaxHealth(player.getMaxHealth()))
            .build());

        // Broadcast new position so all clients see the teleport
        nettyServer.broadcastToAll(NetworkProto.ServerMessage.newBuilder()
            .setEntityUpdate(NetworkProto.EntityUpdate.newBuilder()
                .setEntityId(player.getId())
                .setX(spawnX)
                .setY(spawnY))
            .build());

        LOG.info("Player {} died and respawned at ({}, {})", player.getId(), spawnX, spawnY);
    }

    /**
     * Tick all ground items: despawn expired ones (broadcast GroundItemDespawn to all),
     * and broadcast newly-public items (owner-only → all players) once the timer flips.
     */
    private void tickGroundItems() {
        List<Integer> toRemove = new ArrayList<>();
        for (GroundItem gi : world.getGroundItems().values()) {
            if (gi.isDespawned(tickCount)) {
                toRemove.add(gi.getGroundItemId());
                nettyServer.broadcastToAll(NetworkProto.ServerMessage.newBuilder()
                    .setGroundItemDespawn(NetworkProto.GroundItemDespawn.newBuilder()
                        .setGroundItemId(gi.getGroundItemId()))
                    .build());
            } else if (!gi.isPublic(gi.getSpawnTick()) && gi.isPublic(tickCount)) {
                // Just became public — broadcast to all players
                String name = world.getItemDef(gi.getItemId()).name;
                nettyServer.broadcastToAll(NetworkProto.ServerMessage.newBuilder()
                    .setGroundItemSpawn(NetworkProto.GroundItemSpawn.newBuilder()
                        .setGroundItemId(gi.getGroundItemId())
                        .setItemId(gi.getItemId())
                        .setQuantity(gi.getQuantity())
                        .setX(gi.getX()).setY(gi.getY())
                        .setItemName(name))
                    .build());
            }
        }
        toRemove.forEach(world::removeGroundItem);
    }

    /**
     * Spawn loot drops on the ground at the NPC's tile when it dies.
     * Delegates loot rolling to World (which has access to package-private WorldData).
     * Sends each spawned item to the killer only (owner-only until OWNER_ONLY_TICKS elapses).
     */
    private void spawnLoot(Player killer, NPC npc) {
        List<GroundItem> spawned = world.rollAndSpawnLoot(npc, killer, tickCount, random);
        for (GroundItem gi : spawned) {
            String name = world.getItemDef(gi.getItemId()).name;
            nettyServer.sendToPlayer(killer.getId(), NetworkProto.ServerMessage.newBuilder()
                .setGroundItemSpawn(NetworkProto.GroundItemSpawn.newBuilder()
                    .setGroundItemId(gi.getGroundItemId())
                    .setItemId(gi.getItemId())
                    .setQuantity(gi.getQuantity())
                    .setX(gi.getX()).setY(gi.getY())
                    .setItemName(name))
                .build());
        }
    }

    /**
     * Randomly reassigns PIDs to all connected players, then schedules the next rotation.
     * OSRS: PID rotates every 100-150 OSRS ticks; lower PID = first to act when simultaneous.
     */
    private void rotatePids() {
        List<Player> players = new ArrayList<>(world.getPlayers().values());
        if (!players.isEmpty()) {
            // Generate a shuffled list of unique IDs in range [1, N]
            List<Integer> pids = new ArrayList<>();
            for (int i = 1; i <= players.size(); i++) pids.add(i);
            Collections.shuffle(pids, random);
            for (int i = 0; i < players.size(); i++) {
                players.get(i).setPid(pids.get(i));
            }
            LOG.debug("PID rotation: {} players reassigned", players.size());
        }
        nextPidRotateTick = tickCount + PID_ROTATE_MIN
            + random.nextInt((int) (PID_ROTATE_MAX - PID_ROTATE_MIN));
    }

    /**
     * Awards combat XP based on the player's current attack style.
     *
     * OSRS formula (per damage point dealt):
     *   Accurate   → 4 Attack XP  + 1.33 Hitpoints XP
     *   Aggressive → 4 Strength XP + 1.33 Hitpoints XP
     *   Defensive  → 4 Defence XP  + 1.33 Hitpoints XP
     *   Controlled → 1.33 Attack + 1.33 Strength + 1.33 Defence + 1.33 Hitpoints XP
     *
     * XP uses integer arithmetic: main skill = damage * 4,
     * HP / controlled split = round(damage * 1.33).
     *
     * Source: https://oldschool.runescape.wiki/w/Combat_Options
     */
    private void awardCombatXp(Player player, int damage) {
        long mainXp = damage * 40L;                       // 4.0 XP/damage stored as tenths
        long hpXp   = Math.round(damage * 13.3);          // 1.33 XP/damage stored as tenths

        CombatStyle style = player.getCombatStyle();
        switch (style) {
            case ACCURATE   -> {
                sendSkillUpdate(player, Player.SKILL_ATTACK,    mainXp);
                sendSkillUpdate(player, Player.SKILL_HITPOINTS, hpXp);
            }
            case AGGRESSIVE -> {
                sendSkillUpdate(player, Player.SKILL_STRENGTH,  mainXp);
                sendSkillUpdate(player, Player.SKILL_HITPOINTS, hpXp);
            }
            case DEFENSIVE  -> {
                sendSkillUpdate(player, Player.SKILL_DEFENCE,   mainXp);
                sendSkillUpdate(player, Player.SKILL_HITPOINTS, hpXp);
            }
            case CONTROLLED -> {
                long splitXp = Math.round(damage * 13.3);  // 1.33 XP/damage stored as tenths
                sendSkillUpdate(player, Player.SKILL_ATTACK,    splitXp);
                sendSkillUpdate(player, Player.SKILL_STRENGTH,  splitXp);
                sendSkillUpdate(player, Player.SKILL_DEFENCE,   splitXp);
                sendSkillUpdate(player, Player.SKILL_HITPOINTS, hpXp);
            }
        }
    }

    /**
     * Adds XP to a player's skill, recomputes level, and sends a SkillUpdate
     * packet to that player only.  If a level-up occurred the packet carries
     * leveled_up=true so the client can display the congratulation overlay.
     */
    private void sendSkillUpdate(Player player, int skillIdx, long xpAmount) {
        boolean leveledUp = player.addSkillXp(skillIdx, xpAmount);
        int newLevel = player.getSkillLevel(skillIdx);
        long totalXp = player.getSkillXp(skillIdx);

        if (leveledUp) {
            LOG.info("Player {} leveled up skill {} → {}", player.getId(), skillIdx, newLevel);
        }

        nettyServer.sendToPlayer(player.getId(), NetworkProto.ServerMessage.newBuilder()
            .setSkillUpdate(NetworkProto.SkillUpdate.newBuilder()
                .setSkillIndex(skillIdx)
                .setNewLevel(newLevel)
                .setTotalXp(totalXp / 10)  // server stores tenths; client receives whole XP
                .setLeveledUp(leveledUp))
            .build());
    }

    private void updateSkills() {
        // XP awards are now handled immediately in awardCombatXp()
    }

    private void processSkillingActions() {
        for (Player player : world.getPlayers().values()) {
            PlayerSession session = getSessionForPlayer(player.getId());
            if (session == null) {
                player.clearSkillingAction();
                continue;
            }

            if (player.getSkillingAction() == SkillingAction.WOODCUTTING) {
                processWoodcutting(player, session);
            } else if (player.getSkillingAction() == SkillingAction.FISHING) {
                processFishing(player, session);
            } else if (player.getSkillingAction() == SkillingAction.COOKING) {
                processCooking(player, session);
            }
        }
    }

    private void processWoodcutting(Player player, PlayerSession session) {
        NPC tree = world.getNPC(player.getSkillingTargetNpcId());
        if (tree == null || tree.isDead()) {
            stopSkilling(player, session, "target-invalid");
            return;
        }

        WoodcuttingRegistry.TreeTier treeTier = resolveTreeTier(tree);
        if (treeTier == null) {
            stopSkilling(player, session, "target-invalid");
            return;
        }

        int requiredLevel = treeTier.levelRequirement();
        int wcLevel = Math.max(1, player.getSkillLevel(Player.SKILL_WOODCUTTING));
        if (wcLevel < requiredLevel) {
            sendChatMessageToPlayer(session.getChannel(),
                "You need a Woodcutting level of " + requiredLevel + " to chop this tree.", 1);
            stopSkilling(player, session, "level-requirement");
            return;
        }

        if (player.isInCombat()) {
            sendChatMessageToPlayer(session.getChannel(), "You are too busy fighting.", 1);
            stopSkilling(player, session, "combat");
            return;
        }

        if (player.isInDialogue()) {
            sendChatMessageToPlayer(session.getChannel(), "Finish your conversation first.", 1);
            stopSkilling(player, session, "dialogue");
            return;
        }

        int axeId = getBestUsableAxeId(player);
        if (axeId < 0) {
            sendChatMessageToPlayer(session.getChannel(), "You need an axe to chop this tree.", 1);
            stopSkilling(player, session, "missing-tool");
            return;
        }

        if (!ensureInRangeOrStep(player, session, tree, "Woodcut")) {
            return;
        }

        announceSkillingActive(player, session, "You swing your axe at the tree.");

        if (player.isInventoryFull()) {
            sendChatMessageToPlayer(session.getChannel(), "Your inventory is too full to hold any more logs.", 1);
            stopSkilling(player, session, "inventory-full");
            return;
        }

        if (tickCount < player.getSkillingNextAttemptTick()) {
            return;
        }

        // OSRS success formula: roll 0-254, succeed if roll < threshold
        // threshold = low + floor((high - low) * wcLevel / 99)
        WoodcuttingRegistry.SuccessRate rate = WoodcuttingRegistry.getSuccessRate(treeTier.definitionId(), axeId);
        boolean success;
        if (rate == null) {
            success = random.nextInt(255) < 128;  // fallback: ~50% if no rate data
        } else {
            int threshold = rate.low() + (int) Math.floor((rate.high() - rate.low()) * (double) wcLevel / 99.0);
            success = random.nextInt(255) < threshold;
        }

        player.setSkillingNextAttemptTick(tickCount + CHOP_ATTEMPT_INTERVAL);

        if (!success) {
            return;
        }

        int slot = player.getFirstEmptySlot();
        if (slot < 0) {
            sendChatMessageToPlayer(session.getChannel(), "Your inventory is too full to hold any more logs.", 1);
            stopSkilling(player, session, "inventory-full");
            return;
        }

        int logItemId = treeTier.logItemId();
        player.setInventoryItem(slot, logItemId, 1);
        sendInventorySlot(session.getChannel(), player, slot);
        sendChatMessageToPlayer(session.getChannel(), "You get some logs.", 0);
        updateSkillQuestObjectives(session, Quest.TaskType.COLLECT, logItemId);
        sendSkillUpdate(player, Player.SKILL_WOODCUTTING, treeTier.xpTenths());

        // Per-tree depletion: standard trees always deplete; others have a 1-in-N chance per log
        boolean depleted;
        if (treeTier.depletionType() == WoodcuttingRegistry.DepletionType.SINGLE_LOG) {
            depleted = true;
        } else {
            depleted = random.nextInt(treeTier.depletionChanceDenominator()) == 0;
        }

        if (depleted) {
            int respawnOsrsTicks = treeTier.respawnMinOsrsTicks() >= treeTier.respawnMaxOsrsTicks()
                ? treeTier.respawnMinOsrsTicks()
                : treeTier.respawnMinOsrsTicks()
                    + random.nextInt(treeTier.respawnMaxOsrsTicks() - treeTier.respawnMinOsrsTicks() + 1);
            tree.setDead(true);
            tree.setRespawnAtTick(tickCount + (long) respawnOsrsTicks * OSRS_TICKS_TO_SERVER);
            nettyServer.broadcastToAll(NetworkProto.ServerMessage.newBuilder()
                .setNpcDespawn(NetworkProto.NpcDespawn.newBuilder().setNpcId(tree.getId()))
                .build());
            stopSkilling(player, session, "depleted");
        }
        // If not depleted, continue chopping (next attempt already scheduled above)
    }

    private WoodcuttingRegistry.TreeTier resolveTreeTier(NPC tree) {
        int definitionId = tree.getDefinitionId();
        WoodcuttingRegistry.TreeTier tier = WoodcuttingRegistry.getTreeByDefinitionId(definitionId);
        if (tier == null) {
            return null;
        }
        String actualName = tree.getName() == null ? "" : tree.getName().trim();
        if (!tier.name().equalsIgnoreCase(actualName)) {
            LOG.error("Tree tier mismatch for npcId={}: definition_id={} expects name='{}', actual='{}'",
                tree.getId(), definitionId, tier.name(), tree.getName());
            return null;
        }
        return tier;
    }

    private void validateTreeVariantConfiguration() {
        List<String> errors = new ArrayList<>();
        for (NPC npc : world.getNPCs().values()) {
            int definitionId = npc.getDefinitionId();
            WoodcuttingRegistry.TreeTier byDef  = WoodcuttingRegistry.getTreeByDefinitionId(definitionId);
            WoodcuttingRegistry.TreeTier byName = WoodcuttingRegistry.getTreeByName(npc.getName());

            if (byDef == null && byName == null) continue;
            if (byDef == null) {
                errors.add("npcId=" + npc.getId() + " name='" + npc.getName()
                    + "' is a known tree name but definition_id=" + definitionId + " is unmapped");
                continue;
            }
            if (byName == null) {
                errors.add("npcId=" + npc.getId() + " definition_id=" + definitionId
                    + " maps to '" + byDef.name() + "' but name='" + npc.getName() + "' is not a known tree name");
                continue;
            }
            if (byDef.definitionId() != byName.definitionId()) {
                errors.add("npcId=" + npc.getId() + " mismatch: definition_id=" + definitionId
                    + "=>" + byDef.name() + " but name='" + npc.getName() + "'=>" + byName.name());
            }
        }
        if (!errors.isEmpty()) {
            String joined = String.join("; ", errors);
            LOG.error("Invalid tree configuration in world data: {}", joined);
            throw new IllegalStateException("Invalid tree configuration: " + joined);
        }
    }

    private void processFishing(Player player, PlayerSession session) {
        NPC spot = world.getNPC(player.getSkillingTargetNpcId());
        if (spot == null || spot.isDead() || !"Fishing Spot".equalsIgnoreCase(spot.getName())) {
            stopSkilling(player, session, "target-invalid");
            return;
        }

        if (player.isInCombat()) {
            sendChatMessageToPlayer(session.getChannel(), "You are too busy fighting.", 1);
            stopSkilling(player, session, "combat");
            return;
        }

        if (player.isInDialogue()) {
            sendChatMessageToPlayer(session.getChannel(), "Finish your conversation first.", 1);
            stopSkilling(player, session, "dialogue");
            return;
        }

        if (!hasSmallFishingNet(player)) {
            sendChatMessageToPlayer(session.getChannel(), "You need a small fishing net to fish here.", 1);
            stopSkilling(player, session, "missing-tool");
            return;
        }

        if (!ensureInRangeOrStep(player, session, spot, "Fishing")) {
            return;
        }

        announceSkillingActive(player, session, "You cast out your net.");

        if (player.isInventoryFull()) {
            sendChatMessageToPlayer(session.getChannel(), "Your inventory is too full to hold any more fish.", 1);
            stopSkilling(player, session, "inventory-full");
            return;
        }

        if (tickCount < player.getSkillingNextAttemptTick()) {
            return;
        }

        int fishingLevel = Math.max(1, player.getSkillLevel(Player.SKILL_FISHING));
        boolean success = random.nextDouble() < fishingSuccessChance(fishingLevel);
        if (!success) {
            player.setSkillingNextAttemptTick(tickCount + nextFishingAttemptTicks(fishingLevel));
            return;
        }

        int slot = player.getFirstEmptySlot();
        if (slot < 0) {
            sendChatMessageToPlayer(session.getChannel(), "Your inventory is too full to hold any more fish.", 1);
            stopSkilling(player, session, "inventory-full");
            return;
        }

        player.setInventoryItem(slot, RAW_SHRIMPS_ITEM_ID, 1);
        sendInventorySlot(session.getChannel(), player, slot);
        sendChatMessageToPlayer(session.getChannel(), "You catch some shrimps.", 0);
        updateSkillQuestObjectives(session, Quest.TaskType.COLLECT, RAW_SHRIMPS_ITEM_ID);
        sendSkillUpdate(player, Player.SKILL_FISHING, SHRIMPS_XP);
        player.setSkillingNextAttemptTick(tickCount + nextFishingAttemptTicks(fishingLevel));
    }

    private void processCooking(Player player, PlayerSession session) {
        NPC fire = world.getNPC(player.getSkillingTargetNpcId());
        if (fire == null || fire.isDead() || !"Cooking Fire".equalsIgnoreCase(fire.getName())) {
            stopSkilling(player, session, "target-invalid");
            return;
        }

        if (player.isInCombat()) {
            sendChatMessageToPlayer(session.getChannel(), "You are too busy fighting.", 1);
            stopSkilling(player, session, "combat");
            return;
        }

        if (!ensureInRangeOrStep(player, session, fire, "Cooking")) {
            return;
        }

        announceSkillingActive(player, session, "You start cooking the shrimps.");

        if (tickCount < player.getSkillingNextAttemptTick()) {
            return;
        }

        int rawSlot = findInventorySlot(player, RAW_SHRIMPS_ITEM_ID);
        if (rawSlot < 0) {
            sendChatMessageToPlayer(session.getChannel(), "You have no raw shrimps to cook.", 1);
            stopSkilling(player, session, "no-input");
            return;
        }

        int cookingLevel = Math.max(1, player.getSkillLevel(Player.SKILL_COOKING));
        boolean burn = random.nextDouble() < shrimpBurnChance(cookingLevel);
        if (burn) {
            player.setInventoryItem(rawSlot, BURNT_SHRIMPS_ITEM_ID, 1);
            sendInventorySlot(session.getChannel(), player, rawSlot);
            sendChatMessageToPlayer(session.getChannel(), "You accidentally burn the shrimps.", 1);
        } else {
            player.setInventoryItem(rawSlot, COOKED_SHRIMPS_ITEM_ID, 1);
            sendInventorySlot(session.getChannel(), player, rawSlot);
            sendChatMessageToPlayer(session.getChannel(), "You cook the shrimps.", 0);
            sendSkillUpdate(player, Player.SKILL_COOKING, COOKED_SHRIMPS_XP);
            updateSkillQuestObjectives(session, Quest.TaskType.ACTION, fire.getDefinitionId());
        }

        player.setSkillingNextAttemptTick(tickCount + nextCookingAttemptTicks(cookingLevel));
    }

    private double fishingSuccessChance(int fishingLevel) {
        double chance = 0.30 + (fishingLevel * 0.0065);
        return Math.max(0.30, Math.min(0.88, chance));
    }

    private int nextFishingAttemptTicks(int fishingLevel) {
        int base = 220;
        int speedBonus = Math.min(110, fishingLevel * 2);
        int interval = Math.max(90, base - speedBonus);
        return interval + random.nextInt(30);
    }

    private double shrimpBurnChance(int cookingLevel) {
        double chance = 0.55 - (cookingLevel * 0.012);
        return Math.max(0.06, Math.min(0.55, chance));
    }

    private int nextCookingAttemptTicks(int cookingLevel) {
        int base = 170;
        int speedBonus = Math.min(70, cookingLevel);
        int interval = Math.max(90, base - speedBonus);
        return interval;
    }

    private int findInventorySlot(Player player, int itemId) {
        for (int i = 0; i < 28; i++) {
            if (player.getInventoryItemId(i) == itemId) {
                return i;
            }
        }
        return -1;
    }

    private int[] findClosestReachableAdjacentTile(Player player, NPC target) {
        int bestX = -1;
        int bestY = -1;
        int bestDist = Integer.MAX_VALUE;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }

                int tx = target.getX() + dx;
                int ty = target.getY() + dy;
                if (!world.canWalkTo(tx, ty)) {
                    continue;
                }
                if (!world.canReach(player.getX(), player.getY(), tx, ty)) {
                    continue;
                }

                int dist = Math.max(Math.abs(player.getX() - tx), Math.abs(player.getY() - ty));
                if (dist < bestDist) {
                    bestDist = dist;
                    bestX = tx;
                    bestY = ty;
                }
            }
        }

        return bestX < 0 ? null : new int[]{bestX, bestY};
    }

    private boolean ensureInRangeOrStep(Player player, PlayerSession session, NPC target, String context) {
        int chebyshev = Math.max(Math.abs(player.getX() - target.getX()), Math.abs(player.getY() - target.getY()));
        if (chebyshev <= 1) {
            return true;
        }

        int[] adjacent = findClosestReachableAdjacentTile(player, target);
        if (adjacent == null) {
            sendChatMessageToPlayer(session.getChannel(), "I can't reach that!", 1);
            stopSkilling(player, session, "unreachable");
            LOG.debug("{} cancelled unreachable: player {} at ({},{}), target {} at ({},{})",
                context, player.getId(), player.getX(), player.getY(), target.getId(), target.getX(), target.getY());
        }
        return false;
    }

    private void announceSkillingActive(Player player, PlayerSession session, String startMessage) {
        if (player.isSkillingActiveAnnounced()) {
            return;
        }
        player.markSkillingActiveAnnounced();
        sendChatMessageToPlayer(session.getChannel(), startMessage, 0);
        sendSkillingStateUpdate(session.getChannel(), toProtoSkillingType(player.getSkillingAction()),
            NetworkProto.SkillingState.SKILLING_STATE_ACTIVE, player.getSkillingTargetNpcId(), "active");
    }

    private void stopSkilling(Player player, PlayerSession session, String reason) {
        SkillingAction action = player.getSkillingAction();
        int targetId = player.getSkillingTargetNpcId();
        player.clearSkillingAction();
        if (session != null && session.getChannel() != null && session.getChannel().isActive()
            && action != SkillingAction.NONE) {
            sendSkillingStateUpdate(session.getChannel(), toProtoSkillingType(action),
                NetworkProto.SkillingState.SKILLING_STATE_STOPPED, targetId, reason);
        }
    }

    private NetworkProto.SkillingType toProtoSkillingType(SkillingAction action) {
        return switch (action) {
            case WOODCUTTING -> NetworkProto.SkillingType.SKILLING_WOODCUTTING;
            case FISHING -> NetworkProto.SkillingType.SKILLING_FISHING;
            case COOKING -> NetworkProto.SkillingType.SKILLING_COOKING;
            case NONE -> NetworkProto.SkillingType.SKILLING_NONE;
        };
    }

    private void sendSkillingStateUpdate(io.netty.channel.Channel channel,
                                         NetworkProto.SkillingType type,
                                         NetworkProto.SkillingState state,
                                         int targetNpcId,
                                         String message) {
        channel.writeAndFlush(NetworkProto.ServerMessage.newBuilder()
            .setSkillingStateUpdate(NetworkProto.SkillingStateUpdate.newBuilder()
                .setSkillingType(type)
                .setState(state)
                .setTargetNpcId(targetNpcId)
                .setMessage(message == null ? "" : message))
            .build());
    }

    private int getBestUsableAxeId(Player player) {
        int wcLevel = Math.max(1, player.getSkillLevel(Player.SKILL_WOODCUTTING));
        List<WoodcuttingRegistry.AxeTier> axes = WoodcuttingRegistry.axes();
        // axes() is ordered bronze→dragon; iterate best-first (highest index first)
        for (int i = axes.size() - 1; i >= 0; i--) {
            WoodcuttingRegistry.AxeTier axe = axes.get(i);
            if (wcLevel < axe.woodcuttingLevel()) continue;
            if (player.getEquipment(EquipmentSlot.WEAPON) == axe.itemId()) return axe.itemId();
            for (int s = 0; s < 28; s++) {
                if (player.getInventoryItemId(s) == axe.itemId()) return axe.itemId();
            }
        }
        return -1;
    }

    private boolean hasSmallFishingNet(Player player) {
        for (int i = 0; i < 28; i++) {
            if (player.getInventoryItemId(i) == SMALL_FISHING_NET_ITEM_ID) {
                return true;
            }
        }
        return false;
    }


    public long getTickCount() {
        return tickCount;
    }
}
