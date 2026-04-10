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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Client-side Netty connection to game server.
 */
public class NettyClient {
    
    private static final Logger LOG = LoggerFactory.getLogger(NettyClient.class);
    private static final String HOST = resolveHost();
    private static final int PORT = 43594;

    private static String resolveHost() {
        String h = System.getProperty("GAME_SERVER_HOST");
        if (h != null && !h.isBlank()) return h.trim();
        h = System.getenv("GAME_SERVER_HOST");
        if (h != null && !h.isBlank()) return h.trim();
        try (java.io.InputStream is = NettyClient.class.getResourceAsStream("/auth.properties")) {
            if (is != null) {
                java.util.Properties props = new java.util.Properties();
                props.load(is);
                String fromFile = props.getProperty("game.server.host");
                if (fromFile != null && !fromFile.isBlank()) return fromFile.trim();
            }
        } catch (Exception ignored) {}
        return "localhost";
    }
    private static final int MAX_FRAME_LENGTH = 1024 * 64;
    
    private EventLoopGroup group;
    private Channel channel;
    private ClientPacketHandler handler;
    private volatile CountDownLatch handshakeLatch = new CountDownLatch(1);
    private volatile NetworkProto.HandshakeResponse lastHandshakeResponse;
    private final AtomicLong friendActionSequence = new AtomicLong(1);
    
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
                        pipeline.addLast(new ProtobufDecoder(NetworkProto.ServerMessage.getDefaultInstance()));
                        
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
    
    public void sendHandshake(String username, String password) {
        lastHandshakeResponse = null;
        handshakeLatch = new CountDownLatch(1);

        NetworkProto.ClientMessage msg = NetworkProto.ClientMessage.newBuilder()
            .setHandshake(NetworkProto.Handshake.newBuilder()
                .setUsername(username)
                .setPassword(password))
            .build();
        channel.writeAndFlush(msg);
        LOG.debug("Sent handshake: {}", username);
    }

    public void sendTokenHandshake(String accessToken) {
        lastHandshakeResponse = null;
        handshakeLatch = new CountDownLatch(1);

        NetworkProto.ClientMessage msg = NetworkProto.ClientMessage.newBuilder()
            .setHandshake(NetworkProto.Handshake.newBuilder()
                .setAccessToken(accessToken))
            .build();
        channel.writeAndFlush(msg);
        LOG.debug("Sent token handshake");
    }

    public NetworkProto.HandshakeResponse awaitHandshakeResponse(long timeoutMs) throws InterruptedException {
        if (!handshakeLatch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
            return null;
        }
        return lastHandshakeResponse;
    }

    public void sendPlayerMovement(int x, int y, int facing) {
        NetworkProto.ClientMessage msg = NetworkProto.ClientMessage.newBuilder()
            .setMovement(NetworkProto.PlayerMovement.newBuilder()
                .setX(x).setY(y).setFacing(facing).setSequence(System.currentTimeMillis()))
            .build();
        channel.writeAndFlush(msg);
    }

    public void sendWalkTo(int targetX, int targetY) {
        NetworkProto.ClientMessage msg = NetworkProto.ClientMessage.newBuilder()
            .setWalkTo(NetworkProto.WalkTo.newBuilder()
                .setTargetX(targetX).setTargetY(targetY).setSequence(System.currentTimeMillis()))
            .build();
        channel.writeAndFlush(msg);
        LOG.debug("Sent WalkTo: ({}, {})", targetX, targetY);
    }

    public void sendAttack(int targetId) {
        NetworkProto.ClientMessage msg = NetworkProto.ClientMessage.newBuilder()
            .setAttack(NetworkProto.Attack.newBuilder()
                .setTargetId(targetId).setSequence(System.currentTimeMillis()))
            .build();
        channel.writeAndFlush(msg);
        LOG.debug("Sent Attack on entity {}", targetId);
    }

