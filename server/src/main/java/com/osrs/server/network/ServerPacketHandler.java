package com.osrs.server.network;

import com.osrs.protocol.NetworkProto;
import com.osrs.server.world.GroundItem;
import com.osrs.server.world.World;
import com.osrs.shared.EquipmentSlot;
import com.osrs.shared.ItemDefinition;
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
            case PICKUP_ITEM -> handlePickupItem(ctx, packet.getPickupItem());
            case DROP_ITEM -> handleDropItem(ctx, packet.getDropItem());
            case USE_ITEM -> handleUseItem(ctx, packet.getUseItem());
            case SWAP_INVENTORY_SLOTS -> handleSwapInventorySlots(ctx, packet.getSwapInventorySlots());
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
    
    // -----------------------------------------------------------------------
    // Inventory / Item handlers
    // -----------------------------------------------------------------------

    private void handlePickupItem(ChannelHandlerContext ctx, NetworkProto.PickupItem req) {
        if (session.getPlayer() == null) return;
        Player player = session.getPlayer();

        GroundItem gi = server.getWorld().getGroundItem(req.getGroundItemId());
        if (gi == null) {
            LOG.debug("Player {} tried to pick up unknown ground item {}", player.getId(), req.getGroundItemId());
            return;
        }

        // Proximity check (Chebyshev ≤ 2)
        int dist = Math.max(Math.abs(player.getX() - gi.getX()), Math.abs(player.getY() - gi.getY()));
        if (dist > 2) {
            LOG.debug("Player {} too far from ground item {} (dist={})", player.getId(), gi.getGroundItemId(), dist);
            return;
        }

        // Inventory full check
        if (player.isInventoryFull()) {
            LOG.debug("Player {} inventory full, cannot pick up item {}", player.getId(), gi.getItemId());
            return;
        }

        // Stackable items: merge into existing stack if present
        ItemDefinition def = server.getWorld().getItemDef(gi.getItemId());
        if (def.stackable) {
            for (int i = 0; i < 28; i++) {
                if (player.getInventoryItemId(i) == gi.getItemId()) {
                    int newQty = player.getInventoryQuantity(i) + gi.getQuantity();
                    player.setInventoryItem(i, gi.getItemId(), newQty);
                    server.getWorld().removeGroundItem(gi.getGroundItemId());
                    sendInventorySlot(ctx, player, i);
                    server.broadcastToAll(NetworkProto.ServerMessage.newBuilder()
                        .setGroundItemDespawn(NetworkProto.GroundItemDespawn.newBuilder()
                            .setGroundItemId(gi.getGroundItemId()))
                        .build());
                    LOG.info("Player {} picked up {} x{} (stacked)", player.getId(), def.name, gi.getQuantity());
                    return;
                }
            }
        }

        // Place in first empty slot
        int slot = player.getFirstEmptySlot();
        player.setInventoryItem(slot, gi.getItemId(), gi.getQuantity());
        server.getWorld().removeGroundItem(gi.getGroundItemId());

        sendInventorySlot(ctx, player, slot);
        server.broadcastToAll(NetworkProto.ServerMessage.newBuilder()
            .setGroundItemDespawn(NetworkProto.GroundItemDespawn.newBuilder()
                .setGroundItemId(gi.getGroundItemId()))
            .build());
        LOG.info("Player {} picked up {} x{} into slot {}", player.getId(), def.name, gi.getQuantity(), slot);
    }

    private void handleDropItem(ChannelHandlerContext ctx, NetworkProto.DropItem req) {
        if (session.getPlayer() == null) return;
        Player player = session.getPlayer();

        int slot = req.getInventorySlot();
        if (slot < 0 || slot >= 28) return;

        int itemId = player.getInventoryItemId(slot);
        if (itemId == 0) return;

        int qty = player.getInventoryQuantity(slot);

        // Remove from inventory
        player.setInventoryItem(slot, 0, 0);

        // Spawn on ground (owner = player — others can't see for OWNER_ONLY_TICKS)
        // Use a long-enough tick count proxy; GameLoop owns tickCount so we pass -1 as owner
        // to make it immediately public (dropped intentionally by player)
        GroundItem gi = server.getWorld().spawnGroundItem(itemId, qty,
            player.getX(), player.getY(), -1, System.currentTimeMillis() / 4); // approximate tick
        String name = server.getWorld().getItemDef(itemId).name;

        // Send cleared slot back to player
        sendInventorySlot(ctx, player, slot);

        // Broadcast new ground item to everyone (owner=-1 means public immediately)
        server.broadcastToAll(NetworkProto.ServerMessage.newBuilder()
            .setGroundItemSpawn(NetworkProto.GroundItemSpawn.newBuilder()
                .setGroundItemId(gi.getGroundItemId())
                .setItemId(gi.getItemId())
                .setQuantity(gi.getQuantity())
                .setX(gi.getX()).setY(gi.getY())
                .setItemName(name))
            .build());

        LOG.info("Player {} dropped {} x{} at ({},{})", player.getId(), name, qty, player.getX(), player.getY());
    }

    private void handleUseItem(ChannelHandlerContext ctx, NetworkProto.UseItem req) {
        if (session.getPlayer() == null) return;
        Player player = session.getPlayer();

        int slot = req.getInventorySlot();
        if (slot < 0 || slot >= 28) return;

        int itemId = player.getInventoryItemId(slot);
        if (itemId == 0) return;

        ItemDefinition def = server.getWorld().getItemDef(itemId);
        String action = req.getAction();

        if (("eat".equals(action) || "drink".equals(action)) && def.consumable) {
            // Consume: heal HP
            int newHp = Math.min(player.getMaxHealth(), player.getHealth() + def.consumeHeal);
            player.setHealth(newHp);
            player.setInventoryItem(slot, 0, 0);

            // Send cleared slot
            sendInventorySlot(ctx, player, slot);

            // Broadcast health update
            server.broadcastToAll(NetworkProto.ServerMessage.newBuilder()
                .setHealthUpdate(NetworkProto.HealthUpdate.newBuilder()
                    .setEntityId(player.getId())
                    .setHealth(newHp)
                    .setMaxHealth(player.getMaxHealth()))
                .build());
            LOG.info("Player {} ate {} — healed {} HP (now {}/{})",
                player.getId(), def.name, def.consumeHeal, newHp, player.getMaxHealth());

        } else if (("wield".equals(action) || "wear".equals(action)) && def.equipable) {
            int equipSlot = def.equipSlot;
            if (equipSlot < 0 || equipSlot >= EquipmentSlot.COUNT) return;

            // Swap: currently equipped item goes back into inventory slot being vacated
            int oldItemId = player.getEquipment(equipSlot);
            player.setEquipment(equipSlot, itemId);
            player.setInventoryItem(slot, oldItemId, oldItemId > 0 ? 1 : 0);

            // Send updated inventory slot
            sendInventorySlot(ctx, player, slot);

            // Send equipment update
            ItemDefinition newEquipDef = server.getWorld().getItemDef(itemId);
            ctx.writeAndFlush(NetworkProto.ServerMessage.newBuilder()
                .setEquipmentUpdate(NetworkProto.EquipmentUpdate.newBuilder()
                    .setSlot(equipSlot)
                    .setItemId(itemId)
                    .setItemName(newEquipDef.name)
                    .setFlags(newEquipDef.getFlags()))
                .build());
            LOG.info("Player {} equipped {} into slot {}", player.getId(), def.name, equipSlot);
        }
    }

    private void handleSwapInventorySlots(ChannelHandlerContext ctx, NetworkProto.SwapInventorySlots req) {
        if (session.getPlayer() == null) return;
        Player player = session.getPlayer();

        int from = req.getFromSlot();
        int to   = req.getToSlot();
        if (from < 0 || from >= 28 || to < 0 || to >= 28 || from == to) return;

        int fromId  = player.getInventoryItemId(from);
        int fromQty = player.getInventoryQuantity(from);
        int toId    = player.getInventoryItemId(to);
        int toQty   = player.getInventoryQuantity(to);

        player.setInventoryItem(from, toId,   toQty);
        player.setInventoryItem(to,   fromId, fromQty);

        sendInventorySlot(ctx, player, from);
        sendInventorySlot(ctx, player, to);
        LOG.debug("Player {} swapped inventory slots {} ↔ {}", player.getId(), from, to);
    }

    /**
     * Helper: build and send an InventoryUpdate for a single slot to the given context.
     */
    private void sendInventorySlot(ChannelHandlerContext ctx, Player player, int slot) {
        int itemId = player.getInventoryItemId(slot);
        int qty    = player.getInventoryQuantity(slot);
        ItemDefinition def = server.getWorld().getItemDef(itemId);
        ctx.writeAndFlush(NetworkProto.ServerMessage.newBuilder()
            .setInventoryUpdate(NetworkProto.InventoryUpdate.newBuilder()
                .setSlot(slot)
                .setItemId(itemId)
                .setQuantity(qty)
                .setItemName(itemId > 0 ? def.name : "")
                .setFlags(itemId > 0 ? def.getFlags() : 0))
            .build());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOG.error("Exception in packet handler", cause);
        ctx.close();
    }
}
