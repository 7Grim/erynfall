package com.osrs.server.network;

import com.osrs.protocol.NetworkProto;
import com.osrs.server.combat.EquipmentBonusCalculator;
import com.osrs.server.auth.AuthTokenSettings;
import com.osrs.server.auth.JwtAccessTokenVerifier;
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
import com.osrs.shared.SkillingAction;
import com.osrs.shared.WoodcuttingRegistry;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles incoming packets from clients.
 */
public class ServerPacketHandler extends SimpleChannelInboundHandler<Object> {
    
    private static final Logger LOG = LoggerFactory.getLogger(ServerPacketHandler.class);
    private static final int MAX_STEP = 1;
    private static final int TRANSIENT_PLAYER_ID_OFFSET = 900_000;
    private static final int MAX_FRIENDS = 200;
    private static final int BRONZE_AXE_ITEM_ID = 1351;
    private static final int SMALL_FISHING_NET_ITEM_ID = 303;
    private static final int RAW_SHRIMPS_ITEM_ID = 317;
    private static final int BONES_ITEM_ID = 526;
    private static final int  TINDERBOX_ITEM_ID   = 590;
    private static final int[] FIREMAKING_LOG_ITEM_IDS = {1511, 1521, 1522, 1523, 1524, 1525};
    private static final long BONES_PRAYER_XP = 45L;  // 4.5 XP stored as tenths
    /** Prayer defs: {prayerId, levelRequired}. IDs 1–6 = F2P melee prayers. */
    private static final int[][] PRAYER_DEFS = {
        {1,  1},  // Thick Skin        (+5% Defence)
        {2,  4},  // Burst of Strength (+5% Strength)
        {3,  7},  // Clarity of Thought(+5% Attack)
        {4, 10},  // Rock Skin         (+10% Defence)
        {5, 13},  // Superhuman Strength(+10% Strength)
        {6, 16},  // Improved Reflexes (+10% Attack)
    };
    private static final long FIREMAKING_LOG_XP   = 400L;  // 40.0 XP stored as tenths
    
    private final NettyServer server;
    private final AuthTokenSettings authTokenSettings;
    private final JwtAccessTokenVerifier jwtAccessTokenVerifier;
    private PlayerSession session;
    
