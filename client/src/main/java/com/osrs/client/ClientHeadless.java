package com.osrs.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Headless client for testing on M2 Mac (no graphics).
 * Useful for network testing without LibGDX/LWJGL3 overhead.
 * 
 * Use: mvn exec:java -Dexec.mainClass="com.osrs.client.ClientHeadless"
 */
public class ClientHeadless {
    
    private static final Logger LOG = LoggerFactory.getLogger(ClientHeadless.class);
    
    public static void main(String[] args) {
        LOG.info("OSRS MMORP Client (Headless) starting...");
        LOG.info("OS: {}", System.getProperty("os.name"));
        LOG.info("Arch: {}", System.getProperty("os.arch"));
        
        LOG.info("Client initialized (headless mode)");
        LOG.info("Ready for network testing");
        
        // Keep running
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
