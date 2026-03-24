package com.osrs.server.network;

import com.osrs.protocol.NetworkProto;
import com.osrs.server.world.World;
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
            Player player = session.getPlayer();
            if (player != null) {
                // Clean up any active dialogue state
                int npcId = player.getDialogueTarget();
                if (npcId >= 0) {
                    NPC npc = server.getWorld().getNPC(npcId);
                    if (npc != null) {
                        npc.setInDialogue(false);
                        npc.setDialoguePlayer(-1);
                    }
                }
                server.getWorld().getPlayers().remove(player.getId());
            }
            server.removeSession(session.getSessionId());
        }
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof NetworkProto.ClientMessage)) {
            LOG.warn("Unknown message type: {}", msg.getClass().getSimpleName());
            return;
        }
        NetworkProto.ClientMessage packet = (NetworkProto.ClientMessage) msg;
        switch (packet.getPayloadCase()) {
            case HANDSHAKE -> handleHandshake(ctx, packet.getHandshake());
            case MOVEMENT -> handlePlayerMovement(ctx, packet.getMovement());
            case WALK_TO -> handleWalkTo(ctx, packet.getWalkTo());
            case ATTACK -> handleAttack(ctx, packet.getAttack());
            case DIALOGUE_RESPONSE -> handleDialogueResponse(ctx, packet.getDialogueResponse());
            case TALK_TO_NPC -> handleTalkToNpc(ctx, packet.getTalkToNpc());
            default -> LOG.warn("Unhandled payload case: {}", packet.getPayloadCase());
        }
    }
    
    private void handleHandshake(ChannelHandlerContext ctx, NetworkProto.Handshake handshake) {
        LOG.debug("Handshake from {}: {}", session.getSessionId(), handshake.getUsername());
        
        // Create player and register in world
        Player player = new Player(session.getSessionId(), handshake.getUsername(), 50, 50);
        session.setPlayer(player);
        session.setAuthenticated(true);
        server.getWorld().getPlayers().put(player.getId(), player);
        LOG.info("Player {} (id={}) registered in world", handshake.getUsername(), player.getId());
        
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
        NetworkProto.ServerMessage wrapped = NetworkProto.ServerMessage.newBuilder()
            .setHandshakeResponse(response)
            .build();
        ctx.writeAndFlush(wrapped);
    }
    
    private void handleAttack(ChannelHandlerContext ctx, NetworkProto.Attack attack) {
        if (session.getPlayer() == null) {
            return;
        }
        
        Player player = session.getPlayer();
        int targetId = attack.getTargetId();
        
        // Find target (could be NPC or another player)
        // For now, assume NPC
        LOG.info("Player {} initiating attack on entity {}", 
            session.getSessionId(), targetId);
        
        player.setCombatTarget(targetId);
    }
    
    private void handleDialogueResponse(ChannelHandlerContext ctx, NetworkProto.DialogueResponse response) {
        if (session.getPlayer() == null) return;

        Player player = session.getPlayer();
        LOG.debug("Player {} selected dialogue option: {}", session.getSessionId(), response.getOptionId());

        // Clear dialogue state on both sides
        int npcId = player.getDialogueTarget();
        if (npcId >= 0) {
            NPC npc = server.getWorld().getNPC(npcId);
            if (npc != null) {
                npc.setInDialogue(false);
                npc.setDialoguePlayer(-1);
            }
            player.setDialogueTarget(-1);
        }
        // TODO: Progress dialogue via DialogueEngine (S3)
    }

    private void handleTalkToNpc(ChannelHandlerContext ctx, NetworkProto.TalkToNpc request) {
        if (session.getPlayer() == null) return;

        Player player = session.getPlayer();
        NPC npc = server.getWorld().getNPC(request.getNpcId());
        if (npc == null) {
            LOG.warn("Player {} tried to talk to unknown NPC {}", player.getId(), request.getNpcId());
            return;
        }

        // Verify proximity (player must be adjacent — Chebyshev ≤ 1)
        int chebyshev = Math.max(
            Math.abs(player.getX() - npc.getX()),
            Math.abs(player.getY() - npc.getY())
        );
        if (chebyshev > 1) {
            LOG.debug("Player {} too far from NPC {} to talk (dist={})", player.getId(), npc.getId(), chebyshev);
            return;
        }

        // If already in dialogue with someone else, ignore
        if (npc.isInDialogue() && npc.getDialoguePlayer() != player.getId()) {
            LOG.debug("NPC {} already in dialogue with another player", npc.getId());
            return;
        }

        // Freeze the NPC and link both sides
        npc.setInDialogue(true);
        npc.setDialoguePlayer(player.getId());
        player.setDialogueTarget(npc.getId());

        LOG.info("Player {} started dialogue with NPC {} ({})", player.getId(), npc.getId(), npc.getName());

        // Send initial DialoguePrompt (TODO: route through DialogueEngine in S3)
        ctx.writeAndFlush(NetworkProto.ServerMessage.newBuilder()
            .setDialoguePrompt(NetworkProto.DialoguePrompt.newBuilder()
                .setNpcId(npc.getId())
                .setNpcSays("Hello, adventurer! Welcome to Tutorial Island. How can I help you?")
                .addOptions(NetworkProto.DialogueOption.newBuilder().setOptionId(0).setText("Goodbye.")))
            .build());
    }
    
    private void sendWorldState(ChannelHandlerContext ctx) {
        NetworkProto.WorldState.Builder wsBuilder = NetworkProto.WorldState.newBuilder()
            .setServerTick(0);

        // All NPCs
        for (com.osrs.shared.NPC npc : server.getWorld().getNPCs().values()) {
            wsBuilder.addEntities(NetworkProto.Entity.newBuilder()
                .setId(npc.getId())
                .setName(npc.getName())
                .setX(npc.getX())
                .setY(npc.getY())
                .setHealth(npc.getHealth())
                .setMaxHealth(npc.getMaxHealth())
                .setIsPlayer(false));
        }

        // All other connected players (excluding self)
        for (PlayerSession ps : server.getSessions().values()) {
            if (ps.getSessionId() == session.getSessionId()) continue;
            if (ps.getPlayer() == null) continue;
            com.osrs.shared.Player p = ps.getPlayer();
            wsBuilder.addEntities(NetworkProto.Entity.newBuilder()
                .setId(p.getId())
                .setName(p.getName())
                .setX(p.getX())
                .setY(p.getY())
                .setIsPlayer(true));
        }

        ctx.writeAndFlush(NetworkProto.ServerMessage.newBuilder()
            .setWorldState(wsBuilder.build())
            .build());
        LOG.info("Sent WorldState to session {}: {} entities",
            session.getSessionId(), wsBuilder.getEntitiesCount());
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOG.error("Exception in packet handler", cause);
        ctx.close();
    }
}
