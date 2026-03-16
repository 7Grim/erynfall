package com.osrs.client.network;

import com.osrs.protocol.NetworkProto;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles incoming packets from server.
 */
public class ClientPacketHandler extends SimpleChannelInboundHandler<Object> {
    
    private static final Logger LOG = LoggerFactory.getLogger(ClientPacketHandler.class);
    
    private final NettyClient client;
    private NetworkProto.HandshakeResponse lastHandshakeResponse;
    
    public ClientPacketHandler(NettyClient client) {
        this.client = client;
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof NetworkProto.HandshakeResponse) {
            handleHandshakeResponse(ctx, (NetworkProto.HandshakeResponse) msg);
        } else if (msg instanceof NetworkProto.WorldState) {
            handleWorldState(ctx, (NetworkProto.WorldState) msg);
        }
    }
    
    private void handleHandshakeResponse(ChannelHandlerContext ctx, NetworkProto.HandshakeResponse response) {
        LOG.info("Handshake response: success={}, message={}, playerId={}", 
            response.getSuccess(), response.getMessage(), response.getPlayerId());
        
        this.lastHandshakeResponse = response;
        client.setConnectedLatch();
    }
    
    private void handleWorldState(ChannelHandlerContext ctx, NetworkProto.WorldState worldState) {
        LOG.debug("Received world state: {} entities at tick {}", 
            worldState.getEntitiesCount(), worldState.getServerTick());
    }
    
    public NetworkProto.HandshakeResponse getLastHandshakeResponse() {
        return lastHandshakeResponse;
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOG.error("Exception in packet handler", cause);
        ctx.close();
    }
}
