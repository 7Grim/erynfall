package com.osrs.client;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
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
import com.osrs.client.renderer.SpriteSheet;
import com.osrs.client.world.MapLoader;
import com.osrs.client.ui.AdminToolsPopup;
import com.osrs.client.ui.ChatBox;
import com.osrs.client.ui.CombatUI;
import com.osrs.client.ui.ContextMenu;
import com.osrs.client.ui.DialogueUI;
import com.osrs.client.ui.FontManager;
import com.osrs.client.ui.BankUI;
import com.osrs.client.ui.LevelUpOverlay;
import com.osrs.client.ui.SkillGuidePopup;
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
    private static final Color COLOR_RED = new Color(1.0f, 0.0f, 0.0f, 1f);
    private static final Color COLOR_GOLD = FontManager.TEXT_GOLD;

    /** Displayed in the top-right HUD. Update this string each release. */
    private static final String GAME_VERSION = "Alpha 0.0.1";

    /** OSRS walk speed: 1 tile per 0.6 s. */
    private static final float TILES_PER_SECOND = 1.0f / 0.6f;
    private static final int ADMIN_BUTTON_X = 10;
    private static final int ADMIN_BUTTON_Y_FROM_TOP = 44;
    private static final int ADMIN_BUTTON_W = 76;
    private static final int ADMIN_BUTTON_H = 24;

    /** Chebyshev distance to activate an NPC interaction. */
    private static final int INTERACT_RANGE = 1;

    // Credentials passed from LoginScreen (null = dev bypass, will use defaults)
    private final ErynfallGame game;
    private final String loginEmail;
    private final String loginPassword;
    private boolean logoutRequested = false;

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
    /** Null when sprites.atlas has not been packed yet — falls back to ShapeRenderer. */
    private SpriteSheet      spriteSheet;
    private BitmapFont       font;
    private Matrix4          screenProjection;
    private final GlyphLayout gl = new GlyphLayout();
    private final GlyphLayout npcTagLayout = new GlyphLayout();

    // -----------------------------------------------------------------------
    // Game objects
    // -----------------------------------------------------------------------
    private NettyClient  nettyClient;
    private ContextMenu  contextMenu;
    private CombatUI     combatUI;
    private SidePanel    sidePanel;
    private DialogueUI   dialogueUI;
    private BankUI       bankUI;
    private ChatBox      chatBox;
    private MiniMap      miniMap;
    private XpDropOverlay xpDropOverlay;
    private LevelUpOverlay levelUpOverlay;
    private SkillGuidePopup skillGuidePopup;
    private AdminToolsPopup adminToolsPopup;
    private boolean loggedAdminButtonVisible = false;
    private int[][]      tileMap;
    private MapLoader    mapLoader;

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
    private final java.util.Set<Integer> localActivePrayers = new java.util.HashSet<>();
    private int playerRunEnergy = 100; // 0–100; full energy on login
    private int playerSpecialAttack = 100; // 0–100; always full until spec system is wired
    private boolean isRunning    = false;
    private float   runRestoreAcc = 0f; // accumulates delta time for energy restore
    private int attackLevel = 1, strengthLevel = 1, defenceLevel = 1;

    // Friends list state
    private boolean friendsListVisible = false;
    private String friendsListText = "";
    private final List<String> friendNamesList = new ArrayList<>();
    private final java.util.Set<Long> friendIds = new java.util.HashSet<>();
    private final Map<Long, String> friendDisplayNames = new HashMap<>();
    private final java.util.LinkedHashMap<Long, String> pendingFriendActionFeedbackBySequence = new java.util.LinkedHashMap<>();
    private static final int MAX_PENDING_FRIEND_FEEDBACK = 16;

    // Camera zoom state
    private float currentZoom = 1.0f;  // 1.0 = 100% zoom (OSRS default)
    private float targetZoom = 1.0f;    // Target zoom for smooth interpolation
    private static final float ZOOM_MIN = 0.5f;   // Zoomed out (2x further view distance)
    private static final float ZOOM_DEFAULT = 1.0f; // Default zoom (OSRS standard)
    private static final float ZOOM_MAX = 2.0f;   // Zoomed in (2x closer view)
    private int pendingScrollAmount = 0;

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

    // Attack animation state
    private float attackAnimTimer = 0f;
    private static final float ATTACK_ANIM_DURATION = 0.6f; // 1 OSRS tick
    private static final int PRAYER_ID_PROTECT_FROM_MAGIC = 15;
    private static final int PRAYER_ID_PROTECT_FROM_MISSILES = 16;
    private static final int PRAYER_ID_PROTECT_FROM_MELEE = 17;
    private String currentAttackPose = "idle";

    public enum PendingAction {
        IDLE,
        ATTACK,
        CHOP,
        FISH,
        SWORD,
        BLOCK,
        PICKUP,
        BURY,
        LIGHT_FIRE
    }

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
    // Projectile system (Phase 2 ranged, Phase 3 magic visuals)
    // -----------------------------------------------------------------------
    /** Arrow travel speed in tiles per second. */
    private static final float ARROW_SPEED_TILES_PER_SEC = 15f;
    /** Magic bolt travel speed in tiles per second (faster than arrows). */
    private static final float SPELL_SPEED_TILES_PER_SEC = 22f;
    /** Peak arc height in isometric camera units. */
    private static final float ARROW_ARC_HEIGHT = 24f;
    /** Lower arc for magic (straighter trajectory). */
    private static final float SPELL_ARC_HEIGHT = 10f;

    private static final class ActiveProjectile {
        final float srcTileX, srcTileY;
        final float dstTileX, dstTileY;
        final float duration;
        float elapsed;
        // Hitsplat data applied when projectile lands
        final int targetTileX, targetTileY;
        final int damage;
        final boolean hit;
        final String chatMessage;  // null = no message
        /** projectile_type from CombatHit: 1=arrow, 3=wind, 4=water, 5=fire, 6=earth */
        final int projectileType;

        ActiveProjectile(float srcX, float srcY, float dstX, float dstY, float duration,
                         int targetTileX, int targetTileY, int damage, boolean hit,
                         String chatMessage, int projectileType) {
            this.srcTileX      = srcX;   this.srcTileY      = srcY;
            this.dstTileX      = dstX;   this.dstTileY      = dstY;
            this.duration      = duration;
            this.elapsed       = 0f;
            this.targetTileX   = targetTileX;
            this.targetTileY   = targetTileY;
            this.damage        = damage;
            this.hit           = hit;
            this.chatMessage   = chatMessage;
            this.projectileType = projectileType;
        }
    }

    private final java.util.List<ActiveProjectile> activeProjectiles = new java.util.ArrayList<>();

    // -----------------------------------------------------------------------
    // Ground item pickup approach
    // -----------------------------------------------------------------------
    /** Ground item ID the player is walking toward to pick up; -1 if none. */
    private int    pendingGroundItemId = -1;
    /** World tile the item is on (cached so we can still pathfind if map updates). */
    private int    pendingGroundItemX  = -1, pendingGroundItemY = -1;
    private int selectedInventorySlot = -1;  // use-mode: slot waiting for "use on" target
    private int inventoryMouseDownSlot = -1; // tracks which slot was pressed down
    private int bankInventoryMouseDownSlot = -1;
    private int bankInventoryDragSlot = -1;
    private int bankInventoryDragStartX = 0;
    private int bankInventoryDragStartY = 0;
    private int bankInventoryDragMouseX = 0;
    private int bankInventoryDragMouseY = 0;
    private boolean bankInventoryDragging = false;
    private static final int BANK_INV_DRAG_THRESHOLD = 6;
    private int bankMouseDownSlot = -1;
    private int bankDragSlot = -1;
    private int bankDragStartX = 0;
    private int bankDragStartY = 0;
    private int bankDragMouseX = 0;
    private int bankDragMouseY = 0;
    private boolean bankDragging = false;
    private static final int BANK_DRAG_THRESHOLD = 6;

    // -----------------------------------------------------------------------
    // Pickup animation
    // -----------------------------------------------------------------------
    /** Seconds remaining in the pickup (kneel-down) animation; 0 = not playing. */
    private float pickupAnimationTimer = 0f;
    /** OSRS pickup animation: 3 OSRS ticks = 1.8 s real time. */
    private static final float PICKUP_ANIM_DURATION = 1.8f;

    // Skilling animation state — set by server ACTIVE/STOPPED signals, not by client click
    /** True while the server has confirmed woodcutting is active. Drives looping chop animation. */
    private boolean isWoodcuttingActive = false;
    /** True while the server has confirmed mining is active. Drives looping mine animation. */
    private boolean isMiningActive = false;

    // Firemaking animation
    /** Seconds remaining in the fire animation; 0 = not playing. */
    private float firemakerAnimTimer = 0f;
    /** Accumulates time for sin-based flicker. */
    private float firemakerFlicker   = 0f;

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

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                int mx  = Gdx.input.getX();
                int smy = Gdx.graphics.getHeight() - Gdx.input.getY(); // screen-space Y
                if (skillGuidePopup != null && skillGuidePopup.isVisible()) {
                    skillGuidePopup.handleScroll(amountY);
                    return true;
                }
                ClientPacketHandler h = handler();
                boolean bankOpen = h != null && h.isBankOpen();
                if (bankOpen && bankUI != null && bankUI.isOver(mx, smy)) {
                    bankUI.handleScroll(Math.round(amountY), h.getBankSlots());
                } else if (smy < ChatBox.TOTAL_H && mx >= 0 && mx < ChatBox.BOX_W) {
                    chatBox.handleScroll(Math.round(amountY));
                } else if (sidePanel.isOverPanel(mx, smy) && sidePanel.isFriendsTabActive()) {
                    sidePanel.scrollFriendsList(Math.round(amountY));
                } else if (sidePanel.isOverPanel(mx, smy) && sidePanel.isQuestsTabActive()) {
                    sidePanel.scrollQuestList(Math.round(amountY));
                } else {
                    pendingScrollAmount += Math.round(amountY);
                }
                return true;
            }
        });

        FontManager.initialize();
        font = FontManager.regular();
        font.setColor(COLOR_WHITE);
        font.getData().markupEnabled = true; // enables [#rrggbb] color tags in strings

        screenProjection = new Matrix4().setToOrtho2D(
            0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        renderer   = new IsometricRenderer(camera, batch, shapeRenderer);
        spriteSheet = SpriteSheet.load();
        renderer.setSpriteSheet(spriteSheet);
        mapLoader  = MapLoader.load();
        tileMap    = mapLoader.getLayout();
        contextMenu = new ContextMenu();
        combatUI   = new CombatUI();
        sidePanel  = new SidePanel();
        dialogueUI = new DialogueUI();
        bankUI = new BankUI();
        miniMap = new MiniMap();
        chatBox        = new ChatBox();
        xpDropOverlay  = new XpDropOverlay();
        levelUpOverlay = new LevelUpOverlay();
        skillGuidePopup = new SkillGuidePopup();
        adminToolsPopup = new AdminToolsPopup();

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
        processCombatFollow();
        processGroundItemApproach();
        updateMovement(delta);
        updateRunEnergy(delta);
        updateNpcVisuals(delta);
        updateClickMarkers(delta);
        if (isWoodcuttingActive) {
            // Loop the chop animation for as long as the server says we're actively woodcutting.
            // Re-trigger when the 0.6s window expires so it plays every OSRS tick.
            if (attackAnimTimer == 0f) triggerAttackPose("chop");
        }
        if (isMiningActive) {
            // Loop the mine animation for as long as the server says we're actively mining.
            if (attackAnimTimer == 0f) triggerAttackPose("mine");
        }
        if (attackAnimTimer > 0f) {
            attackAnimTimer = Math.max(0f, attackAnimTimer - delta);
            if (attackAnimTimer == 0f && !isWoodcuttingActive && !isMiningActive) currentAttackPose = "idle";
        }

        // Mouse scroll wheel zoom controls - OSRS-style
        if (pendingScrollAmount != 0) {
            targetZoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX,
                targetZoom + pendingScrollAmount * 0.1f));
            pendingScrollAmount = 0;
        }

        // Middle mouse button resets zoom to OSRS default
        if (Gdx.input.isButtonJustPressed(Input.Buttons.MIDDLE)) {
            targetZoom = ZOOM_DEFAULT;
        }

        // F5: hot-reload sprite atlas (run mvn generate-resources -pl client first)
        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) {
            if (spriteSheet != null) spriteSheet.dispose();
            spriteSheet = SpriteSheet.load();
            renderer.setSpriteSheet(spriteSheet);
            LOG.info("Sprite atlas reloaded");
        }

        updateCameraZoom(delta);
        if (pickupAnimationTimer > 0) pickupAnimationTimer = Math.max(0f, pickupAnimationTimer - delta);

        // Camera always follows the player's interpolated visual position
        camera.position.set(
            renderer.worldToScreenX(visualX, visualY),
            renderer.worldToScreenY(visualX, visualY),
            0
        );
        camera.update();

        // --- World ---
        renderer.renderWorld(tileMap, visualX, visualY);

        // Ground items — rendered before NPCs and player
        for (Map.Entry<Integer, int[]> entry : groundItemsOnMap.entrySet()) {
            int[] data = entry.getValue();  // {itemId, qty, x, y}
            renderer.renderGroundItem(data[2], data[3], data[0], data[1]);
        }
        renderGroundItemLabels();

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
        String playerPose = attackAnimTimer > 0f ? currentAttackPose : null;
        renderer.renderPlayer(visualX, visualY, pickupAnimationTimer > 0, playerPose);

        // Firemaking animation — fire on the player's tile
        if (firemakerAnimTimer > 0) {
            firemakerAnimTimer -= delta;
            firemakerFlicker   += delta * 8f;
            renderFireAnimation(visualX, visualY);
        }

        // Projectiles (ranged) + Hitsplats
        updateProjectiles(delta);
        renderProjectiles();
        combatUI.update(delta);
        combatUI.render(shapeRenderer, batch, font, camera);

        // Overhead chat text (world space — same projection as hitsplats)
        chatBox.update(delta);
        xpDropOverlay.update(delta);
        levelUpOverlay.update(delta);
        renderOtherPlayerNametags();
        renderNpcNametags();
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
        skillGuidePopup.render(shapeRenderer, screenBatch, font, w, h, screenProjection);
        renderAdminToolsButton(shapeRenderer, screenBatch, font, w, h, screenProjection, mouseScreenX, mouseScreenY);
        adminToolsPopup.render(shapeRenderer, screenBatch, font, w, h, screenProjection, handler());
        xpDropOverlay.render(shapeRenderer, screenBatch, font, w, h, screenProjection,
            sidePanel.getPanelX(), SidePanel.TOTAL_H + SidePanel.MARGIN);
        if (handler != null && handler.isBankOpen()) {
            bankUI.render(shapeRenderer, screenBatch, font, w, h, screenProjection,
                mouseScreenX, mouseScreenY, handler.getBankCapacity(), handler.getBankSlots(), handler,
                bankInventoryDragSlot, bankDragSlot,
                bankDragging ? bankDragMouseX : bankInventoryDragMouseX,
                bankDragging ? bankDragMouseY : bankInventoryDragMouseY);
        }
        if (contextMenu.isVisible()) renderContextMenu();
        if (deathScreenTimer > 0) renderDeathScreen(delta);

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    /**
     * Update camera zoom with smooth interpolation.
     * Gradually moves currentZoom toward targetZoom.
     */
    private void updateCameraZoom(float delta) {
        currentZoom = targetZoom;
        camera.zoom = currentZoom;
        camera.update();
    }

    private boolean isInUiArea(int mouseX, int mouseY) {
        if (mouseY < ChatBox.TOTAL_H && mouseX >= 0 && mouseX < ChatBox.BOX_W) return true;
        if (isAdminToolsButtonVisible() && isAdminToolsButtonHit(mouseX, mouseY, Gdx.graphics.getHeight())) return true;
        if (sidePanel != null && sidePanel.isOverPanel(mouseX, mouseY)) return true;
        if (dialogueUI != null && dialogueUI.isVisible() && dialogueUI.isOverDialogue(mouseX, mouseY)) return true;
        if (adminToolsPopup != null && adminToolsPopup.isVisible()) return true;
        if (skillGuidePopup != null && skillGuidePopup.isVisible()) return true;
        if (contextMenu != null && contextMenu.isVisible()) return true;
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        if (Math.hypot(mouseX - MiniMap.getCenterX(w), mouseY - MiniMap.getCenterY(h)) <= MiniMap.RADIUS + 2) return true;
        int miniLeftX = MiniMap.getLeftX(w);
        int orbCx = miniLeftX - 8 - 22;
        if (Math.hypot(mouseX - orbCx, mouseY - (h - 44)) <= 22) return true;
        if (Math.hypot(mouseX - orbCx, mouseY - (h - 92)) <= 22) return true;
        return false;
    }

    private boolean isAdminToolsButtonVisible() {
        ClientPacketHandler h = handler();
        return h != null && h.isAdminToolsEnabled();
    }

    private boolean isAdminToolsButtonHit(int mouseX, int mouseY, int screenH) {
        int x = ADMIN_BUTTON_X;
        int y = screenH - ADMIN_BUTTON_Y_FROM_TOP;
        return mouseX >= x && mouseX <= x + ADMIN_BUTTON_W && mouseY >= y && mouseY <= y + ADMIN_BUTTON_H;
    }

    private void renderAdminToolsButton(ShapeRenderer shapeRenderer,
                                        SpriteBatch batch,
                                        BitmapFont font,
                                        int screenW,
                                        int screenH,
                                        Matrix4 projection,
                                        int mouseX,
                                        int mouseY) {
        if (!isAdminToolsButtonVisible()) {
            loggedAdminButtonVisible = false;
            return;
        }
        if (!loggedAdminButtonVisible) {
            LOG.info("Admin tools button visible for current player");
            loggedAdminButtonVisible = true;
        }
        int x = ADMIN_BUTTON_X;
        int y = screenH - ADMIN_BUTTON_Y_FROM_TOP;
        boolean hover = isAdminToolsButtonHit(mouseX, mouseY, screenH);

        shapeRenderer.setProjectionMatrix(projection);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(hover ? 0.74f : 0.64f, hover ? 0.56f : 0.48f, hover ? 0.28f : 0.24f, 0.95f);
        shapeRenderer.rect(x, y, ADMIN_BUTTON_W, ADMIN_BUTTON_H);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.24f, 0.16f, 0.06f, 1f);
        shapeRenderer.rect(x, y, ADMIN_BUTTON_W, ADMIN_BUTTON_H);
        shapeRenderer.end();

        batch.setProjectionMatrix(projection);
        batch.begin();
        font.getData().setScale(0.62f);
        font.setColor(0.16f, 0.10f, 0.03f, 1f);
        font.draw(batch, "Admin", x + 18, y + 17);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    // -----------------------------------------------------------------------
    // Server events
    // -----------------------------------------------------------------------

    private void processServerEvents() {
        ClientPacketHandler h = handler();
        if (h == null) return;

        // Cache local player ID for overhead text lookup
        if (localPlayerId < 0) localPlayerId = h.getMyPlayerId();

        // Clear the pending bank approach only when the server confirms the bank is open.
        // Do NOT clear on timer-expiry while the bank is still closed — that would prevent
        // processApproach() from auto-retrying when the first request was rejected due to
        // a one-tick movement-lag false rejection.
        if (h.isBankOpen() && "bank".equals(pendingAction)) {
            clearPendingAction();
        }

        if (h.consumeAdminTeleportApplied()) {
            int tx = h.getAdminTeleportX();
            int ty = h.getAdminTeleportY();
            playerX = tx;
            playerY = ty;
            visualX = tx;
            visualY = ty;
            walkDestX = -1;
            walkDestY = -1;
            walkPath.clear();
            lastStepSentX = Integer.MIN_VALUE;
            lastStepSentY = Integer.MIN_VALUE;
            clearPendingAction();
            combatTargetId = -1;
        }

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
        sidePanel.setSelectedSpellId(h.getSelectedSpellId());
        if (localPlayerId >= 0) {
            String pname = h.getEntityName(localPlayerId);
            if (pname != null && !pname.isEmpty()) sidePanel.setPlayerName(pname);
        }

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

        for (ClientPacketHandler.FriendsListEvent event : h.drainFriendsListUpdates()) {
            friendsListVisible = true;
            friendNamesList.clear();
            friendIds.clear();
            friendDisplayNames.clear();

            List<SidePanel.FriendEntryView> views = new ArrayList<>();
            for (ClientPacketHandler.FriendsListEvent.Entry entry : event.entries) {
                friendNamesList.add(entry.name);
                friendIds.add(entry.playerId);
                friendDisplayNames.put(entry.playerId, entry.name);
                views.add(new SidePanel.FriendEntryView(entry.playerId, entry.name, entry.online));
            }
            sidePanel.setFriendsList(views);
            friendsListText = "Friends: " + String.join(", ", friendNamesList);
        }

        for (ClientPacketHandler.FriendActionResultEvent result : h.drainFriendActionResults()) {
            String queuedFeedback = pendingFriendActionFeedbackBySequence.remove(result.sequence);
            if (!result.success && result.error != null && !result.error.isEmpty()) {
                chatBox.addSystemMessage(result.error);
            } else if (result.success) {
                chatBox.addSystemMessage(queuedFeedback != null ? queuedFeedback : "Friends list updated.");
            }
        }

        // Process NPC deaths — remove visuals for despawned NPCs
        for (int deadId : h.drainDespawnedNpcs()) {
            npcVisual.remove(deadId);
            if (combatTargetId == deadId) {
                combatTargetId = -1;
            }
            LOG.debug("Client: removed visual for dead NPC {}", deadId);
        }

        for (ClientPacketHandler.SkillingStateEvent skillingEvent : h.drainSkillingStateEvents()) {
            boolean isWoodcutEvent = skillingEvent.type == NetworkProto.SkillingType.SKILLING_WOODCUTTING;
            if (isWoodcutEvent && skillingEvent.state == NetworkProto.SkillingState.SKILLING_STATE_ACTIVE) {
                isWoodcuttingActive = true;
                triggerAttackPose("chop");  // start animation on server confirmation, not on click
            }
            if (isWoodcutEvent && skillingEvent.state == NetworkProto.SkillingState.SKILLING_STATE_STOPPED) {
                isWoodcuttingActive = false;
                attackAnimTimer = 0f;
                if ("chop".equals(currentAttackPose)) {
                    currentAttackPose = "idle";
                }
            }

            boolean isMineEvent = skillingEvent.type == NetworkProto.SkillingType.SKILLING_MINING;
            if (isMineEvent && skillingEvent.state == NetworkProto.SkillingState.SKILLING_STATE_ACTIVE) {
                isMiningActive = true;
                triggerAttackPose("mine");
            }
            if (isMineEvent && skillingEvent.state == NetworkProto.SkillingState.SKILLING_STATE_STOPPED) {
                isMiningActive = false;
                attackAnimTimer = 0f;
                if ("mine".equals(currentAttackPose)) {
                    currentAttackPose = "idle";
                }
            }

            if (pendingAction == null) {
                continue;
            }
            NetworkProto.SkillingType pendingType = switch (pendingAction) {
                case "chop" -> NetworkProto.SkillingType.SKILLING_WOODCUTTING;
                case "fish_net", "fish_bait", "fish_lure", "fish_cage", "fish_harpoon" ->
                    NetworkProto.SkillingType.SKILLING_FISHING;
                case "cook_at" -> NetworkProto.SkillingType.SKILLING_COOKING;
                case "mine" -> NetworkProto.SkillingType.SKILLING_MINING;
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
                || "You mine some ore.".equals(msg)
                || msg.startsWith("You catch")
                || "You start cooking the shrimps.".equals(msg)
                || "You cook the shrimps.".equals(msg)
                || "You accidentally burn the shrimps.".equals(msg)
                || "This tree has been chopped down.".equals(msg)
                || "There are no fish here right now.".equals(msg)
                || "This rock is depleted.".equals(msg)
                || "You are too busy fighting.".equals(msg)
                || "Your inventory is too full to hold any more logs.".equals(msg)
                || "Your inventory is too full to hold any more ore.".equals(msg)) {
                clearPendingAction();
            }

            if ("You need an axe to chop this tree.".equals(msg)
                || msg.startsWith("You need")
                || "Your inventory is too full to hold any more fish.".equals(msg)
                || "You have no raw shrimps to cook.".equals(msg)
                || msg.startsWith("You need a Woodcutting level of")
                || msg.startsWith("You need a Fishing level of")
                || msg.startsWith("You need a Mining level of")) {
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
            boolean npcHitMe = (evt.targetId == h.getMyPlayerId());
            String chatMsg = npcHitMe
                ? (evt.hit ? String.format("You were hit for %d!", evt.damage) : "The attack missed you.")
                : (evt.hit ? String.format("You hit for %d!", evt.damage) : "Your attack missed.");

            if (evt.projectileType > 0) {
                // Ranged / magic: spawn a flying projectile; hitsplat deferred until it lands
                float dx = evt.targetX - evt.attackerX;
                float dy = evt.targetY - evt.attackerY;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                boolean isSpell = evt.projectileType >= 3;
                float speed = isSpell ? SPELL_SPEED_TILES_PER_SEC : ARROW_SPEED_TILES_PER_SEC;
                float duration = Math.max(0.08f, dist / speed);
                activeProjectiles.add(new ActiveProjectile(
                    evt.attackerX, evt.attackerY,
                    evt.targetX,   evt.targetY,
                    duration,
                    evt.targetX, evt.targetY, evt.damage, evt.hit, chatMsg,
                    evt.projectileType));
            } else {
                // Melee / instant: show hitsplat immediately
                combatUI.addDamageNumber(evt.targetX, evt.targetY, evt.damage, evt.hit);
                chatBox.addSystemMessage(chatMsg);
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
            if (playerPrayer == 0) localActivePrayers.clear();
        }
        // Keep SidePanel prayer tab in sync with latest values
        sidePanel.setPrayerState(playerPrayer, playerMaxPrayer, localActivePrayers);
        sidePanel.setHpState(playerHealth, playerMaxHealth);
        for (ClientPacketHandler.XpDropEvent xp : h.drainXpDrops()) {
            if (xp.skillIndex >= 0 && xp.skillIndex < skillNames.length) {
                xpDropOverlay.addDrop(xp.skillIndex, xp.xpGained);
            }
            if (xp.skillIndex == com.osrs.shared.Player.SKILL_FIREMAKING) {
                firemakerAnimTimer = 2.5f;
                firemakerFlicker   = 0f;
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

        if (adminToolsPopup != null && adminToolsPopup.isVisible()) {
            int mx = Gdx.input.getX();
            int screenMy = Gdx.graphics.getHeight() - Gdx.input.getY();
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                adminToolsPopup.dismiss();
                return;
            }

            if (nettyClient != null) {
                String query = adminToolsPopup.consumeItemSearchQueryIfDirty();
                if (query != null) {
                    nettyClient.sendAdminSearchItems(query);
                }
            }

            AdminToolsPopup.AdminSkillAction skillAction = adminToolsPopup.consumePendingSkillAction();
            if (skillAction != null && nettyClient != null) {
                if (skillAction.setLevel) {
                    nettyClient.sendAdminSetSkillLevel(skillAction.skillIdx, skillAction.levelValue);
                } else {
                    nettyClient.sendAdminAdjustSkillXp(skillAction.skillIdx, skillAction.xpDeltaWhole);
                }
            }

            AdminToolsPopup.AdminItemAction itemAction = adminToolsPopup.consumePendingItemAction();
            if (itemAction != null && nettyClient != null) {
                nettyClient.sendAdminGiveItem(
                    itemAction.itemId,
                    itemAction.quantity,
                    itemAction.toBank
                        ? NetworkProto.AdminItemDestination.ADMIN_ITEM_DESTINATION_BANK
                        : NetworkProto.AdminItemDestination.ADMIN_ITEM_DESTINATION_INVENTORY
                );
            }

            AdminToolsPopup.AdminTravelAction travelAction = adminToolsPopup.consumePendingTravelAction();
            if (travelAction != null && nettyClient != null) {
                nettyClient.sendAdminTeleport(travelAction.destination);
            }

            if (adminToolsPopup.isItemSearchFocused()) {
                boolean shiftDown = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                    || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
                if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)
                    && adminToolsPopup.handleItemSearchKey(Input.Keys.BACKSPACE, shiftDown)) {
                    return;
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                    && adminToolsPopup.handleItemSearchKey(Input.Keys.SPACE, shiftDown)) {
                    return;
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.MINUS)
                    && adminToolsPopup.handleItemSearchKey(Input.Keys.MINUS, shiftDown)) {
                    return;
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.APOSTROPHE)
                    && adminToolsPopup.handleItemSearchKey(Input.Keys.APOSTROPHE, shiftDown)) {
                    return;
                }
                for (int key = Input.Keys.A; key <= Input.Keys.Z; key++) {
                    if (Gdx.input.isKeyJustPressed(key) && adminToolsPopup.handleItemSearchKey(key, shiftDown)) {
                        return;
                    }
                }
                int[] numKeys = {
                    Input.Keys.NUM_0, Input.Keys.NUM_1, Input.Keys.NUM_2, Input.Keys.NUM_3, Input.Keys.NUM_4,
                    Input.Keys.NUM_5, Input.Keys.NUM_6, Input.Keys.NUM_7, Input.Keys.NUM_8, Input.Keys.NUM_9
                };
                for (int key : numKeys) {
                    if (Gdx.input.isKeyJustPressed(key) && adminToolsPopup.handleItemSearchKey(key, shiftDown)) {
                        return;
                    }
                }
            }

            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)
                || Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)
                || Gdx.input.isButtonJustPressed(Input.Buttons.MIDDLE)) {
                adminToolsPopup.handleClick(mx, screenMy);

                AdminToolsPopup.AdminSkillAction clickSkillAction = adminToolsPopup.consumePendingSkillAction();
                if (clickSkillAction != null && nettyClient != null) {
                    if (clickSkillAction.setLevel) {
                        nettyClient.sendAdminSetSkillLevel(clickSkillAction.skillIdx, clickSkillAction.levelValue);
                    } else {
                        nettyClient.sendAdminAdjustSkillXp(clickSkillAction.skillIdx, clickSkillAction.xpDeltaWhole);
                    }
                }

                AdminToolsPopup.AdminItemAction clickItemAction = adminToolsPopup.consumePendingItemAction();
                if (clickItemAction != null && nettyClient != null) {
                    nettyClient.sendAdminGiveItem(
                        clickItemAction.itemId,
                        clickItemAction.quantity,
                        clickItemAction.toBank
                            ? NetworkProto.AdminItemDestination.ADMIN_ITEM_DESTINATION_BANK
                            : NetworkProto.AdminItemDestination.ADMIN_ITEM_DESTINATION_INVENTORY
                    );
                }

                AdminToolsPopup.AdminTravelAction clickTravelAction = adminToolsPopup.consumePendingTravelAction();
                if (clickTravelAction != null && nettyClient != null) {
                    nettyClient.sendAdminTeleport(clickTravelAction.destination);
                }
            }

            return;
        }

        if (skillGuidePopup != null && skillGuidePopup.isVisible()) {
            int mx = Gdx.input.getX();
            int screenMy = Gdx.graphics.getHeight() - Gdx.input.getY();
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                skillGuidePopup.dismiss();
                return;
            }
            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)
                || Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)
                || Gdx.input.isButtonJustPressed(Input.Buttons.MIDDLE)) {
                skillGuidePopup.handleClick(mx, screenMy);
                return;
            }
        }

        ClientPacketHandler packetHandler = handler();
        if (packetHandler != null && packetHandler.isBankOpen()) {
            int mx = Gdx.input.getX();
            int screenMy = Gdx.graphics.getHeight() - Gdx.input.getY();
            if (bankMouseDownSlot >= 0 && Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                bankDragMouseX = mx;
                bankDragMouseY = screenMy;
                if (!bankDragging) {
                    int dx = Math.abs(mx - bankDragStartX);
                    int dy = Math.abs(screenMy - bankDragStartY);
                    if (dx >= BANK_DRAG_THRESHOLD || dy >= BANK_DRAG_THRESHOLD) {
                        bankDragging = true;
                        bankDragSlot = bankMouseDownSlot;
                    }
                }
            }
            if (!Gdx.input.isButtonPressed(Input.Buttons.LEFT) && bankMouseDownSlot >= 0) {
                int releaseCellSlot = bankUI.getBankCellSlotAt(mx, screenMy);
                int releaseOccupiedSlot = bankUI.getBankSlotAt(mx, screenMy, packetHandler.getBankSlots());
                int tabDropTarget = bankUI.getTabDropTarget(mx, screenMy, packetHandler.getBankSlots());
                if (bankDragging) {
                    if (tabDropTarget >= 0 && nettyClient != null) {
                        nettyClient.sendMoveBankItemToTab(bankMouseDownSlot, tabDropTarget);
                    } else if (bankUI.isAllTabSelected()
                            && releaseCellSlot >= 0
                            && releaseCellSlot != bankMouseDownSlot
                            && nettyClient != null) {
                        nettyClient.sendRearrangeBankSlots(bankMouseDownSlot, releaseCellSlot);
                    }
                } else {
                    if (releaseOccupiedSlot == bankMouseDownSlot && nettyClient != null) {
                        nettyClient.sendWithdrawBankItem(bankMouseDownSlot, bankUI.getSelectedAmount());
                    }
                }
                bankMouseDownSlot = -1;
                bankDragSlot = -1;
                bankDragging = false;
            }
            if (bankInventoryMouseDownSlot >= 0 && Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                bankInventoryDragMouseX = mx;
                bankInventoryDragMouseY = screenMy;
                if (!bankInventoryDragging) {
                    int dx = Math.abs(mx - bankInventoryDragStartX);
                    int dy = Math.abs(screenMy - bankInventoryDragStartY);
                    if (dx >= BANK_INV_DRAG_THRESHOLD || dy >= BANK_INV_DRAG_THRESHOLD) {
                        bankInventoryDragging = true;
                        bankInventoryDragSlot = bankInventoryMouseDownSlot;
                    }
                }
            }
            if (!Gdx.input.isButtonPressed(Input.Buttons.LEFT) && bankInventoryMouseDownSlot >= 0) {
                int releaseCellSlot = bankUI.getInventoryCellSlotAt(mx, screenMy);
                int releaseOccupiedSlot = bankUI.getInventorySlotAt(mx, screenMy, packetHandler);
                if (bankInventoryDragging) {
                    if (releaseCellSlot >= 0 && releaseCellSlot != bankInventoryMouseDownSlot && nettyClient != null) {
                        nettyClient.sendSwapInventorySlots(bankInventoryMouseDownSlot, releaseCellSlot);
                    }
                } else {
                    if (releaseOccupiedSlot == bankInventoryMouseDownSlot && nettyClient != null) {
                        nettyClient.sendDepositBankItem(bankInventoryMouseDownSlot, bankUI.getSelectedAmount());
                    }
                }
                bankInventoryMouseDownSlot = -1;
                bankInventoryDragSlot = -1;
                bankInventoryDragging = false;
            }
            if (bankUI.isSearchFocused()) {
                boolean shiftDown = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                    || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
                if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                    if (bankUI.handleSearchKey(Input.Keys.ESCAPE, shiftDown)) {
                        return;
                    }
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
                    if (bankUI.handleSearchKey(Input.Keys.BACKSPACE, shiftDown)) {
                        return;
                    }
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                    if (bankUI.handleSearchKey(Input.Keys.SPACE, shiftDown)) {
                        return;
                    }
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.MINUS)) {
                    if (bankUI.handleSearchKey(Input.Keys.MINUS, shiftDown)) {
                        return;
                    }
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.APOSTROPHE)) {
                    if (bankUI.handleSearchKey(Input.Keys.APOSTROPHE, shiftDown)) {
                        return;
                    }
                }
                for (int key = Input.Keys.A; key <= Input.Keys.Z; key++) {
                    if (Gdx.input.isKeyJustPressed(key) && bankUI.handleSearchKey(key, shiftDown)) {
                        return;
                    }
                }
                int[] numKeys = {
                    Input.Keys.NUM_0, Input.Keys.NUM_1, Input.Keys.NUM_2, Input.Keys.NUM_3, Input.Keys.NUM_4,
                    Input.Keys.NUM_5, Input.Keys.NUM_6, Input.Keys.NUM_7, Input.Keys.NUM_8, Input.Keys.NUM_9
                };
                for (int key : numKeys) {
                    if (Gdx.input.isKeyJustPressed(key) && bankUI.handleSearchKey(key, shiftDown)) {
                        return;
                    }
                }
            }
            handleBankInput();
            return;
        }
        bankInventoryMouseDownSlot = -1;
        bankInventoryDragSlot = -1;
        bankInventoryDragging = false;
        bankMouseDownSlot = -1;
        bankDragSlot = -1;
        bankDragging = false;
        if (bankUI != null) {
            bankUI.resetSelectedTab();
        }

        // ── Add Friend overlay: handle keys AND mouse clicks, then skip world input ──
        if (sidePanel.isAddFriendOverlayActive()) {
            // Keyboard
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                sidePanel.handleAddFriendKey(Input.Keys.ENTER);
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                sidePanel.handleAddFriendKey(Input.Keys.ESCAPE);
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
                sidePanel.handleAddFriendKey(Input.Keys.BACKSPACE);
            } else {
                for (int key = Input.Keys.A; key <= Input.Keys.Z; key++) {
                    if (Gdx.input.isKeyJustPressed(key)) {
                        char c = Input.Keys.toString(key).charAt(0);
                        if (!Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                         && !Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) {
                            c = Character.toLowerCase(c);
                        }
                        sidePanel.typeAddFriendChar(c);
                        break;
                    }
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) sidePanel.typeAddFriendChar(' ');
                if (Gdx.input.isKeyJustPressed(Input.Keys.MINUS)) sidePanel.typeAddFriendChar('-');
                if (Gdx.input.isKeyJustPressed(Input.Keys.APOSTROPHE)) sidePanel.typeAddFriendChar('\'');
                int[] numKeys = {Input.Keys.NUM_0,Input.Keys.NUM_1,Input.Keys.NUM_2,Input.Keys.NUM_3,
                                 Input.Keys.NUM_4,Input.Keys.NUM_5,Input.Keys.NUM_6,Input.Keys.NUM_7,
                                 Input.Keys.NUM_8,Input.Keys.NUM_9};
                for (int i = 0; i < numKeys.length; i++) {
                    if (Gdx.input.isKeyJustPressed(numKeys[i])) { sidePanel.typeAddFriendChar((char)('0'+i)); break; }
                }
            }
            // Mouse click — routes through the normal panel click path so Cancel works
            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                int ox = Gdx.input.getX();
                int oy = Gdx.graphics.getHeight() - Gdx.input.getY();
                sidePanel.handleLeftClick(ox, oy);
                String addFriendName = sidePanel.consumeAddFriendRequested();
                if (addFriendName != null && !addFriendName.isEmpty()) {
                    sendFriendActionWithFeedback(NetworkProto.FriendAction.Action.ADD, 0L, addFriendName);
                }
            }
            return; // skip all world/camera input while overlay is open
        }

        // ── Chat input ────────────────────────────────────────────────────────
        if (suppressInitialEnter) {
            if (!Gdx.input.isKeyPressed(Input.Keys.ENTER)) {
                suppressInitialEnter = false;
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            if (levelUpOverlay.handleEnter()) return;
            // Logout modal has priority focus: Enter confirms Logout.
            if (sidePanel.isLogoutTabActive()) {
                sidePanel.cancelLogout(); // dismiss the tab UI
                requestLogout();          // trigger directly — no flag polling needed
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
            // A left click outside the chat area deactivates chat and lets the click propagate
            // to normal mouse handlers below — do NOT return in that case.
            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                int mx2 = Gdx.input.getX();
                int screenMy2 = Gdx.graphics.getHeight() - Gdx.input.getY();
                boolean inChatArea = screenMy2 < ChatBox.TOTAL_H && mx2 >= 0 && mx2 < ChatBox.BOX_W;
                if (!inChatArea) {
                    chatBox.setActive(false);
                    // Fall through — do not return; let the LEFT click block below handle the click
                }
                // Click inside chat area: also fall through so the LEFT click handler can route it
            } else {
                // No mouse click this frame — handle keyboard input and absorb
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

        if (sidePanel.isInventoryTabActive() && sidePanel.isOverPanel(mx, screenMy) && !sidePanel.isInventoryDragging()) {
            sidePanel.setInventoryHoveredSlot(sidePanel.getInventorySlotAt(mx, screenMy));
        } else {
            sidePanel.setInventoryHoveredSlot(-1);
        }

        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            if (screenMy < ChatBox.TOTAL_H && mx >= 0 && mx < ChatBox.BOX_W) {
                if (chatBox.handleClick(mx, screenMy, true)) {
                    return;
                }
            }
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
            // Level-up overlay has highest click priority
            if (levelUpOverlay.isActive() && levelUpOverlay.handleClick(mx, screenMy)) return;

            if (isAdminToolsButtonVisible() && isAdminToolsButtonHit(mx, screenMy, h)) {
                adminToolsPopup.show();
                contextMenu.close();
                return;
            }

            // OSRS run: clicking the run energy orb toggles run on/off
            {
                int miniLeftX = MiniMap.getLeftX(w);
                int orbCx = miniLeftX - 8 - 22;
                if (Math.hypot(mx - orbCx, screenMy - (h - 44)) <= 22) {
                    isRunning = !isRunning;
                    if (playerRunEnergy <= 0) isRunning = false; // can't enable at 0 energy
                    return;
                }
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
                } else if (click == -50) {
                    if (nettyClient != null) nettyClient.sendSetAutoRetaliate(sidePanel.isAutoRetaliate());
                } else if (click <= -200) {
                    int prayerId = -(click + 200);
                    if (playerPrayer > 0) {
                        if (localActivePrayers.contains(prayerId)) localActivePrayers.remove(prayerId);
                        else localActivePrayers.add(prayerId);
                    }
                    if (nettyClient != null) nettyClient.sendTogglePrayer(prayerId);
                } else if (click <= -100) {
                    int equipSlot = -(click + 100);
                    if (nettyClient != null) nettyClient.sendUnequipItem(equipSlot);
                }
                int skillClickIdx = sidePanel.consumeSkillClickIdx();
                if (skillClickIdx >= 0) {
                    skillGuidePopup.show(skillClickIdx,
                        handler().getSkillLevel(skillClickIdx),
                        handler().getSkillTotalXp(skillClickIdx));
                }
                // Magic tab: spell selection
                int pendingSpell = sidePanel.consumePendingSpellSelected();
                if (pendingSpell != -2) {
                    // -1 = deselect, >=1 = spell ID
                    sidePanel.setSelectedSpellId(pendingSpell);
                    if (handler() != null) handler().setSelectedSpellId(pendingSpell);
                    if (nettyClient != null) nettyClient.sendSetSpell(pendingSpell);
                }
                long removeFriendId = sidePanel.consumeRemoveFriendRequestedId();
                if (removeFriendId > 0) {
                    sendFriendActionWithFeedback(
                        NetworkProto.FriendAction.Action.REMOVE,
                        removeFriendId,
                        friendDisplayNames.getOrDefault(removeFriendId, "")
                    );
                }
                String addFriendName = sidePanel.consumeAddFriendRequested();
                if (addFriendName != null && !addFriendName.isEmpty()) {
                    sendFriendActionWithFeedback(NetworkProto.FriendAction.Action.ADD, 0L, addFriendName);
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
            sidePanel.setInventoryHoveredSlot(-1);
            inventoryMouseDownSlot = -1;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (adminToolsPopup != null && adminToolsPopup.isVisible()) {
                adminToolsPopup.dismiss();
                return;
            }
            if (skillGuidePopup != null && skillGuidePopup.isVisible()) {
                skillGuidePopup.dismiss();
                return;
            }
            if (sidePanel.isLogoutTabActive()) {
                sidePanel.cancelLogout();
            } else {
                boolean dismissed = contextMenu.isVisible() || selectedInventorySlot >= 0;
                contextMenu.close();
                if (selectedInventorySlot >= 0) {
                    selectedInventorySlot = -1;
                    sidePanel.setSelectedInventorySlot(-1);
                }
                if (!dismissed) {
                    sidePanel.showLogoutTab();
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
                    } else if (click == -50) {
                        if (nettyClient != null) nettyClient.sendSetAutoRetaliate(sidePanel.isAutoRetaliate());
                    } else if (click <= -200) {
                        int prayerId = -(click + 200);
                        if (playerPrayer > 0) {
                            if (localActivePrayers.contains(prayerId)) localActivePrayers.remove(prayerId);
                            else localActivePrayers.add(prayerId);
                        }
                        if (nettyClient != null) nettyClient.sendTogglePrayer(prayerId);
                    } else if (click <= -100) {
                        int equipSlot = -(click + 100);
                        if (nettyClient != null) nettyClient.sendUnequipItem(equipSlot);
                    }
                    long removeFriendId = sidePanel.consumeRemoveFriendRequestedId();
                    if (removeFriendId > 0) {
                        sendFriendActionWithFeedback(
                            NetworkProto.FriendAction.Action.REMOVE,
                            removeFriendId,
                            friendDisplayNames.getOrDefault(removeFriendId, "")
                        );
                    }
                    String addFriendName2 = sidePanel.consumeAddFriendRequested();
                    if (addFriendName2 != null && !addFriendName2.isEmpty()) {
                        sendFriendActionWithFeedback(NetworkProto.FriendAction.Action.ADD, 0L, addFriendName2);
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

    private void handleBankInput() {
        ClientPacketHandler h = handler();
        if (h == null) {
            return;
        }
        int mx = Gdx.input.getX();
        int screenMy = Gdx.graphics.getHeight() - Gdx.input.getY();

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (bankUI.isSearchActive()) {
                bankUI.clearSearch();
                return;
            }
            if (bankUI.isSearchFocused()) {
                bankUI.unfocusSearch();
                return;
            }
            if (nettyClient != null) {
                nettyClient.sendCloseBankRequest();
            }
            return;
        }

        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            if (bankUI.isSearchBoxHit(mx, screenMy)) {
                contextMenu.close();
                return;
            }
            int bankSlot = bankUI.getBankSlotAt(mx, screenMy, h.getBankSlots());
            if (bankSlot >= 0) {
                showBankWithdrawContextMenu(bankSlot, mx, screenMy);
                return;
            }
            int inventorySlot = bankUI.getInventorySlotAt(mx, screenMy, h);
            if (inventorySlot >= 0) {
                showBankDepositContextMenu(inventorySlot, mx, screenMy);
                return;
            }
            contextMenu.close();
            return;
        }

        if (!Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            return;
        }

        if (contextMenu.isVisible()) {
            ContextMenu.MenuItem clicked = contextMenu.getClickedItem(mx, screenMy);
            if (clicked != null) {
                handleContextMenuAction(clicked);
            }
            contextMenu.close();
            return;
        }

        if (bankUI.isSearchBoxHit(mx, screenMy)) {
            bankUI.focusSearch();
            return;
        } else {
            bankUI.unfocusSearch();
        }

        if (bankUI.clickTab(mx, screenMy, h.getBankSlots())) {
            return;
        }

        if (bankUI.clickAmountButton(mx, screenMy)) {
            return;
        }

        if (bankUI != null && bankUI.isCloseButtonHit(mx, screenMy)) {
            if (nettyClient != null) {
                nettyClient.sendCloseBankRequest();
            }
            return;
        }

        int bankSlot = bankUI.getBankSlotAt(mx, screenMy, h.getBankSlots());
        if (bankSlot >= 0) {
            bankMouseDownSlot = bankSlot;
            bankDragging = false;
            bankDragSlot = -1;
            bankDragStartX = mx;
            bankDragStartY = screenMy;
            bankDragMouseX = mx;
            bankDragMouseY = screenMy;
            return;
        }

        int inventorySlot = bankUI.getInventorySlotAt(mx, screenMy, h);
        if (inventorySlot >= 0) {
            bankInventoryMouseDownSlot = inventorySlot;
            bankInventoryDragging = false;
            bankInventoryDragSlot = -1;
            bankInventoryDragStartX = mx;
            bankInventoryDragStartY = screenMy;
            bankInventoryDragMouseX = mx;
            bankInventoryDragMouseY = screenMy;
            return;
        }

        if (bankUI != null && !bankUI.isOver(mx, screenMy)) {
            if (nettyClient != null) {
                nettyClient.sendCloseBankRequest();
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

    private void showBankWithdrawContextMenu(int bankSlot, int mx, int my) {
        ClientPacketHandler h = handler();
        if (h == null) {
            return;
        }

        String name = "Item";
        for (ClientPacketHandler.BankSlotSnapshot slot : h.getBankSlots()) {
            if (slot.slot == bankSlot) {
                if (slot.itemName != null && !slot.itemName.isBlank()) {
                    name = slot.itemName;
                }
                break;
            }
        }

        List<ContextMenu.MenuItem> opts = new ArrayList<>();
        opts.add(new ContextMenu.MenuItem("Withdraw-1 " + name, "bank_withdraw_1", bankSlot));
        opts.add(new ContextMenu.MenuItem("Withdraw-5 " + name, "bank_withdraw_5", bankSlot));
        opts.add(new ContextMenu.MenuItem("Withdraw-10 " + name, "bank_withdraw_10", bankSlot));
        opts.add(new ContextMenu.MenuItem("Withdraw-All " + name, "bank_withdraw_all", bankSlot));
        contextMenu.open(mx, my, opts, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private void showBankDepositContextMenu(int inventorySlot, int mx, int my) {
        ClientPacketHandler h = handler();
        if (h == null) {
            return;
        }

        String name = h.getInventoryName(inventorySlot);
        if (name == null || name.isBlank()) {
            name = "Item";
        }

        List<ContextMenu.MenuItem> opts = new ArrayList<>();
        opts.add(new ContextMenu.MenuItem("Deposit-1 " + name, "bank_deposit_1", inventorySlot));
        opts.add(new ContextMenu.MenuItem("Deposit-5 " + name, "bank_deposit_5", inventorySlot));
        opts.add(new ContextMenu.MenuItem("Deposit-10 " + name, "bank_deposit_10", inventorySlot));
        opts.add(new ContextMenu.MenuItem("Deposit-All " + name, "bank_deposit_all", inventorySlot));
        contextMenu.open(mx, my, opts, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private void showInventoryContextMenu(int slot, int mx, int my) {
        String name = sidePanel.getInventoryItemName(slot);
        if (name == null || name.isEmpty()) name = "Item";
        int itemId = sidePanel.getInventoryItemId(slot);
        int itemFlags = sidePanel.getInventoryItemFlags(slot);

        List<ContextMenu.MenuItem> opts = new ArrayList<>();
        if ((itemFlags & 0x2) != 0) {
            // Consumable
            opts.add(new ContextMenu.MenuItem(ContextMenu.Action.EAT, name, slot));
        }
        if ((itemFlags & 0x1) != 0) {
            // Equipable
            opts.add(new ContextMenu.MenuItem(ContextMenu.Action.WIELD, name, slot));
        }
        if (itemId == 526) {
            opts.add(new ContextMenu.MenuItem(ContextMenu.Action.BURY, name, slot));
        }
        if (itemId == 590) {
            opts.add(new ContextMenu.MenuItem(ContextMenu.Action.USE, name, slot));
        }
        opts.add(new ContextMenu.MenuItem(ContextMenu.Action.DROP, name, slot));
        opts.add(new ContextMenu.MenuItem(ContextMenu.Action.EXAMINE_ITEM, name, slot));
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

        int range = getActionRange(action);
        if (isInRange(playerX, playerY, pos[0], pos[1], range)) {
            executePendingAction();
        } else {
            int[] dest = closestTileInRange(playerX, playerY, pos[0], pos[1], range);
            autoWalkTo(dest[0], dest[1]);
            LOG.info("Approaching NPC {} for '{}' (range={}) → walking to ({},{})",
                npcId, action, range, dest[0], dest[1]);
        }
    }

    /**
     * Every frame: check whether we've reached the NPC.
     * If the NPC has wandered since we started walking, re-route to its new
     * adjacent tile (but only when our current target is no longer optimal).
     */
    /**
     * When the player is in combat but has no pending approach, check whether they are still
     * in weapon range of the target. If the NPC has wandered out of range, re-queue an approach
     * so the server's range gate will pass on the next game-loop tick.
     */
    private void processCombatFollow() {
        if (combatTargetId < 0) return;        // not in combat
        if (pendingNpcId >= 0) return;         // already approaching something
        ClientPacketHandler h = handler();
        if (h == null) return;
        int[] pos = npcLogicalPosition(combatTargetId);
        if (pos == null) { combatTargetId = -1; return; }
        int range = getActionRange("attack");
        if (!isInRange(playerX, playerY, pos[0], pos[1], range)) {
            startApproach(combatTargetId, "attack");
        }
    }

    private void processApproach() {
        if (pendingNpcId < 0) return;
        if (pendingActionRetryTimer > 0f) {
            pendingActionRetryTimer = Math.max(0f, pendingActionRetryTimer - Gdx.graphics.getDeltaTime());
        }

        int[] pos = npcLogicalPosition(pendingNpcId);
        if (pos == null) { clearPendingAction(); return; }

        int range = getActionRange(pendingAction);
        if (isInRange(playerX, playerY, pos[0], pos[1], range)) {
            executePendingAction();
            return;
        }

        // Re-route if the NPC moved and our walk target is stale
        int[] best = closestTileInRange(playerX, playerY, pos[0], pos[1], range);
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
                triggerAttackPose("sword");
                combatTargetId = pendingNpcId;
                clearPendingAction();
            } else if ("talk".equals(pendingAction)) {
                nettyClient.sendTalkToNpc(pendingNpcId);
                // Keep pending action alive until server actually opens dialogue,
                // because a moving NPC can invalidate a single packet attempt.
                pendingActionRetryTimer = 0.25f;
            } else if ("chop".equals(pendingAction)) {
                nettyClient.sendStartSkilling(pendingNpcId, NetworkProto.SkillingType.SKILLING_WOODCUTTING);
                // Animation is triggered when server confirms SKILLING_STATE_ACTIVE, not here
                pendingActionRetryTimer = 0.25f;
            } else if ("fish_net".equals(pendingAction)
                || "fish_bait".equals(pendingAction)
                || "fish_lure".equals(pendingAction)
                || "fish_cage".equals(pendingAction)
                || "fish_harpoon".equals(pendingAction)) {
                NetworkProto.FishingActionType actionType = switch (pendingAction) {
                    case "fish_net" -> NetworkProto.FishingActionType.FISHING_ACTION_NET;
                    case "fish_bait" -> NetworkProto.FishingActionType.FISHING_ACTION_BAIT;
                    case "fish_lure" -> NetworkProto.FishingActionType.FISHING_ACTION_LURE;
                    case "fish_cage" -> NetworkProto.FishingActionType.FISHING_ACTION_CAGE;
                    case "fish_harpoon" -> NetworkProto.FishingActionType.FISHING_ACTION_HARPOON;
                    default -> NetworkProto.FishingActionType.FISHING_ACTION_NONE;
                };
                nettyClient.sendStartSkilling(pendingNpcId, NetworkProto.SkillingType.SKILLING_FISHING, actionType);
                triggerAttackPose("fish");
                pendingActionRetryTimer = 0.25f;
            } else if ("mine".equals(pendingAction)) {
                nettyClient.sendStartSkilling(pendingNpcId, NetworkProto.SkillingType.SKILLING_MINING);
                // Animation is triggered when server confirms SKILLING_STATE_ACTIVE, not here
                pendingActionRetryTimer = 0.25f;
            } else if ("cook_at".equals(pendingAction)) {
                nettyClient.sendStartSkilling(pendingNpcId, NetworkProto.SkillingType.SKILLING_COOKING);
                pendingActionRetryTimer = 0.25f;
            } else if ("bank".equals(pendingAction)) {
                nettyClient.sendOpenBankRequest(pendingNpcId);
                pendingActionRetryTimer = 0.25f;
            }
        }
    }

    private void triggerAttackPose(String pose) {
        currentAttackPose = pose;
        attackAnimTimer = ATTACK_ANIM_DURATION;
    }

    private void clearPendingAction() {
        pendingNpcId = -1; pendingAction = null;
        pendingWalkTargX = -1; pendingWalkTargY = -1;
        pendingActionRetryTimer = 0f;
        // isWoodcuttingActive is intentionally NOT cleared here — only the STOPPED
        // skilling event from the server should stop the animation loop.
    }

    private boolean isInRange(int px, int py, int nx, int ny, int range) {
        int chebyshev = Math.max(Math.abs(px - nx), Math.abs(py - ny));
        return chebyshev <= range && !(px == nx && py == ny);
    }

    /** Returns the effective interaction range for the given action. */
    private int getActionRange(String action) {
        if ("attack".equals(action)) {
            ClientPacketHandler h = handler();
            return (h != null) ? h.getPlayerAttackRange() : INTERACT_RANGE;
        }
        return INTERACT_RANGE;
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
     * Non-walkable tiles are derived from map.yaml walkability definitions.
     */
    private boolean canReachTile(int fromX, int fromY, int toX, int toY) {
        if (!CoordinateConverter.isValidTile(toX, toY)) return false;
        // Item tile itself must not be a hard blocker
        int destType = tileMap[toX][toY];
        if (!mapLoader.isWalkableTile(destType)) return false;

        if (fromX == toX && fromY == toY) return true;

        boolean[][] visited = new boolean[MapLoader.WIDTH][MapLoader.HEIGHT];
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
                if (!mapLoader.isWalkableTile(t)) continue;
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

        int width = MapLoader.WIDTH;
        int height = MapLoader.HEIGHT;
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

    /**
     * Returns the closest walkable tile to the player that is within {@code range}
     * Chebyshev tiles of (nx, ny) but not on the NPC itself.
     * Falls back to {@link #closestAdjacentTile} when no such tile exists.
     */
    private int[] closestTileInRange(int px, int py, int nx, int ny, int range) {
        if (range <= 1) return closestAdjacentTile(px, py, nx, ny);
        int[] best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                if (dx == 0 && dy == 0) continue;
                int tx = nx + dx, ty = ny + dy;
                int cheby = Math.max(Math.abs(dx), Math.abs(dy));
                if (cheby > range) continue;
                if (!CoordinateConverter.isValidTile(tx, ty)) continue;
                if (!isWalkableClientTile(tx, ty)) continue;
                double d = Math.hypot(px - tx, py - ty);
                if (d < bestDist) { bestDist = d; best = new int[]{tx, ty}; }
            }
        }
        if (best == null) return closestAdjacentTile(px, py, nx, ny);
        return best;
    }

    private boolean isWalkableClientTile(int x, int y) {
        return mapLoader.isWalkable(x, y);
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

        opts.add(new ContextMenu.MenuItem(ContextMenu.Action.WALK_HERE, null, new int[]{tileX, tileY}));

        // Ground items at this tile
        for (Map.Entry<Integer, int[]> entry : groundItemsOnMap.entrySet()) {
            int[] data = entry.getValue();  // {itemId, qty, x, y}
            if (data[2] == tileX && data[3] == tileY) {
                String name = groundItemNamesMap.getOrDefault(entry.getKey(), "Item");
                opts.add(new ContextMenu.MenuItem(ContextMenu.Action.TAKE, name, entry.getKey()));
                opts.add(new ContextMenu.MenuItem(ContextMenu.Action.EXAMINE_GROUND_ITEM, name, entry.getKey()));
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
                    boolean isFriend = friendIds.contains((long) id);
                    opts.add(new ContextMenu.MenuItem(ContextMenu.Action.TRADE, yellow, id));
                    opts.add(new ContextMenu.MenuItem(ContextMenu.Action.FOLLOW, yellow, id));
                    opts.add(new ContextMenu.MenuItem(ContextMenu.Action.CHALLENGE, yellow, id));
                    opts.add(new ContextMenu.MenuItem(
                        isFriend ? ContextMenu.Action.REMOVE_FRIEND : ContextMenu.Action.ADD_FRIEND,
                        yellow,
                        id));
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
                    boolean isTreeResource = "Oak Tree".equalsIgnoreCase(rawName)
                        || "Willow Tree".equalsIgnoreCase(rawName)
                        || "Maple Tree".equalsIgnoreCase(rawName)
                        || "Yew Tree".equalsIgnoreCase(rawName)
                        || "Magic Tree".equalsIgnoreCase(rawName);
                    boolean isMiningRock = "Copper Rock".equalsIgnoreCase(rawName)
                        || "Tin Rock".equalsIgnoreCase(rawName)
                        || "Iron Rock".equalsIgnoreCase(rawName)
                        || "Silver Rock".equalsIgnoreCase(rawName)
                        || "Coal Rock".equalsIgnoreCase(rawName)
                        || "Gold Rock".equalsIgnoreCase(rawName)
                        || "Mithril Rock".equalsIgnoreCase(rawName)
                        || "Adamantite Rock".equalsIgnoreCase(rawName)
                        || "Runite Rock".equalsIgnoreCase(rawName);
                    boolean isFishingSpot = "Fishing Spot".equalsIgnoreCase(rawName);
                    boolean isFire = "Cooking Fire".equalsIgnoreCase(rawName);
                    boolean isBanker = "Banker".equalsIgnoreCase(rawName);

                    if (level > 0) {
                        // Combat NPC: Attack is the primary option (top)
                        opts.add(new ContextMenu.MenuItem(
                            "Attack " + yellowName + levelSuffix, ContextMenu.Action.ATTACK.id, id));
                    }
                    if (isTreeResource) {
                        opts.add(new ContextMenu.MenuItem(ContextMenu.Action.CHOP, yellowName, id));
                    } else if (isMiningRock) {
                        opts.add(new ContextMenu.MenuItem(ContextMenu.Action.MINE, yellowName, id));
                    } else if (isFishingSpot) {
                        List<String> fishingActions = h.getFishingActions(id);
                        for (String fishingAction : fishingActions) {
                            if ("fish_net".equals(fishingAction)) {
                                opts.add(new ContextMenu.MenuItem(ContextMenu.Action.NET, yellowName, id));
                            } else if ("fish_bait".equals(fishingAction)) {
                                opts.add(new ContextMenu.MenuItem(ContextMenu.Action.BAIT, yellowName, id));
                            } else if ("fish_lure".equals(fishingAction)) {
                                opts.add(new ContextMenu.MenuItem(ContextMenu.Action.LURE, yellowName, id));
                            } else if ("fish_cage".equals(fishingAction)) {
                                opts.add(new ContextMenu.MenuItem(ContextMenu.Action.CAGE, yellowName, id));
                            } else if ("fish_harpoon".equals(fishingAction)) {
                                opts.add(new ContextMenu.MenuItem(ContextMenu.Action.HARPOON, yellowName, id));
                            }
                        }
                    } else if (isFire) {
                        opts.add(new ContextMenu.MenuItem(ContextMenu.Action.COOK_AT, yellowName, id));
                    } else if (isBanker) {
                        opts.add(new ContextMenu.MenuItem(ContextMenu.Action.BANK, yellowName, id));
                        opts.add(new ContextMenu.MenuItem(ContextMenu.Action.TALK_TO, yellowName, id));
                    } else {
                        opts.add(new ContextMenu.MenuItem(ContextMenu.Action.TALK_TO, yellowName, id));
                    }
                    opts.add(new ContextMenu.MenuItem(ContextMenu.Action.EXAMINE_NPC, yellowName, id));
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
            case "bank"   -> startApproach((Integer) item.target, "bank");
            case "chop"   -> startApproach((Integer) item.target, "chop");
            case "mine"   -> startApproach((Integer) item.target, "mine");
            case "fish_net" -> startApproach((Integer) item.target, "fish_net");
            case "fish_bait" -> startApproach((Integer) item.target, "fish_bait");
            case "fish_lure" -> startApproach((Integer) item.target, "fish_lure");
            case "fish_cage" -> startApproach((Integer) item.target, "fish_cage");
            case "fish_harpoon" -> startApproach((Integer) item.target, "fish_harpoon");
            case "cook_at" -> startApproach((Integer) item.target, "cook_at");
            case "trade_player"  -> LOG.info("Trade not yet implemented for player {}", item.target);
            case "follow_player" -> LOG.info("Follow not yet implemented for player {}", item.target);
            case "challenge_player" -> LOG.info("Challenge not yet implemented for player {}", item.target);
            case "friend_add" -> {
                Integer targetId = (Integer) item.target;
                if (targetId != null) {
                    ClientPacketHandler h = handler();
                    String targetName = h != null ? h.getEntityName(targetId) : "";
                    sendFriendActionWithFeedback(NetworkProto.FriendAction.Action.ADD, targetId, targetName);
                }
            }
            case "friend_remove" -> {
                Integer targetId = (Integer) item.target;
                if (targetId != null) {
                    ClientPacketHandler h = handler();
                    String targetName = h != null ? h.getEntityName(targetId) : "";
                    sendFriendActionWithFeedback(NetworkProto.FriendAction.Action.REMOVE, targetId, targetName);
                }
            }
            case "take"   -> startGroundItemApproach((Integer) item.target);
            case "examine_ground_item" -> {
                String name = groundItemNamesMap.get((Integer) item.target);
                if (name == null || name.isEmpty()) name = "item";
                chatBox.addSystemMessage("It's a " + name + ".");
            }
            case "inv_eat"   -> { if (nettyClient != null) nettyClient.sendUseItem((Integer) item.target, "eat"); }
            case "inv_wield" -> { if (nettyClient != null) nettyClient.sendUseItem((Integer) item.target, "wield"); }
            case "inv_bury"  -> { if (nettyClient != null) nettyClient.sendUseItem((Integer) item.target, "bury"); pickupAnimationTimer = PICKUP_ANIM_DURATION; }
            case "inv_use" -> {
                selectedInventorySlot = (Integer) item.target;
                sidePanel.setSelectedInventorySlot(selectedInventorySlot);
            }
            case "inv_drop"  -> { if (nettyClient != null) nettyClient.sendDropItem((Integer) item.target); }
            case "inv_examine" -> { if (nettyClient != null) nettyClient.sendExamineItem((Integer) item.target); }
            case "bank_withdraw_1" -> {
                if (nettyClient != null) nettyClient.sendWithdrawBankItem((Integer) item.target, 1);
            }
            case "bank_withdraw_5" -> {
                if (nettyClient != null) nettyClient.sendWithdrawBankItem((Integer) item.target, 5);
            }
            case "bank_withdraw_10" -> {
                if (nettyClient != null) nettyClient.sendWithdrawBankItem((Integer) item.target, 10);
            }
            case "bank_withdraw_all" -> {
                if (nettyClient != null) nettyClient.sendWithdrawBankItem((Integer) item.target, Integer.MAX_VALUE);
            }
            case "bank_deposit_1" -> {
                if (nettyClient != null) nettyClient.sendDepositBankItem((Integer) item.target, 1);
            }
            case "bank_deposit_5" -> {
                if (nettyClient != null) nettyClient.sendDepositBankItem((Integer) item.target, 5);
            }
            case "bank_deposit_10" -> {
                if (nettyClient != null) nettyClient.sendDepositBankItem((Integer) item.target, 10);
            }
            case "bank_deposit_all" -> {
                if (nettyClient != null) nettyClient.sendDepositBankItem((Integer) item.target, Integer.MAX_VALUE);
            }
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

    private void sendFriendActionWithFeedback(NetworkProto.FriendAction.Action action, long targetPlayerId, String targetName) {
        if (nettyClient == null || !nettyClient.isConnected()) {
            return;
        }

        String displayName = (targetName == null || targetName.isBlank())
            ? "Player " + targetPlayerId
            : targetName;
        String feedback = switch (action) {
            case ADD -> "Added " + displayName + " to your friends list.";
            case REMOVE -> "Removed " + displayName + " from your friends list.";
            case UNRECOGNIZED -> "Friends list updated.";
        };

        if (pendingFriendActionFeedbackBySequence.size() >= MAX_PENDING_FRIEND_FEEDBACK) {
            java.util.Iterator<Long> it = pendingFriendActionFeedbackBySequence.keySet().iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
        }
        long sequence = nettyClient.sendFriendAction(action, targetPlayerId, targetName);
        if (sequence > 0) {
            pendingFriendActionFeedbackBySequence.put(sequence, feedback);
        }
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
        boolean prayerIndicatorVisible = !localActivePrayers.isEmpty();
        if (overheadTexts.isEmpty() && !prayerIndicatorVisible) return;

        ClientPacketHandler h = handler();

        // Expire old entries
        if (!overheadTexts.isEmpty()) {
            overheadTexts.values().removeIf(ot -> {
                ot.timer -= delta;
                return ot.timer <= 0;
            });
        }

        if (overheadTexts.isEmpty() && !prayerIndicatorVisible) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        if (!overheadTexts.isEmpty()) {
            batch.setProjectionMatrix(camera.combined);
            batch.begin();
        }

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

        if (!overheadTexts.isEmpty()) {
            batch.end();
        }

        int overheadPrayerId = -1;
        if (localActivePrayers.contains(PRAYER_ID_PROTECT_FROM_MELEE)) {
            overheadPrayerId = PRAYER_ID_PROTECT_FROM_MELEE;
        } else if (localActivePrayers.contains(PRAYER_ID_PROTECT_FROM_MISSILES)) {
            overheadPrayerId = PRAYER_ID_PROTECT_FROM_MISSILES;
        } else if (localActivePrayers.contains(PRAYER_ID_PROTECT_FROM_MAGIC)) {
            overheadPrayerId = PRAYER_ID_PROTECT_FROM_MAGIC;
        }

        if (overheadPrayerId >= 0) {
            float iconSx = (visualX - visualY) * 16f;
            float iconSy = (visualX + visualY) * 8f + 65f;
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            if (overheadPrayerId == PRAYER_ID_PROTECT_FROM_MELEE) {
                shapeRenderer.setColor(0.2f, 0.5f, 1.0f, 1f);   // blue — melee
            } else if (overheadPrayerId == PRAYER_ID_PROTECT_FROM_MISSILES) {
                shapeRenderer.setColor(0.2f, 0.85f, 0.3f, 1f);  // green — ranged
            } else {
                shapeRenderer.setColor(0.85f, 0.2f, 0.2f, 1f);  // red — magic
            }
            shapeRenderer.circle(iconSx, iconSy, 4f);
            shapeRenderer.end();
        }

        font.setColor(COLOR_WHITE);
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void renderGroundItemLabels() {
        if (groundItemsOnMap.isEmpty()) return;

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        font.getData().setScale(FontManager.getScale(FontManager.FontContext.SMALL_LABEL));
        font.setColor(FontManager.TEXT_YELLOW);
        GlyphLayout gl = new GlyphLayout();

        for (Map.Entry<Integer, int[]> entry : groundItemsOnMap.entrySet()) {
            String itemName = groundItemNamesMap.get(entry.getKey());
            if (itemName == null || itemName.isEmpty()) continue;

            int[] data = entry.getValue(); // {itemId, qty, x, y}
            float sx = renderer.worldToScreenX(data[2], data[3]);
            float sy = renderer.worldToScreenY(data[2], data[3]);

            gl.setText(font, itemName);
            font.draw(batch, gl, sx - gl.width / 2f, sy + 18f);
        }

        batch.end();
        font.getData().setScale(FontManager.getScale(FontManager.FontContext.BASE_UI));
        font.setColor(COLOR_WHITE);
    }

    /**
     * Renders a persistent yellow nametag above each other connected player.
     * Called every frame in world-space projection.
     */
    private void renderOtherPlayerNametags() {
        ClientPacketHandler h = handler();
        if (h == null) return;

        boolean started = false;
        font.getData().setScale(FontManager.getScale(FontManager.FontContext.TOOLTIP));

        for (Map.Entry<Integer, int[]> entry : h.getEntityPositions().entrySet()) {
            int id = entry.getKey();
            if (!h.isPlayer(id)) continue;
            if (localPlayerId >= 0 && id == localPlayerId) continue;
            float[] vis = npcVisual.get(id);
            if (vis == null) continue;
            String name = h.getEntityName(id);
            if (name == null || name.isEmpty()) continue;

            if (!started) {
                Gdx.gl.glEnable(GL20.GL_BLEND);
                Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                batch.setProjectionMatrix(camera.combined);
                batch.begin();
                started = true;
            }

            float sx = (vis[0] - vis[1]) * 16f;
            float sy = (vis[0] + vis[1]) * 8f + 28f;   // 28px above player feet

            npcTagLayout.setText(font, name);

            // Shadow
            font.setColor(0f, 0f, 0f, 0.8f);
            font.draw(batch, name, sx - npcTagLayout.width * 0.5f + 1f, sy - 1f);
            // Yellow name
            font.setColor(COLOR_YELLOW);
            font.draw(batch, name, sx - npcTagLayout.width * 0.5f, sy);
        }

        font.getData().setScale(FontManager.getScale(FontManager.FontContext.BASE_UI));
        font.setColor(COLOR_WHITE);
        if (started) {
            batch.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
    }

    private void renderNpcNametags() {
        ClientPacketHandler h = handler();
        if (h == null) return;

        boolean started = false;
        font.getData().setScale(FontManager.getScale(FontManager.FontContext.TOOLTIP));

        for (Map.Entry<Integer, int[]> entry : h.getEntityPositions().entrySet()) {
            int id = entry.getKey();
            if (h.isPlayer(id)) continue;
            if (h.getResourcePrimarySkill(id) != null) continue;

            float[] vis = npcVisual.get(id);
            if (vis == null) continue;
            String name = h.getEntityName(id);
            if (name == null || name.isEmpty()) continue;

            if (!started) {
                Gdx.gl.glEnable(GL20.GL_BLEND);
                Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                batch.setProjectionMatrix(camera.combined);
                batch.begin();
                started = true;
            }

            boolean hostile = h.isNpcHostile(id);
            Color nameColor = hostile ? COLOR_RED : COLOR_YELLOW;
            float sx = (vis[0] - vis[1]) * 16f;
            float sy = (vis[0] + vis[1]) * 8f + 28f;

            npcTagLayout.setText(font, name);

            font.setColor(0f, 0f, 0f, 0.8f);
            font.draw(batch, name, sx - npcTagLayout.width * 0.5f + 1f, sy - 1f);
            font.setColor(nameColor);
            font.draw(batch, name, sx - npcTagLayout.width * 0.5f, sy);
        }

        font.getData().setScale(FontManager.getScale(FontManager.FontContext.BASE_UI));
        font.setColor(COLOR_WHITE);
        if (started) {
            batch.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
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

    // -----------------------------------------------------------------------
    // Projectile update + render
    // -----------------------------------------------------------------------

    private void updateProjectiles(float delta) {
        if (activeProjectiles.isEmpty()) return;
        java.util.Iterator<ActiveProjectile> it = activeProjectiles.iterator();
        while (it.hasNext()) {
            ActiveProjectile p = it.next();
            p.elapsed += delta;
            if (p.elapsed >= p.duration) {
                combatUI.addDamageNumber(p.targetTileX, p.targetTileY, p.damage, p.hit);
                if (p.chatMessage != null) chatBox.addSystemMessage(p.chatMessage);
                it.remove();
            }
        }
    }

    private void renderProjectiles() {
        if (activeProjectiles.isEmpty()) return;
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);
        for (ActiveProjectile p : activeProjectiles) {
            float progress = p.duration > 0f ? (p.elapsed / p.duration) : 1f;
            float tileX = p.srcTileX + (p.dstTileX - p.srcTileX) * progress;
            float tileY = p.srcTileY + (p.dstTileY - p.srcTileY) * progress;
            float sx = renderer.worldToScreenX(tileX, tileY);
            float sy = renderer.worldToScreenY(tileX, tileY);
            float arcH = (p.projectileType >= 3) ? SPELL_ARC_HEIGHT : ARROW_ARC_HEIGHT;
            sy += arcH * (float) Math.sin(Math.PI * progress);
            // Colour by projectile type
            switch (p.projectileType) {
                case 3  -> shapeRenderer.setColor(0.90f, 0.90f, 0.90f, 1f); // wind: white
                case 4  -> shapeRenderer.setColor(0.25f, 0.60f, 1.00f, 1f); // water: blue
                case 5  -> shapeRenderer.setColor(1.00f, 0.40f, 0.10f, 1f); // fire: orange-red
                case 6  -> shapeRenderer.setColor(0.30f, 0.75f, 0.25f, 1f); // earth: green
                default -> shapeRenderer.setColor(1f, 0.72f, 0.1f, 1f);     // arrow: orange-gold
            }
            float radius = (p.projectileType >= 3) ? 5f : 4f;
            shapeRenderer.circle(sx, sy, radius, 10);
        }
        shapeRenderer.end();
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

    /** Draws a flickering fire shape on the given world tile (world-space projection). */
    private void renderFireAnimation(float wx, float wy) {
        float sx    = renderer.worldToScreenX(wx, wy);
        float sy    = renderer.worldToScreenY(wx, wy);
        float flick = (float) Math.sin(firemakerFlicker);

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);
        // Base ember
        shapeRenderer.setColor(0.55f, 0.08f, 0.02f, 0.9f);
        shapeRenderer.ellipse(sx - 7, sy - 2, 14, 7);
        // Orange flame body
        shapeRenderer.setColor(0.95f, 0.45f, 0.05f, 0.85f);
        float h1 = 10f + flick * 2f;
        shapeRenderer.ellipse(sx - 5, sy, 10, h1);
        // Yellow tip
        shapeRenderer.setColor(1.0f, 0.90f, 0.15f, 0.75f);
        float h2 = 6f + flick * 1.5f;
        shapeRenderer.ellipse(sx - 3, sy + h1 - 3f, 6, h2);
        shapeRenderer.end();
    }

    private void renderHUD() {
        int w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();

        // Run energy orb -- to the LEFT of the minimap, OSRS style
        final int ORB_R  = 22;
        int miniLeftX = MiniMap.getLeftX(w);
        final int ORB_CX = miniLeftX - 8 - ORB_R;
        final int RN_CY  = h - 44;

        float rnRatio = Math.min(1f, playerRunEnergy / 100f);

        shapeRenderer.setProjectionMatrix(screenProjection);

        // -- Pass 1: dark background circle --
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.07f, 0.10f, 0.04f, 0.92f);
        shapeRenderer.circle(ORB_CX, RN_CY, ORB_R);
        shapeRenderer.end();

        // -- Pass 2: arc fill (green when idle, yellow when running) --
        if (rnRatio > 0.01f) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            if (isRunning) {
                shapeRenderer.setColor(1.00f, 0.85f, 0.00f, 1f);
            } else {
                shapeRenderer.setColor(0.60f, 0.90f, 0.18f, 1f);
            }
            shapeRenderer.arc(ORB_CX, RN_CY, ORB_R - 3,
                90f - rnRatio * 360f, rnRatio * 360f, 32);
            shapeRenderer.end();
        }

        // -- Pass 3: outline ring (gold glow when active) --
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        if (isRunning) {
            shapeRenderer.setColor(1.00f, 0.70f, 0.00f, 1f);
            shapeRenderer.circle(ORB_CX, RN_CY, ORB_R + 3);
            shapeRenderer.setColor(1.00f, 0.95f, 0.20f, 1f);
        } else {
            shapeRenderer.setColor(0.40f, 0.75f, 0.20f, 1f);
        }
        shapeRenderer.circle(ORB_CX, RN_CY, ORB_R);
        shapeRenderer.end();


        // -- Pass 4: lightning bolt icon inside orb --
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.28f, 0.82f, 0.14f, 0.50f);
        shapeRenderer.triangle(ORB_CX + 4, RN_CY + 9, ORB_CX - 2, RN_CY + 1, ORB_CX + 3, RN_CY + 1);
        shapeRenderer.triangle(ORB_CX - 3, RN_CY, ORB_CX + 4, RN_CY, ORB_CX - 3, RN_CY - 9);
        shapeRenderer.end();

        // -- Pass 5: run energy number centered in orb --
        screenBatch.setProjectionMatrix(screenProjection);
        screenBatch.begin();

        font.getData().setScale(FontManager.getScale(FontManager.FontContext.SKILL));

        gl.setText(font, String.valueOf(playerRunEnergy));
        if (isRunning) {
            font.setColor(1.00f, 0.95f, 0.20f, 1f);
        } else {
            font.setColor(0.82f, 1.00f, 0.68f, 1f);
        }
        font.draw(screenBatch, gl, ORB_CX - gl.width / 2f, RN_CY + gl.height / 2f);

        // Coordinates debug text -- bottom-right, unchanged
        font.getData().setScale(FontManager.getScale(FontManager.FontContext.BASE_UI));
        font.setColor(0.6f, 0.6f, 0.6f, 0.8f);
        font.draw(screenBatch, String.format("(%d,%d)", playerX, playerY), w - 70, 15);

        // Version label — top-right, right-justified just left of the minimap circle
        font.getData().setScale(0.90f);
        font.setColor(1f, 1f, 1f, 0.98f);
        GlyphLayout verLayout = new GlyphLayout(font, GAME_VERSION);
        font.draw(screenBatch, verLayout, miniLeftX - 6 - verLayout.width, h - 8);

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
        if (spriteSheet  != null) spriteSheet.dispose();
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
