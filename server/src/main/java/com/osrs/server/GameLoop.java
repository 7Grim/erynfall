package com.osrs.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 256-tick game loop running on a dedicated thread.
 * 
 * Each tick (3.9ms) processes:
 * 1. Player input
 * 2. Entity updates
 * 3. Combat calculations
 * 4. Delta synchronization
 */
public class GameLoop {
    
    private static final Logger LOG = LoggerFactory.getLogger(GameLoop.class);
    
    private final long tickIntervalNs;
    private volatile boolean running = false;
    private Thread loopThread;
    private long tickCount = 0;
    
    public GameLoop(long tickIntervalNs) {
        this.tickIntervalNs = tickIntervalNs;
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
    
    private void processTick() {
        // Tick processing stages (framework for future sprints):
        // 1. Dequeue client input packets (S1-005)
        // 2. Update entity positions (S1-009)
        // 3. Validate collisions / pathfinding (S1-009)
        // 4. Execute combat calculations (S2-011)
        // 5. Build delta updates + broadcast (S1-005)
    }
    
    public long getTickCount() {
        return tickCount;
    }
}
