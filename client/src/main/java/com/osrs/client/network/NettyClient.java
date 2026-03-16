package com.osrs.client.network;

import com.osrs.protocol.NetworkProto;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

/**
 * Client-side Netty connection to game server.
 */
public class NettyClient {
    
    private static final Logger LOG = LoggerFactory.getLogger(NettyClient.class);
    private static final String HOST = "localhost";
    private static final int PORT = 43594;
    private static final int MAX_FRAME_LENGTH = 1024 * 64;
    
    private EventLoopGroup group;
    private Channel channel;
    private ClientPacketHandler handler;
    private CountDownLatch connectedLatch = new CountDownLatch(1);
    
    public void connect() throws Exception {
        group = new NioEventLoopGroup();
        
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        
                        pipeline.addLast(new LengthFieldBasedFrameDecoder(MAX_FRAME_LENGTH, 0, 4, 0, 4));
                        pipeline.addLast(new ProtobufDecoder(NetworkProto.HandshakeResponse.getDefaultInstance()));
                        
                        pipeline.addLast(new LengthFieldPrepender(4));
                        pipeline.addLast(new ProtobufEncoder());
                        
                        handler = new ClientPacketHandler(NettyClient.this);
                        pipeline.addLast(handler);
                    }
                })
                .option(ChannelOption.SO_KEEPALIVE, true);
            
            ChannelFuture future = bootstrap.connect(HOST, PORT).sync();
            channel = future.channel();
            LOG.info("Connected to server at {}:{}", HOST, PORT);
            
            // Wait for connection acknowledgement
            connectedLatch.await();
            LOG.info("Server acknowledged connection");
            
        } catch (Exception e) {
            LOG.error("Failed to connect to server", e);
            throw e;
        }
    }
    
    public void disconnect() throws Exception {
        LOG.info("Disconnecting from server");
        if (channel != null) {
            channel.close().sync();
        }
        group.shutdownGracefully();
    }
    
    public void sendHandshake(String username) {
        NetworkProto.Handshake handshake = NetworkProto.Handshake.newBuilder()
            .setUsername(username)
            .setPassword("dummy")
            .build();
        
        channel.writeAndFlush(handshake);
        LOG.debug("Sent handshake: {}", username);
    }
    
    public void sendPlayerMovement(int x, int y, int facing) {
        NetworkProto.PlayerMovement movement = NetworkProto.PlayerMovement.newBuilder()
            .setX(x)
            .setY(y)
            .setFacing(facing)
            .setSequence(System.currentTimeMillis())
            .build();
        
        channel.writeAndFlush(movement);
    }
    
    public boolean isConnected() {
        return channel != null && channel.isActive();
    }
    
    public void setConnectedLatch() {
        connectedLatch.countDown();
    }
    
    public ClientPacketHandler getHandler() {
        return handler;
    }
}