    public void sendTalkToNpc(int npcId) {
        NetworkProto.ClientMessage msg = NetworkProto.ClientMessage.newBuilder()
            .setTalkToNpc(NetworkProto.TalkToNpc.newBuilder()
                .setNpcId(npcId).setSequence(System.currentTimeMillis()))
            .build();
        channel.writeAndFlush(msg);
        LOG.debug("Sent TalkToNpc: npc {}", npcId);
    }

    public void sendClaimNpcSupplies(int npcId) {
        if (channel == null || !channel.isActive()) return;
        NetworkProto.ClientMessage msg = NetworkProto.ClientMessage.newBuilder()
            .setClaimNpcSupplies(NetworkProto.ClaimNpcSupplies.newBuilder()
                .setNpcId(npcId)
                .setSequence(System.currentTimeMillis()))
            .build();
        channel.writeAndFlush(msg);
        LOG.debug("Sent ClaimNpcSupplies: npcId={}", npcId);
    }

    public void sendOpenSmeltingMenu(int furnaceNpcId) {
        if (channel == null || !channel.isActive()) return;
        channel.writeAndFlush(NetworkProto.ClientMessage.newBuilder()
            .setOpenSmeltingMenu(NetworkProto.OpenSmeltingMenu.newBuilder()
                .setNpcId(furnaceNpcId)
                .setSequence(System.currentTimeMillis()))
            .build());
        LOG.debug("Sent OpenSmeltingMenu: furnaceNpcId={}", furnaceNpcId);
    }

    public void sendStartSmelting(int furnaceNpcId, int barItemId) {
        if (channel == null || !channel.isActive()) return;
        channel.writeAndFlush(NetworkProto.ClientMessage.newBuilder()
            .setStartSmelting(NetworkProto.StartSmelting.newBuilder()
                .setNpcId(furnaceNpcId)
                .setBarItemId(barItemId)
                .setSequence(System.currentTimeMillis()))
            .build());
        LOG.debug("Sent StartSmelting: furnaceNpcId={} barItemId={}", furnaceNpcId, barItemId);
    }

    public void sendCloseSmeltingMenu() {
        if (channel == null || !channel.isActive()) return;
        channel.writeAndFlush(NetworkProto.ClientMessage.newBuilder()
            .setCloseSmeltingMenu(NetworkProto.CloseSmeltingMenu.newBuilder()
                .setSequence(System.currentTimeMillis()))
            .build());
        LOG.debug("Sent CloseSmeltingMenu");
    }

    public void sendOpenSmithingMenu(int anvilNpcId) {
        if (channel == null || !channel.isActive()) return;
        channel.writeAndFlush(NetworkProto.ClientMessage.newBuilder()
            .setOpenSmithingMenu(NetworkProto.OpenSmithingMenu.newBuilder()
                .setNpcId(anvilNpcId)
                .setSequence(System.currentTimeMillis()))
            .build());
        LOG.debug("Sent OpenSmithingMenu: anvilNpcId={}", anvilNpcId);
    }

    public void sendStartSmithingProduct(int anvilNpcId, int productItemId) {
        if (channel == null || !channel.isActive()) return;
        channel.writeAndFlush(NetworkProto.ClientMessage.newBuilder()
            .setStartSmithingProduct(NetworkProto.StartSmithingProduct.newBuilder()
                .setNpcId(anvilNpcId)
                .setProductItemId(productItemId)
                .setSequence(System.currentTimeMillis()))
            .build());
        LOG.debug("Sent StartSmithingProduct: anvilNpcId={} productItemId={}", anvilNpcId, productItemId);
    }

    public void sendCloseSmithingMenu() {
        if (channel == null || !channel.isActive()) return;
        channel.writeAndFlush(NetworkProto.ClientMessage.newBuilder()
            .setCloseSmithingMenu(NetworkProto.CloseSmithingMenu.newBuilder()
                .setSequence(System.currentTimeMillis()))
            .build());
        LOG.debug("Sent CloseSmithingMenu");
    }

