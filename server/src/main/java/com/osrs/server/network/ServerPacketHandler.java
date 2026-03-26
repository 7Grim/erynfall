package com.osrs.server.network;

import com.osrs.protocol.NetworkProto;
import com.osrs.server.database.DatabaseManager;
import com.osrs.server.database.PlayerRepository;
import com.osrs.server.quest.DialogueEngine;
import com.osrs.server.quest.Quest;
import com.osrs.server.quest.QuestManager;
import com.osrs.server.world.GroundItem;
import com.osrs.server.world.World;
import com.osrs.shared.CombatStyle;
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
    private static final int MAX_STEP = 1;
    private static final int TRANSIENT_PLAYER_ID_OFFSET = 900_000;
    
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
        if (session != null) {
            LOG.info("Client {} disconnected", session.getSessionId());
            Player player = session.getPlayer();
            if (player != null) {
                closeDialogue(player);
                // Save player state to DB on disconnect
                if (session.isAuthenticated() && DatabaseManager.isHealthy()) {
                    PlayerRepository.savePlayer(player);
                    PlayerRepository.saveInventory(player);
                    LOG.info("Saved player {} on disconnect", player.getName());
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
            case SET_COMBAT_STYLE    -> handleSetCombatStyle(packet.getSetCombatStyle());
            case PUBLIC_CHAT         -> handlePublicChat(ctx, packet.getPublicChat());
            default -> LOG.warn("Unhandled payload case: {}", packet.getPayloadCase());
        }
    }
    
    private void handleHandshake(ChannelHandlerContext ctx, NetworkProto.Handshake handshake) {
        String username = handshake.getUsername().trim();
        String password = handshake.getPassword();
        LOG.info("Handshake from session {}: username={}", session.getSessionId(), username);

        // Basic validation
        if (username.isEmpty() || username.length() > 12 || password.isEmpty()) {
            sendHandshakeResponse(ctx, false, "Invalid username or password.", 0);
            return;
        }

        Player player = null;

        if (DatabaseManager.isHealthy()) {
            // Try login first; if account doesn't exist, register a new one
            player = PlayerRepository.login(username, password, session.getSessionId());
            if (player == null) {
                // Not found → attempt registration (first time playing)
                player = PlayerRepository.register(username, password, session.getSessionId());
                if (player == null) {
                    // Username taken with wrong password
                    sendHandshakeResponse(ctx, false, "Incorrect password.", 0);
                    return;
                }
                LOG.info("New account created: {}", username);
            } else {
                // Load inventory for returning player
                PlayerRepository.loadInventory(player);
            }
        } else {
            // DB offline: fall back to in-memory session (no persistence)
            LOG.warn("DB unavailable — logging in {} without persistence", username);
            player = new Player(
                TRANSIENT_PLAYER_ID_OFFSET + session.getSessionId(),
                username,
                server.getWorld().getSpawnX(),
                server.getWorld().getSpawnY()
            );
        }

        // Heal legacy accounts whose persisted coordinates are outside this local map.
        if (!server.getWorld().canWalkTo(player.getX(), player.getY())) {
            LOG.warn("Player {} had out-of-bounds position ({},{}); resetting to spawn ({},{})",
                username, player.getX(), player.getY(), server.getWorld().getSpawnX(), server.getWorld().getSpawnY());
            player.setPosition(server.getWorld().getSpawnX(), server.getWorld().getSpawnY());
        }

        session.setPlayer(player);
        session.setAuthenticated(true);
        initializeQuestsForSession();
        server.getWorld().getPlayers().put(player.getId(), player);
        LOG.info("Player {} (id={}) entered world at ({},{})", username, player.getId(), player.getX(), player.getY());

        sendHandshakeResponse(ctx, true, "Welcome back, " + username + "!", player.getId());

        // Send initial skill state so the client's skills tab is populated immediately
        sendAllSkillUpdates(ctx, player);

        sendInitialQuestState(ctx);

        sendWorldState(ctx);
    }

    /** Sends a SkillUpdate (leveled_up=false) for every skill — used on login to sync client. */
    private void sendAllSkillUpdates(ChannelHandlerContext ctx, Player player) {
        for (int i = 0; i < Player.SKILL_COUNT; i++) {
            ctx.writeAndFlush(NetworkProto.ServerMessage.newBuilder()
                .setSkillUpdate(NetworkProto.SkillUpdate.newBuilder()
                    .setSkillIndex(i)
                    .setNewLevel(player.getSkillLevel(i))
                    .setTotalXp(player.getSkillXp(i))
                    .setLeveledUp(false))
                .build());
        }
    }
    
    private void handlePlayerMovement(ChannelHandlerContext ctx, NetworkProto.PlayerMovement movement) {
        if (session.getPlayer() == null) {
            return;
        }

        Player player = session.getPlayer();
        int fromX = player.getX();
        int fromY = player.getY();
        int toX = movement.getX();
        int toY = movement.getY();

        int chebyshev = Math.max(Math.abs(toX - fromX), Math.abs(toY - fromY));
        if (chebyshev > MAX_STEP) {
            LOG.warn("Rejected movement for player {}: step too large from ({},{}) to ({},{})",
                player.getId(), fromX, fromY, toX, toY);
            sendPositionCorrection(ctx, player);
            return;
        }

        if (!server.getWorld().canWalkTo(toX, toY)) {
            LOG.debug("Rejected movement for player {}: blocked tile ({},{})", player.getId(), toX, toY);
            sendChatMessage(ctx, "I can't reach that!", 1);
            sendPositionCorrection(ctx, player);
            return;
        }

        if (player.isInDialogue() && (toX != fromX || toY != fromY)) {
            closeDialogue(player);
        }

        player.setPosition(toX, toY);
        player.setFacing(movement.getFacing());
        
        LOG.debug("Player {} moved to ({}, {})", 
            session.getSessionId(), movement.getX(), movement.getY());
    }
    
    private void handleWalkTo(ChannelHandlerContext ctx, NetworkProto.WalkTo walkTo) {
        if (session.getPlayer() == null) return;
        Player player = session.getPlayer();

        int tx = walkTo.getTargetX();
        int ty = walkTo.getTargetY();
        if (!server.getWorld().canWalkTo(tx, ty)) {
            sendChatMessage(ctx, "I can't reach that!", 1);
            LOG.debug("Rejected walk-to for player {}: target ({},{}) not walkable", player.getId(), tx, ty);
            return;
        }

        if (!server.getWorld().canReach(player.getX(), player.getY(), tx, ty)) {
            sendChatMessage(ctx, "I can't reach that!", 1);
            LOG.debug("Rejected walk-to for player {}: no path to ({},{})", player.getId(), tx, ty);
            return;
        }

        // Walk destination noted. Actual position is updated tile-by-tile via
        // PlayerMovement packets as the client crosses each tile boundary.
        LOG.debug("Player {} walk-to destination: ({}, {})",
            session.getSessionId(), walkTo.getTargetX(), walkTo.getTargetY());
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
        NPC target = server.getWorld().getNPC(targetId);
        if (target == null || target.isDead()) {
            sendChatMessage(ctx, "They seem to be gone.", 1);
            return;
        }
        if (target.getCombatLevel() <= 0) {
            sendChatMessage(ctx, "You can't attack them.", 1);
            return;
        }

        if (player.isInDialogue()) {
            closeDialogue(player);
        }
        
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

        if (!player.isInDialogue()) {
            LOG.debug("Ignoring dialogue response from player {} — not in dialogue", player.getId());
            return;
        }

        NPC npc = server.getWorld().getNPC(player.getDialogueTarget());
        DialogueEngine.Dialogue next = server.getDialogueEngine().selectOption(player.getId(), response.getOptionId());
        if (next == null) {
            closeDialogue(player);
            sendDialoguePrompt(ctx, -1, "Farewell.");
            return;
        }

        // Keep lock consistent if dialogue hops across NPC IDs.
        if (npc != null && next.npcId != npc.getId()) {
            npc.setInDialogue(false);
            npc.setDialoguePlayer(-1);
        }
        NPC nextNpc = server.getWorld().getNPC(next.npcId);
        if (nextNpc != null) {
            nextNpc.setInDialogue(true);
            nextNpc.setDialoguePlayer(player.getId());
        }
        player.setDialogueTarget(next.npcId);
        sendDialoguePrompt(ctx, next);
    }

    private void handleTalkToNpc(ChannelHandlerContext ctx, NetworkProto.TalkToNpc request) {
        if (session.getPlayer() == null) return;

        Player player = session.getPlayer();
        NPC npc = server.getWorld().getNPC(request.getNpcId());
        if (npc == null) {
            LOG.warn("Player {} tried to talk to unknown NPC {}", player.getId(), request.getNpcId());
            return;
        }

        if (player.isInDialogue() && player.getDialogueTarget() != npc.getId()) {
            LOG.debug("Player {} tried to start a second dialogue", player.getId());
            return;
        }

        if (player.isInDialogue() && player.getDialogueTarget() == npc.getId()) {
            DialogueEngine.Dialogue current = server.getDialogueEngine().getCurrentDialogue(player.getId());
            if (current != null) {
                sendDialoguePrompt(ctx, current);
                return;
            }
        }

        // Verify proximity (talking requires adjacency)
        int chebyshev = Math.max(
            Math.abs(player.getX() - npc.getX()),
            Math.abs(player.getY() - npc.getY())
        );
        if (chebyshev > 1) {
            LOG.debug("Player {} too far from NPC {} to talk (dist={})", player.getId(), npc.getId(), chebyshev);
            sendChatMessage(ctx, "I can't reach that!", 1);
            return;
        }

        // If already in dialogue with someone else, ignore
        if (npc.isInDialogue() && npc.getDialoguePlayer() != player.getId()) {
            LOG.debug("NPC {} already in dialogue with another player", npc.getId());
            return;
        }

        String entryDialogueId = server.getInitialDialogueIdForNpc(npc.getId());
        if (entryDialogueId == null) {
            sendChatMessage(ctx, "They have nothing interesting to say right now.", 2);
            return;
        }

        DialogueEngine.Dialogue dialogue = server.getDialogueEngine().startDialogue(player.getId(), entryDialogueId);
        if (dialogue == null) {
            LOG.warn("NPC {} has entry dialogue id '{}' but no registered dialogue", npc.getId(), entryDialogueId);
            sendChatMessage(ctx, "The conversation trails off awkwardly.", 2);
            return;
        }

        // Freeze the NPC and link both sides
        npc.setInDialogue(true);
        npc.setDialoguePlayer(player.getId());
        player.setDialogueTarget(npc.getId());

        LOG.info("Player {} started dialogue with NPC {} ({})", player.getId(), npc.getId(), npc.getName());
        updateDialogueQuestObjectives(ctx, npc.getId());
        sendDialoguePrompt(ctx, dialogue);
    }

    private void initializeQuestsForSession() {
        if (session.getQuestManager() != null || session.getPlayer() == null) {
            return;
        }

        QuestManager questManager = new QuestManager(session.getPlayer().getId());
        for (Quest quest : server.getQuestDefinitions().values()) {
            questManager.startQuest(quest);
        }
        session.setQuestManager(questManager);
    }

    private void sendInitialQuestState(ChannelHandlerContext ctx) {
        QuestManager questManager = session.getQuestManager();
        if (questManager == null) {
            return;
        }

        for (Quest quest : questManager.getQuests().values()) {
            sendQuestUpdate(ctx, quest);
        }
    }

    private void updateDialogueQuestObjectives(ChannelHandlerContext ctx, int npcId) {
        QuestManager questManager = session.getQuestManager();
        if (questManager == null) {
            return;
        }

        for (Quest quest : questManager.getQuests().values()) {
            for (Quest.Task task : quest.tasks) {
                if (task.type != Quest.TaskType.DIALOGUE || task.completed || task.targetEntityId != npcId) {
                    continue;
                }

                boolean completed = questManager.addTaskProgress(quest.id, task.id, 1);
                if (completed) {
                    sendQuestUpdate(ctx, questManager.getQuest(quest.id));
                }
            }
        }
    }

    private void sendQuestUpdate(ChannelHandlerContext ctx, Quest quest) {
        if (quest == null) {
            return;
        }

        int completed = quest.getCompletedTaskCount();
        int total = quest.tasks.size();
        boolean done = quest.allTasksCompleted();

        ctx.writeAndFlush(NetworkProto.ServerMessage.newBuilder()
            .setQuestUpdate(NetworkProto.QuestUpdate.newBuilder()
                .setQuestId(quest.id)
                .setQuestName(quest.name)
                .setTasksCompleted(completed)
                .setTasksTotal(total)
                .setCompleted(done))
            .build());
    }

    private void closeDialogue(Player player) {
        int npcId = player.getDialogueTarget();
        if (npcId >= 0) {
            NPC npc = server.getWorld().getNPC(npcId);
            if (npc != null) {
                npc.setInDialogue(false);
                npc.setDialoguePlayer(-1);
            }
        }
        server.getDialogueEngine().endDialogue(player.getId());
        player.setDialogueTarget(-1);
    }

    private void sendDialoguePrompt(ChannelHandlerContext ctx, DialogueEngine.Dialogue dialogue) {
        sendDialoguePrompt(ctx, dialogue.npcId, dialogue.npcText, dialogue.options);
    }

    private void sendDialoguePrompt(ChannelHandlerContext ctx, int npcId, String npcSays) {
        sendDialoguePrompt(ctx, npcId, npcSays, null);
    }

    private void sendDialoguePrompt(ChannelHandlerContext ctx, int npcId, String npcSays,
                                    java.util.List<DialogueEngine.DialogueOption> options) {
        NetworkProto.DialoguePrompt.Builder prompt = NetworkProto.DialoguePrompt.newBuilder()
            .setNpcId(npcId)
            .setNpcSays(npcSays == null ? "" : npcSays);
        if (options != null) {
            for (DialogueEngine.DialogueOption opt : options) {
                prompt.addOptions(NetworkProto.DialogueOption.newBuilder()
                    .setOptionId(opt.optionId)
                    .setText(opt.text));
            }
        }

        ctx.writeAndFlush(NetworkProto.ServerMessage.newBuilder()
            .setDialoguePrompt(prompt)
            .build());
    }

    private void sendPositionCorrection(ChannelHandlerContext ctx, Player player) {
        ctx.writeAndFlush(NetworkProto.ServerMessage.newBuilder()
            .setEntityUpdate(NetworkProto.EntityUpdate.newBuilder()
                .setEntityId(player.getId())
                .setX(player.getX())
                .setY(player.getY()))
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
                .setCombatLevel(npc.getCombatLevel())
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

    /**
     * OSRS-accurate pickup: validates proximity then schedules a 3-OSRS-tick
     * (≈ 462 server ticks) animation delay before the item enters inventory.
     *
     * Validation:
     *   • Item must still exist on the ground
     *   • Player must be within 1 tile (Chebyshev) — they must walk there first
     *   • Inventory must not be full
     *
     * The actual inventory transfer is executed by GameLoop.processPendingPickups().
     */
    private void handlePickupItem(ChannelHandlerContext ctx, NetworkProto.PickupItem req) {
        if (session.getPlayer() == null) return;
        Player player = session.getPlayer();

        GroundItem gi = server.getWorld().getGroundItem(req.getGroundItemId());
        if (gi == null) {
            // Item already gone (despawned or picked up by someone else)
            sendChatMessage(ctx, "Too late — it's gone!", 1);
            return;
        }

        // OSRS-like pickup stance: player should stand on the item tile.
        if (player.getX() != gi.getX() || player.getY() != gi.getY()) {
            LOG.debug("Player {} not on ground item {} tile (player={},{} item={},{}); client should walk first",
                player.getId(), gi.getGroundItemId(), player.getX(), player.getY(), gi.getX(), gi.getY());
            sendChatMessage(ctx, "I can't reach that!", 1);
            return;
        }

        // Inventory check up-front (early-out with OSRS message)
        if (player.isInventoryFull()) {
            ItemDefinition def = server.getWorld().getItemDef(gi.getItemId());
            // Stackable items might still fit — check before rejecting
            boolean hasStack = false;
            if (def.stackable) {
                for (int i = 0; i < 28; i++) {
                    if (player.getInventoryItemId(i) == gi.getItemId()) { hasStack = true; break; }
                }
            }
            if (!hasStack) {
                sendChatMessage(ctx, "Your inventory is too full to pick up the " + def.name + ".", 1);
                return;
            }
        }

        // Schedule pickup after 3 OSRS ticks (≈ 462 server ticks at 256 Hz)
        // 1 OSRS tick = 0.6 s; 256 Hz × 0.6 s ≈ 154 server ticks; 3 × 154 = 462
        long currentTick = server.getCurrentTick();
        server.getWorld().schedulePendingPickup(player, gi.getGroundItemId(), currentTick + 462, ctx);
        LOG.debug("Player {} scheduled pickup of ground item {} at tick {}",
            player.getId(), gi.getGroundItemId(), currentTick + 462);
    }

    /** Send an OSRS-style game / error message only to this player's session. */
    private void sendChatMessage(ChannelHandlerContext ctx, String text, int type) {
        ctx.writeAndFlush(NetworkProto.ServerMessage.newBuilder()
            .setChatMessage(NetworkProto.ChatMessage.newBuilder()
                .setText(text).setType(type))
            .build());
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

            // Update player's effective attack range from the equipped weapon
            // Weapon slot = EquipmentSlot.WEAPON (index 3); only weapons override range
            if (equipSlot == EquipmentSlot.WEAPON) {
                player.setWeaponAttackRange(def.attackRange);
            }
            // Unequipping (swapping weapon back to empty): restore melee range
            if (equipSlot == EquipmentSlot.WEAPON && itemId == 0) {
                player.setWeaponAttackRange(1);
            }

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
     * Public chat: validate the message and broadcast it to all players within
     * PUBLIC_CHAT_RANGE tiles (Chebyshev) of the sender — matching OSRS "local scene" range.
     * The sender also receives their own broadcast so they see it in their own chat box.
     */
    private static final int PUBLIC_CHAT_RANGE = 15;
    private static final int PUBLIC_CHAT_MAX_LENGTH = 80;

    private void handlePublicChat(ChannelHandlerContext ctx, NetworkProto.PublicChat req) {
        if (session.getPlayer() == null) return;
        Player sender = session.getPlayer();

        String text = req.getText().trim();
        if (text.isEmpty() || text.length() > PUBLIC_CHAT_MAX_LENGTH) return;

        LOG.info("Player {} says: {}", sender.getName(), text);

        NetworkProto.ServerMessage broadcast = NetworkProto.ServerMessage.newBuilder()
            .setChatBroadcast(NetworkProto.ChatBroadcast.newBuilder()
                .setSenderId(sender.getId())
                .setSenderName(sender.getName())
                .setText(text)
                .setX(sender.getX())
                .setY(sender.getY()))
            .build();

        // Broadcast to all players within range, including the sender
        for (PlayerSession s : server.getSessions().values()) {
            if (!s.isAuthenticated() || s.getPlayer() == null) continue;
            Player other = s.getPlayer();
            int dist = Math.max(
                Math.abs(other.getX() - sender.getX()),
                Math.abs(other.getY() - sender.getY())
            );
            if (dist <= PUBLIC_CHAT_RANGE) {
                s.getChannel().writeAndFlush(broadcast);
            }
        }
    }

    private void handleSetCombatStyle(NetworkProto.SetCombatStyle req) {
        if (session.getPlayer() == null) return;
        CombatStyle style = CombatStyle.fromIndex(req.getStyle());
        session.getPlayer().setCombatStyle(style);
        LOG.info("Player {} set combat style to {}", session.getPlayer().getId(), style.displayName);
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
