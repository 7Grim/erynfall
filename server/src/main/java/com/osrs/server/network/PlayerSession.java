package com.osrs.server.network;

import com.osrs.server.quest.QuestManager;
import com.osrs.shared.Player;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a connected player session.
 */
public class PlayerSession {
    
    private static final Logger LOG = LoggerFactory.getLogger(PlayerSession.class);
    
    private final int sessionId;
    private final Channel channel;
    private Player player;
    private QuestManager questManager;
    private boolean authenticated = false;
    
    public PlayerSession(int sessionId, Channel channel) {
        this.sessionId = sessionId;
        this.channel = channel;
    }
    
    public int getSessionId() {
        return sessionId;
    }
    
    public Channel getChannel() {
        return channel;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public void setPlayer(Player player) {
        this.player = player;
    }

    public QuestManager getQuestManager() {
        return questManager;
    }

    public void setQuestManager(QuestManager questManager) {
        this.questManager = questManager;
    }
    
    public boolean isAuthenticated() {
        return authenticated;
    }
    
    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }
    
    public boolean isConnected() {
        return channel.isActive();
    }
}