    public void sendOpenShop(int npcId) {
        if (channel == null || !channel.isActive()) return;
        channel.writeAndFlush(NetworkProto.ClientMessage.newBuilder()
            .setOpenShop(NetworkProto.OpenShop.newBuilder()
                .setNpcId(npcId)
                .setSequence(System.currentTimeMillis()))
            .build());
        LOG.debug("Sent OpenShop: npcId={}", npcId);
    }

    public void sendBuyShopItem(int npcId, int itemId, int quantity) {
        if (channel == null || !channel.isActive()) return;
        channel.writeAndFlush(NetworkProto.ClientMessage.newBuilder()
            .setBuyShopItem(NetworkProto.BuyShopItem.newBuilder()
                .setNpcId(npcId)
                .setItemId(itemId)
                .setQuantity(quantity)
                .setSequence(System.currentTimeMillis()))
            .build());
        LOG.debug("Sent BuyShopItem: npcId={} itemId={} qty={}", npcId, itemId, quantity);
    }

    public void sendCloseShop() {
        if (channel == null || !channel.isActive()) return;
        channel.writeAndFlush(NetworkProto.ClientMessage.newBuilder()
            .setCloseShop(NetworkProto.CloseShop.newBuilder()
                .setSequence(System.currentTimeMillis()))
            .build());
        LOG.debug("Sent CloseShop");
    }

    public void sendOpenBankRequest(int npcId) {
        NetworkProto.ClientMessage msg = NetworkProto.ClientMessage.newBuilder()
            .setOpenBankRequest(NetworkProto.OpenBankRequest.newBuilder()
                .setNpcId(npcId)
                .setSequence(System.currentTimeMillis()))
            .build();
        channel.writeAndFlush(msg);
        LOG.debug("Sent OpenBankRequest: npc {}", npcId);
    }

    public void sendCloseBankRequest() {
        NetworkProto.ClientMessage msg = NetworkProto.ClientMessage.newBuilder()
            .setCloseBankRequest(NetworkProto.CloseBankRequest.newBuilder()
                .setSequence(System.currentTimeMillis()))
            .build();
        channel.writeAndFlush(msg);
        LOG.debug("Sent CloseBankRequest");
    }

    public void sendDepositBankItem(int inventorySlot, int amount) {
        if (channel == null || !channel.isActive()) return;
        channel.writeAndFlush(NetworkProto.ClientMessage.newBuilder()
            .setDepositBankItem(NetworkProto.DepositBankItem.newBuilder()
                .setInventorySlot(inventorySlot)
                .setAmount(amount)
                .setSequence(System.currentTimeMillis()))
            .build());
        LOG.debug("Sent DepositBankItem: slot={} amount={}", inventorySlot, amount);
    }

    public void sendWithdrawBankItem(int bankSlot, int amount) {
        if (channel == null || !channel.isActive()) return;
        channel.writeAndFlush(NetworkProto.ClientMessage.newBuilder()
            .setWithdrawBankItem(NetworkProto.WithdrawBankItem.newBuilder()
                .setBankSlot(bankSlot)
                .setAmount(amount)
                .setSequence(System.currentTimeMillis()))
            .build());
        LOG.debug("Sent WithdrawBankItem: slot={} amount={}", bankSlot, amount);
    }

    public void sendRearrangeBankSlots(int fromSlot, int toSlot) {
        if (channel == null || !channel.isActive()) return;
        channel.writeAndFlush(NetworkProto.ClientMessage.newBuilder()
            .setRearrangeBankSlots(NetworkProto.RearrangeBankSlots.newBuilder()
                .setFromSlot(fromSlot)
                .setToSlot(toSlot)
                .setSequence(System.currentTimeMillis()))
            .build());
        LOG.debug("Sent RearrangeBankSlots: {} -> {}", fromSlot, toSlot);
    }

