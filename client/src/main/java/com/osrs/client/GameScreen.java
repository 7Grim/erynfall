package com.osrs.client;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.osrs.protocol.NetworkProto;
import com.osrs.client.auth.AuthApiClient;
import com.osrs.client.network.ClientPacketHandler;
import com.osrs.client.network.NettyClient;
import com.osrs.client.renderer.CoordinateConverter;
import com.osrs.client.renderer.IsometricRenderer;
import com.osrs.client.world.TutorialIslandMap;
import com.osrs.client.ui.ChatBox;
import com.osrs.client.ui.CombatUI;
import com.osrs.client.ui.ContextMenu;
import com.osrs.client.ui.DialogueUI;
import com.osrs.client.ui.FontManager;
import com.osrs.client.ui.LevelUpOverlay;
import com.osrs.client.ui.MiniMap;
import com.osrs.client.ui.SidePanel;
import com.osrs.client.ui.XpDropOverlay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main game screen.
 *
 * Entity positions are driven entirely by the server (WorldState on login,
 * EntityUpdate on every move).  The hardcoded NPC arrays are gone — all
 * entities live in ClientPacketHandler's concurrent maps and are read here
 * on the render thread.
 *
 * NPC sprites interpolate smoothly toward their server-authoritative tile,
 * exactly mirroring how the player's visualX/Y interpolates.
 */
