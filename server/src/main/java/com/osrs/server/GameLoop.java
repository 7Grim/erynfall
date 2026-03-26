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
import com.osrs.shared.NPC;
import com.osrs.shared.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
    
    public GameLoop(long tickIntervalNs, World world, NettyServer nettyServer) {
        this.tickIntervalNs = tickIntervalNs;
        this.world = world;
        this.nettyServer = nettyServer;
        this.combatEngine = new CombatEngine(world.getItemDefs());
    }
    
    public void start() {
        LOG.info("Starting game loop (interval: {} ns)", tickIntervalNs);
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
            updateSkills();

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
        int itemId = player.getInventoryItemId(slot);
        int qty    = player.getInventoryQuantity(slot);
        ItemDefinition def = world.getItemDef(itemId);
        ctx.writeAndFlush(NetworkProto.ServerMessage.newBuilder()
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
        ctx.writeAndFlush(NetworkProto.ServerMessage.newBuilder()
            .setChatMessage(NetworkProto.ChatMessage.newBuilder()
                .setText(text).setType(type))
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
            // Also freeze if any player is currently attacking this NPC (even before aggro)
            if (isBeingAttackedByPlayer(npc.getId())) continue;

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
        PlayerSession killerSession = nettyServer.getSessions().get(killer.getId());
        if (killerSession == null || killerSession.getQuestManager() == null) {
            return;
        }

        QuestManager questManager = killerSession.getQuestManager();
        for (Quest quest : questManager.getQuests().values()) {
            for (Quest.Task task : quest.tasks) {
                if (task.type != Quest.TaskType.KILL || task.completed || task.targetEntityId != npcDefinitionId) {
                    continue;
                }

                boolean finished = questManager.addTaskProgress(quest.id, task.id, 1);
                Quest updated = questManager.getQuest(quest.id);
                sendQuestUpdate(killerSession, updated);

                if (finished) {
                    LOG.info("Player {} completed kill objective '{}' for quest {}",
                        killer.getId(), task.id, quest.id);
                }
            }
        }
    }

    private void sendQuestUpdate(PlayerSession session, Quest quest) {
        if (session == null || quest == null) {
            return;
        }

        session.getChannel().writeAndFlush(NetworkProto.ServerMessage.newBuilder()
            .setQuestUpdate(NetworkProto.QuestUpdate.newBuilder()
                .setQuestId(quest.id)
                .setQuestName(quest.name)
                .setTasksCompleted(quest.getCompletedTaskCount())
                .setTasksTotal(quest.tasks.size())
                .setCompleted(quest.allTasksCompleted()))
            .build());
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
                    .setCombatLevel(npc.getDefinitionId()))
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

    /**
     * Teleport the player to the world spawn point (Lumbridge) and restore full HP.
     * Broadcasts a PlayerDeath packet to only that player's session, and an EntityUpdate
     * (new position) to all clients so they see the player teleport away.
     */
    private void respawnPlayer(Player player) {
        int spawnX = world.getSpawnX();
        int spawnY = world.getSpawnY();

        // Reset state
        player.setHealth(player.getMaxHealth());
        player.setPosition(spawnX, spawnY);
        player.setCombatTarget(-1);

        // Notify the dying player (death screen + respawn coords)
        nettyServer.sendToSession(player.getId(), NetworkProto.ServerMessage.newBuilder()
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
            nettyServer.sendToSession(killer.getId(), NetworkProto.ServerMessage.newBuilder()
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
        long mainXp = damage * 4L;
        long hpXp   = Math.round(damage * 1.33);

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
                long splitXp = Math.round(damage * 1.33);
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

        nettyServer.sendToSession(player.getId(), NetworkProto.ServerMessage.newBuilder()
            .setSkillUpdate(NetworkProto.SkillUpdate.newBuilder()
                .setSkillIndex(skillIdx)
                .setNewLevel(newLevel)
                .setTotalXp(totalXp)
                .setLeveledUp(leveledUp))
            .build());
    }

    private void updateSkills() {
        // XP awards are now handled immediately in awardCombatXp()
    }

    public long getTickCount() {
        return tickCount;
    }
}