    public void sendMoveBankItemToTab(int bankSlot, int targetTabIndex) {
        if (channel == null || !channel.isActive()) return;
        channel.writeAndFlush(NetworkProto.ClientMessage.newBuilder()
            .setMoveBankItemToTab(NetworkProto.MoveBankItemToTab.newBuilder()
                .setBankSlot(bankSlot)
                .setTargetTabIndex(targetTabIndex)
                .setSequence(System.currentTimeMillis()))
            .build());
        LOG.debug("Sent MoveBankItemToTab: slot={} tab={}", bankSlot, targetTabIndex);
    }

    public void sendAdminSetSkillLevel(int skillIdx, int targetLevel) {
        if (channel == null || !channel.isActive()) return;
        channel.writeAndFlush(NetworkProto.ClientMessage.newBuilder()
            .setAdminSetSkillLevel(NetworkProto.AdminSetSkillLevel.newBuilder()
                .setSkillIndex(skillIdx)
                .setTargetLevel(targetLevel)
                .setSequence(System.currentTimeMillis()))
            .build());
        LOG.debug("Sent AdminSetSkillLevel: skill={} level={}", skillIdx, targetLevel);
    }

    public void sendAdminAdjustSkillXp(int skillIdx, long deltaXpWhole) {
        if (channel == null || !channel.isActive()) return;
        channel.writeAndFlush(NetworkProto.ClientMessage.newBuilder()
            .setAdminAdjustSkillXp(NetworkProto.AdminAdjustSkillXp.newBuilder()
                .setSkillIndex(skillIdx)
                .setDeltaXpWhole(deltaXpWhole)
                .setSequence(System.currentTimeMillis()))
            .build());
        LOG.debug("Sent AdminAdjustSkillXp: skill={} deltaXpWhole={}", skillIdx, deltaXpWhole);
    }

    public void sendAdminSearchItems(String query) {
        if (channel == null || !channel.isActive()) return;
        channel.writeAndFlush(NetworkProto.ClientMessage.newBuilder()
            .setAdminSearchItems(NetworkProto.AdminSearchItems.newBuilder()
                .setQuery(query == null ? "" : query)
                .setSequence(System.currentTimeMillis()))
            .build());
        LOG.debug("Sent AdminSearchItems: query='{}'", query);
    }

    public void sendAdminGiveItem(int itemId, long quantity, NetworkProto.AdminItemDestination destination) {
        if (channel == null || !channel.isActive()) return;
        channel.writeAndFlush(NetworkProto.ClientMessage.newBuilder()
            .setAdminGiveItem(NetworkProto.AdminGiveItem.newBuilder()
                .setItemId(itemId)
                .setQuantity(quantity)
                .setDestination(destination)
                .setSequence(System.currentTimeMillis()))
            .build());
        LOG.debug("Sent AdminGiveItem: itemId={} qty={} dest={}", itemId, quantity, destination);
    }

    public void sendAdminTeleport(NetworkProto.AdminTravelDestination destination) {
        if (channel == null || !channel.isActive()) return;
        channel.writeAndFlush(NetworkProto.ClientMessage.newBuilder()
            .setAdminTeleport(NetworkProto.AdminTeleport.newBuilder()
                .setDestination(destination)
                .setSequence(System.currentTimeMillis()))
            .build());
        LOG.debug("Sent AdminTeleport: destination={}", destination);
    }

    public void sendStartSkilling(int npcId, NetworkProto.SkillingType skillingType) {
        sendStartSkilling(npcId, skillingType, NetworkProto.FishingActionType.FISHING_ACTION_NONE);
    }

    public void sendStartSkilling(int npcId,
                                  NetworkProto.SkillingType skillingType,
                                  NetworkProto.FishingActionType fishingAction) {
        NetworkProto.ClientMessage msg = NetworkProto.ClientMessage.newBuilder()
            .setStartSkilling(NetworkProto.StartSkillingRequest.newBuilder()
                .setTargetNpcId(npcId)
                .setSkillingType(skillingType)
                .setSequence(System.currentTimeMillis())
                .setFishingAction(fishingAction))
            .build();
        channel.writeAndFlush(msg);
        LOG.debug("Sent StartSkillingRequest: npc={} type={} fishingAction={}", npcId, skillingType, fishingAction);
    }

