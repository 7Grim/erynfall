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
    private static final int PORT = 43594;
    private static final int MAX_FRAME_LENGTH = 1024 * 64;
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    
    private final Map<Integer, PlayerSession> sessions = new HashMap<>();
    private int nextSessionId = 1;
    
    public void start() throws Exception {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        
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
                        
                        // Protobuf decoders (generic message)
                        pipeline.addLast(new ProtobufDecoder(NetworkProto.Handshake.getDefaultInstance()));
                        
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
            
            serverChannel = bootstrap.bind(PORT).sync().channel();
            LOG.info("Netty server listening on port {}", PORT);
            
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
}
