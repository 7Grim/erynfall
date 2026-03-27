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
import com.osrs.client.network.ClientPacketHandler;
import com.osrs.client.network.NettyClient;
import com.osrs.client.renderer.CoordinateConverter;
import com.osrs.client.renderer.IsometricRenderer;
import com.osrs.client.world.TutorialIslandMap;
import com.osrs.client.ui.ChatBox;
import com.osrs.client.ui.CombatUI;
import com.osrs.client.ui.ContextMenu;
import com.osrs.client.ui.DialogueUI;
import com.osrs.client.ui.LevelUpOverlay;
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

    /** OSRS walk speed: 1 tile per 0.6 s. */
    private static final float TILES_PER_SECOND = 1.0f / 0.6f;

    /** Chebyshev distance to activate an NPC interaction. */
    private static final int INTERACT_RANGE = 1;

    // Credentials passed from LoginScreen (null = dev bypass, will use defaults)
    private final String loginUsername;
    private final String loginPassword;

    public GameScreen() {
        this.loginUsername = "TestPlayer";
        this.loginPassword = "testpass";
    }

    public GameScreen(String username, String password) {
        this.loginUsername = username;
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
    /** Smoothly interpolated render position. */
    private float visualX = 50f, visualY = 50f;

    // HUD (updated from server each frame)
    private int playerHealth = 10, playerMaxHealth = 10;
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
    private String pendingAction     = null; // "attack" | "talk"
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
    /** entityId → active overhead text (null / absent = no text shown). */
    private final Map<Integer, OverheadText> overheadTexts = new HashMap<>();
    /** Entity ID used for the local player's overhead text. */
    private int localPlayerId = -1;
    /** questId -> last displayed task completion count. */
    private final Map<Integer, Integer> shownQuestProgress = new HashMap<>();

    private boolean initialized = false;

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @Override
    public void create() {
        LOG.info("GameScreen created – display {}x{}",
            Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        batch        = new SpriteBatch();
        screenBatch  = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        camera       = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0, 0, 0);
        camera.update();

        font = new BitmapFont();
        font.setColor(Color.WHITE);
        font.getData().markupEnabled = true; // enables [#rrggbb] color tags in strings

        screenProjection = new Matrix4().setToOrtho2D(
            0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        renderer   = new IsometricRenderer(camera, batch, shapeRenderer);
        tileMap    = TutorialIslandMap.generate();
        contextMenu = new ContextMenu();
        combatUI   = new CombatUI();
        sidePanel  = new SidePanel();
        dialogueUI = new DialogueUI();
        chatBox        = new ChatBox();
        xpDropOverlay  = new XpDropOverlay();
        levelUpOverlay = new LevelUpOverlay();

        try {
            nettyClient = new NettyClient();
            nettyClient.connect();
            nettyClient.sendHandshake(loginUsername, loginPassword);
            LOG.info("Connected to server as {}", loginUsername);
        } catch (Exception e) {
            LOG.error("Failed to connect to server", e);
        }

        initialized = true;
    }

    // -----------------------------------------------------------------------
    // Render loop
    // -----------------------------------------------------------------------

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.12f, 0.12f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        if (!initialized) return;

        float delta = Gdx.graphics.getDeltaTime();

        processServerEvents();
        handleInput();
        processApproach();
        processGroundItemApproach();
        updateMovement(delta);
        updateNpcVisuals(delta);
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

        // NPCs — rendered at their smoothly interpolated visual positions
        ClientPacketHandler handler = handler();
        if (handler != null) {
            for (Map.Entry<Integer, int[]> entry : handler.getEntityPositions().entrySet()) {
                int id = entry.getKey();
                if (handler.isPlayer(id)) continue;          // skip other players for now

                float[] vis = npcVisual.get(id);
                if (vis == null) continue;

                String npcName = handler.getEntityName(id);
                renderer.renderNPC(vis[0], vis[1], id, npcName);

                int[] hp = handler.getEntityHealth(id);
                if (hp[0] < hp[1]) {
                    renderer.renderHealthBar(vis[0], vis[1], hp[0], hp[1]);
                }
            }
        }

        // Player (pickup animation plays for 1.8 s after item is clicked)
        renderer.renderPlayer(visualX, visualY, pickupAnimationTimer > 0);

        // Hitsplats
        combatUI.update(delta);
        combatUI.render(shapeRenderer, batch, font, camera);

        // Overhead chat text (world space — same projection as hitsplats)
        chatBox.update(delta);
        xpDropOverlay.update(delta);
        levelUpOverlay.update(delta);
        renderOverheadText(delta);

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
        int[]  lvls = new int[6];
        long[] xps  = new long[6];
        for (int i = 0; i < 6; i++) {
            lvls[i] = h.getSkillLevel(i);
            xps[i]  = h.getSkillTotalXp(i);
        }
        sidePanel.setSkillData(lvls, xps);

        // Sync inventory to the Inventory tab
        for (int i = 0; i < 28; i++) {
            sidePanel.setInventorySlot(i,
                h.getInventoryItemId(i),
                h.getInventoryQuantity(i),
                h.getInventoryName(i),
                h.getInventoryFlags(i));
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

        // Server chat messages ("I can't reach that!", "Too late — it's gone!", etc.)
        for (String msg : h.drainServerChatMessages()) {
            chatBox.addSystemMessage(msg);
            if ("I can't reach that!".equals(msg) || "Please step away before talking.".equals(msg)) {
                clearPendingAction();
                pendingGroundItemId = -1;
            }
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
        String[] skillNames = {"Attack", "Strength", "Defence", "Hitpoints", "Ranged", "Magic"};
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
            if (shown != evt.tasksCompleted || evt.completed) {
                if (evt.completed) {
                    chatBox.addSystemMessage("Quest complete: " + evt.questName + "!");
                } else {
                    chatBox.addSystemMessage(evt.questName + ": "
                        + evt.tasksCompleted + "/" + evt.tasksTotal + " objectives complete.");
                }
                shownQuestProgress.put(evt.questId, evt.tasksCompleted);
            }
        }

        for (ClientPacketHandler.DialoguePromptEvent evt : h.drainDialoguePrompts()) {
            if (evt.options.isEmpty()) {
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
            if (h.isPlayer(id)) continue;

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
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
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
                    contextMenu.open(mx, screenMy, opts);
            }
        }

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
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
                int style = sidePanel.handleLeftClick(mx, screenMy);
                if (style >= 0 && nettyClient != null) {
                    nettyClient.sendSetCombatStyle(style);
                }
            } else {
                int[] tile = screenToTile(mx, my);
                if (CoordinateConverter.isValidTile(tile[0], tile[1]))
                    walkTo(tile[0], tile[1]);
            }
        }

        // Mouse-up: finish inventory drag
        if (!Gdx.input.isButtonPressed(Input.Buttons.LEFT) && sidePanel.isInventoryDragging()) {
            int[] swap = sidePanel.handleInventoryMouseUp(mx, screenMy);
            if (swap != null && nettyClient != null) {
                nettyClient.sendSwapInventorySlots(swap[0], swap[1]);
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) contextMenu.close();
    }

    private void handleDialogueInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            submitDialogueOptionByIndex(0);
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            submitDialogueOptionByIndex(1);
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
            submitDialogueOptionByIndex(2);
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) {
            submitDialogueOptionByIndex(3);
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_5)) {
            submitDialogueOptionByIndex(4);
            return;
        }

        // OSRS-style continue shortcut; for option prompts we map Space to option 1.
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            submitDialogueOptionByIndex(0);
            return;
        }

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            int mx = Gdx.input.getX();
            int my = Gdx.graphics.getHeight() - Gdx.input.getY();
            DialogueUI.DialogueOption selected = dialogueUI.getSelectedOption(mx, my);
            if (selected != null && nettyClient != null) {
                nettyClient.sendDialogueResponse(selected.optionId);
            }
        }
    }

    private void submitDialogueOptionByIndex(int index) {
        if (index < 0 || index >= dialogueUI.getOptions().size() || nettyClient == null) {
            return;
        }
        nettyClient.sendDialogueResponse(dialogueUI.getOptions().get(index).optionId);
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
        opts.add(new ContextMenu.MenuItem("Drop " + name, "inv_drop", slot));
        opts.add(new ContextMenu.MenuItem("Examine " + name, "inv_examine", slot));
        contextMenu.open(mx, my, opts);
    }

    /** Player-initiated walk — cancels any pending approach. */
    private void walkTo(int x, int y) {
        clearPendingAction();
        walkDestX = x; walkDestY = y;
        if (nettyClient != null) nettyClient.sendWalkTo(x, y);
    }

    /** Auto-walk from approach logic — does NOT clear the pending action. */
    private void autoWalkTo(int x, int y) {
        walkDestX = x; walkDestY = y;
        pendingWalkTargX = x; pendingWalkTargY = y;
        if (nettyClient != null) nettyClient.sendWalkTo(x, y);
    }

    // -----------------------------------------------------------------------
    // Player movement interpolation
    // -----------------------------------------------------------------------

    private void updateMovement(float delta) {
        // Move toward walk destination if one is set, otherwise stay at current tile
        float targetX = (walkDestX >= 0) ? walkDestX : playerX;
        float targetY = (walkDestY >= 0) ? walkDestY : playerY;

        float dx = targetX - visualX, dy = targetY - visualY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist < 0.01f) {
            visualX = targetX; visualY = targetY;
            if (walkDestX >= 0) {
                // Arrived — commit destination as current tile, notify server
                playerX = walkDestX; playerY = walkDestY;
                walkDestX = -1; walkDestY = -1;
                if (nettyClient != null) nettyClient.sendPlayerMovement(playerX, playerY, 0);
            }
            return;
        }

        // Snapshot current rounded tile before moving
        int prevTileX = Math.round(visualX);
        int prevTileY = Math.round(visualY);

        float step = TILES_PER_SECOND * delta;
        if (step >= dist) {
            visualX = targetX; visualY = targetY;
            playerX = (int) Math.round(targetX);
            playerY = (int) Math.round(targetY);
            walkDestX = -1; walkDestY = -1;
            if (nettyClient != null) nettyClient.sendPlayerMovement(playerX, playerY, 0);
        } else {
            visualX += (dx / dist) * step;
            visualY += (dy / dist) * step;
            // Advance logical tile whenever visual crosses a tile boundary
            int newTileX = Math.round(visualX);
            int newTileY = Math.round(visualY);
            if (newTileX != prevTileX || newTileY != prevTileY) {
                playerX = newTileX; playerY = newTileY;
                if (nettyClient != null) nettyClient.sendPlayerMovement(playerX, playerY, 0);
            }
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
        LOG.info("In range of NPC {} — executing '{}'", pendingNpcId, pendingAction);
        if (pendingActionRetryTimer > 0f) {
            return;
        }
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
            walkDestX = itemX; walkDestY = itemY;
            if (nettyClient != null) nettyClient.sendWalkTo(itemX, itemY);
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
                if (h.isPlayer(id)) continue;
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

                    if (level > 0) {
                        // Combat NPC: Attack is the primary option (top)
                        opts.add(new ContextMenu.MenuItem(
                            "Attack " + yellowName + levelSuffix, "attack", id));
                    }
                    opts.add(new ContextMenu.MenuItem(
                        "Talk-to " + yellowName, "talk", id));
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
            case "take"   -> startGroundItemApproach((Integer) item.target);
            case "inv_eat"   -> { if (nettyClient != null) nettyClient.sendUseItem((Integer) item.target, "eat"); }
            case "inv_wield" -> { if (nettyClient != null) nettyClient.sendUseItem((Integer) item.target, "wield"); }
            case "inv_drop"  -> { if (nettyClient != null) nettyClient.sendDropItem((Integer) item.target); }
            case "inv_examine" -> LOG.info("Examine: {}", item.label);
            case "examine_npc" -> examineNpc((Integer) item.target);
        }
    }

    /** Show an examine message in the chat area — text sourced per NPC type. */
    private void examineNpc(int npcId) {
        ClientPacketHandler h = handler();
        String name = (h != null) ? h.getEntityName(npcId) : "";
        String text = switch (name) {
            case "Rat"               -> "A small rat. It doesn't look very threatening.";
            case "Giant Rat"         -> "A big, fat, and very ugly rat.";
            case "Chicken"           -> "It's a chicken. It's looking at me funny.";
            case "Cow"               -> "A dairy cow. Moo.";
            case "Goblin"            -> "A vile little green creature.";
            case "Tutorial Guide"    -> "He looks like he wants to help.";
            case "Combat Instructor" -> "A seasoned warrior, ready to teach combat basics.";
            default                  -> "It's a " + (name.isEmpty() ? "creature" : name) + ".";
        };
        chatBox.addSystemMessage(text);
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
            font.setColor(1f, 1f, 0f, alpha);
            font.draw(batch, ot.text, textX, textY);
        }

        batch.end();
        font.setColor(Color.WHITE);
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    // -----------------------------------------------------------------------
    // Screen-space rendering
    // -----------------------------------------------------------------------

    private void renderContextMenu() {
        List<ContextMenu.MenuItem> items = contextMenu.getItems();
        if (items.isEmpty()) return;

        int mx = contextMenu.getScreenX(), my = contextMenu.getScreenY();
        int n = items.size();
        int totalH = ContextMenu.HEADER_HEIGHT + n * ContextMenu.ITEM_HEIGHT + 4;
        int w = ContextMenu.MENU_WIDTH;

        shapeRenderer.setProjectionMatrix(screenProjection);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.06f, 0.06f, 0.15f, 0.92f);
        shapeRenderer.rect(mx, my, w, totalH);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.85f, 0.75f, 0.1f, 1f);
        shapeRenderer.rect(mx, my, w, totalH);
        shapeRenderer.end();

        screenBatch.setProjectionMatrix(screenProjection);
        screenBatch.begin();
        font.setColor(1f, 0.85f, 0f, 1f);
        font.draw(screenBatch, "Choose Option", mx + 5, my + n * ContextMenu.ITEM_HEIGHT + 14);
        font.setColor(Color.WHITE);
        for (int i = 0; i < n; i++) {
            font.setColor(Color.WHITE);
            font.draw(screenBatch, items.get(i).label, mx + 5,
                my + (n - 1 - i) * ContextMenu.ITEM_HEIGHT + 13);
        }
        screenBatch.end();
        font.setColor(Color.WHITE);
    }

    private void renderDialogueOverlay(int mouseX, int mouseY) {
        if (!dialogueUI.isVisible()) return;

        int panelX = DialogueUI.PANEL_X;
        int panelY = DialogueUI.PANEL_Y;
        int optionX = panelX + DialogueUI.H_PADDING;
        int optionY = panelY + ChatBox.INPUT_H + DialogueUI.CONTENT_TOP_PADDING;
        int optionW = DialogueUI.PANEL_WIDTH - (DialogueUI.H_PADDING * 2);
        int contentTop = panelY + DialogueUI.PANEL_HEIGHT - 8;

        shapeRenderer.setProjectionMatrix(screenProjection);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.05f, 0.04f, 0.04f, 0.9f);
        shapeRenderer.rect(panelX, panelY, DialogueUI.PANEL_WIDTH, DialogueUI.PANEL_HEIGHT);
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 0.8f);
        shapeRenderer.rect(panelX, panelY + ChatBox.INPUT_H - 1, DialogueUI.PANEL_WIDTH, 1);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.85f, 0.75f, 0.1f, 1f);
        shapeRenderer.rect(panelX, panelY, DialogueUI.PANEL_WIDTH, DialogueUI.PANEL_HEIGHT);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        List<DialogueUI.DialogueOption> options = dialogueUI.getOptions();
        for (int i = 0; i < options.size(); i++) {
            int y = optionY + i * (DialogueUI.OPTION_HEIGHT + DialogueUI.OPTION_GAP);
            boolean hover = mouseX >= optionX && mouseX < optionX + optionW
                && mouseY >= y && mouseY < y + DialogueUI.OPTION_HEIGHT;
            if (hover) {
                shapeRenderer.setColor(0.2f, 0.24f, 0.34f, 0.9f);
            } else {
                shapeRenderer.setColor(0.1f, 0.1f, 0.16f, 0.72f);
            }
            shapeRenderer.rect(optionX, y, optionW, DialogueUI.OPTION_HEIGHT);
        }
        shapeRenderer.end();

        screenBatch.setProjectionMatrix(screenProjection);
        screenBatch.begin();

        font.setColor(Color.WHITE);
        String npcText = dialogueUI.getNpcText();
        GlyphLayout layout = new GlyphLayout(font, npcText, Color.WHITE, optionW, -1, true);
        font.draw(screenBatch, layout, optionX, contentTop);

        for (int i = 0; i < options.size(); i++) {
            int y = optionY + i * (DialogueUI.OPTION_HEIGHT + DialogueUI.OPTION_GAP);
            font.setColor(Color.WHITE);
            font.draw(screenBatch, (i + 1) + ". " + options.get(i).text, optionX + 8, y + 14);
        }

        font.setColor(0.95f, 0.95f, 0.5f, 1f);
        font.draw(screenBatch, "Choose an Option (1-5 or click)", panelX + 8, panelY + ChatBox.INPUT_H - 4);
        screenBatch.end();
        font.setColor(Color.WHITE);
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
        font.getData().setScale(2.5f);
        String title = "Oh dear, you are dead!";
        font.draw(screenBatch, title, w / 2f - 180, h / 2f + 60);

        // Subtitle
        font.setColor(Color.WHITE);
        font.getData().setScale(1.2f);
        font.draw(screenBatch, "You have been teleported back to Lumbridge.", w / 2f - 200, h / 2f + 10);

        // Countdown
        font.setColor(new Color(0.8f, 0.8f, 0.8f, 1f));
        font.getData().setScale(1.0f);
        int secs = (int) Math.ceil(deathScreenTimer);
        font.draw(screenBatch, "Respawning in " + secs + "s  (click to continue)", w / 2f - 140, h / 2f - 30);

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
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
        font.getData().setScale(1f);
        font.setColor(1f, 0.85f, 0f, 1f);  // gold NPC name
        font.draw(screenBatch, npcName, panelX + 8, panelY + panelH - 6);
        font.setColor(0.75f, 0.75f, 0.75f, 1f);  // grey subtitle
        font.draw(screenBatch,
            String.format("Level: %d   HP: %d / %d", combatLevel, curHp, maxHp),
            panelX + 8, panelY + 22);
        screenBatch.end();
        font.setColor(Color.WHITE);
    }

    private void renderHUD() {
        int w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
        int maxHp = Math.max(playerMaxHealth, 1);

        shapeRenderer.setProjectionMatrix(screenProjection);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.25f, 0f, 0f, 0.85f);
        shapeRenderer.rect(10, h - 18, 120, 10);
        shapeRenderer.setColor(0.85f, 0.05f, 0.05f, 1f);
        shapeRenderer.rect(10, h - 18, 120f * playerHealth / maxHp, 10);
        shapeRenderer.end();

        screenBatch.setProjectionMatrix(screenProjection);
        screenBatch.begin();
        font.setColor(1f, 0.9f, 0.9f, 1f);
        font.draw(screenBatch, String.format("HP %d/%d", playerHealth, maxHp), 136, h - 9);
        font.setColor(0.6f, 0.6f, 0.6f, 0.8f);
        font.draw(screenBatch, String.format("(%d,%d)", playerX, playerY), w - 70, 15);
        screenBatch.end();
        font.setColor(Color.WHITE);
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
        if (font         != null) font.dispose();
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