    public ServerPacketHandler(NettyServer server) {
        this.server = server;
        this.authTokenSettings = new AuthTokenSettings();
        this.jwtAccessTokenVerifier = new JwtAccessTokenVerifier(authTokenSettings);
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // New client connected
        session = server.createSession(ctx.channel());
        LOG.info("Client connected from {}", ctx.channel().remoteAddress());
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
                    PlayerRepository.saveEquipment(player);
                    PlayerRepository.saveFriends(player);
                    PlayerRepository.saveQuestProgress(player, session.getQuestManager());
                    LOG.info("Saved player {} on disconnect", player.getName());
                }
                // Broadcast player departure before removing from world/session maps
                NetworkProto.ServerMessage leaveMsg = NetworkProto.ServerMessage.newBuilder()
                    .setNpcDespawn(NetworkProto.NpcDespawn.newBuilder()
                        .setNpcId(player.getId()))
                    .build();
                server.broadcastToAll(leaveMsg);
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
            case UNEQUIP_ITEM -> handleUnequipItem(ctx, packet.getUnequipItem());
            case USE_ITEM_ON_ITEM -> handleUseItemOnItem(ctx, packet.getUseItemOnItem());
            case SWAP_INVENTORY_SLOTS -> handleSwapInventorySlots(ctx, packet.getSwapInventorySlots());
            case SET_COMBAT_STYLE    -> handleSetCombatStyle(packet.getSetCombatStyle());
            case PUBLIC_CHAT         -> handlePublicChat(ctx, packet.getPublicChat());
            case EXAMINE_NPC         -> handleExamineNpc(ctx, packet.getExamineNpc());
            case EXAMINE_ITEM        -> handleExamineItem(ctx, packet.getExamineItem());
            case FRIEND_ACTION       -> handleFriendAction(ctx, packet.getFriendAction());
            case LOGOUT_REQUEST      -> handleLogoutRequest(ctx, packet.getLogoutRequest());
            case START_SKILLING      -> handleStartSkilling(ctx, packet.getStartSkilling());
            case TOGGLE_PRAYER       -> handleTogglePrayer(ctx, packet.getTogglePrayer());
            case SET_AUTO_RETALIATE  -> handleSetAutoRetaliate(packet.getSetAutoRetaliate());
            default -> LOG.warn("Unhandled payload case: {}", packet.getPayloadCase());
        }
    }
    
    private void handleHandshake(ChannelHandlerContext ctx, NetworkProto.Handshake handshake) {
        String accessToken = handshake.getAccessToken().trim();
        if (!accessToken.isEmpty()) {
            handleTokenHandshake(ctx, accessToken);
            return;
        }

        if (!authTokenSettings.allowLegacyLogin()) {
            sendHandshakeResponse(ctx, false, "Access token required.", 0);
            return;
        }

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
                PlayerRepository.loadFriends(player);
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

        ensureStarterSkillingTools(player);

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
        sendFullInventory(ctx, player);
        sendFullEquipment(ctx, player);
        sendFriendsListUpdate(ctx, player);

        sendInitialQuestState(ctx);

        sendWorldState(ctx);

        // Notify already-connected clients of the new player
        Player newPlayer = session.getPlayer();
        if (newPlayer != null) {
            NetworkProto.ServerMessage joinMsg = NetworkProto.ServerMessage.newBuilder()
                .setEntityUpdate(NetworkProto.EntityUpdate.newBuilder()
                    .setEntityId(newPlayer.getId())
                    .setX(newPlayer.getX())
                    .setY(newPlayer.getY())
                    .setIsPlayer(true)
                    .setName(newPlayer.getName()))
                .build();
            for (PlayerSession ps : server.getSessions().values()) {
                if (ps.getSessionId() == session.getSessionId()) continue;
                if (!ps.isAuthenticated() || ps.getPlayer() == null) continue;
                ps.getChannel().writeAndFlush(joinMsg);
            }
        }
    }

    private void handleTokenHandshake(ChannelHandlerContext ctx, String accessToken) {
        LOG.info("Token handshake from session {}", session.getSessionId());

        if (!jwtAccessTokenVerifier.isConfigured()) {
            LOG.error("Token login rejected — JWT_SIGNING_KEY not configured on game server");
            sendHandshakeResponse(ctx, false, "Token auth not configured on server.", 0);
            return;
        }

        if (!DatabaseManager.isHealthy()) {
            sendHandshakeResponse(ctx, false, "Database unavailable.", 0);
            return;
        }

        JwtAccessTokenVerifier.VerifiedAccessToken verified;
        try {
            verified = jwtAccessTokenVerifier.verify(accessToken);
        } catch (JwtException | IllegalStateException ex) {
            LOG.warn("Token handshake rejected for session {}: {}", session.getSessionId(), ex.getMessage());
            sendHandshakeResponse(ctx, false, "Invalid or expired access token.", 0);
            return;
        }

        PlayerRepository.AuthCharacter authCharacter = PlayerRepository.findActiveAuthCharacterById(
            authTokenSettings.authDbSchema(),
            verified.characterId()
        );
        if (authCharacter == null) {
            sendHandshakeResponse(ctx, false, "Character is not available.", 0);
            return;
        }

        if (authCharacter.accountId() != verified.accountId()) {
            LOG.warn("Token handshake rejected — account mismatch for session {} (token sub={}, db account={})",
                session.getSessionId(), verified.accountId(), authCharacter.accountId());
            sendHandshakeResponse(ctx, false, "Invalid access token.", 0);
            return;
        }

        String characterName = authCharacter.characterName();
        Player player = PlayerRepository.loginOrRegisterTokenCharacter(characterName);
        if (player == null) {
            sendHandshakeResponse(ctx, false, "Unable to load character.", 0);
            return;
        }

        PlayerRepository.loadInventory(player);
        PlayerRepository.loadFriends(player);
        ensureStarterSkillingTools(player);

        if (!server.getWorld().canWalkTo(player.getX(), player.getY())) {
            LOG.warn("Player {} had out-of-bounds position ({},{}); resetting to spawn ({},{})",
                characterName, player.getX(), player.getY(), server.getWorld().getSpawnX(), server.getWorld().getSpawnY());
            player.setPosition(server.getWorld().getSpawnX(), server.getWorld().getSpawnY());
        }

        session.setPlayer(player);
        session.setAuthenticated(true);
        initializeQuestsForSession();
        server.getWorld().getPlayers().put(player.getId(), player);

        LOG.info("Token login successful for account {} character {} (entityId={})",
            verified.accountId(), characterName, player.getId());

        sendHandshakeResponse(ctx, true, "Welcome back, " + characterName + "!", player.getId());
        sendAllSkillUpdates(ctx, player);
        sendFullInventory(ctx, player);
        sendFullEquipment(ctx, player);
        sendFriendsListUpdate(ctx, player);
        sendInitialQuestState(ctx);
        sendWorldState(ctx);

        // Notify already-connected clients of the new player
        Player newPlayer = session.getPlayer();
        if (newPlayer != null) {
            NetworkProto.ServerMessage joinMsg = NetworkProto.ServerMessage.newBuilder()
                .setEntityUpdate(NetworkProto.EntityUpdate.newBuilder()
                    .setEntityId(newPlayer.getId())
                    .setX(newPlayer.getX())
                    .setY(newPlayer.getY())
                    .setIsPlayer(true)
                    .setName(newPlayer.getName()))
                .build();
            for (PlayerSession ps : server.getSessions().values()) {
                if (ps.getSessionId() == session.getSessionId()) continue;
                if (!ps.isAuthenticated() || ps.getPlayer() == null) continue;
                ps.getChannel().writeAndFlush(joinMsg);
            }
        }
    }

    private void sendFullEquipment(ChannelHandlerContext ctx, Player player) {
        for (int slot = 0; slot < EquipmentSlot.COUNT; slot++) {
            int eqId = player.getEquipment(slot);
            if (eqId <= 0) continue;
            ItemDefinition eqDef = server.getWorld().getItemDef(eqId);
            if (eqDef == null) continue;
            ctx.writeAndFlush(NetworkProto.ServerMessage.newBuilder()
                .setEquipmentUpdate(NetworkProto.EquipmentUpdate.newBuilder()
                    .setSlot(slot)
                    .setItemId(eqId)
                    .setItemName(eqDef.name)
                    .setFlags(eqDef.getFlags()))
                .build());
        }
        sendEquipmentBonuses(ctx, player);
    }

    private void sendEquipmentBonuses(ChannelHandlerContext ctx, Player player) {
        EquipmentBonusCalculator.BonusSet b =
            EquipmentBonusCalculator.calculate(player, server.getWorld().getItemDefs());
        ctx.writeAndFlush(NetworkProto.ServerMessage.newBuilder()
            .setEquipmentBonuses(NetworkProto.EquipmentBonuses.newBuilder()
                .setStabAttack(b.stabAttack)
                .setSlashAttack(b.slashAttack)
                .setCrushAttack(b.crushAttack)
                .setMagicAttack(b.magicAttack)
                .setRangedAttack(b.rangedAttack)
                .setStabDefence(b.stabDefence)
                .setSlashDefence(b.slashDefence)
                .setCrushDefence(b.crushDefence)
                .setMagicDefence(b.magicDefence)
                .setRangedDefence(b.rangedDefence)
                .setMeleeStrength(b.meleeStrength)
                .setRangedStrength(b.rangedStrength)
                .setMagicDamage(b.magicDamage)
                .setPrayer(b.prayer))
            .build());
    }

    /** Sends a SkillUpdate (leveled_up=false) for every skill — used on login to sync client. */
    private void sendAllSkillUpdates(ChannelHandlerContext ctx, Player player) {
        for (int i = 0; i < Player.SKILL_COUNT; i++) {
            ctx.writeAndFlush(NetworkProto.ServerMessage.newBuilder()
                .setSkillUpdate(NetworkProto.SkillUpdate.newBuilder()
                    .setSkillIndex(i)
                    .setNewLevel(player.getSkillLevel(i))
                    .setTotalXp(player.getSkillXp(i) / 10)  // server stores tenths; client receives whole XP
                    .setLeveledUp(false))
                .build());
        }
        // Initialise prayer points to prayer level on login
        int prayerLevel = player.getSkillLevel(Player.SKILL_PRAYER);
        player.setPrayerPoints(prayerLevel);
        ctx.writeAndFlush(NetworkProto.ServerMessage.newBuilder()
            .setPrayerPointsUpdate(NetworkProto.PrayerPointsUpdate.newBuilder()
                .setCurrent(prayerLevel)
                .setMaximum(prayerLevel))
            .build());
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

        if (player.isSkilling() && (toX != fromX || toY != fromY)) {
            player.clearSkillingAction();
        }

        player.setPosition(toX, toY);
        player.setFacing(movement.getFacing());

        // Broadcast authoritative player movement so all clients (including self)
        // keep entity position state in sync and do not snap back to stale tiles.
        server.broadcastToAll(NetworkProto.ServerMessage.newBuilder()
            .setEntityUpdate(NetworkProto.EntityUpdate.newBuilder()
                .setEntityId(player.getId())
                .setX(toX)
                .setY(toY)
                .setFacing(movement.getFacing()))
            .build());
        
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
            .setIsMember(success && session != null && session.getPlayer() != null && session.getPlayer().isMember())
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

        if (player.isSkilling()) {
            player.clearSkillingAction();
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

        if (tryStartSkilling(ctx, player, npc, NetworkProto.SkillingType.SKILLING_NONE, false)) {
            return;
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
        updateHandInQuestObjectives(ctx, npc.getId());
        sendDialoguePrompt(ctx, dialogue);
    }

    private void handleStartSkilling(ChannelHandlerContext ctx, NetworkProto.StartSkillingRequest request) {
        if (session.getPlayer() == null) {
            return;
        }
        Player player = session.getPlayer();
        NPC npc = server.getWorld().getNPC(request.getTargetNpcId());
        if (npc == null) {
            sendChatMessage(ctx, "I can't reach that!", 1);
            return;
        }

        if (!tryStartSkilling(ctx, player, npc, request.getSkillingType(), true)) {
            sendChatMessage(ctx, "You can't do that with this target.", 1);
            return;
        }

        if (request.getSkillingType() == NetworkProto.SkillingType.SKILLING_WOODCUTTING
            && player.getSkillingAction() == SkillingAction.WOODCUTTING
            && player.getSkillingTargetNpcId() == npc.getId()) {
            updateGenericQuestObjectives(session, Quest.TaskType.ACTION, npc.getDefinitionId());
        }

    }

    private boolean tryStartSkilling(ChannelHandlerContext ctx,
                                     Player player,
                                     NPC npc,
                                     NetworkProto.SkillingType requestedType,
                                     boolean strictType) {
        WoodcuttingRegistry.TreeTier treeTier = WoodcuttingRegistry.getTreeByDefinitionId(npc.getDefinitionId());
        if (treeTier != null) {
            if (strictType && requestedType != NetworkProto.SkillingType.SKILLING_WOODCUTTING) {
                return false;
            }
            if (npc.isDead()) {
                sendChatMessage(ctx, "This tree has been chopped down.", 1);
                return true;
            }
            if (player.isInCombat()) {
                sendChatMessage(ctx, "You are too busy fighting.", 1);
                return true;
            }
            // Check all registry axe tiers (not just bronze) — bronze-only check was wrong
            if (!hasUsableAxe(player)) {
                sendChatMessage(ctx, "You need an axe to chop this tree.", 1);
                return true;
            }
            // Level check here (before starting action) so only one message is ever sent
            int wcLevel = Math.max(1, player.getSkillLevel(Player.SKILL_WOODCUTTING));
            if (wcLevel < treeTier.levelRequirement()) {
                sendChatMessage(ctx, "You need a Woodcutting level of " + treeTier.levelRequirement()
                    + " to chop this tree.", 1);
                return true;
            }
            if (!canReachAnyAdjacentTile(player, npc)) {
                sendChatMessage(ctx, "I can't reach that!", 1);
                return true;
            }
            player.startSkillingAction(SkillingAction.WOODCUTTING, npc.getId(), server.getCurrentTick() + 1);
            sendSkillingStateUpdate(ctx, NetworkProto.SkillingType.SKILLING_WOODCUTTING,
                NetworkProto.SkillingState.SKILLING_STATE_QUEUED, npc.getId(), "queued");
            return true;
        }

        String npcName = npc.getName();

        if ("Fishing Spot".equalsIgnoreCase(npcName)) {
            if (strictType && requestedType != NetworkProto.SkillingType.SKILLING_FISHING) {
                return false;
            }
            if (npc.isDead()) {
                sendChatMessage(ctx, "There are no fish here right now.", 1);
                return true;
            }
            if (player.isInCombat()) {
                sendChatMessage(ctx, "You are too busy fighting.", 1);
                return true;
            }
            if (!hasSmallFishingNet(player)) {
                sendChatMessage(ctx, "You need a small fishing net to fish here.", 1);
                return true;
            }
            if (!canReachAnyAdjacentTile(player, npc)) {
                sendChatMessage(ctx, "I can't reach that!", 1);
                return true;
            }
            player.startSkillingAction(SkillingAction.FISHING, npc.getId(), server.getCurrentTick() + 1);
            sendSkillingStateUpdate(ctx, NetworkProto.SkillingType.SKILLING_FISHING,
                NetworkProto.SkillingState.SKILLING_STATE_QUEUED, npc.getId(), "queued");
            return true;
        }

        if ("Cooking Fire".equalsIgnoreCase(npcName)) {
            if (strictType && requestedType != NetworkProto.SkillingType.SKILLING_COOKING) {
                return false;
            }
            if (player.isInCombat()) {
                sendChatMessage(ctx, "You are too busy fighting.", 1);
                return true;
            }
            if (!hasRawShrimps(player)) {
                sendChatMessage(ctx, "You have no raw shrimps to cook.", 1);
                return true;
            }
            if (!canReachAnyAdjacentTile(player, npc)) {
                sendChatMessage(ctx, "I can't reach that!", 1);
                return true;
            }
            player.startSkillingAction(SkillingAction.COOKING, npc.getId(), server.getCurrentTick() + 1);
            sendSkillingStateUpdate(ctx, NetworkProto.SkillingType.SKILLING_COOKING,
                NetworkProto.SkillingState.SKILLING_STATE_QUEUED, npc.getId(), "queued");
            return true;
        }

        return false;
    }

    private void initializeQuestsForSession() {
        if (session.getQuestManager() != null || session.getPlayer() == null) {
            return;
        }

        QuestManager questManager = new QuestManager(session.getPlayer().getId());
        for (Quest quest : server.getQuestDefinitions().values()) {
            questManager.startQuest(quest);
        }

        if (DatabaseManager.isHealthy()) {
            PlayerRepository.loadQuestProgress(session.getPlayer(), questManager);
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

                boolean wasComplete = (quest.status == Quest.QuestStatus.COMPLETED);
                boolean completed = questManager.addTaskProgress(quest.id, task.id, 1);
                if (completed) {
                    Quest updatedQuest = questManager.getQuest(quest.id);
                    sendQuestUpdate(ctx, updatedQuest);

                    boolean isNowComplete = updatedQuest.allTasksCompleted()
                        && updatedQuest.status == Quest.QuestStatus.COMPLETED;
                    if (!wasComplete && isNowComplete) {
                        if (updatedQuest.totalRewardXp > 0) {
                            sendSkillUpdate(session.getPlayer(), updatedQuest.rewardSkillIndex, updatedQuest.totalRewardXp);
                        }
                        sendChatMessageToPlayer(ctx.channel(),
                            "You have completed: " + updatedQuest.name + "! You earned "
                                + updatedQuest.totalRewardXp + " XP.", 3);
                        PlayerRepository.saveQuestProgress(
                            session.getPlayer(), session.getQuestManager());
                    }
                }
            }
        }
    }

    private void updateHandInQuestObjectives(ChannelHandlerContext ctx, int npcId) {
        QuestManager questManager = session.getQuestManager();
        if (questManager == null) return;

        for (Quest quest : questManager.getQuests().values()) {
            for (Quest.Task task : quest.tasks) {
                if (task.type != Quest.TaskType.HAND_IN
                        || task.completed
                        || task.targetEntityId != npcId) {
                    continue;
                }
                boolean allOthersDone = quest.tasks.stream()
                    .filter(t -> t.type != Quest.TaskType.HAND_IN)
                    .allMatch(t -> t.completed);
                if (!allOthersDone) continue;

                boolean wasComplete = (quest.status == Quest.QuestStatus.COMPLETED);
                boolean completed = questManager.addTaskProgress(quest.id, task.id, 1);
                if (completed) {
                    Quest updatedQuest = questManager.getQuest(quest.id);
                    sendQuestUpdate(ctx, updatedQuest);
                    boolean isNowComplete = updatedQuest.allTasksCompleted()
                        && updatedQuest.status == Quest.QuestStatus.COMPLETED;
                    if (!wasComplete && isNowComplete) {
                        if (updatedQuest.totalRewardXp > 0) {
                            sendSkillUpdate(session.getPlayer(),
                                updatedQuest.rewardSkillIndex, updatedQuest.totalRewardXp);
                        }
                        sendChatMessageToPlayer(ctx.channel(),
                            "You have completed: " + updatedQuest.name
                                + "! You earned " + updatedQuest.totalRewardXp + " XP.", 3);
                        PlayerRepository.saveQuestProgress(
                            session.getPlayer(), session.getQuestManager());
                    }
                }
            }
        }
    }

    private void updateGenericQuestObjectives(PlayerSession session, Quest.TaskType type, int targetId) {
        if (session == null || session.getQuestManager() == null) {
            return;
        }

        QuestManager questManager = session.getQuestManager();
        for (Quest quest : questManager.getQuests().values()) {
            for (Quest.Task task : quest.tasks) {
                if (task.type != type || task.completed || task.targetEntityId != targetId) {
                    continue;
                }

                boolean wasComplete = (quest.status == Quest.QuestStatus.COMPLETED);
                questManager.addTaskProgress(quest.id, task.id, 1);
                Quest updatedQuest = questManager.getQuest(quest.id);
                sendQuestUpdate(session, updatedQuest);

                boolean isNowComplete = updatedQuest.allTasksCompleted()
                    && updatedQuest.status == Quest.QuestStatus.COMPLETED;
                if (!wasComplete && isNowComplete) {
                    if (updatedQuest.totalRewardXp > 0) {
                        sendSkillUpdate(session.getPlayer(), updatedQuest.rewardSkillIndex, updatedQuest.totalRewardXp);
                    }
                    sendChatMessageToPlayer(session.getChannel(),
                        "You have completed: " + updatedQuest.name + "! You earned "
                            + updatedQuest.totalRewardXp + " XP.", 3);
                    PlayerRepository.saveQuestProgress(
                        session.getPlayer(), session.getQuestManager());
                }
            }
        }

        if (DatabaseManager.isHealthy() && session.getPlayer() != null) {
            PlayerRepository.saveQuestProgress(session.getPlayer(), questManager);
        }
    }

    private void sendQuestUpdate(ChannelHandlerContext ctx, Quest quest) {
        sendQuestUpdate(session, quest);
    }

    private void sendQuestUpdate(PlayerSession session, Quest quest) {
        QuestManager questManager = session == null ? null : session.getQuestManager();
        if (quest == null || questManager == null) {
            return;
        }

        int completed = quest.getCompletedTaskCount();
        int total = quest.tasks.size();
        boolean done = quest.allTasksCompleted();

        NetworkProto.QuestUpdate.Builder update = NetworkProto.QuestUpdate.newBuilder()
            .setQuestId(quest.id)
            .setQuestName(quest.name)
            .setTasksCompleted(completed)
            .setTasksTotal(total)
            .setCompleted(done)
            .setQuestDescription(quest.description == null ? "" : quest.description)
            .setMiniquest(quest.miniquest)
            .setQuestPointsReward(quest.questPointsReward)
            .setPlayerTotalQuestPoints(questManager.getTotalQuestPoints())
            .setStatus(switch (quest.status) {
                case COMPLETED -> NetworkProto.QuestStatus.QUEST_COMPLETED;
                case IN_PROGRESS -> NetworkProto.QuestStatus.QUEST_IN_PROGRESS;
                case NOT_STARTED -> NetworkProto.QuestStatus.QUEST_NOT_STARTED;
            });

        for (Quest.Task task : quest.tasks) {
            int current = questManager.getTaskProgress(quest.id, task.id);
            update.addTasks(NetworkProto.QuestTaskUpdate.newBuilder()
                .setTaskId(task.id)
                .setDescription(task.description == null ? task.id : task.description)
                .setCurrentCount(current)
                .setRequiredCount(Math.max(1, task.quantity))
                .setCompleted(task.completed));
        }

        session.getChannel().writeAndFlush(NetworkProto.ServerMessage.newBuilder()
            .setQuestUpdate(update)
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

    private boolean canReachAnyAdjacentTile(Player player, NPC npc) {
        World world = server.getWorld();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int tx = npc.getX() + dx;
                int ty = npc.getY() + dy;
                if (!world.canWalkTo(tx, ty)) continue;
                if (world.canReach(player.getX(), player.getY(), tx, ty)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasUsableAxe(Player player) {
        // Check equipped weapon slot and all inventory slots against every registry axe ID.
        // WC level requirement is checked separately — any axe in possession qualifies here.
        for (WoodcuttingRegistry.AxeTier axe : WoodcuttingRegistry.axes()) {
            int id = axe.itemId();
            if (player.getEquipment(EquipmentSlot.WEAPON) == id) return true;
            for (int s = 0; s < 28; s++) {
                if (player.getInventoryItemId(s) == id) return true;
            }
        }
        return false;
    }

    private boolean hasSmallFishingNet(Player player) {
        for (int i = 0; i < 28; i++) {
            if (player.getInventoryItemId(i) == SMALL_FISHING_NET_ITEM_ID) {
                return true;
            }
        }
        return false;
    }

    private boolean hasRawShrimps(Player player) {
        for (int i = 0; i < 28; i++) {
            if (player.getInventoryItemId(i) == RAW_SHRIMPS_ITEM_ID) {
                return true;
            }
        }
        return false;
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

        // Include self so client initializes to authoritative spawn tile immediately.
        if (session.getPlayer() != null) {
            com.osrs.shared.Player self = session.getPlayer();
            wsBuilder.addEntities(NetworkProto.Entity.newBuilder()
                .setId(self.getId())
                .setName(self.getName())
                .setX(self.getX())
                .setY(self.getY())
                .setCombatLevel(0)
                .setIsPlayer(true));
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
        ItemDefinition def = server.getWorld().getItemDef(gi.getItemId());

        // OSRS-like pickup stance: player should stand on the item tile.
        if (player.getX() != gi.getX() || player.getY() != gi.getY()) {
            LOG.debug("Player {} not on ground item {} tile (player={},{} item={},{}); client should walk first",
                player.getId(), gi.getGroundItemId(), player.getX(), player.getY(), gi.getX(), gi.getY());
            sendChatMessage(ctx, "I can't reach that!", 1);
            return;
        }

        // Inventory check up-front (early-out with OSRS message)
        if (player.isInventoryFull()) {
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
        updateGenericQuestObjectives(session, Quest.TaskType.COLLECT, def.id);
    }

    /** Send an OSRS-style game / error message only to this player's session. */
    private void sendChatMessage(ChannelHandlerContext ctx, String text, int type) {
        sendChatMessageToPlayer(ctx.channel(), text, type);
    }

    private void sendChatMessageToPlayer(io.netty.channel.Channel channel, String text, int type) {
        channel.writeAndFlush(NetworkProto.ServerMessage.newBuilder()
            .setChatMessage(NetworkProto.ChatMessage.newBuilder()
                .setText(text).setType(type))
            .build());
    }

    private void sendSkillUpdate(Player player, int skillIdx, long xpAmount) {
        if (player == null || session == null || session.getChannel() == null) {
            return;
        }

        boolean leveledUp = player.addSkillXp(skillIdx, xpAmount);
        int newLevel = player.getSkillLevel(skillIdx);
        long totalXp = player.getSkillXp(skillIdx);

        session.getChannel().writeAndFlush(NetworkProto.ServerMessage.newBuilder()
            .setSkillUpdate(NetworkProto.SkillUpdate.newBuilder()
                .setSkillIndex(skillIdx)
                .setNewLevel(newLevel)
                .setTotalXp(totalXp / 10)  // server stores tenths; client receives whole XP
                .setLeveledUp(leveledUp))
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

        } else if ("bury".equals(action) && itemId == BONES_ITEM_ID) {
            player.setInventoryItem(slot, 0, 0);
            sendInventorySlot(ctx, player, slot);

            boolean leveledUp = player.addSkillXp(Player.SKILL_PRAYER, BONES_PRAYER_XP);
            int newLevel = player.getSkillLevel(Player.SKILL_PRAYER);
            long totalXp = player.getSkillXp(Player.SKILL_PRAYER);
            ctx.writeAndFlush(NetworkProto.ServerMessage.newBuilder()
                .setSkillUpdate(NetworkProto.SkillUpdate.newBuilder()
                    .setSkillIndex(Player.SKILL_PRAYER)
                    .setNewLevel(newLevel)
                    .setTotalXp(totalXp / 10)  // server stores tenths; client receives whole XP
                    .setLeveledUp(leveledUp))
                .build());

            sendChatMessage(ctx, "You bury the bones.", 0);
            // Update prayer points max if level increased
            int pMax = player.getSkillLevel(Player.SKILL_PRAYER);
            int pCur = Math.min(player.getPrayerPoints(), pMax);
            player.setPrayerPoints(pCur);
            ctx.writeAndFlush(NetworkProto.ServerMessage.newBuilder()
                .setPrayerPointsUpdate(NetworkProto.PrayerPointsUpdate.newBuilder()
                    .setCurrent(pCur).setMaximum(pMax))
                .build());
            if (leveledUp) {
                sendChatMessage(ctx, "Congratulations, you just advanced a Prayer level.", 2);
            }
            LOG.info("Player {} buried bones (+{} prayer xp)", player.getId(), BONES_PRAYER_XP);
            updateGenericQuestObjectives(session, Quest.TaskType.ACTION, BONES_ITEM_ID);

        } else if (("equip".equals(action) || "wield".equals(action) || "wear".equals(action)) && def.equipable) {
            if (player.getSkillLevel(Player.SKILL_DEFENCE) < def.defenceReq) {
                sendChatMessage(ctx, "You need a Defence level of " + def.defenceReq
                    + " to wear this.", 0);
                return;
            }
            if (def.equipSlot == EquipmentSlot.WEAPON
                    && player.getSkillLevel(Player.SKILL_ATTACK) < def.attackReq) {
                sendChatMessage(ctx, "You need an Attack level of " + def.attackReq
                    + " to wield this.", 0);
                return;
            }
            if (def.strengthReq > 0 && player.getSkillLevel(Player.SKILL_STRENGTH) < def.strengthReq) {
                sendChatMessage(ctx, "You need a Strength level of " + def.strengthReq
                    + " to wield this.", 0);
                return;
            }
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
            updateGenericQuestObjectives(session, Quest.TaskType.EQUIP, def.id);
            sendEquipmentBonuses(ctx, player);
        }
    }

    private void handleUnequipItem(ChannelHandlerContext ctx, NetworkProto.UnequipItem req) {
        if (session.getPlayer() == null) return;
        Player player = session.getPlayer();

        int equipSlot = req.getEquipmentSlot();
        if (equipSlot < 0 || equipSlot >= EquipmentSlot.COUNT) return;

        int itemId = player.getEquipment(equipSlot);
        if (itemId == 0) return;

        // Find an empty inventory slot
        int invSlot = -1;
        for (int i = 0; i < 28; i++) {
            if (player.getInventoryItemId(i) == 0) { invSlot = i; break; }
        }
        if (invSlot == -1) {
            sendChatMessage(ctx, "Your inventory is too full to unequip that.", 0);
            return;
        }

        // Move item: equipment slot -> inventory
        player.setEquipment(equipSlot, 0);
        player.setInventoryItem(invSlot, itemId, 1);

        if (equipSlot == EquipmentSlot.WEAPON) {
            player.setWeaponAttackRange(1);
        }

        sendInventorySlot(ctx, player, invSlot);
        ctx.writeAndFlush(NetworkProto.ServerMessage.newBuilder()
            .setEquipmentUpdate(NetworkProto.EquipmentUpdate.newBuilder()
                .setSlot(equipSlot).setItemId(0).setItemName("").setFlags(0))
            .build());
        sendEquipmentBonuses(ctx, player);
        LOG.info("Player {} unequipped slot {} (itemId={})", player.getId(), equipSlot, itemId);
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

    private void handleUseItemOnItem(ChannelHandlerContext ctx, NetworkProto.UseItemOnItem req) {
        if (session.getPlayer() == null) return;
        Player player = session.getPlayer();

        int srcSlot = req.getSourceSlot();
        int tgtSlot = req.getTargetSlot();
        if (srcSlot < 0 || srcSlot >= 28 || tgtSlot < 0 || tgtSlot >= 28 || srcSlot == tgtSlot) return;

        int srcItem = player.getInventoryItemId(srcSlot);
        int tgtItem = player.getInventoryItemId(tgtSlot);

        boolean isFiremaking = (srcItem == TINDERBOX_ITEM_ID && isFiremakingLogItem(tgtItem))
                             || (isFiremakingLogItem(srcItem) && tgtItem == TINDERBOX_ITEM_ID);
        if (isFiremaking) {
            int usedLogItemId = isFiremakingLogItem(srcItem) ? srcItem : tgtItem;
            int logsSlot = isFiremakingLogItem(srcItem) ? srcSlot : tgtSlot;
            player.setInventoryItem(logsSlot, 0, 0);
            sendInventorySlot(ctx, player, logsSlot);

            boolean leveledUp = player.addSkillXp(Player.SKILL_FIREMAKING, FIREMAKING_LOG_XP);
            int  newLevel = player.getSkillLevel(Player.SKILL_FIREMAKING);
            long totalXp  = player.getSkillXp(Player.SKILL_FIREMAKING);
            ctx.writeAndFlush(NetworkProto.ServerMessage.newBuilder()
                .setSkillUpdate(NetworkProto.SkillUpdate.newBuilder()
                    .setSkillIndex(Player.SKILL_FIREMAKING)
                    .setNewLevel(newLevel).setTotalXp(totalXp / 10).setLeveledUp(leveledUp))  // tenths→whole
                .build());
            sendChatMessage(ctx, "You light a fire.", 0);
            if (leveledUp) sendChatMessage(ctx, "Congratulations, you just advanced a Firemaking level.", 2);
            updateGenericQuestObjectives(session, Quest.TaskType.ACTION, usedLogItemId);
            LOG.info("Player {} lit a fire (+{} firemaking xp)", player.getId(), FIREMAKING_LOG_XP);
            return;
        }

        sendChatMessage(ctx, "Nothing interesting happens.", 0);
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

    private boolean isFiremakingLogItem(int itemId) {
        for (int id : FIREMAKING_LOG_ITEM_IDS) {
            if (id == itemId) return true;
        }
        return false;
    }

    private void handleSetCombatStyle(NetworkProto.SetCombatStyle req) {
        if (session.getPlayer() == null) return;
        CombatStyle style = CombatStyle.fromIndex(req.getStyle());
        session.getPlayer().setCombatStyle(style);
        LOG.info("Player {} set combat style to {}", session.getPlayer().getId(), style.displayName);
    }

    private void handleSetAutoRetaliate(NetworkProto.SetAutoRetaliate req) {
        if (session.getPlayer() == null) return;
        session.getPlayer().setAutoRetaliate(req.getEnabled());
        LOG.debug("Player {} autoRetaliate={}", session.getPlayer().getId(), req.getEnabled());
    }

    private void handleExamineNpc(ChannelHandlerContext ctx, NetworkProto.ExamineNpc req) {
        if (session.getPlayer() == null) return;

        int npcId = req.getNpcId();
        NPC npc = server.getWorld().getNPC(npcId);
        if (npc == null) {
            sendChatMessage(ctx, "You don't see anything interesting.", 0);
            return;
        }

        sendChatMessage(ctx, server.getWorld().getNpcExamineText(npcId), 0);
    }

    private void handleExamineItem(ChannelHandlerContext ctx, NetworkProto.ExamineItem req) {
        if (session.getPlayer() == null) return;

        int slot = req.getInventorySlot();
        if (slot < 0 || slot > 27) return;

        Player player = session.getPlayer();
        int itemId = player.getInventoryItemId(slot);

        if (itemId == 0) {
            sendChatMessage(ctx, "You don't see anything interesting.", 0);
            return;
        }

        ItemDefinition def = server.getWorld().getItemDef(itemId);
        String examineText = def.examine;

        if (examineText == null || examineText.isEmpty()) {
            // Fallback if items.yaml has no examine entry for this item
            examineText = "It's a " + def.name + ".";
        }

        sendChatMessage(ctx, examineText, 0);
    }

    private void handleFriendAction(ChannelHandlerContext ctx, NetworkProto.FriendAction req) {
        if (session.getPlayer() == null) return;
        Player player = session.getPlayer();
        int sessionPlayerDbId = resolveSessionPlayerDbId(player);

        boolean success = true;
        String error = "";
        long targetDbId = -1L;

        switch (req.getAction()) {
            case ADD -> {
                targetDbId = resolveTargetPlayerDbId(req);
                if (targetDbId <= 0) {
                    success = false;
                    error = "Invalid player.";
                } else if (sessionPlayerDbId > 0 && targetDbId == sessionPlayerDbId) {
                    success = false;
                    error = "You can't add yourself.";
                } else if (player.hasFriend(targetDbId)) {
                    success = false;
                    error = "That player is already on your friends list.";
                } else if (player.getFriends().size() >= MAX_FRIENDS) {
                    success = false;
                    error = "Friend list at capacity.";
                } else {
                    player.addFriend(targetDbId);
                }
            }
            case REMOVE -> {
                targetDbId = resolveTargetPlayerDbId(req);
                if (targetDbId <= 0) {
                    success = false;
                    error = "Invalid player.";
                } else {
                    player.removeFriend(targetDbId);
                    player.removeFromBlock(targetDbId);
                }
            }
            case UNRECOGNIZED -> {
                success = false;
                error = "Unknown friend action.";
            }
        }

        if (DatabaseManager.isHealthy()) {
            PlayerRepository.saveFriends(player);
        }

        ctx.writeAndFlush(NetworkProto.ServerMessage.newBuilder()
            .setFriendActionResult(NetworkProto.FriendActionResult.newBuilder()
                .setSuccess(success)
                .setError(error)
                .setSequence(req.getSequence()))
            .build());

        if (success) {
            sendFriendsListUpdate(ctx, player);
        }
    }

    private void sendFriendsListUpdate(ChannelHandlerContext ctx, Player player) {
        NetworkProto.FriendsListUpdate.Builder list = NetworkProto.FriendsListUpdate.newBuilder();
        java.util.Set<Integer> onlineDbIds = collectOnlineDbIds();
        for (Long friendIdLong : player.getFriends()) {
            int friendDbId = (int) (long) friendIdLong;
            String name = PlayerRepository.findUsernameByDbId(friendDbId);
            if (name == null || name.isBlank()) {
                name = "Player " + friendDbId;
            }
            list.addFriends(NetworkProto.PlayerEntry.newBuilder()
                .setName(name)
                .setPlayerId(friendDbId)
                .setOnline(onlineDbIds.contains(friendDbId)));
        }

        ctx.writeAndFlush(NetworkProto.ServerMessage.newBuilder()
            .setFriendsListUpdate(list)
            .build());
    }

    private int resolveSessionPlayerDbId(Player player) {
        if (player == null || !DatabaseManager.isHealthy()) {
            return -1;
        }
        int dbIdFromEntity = PlayerRepository.tryMapEntityIdToDbId(player.getId());
        if (dbIdFromEntity > 0 && PlayerRepository.playerExistsByDbId(dbIdFromEntity)) {
            return dbIdFromEntity;
        }
        return PlayerRepository.findDbIdByUsername(player.getName());
    }

    private long resolveTargetPlayerDbId(NetworkProto.FriendAction req) {
        if (!DatabaseManager.isHealthy()) {
            return req.getPlayerId() > 0 ? req.getPlayerId() : -1L;
        }

        String name = req.getName();
        if (name != null && !name.isBlank()) {
            int byName = PlayerRepository.findDbIdByUsername(name.trim());
            if (byName > 0) {
                return byName;
            }
        }

        long suppliedId = req.getPlayerId();
        if (suppliedId <= 0 || suppliedId > Integer.MAX_VALUE) {
            return -1L;
        }

        Player onlineByEntity = server.getWorld().getPlayers().get((int) suppliedId);
        if (onlineByEntity != null) {
            int byEntityName = PlayerRepository.findDbIdByUsername(onlineByEntity.getName());
            if (byEntityName > 0) {
                return byEntityName;
            }
        }

        int dbCandidate = (int) suppliedId;
        if (PlayerRepository.playerExistsByDbId(dbCandidate)) {
            return dbCandidate;
        }

        int mappedFromEntity = PlayerRepository.tryMapEntityIdToDbId((int) suppliedId);
        if (mappedFromEntity > 0 && PlayerRepository.playerExistsByDbId(mappedFromEntity)) {
            return mappedFromEntity;
        }
        return -1L;
    }

    private java.util.Set<Integer> collectOnlineDbIds() {
        java.util.Set<Integer> out = new java.util.HashSet<>();
        if (!DatabaseManager.isHealthy()) {
            return out;
        }
        for (Player onlinePlayer : server.getWorld().getPlayers().values()) {
            int dbId = PlayerRepository.tryMapEntityIdToDbId(onlinePlayer.getId());
            if (dbId > 0) {
                out.add(dbId);
            }
        }
        return out;
    }

    private void handleLogoutRequest(ChannelHandlerContext ctx, NetworkProto.LogoutRequest req) {
        if (session == null || session.getPlayer() == null) {
            ctx.writeAndFlush(NetworkProto.ServerMessage.newBuilder()
                .setLogoutResponse(NetworkProto.LogoutResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Goodbye."))
                .build())
                .addListener(ChannelFutureListener.CLOSE);
            return;
        }

        Player player = session.getPlayer();
        LOG.info("Logout requested by player {}", player.getName());

        closeDialogue(player);
        if (session.isAuthenticated() && DatabaseManager.isHealthy()) {
            PlayerRepository.savePlayer(player);
            PlayerRepository.saveInventory(player);
            PlayerRepository.saveFriends(player);
            PlayerRepository.saveQuestProgress(player, session.getQuestManager());
            LOG.info("Saved player {} on explicit logout", player.getName());
        }

        server.getWorld().getPlayers().remove(player.getId());
        session.setAuthenticated(false);
        session.setQuestManager(null);
        session.setPlayer(null);

        ctx.writeAndFlush(NetworkProto.ServerMessage.newBuilder()
            .setLogoutResponse(NetworkProto.LogoutResponse.newBuilder()
                .setSuccess(true)
                .setMessage("You have been logged out."))
            .build())
            .addListener(ChannelFutureListener.CLOSE);
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

    private void sendFullInventory(ChannelHandlerContext ctx, Player player) {
        for (int slot = 0; slot < 28; slot++) {
            sendInventorySlot(ctx, player, slot);
        }
    }

    private void handleTogglePrayer(ChannelHandlerContext ctx, NetworkProto.TogglePrayer req) {
        if (session.getPlayer() == null) return;
        Player player = session.getPlayer();
        int prayerId = req.getPrayerId();

        // Validate prayer ID
        int levelReq = -1;
        for (int[] def : PRAYER_DEFS) {
            if (def[0] == prayerId) { levelReq = def[1]; break; }
        }
        if (levelReq < 0) return;

        if (player.isPrayerActive(prayerId)) {
            player.deactivatePrayer(prayerId);
        } else {
            if (player.getSkillLevel(Player.SKILL_PRAYER) < levelReq) {
                sendChatMessage(ctx, "You need level " + levelReq + " Prayer to use this.", 1);
                return;
            }
            if (player.getPrayerPoints() <= 0) {
                sendChatMessage(ctx, "You have run out of Prayer points.", 1);
                return;
            }
            player.activatePrayer(prayerId);
        }
    }

    private void sendSkillingStateUpdate(ChannelHandlerContext ctx,
                                         NetworkProto.SkillingType type,
                                         NetworkProto.SkillingState state,
                                         int targetNpcId,
                                         String message) {
        ctx.writeAndFlush(NetworkProto.ServerMessage.newBuilder()
            .setSkillingStateUpdate(NetworkProto.SkillingStateUpdate.newBuilder()
                .setSkillingType(type)
                .setState(state)
                .setTargetNpcId(targetNpcId)
                .setMessage(message == null ? "" : message))
            .build());
    }

    private void ensureStarterSkillingTools(Player player) {
        ensureStarterItem(player, BRONZE_AXE_ITEM_ID);
        ensureStarterItem(player, SMALL_FISHING_NET_ITEM_ID);
        ensureStarterItem(player, TINDERBOX_ITEM_ID);
        ensureStarterItem(player, 1115);  // Bronze full helm for equip_armor quest
    }

    private void ensureStarterItem(Player player, int itemId) {
        if (player.getEquipment(EquipmentSlot.WEAPON) == itemId) {
            return;
        }
        for (int i = 0; i < 28; i++) {
            if (player.getInventoryItemId(i) == itemId) {
                return;
            }
        }
        int empty = player.getFirstEmptySlot();
        if (empty >= 0) {
            player.setInventoryItem(empty, itemId, 1);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOG.error("Exception in packet handler", cause);
        ctx.close();
    }
}
