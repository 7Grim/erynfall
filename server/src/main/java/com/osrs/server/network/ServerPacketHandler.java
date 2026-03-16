package com.osrs.server.network;

import com.osrs.protocol.NetworkProto;
import com.osrs.shared.NPC;
import com.osrs.shared.Player;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles incoming packets from clients.
 */
public class ServerPacketHandler extends SimpleChannelInboundHandler<Object> {
    
    private static final Logger LOG = LoggerFactory.getLogger(ServerPacketHandler.class);
    
    private final NettyServer server;
    private PlayerSession session;
    
    public ServerPacketHandler(NettyServer server) {
        this.server = server;
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // New client connected
        session = server.createSession(ctx.channel());
        LOG.info("Client connected from {}", ctx.channel().remoteAddress());
        
        // Send welcome message
        sendHandshakeResponse(ctx, true, "Welcome to OSRS MMORP", 1);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        // Client disconnected
        if (session != null) {
            LOG.info("Client {} disconnected", session.getSessionId());
            server.removeSession(session.getSessionId());
        }
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        // Handle incoming packets
        if (msg instanceof NetworkProto.PlayerMovement) {
            handlePlayerMovement(ctx, (NetworkProto.PlayerMovement) msg);
        } else if (msg instanceof NetworkProto.WalkTo) {
            handleWalkTo(ctx, (NetworkProto.WalkTo) msg);
        } else if (msg instanceof NetworkProto.Handshake) {
            handleHandshake(ctx, (NetworkProto.Handshake) msg);
        }
    }
    
    private void handleHandshake(ChannelHandlerContext ctx, NetworkProto.Handshake handshake) {
        LOG.debug("Handshake from {}: {}", session.getSessionId(), handshake.getUsername());
        
        // Create player
        Player player = new Player(session.getSessionId(), handshake.getUsername(), 50, 50);
        session.setPlayer(player);
        session.setAuthenticated(true);
        
        // Send response
        sendHandshakeResponse(ctx, true, "Login successful", session.getSessionId());
        
        // Send world state
        sendWorldState(ctx);
    }
    
    private void handlePlayerMovement(ChannelHandlerContext ctx, NetworkProto.PlayerMovement movement) {
        if (session.getPlayer() == null) {
            return;
        }
        
        // TODO: Validate movement (S1-009)
        Player player = session.getPlayer();
        player.setPosition(movement.getX(), movement.getY());
        player.setFacing(movement.getFacing());
        
        LOG.debug("Player {} moved to ({}, {})", 
            session.getSessionId(), movement.getX(), movement.getY());
    }
    
    private void handleWalkTo(ChannelHandlerContext ctx, NetworkProto.WalkTo walkTo) {
        if (session.getPlayer() == null) {
            return;
        }
        
        Player player = session.getPlayer();
        int targetX = walkTo.getTargetX();
        int targetY = walkTo.getTargetY();
        
        // TODO: Calculate pathfinding via World.findPath()
        // For now, just validate and move directly
        player.setPosition(targetX, targetY);
        
        LOG.debug("Player {} walk-to request: ({}, {})", 
            session.getSessionId(), targetX, targetY);
    }
    
    private void sendHandshakeResponse(ChannelHandlerContext ctx, boolean success, String message, int playerId) {
        NetworkProto.HandshakeResponse response = NetworkProto.HandshakeResponse.newBuilder()
            .setSuccess(success)
            .setMessage(message)
            .setPlayerId(playerId)
            .build();
        
        ctx.writeAndFlush(response);
    }
    
    private void sendWorldState(ChannelHandlerContext ctx) {
        // TODO: Send full world state (S1-010)
        // For now, just acknowledge
        LOG.debug("Sending world state to session {}", session.getSessionId());
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOG.error("Exception in packet handler", cause);
        ctx.close();
    }
}
