package com.osrs.server;

import com.osrs.server.combat.CombatEngine;
import com.osrs.server.network.NettyServer;
import com.osrs.server.world.World;
import com.osrs.shared.NPC;
import com.osrs.shared.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    
    public GameLoop(long tickIntervalNs, World world, NettyServer nettyServer) {
        this.tickIntervalNs = tickIntervalNs;
        this.world = world;
        this.nettyServer = nettyServer;
        this.combatEngine = new CombatEngine();
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
            // Stage 1: Input dequeue (dequeue packets from Netty)
            // Currently handled by ServerPacketHandler directly
            // TODO: Create InputTicker to centralize this
            
            // Stage 2: Movement update (update player positions via pathfinding)
            // TODO: Create MovementTicker
            updateEntityPositions();
            
            // Stage 3: Combat calculation (CombatEngine integration)
            // TODO: Create CombatTicker with proper stage 3 semantics
            processCombat();
            
            // Stage 4: Skill progression (XP awards, level-ups)
            // TODO: Create SkillTicker
            updateSkills();
            
            // Stage 5: Loot generation (drop items, remove dead entities)
            // TODO: Create LootTicker
            
            // Stage 6: Broadcast to clients (send delta packets)
            // TODO: Create BroadcastTicker
            // broadcastWorldState();
            
        } catch (Exception e) {
            LOG.error("Error in tick {}", tickCount, e);
        }
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
     * Update entity positions (movement animation interpolation).
     * TODO: Smooth movement between waypoints.
     */
    private void updateEntityPositions() {
        // Placeholder: positions are updated immediately in ServerPacketHandler
        // In a full implementation, we'd animate movement here
    }
    
    /**
     * Process all active combats.
     * Each player in combat rolls hit/miss/damage every attack speed ticks.
     */
    private void processCombat() {
        for (Player player : world.getPlayers().values()) {
            if (!player.isInCombat()) {
                continue;
            }
            
            NPC target = world.getNPC(player.getCombatTarget());
            if (target == null || target.getHealth() <= 0) {
                // Target is dead or doesn't exist
                player.setCombatTarget(-1);
                LOG.debug("Player {} combat ended (target dead/gone)", player.getId());
                continue;
            }
            
            // Attack every 4 ticks (default weapon speed)
            // TODO: Check weapon for actual attack speed
            int attackSpeed = 4;
            if (tickCount - player.getLastAttackTick() >= attackSpeed) {
                // Roll hit/miss/damage
                CombatEngine.HitResult result = combatEngine.calculateHit(player, target, tickCount);
                
                // Apply damage
                if (result.hit) {
                    target.setHealth(target.getHealth() - result.damage);
                    LOG.debug("Player {} hit {} for {}", 
                        player.getId(), target.getId(), result.damage);
                } else {
                    LOG.debug("Player {} missed {}", player.getId(), target.getId());
                }
                
                // Award XP (TODO: to actual stats)
                // player.getStats().addExperience(Skill.STRENGTH, result.xpAwarded);
                
                // Update last attack tick
                player.setLastAttackTick(tickCount);
                
                // TODO: Broadcast CombatHit packet to clients
            }
        }
    }
    
    /**
     * Update skill experience and levels.
     * TODO: Process XP awards and level-up events.
     */
    private void updateSkills() {
        // Placeholder: XP tracking is in Stats class
        // In a full implementation, we'd process level-up events here
    }
    
    public long getTickCount() {
        return tickCount;
    }
}