public class GameScreen extends ApplicationAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(GameScreen.class);

    // OSRS-style color palette for consistent text rendering
    private static final Color COLOR_WHITE = FontManager.TEXT_WHITE;
    private static final Color COLOR_CYAN = FontManager.TEXT_CYAN;
    private static final Color COLOR_YELLOW = FontManager.TEXT_YELLOW;
    private static final Color COLOR_GOLD = FontManager.TEXT_GOLD;

    /** OSRS walk speed: 1 tile per 0.6 s. */
    private static final float TILES_PER_SECOND = 1.0f / 0.6f;

    /** Chebyshev distance to activate an NPC interaction. */
    private static final int INTERACT_RANGE = 1;

    // Credentials passed from LoginScreen (null = dev bypass, will use defaults)
    private final ErynfallGame game;
    private final String loginEmail;
    private final String loginPassword;
    private boolean logoutRequested = false;
    private boolean logoutMenuVisible = false;

    public GameScreen() {
        this.game = null;
        this.loginEmail = "test1@example.com";
        this.loginPassword = "testpass";
    }

    public GameScreen(String username, String password) {
        this.game = null;
        this.loginEmail = username;
        this.loginPassword = password;
    }

    public GameScreen(ErynfallGame game, String username, String password) {
        this.game = game;
        this.loginEmail = username;
        this.loginPassword = password;
    }

    // -----------------------------------------------------------------------
    // Graphics
    // -----------------------------------------------------------------------
    private SpriteBatch      batch;
    private SpriteBatch      screenBatch;
    private ShapeRenderer    shapeRenderer;
    private OrthographicCamera camera;
    private IsometricRenderer renderer;
    private BitmapFont       font;
    private Matrix4          screenProjection;

    // -----------------------------------------------------------------------
    // Game objects
    // -----------------------------------------------------------------------
    private NettyClient  nettyClient;
    private ContextMenu  contextMenu;
    private CombatUI     combatUI;
    private SidePanel    sidePanel;
    private DialogueUI   dialogueUI;
    private ChatBox      chatBox;
    private MiniMap      miniMap;
    private XpDropOverlay xpDropOverlay;
    private LevelUpOverlay levelUpOverlay;
    private int[][]      tileMap;

    // -----------------------------------------------------------------------
    // Player state
    // -----------------------------------------------------------------------
    /** Current logical tile the player is standing on (advances tile-by-tile during movement). */
    private int   playerX = 50, playerY = 50;
    /** Walk destination tile; -1 when not walking. Visual interpolates toward this. */
    private int   walkDestX = -1, walkDestY = -1;
    /** Planned tile-by-tile walking path (adjacent steps). */
    private final Deque<int[]> walkPath = new ArrayDeque<>();
    /** Last path step sent to server via PlayerMovement. */
    private int lastStepSentX = Integer.MIN_VALUE;
    private int lastStepSentY = Integer.MIN_VALUE;
    /** Smoothly interpolated render position. */
    private float visualX = 50f, visualY = 50f;

    // HUD (updated from server each frame)
    private int playerHealth = 10, playerMaxHealth = 10;
    private int playerPrayer = 0,  playerMaxPrayer = 0;
    private int playerRunEnergy = 100; // 0–100; full energy on login
    private boolean isRunning    = false;
    private float   runRestoreAcc = 0f; // accumulates delta time for energy restore
    private int attackLevel = 1, strengthLevel = 1, defenceLevel = 1;

    // -----------------------------------------------------------------------
    // NPC smooth movement
    // NPC logical positions come from ClientPacketHandler (server-driven).
    // Visual positions are interpolated here on the render thread.
    // -----------------------------------------------------------------------
    /** npcId → float[]{visualX, visualY} */
    private final Map<Integer, float[]> npcVisual = new HashMap<>();

    // -----------------------------------------------------------------------
    // Ground items (synced from ClientPacketHandler each frame)
    // -----------------------------------------------------------------------
    /** groundItemId → int[]{itemId, qty, x, y} */
    private final Map<Integer, int[]>  groundItemsOnMap  = new ConcurrentHashMap<>();
    private final Map<Integer, String> groundItemNamesMap = new ConcurrentHashMap<>();

    // -----------------------------------------------------------------------
    // Approach-and-act state
    // -----------------------------------------------------------------------
    private int    pendingNpcId      = -1;
    private String pendingAction     = null; // "attack" | "talk" | "chop"
    /** Seconds until we can resend talk/attack while in range. */
    private float  pendingActionRetryTimer = 0f;
    /** The walk-target tile we last submitted so we don't spam WalkTo. */
    private int    pendingWalkTargX  = -1, pendingWalkTargY = -1;

    // -----------------------------------------------------------------------
    // Combat target tracking (for opponent info HUD panel)
    // -----------------------------------------------------------------------
    /** Entity ID of the NPC the player is currently fighting, or -1 if none. */
    private int combatTargetId = -1;

    // -----------------------------------------------------------------------
    // Ground item pickup approach
    // -----------------------------------------------------------------------
    /** Ground item ID the player is walking toward to pick up; -1 if none. */
    private int    pendingGroundItemId = -1;
    /** World tile the item is on (cached so we can still pathfind if map updates). */
    private int    pendingGroundItemX  = -1, pendingGroundItemY = -1;
    private int selectedInventorySlot = -1;  // use-mode: slot waiting for "use on" target
    private int inventoryMouseDownSlot = -1; // tracks which slot was pressed down

    // -----------------------------------------------------------------------
    // Pickup animation
    // -----------------------------------------------------------------------
    /** Seconds remaining in the pickup (kneel-down) animation; 0 = not playing. */
    private float pickupAnimationTimer = 0f;
    /** OSRS pickup animation: 3 OSRS ticks = 1.8 s real time. */
    private static final float PICKUP_ANIM_DURATION = 1.8f;

    // -----------------------------------------------------------------------
    // Death screen state
    // -----------------------------------------------------------------------
    /** > 0 while the "Oh dear, you are dead!" overlay is showing (counts down in seconds). */
    private float deathScreenTimer = 0f;
    private static final float DEATH_SCREEN_DURATION = 4f;

    // -----------------------------------------------------------------------
    // Overhead chat text
    // -----------------------------------------------------------------------
    /** Overhead text per entity ID — yellow text floating above their head for 3 s (OSRS spec). */
    private static class OverheadText {
        final String text;
        float timer;  // counts down from 3.0 to 0
        OverheadText(String t) { this.text = t; this.timer = 3.0f; }
    }

    private static class ClickMarker {
        final int tileX;
        final int tileY;
        final boolean isAction;
        private float age = 0f;
        private static final float LIFETIME = 0.45f;

        ClickMarker(int tileX, int tileY, boolean isAction) {
            this.tileX = tileX;
            this.tileY = tileY;
            this.isAction = isAction;
        }

        void update(float delta) { age += delta; }
        boolean expired() { return age >= LIFETIME; }
        float alpha() {
            float a = 1f - (age / LIFETIME);
            return Math.max(0f, Math.min(1f, a));
        }
        float rotationRad() { return age * 7f; }
    }
    /** entityId → active overhead text (null / absent = no text shown). */
    private final Map<Integer, OverheadText> overheadTexts = new HashMap<>();
    private final List<ClickMarker> clickMarkers = new ArrayList<>();
    /** Entity ID used for the local player's overhead text. */
    private int localPlayerId = -1;
    /** questId -> last displayed task completion count. */
    private final Map<Integer, Integer> shownQuestProgress = new HashMap<>();
    /** questId -> whether completion message already shown. */
    private final Map<Integer, Boolean> shownQuestComplete = new HashMap<>();

    private enum AuthState { CONNECTING, FAILED, READY }

    private volatile boolean initialized = false;
    private volatile AuthState authState = AuthState.CONNECTING;
    private volatile String authError = "";
    private boolean suppressInitialEnter = true;

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @Override
    public void create() {
        LOG.info("GameScreen created – display {}x{}",
            Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        authState = AuthState.CONNECTING;
        authError = "";
        initialized = false;

        batch        = new SpriteBatch();
        screenBatch  = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        camera       = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0, 0, 0);
        camera.update();

        FontManager.initialize();
        font = FontManager.regular();
        font.setColor(COLOR_WHITE);
        font.getData().markupEnabled = true; // enables [#rrggbb] color tags in strings

        screenProjection = new Matrix4().setToOrtho2D(
            0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        renderer   = new IsometricRenderer(camera, batch, shapeRenderer);
        tileMap    = TutorialIslandMap.generate();
        contextMenu = new ContextMenu();
        combatUI   = new CombatUI();
        sidePanel  = new SidePanel();
        dialogueUI = new DialogueUI();
        miniMap = new MiniMap();
        chatBox        = new ChatBox();
        xpDropOverlay  = new XpDropOverlay();
        levelUpOverlay = new LevelUpOverlay();

        Thread t = new Thread(() -> {
            try {
                AuthApiClient authApiClient = new AuthApiClient();
                AuthApiClient.LoginResult loginResult = authApiClient.login(loginEmail, loginPassword);
                if (!loginResult.success()) {
                    authError = loginResult.errorMessage().isBlank()
                        ? "Unable to authenticate account."
                        : loginResult.errorMessage();
                    authState = AuthState.FAILED;
                    return;
                }
                if (loginResult.accessToken().isBlank()) {
                    authError = "Auth login did not return an access token.";
                    authState = AuthState.FAILED;
                    return;
                }

                nettyClient = new NettyClient();
                nettyClient.connect();
                nettyClient.sendTokenHandshake(loginResult.accessToken());

                NetworkProto.HandshakeResponse handshake = nettyClient.awaitHandshakeResponse(5000);
                if (handshake == null) {
                    authError = "Login timed out. Please try again.";
                    authState = AuthState.FAILED;
                    return;
                }
                if (!handshake.getSuccess()) {
                    authError = handshake.getMessage().isBlank()
                        ? "Login failed."
                        : handshake.getMessage();
                    authState = AuthState.FAILED;
                    return;
                }

                String connectedName = loginResult.characterName().isBlank() ? "(unknown)" : loginResult.characterName();
                LOG.info("Connected to server as character {}", connectedName);
                suppressInitialEnter = true;
                initialized = true;
                authState = AuthState.READY;
            } catch (Exception e) {
                LOG.error("Auth/connect failed", e);
                authError = "Unable to connect to game server.";
                authState = AuthState.FAILED;
            }

            if (authState == AuthState.FAILED && nettyClient != null) {
                try {
                    nettyClient.disconnect();
                } catch (Exception e) {
                    LOG.debug("Failed disconnect after login error", e);
                }
            }
        }, "erynfall-auth-connect");
        t.setDaemon(true);
        t.start();
    }

    // -----------------------------------------------------------------------
    // Render loop
    // -----------------------------------------------------------------------

    @Override
    public void render() {
        if (authState == AuthState.FAILED) {
            if (game != null) {
                String error = authError;
                authState = AuthState.CONNECTING;
                Gdx.app.postRunnable(() -> game.showLoginScreen(error));
            }
            return;
        }

        if (!initialized) {
            Gdx.gl.glClearColor(0.05f, 0.05f, 0.05f, 1f);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            screenBatch.setProjectionMatrix(screenProjection);
            screenBatch.begin();
            font.setColor(Color.LIGHT_GRAY);
            font.draw(screenBatch, "Logging in...",
                Gdx.graphics.getWidth() / 2f - 40,
                Gdx.graphics.getHeight() / 2f);
            screenBatch.end();
            return;
        }

        Gdx.gl.glClearColor(0.12f, 0.12f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float delta = Gdx.graphics.getDeltaTime();

        processServerEvents();
        handleInput();
        processApproach();
        processGroundItemApproach();
        updateMovement(delta);
        updateRunEnergy(delta);
        updateNpcVisuals(delta);
        updateClickMarkers(delta);
        if (pickupAnimationTimer > 0) pickupAnimationTimer = Math.max(0f, pickupAnimationTimer - delta);

        // Camera follows player visual position
        camera.position.set(
            renderer.worldToScreenX(visualX, visualY),
            renderer.worldToScreenY(visualX, visualY), 0);
        camera.update();

        // --- World ---
        renderer.renderWorld(tileMap, visualX, visualY);

        // Ground items — rendered before NPCs and player
        for (Map.Entry<Integer, int[]> entry : groundItemsOnMap.entrySet()) {
            int[] data = entry.getValue();  // {itemId, qty, x, y}
            renderer.renderGroundItem(data[2], data[3], data[0], data[1]);
        }

        // Entities — NPCs and other players at their interpolated visual positions
        ClientPacketHandler handler = handler();
        if (handler != null) {
            for (Map.Entry<Integer, int[]> entry : handler.getEntityPositions().entrySet()) {
                int id = entry.getKey();
                if (localPlayerId >= 0 && id == localPlayerId) continue;  // local player rendered below

                float[] vis = npcVisual.get(id);
                if (vis == null) continue;

                if (handler.isPlayer(id)) {
                    // Other connected player — render as player sprite (no action state known)
                    renderer.renderPlayer(vis[0], vis[1], false, null);
                } else {
                    String npcName = handler.getEntityName(id);
                    renderer.renderNPC(vis[0], vis[1], id, npcName);
                }

                int[] hp = handler.getEntityHealth(id);
                if (hp[0] < hp[1]) {
                    renderer.renderHealthBar(vis[0], vis[1], hp[0], hp[1]);
                }
            }
        }

        // Player (pickup animation plays for 1.8 s after item is clicked)
        renderer.renderPlayer(visualX, visualY, pickupAnimationTimer > 0, pendingAction);

        // Hitsplats
        combatUI.update(delta);
        combatUI.render(shapeRenderer, batch, font, camera);

        // Overhead chat text (world space — same projection as hitsplats)
        chatBox.update(delta);
        xpDropOverlay.update(delta);
        levelUpOverlay.update(delta);
        renderOtherPlayerNametags();
        renderOverheadText(delta);
        renderClickMarkers();

        // --- Screen-space UI ---
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        int w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
        renderHUD();
        if (combatTargetId >= 0) renderOpponentInfo();
        sidePanel.update(delta);
        // Convert LibGDX mouse Y (0=top) to screen Y (0=bottom)
        int mouseScreenX = Gdx.input.getX();
        int mouseScreenY = h - Gdx.input.getY();
        sidePanel.render(shapeRenderer, screenBatch, font, w, h, screenProjection,
                         mouseScreenX, mouseScreenY);
        miniMap.render(shapeRenderer, screenBatch, font, screenProjection,
            w, h, playerX, playerY, tileMap, handler());
        if (dialogueUI.isVisible()) {
            renderDialogueOverlay(mouseScreenX, mouseScreenY);
        } else {
            chatBox.render(shapeRenderer, screenBatch, font, w, h, screenProjection);
        }
        levelUpOverlay.render(shapeRenderer, screenBatch, font, w, h, screenProjection);
        xpDropOverlay.render(shapeRenderer, screenBatch, font, w, h, screenProjection,
            sidePanel.getPanelX(), SidePanel.TOTAL_H + SidePanel.MARGIN);
        if (contextMenu.isVisible()) renderContextMenu();
        if (deathScreenTimer > 0) renderDeathScreen(delta);
        renderLogoutMenu();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    // -----------------------------------------------------------------------
    // Server events
    // -----------------------------------------------------------------------

    private void processServerEvents() {
        ClientPacketHandler h = handler();
        if (h == null) return;

        // Cache local player ID for overhead text lookup
        if (localPlayerId < 0) localPlayerId = h.getMyPlayerId();

        playerHealth    = h.getPlayerHealth();
        playerMaxHealth = h.getPlayerMaxHealth();
        attackLevel   = h.getSkillLevel(0);
        strengthLevel = h.getSkillLevel(1);
        defenceLevel  = h.getSkillLevel(2);

        // Sync full skill data to the Skills tab
        int[]  lvls = new int[23];
        long[] xps  = new long[23];
        for (int i = 0; i < 23; i++) {
            lvls[i] = h.getSkillLevel(i);
            xps[i]  = h.getSkillTotalXp(i);
        }
        sidePanel.setSkillData(lvls, xps);
        sidePanel.setMember(h.isMember());

        // Sync inventory to the Inventory tab
        for (int i = 0; i < 28; i++) {
            sidePanel.setInventorySlot(i,
                h.getInventoryItemId(i),
                h.getInventoryQuantity(i),
                h.getInventoryName(i),
                h.getInventoryFlags(i));
        }

        for (int slot = 0; slot < 11; slot++) {
            sidePanel.setEquipmentSlot(slot,
                h.getEquipmentItemId(slot),
                h.getEquipmentName(slot));
        }
        sidePanel.setEquipBonuses(h.getEquipBonuses());

        // Authoritative local-player position correction from server.
        // This is critical now that some interactions (e.g. skilling) can move
        // the player server-side while approaching a target.
        int myId = h.getMyPlayerId();
        if (myId > 0) {
            int[] serverPos = h.getEntityPosition(myId);
            if (serverPos != null) {
                int sx = serverPos[0];
                int sy = serverPos[1];
                int dx = Math.abs(sx - playerX);
                int dy = Math.abs(sy - playerY);
                boolean locallySteering = walkDestX >= 0 || pendingNpcId >= 0 || pendingGroundItemId >= 0;

                if (!locallySteering) {
                    playerX = sx;
                    playerY = sy;
                    walkPath.clear();
                    lastStepSentX = Integer.MIN_VALUE;
                    lastStepSentY = Integer.MIN_VALUE;
                }
                // While steering, only hard-correct on very large divergence.
                if (!locallySteering || dx > 4 || dy > 4) {
                    playerX = sx;
                    playerY = sy;
                    visualX = sx;
                    visualY = sy;
                    if (!locallySteering) {
                        walkDestX = -1;
                        walkDestY = -1;
                        walkPath.clear();
                        lastStepSentX = Integer.MIN_VALUE;
                        lastStepSentY = Integer.MIN_VALUE;
                    }
                }
            }
        }

        // Sync ground items
        groundItemsOnMap.clear();
        groundItemsOnMap.putAll(h.getGroundItems());
        groundItemNamesMap.clear();
        groundItemNamesMap.putAll(h.getGroundItemNames());

        // Process NPC deaths — remove visuals for despawned NPCs
        for (int deadId : h.drainDespawnedNpcs()) {
            npcVisual.remove(deadId);
            if (combatTargetId == deadId) {
                combatTargetId = -1;
            }
            LOG.debug("Client: removed visual for dead NPC {}", deadId);
        }

        for (ClientPacketHandler.SkillingStateEvent skillingEvent : h.drainSkillingStateEvents()) {
            if (pendingAction == null) {
                continue;
            }
            NetworkProto.SkillingType pendingType = switch (pendingAction) {
                case "chop" -> NetworkProto.SkillingType.SKILLING_WOODCUTTING;
                case "fish" -> NetworkProto.SkillingType.SKILLING_FISHING;
                case "cook_at" -> NetworkProto.SkillingType.SKILLING_COOKING;
                default -> NetworkProto.SkillingType.SKILLING_NONE;
            };
            if (pendingType == skillingEvent.type && pendingNpcId == skillingEvent.targetNpcId
                && (skillingEvent.state == NetworkProto.SkillingState.SKILLING_STATE_ACTIVE
                || skillingEvent.state == NetworkProto.SkillingState.SKILLING_STATE_STOPPED)) {
                clearPendingAction();
            }
        }

        // Server chat messages ("I can't reach that!", "Too late — it's gone!", etc.)
        for (String msg : h.drainServerChatMessages()) {
            chatBox.addSystemMessage(msg);
            // Any server response to a talk attempt means the server handled it — stop retrying.
            if ("talk".equals(pendingAction)) {
                clearPendingAction();
            }
            if ("I can't reach that!".equals(msg)) {
                clearPendingAction();
                pendingGroundItemId = -1;
                continue;
            }

            if ("Please step away before talking.".equals(msg)
                || "You get some logs.".equals(msg)
                || "You catch some shrimps.".equals(msg)
                || "You start cooking the shrimps.".equals(msg)
                || "You cook the shrimps.".equals(msg)
                || "You accidentally burn the shrimps.".equals(msg)
                || "This tree has been chopped down.".equals(msg)
                || "There are no fish here right now.".equals(msg)
                || "You are too busy fighting.".equals(msg)
                || "Your inventory is too full to hold any more logs.".equals(msg)) {
                clearPendingAction();
            }

            if ("You need an axe to chop this tree.".equals(msg)
                || "You need a small fishing net to fish here.".equals(msg)
                || "Your inventory is too full to hold any more fish.".equals(msg)
                || "You have no raw shrimps to cook.".equals(msg)) {
                clearPendingAction();
            }
        }

        ClientPacketHandler.LogoutEvent logoutEvent = h.consumeLogoutEvent();
        if (logoutEvent != null) {
            handleLogoutEvent(logoutEvent);
            return;
        }

        // Death detection — show overlay and snap to respawn
        if (h.isPlayerDead()) {
            h.consumePlayerDeath();
            deathScreenTimer = DEATH_SCREEN_DURATION;
            playerX   = h.getDeathRespawnX();
            playerY   = h.getDeathRespawnY();
            visualX   = playerX;
            visualY   = playerY;
            combatTargetId = -1;
            clearPendingAction();
            chatBox.addSystemMessage("Oh dear, you are dead!");
            LOG.info("Death screen shown — respawning at ({}, {})", playerX, playerY);
        }

        for (ClientPacketHandler.CombatHitEvent evt : h.drainCombatHits()) {
            combatUI.addDamageNumber(evt.targetX, evt.targetY, evt.damage, evt.hit);
            boolean npcHitMe = (evt.targetId == h.getMyPlayerId());
            if (npcHitMe) {
                chatBox.addSystemMessage(evt.hit
                    ? String.format("You were hit for %d!", evt.damage)
                    : "The attack missed you.");
            } else {
                chatBox.addSystemMessage(evt.hit
                    ? String.format("You hit for %d!", evt.damage)
                    : "Your attack missed.");
                // XP is shown via XpDropOverlay (per-skill events), not here
            }
        }

        // XP drop events — show OSRS-style floating XP drops on the right side
        String[] skillNames = {
            "Attack", "Strength", "Defence", "Hitpoints", "Ranged", "Magic",
            "Prayer", "Woodcutting", "Fishing", "Cooking", "Mining", "Smithing", "Firemaking",
            "Crafting", "Runecrafting", "Fletching", "Agility", "Herblore", "Thieving",
            "Slayer", "Farming", "Hunter", "Construction"
        };
        for (ClientPacketHandler.PrayerPointsEvent pp : h.drainPrayerPoints()) {
            playerPrayer    = pp.current;
            playerMaxPrayer = pp.maximum;
        }
        // Keep SidePanel prayer tab in sync with latest values
        sidePanel.setPrayerState(playerPrayer, playerMaxPrayer, null);
        for (ClientPacketHandler.XpDropEvent xp : h.drainXpDrops()) {
            if (xp.skillIndex >= 0 && xp.skillIndex < skillNames.length) {
                xpDropOverlay.addDrop(xp.skillIndex, xp.xpGained);
            }
        }

        if (h.consumeLevelUp()) {
            int idx = h.getLeveledUpSkill();
            if (idx >= 0 && idx < skillNames.length) {
                int newLevel = h.getSkillLevel(idx);
                levelUpOverlay.addLevelUp(idx, newLevel);
                chatBox.addSystemMessage(
                    "Congratulations, you just advanced a "
                    + skillNames[idx] + " level. Your "
                    + skillNames[idx] + " level is now " + newLevel + ".");
            }
        }

        // Public chat broadcasts from nearby players.
        // Skip our own messages — already shown optimistically in submitChat().
        for (ClientPacketHandler.ChatBroadcastEvent evt : h.drainChatBroadcasts()) {
            if (evt.senderId == localPlayerId) continue;  // own message already displayed
            chatBox.addPublicMessage(evt.senderName, evt.text);
            overheadTexts.put(evt.senderId, new OverheadText(evt.text));
        }

        for (ClientPacketHandler.QuestUpdateEvent evt : h.drainQuestUpdates()) {
            int shown = shownQuestProgress.getOrDefault(evt.questId, -1);
            boolean wasComplete = shownQuestComplete.getOrDefault(evt.questId, false);

            List<SidePanel.QuestTaskView> tasks = new ArrayList<>();
            for (ClientPacketHandler.QuestUpdateEvent.TaskEvent task : evt.tasks) {
                tasks.add(new SidePanel.QuestTaskView(
                    task.taskId,
                    task.description,
                    task.currentCount,
                    task.requiredCount,
                    task.completed
                ));
            }

            SidePanel.QuestStatus status = switch (evt.status) {
                case COMPLETED -> SidePanel.QuestStatus.COMPLETED;
                case IN_PROGRESS -> SidePanel.QuestStatus.IN_PROGRESS;
                case NOT_STARTED -> SidePanel.QuestStatus.NOT_STARTED;
            };

            sidePanel.setQuestState(new SidePanel.QuestView(
                evt.questId,
                evt.questName,
                evt.questDescription,
                evt.miniquest,
                evt.questPointsReward,
                status,
                tasks
            ), evt.playerTotalQuestPoints);

            if (shown != evt.tasksCompleted || (evt.completed && !wasComplete)) {
                if (evt.completed && !wasComplete) {
                    chatBox.addSystemMessage("Quest complete: " + evt.questName + "!");
                } else {
                    chatBox.addSystemMessage(evt.questName + ": "
                        + evt.tasksCompleted + "/" + evt.tasksTotal + " objectives complete.");
                }
                shownQuestProgress.put(evt.questId, evt.tasksCompleted);
                shownQuestComplete.put(evt.questId, evt.completed);
            }
        }

        for (ClientPacketHandler.DialoguePromptEvent evt : h.drainDialoguePrompts()) {
            if (evt.options.isEmpty() && (evt.npcText == null || evt.npcText.isBlank())) {
                dialogueUI.close();
                continue;
            }

            List<DialogueUI.DialogueOption> uiOptions = new ArrayList<>();
            for (ClientPacketHandler.DialoguePromptEvent.DialogueOptionEvent opt : evt.options) {
                uiOptions.add(new DialogueUI.DialogueOption(opt.optionId, opt.text));
            }
            dialogueUI.open("npc_" + evt.npcId, evt.npcId, evt.npcText, uiOptions);
            // We got server acknowledgement; stop re-sending talk intent.
            if (pendingNpcId == evt.npcId && "talk".equals(pendingAction)) {
                clearPendingAction();
            }
        }
    }

    // -----------------------------------------------------------------------
    // NPC visual interpolation
    // -----------------------------------------------------------------------

    /**
     * For each NPC whose logical position changed (from EntityUpdate), the
     * visual position smoothly interpolates at TILES_PER_SECOND.
     * New entities are snapped directly to their logical position.
     */
    private void updateNpcVisuals(float delta) {
        ClientPacketHandler h = handler();
        if (h == null) return;

        for (Map.Entry<Integer, int[]> entry : h.getEntityPositions().entrySet()) {
            int id = entry.getKey();
            if (localPlayerId >= 0 && id == localPlayerId) continue;

            int[] logical = entry.getValue();
            float[] vis   = npcVisual.get(id);

            if (vis == null) {
                // First time we see this NPC — snap to position
                npcVisual.put(id, new float[]{logical[0], logical[1]});
                continue;
            }

            float dx   = logical[0] - vis[0];
            float dy   = logical[1] - vis[1];
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist < 0.01f) {
                vis[0] = logical[0];
                vis[1] = logical[1];
            } else {
                float step = TILES_PER_SECOND * delta;
                if (step >= dist) {
                    vis[0] = logical[0];
                    vis[1] = logical[1];
                } else {
                    vis[0] += (dx / dist) * step;
                    vis[1] += (dy / dist) * step;
                }
            }
        }
    }

    private void updateClickMarkers(float delta) {
        clickMarkers.removeIf(m -> { m.update(delta); return m.expired(); });
    }

    // -----------------------------------------------------------------------
    // Input
    // -----------------------------------------------------------------------

    private int[] screenToTile(int sx, int sy) {
        Vector3 w = new Vector3(sx, sy, 0);
        camera.unproject(w);
        return new int[]{
            CoordinateConverter.screenToWorldX(w.x, w.y),
            CoordinateConverter.screenToWorldY(w.x, w.y)
        };
    }

    private void handleInput() {
        // Block all input while death screen is showing
        if (deathScreenTimer > 0) return;

        if (dialogueUI.isVisible()) {
            handleDialogueInput();
            return;
        }

        // ── Chat input ────────────────────────────────────────────────────────
        if (suppressInitialEnter) {
            if (!Gdx.input.isKeyPressed(Input.Keys.ENTER)) {
                suppressInitialEnter = false;
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            // Logout modal has priority focus: Enter confirms Logout.
            if (logoutMenuVisible) {
                logoutMenuVisible = false;
                requestLogout();
                return;
            }
            if (suppressInitialEnter) {
                return;
            }
            if (!chatBox.isActive()) {
                chatBox.setActive(true);
            } else {
                submitChat();
            }
            return;
        }

        if (chatBox.isActive()) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                chatBox.setActive(false);
                return;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
                String buf = chatBox.getInputBuffer();
                if (!buf.isEmpty())
                    chatBox.setInputBuffer(buf.substring(0, buf.length() - 1));
                return;
            }
            // ── Letters: A-Z (key codes 29-54 in LibGDX) ──────────────────
            // NOTE: Input.Keys.SPACE=62, Input.Keys.A=29, Input.Keys.Z=54.
            // The range must start at A (29), NOT SPACE (62).
            for (int key = Input.Keys.A; key <= Input.Keys.Z; key++) {
                if (Gdx.input.isKeyJustPressed(key)) {
                    if (chatBox.getInputBuffer().length() >= 80) return;
                    char c = Input.Keys.toString(key).charAt(0);  // returns uppercase e.g. "A"
                    if (!Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                     && !Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) {
                        c = Character.toLowerCase(c);
                    }
                    chatBox.setInputBuffer(chatBox.getInputBuffer() + c);
                    return;
                }
            }
            // ── Space ──────────────────────────────────────────────────────
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                if (chatBox.getInputBuffer().length() < 80)
                    chatBox.setInputBuffer(chatBox.getInputBuffer() + ' ');
                return;
            }
            // ── Digits 0-9 ─────────────────────────────────────────────────
            int[] numKeys  = {Input.Keys.NUM_0, Input.Keys.NUM_1, Input.Keys.NUM_2,
                              Input.Keys.NUM_3, Input.Keys.NUM_4, Input.Keys.NUM_5,
                              Input.Keys.NUM_6, Input.Keys.NUM_7, Input.Keys.NUM_8,
                              Input.Keys.NUM_9};
            char[] numChars = {'0','1','2','3','4','5','6','7','8','9'};
            for (int i = 0; i < numKeys.length; i++) {
                if (Gdx.input.isKeyJustPressed(numKeys[i])) {
                    if (chatBox.getInputBuffer().length() < 80)
                        chatBox.setInputBuffer(chatBox.getInputBuffer() + numChars[i]);
                    return;
                }
            }
            // ── Common punctuation ─────────────────────────────────────────
            if (Gdx.input.isKeyJustPressed(Input.Keys.APOSTROPHE)) {
                if (chatBox.getInputBuffer().length() < 80)
                    chatBox.setInputBuffer(chatBox.getInputBuffer() + '\'');
                return;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.COMMA)) {
                if (chatBox.getInputBuffer().length() < 80)
                    chatBox.setInputBuffer(chatBox.getInputBuffer() + ',');
                return;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.PERIOD)) {
                if (chatBox.getInputBuffer().length() < 80)
                    chatBox.setInputBuffer(chatBox.getInputBuffer() + '.');
                return;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.MINUS)) {
                if (chatBox.getInputBuffer().length() < 80)
                    chatBox.setInputBuffer(chatBox.getInputBuffer() + '-');
                return;
            }
            // Absorb all other keys while chat is open
            return;
        }
        // ── End chat input ────────────────────────────────────────────────────

        int mx = Gdx.input.getX();
        int my = Gdx.input.getY();
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        // LibGDX y-flip: screen-space has Y=0 at bottom
        int screenMy = h - my;

        // Update drag position continuously (for inventory drag)
        sidePanel.updateDrag(mx, screenMy);

        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            // Check side panel right-click first
            if (sidePanel.isOverPanel(mx, screenMy)) {
                if (sidePanel.isInventoryTabActive()) {
                    int slot = sidePanel.getInventoryRightClickSlot(mx, screenMy);
                    if (slot >= 0 && sidePanel.getInventoryItemId(slot) > 0) {
                        showInventoryContextMenu(slot, mx, screenMy);
                    }
                }
            } else {
                int[] tile = screenToTile(mx, my);
                LOG.debug("Right-click tile ({},{})", tile[0], tile[1]);
                List<ContextMenu.MenuItem> opts = generateContextMenu(tile[0], tile[1]);
                if (!opts.isEmpty())
                    contextMenu.open(mx, screenMy, opts, w, h);
            }
        }

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            inventoryMouseDownSlot = -1;
            // OSRS run: clicking the run energy orb toggles run on/off
            {
                int rnCy = h - 180;
                if (Math.hypot(mx - 35, screenMy - rnCy) <= 22) {
                    isRunning = !isRunning;
                    if (playerRunEnergy <= 0) isRunning = false; // can't enable at 0 energy
                    return;
                }
            }
            if (logoutMenuVisible) {
                handleLogoutMenuClick(mx, screenMy);
                return;
            }
            if (screenMy < ChatBox.TOTAL_H && mx >= 0 && mx < ChatBox.BOX_W) {
                if (chatBox.handleClick(mx, screenMy)) {
                    return;
                }
            }

            // Click on the chat box input area activates typing (OSRS behaviour)
            if (screenMy < ChatBox.TOTAL_H && mx >= 0 && mx < ChatBox.BOX_W) {
                chatBox.setActive(true);
                return;
            }
            if (contextMenu.isVisible()) {
                ContextMenu.MenuItem clicked = contextMenu.getClickedItem(mx, screenMy);
                if (clicked != null) handleContextMenuAction(clicked);
                contextMenu.close();
            } else if (sidePanel.isOverPanel(mx, screenMy)) {
                if (sidePanel.isInventoryTabActive()) {
                    int slot = sidePanel.getInventorySlotAt(mx, screenMy);
                    inventoryMouseDownSlot = slot;
                    if (selectedInventorySlot >= 0
                            && (slot < 0 || sidePanel.getInventoryItemId(slot) <= 0)) {
                        selectedInventorySlot = -1;
                        sidePanel.setSelectedInventorySlot(-1);
                    }
                }
                int click = sidePanel.handleLeftClick(mx, screenMy);
                if (click >= 0) {
                    sidePanel.setCombatStyle(click);
                    if (nettyClient != null) nettyClient.sendSetCombatStyle(click);
                } else if (click <= -200) {
                    int prayerId = -(click + 200);
                    if (nettyClient != null) nettyClient.sendTogglePrayer(prayerId);
                } else if (click <= -100) {
                    int equipSlot = -(click + 100);
                    if (nettyClient != null) nettyClient.sendUnequipItem(equipSlot);
                }
                if (sidePanel.consumeLogoutRequested()) {
                    requestLogout();
                }
            } else {
                if (selectedInventorySlot >= 0) {
                    selectedInventorySlot = -1;
                    sidePanel.setSelectedInventorySlot(-1);
                }
                int[] tile = screenToTile(mx, my);
                if (CoordinateConverter.isValidTile(tile[0], tile[1])) {
                    boolean didAction = handleWorldLeftClick(tile[0], tile[1]);
                    if (!didAction) {
                        walkTo(tile[0], tile[1]);
                    }
                    clickMarkers.add(new ClickMarker(tile[0], tile[1], didAction));
                }
            }
        }

        // Mouse-up: finish inventory drag
        if (!Gdx.input.isButtonPressed(Input.Buttons.LEFT) && sidePanel.isInventoryDragging()) {
            int[] swap = sidePanel.handleInventoryMouseUp(mx, screenMy);
            if (swap != null && nettyClient != null) {
                nettyClient.sendSwapInventorySlots(swap[0], swap[1]);
            } else if (swap == null && inventoryMouseDownSlot >= 0) {
                int releaseSlot = sidePanel.getInventorySlotAt(mx, screenMy);
                if (releaseSlot == inventoryMouseDownSlot) {
                    handleInventoryPrimaryAction(inventoryMouseDownSlot);
                }
            }
            inventoryMouseDownSlot = -1;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (logoutMenuVisible) {
                logoutMenuVisible = false;
            } else {
                boolean dismissed = contextMenu.isVisible() || selectedInventorySlot >= 0;
                contextMenu.close();
                if (selectedInventorySlot >= 0) {
                    selectedInventorySlot = -1;
                    sidePanel.setSelectedInventorySlot(-1);
                }
                if (!dismissed) {
                    logoutMenuVisible = true;
                }
            }
        }
    }

    private void handleDialogueInput() {
        // Escape: dismiss dialogue without selecting an option
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            dialogueUI.close();
            return;
        }

        DialogueUI.DialoguePhase phase = dialogueUI.getCurrentPhase();

        if (phase == DialogueUI.DialoguePhase.NPC_SPEAKING
                || phase == DialogueUI.DialoguePhase.NPC_RESPONDING) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                    || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                dialogueUI.advanceToNextLine();
            }
            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                int mx = Gdx.input.getX();
                int rawMy = Gdx.input.getY();
                int screenMy = Gdx.graphics.getHeight() - rawMy;

                if (dialogueUI.isContinueButtonHit(mx, screenMy)) {
                    dialogueUI.advanceToNextLine();
                } else if (!dialogueUI.isOverDialogue(mx, screenMy)) {
                    dialogueUI.close();
                }
            }
            return;
        }

        if (phase != DialogueUI.DialoguePhase.PLAYER_CHOOSING) {
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) { submitDialogueOptionByIndex(0); return; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) { submitDialogueOptionByIndex(1); return; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) { submitDialogueOptionByIndex(2); return; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) { submitDialogueOptionByIndex(3); return; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_5)) { submitDialogueOptionByIndex(4); return; }

        // OSRS-style continue shortcut; for option prompts we map Space to option 1.
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) { submitDialogueOptionByIndex(0); return; }

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            int mx      = Gdx.input.getX();
            int rawMy   = Gdx.input.getY();
            int screenMy = Gdx.graphics.getHeight() - rawMy;

            DialogueUI.DialogueOption selected = dialogueUI.getSelectedOption(mx, screenMy);
            if (selected != null) {
                // Send response to server (if connected)
                if (nettyClient != null) {
                    if (selected.optionId < 0 && dialogueUI.hasBackButton()) {
                        nettyClient.sendDialogueResponse(-1);
                    } else if (selected.optionId >= 0) {
                        nettyClient.sendDialogueResponse(selected.optionId);
                    }
                }
                // Always close: server reopens with new DialoguePromptEvent if needed.
                // This is OSRS-accurate: dialogue closes on selection, server drives next state.
                dialogueUI.close();
            } else if (!dialogueUI.isOverDialogue(mx, screenMy)) {
                dialogueUI.close();
                if (sidePanel.isOverPanel(mx, screenMy)) {
                    int click = sidePanel.handleLeftClick(mx, screenMy);
                    if (click >= 0) {
                        sidePanel.setCombatStyle(click);
                        if (nettyClient != null) nettyClient.sendSetCombatStyle(click);
                    } else if (click <= -200) {
                        int prayerId = -(click + 200);
                        if (nettyClient != null) nettyClient.sendTogglePrayer(prayerId);
                    } else if (click <= -100) {
                        int equipSlot = -(click + 100);
                        if (nettyClient != null) nettyClient.sendUnequipItem(equipSlot);
                    }
                    if (sidePanel.consumeLogoutRequested()) requestLogout();
                } else {
                    int[] tile = screenToTile(mx, rawMy);
                    if (CoordinateConverter.isValidTile(tile[0], tile[1])) {
                        walkTo(tile[0], tile[1]);
                    }
                }
            }
        }
    }

    private void submitDialogueOptionByIndex(int index) {
        if (dialogueUI.getCurrentPhase() != DialogueUI.DialoguePhase.PLAYER_CHOOSING) {
            return;
        }
        if (index < 0 || index >= dialogueUI.getOptions().size() || nettyClient == null) {
            return;
        }
        nettyClient.sendDialogueResponse(dialogueUI.getOptions().get(index).optionId);
        // Always close after sending; server reopens if there is more dialogue.
        dialogueUI.close();
    }

    private void requestLogout() {
        if (logoutRequested) return;
        logoutRequested = true;
        clearPendingAction();
        pendingGroundItemId = -1;
        chatBox.addSystemMessage("Logging out...");

        if (nettyClient != null && nettyClient.isConnected()) {
            nettyClient.sendLogoutRequest();
            return;
        }

        // Offline fallback
        if (game != null) {
            Gdx.app.postRunnable(game::showLoginScreen);
        } else {
            Gdx.app.exit();
        }
    }

    private void handleLogoutEvent(ClientPacketHandler.LogoutEvent event) {
        if (!event.success) {
            logoutRequested = false;
            chatBox.addSystemMessage(
                event.message == null || event.message.isBlank()
                    ? "Unable to log out right now."
                    : event.message
            );
            return;
        }

        if (nettyClient != null) {
            try {
                nettyClient.disconnect();
            } catch (Exception e) {
                LOG.debug("Disconnect after logout acknowledgement failed", e);
            }
        }

        if (game != null) {
            Gdx.app.postRunnable(game::showLoginScreen);
        } else {
            Gdx.app.exit();
        }
    }

    /**
     * Send the current input buffer as a public chat message.
     *
     * OSRS behaviour: message appears in the chat box IMMEDIATELY (client-side),
     * then the server broadcasts it to nearby players. We do the same: show
     * optimistically here, and skip the server echo when it arrives back to us.
     */
    private void submitChat() {
        String msg = chatBox.getInputBuffer().trim();
        chatBox.setActive(false);   // close input regardless
        if (msg.isEmpty()) return;

        // Immediate local display — player always sees their own message at once
        ClientPacketHandler h = handler();
        String myName = "";
        if (h != null && localPlayerId >= 0) myName = h.getEntityName(localPlayerId);
        if (myName.isEmpty()) myName = "Me";

        chatBox.addPublicMessage(myName, msg);
        if (localPlayerId >= 0) {
            overheadTexts.put(localPlayerId, new OverheadText(msg));
        }

        // Send to server for broadcast to other nearby players
        if (nettyClient != null) nettyClient.sendPublicChat(msg);
    }

    private void showInventoryContextMenu(int slot, int mx, int my) {
        String name = sidePanel.getInventoryItemName(slot);
        if (name == null || name.isEmpty()) name = "Item";
        int itemId = sidePanel.getInventoryItemId(slot);
        int itemFlags = sidePanel.getInventoryItemFlags(slot);

        List<ContextMenu.MenuItem> opts = new ArrayList<>();
        if ((itemFlags & 0x2) != 0) {
            // Consumable
            opts.add(new ContextMenu.MenuItem("Eat " + name, "inv_eat", slot));
        }
        if ((itemFlags & 0x1) != 0) {
            // Equipable
            opts.add(new ContextMenu.MenuItem("Wield " + name, "inv_wield", slot));
        }
        if (itemId == 526) {
            opts.add(new ContextMenu.MenuItem("Bury " + name, "inv_bury", slot));
        }
        if (itemId == 590) {
            opts.add(new ContextMenu.MenuItem("Use " + name, "inv_use", slot));
        }
        opts.add(new ContextMenu.MenuItem("Drop " + name, "inv_drop", slot));
        opts.add(new ContextMenu.MenuItem("Examine " + name, "inv_examine", slot));
        contextMenu.open(mx, my, opts, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    /**
     * Fires the left-click primary action for a given inventory slot.
     * Called on mouse UP when the user clicked (not dragged) an item.
     * Use-mode is checked first — a second click in use-mode sends UseItemOnItem.
     */
    private void handleInventoryPrimaryAction(int slot) {
        int itemId    = sidePanel.getInventoryItemId(slot);
        int itemFlags = sidePanel.getInventoryItemFlags(slot);
        if (itemId <= 0) return;

        if (selectedInventorySlot >= 0) {
            if (slot != selectedInventorySlot && nettyClient != null) {
                nettyClient.sendUseItemOnItem(selectedInventorySlot, slot);
            }
            selectedInventorySlot = -1;
            sidePanel.setSelectedInventorySlot(-1);
            return;
        }

        if (itemId == 526) {
            if (nettyClient != null) nettyClient.sendUseItem(slot, "bury");
            pickupAnimationTimer = PICKUP_ANIM_DURATION;
            return;
        }

        if ((itemFlags & 0x2) != 0) {
            if (nettyClient != null) nettyClient.sendUseItem(slot, "eat");
            return;
        }

        if ((itemFlags & 0x1) != 0) {
            if (nettyClient != null) nettyClient.sendUseItem(slot, "wield");
            return;
        }

        if (itemId == 590) {
            selectedInventorySlot = slot;
            sidePanel.setSelectedInventorySlot(slot);
            return;
        }
    }

    /** Player-initiated walk — cancels any pending approach. */
    private void walkTo(int x, int y) {
        clearPendingAction();
        if (!planWalkPath(x, y)) {
            chatBox.addSystemMessage("I can't reach that!");
            return;
        }
        if (nettyClient != null) nettyClient.sendWalkTo(x, y);
    }

    /** Auto-walk from approach logic — does NOT clear the pending action. */
    private void autoWalkTo(int x, int y) {
        if (!planWalkPath(x, y)) {
            return;
        }
        pendingWalkTargX = x; pendingWalkTargY = y;
        if (nettyClient != null) nettyClient.sendWalkTo(x, y);
    }

    private boolean planWalkPath(int targetX, int targetY) {
        // If a speculative step is already in-flight, plan from the server's current
        // position (lastStepSentX/Y) rather than playerX/playerY, which lags one tile
        // behind. Prepend the in-flight tile so ongoing visual animation is not disrupted.
        boolean speculativeInFlight = lastStepSentX != Integer.MIN_VALUE;
        int fromX = speculativeInFlight ? lastStepSentX : playerX;
        int fromY = speculativeInFlight ? lastStepSentY : playerY;

        List<int[]> path = findPath(fromX, fromY, targetX, targetY);
        if (path.isEmpty()) {
            walkDestX = -1;
            walkDestY = -1;
            walkPath.clear();
            lastStepSentX = Integer.MIN_VALUE;
            lastStepSentY = Integer.MIN_VALUE;
            return false;
        }
        walkDestX = targetX;
        walkDestY = targetY;
        walkPath.clear();
        // Keep the in-flight step at the head so the running visual animation
        // finishes naturally before the newly planned steps begin.
        if (speculativeInFlight) {
            walkPath.add(new int[]{lastStepSentX, lastStepSentY});
        }
        walkPath.addAll(path);
        lastStepSentX = Integer.MIN_VALUE;
        lastStepSentY = Integer.MIN_VALUE;
        return true;
    }

    // -----------------------------------------------------------------------
    // Player movement interpolation
    // -----------------------------------------------------------------------

    private void updateMovement(float delta) {
        int[] nextStep = walkPath.peekFirst();
        float targetX = (nextStep != null) ? nextStep[0] : playerX;
        float targetY = (nextStep != null) ? nextStep[1] : playerY;

        // Send the intended adjacent step as soon as we start moving toward it,
        // so server-authoritative position advances even if render interpolation
        // gets corrected or interrupted.
        if (nextStep != null && nettyClient != null) {
            int sx = nextStep[0];
            int sy = nextStep[1];
            if (sx != lastStepSentX || sy != lastStepSentY) {
                nettyClient.sendPlayerMovement(sx, sy, 0);
                lastStepSentX = sx;
                lastStepSentY = sy;
            }
        }

        float dx = targetX - visualX, dy = targetY - visualY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist < 0.01f) {
            visualX = targetX; visualY = targetY;
            if (nextStep != null) {
                playerX = nextStep[0];
                playerY = nextStep[1];
                walkPath.pollFirst();
                if (isRunning) consumeRunEnergyStep();
                if (nettyClient != null) nettyClient.sendPlayerMovement(playerX, playerY, 0);
                if (walkPath.isEmpty()) {
                    walkDestX = -1;
                    walkDestY = -1;
                    lastStepSentX = Integer.MIN_VALUE;
                    lastStepSentY = Integer.MIN_VALUE;
                }
            }
            return;
        }

        float tps   = (isRunning && playerRunEnergy > 0) ? TILES_PER_SECOND * 2f : TILES_PER_SECOND;
        float step  = tps * delta;
        if (step >= dist) {
            visualX = targetX; visualY = targetY;
            if (nextStep != null) {
                playerX = nextStep[0];
                playerY = nextStep[1];
                walkPath.pollFirst();
                if (isRunning) consumeRunEnergyStep();
                if (nettyClient != null) nettyClient.sendPlayerMovement(playerX, playerY, 0);
                if (walkPath.isEmpty()) {
                    walkDestX = -1;
                    walkDestY = -1;
                    lastStepSentX = Integer.MIN_VALUE;
                    lastStepSentY = Integer.MIN_VALUE;
                }
            }
        } else {
            visualX += (dx / dist) * step;
            visualY += (dy / dist) * step;
        }
    }

    /** Drain 1 energy unit when the player completes a run step. Auto-disables run at 0. */
    private void consumeRunEnergyStep() {
        playerRunEnergy = Math.max(0, playerRunEnergy - 1);
        if (playerRunEnergy <= 0) {
            isRunning = false;
        }
    }

    /**
     * Restores run energy over time when not running.
     * Rate: 1 unit per 4 seconds (≈ OSRS Agility level 1 restore rate on 0–100 scale).
     */
    private void updateRunEnergy(float delta) {
        if (isRunning || playerRunEnergy >= 100) {
            if (isRunning) runRestoreAcc = 0f;
            return;
        }
        runRestoreAcc += delta;
        if (runRestoreAcc >= 4.0f) {
            runRestoreAcc -= 4.0f;
            playerRunEnergy = Math.min(100, playerRunEnergy + 1);
        }
    }

    // -----------------------------------------------------------------------
    // Approach-and-act
    // -----------------------------------------------------------------------

    private void startApproach(int npcId, String action) {
        int[] pos = npcLogicalPosition(npcId);
        if (pos == null) return;

        pendingNpcId  = npcId;
        pendingAction = action;
        pendingActionRetryTimer = 0f;

        if (isInRange(playerX, playerY, pos[0], pos[1])) {
            executePendingAction();
        } else {
            int[] dest = closestAdjacentTile(playerX, playerY, pos[0], pos[1]);
            autoWalkTo(dest[0], dest[1]);
            LOG.info("Approaching NPC {} for '{}' → walking to ({},{})",
                npcId, action, dest[0], dest[1]);
        }
    }

    /**
     * Every frame: check whether we've reached the NPC.
     * If the NPC has wandered since we started walking, re-route to its new
     * adjacent tile (but only when our current target is no longer optimal).
     */
    private void processApproach() {
        if (pendingNpcId < 0) return;
        if (pendingActionRetryTimer > 0f) {
            pendingActionRetryTimer = Math.max(0f, pendingActionRetryTimer - Gdx.graphics.getDeltaTime());
        }

        int[] pos = npcLogicalPosition(pendingNpcId);
        if (pos == null) { clearPendingAction(); return; }

        if (isInRange(playerX, playerY, pos[0], pos[1])) {
            executePendingAction();
            return;
        }

        // Re-route if the NPC moved and our walk target is stale
        int[] best = closestAdjacentTile(playerX, playerY, pos[0], pos[1]);
        if (best[0] != pendingWalkTargX || best[1] != pendingWalkTargY) {
            autoWalkTo(best[0], best[1]);
        }
    }

    private void executePendingAction() {
        if (pendingActionRetryTimer > 0f) {
            return;
        }
        LOG.info("In range of NPC {} — executing '{}'", pendingNpcId, pendingAction);
        if (nettyClient != null) {
            if ("attack".equals(pendingAction)) {
                nettyClient.sendAttack(pendingNpcId);
                combatTargetId = pendingNpcId;
                clearPendingAction();
            } else if ("talk".equals(pendingAction)) {
                nettyClient.sendTalkToNpc(pendingNpcId);
                // Keep pending action alive until server actually opens dialogue,
                // because a moving NPC can invalidate a single packet attempt.
                pendingActionRetryTimer = 0.25f;
            } else if ("chop".equals(pendingAction)) {
                nettyClient.sendStartSkilling(pendingNpcId, NetworkProto.SkillingType.SKILLING_WOODCUTTING);
                pendingActionRetryTimer = 0.25f;
            } else if ("fish".equals(pendingAction)) {
                nettyClient.sendStartSkilling(pendingNpcId, NetworkProto.SkillingType.SKILLING_FISHING);
                pendingActionRetryTimer = 0.25f;
            } else if ("cook_at".equals(pendingAction)) {
                nettyClient.sendStartSkilling(pendingNpcId, NetworkProto.SkillingType.SKILLING_COOKING);
                pendingActionRetryTimer = 0.25f;
            }
        }
    }

    private void clearPendingAction() {
        pendingNpcId = -1; pendingAction = null;
        pendingWalkTargX = -1; pendingWalkTargY = -1;
        pendingActionRetryTimer = 0f;
    }

    private boolean isInRange(int px, int py, int nx, int ny) {
        int chebyshev = Math.max(Math.abs(px - nx), Math.abs(py - ny));
        return chebyshev <= INTERACT_RANGE && !(px == nx && py == ny);
    }

    // -----------------------------------------------------------------------
    // Ground item approach + pickup
    // -----------------------------------------------------------------------

    /**
     * Begin walking toward a ground item to pick it up.
     * Runs a client-side BFS first — if the item's tile is unreachable,
     * shows "I can't reach that!" immediately without sending a packet.
     */
    private void startGroundItemApproach(int groundItemId) {
        ClientPacketHandler h = handler();
        if (h == null) return;

        int[] data = h.getGroundItems().get(groundItemId);
        if (data == null) {
            chatBox.addSystemMessage("Too late — it's gone!");
            return;
        }
        int itemX = data[2], itemY = data[3];

        // BFS reachability check (client-side, OSRS "I can't reach that!")
        if (!canReachTile(playerX, playerY, itemX, itemY)) {
            chatBox.addSystemMessage("I can't reach that!");
            return;
        }

        pendingGroundItemId = groundItemId;
        pendingGroundItemX  = itemX;
        pendingGroundItemY  = itemY;

        // Walk onto the item's tile (OSRS pickup stance)
        if (playerX != itemX || playerY != itemY) {
            if (planWalkPath(itemX, itemY) && nettyClient != null) {
                nettyClient.sendWalkTo(itemX, itemY);
            }
        }
    }

    /**
     * Called every frame while a ground item pickup is pending.
     * When the player is adjacent to the item tile, fires the PickupItem packet
     * and starts the pickup animation.
     */
    private void processGroundItemApproach() {
        if (pendingGroundItemId < 0) return;

        ClientPacketHandler h = handler();
        if (h == null) { pendingGroundItemId = -1; return; }

        // Confirm item still exists (may have despawned while walking)
        if (!h.getGroundItems().containsKey(pendingGroundItemId)) {
            chatBox.addSystemMessage("Too late — it's gone!");
            pendingGroundItemId = -1;
            return;
        }

        // Check if we've reached the item tile itself.
        if (playerX == pendingGroundItemX && playerY == pendingGroundItemY) {
            // Send pickup packet — server will execute after 3-tick animation delay
            if (nettyClient != null) nettyClient.sendPickupItem(pendingGroundItemId);
            pickupAnimationTimer = PICKUP_ANIM_DURATION;
            pendingGroundItemId = -1;
        }
    }

    /**
     * Breadth-first search on the client walkability map.
     * Returns true if there is any walkable path from (fromX, fromY) to
     * exactly (toX, toY).
     *
     * Non-walkable tiles: WATER (2) and WALL (4) — matches TutorialIslandMap constants.
     */
    private boolean canReachTile(int fromX, int fromY, int toX, int toY) {
        if (!CoordinateConverter.isValidTile(toX, toY)) return false;
        // Item tile itself must not be a hard blocker
        int destType = tileMap[toX][toY];
        if (destType == TutorialIslandMap.WATER || destType == TutorialIslandMap.WALL) return false;

        if (fromX == toX && fromY == toY) return true;

        boolean[][] visited = new boolean[TutorialIslandMap.WIDTH][TutorialIslandMap.HEIGHT];
        Deque<int[]> queue  = new ArrayDeque<>();
        queue.add(new int[]{fromX, fromY});
        visited[fromX][fromY] = true;

        int[] dx = {0, 1, 0, -1, 1, 1, -1, -1};
        int[] dy = {1, 0, -1, 0, 1, -1, 1, -1};

        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            if (cur[0] == toX && cur[1] == toY) return true;

            for (int i = 0; i < 8; i++) {
                int nx = cur[0] + dx[i], ny = cur[1] + dy[i];
                if (!CoordinateConverter.isValidTile(nx, ny)) continue;
                if (visited[nx][ny]) continue;
                int t = tileMap[nx][ny];
                if (t == TutorialIslandMap.WATER || t == TutorialIslandMap.WALL) continue;
                visited[nx][ny] = true;
                queue.add(new int[]{nx, ny});
            }
        }
        return false;
    }

    private List<int[]> findPath(int fromX, int fromY, int toX, int toY) {
        if (!CoordinateConverter.isValidTile(fromX, fromY) || !CoordinateConverter.isValidTile(toX, toY)) {
            return List.of();
        }
        if (!isWalkableClientTile(toX, toY)) {
            return List.of();
        }
        if (fromX == toX && fromY == toY) {
            return List.of();
        }

        int width = TutorialIslandMap.WIDTH;
        int height = TutorialIslandMap.HEIGHT;
        boolean[][] visited = new boolean[width][height];
        int[][] prevX = new int[width][height];
        int[][] prevY = new int[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                prevX[x][y] = -1;
                prevY[x][y] = -1;
            }
        }

        Deque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{fromX, fromY});
        visited[fromX][fromY] = true;

        int[] dx = {0, 1, 0, -1, 1, 1, -1, -1};
        int[] dy = {1, 0, -1, 0, 1, -1, 1, -1};
        boolean found = false;

        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            if (cur[0] == toX && cur[1] == toY) {
                found = true;
                break;
            }
            for (int i = 0; i < 8; i++) {
                int nx = cur[0] + dx[i];
                int ny = cur[1] + dy[i];
                if (!CoordinateConverter.isValidTile(nx, ny)) continue;
                if (visited[nx][ny]) continue;
                if (!isWalkableClientTile(nx, ny)) continue;

                visited[nx][ny] = true;
                prevX[nx][ny] = cur[0];
                prevY[nx][ny] = cur[1];
                queue.add(new int[]{nx, ny});
            }
        }

        if (!found) {
            return List.of();
        }

        Deque<int[]> reversed = new ArrayDeque<>();
        int cx = toX;
        int cy = toY;
        while (!(cx == fromX && cy == fromY)) {
            reversed.push(new int[]{cx, cy});
            int px = prevX[cx][cy];
            int py = prevY[cx][cy];
            if (px < 0 || py < 0) {
                return List.of();
            }
            cx = px;
            cy = py;
        }

        return new ArrayList<>(reversed);
    }

    private int[] closestAdjacentTile(int px, int py, int nx, int ny) {
        int bx = nx, by = ny - 1;
        double best = Double.MAX_VALUE;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int tx = nx + dx, ty = ny + dy;
                if (!CoordinateConverter.isValidTile(tx, ty)) continue;
                if (!isWalkableClientTile(tx, ty)) continue;
                if (isNpcOccupying(tx, ty, pendingNpcId)) continue;
                double d = Math.hypot(px - tx, py - ty);
                if (d < best) { best = d; bx = tx; by = ty; }
            }
        }
        return new int[]{bx, by};
    }

    private boolean isWalkableClientTile(int x, int y) {
        int tile = tileMap[x][y];
        return tile != TutorialIslandMap.WATER && tile != TutorialIslandMap.WALL;
    }

    private boolean isNpcOccupying(int x, int y, int exceptNpcId) {
        ClientPacketHandler h = handler();
        if (h == null) return false;
        for (Map.Entry<Integer, int[]> e : h.getEntityPositions().entrySet()) {
            int id = e.getKey();
            if (id == exceptNpcId || h.isPlayer(id)) continue;
            int[] pos = e.getValue();
            if (pos[0] == x && pos[1] == y) return true;
        }
        return false;
    }

    /**
     * Returns the server-authoritative tile position of an NPC from the
     * ClientPacketHandler's live entity map.  Returns null if unknown.
     */
    private int[] npcLogicalPosition(int npcId) {
        ClientPacketHandler h = handler();
        if (h == null) return null;
        return h.getEntityPosition(npcId);
    }

    // -----------------------------------------------------------------------
    // Context menu
    // -----------------------------------------------------------------------

    private List<ContextMenu.MenuItem> generateContextMenu(int tileX, int tileY) {
        List<ContextMenu.MenuItem> opts = new ArrayList<>();
        if (!CoordinateConverter.isValidTile(tileX, tileY)) return opts;

        opts.add(new ContextMenu.MenuItem("Walk here", "walk", new int[]{tileX, tileY}));

        // Ground items at this tile
        for (Map.Entry<Integer, int[]> entry : groundItemsOnMap.entrySet()) {
            int[] data = entry.getValue();  // {itemId, qty, x, y}
            if (data[2] == tileX && data[3] == tileY) {
                String name = groundItemNamesMap.getOrDefault(entry.getKey(), "Item");
                opts.add(new ContextMenu.MenuItem("Take " + name, "take", entry.getKey()));
            }
        }

        // Check all server-tracked NPCs — show options for whichever NPC the
        // player right-clicked (their current rendered tile, rounded).
        // OSRS format: verb in white, NPC name in yellow (#ffff00), level appended.
        ClientPacketHandler h = handler();
        if (h != null) {
            for (Map.Entry<Integer, int[]> entry : h.getEntityPositions().entrySet()) {
                int id = entry.getKey();
                if (h.isPlayer(id)) {
                    // Other player — add Trade/Follow options if standing on this tile
                    if (localPlayerId >= 0 && id == localPlayerId) continue;
                    float[] vis = npcVisual.get(id);
                    if (vis == null) continue;
                    int vx = (int) Math.round(vis[0]);
                    int vy = (int) Math.round(vis[1]);
                    if (tileX != vx || tileY != vy) continue;
                    String pName = h.getEntityName(id);
                    if (pName == null || pName.isEmpty()) pName = "Player";
                    String yellow = "[#ffff00]" + pName + "[]";
                    opts.add(new ContextMenu.MenuItem(
                        "Trade with " + yellow, "trade_player", id));
                    opts.add(new ContextMenu.MenuItem(
                        "Follow " + yellow, "follow_player", id));
                    continue;
                }
                // Use visual (rounded) position so click target matches the sprite
                float[] vis = npcVisual.get(id);
                if (vis == null) continue;
                int vx = (int) Math.round(vis[0]);
                int vy = (int) Math.round(vis[1]);
                if (tileX == vx && tileY == vy) {
                    String rawName = h.getEntityName(id);
                    if (rawName == null || rawName.isEmpty()) rawName = "NPC " + id;
                    int level = h.getEntityCombatLevel(id);

                    // Yellow name with level suffix — OSRS style
                    String yellowName = "[#ffff00]" + rawName + "[]";
                    String levelSuffix = level > 0 ? " (level-" + level + ")" : "";
                    boolean isTreeResource = "Tree".equalsIgnoreCase(rawName);
                    boolean isFishingSpot = "Fishing spot".equalsIgnoreCase(rawName);
                    boolean isFire = "Fire".equalsIgnoreCase(rawName);

                    if (level > 0) {
                        // Combat NPC: Attack is the primary option (top)
                        opts.add(new ContextMenu.MenuItem(
                            "Attack " + yellowName + levelSuffix, "attack", id));
                    }
                    if (isTreeResource) {
                        opts.add(new ContextMenu.MenuItem(
                            "Chop down " + yellowName, "chop", id));
                    } else if (isFishingSpot) {
                        opts.add(new ContextMenu.MenuItem(
                            "Net " + yellowName, "fish", id));
                    } else if (isFire) {
                        opts.add(new ContextMenu.MenuItem(
                            "Cook-at " + yellowName, "cook_at", id));
                    } else {
                        opts.add(new ContextMenu.MenuItem(
                            "Talk-to " + yellowName, "talk", id));
                    }
                    opts.add(new ContextMenu.MenuItem(
                        "Examine " + yellowName, "examine_npc", id));
                }
            }
        }
        return opts;
    }

    private void handleContextMenuAction(ContextMenu.MenuItem item) {
        LOG.info("Menu action: {}", item.label);
        switch (item.action) {
            case "walk"   -> { int[] t = (int[]) item.target; walkTo(t[0], t[1]); }
            case "attack" -> startApproach((Integer) item.target, "attack");
            case "talk"   -> startApproach((Integer) item.target, "talk");
            case "chop"   -> startApproach((Integer) item.target, "chop");
            case "fish"   -> startApproach((Integer) item.target, "fish");
            case "cook_at" -> startApproach((Integer) item.target, "cook_at");
            case "trade_player"  -> LOG.info("Trade not yet implemented for player {}", item.target);
            case "follow_player" -> LOG.info("Follow not yet implemented for player {}", item.target);
            case "take"   -> startGroundItemApproach((Integer) item.target);
            case "inv_eat"   -> { if (nettyClient != null) nettyClient.sendUseItem((Integer) item.target, "eat"); }
            case "inv_wield" -> { if (nettyClient != null) nettyClient.sendUseItem((Integer) item.target, "wield"); }
            case "inv_bury"  -> { if (nettyClient != null) nettyClient.sendUseItem((Integer) item.target, "bury"); pickupAnimationTimer = PICKUP_ANIM_DURATION; }
            case "inv_use" -> {
                selectedInventorySlot = (Integer) item.target;
                sidePanel.setSelectedInventorySlot(selectedInventorySlot);
            }
            case "inv_drop"  -> { if (nettyClient != null) nettyClient.sendDropItem((Integer) item.target); }
            case "inv_examine" -> LOG.info("Examine: {}", item.label);
            case "examine_npc" -> requestNpcExamine((Integer) item.target);
        }
    }

    /** Request server-authoritative NPC examine text. */
    private void requestNpcExamine(int npcId) {
        if (nettyClient != null) {
            try {
                nettyClient.sendExamineNpc(npcId);
                return;
            } catch (LinkageError e) {
                // Compatibility guard: if client runtime is out of sync with shared proto,
                // do not hard-crash the game loop on Examine.
                LOG.error("ExamineNpc packet type missing at runtime; falling back to local examine text", e);
            }
        }

        // Offline fallback keeps UI usable if networking is unavailable.
        ClientPacketHandler h = handler();
        String name = (h != null) ? h.getEntityName(npcId) : "creature";
        chatBox.addSystemMessage("It's a " + (name == null || name.isEmpty() ? "creature" : name) + ".");
    }

    /**
     * Handles a left-click on a world tile by executing the primary context action.
     * Returns true if a primary action was found and initiated (suppress plain walk).
     * Returns false if no entity/item at tile — caller should plain-walk.
     */
    private boolean handleWorldLeftClick(int tileX, int tileY) {
        ClientPacketHandler h = handler();
        if (h == null) return false;

        Integer groundItemId = h.getGroundItemAt(tileX, tileY);
        if (groundItemId != null) {
            startGroundItemApproach(groundItemId);
            return true;
        }

        Integer npcId = h.getNpcAt(tileX, tileY);
        if (npcId != null) {
            boolean isHostile = h.isNpcHostile(npcId);
            if (isHostile) {
                startApproach(npcId, "attack");
            } else {
                startApproach(npcId, "talk");
            }
            return true;
        }

        Integer resourceNpcId = h.getResourceNpcAt(tileX, tileY);
        if (resourceNpcId != null) {
            String primarySkill = h.getResourcePrimarySkill(resourceNpcId);
            if (primarySkill != null) {
                startApproach(resourceNpcId, primarySkill);
                return true;
            }
        }

        return false;
    }

    // -----------------------------------------------------------------------
    // Overhead chat text (world space)
    // -----------------------------------------------------------------------

    /**
     * Tick and render all active overhead chat texts.
     *
     * OSRS spec:
     *   - Yellow text by default
     *   - Black drop shadow (1px offset, no outline box)
     *   - No background — text floats above the entity's head
     *   - Centered horizontally above the entity
     *   - Visible for 3 seconds (150 × 20ms client ticks), then removed
     */
    private void renderOverheadText(float delta) {
        if (overheadTexts.isEmpty()) return;

        ClientPacketHandler h = handler();

        // Expire old entries
        overheadTexts.values().removeIf(ot -> {
            ot.timer -= delta;
            return ot.timer <= 0;
        });

        if (overheadTexts.isEmpty()) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        for (Map.Entry<Integer, OverheadText> entry : overheadTexts.entrySet()) {
            int      entityId = entry.getKey();
            OverheadText ot   = entry.getValue();

            // Determine world tile of this entity
            float tx, ty;
            if (entityId == localPlayerId) {
                tx = visualX; ty = visualY;
            } else {
                float[] vis = npcVisual.get(entityId);
                if (vis != null) {
                    tx = vis[0]; ty = vis[1];
                } else if (h != null) {
                    int[] pos = h.getEntityPosition(entityId);
                    if (pos == null) continue;
                    tx = pos[0]; ty = pos[1];
                } else continue;
            }

            // Isometric screen coordinates — 50px above entity head
            float sx = (tx - ty) * 16f;
            float sy = (tx + ty) * 8f + 50f;

            // Alpha fades to 0 in the last 0.5 s
            float alpha = (ot.timer < 0.5f) ? ot.timer / 0.5f : 1f;

            // Centre the text
            com.badlogic.gdx.graphics.g2d.GlyphLayout gl =
                new com.badlogic.gdx.graphics.g2d.GlyphLayout(font, ot.text);
            float textX = sx - gl.width * 0.5f;
            float textY = sy;

            // Shadow (black, 1px down-right)
            font.setColor(0f, 0f, 0f, alpha * 0.85f);
            font.draw(batch, ot.text, textX + 1f, textY - 1f);

            // Main text — OSRS yellow
            font.setColor(COLOR_YELLOW.r, COLOR_YELLOW.g, COLOR_YELLOW.b, alpha);
            font.draw(batch, ot.text, textX, textY);
        }

        batch.end();
        font.setColor(COLOR_WHITE);
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    /**
     * Renders a persistent yellow nametag above each other connected player.
     * Called every frame in world-space projection.
     */
    private void renderOtherPlayerNametags() {
        ClientPacketHandler h = handler();
        if (h == null) return;

        boolean anyPlayer = false;
        for (Map.Entry<Integer, int[]> entry : h.getEntityPositions().entrySet()) {
            int id = entry.getKey();
            if (!h.isPlayer(id)) continue;
            if (localPlayerId >= 0 && id == localPlayerId) continue;
            float[] vis = npcVisual.get(id);
            if (vis == null) continue;
            String name = h.getEntityName(id);
            if (name == null || name.isEmpty()) continue;
            anyPlayer = true;
            break;
        }
        if (!anyPlayer) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        font.getData().setScale(FontManager.getScale(FontManager.FontContext.TOOLTIP));

        for (Map.Entry<Integer, int[]> entry : h.getEntityPositions().entrySet()) {
            int id = entry.getKey();
            if (!h.isPlayer(id)) continue;
            if (localPlayerId >= 0 && id == localPlayerId) continue;
            float[] vis = npcVisual.get(id);
            if (vis == null) continue;
            String name = h.getEntityName(id);
            if (name == null || name.isEmpty()) continue;

            float sx = (vis[0] - vis[1]) * 16f;
            float sy = (vis[0] + vis[1]) * 8f + 28f;   // 28px above player feet

            com.badlogic.gdx.graphics.g2d.GlyphLayout gl =
                new com.badlogic.gdx.graphics.g2d.GlyphLayout(font, name);

            // Shadow
            font.setColor(0f, 0f, 0f, 0.8f);
            font.draw(batch, name, sx - gl.width * 0.5f + 1f, sy - 1f);
            // Yellow name
            font.setColor(COLOR_YELLOW);
            font.draw(batch, name, sx - gl.width * 0.5f, sy);
        }

        font.getData().setScale(FontManager.getScale(FontManager.FontContext.BASE_UI));
        font.setColor(COLOR_WHITE);
        batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void renderClickMarkers() {
        if (clickMarkers.isEmpty()) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);

        for (ClickMarker m : clickMarkers) {
            if (m.expired()) continue;

            float alpha = m.alpha();
            float sx    = renderer.worldToScreenX(m.tileX, m.tileY);
            float sy    = renderer.worldToScreenY(m.tileX, m.tileY);
            float rot   = m.rotationRad();

            if (m.isAction) {
                shapeRenderer.setColor(1f, 0.18f, 0.18f, alpha); // OSRS red
            } else {
                shapeRenderer.setColor(1f, 1f, 0f, alpha);        // OSRS yellow
            }

            // 4-pointed star: 4 diamond arms, each a filled triangle
            float outerR = 9f;
            float innerR = 3.5f;
            float wingW  = 2.5f;

            for (int i = 0; i < 4; i++) {
                float arm  = rot + i * com.badlogic.gdx.math.MathUtils.PI / 2f;
                float perp = arm + com.badlogic.gdx.math.MathUtils.PI / 2f;

                float cosArm  = com.badlogic.gdx.math.MathUtils.cos(arm);
                float sinArm  = com.badlogic.gdx.math.MathUtils.sin(arm);
                float cosPerp = com.badlogic.gdx.math.MathUtils.cos(perp);
                float sinPerp = com.badlogic.gdx.math.MathUtils.sin(perp);

                // Tip of this arm
                float tipX = sx + cosArm * outerR;
                float tipY = sy + sinArm * outerR;

                // Two base vertices (at innerR, spread perpendicular by wingW)
                float b1x = sx + cosArm * innerR + cosPerp * wingW;
                float b1y = sy + sinArm * innerR + sinPerp * wingW;
                float b2x = sx + cosArm * innerR - cosPerp * wingW;
                float b2y = sy + sinArm * innerR - sinPerp * wingW;

                shapeRenderer.triangle(tipX, tipY, b1x, b1y, b2x, b2y);
            }
        }

        shapeRenderer.end();
    }

    // -----------------------------------------------------------------------
    // Screen-space rendering
    // -----------------------------------------------------------------------

    private void renderContextMenu() {
        List<ContextMenu.MenuItem> items = contextMenu.getItems();
        if (items.isEmpty()) return;

        int mx = contextMenu.getScreenX(), my = contextMenu.getScreenY();
        int n = items.size();
        int totalH = contextMenu.getTotalHeight();
        int w = contextMenu.getMenuWidth();
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
        int hovered = contextMenu.getHoveredItemIndex(mouseX, mouseY);
        int itemsBottomY = my + ContextMenu.V_PAD;
        int headerY = itemsBottomY + n * ContextMenu.ITEM_HEIGHT;

        shapeRenderer.setProjectionMatrix(screenProjection);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.07f, 0.07f, 0.12f, 0.95f);
        shapeRenderer.rect(mx, my, w, totalH);

        // Header strip
        shapeRenderer.setColor(0.19f, 0.17f, 0.12f, 0.98f);
        shapeRenderer.rect(mx + 1, headerY, w - 2, ContextMenu.HEADER_HEIGHT + ContextMenu.V_PAD - 1);

        for (int i = 0; i < n; i++) {
            if (i == hovered) {
                int rowY = itemsBottomY + (n - 1 - i) * ContextMenu.ITEM_HEIGHT;
                shapeRenderer.setColor(0.24f, 0.20f, 0.08f, 0.95f);
                shapeRenderer.rect(mx + 2, rowY, w - 4, ContextMenu.ITEM_HEIGHT);
            }
        }
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.85f, 0.75f, 0.1f, 1f);
        shapeRenderer.rect(mx, my, w, totalH);
        shapeRenderer.setColor(0.45f, 0.36f, 0.12f, 1f);
        shapeRenderer.line(mx + 1, headerY, mx + w - 2, headerY);
        shapeRenderer.end();

        screenBatch.setProjectionMatrix(screenProjection);
        screenBatch.begin();
        font.setColor(COLOR_GOLD);
        font.draw(screenBatch, "Choose Option", mx + ContextMenu.H_PAD, headerY + 14);
        for (int i = 0; i < n; i++) {
            font.setColor(i == hovered ? new Color(1f, 0.95f, 0.45f, 1f) : COLOR_WHITE);
            font.draw(screenBatch, items.get(i).label, mx + ContextMenu.H_PAD,
                itemsBottomY + (n - 1 - i) * ContextMenu.ITEM_HEIGHT + 14);
        }
        screenBatch.end();
        font.setColor(COLOR_WHITE);
    }

    private void renderLogoutMenu() {
        if (!logoutMenuVisible) return;
        int sw = Gdx.graphics.getWidth();
        int sh = Gdx.graphics.getHeight();

        // OSRS-style dark backdrop (matching login screen aesthetic)
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(screenProjection);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.08f, 0.06f, 0.08f, 0.85f);
        shapeRenderer.rect(0, 0, sw, sh);

        // Modal box - centred on full screen
        int mw = 220, mh = 100;
        int mx = (sw - mw) / 2;
        int my = (sh - mh) / 2;
        shapeRenderer.setColor(0.12f, 0.10f, 0.08f, 1f);
        shapeRenderer.rect(mx, my, mw, mh);

        // Double gold border (filled 1-2px strips for consistent thickness)
        // Inner border
        shapeRenderer.setColor(0.75f, 0.60f, 0.10f, 1f);
        shapeRenderer.rect(mx, my, mw, 1);
        shapeRenderer.rect(mx, my + mh - 1, mw, 1);
        shapeRenderer.rect(mx, my, 1, mh);
        shapeRenderer.rect(mx + mw - 1, my, 1, mh);
        // Outer border (2px offset)
        shapeRenderer.rect(mx - 2, my - 2, mw + 4, 1);
        shapeRenderer.rect(mx - 2, my + mh + 1, mw + 4, 1);
        shapeRenderer.rect(mx - 2, my - 2, 1, mh + 4);
        shapeRenderer.rect(mx + mw + 1, my - 2, 1, mh + 4);

        // Buttons - raised bevel style matching login screen
        int btnW = 90, btnH = 26;
        int logBtnX = mx + mw / 2 - btnW - 10;
        int canBtnX = mx + mw / 2 + 10;
        int btnY = my + 14;

        // Button backgrounds
        shapeRenderer.setColor(0.20f, 0.16f, 0.08f, 1f);
        shapeRenderer.rect(logBtnX, btnY, btnW, btnH);
        shapeRenderer.rect(canBtnX, btnY, btnW, btnH);

        // Gold button borders
        shapeRenderer.setColor(0.75f, 0.60f, 0.10f, 1f);
        shapeRenderer.rect(logBtnX, btnY, btnW, 1);
        shapeRenderer.rect(logBtnX, btnY + btnH - 1, btnW, 1);
        shapeRenderer.rect(logBtnX, btnY, 1, btnH);
        shapeRenderer.rect(logBtnX + btnW - 1, btnY, 1, btnH);

        shapeRenderer.rect(canBtnX, btnY, btnW, 1);
        shapeRenderer.rect(canBtnX, btnY + btnH - 1, btnW, 1);
        shapeRenderer.rect(canBtnX, btnY, 1, btnH);
        shapeRenderer.rect(canBtnX + btnW - 1, btnY, 1, btnH);

        // Raised bevel highlights/shadows
        shapeRenderer.setColor(0.60f, 0.52f, 0.30f, 1f); // top/left highlight
        shapeRenderer.rect(logBtnX + 1, btnY + btnH - 2, btnW - 2, 1);
        shapeRenderer.rect(logBtnX + 1, btnY + 1, 1, btnH - 2);
        shapeRenderer.rect(canBtnX + 1, btnY + btnH - 2, btnW - 2, 1);
        shapeRenderer.rect(canBtnX + 1, btnY + 1, 1, btnH - 2);

        shapeRenderer.setColor(0.08f, 0.04f, 0.04f, 0.45f); // bottom/right shadow
        shapeRenderer.rect(logBtnX + 1, btnY + 1, btnW - 2, 1);
        shapeRenderer.rect(logBtnX + btnW - 2, btnY + 1, 1, btnH - 2);
        shapeRenderer.rect(canBtnX + 1, btnY + 1, btnW - 2, 1);
        shapeRenderer.rect(canBtnX + btnW - 2, btnY + 1, 1, btnH - 2);

        // Focus outline for the logout button (matching login screen click-to-focus style)
        if (logoutMenuVisible && logoutRequested) {
            shapeRenderer.setColor(0.95f, 0.85f, 0.40f, 1f);
            shapeRenderer.rect(logBtnX - 3, btnY - 3, btnW + 6, 1);
            shapeRenderer.rect(logBtnX - 3, btnY + btnH + 2, btnW + 6, 1);
            shapeRenderer.rect(logBtnX - 3, btnY - 3, 1, btnH + 6);
            shapeRenderer.rect(logBtnX + btnW + 2, btnY - 3, 1, btnH + 6);
        }
        shapeRenderer.end();

        // Text - use the existing screenBatch + font setup
        screenBatch.setProjectionMatrix(screenProjection);
        screenBatch.begin();
        // Title - consistent with login screen
        font.getData().setScale(FontManager.getScale(FontManager.FontContext.SKILL));
        font.setColor(1f, 0.88f, 0.52f, 1f);
        font.draw(screenBatch, "Game Menu", mx + 68, my + mh - 10);

        // Button labels - consistent sizing with login screen
        font.getData().setScale(FontManager.getScale(FontManager.FontContext.BASE_UI));
        font.setColor(1f, 0.72f, 0.55f, 1f);
        font.draw(screenBatch, "Logout", logBtnX + 20, btnY + 16);
        font.setColor(1f, 0.72f, 0.55f, 1f);
        font.draw(screenBatch, "Cancel", canBtnX + 20, btnY + 16);
        font.getData().setScale(FontManager.getScale(FontManager.FontContext.BASE_UI));
        font.setColor(COLOR_WHITE);
        screenBatch.end();
    }

    private void handleLogoutMenuClick(int screenMx, int screenMy) {
        int sw = Gdx.graphics.getWidth();
        int sh = Gdx.graphics.getHeight();
        int mw = 220, mh = 100;
        int mx = (sw - mw) / 2;
        int my = (sh - mh) / 2;
        int btnW = 90, btnH = 26;
        int logBtnX = mx + mw / 2 - btnW - 10;
        int canBtnX = mx + mw / 2 + 10;
        int btnY = my + 14;

        if (screenMx >= logBtnX && screenMx < logBtnX + btnW
                && screenMy >= btnY && screenMy < btnY + btnH) {
            logoutMenuVisible = false;
            requestLogout();
        } else if (screenMx >= canBtnX && screenMx < canBtnX + btnW
                && screenMy >= btnY && screenMy < btnY + btnH) {
            logoutMenuVisible = false;
        } else if (screenMx < mx || screenMx > mx + mw
                || screenMy < my || screenMy > my + mh) {
            logoutMenuVisible = false; // click outside modal
        }
    }

    private void renderDialogueOverlay(int mouseX, int mouseY) {
        if (!dialogueUI.isVisible()) return;
        dialogueUI.render(shapeRenderer, screenBatch, font, screenProjection, mouseX, mouseY);
    }

    /**
     * Full-screen death overlay: dark translucent background + "Oh dear, you are dead!"
     * Auto-dismisses after DEATH_SCREEN_DURATION seconds; click to dismiss early.
     */
    private void renderDeathScreen(float delta) {
        deathScreenTimer = Math.max(0f, deathScreenTimer - delta);

        // Dismiss on any click
        if (Gdx.input.justTouched()) {
            deathScreenTimer = 0f;
            return;
        }

        int w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();

        // Dark overlay (fade alpha from 0.85 at start to 0.5 near end)
        float alpha = 0.75f;
        shapeRenderer.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, w, h));
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, alpha);
        shapeRenderer.rect(0, 0, w, h);
        shapeRenderer.end();

        // Red border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.rect(8, 8, w - 16, h - 16);
        shapeRenderer.end();

        screenBatch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, w, h));
        screenBatch.begin();

        // Title
        font.setColor(Color.RED);
        font.getData().setScale(FontManager.getScale(FontManager.FontContext.BASE_UI) * 2.5f);
        String title = "Oh dear, you are dead!";
        font.draw(screenBatch, title, w / 2f - 180, h / 2f + 60);

        // Subtitle
        font.setColor(COLOR_WHITE);
        font.getData().setScale(FontManager.getScale(FontManager.FontContext.BASE_UI) * 1.2f);
        font.draw(screenBatch, "You have been teleported back to Lumbridge.", w / 2f - 200, h / 2f + 10);

        // Countdown
        font.setColor(new Color(0.8f, 0.8f, 0.8f, 1f));
        font.getData().setScale(FontManager.getScale(FontManager.FontContext.BASE_UI));
        int secs = (int) Math.ceil(deathScreenTimer);
        font.draw(screenBatch, "Respawning in " + secs + "s  (click to continue)", w / 2f - 140, h / 2f - 30);

        font.getData().setScale(FontManager.getScale(FontManager.FontContext.BASE_UI));
        font.setColor(COLOR_WHITE);
        screenBatch.end();
    }

    /**
     * Renders a RuneLite-style "Opponent Information" panel in the top-left when
     * the player has an active combat target.  Shows NPC name, combat level, and HP bar.
     */
    private void renderOpponentInfo() {
        ClientPacketHandler h = handler();
        if (h == null) return;

        String npcName   = h.getEntityName(combatTargetId);
        int combatLevel  = h.getEntityCombatLevel(combatTargetId);
        int[] hp         = h.getEntityHealth(combatTargetId);
        int curHp        = hp[0];
        int maxHp        = Math.max(hp[1], 1);

        // If the entity disappeared from the server, clear the target
        if (npcName.isEmpty() && h.getEntityPosition(combatTargetId) == null) {
            combatTargetId = -1;
            return;
        }
        if (npcName.isEmpty()) npcName = "NPC " + combatTargetId;

        // Panel dimensions and position (top-left, below the player HP bar with gap)
        int panelX = 10, panelY = Gdx.graphics.getHeight() - 105;
        int panelW = 190, panelH = 65;
        int barW   = panelW - 16;
        int barH   = 10;

        shapeRenderer.setProjectionMatrix(screenProjection);

        // Panel background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.08f, 0.08f, 0.08f, 0.88f);
        shapeRenderer.rect(panelX, panelY, panelW, panelH);
        shapeRenderer.end();

        // Panel border (gold)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.75f, 0.60f, 0.10f, 1f);
        shapeRenderer.rect(panelX, panelY, panelW, panelH);
        shapeRenderer.end();

        // HP bar background (dark red)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.35f, 0f, 0f, 1f);
        shapeRenderer.rect(panelX + 8, panelY + 8, barW, barH);
        // HP bar fill — green → yellow → red based on percentage
        float ratio = (float) curHp / maxHp;
        if (ratio > 0.5f)
            shapeRenderer.setColor(0.1f, 0.8f, 0.1f, 1f);   // green
        else if (ratio > 0.25f)
            shapeRenderer.setColor(0.9f, 0.8f, 0.0f, 1f);   // yellow
        else
            shapeRenderer.setColor(0.9f, 0.1f, 0.1f, 1f);   // red
        shapeRenderer.rect(panelX + 8, panelY + 8, barW * ratio, barH);
        shapeRenderer.end();

        // Text
        screenBatch.setProjectionMatrix(screenProjection);
        screenBatch.begin();
        font.getData().setScale(FontManager.getScale(FontManager.FontContext.BASE_UI));
        font.setColor(COLOR_GOLD);  // gold NPC name
        font.draw(screenBatch, npcName, panelX + 8, panelY + panelH - 6);
        font.setColor(0.75f, 0.75f, 0.75f, 1f);  // grey subtitle
        font.draw(screenBatch,
            String.format("Level: %d   HP: %d / %d", combatLevel, curHp, maxHp),
            panelX + 8, panelY + 22);
        screenBatch.end();
        font.setColor(COLOR_WHITE);
    }

    private void renderHUD() {
        int w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();

        // Orb geometry -- vertical column, top-left, clear of the minimap (top-right)
        final int ORB_R  = 22;
        final int ORB_CX = 35;
        final int HP_CY  = h - 50;
        final int PR_CY  = h - 115;
        final int RN_CY  = h - 180;

        // Ratios clamped [0,1]
        float hpRatio = playerMaxHealth  > 0 ? Math.min(1f, (float) playerHealth    / playerMaxHealth)  : 0f;
        float prRatio = playerMaxPrayer  > 0 ? Math.min(1f, (float) playerPrayer    / playerMaxPrayer)  : 0f;
        float rnRatio = Math.min(1f, playerRunEnergy / 100f);

        shapeRenderer.setProjectionMatrix(screenProjection);

        // -- Pass 1: dark background circles --
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.10f, 0.06f, 0.06f, 0.92f);
        shapeRenderer.circle(ORB_CX, HP_CY, ORB_R);
        shapeRenderer.setColor(0.06f, 0.06f, 0.14f, 0.92f);
        shapeRenderer.circle(ORB_CX, PR_CY, ORB_R);
        shapeRenderer.setColor(0.07f, 0.10f, 0.04f, 0.92f);
        shapeRenderer.circle(ORB_CX, RN_CY, ORB_R);
        shapeRenderer.end();

        // -- Pass 2: colored fill -- arc sector, clock-sweep from 12 o'clock --
        // arc(x, y, radius, startDeg, sweepDeg, segments)
        // startDeg = 90 - ratio*360  (adjusts start so the filled sector
        //            sweeps clockwise from the top as the ratio grows)
        // sweepDeg = ratio * 360     (counter-clockwise, positive)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        if (hpRatio > 0.01f) {
            shapeRenderer.setColor(0.75f, 0.10f, 0.10f, 1f);
            shapeRenderer.arc(ORB_CX, HP_CY, ORB_R - 3,
                90f - hpRatio * 360f, hpRatio * 360f, 32);
        }
        if (prRatio > 0.01f) {
            shapeRenderer.setColor(0.18f, 0.42f, 0.90f, 1f);
            shapeRenderer.arc(ORB_CX, PR_CY, ORB_R - 3,
                90f - prRatio * 360f, prRatio * 360f, 32);
        }
        if (rnRatio > 0.01f) {
            // Bright yellow when run is active (OSRS run orb lights up yellow)
            if (isRunning) {
                shapeRenderer.setColor(1.00f, 0.85f, 0.00f, 1f);
            } else {
                shapeRenderer.setColor(0.60f, 0.90f, 0.18f, 1f);
            }
            shapeRenderer.arc(ORB_CX, RN_CY, ORB_R - 3,
                90f - rnRatio * 360f, rnRatio * 360f, 32);
        }
        shapeRenderer.end();

        // -- Pass 3: bright outline rings --
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.90f, 0.38f, 0.38f, 1f);
        shapeRenderer.circle(ORB_CX, HP_CY, ORB_R);
        shapeRenderer.setColor(0.42f, 0.62f, 1.00f, 1f);
        shapeRenderer.circle(ORB_CX, PR_CY, ORB_R);
        if (isRunning) {
            // Outer gold glow ring + bright yellow inner ring when run is active
            shapeRenderer.setColor(1.00f, 0.70f, 0.00f, 1f);
            shapeRenderer.circle(ORB_CX, RN_CY, ORB_R + 3);
            shapeRenderer.setColor(1.00f, 0.95f, 0.20f, 1f);
        } else {
            shapeRenderer.setColor(0.72f, 1.00f, 0.42f, 1f);
        }
        shapeRenderer.circle(ORB_CX, RN_CY, ORB_R);
        shapeRenderer.end();

        // -- Pass 4: numbers and labels --
        screenBatch.setProjectionMatrix(screenProjection);
        screenBatch.begin();

        font.getData().setScale(FontManager.getScale(FontManager.FontContext.SKILL));

        // HP value centered in orb
        GlyphLayout gl = new GlyphLayout(font, String.valueOf(playerHealth));
        font.setColor(1.00f, 0.82f, 0.82f, 1f);
        font.draw(screenBatch, gl, ORB_CX - gl.width / 2f, HP_CY + gl.height / 2f);

        // Prayer value
        gl.setText(font, String.valueOf(playerPrayer));
        font.setColor(0.72f, 0.85f, 1.00f, 1f);
        font.draw(screenBatch, gl, ORB_CX - gl.width / 2f, PR_CY + gl.height / 2f);

        // Run energy value
        gl.setText(font, String.valueOf(playerRunEnergy));
        if (isRunning) {
            font.setColor(1.00f, 0.95f, 0.20f, 1f); // bright yellow text when active
        } else {
            font.setColor(0.82f, 1.00f, 0.68f, 1f);
        }
        font.draw(screenBatch, gl, ORB_CX - gl.width / 2f, RN_CY + gl.height / 2f);

        // Small labels below each orb
        font.getData().setScale(FontManager.getScale(FontManager.FontContext.SMALL_LABEL));
        font.setColor(0.52f, 0.52f, 0.52f, 0.85f);
        GlyphLayout lbl = new GlyphLayout(font, "HP");
        font.draw(screenBatch, lbl, ORB_CX - lbl.width / 2f, HP_CY - ORB_R + 4);
        lbl.setText(font, "Pray");
        font.draw(screenBatch, lbl, ORB_CX - lbl.width / 2f, PR_CY - ORB_R + 4);
        lbl.setText(font, "Run");
        font.draw(screenBatch, lbl, ORB_CX - lbl.width / 2f, RN_CY - ORB_R + 4);

        // Coordinates debug text -- bottom-right, unchanged
        font.getData().setScale(FontManager.getScale(FontManager.FontContext.BASE_UI));
        font.setColor(0.6f, 0.6f, 0.6f, 0.8f);
        font.draw(screenBatch, String.format("(%d,%d)", playerX, playerY), w - 70, 15);

        screenBatch.end();
        // Always reset font to defaults after HUD draw
        font.setColor(COLOR_WHITE);
        font.getData().setScale(FontManager.getScale(FontManager.FontContext.BASE_UI));
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @Override
    public void dispose() {
        if (nettyClient != null)
            try { nettyClient.disconnect(); } catch (Exception e) { LOG.error("disconnect", e); }
        if (batch        != null) batch.dispose();
        if (screenBatch  != null) screenBatch.dispose();
        if (shapeRenderer!= null) shapeRenderer.dispose();
        // FontManager owns shared font lifecycle.
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth  = width;
        camera.viewportHeight = height;
        camera.update();
        screenProjection = new Matrix4().setToOrtho2D(0, 0, width, height);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private ClientPacketHandler handler() {
        return nettyClient != null ? nettyClient.getHandler() : null;
    }
}