    public void sendDialogueResponse(int optionId) {
        NetworkProto.ClientMessage msg = NetworkProto.ClientMessage.newBuilder()
            .setDialogueResponse(NetworkProto.DialogueResponse.newBuilder()
                .setOptionId(optionId).setSequence(System.currentTimeMillis()))
            .build();
        channel.writeAndFlush(msg);
        LOG.debug("Sent dialogue response: option {}", optionId);
    }

    public void sendPickupItem(int groundItemId) {
        NetworkProto.ClientMessage msg = NetworkProto.ClientMessage.newBuilder()
            .setPickupItem(NetworkProto.PickupItem.newBuilder()
                .setGroundItemId(groundItemId).setSequence(System.currentTimeMillis()))
            .build();
        channel.writeAndFlush(msg);
        LOG.debug("Sent PickupItem: groundItemId={}", groundItemId);
    }

    public void sendDropItem(int inventorySlot) {
        NetworkProto.ClientMessage msg = NetworkProto.ClientMessage.newBuilder()
            .setDropItem(NetworkProto.DropItem.newBuilder()
                .setInventorySlot(inventorySlot).setSequence(System.currentTimeMillis()))
            .build();
        channel.writeAndFlush(msg);
        LOG.debug("Sent DropItem: slot={}", inventorySlot);
    }

    public void sendUseItem(int inventorySlot, String action) {
        NetworkProto.ClientMessage msg = NetworkProto.ClientMessage.newBuilder()
            .setUseItem(NetworkProto.UseItem.newBuilder()
                .setInventorySlot(inventorySlot).setAction(action)
                .setSequence(System.currentTimeMillis()))
            .build();
        channel.writeAndFlush(msg);
        LOG.debug("Sent UseItem: slot={} action={}", inventorySlot, action);
    }

    public void sendUnequipItem(int equipmentSlot) {
        if (channel == null || !channel.isActive()) return;
        channel.writeAndFlush(NetworkProto.ClientMessage.newBuilder()
            .setUnequipItem(NetworkProto.UnequipItem.newBuilder()
                .setEquipmentSlot(equipmentSlot))
            .build());
    }

    public void sendUseItemOnItem(int sourceSlot, int targetSlot) {
        NetworkProto.ClientMessage msg = NetworkProto.ClientMessage.newBuilder()
            .setUseItemOnItem(NetworkProto.UseItemOnItem.newBuilder()
                .setSourceSlot(sourceSlot)
                .setTargetSlot(targetSlot)
                .setSequence(System.currentTimeMillis()))
            .build();
        channel.writeAndFlush(msg);
        LOG.debug("Sent UseItemOnItem: src={} tgt={}", sourceSlot, targetSlot);
    }

    public void sendSetCombatStyle(int styleIndex) {
        NetworkProto.ClientMessage msg = NetworkProto.ClientMessage.newBuilder()
            .setSetCombatStyle(NetworkProto.SetCombatStyle.newBuilder()
                .setStyle(styleIndex).setSequence(System.currentTimeMillis()))
            .build();
        channel.writeAndFlush(msg);
        LOG.debug("Sent SetCombatStyle: {}", styleIndex);
    }

    public void sendSwapInventorySlots(int fromSlot, int toSlot) {
        NetworkProto.ClientMessage msg = NetworkProto.ClientMessage.newBuilder()
            .setSwapInventorySlots(NetworkProto.SwapInventorySlots.newBuilder()
                .setFromSlot(fromSlot).setToSlot(toSlot)
                .setSequence(System.currentTimeMillis()))
            .build();
        channel.writeAndFlush(msg);
        LOG.debug("Sent SwapInventorySlots: {} ↔ {}", fromSlot, toSlot);
    }
    
