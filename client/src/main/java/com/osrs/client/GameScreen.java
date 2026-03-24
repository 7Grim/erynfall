package com.osrs.client;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.osrs.client.network.ClientPacketHandler;
import com.osrs.client.network.NettyClient;
import com.osrs.client.renderer.CoordinateConverter;
import com.osrs.client.renderer.IsometricRenderer;
import com.osrs.client.world.TutorialIslandMap;
import com.osrs.client.ui.CombatUI;
import com.osrs.client.ui.ContextMenu;
import com.osrs.client.ui.DialogueUI;
import com.osrs.client.ui.InventoryUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private DialogueUI   dialogueUI;
    private InventoryUI  inventoryUI;
    private int[][]      tileMap;

    // -----------------------------------------------------------------------
    // Player state
    // -----------------------------------------------------------------------
    /** Logical (server-authoritative) tile the player is walking toward. */
    private int   playerX = 50, playerY = 50;
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
    // Approach-and-act state
    // -----------------------------------------------------------------------
    private int    pendingNpcId      = -1;
    private String pendingAction     = null; // "attack" | "talk"
    /** The walk-target tile we last submitted so we don't spam WalkTo. */
    private int    pendingWalkTargX  = -1, pendingWalkTargY = -1;

    // -----------------------------------------------------------------------
    // Death screen state
    // -----------------------------------------------------------------------
    /** > 0 while the "Oh dear, you are dead!" overlay is showing (counts down in seconds). */
    private float deathScreenTimer = 0f;
    private static final float DEATH_SCREEN_DURATION = 4f;

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

        screenProjection = new Matrix4().setToOrtho2D(
            0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        renderer    = new IsometricRenderer(camera, batch, shapeRenderer);
        tileMap     = TutorialIslandMap.generate();
        contextMenu = new ContextMenu();
        combatUI    = new CombatUI();
        dialogueUI  = new DialogueUI();
        inventoryUI = new InventoryUI();

        try {
            nettyClient = new NettyClient();
            nettyClient.connect();
            nettyClient.sendHandshake("TestPlayer");
            LOG.info("Connected to server");
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
        updateMovement(delta);
        updateNpcVisuals(delta);

        // Camera follows player visual position
        camera.position.set(
            renderer.worldToScreenX(visualX, visualY),
            renderer.worldToScreenY(visualX, visualY), 0);
        camera.update();

        // --- World ---
        renderer.renderWorld(tileMap, visualX, visualY);

        // NPCs — rendered at their smoothly interpolated visual positions
        ClientPacketHandler handler = handler();
        if (handler != null) {
            for (Map.Entry<Integer, int[]> entry : handler.getEntityPositions().entrySet()) {
                int id = entry.getKey();
                if (handler.isPlayer(id)) continue;          // skip other players for now

                float[] vis = npcVisual.get(id);
                if (vis == null) continue;

                renderer.renderNPC((int) Math.round(vis[0]), (int) Math.round(vis[1]), id);

                int[] hp = handler.getEntityHealth(id);
                if (hp[0] < hp[1]) {
                    renderer.renderHealthBar(
                        (int) Math.round(vis[0]), (int) Math.round(vis[1]), hp[0], hp[1]);
                }
            }
        }

        // Player
        renderer.renderPlayer(visualX, visualY);

        // Hitsplats
        combatUI.update(delta);
        combatUI.render(shapeRenderer, batch, font, camera);

        // --- Screen-space UI ---
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        renderHUD();
        combatUI.renderMessages(screenBatch, font, Gdx.graphics.getHeight());
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

        playerHealth    = h.getPlayerHealth();
        playerMaxHealth = h.getPlayerMaxHealth();
        attackLevel     = h.getSkillLevel(0);
        strengthLevel   = h.getSkillLevel(1);
        defenceLevel    = h.getSkillLevel(2);

        // Death detection — show overlay and snap to respawn
        if (h.isPlayerDead()) {
            h.consumePlayerDeath();
            deathScreenTimer = DEATH_SCREEN_DURATION;
            playerX   = h.getDeathRespawnX();
            playerY   = h.getDeathRespawnY();
            visualX   = playerX;
            visualY   = playerY;
            clearPendingAction();
            combatUI.addMessage("Oh dear, you are dead!");
            LOG.info("Death screen shown — respawning at ({}, {})", playerX, playerY);
        }

        for (ClientPacketHandler.CombatHitEvent evt : h.drainCombatHits()) {
            combatUI.addDamageNumber(evt.targetX, evt.targetY, evt.damage, evt.hit);
            boolean npcHitMe = (evt.targetId == h.getMyPlayerId());
            if (npcHitMe) {
                combatUI.addMessage(evt.hit
                    ? String.format("You were hit for %d!", evt.damage)
                    : "The attack missed you.");
            } else {
                combatUI.addMessage(evt.hit
                    ? String.format("You hit for %d!", evt.damage)
                    : "Your attack missed.");
                if (evt.xpAwarded > 0) combatUI.addMessage("+" + evt.xpAwarded + " XP");
            }
        }

        if (h.consumeLevelUp()) {
            String[] skills = {"Attack", "Strength", "Defence", "Hitpoints", "Ranged", "Magic"};
            int idx = h.getLeveledUpSkill();
            if (idx >= 0 && idx < skills.length)
                combatUI.addMessage("LEVEL UP! " + skills[idx] + " → " + h.getSkillLevel(idx));
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

        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            int mx = Gdx.input.getX(), my = Gdx.input.getY();
            int[] tile = screenToTile(mx, my);
            LOG.debug("Right-click tile ({},{})", tile[0], tile[1]);
            List<ContextMenu.MenuItem> opts = generateContextMenu(tile[0], tile[1]);
            if (!opts.isEmpty())
                contextMenu.open(mx, Gdx.graphics.getHeight() - my, opts);
        }

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            int mx = Gdx.input.getX(), my = Gdx.input.getY();
            if (contextMenu.isVisible()) {
                ContextMenu.MenuItem clicked =
                    contextMenu.getClickedItem(mx, Gdx.graphics.getHeight() - my);
                if (clicked != null) handleContextMenuAction(clicked);
                contextMenu.close();
            } else {
                int[] tile = screenToTile(mx, my);
                if (CoordinateConverter.isValidTile(tile[0], tile[1]))
                    walkTo(tile[0], tile[1]);
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) contextMenu.close();
    }

    /** Player-initiated walk — cancels any pending approach. */
    private void walkTo(int x, int y) {
        clearPendingAction();
        playerX = x; playerY = y;
        if (nettyClient != null) nettyClient.sendWalkTo(x, y);
    }

    /** Auto-walk from approach logic — does NOT clear the pending action. */
    private void autoWalkTo(int x, int y) {
        playerX = x; playerY = y;
        pendingWalkTargX = x; pendingWalkTargY = y;
        if (nettyClient != null) nettyClient.sendWalkTo(x, y);
    }

    // -----------------------------------------------------------------------
    // Player movement interpolation
    // -----------------------------------------------------------------------

    private void updateMovement(float delta) {
        float dx = playerX - visualX, dy = playerY - visualY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist < 0.01f) { visualX = playerX; visualY = playerY; return; }
        float step = TILES_PER_SECOND * delta;
        if (step >= dist) { visualX = playerX; visualY = playerY; }
        else { visualX += (dx / dist) * step; visualY += (dy / dist) * step; }
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
        if (nettyClient != null) {
            if ("attack".equals(pendingAction))
                nettyClient.sendAttack(pendingNpcId);
            else if ("talk".equals(pendingAction))
                nettyClient.sendTalkToNpc(pendingNpcId);
        }
        clearPendingAction();
    }

    private void clearPendingAction() {
        pendingNpcId = -1; pendingAction = null;
        pendingWalkTargX = -1; pendingWalkTargY = -1;
    }

    private boolean isInRange(int px, int py, int nx, int ny) {
        return Math.max(Math.abs(px - nx), Math.abs(py - ny)) <= INTERACT_RANGE;
    }

    private int[] closestAdjacentTile(int px, int py, int nx, int ny) {
        int bx = nx, by = ny - 1;
        double best = Double.MAX_VALUE;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int tx = nx + dx, ty = ny + dy;
                if (!CoordinateConverter.isValidTile(tx, ty)) continue;
                double d = Math.hypot(px - tx, py - ty);
                if (d < best) { best = d; bx = tx; by = ty; }
            }
        }
        return new int[]{bx, by};
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

        // Check all server-tracked NPCs — show options for whichever NPC the
        // player right-clicked (their current rendered tile, rounded)
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
                    String name = "NPC " + id;
                    opts.add(new ContextMenu.MenuItem("Attack " + name, "attack", id));
                    opts.add(new ContextMenu.MenuItem("Talk-to " + name, "talk",   id));
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
        }
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
        for (int i = 0; i < n; i++)
            font.draw(screenBatch, items.get(i).label, mx + 5,
                my + (n - 1 - i) * ContextMenu.ITEM_HEIGHT + 13);
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
        font.setColor(0.9f, 0.9f, 0.9f, 1f);
        font.draw(screenBatch,
            String.format("Atk:%d  Str:%d  Def:%d", attackLevel, strengthLevel, defenceLevel),
            10, h - 28);
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
