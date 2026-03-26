package com.osrs.server.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import com.osrs.protocol.NetworkProto;
import com.osrs.server.GameContent;
import com.osrs.server.quest.DialogueEngine;
import com.osrs.server.quest.Quest;
import com.osrs.server.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Netty server listening on port 43594.
 * Handles client connections + packet routing.
 */
public class NettyServer {

    private static final Logger LOG = LoggerFactory.getLogger(NettyServer.class);
    private static final int MAX_FRAME_LENGTH = 1024 * 64;

    private final int port;
    private final int bossThreads;
    private final int workerThreads;
    private final World world;
    private final GameContent gameContent;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    private final Map<Integer, PlayerSession> sessions = new HashMap<>();
    private int nextSessionId = 1;

    /** Shared tick counter — written by GameLoop, read by ServerPacketHandler for pickup scheduling. */
    private volatile long currentTick = 0;

    public long getCurrentTick()            { return currentTick; }
    public void setCurrentTick(long tick)   { this.currentTick = tick; }

    public NettyServer(int port, int bossThreads, int workerThreads, World world, GameContent gameContent) {
        this.port = port;
        this.bossThreads = bossThreads;
        this.workerThreads = workerThreads;
        this.world = world;
        this.gameContent = gameContent;
    }

    public World getWorld() { return world; }
    public DialogueEngine getDialogueEngine() { return gameContent.getDialogueEngine(); }
    public String getInitialDialogueIdForNpc(int npcId) { return gameContent.getInitialDialogueIdForNpc(npcId); }
    public Map<Integer, Quest> getQuestDefinitions() { return gameContent.getQuestDefinitions(); }

    public void start() throws Exception {
        bossGroup = new NioEventLoopGroup(bossThreads);
        workerGroup = new NioEventLoopGroup(workerThreads);
        
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        
                        // Frame length decoder (message framing)
                        pipeline.addLast(new LengthFieldBasedFrameDecoder(MAX_FRAME_LENGTH, 0, 4, 0, 4));
                        
                        // Protobuf decoder: decode all client packets as ClientMessage wrapper
                        pipeline.addLast(new ProtobufDecoder(NetworkProto.ClientMessage.getDefaultInstance()));
                        
                        // Frame length encoder
                        pipeline.addLast(new LengthFieldPrepender(4));
                        
                        // Protobuf encoder
                        pipeline.addLast(new ProtobufEncoder());
                        
                        // Custom handler
                        pipeline.addLast(new ServerPacketHandler(NettyServer.this));
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
            
            serverChannel = bootstrap.bind(port).sync().channel();
            LOG.info("Netty server listening on port {}", port);
            
        } catch (Exception e) {
            LOG.error("Failed to start Netty server", e);
            throw e;
        }
    }
    
    public void stop() throws Exception {
        LOG.info("Stopping Netty server");
        if (serverChannel != null) {
            serverChannel.close().sync();
        }
        
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        LOG.info("Netty server stopped");
    }
    
    public PlayerSession createSession(Channel channel) {
        int sessionId = nextSessionId++;
        PlayerSession session = new PlayerSession(sessionId, channel);
        sessions.put(sessionId, session);
        LOG.info("Created session {} for {}", sessionId, channel.remoteAddress());
        return session;
    }
    
    public void removeSession(int sessionId) {
        sessions.remove(sessionId);
        LOG.debug("Removed session {}", sessionId);
    }
    
    public Map<Integer, PlayerSession> getSessions() {
        return sessions;
    }

    /**
     * Broadcast a ServerMessage to all connected, authenticated sessions.
     */
    public void broadcastToAll(NetworkProto.ServerMessage message) {
        for (PlayerSession session : sessions.values()) {
            if (session.isAuthenticated() && session.getChannel().isActive()) {
                session.getChannel().writeAndFlush(message);
            }
        }
    }

    /**
     * Send a ServerMessage to a specific session.
     */
    public void sendToSession(int sessionId, NetworkProto.ServerMessage message) {
        PlayerSession session = sessions.get(sessionId);
        if (session != null && session.getChannel().isActive()) {
            session.getChannel().writeAndFlush(message);
        }
    }
}