    public void sendPublicChat(String text) {
        if (text == null || text.trim().isEmpty()) return;
        NetworkProto.ClientMessage msg = NetworkProto.ClientMessage.newBuilder()
            .setPublicChat(NetworkProto.PublicChat.newBuilder()
                .setText(text.trim()))
            .build();
        channel.writeAndFlush(msg);
        LOG.debug("Sent PublicChat: {}", text);
    }

    public void sendExamineNpc(int npcId) {
        NetworkProto.ClientMessage msg = NetworkProto.ClientMessage.newBuilder()
            .setExamineNpc(NetworkProto.ExamineNpc.newBuilder()
                .setNpcId(npcId))
            .build();
        channel.writeAndFlush(msg);
        LOG.debug("Sent ExamineNpc: npc {}", npcId);
    }

    public void sendExamineItem(int inventorySlot) {
        if (channel == null || !channel.isActive()) return;
        channel.writeAndFlush(NetworkProto.ClientMessage.newBuilder()
            .setExamineItem(NetworkProto.ExamineItem.newBuilder()
                .setInventorySlot(inventorySlot))
            .build());
        LOG.debug("Sent ExamineItem: slot={}", inventorySlot);
    }

    public void sendTogglePrayer(int prayerId) {
        if (channel == null || !channel.isActive()) return;
        channel.writeAndFlush(NetworkProto.ClientMessage.newBuilder()
            .setTogglePrayer(NetworkProto.TogglePrayer.newBuilder()
                .setPrayerId(prayerId)
                .setSequence(System.currentTimeMillis()))
            .build());
        LOG.debug("Sent TogglePrayer: id={}", prayerId);
    }

    public void sendSetSpell(int spellId) {
        if (channel == null || !channel.isActive()) return;
        channel.writeAndFlush(NetworkProto.ClientMessage.newBuilder()
            .setSetSpell(NetworkProto.SetSpell.newBuilder()
                .setSpellId(spellId)
                .setSequence(System.currentTimeMillis()))
            .build());
        LOG.debug("Sent SetSpell: id={}", spellId);
    }

    public void sendSetAutoRetaliate(boolean enabled) {
        if (channel == null || !channel.isActive()) return;
        channel.writeAndFlush(NetworkProto.ClientMessage.newBuilder()
            .setSetAutoRetaliate(NetworkProto.SetAutoRetaliate.newBuilder()
                .setEnabled(enabled))
            .build());
        LOG.debug("Sent SetAutoRetaliate: {}", enabled);
    }

    public long sendFriendAction(NetworkProto.FriendAction.Action action, long playerId, String name) {
        if (channel == null || !channel.isActive()) return -1L;
        long sequence = friendActionSequence.getAndIncrement();
        channel.writeAndFlush(NetworkProto.ClientMessage.newBuilder()
            .setFriendAction(NetworkProto.FriendAction.newBuilder()
                .setAction(action)
                .setPlayerId(playerId)
                .setName(name == null ? "" : name)
                .setSequence(sequence))
            .build());
        LOG.debug("Sent FriendAction: action={} playerId={} name={}", action, playerId, name);
        return sequence;
    }

    public void sendLogoutRequest() {
        NetworkProto.ClientMessage msg = NetworkProto.ClientMessage.newBuilder()
            .setLogoutRequest(NetworkProto.LogoutRequest.newBuilder()
                .setSequence(System.currentTimeMillis()))
            .build();
        channel.writeAndFlush(msg);
        LOG.debug("Sent LogoutRequest");
    }

    public boolean isConnected() {
        return channel != null && channel.isActive();
    }
    
    public void setHandshakeResponse(NetworkProto.HandshakeResponse response) {
        lastHandshakeResponse = response;
        handshakeLatch.countDown();
    }
    
    public ClientPacketHandler getHandler() {
        return handler;
    }
}
