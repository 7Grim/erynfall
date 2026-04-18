package com.osrs.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.osrs.client.ErynfallGame;
import com.osrs.client.LaunchOptions;

import java.nio.file.Path;
import java.util.List;

/**
 * Login / character-select screen.
 *
 * ACCOUNT_LOGIN    — two-panel stone layout: LOG IN (left) + CREATE ACCOUNT (right)
 * CHARACTER_SELECT — character slot panel after credential validation
 *
 * Bottom bar (always visible): [AUDIO] [< World N >] [QUIT]
 */
public class LoginScreen extends ScreenAdapter {

    private enum ViewState { ACCOUNT_LOGIN, CHARACTER_SELECT }

    // ── Prefs keys ───────────────────────────────────────────────────────────
    private static final String PREFS_NAME                 = "erynfall-login";
    private static final String PREF_KEY_LAST_EMAIL        = "email";
    private static final String PREF_KEY_SAVED_EMAIL       = "saved_email";
    private static final String PREF_KEY_SAVED_PASSWORD    = "saved_password";
    private static final String PREF_KEY_SELECTED_WORLD_ID = "selected_world_id";
    private static final String PREF_KEY_CHARACTER_NAME    = "character_name";

    // ── Two-panel login dimensions ────────────────────────────────────────────
    private static final int LOGIN_PANEL_W = 358;
    private static final int LOGIN_PANEL_H = 440;
    private static final int RIGHT_PANEL_W = 248;
    private static final int RIGHT_PANEL_H = 440;
    private static final int PANEL_GAP     = 4;
    private static final int TOTAL_PANEL_W = LOGIN_PANEL_W + PANEL_GAP + RIGHT_PANEL_W; // 610

    // ── Character-select panel ────────────────────────────────────────────────
    private static final int CHAR_PANEL_W    = 480;
    private static final int CHAR_PANEL_H    = 420;
    private static final int CHAR_SLOT_H     = 58;
    private static final int CHAR_SLOT_GAP   = 6;
    private static final int CHAR_PLAY_BTN_W = 70;
    private static final int MAX_MEMBER_CHARS = 3;

    // ── Shared UI constants ───────────────────────────────────────────────────
    private static final int   FIELD_H          = 30;
    private static final int   BTN_H            = 36;
    private static final int   PAD              = 18;
    private static final int   EMAIL_MAX_LEN    = 254;
    private static final int   PASSWORD_MAX_LEN = 128;
    private static final float BACKSPACE_INITIAL_DELAY   = 0.35f;
    private static final float BACKSPACE_REPEAT_INTERVAL = 0.045f;

    // ── Bottom bar ────────────────────────────────────────────────────────────
    private static final int BAR_BTN_H    = 34;
    private static final int BAR_MARGIN   = 16;
    private static final int AUDIO_BTN_W  = 80;
    private static final int QUIT_BTN_W   = 72;
    private static final int WORLD_SEL_W  = 200;
    private static final int ARROW_BTN_W  = 30;

    // ── Stone color palette ───────────────────────────────────────────────────
    private static final float[] PANEL_BG      = {0.07f, 0.07f, 0.08f};
    private static final float[] BORDER_OUTER  = {0.18f, 0.16f, 0.13f};
    private static final float[] BORDER_INNER  = {0.32f, 0.28f, 0.20f};
    private static final float[] GOLD          = {0.72f, 0.58f, 0.18f};
    private static final float[] DIVIDER_COL   = {0.22f, 0.20f, 0.16f};

    // ── Fields ────────────────────────────────────────────────────────────────
    private final ErynfallGame game;

    private BitmapFont    font;
    private SpriteBatch   batch;
    private ShapeRenderer sr;
    private Matrix4       proj;
    private Texture       bgTexture;

    private ViewState viewState        = ViewState.ACCOUNT_LOGIN;
    private boolean   savedAccountMode = false;

    private String  emailBuffer    = "";
    private String  passwordBuffer = "";
    private boolean focusEmail     = true;

    private String pendingEmail    = "";
    private String pendingPassword = "";
    private String characterName   = "";

    private String  errorMessage              = "";
    private float   cursorBlink               = 0f;
    private boolean transitioning             = false;
    private InputAdapter inputProcessor;
    private boolean backspaceHeld             = false;
    private float   backspaceHeldTime         = 0f;
    private float   backspaceRepeatAccumulator = 0f;

    private boolean hasSavedCredentials = false;
    private String  savedEmail          = "";
    private String  savedPassword       = "";

    private List<LaunchOptions.WorldOption> worldOptions      = List.of();
    private int                             selectedWorldIndex = 0;

    // =========================================================================
    // Constructor
    // =========================================================================

    public LoginScreen(ErynfallGame game) {
        this(game, "");
    }

    public LoginScreen(ErynfallGame game, String initialErrorMessage) {
        this.game = game;
        this.errorMessage = initialErrorMessage == null ? "" : initialErrorMessage;
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @Override
    public void show() {
        if (game.getAudioManager() != null) game.getAudioManager().playLoginMusic();

        FontManager.initialize();
        font  = FontManager.regular();
        batch = new SpriteBatch();
        sr    = new ShapeRenderer();
        proj  = new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        Path bgPath = game.getLaunchOptions().repoRootPath().resolve("art/assets/login-bg.png");
        if (bgPath.toFile().exists()) {
            bgTexture = new Texture(Gdx.files.absolute(bgPath.toString()));
        }

        inputProcessor = buildInputAdapter();

        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        savedEmail          = prefs.getString(PREF_KEY_SAVED_EMAIL, "");
        savedPassword       = prefs.getString(PREF_KEY_SAVED_PASSWORD, "");
        hasSavedCredentials = !savedEmail.isBlank() && !savedPassword.isBlank();
        savedAccountMode    = hasSavedCredentials;
        viewState           = ViewState.ACCOUNT_LOGIN;
        characterName       = prefs.getString(PREF_KEY_CHARACTER_NAME, "");
        worldOptions        = LaunchOptions.supportedWorlds();

        String preferredWorldId = prefs.getString(PREF_KEY_SELECTED_WORLD_ID, LaunchOptions.defaultLoginWorldId());
        selectedWorldIndex      = indexForWorldId(preferredWorldId);

        String lastEmail = prefs.getString(PREF_KEY_LAST_EMAIL, "");
        if (hasSavedCredentials) {
            emailBuffer    = savedEmail;
            passwordBuffer = savedPassword;
            focusEmail     = false;
        } else if (!lastEmail.isBlank()) {
            emailBuffer = lastEmail;
            focusEmail  = false;
        }

        Gdx.input.setInputProcessor(inputProcessor);
    }

    private InputAdapter buildInputAdapter() {
        return new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (transitioning) return true;

                switch (keycode) {
                    case Input.Keys.ENTER -> {
                        if (viewState == ViewState.CHARACTER_SELECT) playSelectedCharacter();
                        else if (savedAccountMode)                   loginSavedAccount();
                        else                                         submit();
                        return true;
                    }
                    case Input.Keys.M -> {
                        toggleAudio();
                        return true;
                    }
                    case Input.Keys.TAB -> {
                        if (!savedAccountMode && viewState == ViewState.ACCOUNT_LOGIN)
                            focusEmail = !focusEmail;
                        return true;
                    }
                    case Input.Keys.BACKSPACE -> {
                        if (!savedAccountMode && viewState == ViewState.ACCOUNT_LOGIN) {
                            deleteActiveFieldChar();
                            backspaceHeld = true;
                            backspaceHeldTime = 0f;
                            backspaceRepeatAccumulator = 0f;
                        }
                        return true;
                    }
                }

                if (isPasteShortcut(keycode)) { pasteFromClipboard(); return true; }
                return false;
            }

            @Override
            public boolean keyTyped(char character) {
                if (transitioning) return true;
                if (savedAccountMode || viewState == ViewState.CHARACTER_SELECT) return true;
                if (character < 32 || character == 127) return false;
                appendChar(character);
                return true;
            }

            @Override
            public boolean keyUp(int keycode) {
                if (keycode == Input.Keys.BACKSPACE) {
                    backspaceHeld = false;
                    backspaceHeldTime = 0f;
                    backspaceRepeatAccumulator = 0f;
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (button != Input.Buttons.LEFT || transitioning) return false;
                int w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
                int fy = h - screenY;   // flip to LibGDX Y-up space

                if (handleBottomBarTouch(screenX, fy, w)) return true;

                return viewState == ViewState.CHARACTER_SELECT
                    ? handleCharSelectTouch(screenX, fy, w, h)
                    : handleLoginTouch(screenX, fy, w, h);
            }
        };
    }

    @Override
    public void hide() {
        if (Gdx.input.getInputProcessor() == inputProcessor) Gdx.input.setInputProcessor(null);
        dispose();
    }

    @Override
    public void dispose() {
        font = null;
        if (bgTexture != null) { bgTexture.dispose();  bgTexture = null; }
        if (batch    != null) { batch.dispose();        batch    = null; }
        if (sr       != null) { sr.dispose();           sr       = null; }
    }

    // =========================================================================
    // Touch handlers
    // =========================================================================

    /** Bottom bar: AUDIO toggle (left), world arrows (center), QUIT (right). */
    private boolean handleBottomBarTouch(int sx, int fy, int w) {
        int barY = BAR_MARGIN;

        // AUDIO button — bottom-left
        if (sx >= BAR_MARGIN && sx <= BAR_MARGIN + AUDIO_BTN_W
         && fy >= barY && fy <= barY + BAR_BTN_H) {
            toggleAudio();
            return true;
        }

        // QUIT button — bottom-right
        int qx = w - BAR_MARGIN - QUIT_BTN_W;
        if (sx >= qx && sx <= qx + QUIT_BTN_W
         && fy >= barY && fy <= barY + BAR_BTN_H) {
            Gdx.app.exit();
            return true;
        }

        // World selector arrows — bottom-center (only when multiple worlds)
        if (!LaunchOptions.isRemoteServerTarget() && worldOptions.size() > 1) {
            int wx = (w - WORLD_SEL_W) / 2;
            if (fy >= barY && fy <= barY + BAR_BTN_H) {
                if (sx >= wx && sx <= wx + ARROW_BTN_W) {
                    selectWorld((selectedWorldIndex - 1 + worldOptions.size()) % worldOptions.size());
                    return true;
                }
                int rx = wx + WORLD_SEL_W - ARROW_BTN_W;
                if (sx >= rx && sx <= rx + ARROW_BTN_W) {
                    selectWorld((selectedWorldIndex + 1) % worldOptions.size());
                    return true;
                }
            }
        }

        return false;
    }

    private boolean handleLoginTouch(int sx, int fy, int w, int h) {
        int pY  = (h - LOGIN_PANEL_H) / 2;
        int lpX = (w - TOTAL_PANEL_W) / 2;
        int rpX = lpX + LOGIN_PANEL_W + PANEL_GAP;

        // Left panel (login)
        int[] la = loginAnchors(pY);
        int loginBtnY = la[0], saveBtnY = la[1], efBottom = la[2], pfBottom = la[3];
        int lfx = lpX + PAD, lfW = LOGIN_PANEL_W - PAD * 2;

        boolean inLX = sx >= lfx && sx <= lfx + lfW;

        if (savedAccountMode) {
            if (inLX && fy >= loginBtnY && fy <= loginBtnY + BTN_H) { loginSavedAccount(); return true; }
            if (inLX && fy >= saveBtnY  && fy <= saveBtnY  + BTN_H) { clearSavedCredentials(); return true; }
        } else {
            if (inLX && fy >= loginBtnY && fy <= loginBtnY + BTN_H) { submit(); return true; }
            if (inLX && fy >= saveBtnY  && fy <= saveBtnY  + BTN_H) { saveAccountDetails(); return true; }
            if (inLX && fy >= efBottom  && fy <= efBottom  + FIELD_H) { focusEmail = true;  return true; }
            if (inLX && fy >= pfBottom  && fy <= pfBottom  + FIELD_H) { focusEmail = false; return true; }
        }

        // Right panel (create / guest)
        int[] ra = rightAnchors(pY);
        int createBtnY = ra[0], guestBtnY = ra[1];
        int rfx = rpX + PAD, rfW = RIGHT_PANEL_W - PAD * 2;

        boolean inRX = sx >= rfx && sx <= rfx + rfW;
        if (inRX && fy >= createBtnY && fy <= createBtnY + BTN_H) { errorMessage = "Account creation coming soon."; return true; }
        if (inRX && fy >= guestBtnY  && fy <= guestBtnY  + BTN_H) { loginAsGuest(); return true; }

        return false;
    }

    private boolean handleCharSelectTouch(int sx, int fy, int w, int h) {
        int pX = (w - CHAR_PANEL_W) / 2;
        int pY = (h - CHAR_PANEL_H) / 2;
        int fW = CHAR_PANEL_W - PAD * 2;
        int fx = pX + PAD;

        // "Use different account" link at panel bottom
        if (fy >= pY + 14 && fy <= pY + 34) {
            viewState = ViewState.ACCOUNT_LOGIN;
            pendingEmail = "";
            pendingPassword = "";
            errorMessage = "";
            return true;
        }

        // Slot 0 PLAY button
        int slotY   = pY + CHAR_PANEL_H - 118;
        int playBtnX = fx + fW - CHAR_PLAY_BTN_W - 8;
        int playBtnY = slotY + (CHAR_SLOT_H - BTN_H) / 2;
        if (sx >= playBtnX && sx <= playBtnX + CHAR_PLAY_BTN_W
         && fy >= playBtnY && fy <= playBtnY + BTN_H) {
            playSelectedCharacter();
            return true;
        }

        return false;
    }

    // =========================================================================
    // Render
    // =========================================================================

    @Override
    public void render(float delta) {
        if (transitioning) return;

        if (game.getAudioManager() != null) game.getAudioManager().update(delta);

        int w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
        cursorBlink = (cursorBlink + delta) % 1.0f;
        updateHeldBackspace(delta);
        proj.setToOrtho2D(0, 0, w, h);
        sr.setProjectionMatrix(proj);

        Gdx.gl.glClearColor(0.04f, 0.04f, 0.05f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (bgTexture != null) {
            batch.setProjectionMatrix(proj);
            batch.begin();
            batch.setColor(Color.WHITE);
            batch.draw(bgTexture, 0, 0, w, h);
            batch.end();
        }

        if (viewState == ViewState.CHARACTER_SELECT) renderCharacterSelect(w, h);
        else                                         renderLoginPanels(w, h);

        renderBottomBar(w, h);
    }

    // =========================================================================
    // ACCOUNT_LOGIN — two-panel stone layout
    // =========================================================================

    private void renderLoginPanels(int w, int h) {
        int pY  = (h - LOGIN_PANEL_H) / 2;
        int lpX = (w - TOTAL_PANEL_W) / 2;
        int rpX = lpX + LOGIN_PANEL_W + PANEL_GAP;

        renderStonePanel(lpX, pY, LOGIN_PANEL_W, LOGIN_PANEL_H);
        renderStonePanel(rpX, pY, RIGHT_PANEL_W,  RIGHT_PANEL_H);

        renderLeftPanelShapes(lpX, pY);
        renderRightPanelShapes(rpX, pY);

        // ── Text pass ─────────────────────────────────────────────────────────
        batch.setProjectionMatrix(proj);
        batch.begin();
        renderLeftPanelText(lpX, pY);
        renderRightPanelText(rpX, pY);
        batch.end();
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
    }

    // ── Left panel layout constants (relative to panelY, panelH=440) ─────────
    //
    //  py+420  WELCOME title
    //  py+398  tagline (0.68x)
    //  py+374  divider
    //  py+356  LOG IN header (0.85x)
    //  py+338  divider
    //  py+318  USERNAME label (0.75x)
    //  py+276  email field   [276, 306]
    //  py+256  PASSWORD label (0.75x)
    //  py+214  password field [214, 244]
    //  py+196  Forgot password? (0.70x)
    //  py+140  LOG IN button  [140, 176]
    //  py+94   Save button    [94,  130]
    //  py+68   error message  (0.78x)
    //  py+42   hint text      (0.60x)
    //  ──────────────────────────────────────────────────────────────────────────

    /** Returns [loginBtnY, saveBtnY, efBottom, pfBottom] */
    private int[] loginAnchors(int pY) {
        return new int[]{
            pY + 140,                       // loginBtnY
            pY + 94,                        // saveBtnY
            pY + LOGIN_PANEL_H - 164,       // efBottom  = pY+276
            pY + LOGIN_PANEL_H - 226        // pfBottom  = pY+214
        };
    }

    /** Returns [createBtnY, guestBtnY] */
    private int[] rightAnchors(int pY) {
        return new int[]{
            pY + 148,   // createBtnY
            pY + 102    // guestBtnY
        };
    }

    private void renderLeftPanelShapes(int pX, int pY) {
        int fW  = LOGIN_PANEL_W - PAD * 2;
        int fx  = pX + PAD;
        int[] a = loginAnchors(pY);
        int loginBtnY = a[0], saveBtnY = a[1], efBottom = a[2], pfBottom = a[3];

        // Dividers
        divider(fx, pY + LOGIN_PANEL_H - 66,  fW);  // below WELCOME area
        divider(fx, pY + LOGIN_PANEL_H - 102, fW);  // below LOG IN header

        if (!savedAccountMode) {
            renderField(fx, efBottom, fW, focusEmail);
            renderField(fx, pfBottom, fW, !focusEmail);
        }

        colorBtn(fx, loginBtnY, fW, BTN_H, BtnStyle.GREEN);
        colorBtn(fx, saveBtnY,  fW, BTN_H, BtnStyle.STONE);
    }

    private void renderLeftPanelText(int pX, int pY) {
        int fW  = LOGIN_PANEL_W - PAD * 2;
        int fx  = pX + PAD;
        int[] a = loginAnchors(pY);
        int loginBtnY = a[0], saveBtnY = a[1], efBottom = a[2], pfBottom = a[3];

        // Title
        font.getData().setScale(1.12f);
        font.setColor(0.95f, 0.88f, 0.62f, 1f);
        centeredInPanel("WELCOME", pX, pY + LOGIN_PANEL_H - 20, LOGIN_PANEL_W);

        // Tagline
        font.getData().setScale(0.68f);
        font.setColor(0.50f, 0.46f, 0.34f, 1f);
        centeredInPanel("Log in or create an account to begin your adventure.", pX, pY + LOGIN_PANEL_H - 44, LOGIN_PANEL_W);

        // LOG IN section header
        font.getData().setScale(0.84f);
        font.setColor(0.76f, 0.68f, 0.42f, 1f);
        centeredInPanel("LOG IN", pX, pY + LOGIN_PANEL_H - 82, LOGIN_PANEL_W);

        font.getData().setScale(0.75f);

        if (!savedAccountMode) {
            // Field labels — baseline sits 10px above respective field top
            int efTop = efBottom + FIELD_H;
            int pfTop = pfBottom + FIELD_H;
            font.setColor(0.60f, 0.56f, 0.42f, 1f);
            font.draw(batch, "USERNAME", fx, efTop + 12);
            font.draw(batch, "PASSWORD", fx, pfTop + 12);

            // Input text
            String cur = cursorBlink < 0.5f ? "|" : "";
            font.getData().setScale(1f);
            font.setColor(0.95f, 0.95f, 0.95f, 1f);
            font.draw(batch, truncate(emailBuffer    + (focusEmail ? cur : ""), fW - 12), fx + 6, efBottom + FIELD_H - 8);
            font.draw(batch, "*".repeat(passwordBuffer.length()) + (!focusEmail ? cur : ""), fx + 6, pfBottom + FIELD_H - 8);

            // "Forgot password?" — below password field, amber
            font.getData().setScale(0.70f);
            font.setColor(0.80f, 0.60f, 0.16f, 1f);
            font.draw(batch, "Forgot password?", fx, pfBottom - 12);

            // Button text
            font.getData().setScale(1f);
            font.setColor(0.96f, 0.96f, 0.96f, 1f);
            centeredInBtn("LOG IN", fx, loginBtnY, fW, BTN_H);
            font.getData().setScale(0.80f);
            font.setColor(0.58f, 0.54f, 0.40f, 1f);
            centeredInBtn("Save account details", fx, saveBtnY, fW, BTN_H);
        } else {
            // Saved account mode
            font.setColor(0.56f, 0.52f, 0.38f, 1f);
            font.draw(batch, "Saved account:", fx, pY + LOGIN_PANEL_H - 122);
            font.getData().setScale(0.90f);
            font.setColor(0.92f, 0.92f, 0.92f, 1f);
            font.draw(batch, truncate(savedEmail, fW - 12), fx + 6, pY + LOGIN_PANEL_H - 144);

            // Buttons
            font.getData().setScale(0.85f);
            font.setColor(0.96f, 0.96f, 0.96f, 1f);
            centeredInBtn(truncate("Login as " + savedEmail, fW - 20), fx, loginBtnY, fW, BTN_H);
            font.getData().setScale(0.80f);
            font.setColor(0.58f, 0.54f, 0.40f, 1f);
            centeredInBtn("Use different account", fx, saveBtnY, fW, BTN_H);
        }

        font.getData().setScale(1f);

        // Error
        if (!errorMessage.isEmpty()) {
            font.getData().setScale(0.78f);
            font.setColor(0.92f, 0.28f, 0.22f, 1f);
            font.draw(batch, truncate(errorMessage, fW), fx, pY + 70);
            font.getData().setScale(1f);
        }

        // Hint
        font.getData().setScale(0.60f);
        font.setColor(0.34f, 0.31f, 0.22f, 1f);
        String hint = savedAccountMode
            ? "Enter to login  |  Click below to switch account"
            : "Tab / click to switch field  |  Enter to submit";
        font.draw(batch, hint, fx, pY + 44);
        font.getData().setScale(1f);
    }

    // ── Right panel layout ────────────────────────────────────────────────────
    //
    //  py+420  CREATE ACCOUNT title
    //  py+392  description line 1 (0.68x)
    //  py+374  description line 2 (0.68x)
    //  py+352  divider
    //  py+322  feature line 1 (0.70x)
    //  py+304  feature line 2
    //  py+286  feature line 3
    //  py+184  CREATE ACCOUNT button [184, 220]
    //  py+138  PLAY AS GUEST button  [138, 174]
    //  ──────────────────────────────────────────────────────────────────────────

    private void renderRightPanelShapes(int pX, int pY) {
        int fW  = RIGHT_PANEL_W - PAD * 2;
        int fx  = pX + PAD;
        int[] a = rightAnchors(pY);

        divider(fx, pY + RIGHT_PANEL_H - 88, fW);

        colorBtn(fx, a[0], fW, BTN_H, BtnStyle.BLUE);
        colorBtn(fx, a[1], fW, BTN_H, BtnStyle.STONE);
    }

    private void renderRightPanelText(int pX, int pY) {
        int fW = RIGHT_PANEL_W - PAD * 2;
        int fx = pX + PAD;
        int[] a = rightAnchors(pY);
        int createBtnY = a[0], guestBtnY = a[1];

        // Title
        font.getData().setScale(1.05f);
        font.setColor(0.95f, 0.88f, 0.62f, 1f);
        centeredInPanel("CREATE ACCOUNT", pX, pY + RIGHT_PANEL_H - 20, RIGHT_PANEL_W);

        // Description
        font.getData().setScale(0.68f);
        font.setColor(0.50f, 0.46f, 0.34f, 1f);
        centeredInPanel("New to Erynfall? Sign up free", pX, pY + RIGHT_PANEL_H - 44, RIGHT_PANEL_W);
        centeredInPanel("to save your characters.",       pX, pY + RIGHT_PANEL_H - 62, RIGHT_PANEL_W);

        // Features
        font.getData().setScale(0.70f);
        font.setColor(0.46f, 0.44f, 0.32f, 1f);
        int fl = pX + PAD + 10;
        font.draw(batch, "+ Save your characters & progress", fl, pY + RIGHT_PANEL_H - 102);
        font.draw(batch, "+ Join multiplayer worlds",          fl, pY + RIGHT_PANEL_H - 122);
        font.draw(batch, "+ Free to play",                     fl, pY + RIGHT_PANEL_H - 142);

        // Button labels
        font.getData().setScale(0.90f);
        font.setColor(0.92f, 0.96f, 1.00f, 1f);
        centeredInBtn("CREATE ACCOUNT", fx, createBtnY, fW, BTN_H);
        font.setColor(0.56f, 0.52f, 0.38f, 1f);
        centeredInBtn("PLAY AS GUEST", fx, guestBtnY, fW, BTN_H);

        font.getData().setScale(1f);
    }

    // =========================================================================
    // CHARACTER_SELECT
    // =========================================================================

    private void renderCharacterSelect(int w, int h) {
        int pX = (w - CHAR_PANEL_W) / 2;
        int pY = (h - CHAR_PANEL_H) / 2;
        int fW = CHAR_PANEL_W - PAD * 2;
        int fx = pX + PAD;

        renderStonePanel(pX, pY, CHAR_PANEL_W, CHAR_PANEL_H);
        divider(fx, pY + CHAR_PANEL_H - 66, fW);

        int firstSlotY = pY + CHAR_PANEL_H - 120;
        for (int i = 0; i < MAX_MEMBER_CHARS; i++) {
            int sy = firstSlotY - i * (CHAR_SLOT_H + CHAR_SLOT_GAP);
            renderCharSlotShape(fx, sy, fW, i == 0);
        }

        batch.setProjectionMatrix(proj);
        batch.begin();

        font.getData().setScale(1.12f);
        font.setColor(0.95f, 0.88f, 0.62f, 1f);
        centeredInPanel("CHOOSE CHARACTER", pX, pY + CHAR_PANEL_H - 20, CHAR_PANEL_W);

        font.getData().setScale(0.70f);
        font.setColor(0.50f, 0.46f, 0.34f, 1f);
        centeredInPanel("Select your character to enter the world", pX, pY + CHAR_PANEL_H - 44, CHAR_PANEL_W);
        font.getData().setScale(1f);

        for (int i = 0; i < MAX_MEMBER_CHARS; i++) {
            int sy = firstSlotY - i * (CHAR_SLOT_H + CHAR_SLOT_GAP);
            renderCharSlotText(fx, sy, fW, i == 0);
        }

        font.getData().setScale(0.70f);
        font.setColor(0.52f, 0.46f, 0.26f, 1f);
        GlyphLayout gl = new GlyphLayout(font, "Use different account");
        font.draw(batch, "Use different account",
            pX + (CHAR_PANEL_W - gl.width) / 2f, pY + 28);
        font.getData().setScale(1f);

        batch.end();
        font.setColor(Color.WHITE);
    }

    private void renderCharSlotShape(int fx, int sy, int fW, boolean active) {
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(active ? 0.11f : 0.08f, active ? 0.10f : 0.07f, active ? 0.08f : 0.07f, 1f);
        sr.rect(fx, sy, fW, CHAR_SLOT_H);
        sr.setColor(active ? 0.40f : 0.20f, active ? 0.34f : 0.18f, active ? 0.14f : 0.10f, 1f);
        sr.rect(fx, sy, fW, 1);
        sr.rect(fx, sy + CHAR_SLOT_H - 1, fW, 1);
        sr.rect(fx, sy, 1, CHAR_SLOT_H);
        sr.rect(fx + fW - 1, sy, 1, CHAR_SLOT_H);
        sr.setColor(active ? GOLD[0] : 0.22f, active ? GOLD[1] : 0.20f, active ? GOLD[2] : 0.16f, 1f);
        sr.rect(fx + 1, sy + 1, 3, CHAR_SLOT_H - 2);
        sr.end();

        int btnX = fx + fW - CHAR_PLAY_BTN_W - 8;
        int btnY = sy + (CHAR_SLOT_H - BTN_H) / 2;
        colorBtn(btnX, btnY, CHAR_PLAY_BTN_W, BTN_H, active ? BtnStyle.GREEN : BtnStyle.DISABLED);

        if (!active) {
            int lx = btnX + CHAR_PLAY_BTN_W / 2 - 3;
            int ly = btnY + (BTN_H - 9) / 2;
            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(0.36f, 0.36f, 0.36f, 1f);
            sr.rect(lx, ly, 6, 5);
            sr.end();
            sr.begin(ShapeRenderer.ShapeType.Line);
            sr.setColor(0.36f, 0.36f, 0.36f, 1f);
            sr.arc(lx + 3, ly + 5, 3f, 0f, 180f, 6);
            sr.end();
        }
    }

    private void renderCharSlotText(int fx, int sy, int fW, boolean active) {
        int textX  = fx + 8 + 3;
        int btnX   = fx + fW - CHAR_PLAY_BTN_W - 8;
        int maxW   = btnX - textX - 8;
        int btnY   = sy + (CHAR_SLOT_H - BTN_H) / 2;

        if (active) {
            String name = characterName.isEmpty()
                ? (pendingEmail.contains("@") ? pendingEmail.split("@")[0] : pendingEmail)
                : characterName;
            font.setColor(1f, 1f, 1f, 1f);
            font.draw(batch, truncate(name, maxW), textX, sy + CHAR_SLOT_H - 14);
            font.getData().setScale(0.72f);
            font.setColor(0.50f, 0.46f, 0.34f, 1f);
            font.draw(batch, "Lv. \u2014", textX, sy + CHAR_SLOT_H - 34);
            font.getData().setScale(1f);
            font.setColor(0.96f, 0.96f, 0.96f, 1f);
            centeredInBtn("PLAY", btnX, btnY, CHAR_PLAY_BTN_W, BTN_H);
        } else {
            font.getData().setScale(0.72f);
            font.setColor(0.36f, 0.33f, 0.24f, 1f);
            font.draw(batch, "Members Only", textX, sy + CHAR_SLOT_H - 20);
            font.getData().setScale(1f);
            font.setColor(0.38f, 0.38f, 0.38f, 1f);
            centeredInBtn("LOCKED", btnX, btnY, CHAR_PLAY_BTN_W, BTN_H);
        }
    }

    // =========================================================================
    // Bottom bar
    // =========================================================================

    private void renderBottomBar(int w, int h) {
        boolean muted = game.getAudioManager() == null || game.getAudioManager().isMusicMuted();
        int barY = BAR_MARGIN;

        // AUDIO button — bottom-left
        colorBtn(BAR_MARGIN, barY, AUDIO_BTN_W, BAR_BTN_H, muted ? BtnStyle.STONE : BtnStyle.STONE);

        // Small mute indicator dot inside AUDIO button
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(muted ? 0.70f : 0.28f, muted ? 0.22f : 0.72f, muted ? 0.18f : 0.28f, 1f);
        sr.rect(BAR_MARGIN + 6, barY + BAR_BTN_H / 2 - 4, 8, 8);
        sr.end();

        // QUIT button — bottom-right
        int qx = w - BAR_MARGIN - QUIT_BTN_W;
        colorBtn(qx, barY, QUIT_BTN_W, BAR_BTN_H, BtnStyle.RED);

        // World selector — bottom-center
        int wx = (w - WORLD_SEL_W) / 2;
        renderWorldSelector(wx, barY);

        // Text pass
        batch.setProjectionMatrix(proj);
        batch.begin();

        font.getData().setScale(0.75f);

        // AUDIO label
        font.setColor(muted ? 0.62f : 0.80f, muted ? 0.50f : 0.80f, muted ? 0.36f : 0.58f, 1f);
        // Shift label right to clear the indicator dot
        centeredInBtn(muted ? "MUTED" : "AUDIO", BAR_MARGIN + 8, barY, AUDIO_BTN_W - 8, BAR_BTN_H);

        // QUIT label
        font.setColor(0.88f, 0.70f, 0.68f, 1f);
        centeredInBtn("QUIT", qx, barY, QUIT_BTN_W, BAR_BTN_H);

        // World selector labels
        renderWorldSelectorText(wx, barY);

        font.getData().setScale(1f);
        batch.end();
        font.setColor(Color.WHITE);
    }

    private void renderWorldSelector(int wx, int wy) {
        if (worldOptions.isEmpty()) return;
        int centerW = WORLD_SEL_W - ARROW_BTN_W * 2;

        colorBtn(wx, wy, ARROW_BTN_W, BAR_BTN_H, BtnStyle.STONE);

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(PANEL_BG[0], PANEL_BG[1], PANEL_BG[2], 1f);
        sr.rect(wx + ARROW_BTN_W, wy, centerW, BAR_BTN_H);
        sr.setColor(BORDER_INNER[0], BORDER_INNER[1], BORDER_INNER[2], 1f);
        sr.rect(wx + ARROW_BTN_W, wy + BAR_BTN_H - 1, centerW, 1);
        sr.rect(wx + ARROW_BTN_W, wy, centerW, 1);
        sr.end();

        colorBtn(wx + WORLD_SEL_W - ARROW_BTN_W, wy, ARROW_BTN_W, BAR_BTN_H, BtnStyle.STONE);
    }

    private void renderWorldSelectorText(int wx, int wy) {
        if (worldOptions.isEmpty()) return;
        int centerW = WORLD_SEL_W - ARROW_BTN_W * 2;
        String label = selectedWorldIndex < worldOptions.size()
            ? worldOptions.get(selectedWorldIndex).displayLabel() : "";

        font.setColor(GOLD[0], GOLD[1], GOLD[2], 1f);
        centeredInBtn("<", wx, wy, ARROW_BTN_W, BAR_BTN_H);
        centeredInBtn(">", wx + WORLD_SEL_W - ARROW_BTN_W, wy, ARROW_BTN_W, BAR_BTN_H);

        font.setColor(0.80f, 0.74f, 0.52f, 1f);
        GlyphLayout gl = new GlyphLayout(font, label);
        font.draw(batch, label,
            wx + ARROW_BTN_W + (centerW - gl.width) / 2f,
            wy + (BAR_BTN_H + gl.height) / 2f);
    }

    // =========================================================================
    // Shared visual helpers
    // =========================================================================

    private enum BtnStyle { GREEN, BLUE, RED, STONE, DISABLED }

    private void renderStonePanel(int x, int y, int pw, int ph) {
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(PANEL_BG[0], PANEL_BG[1], PANEL_BG[2], 1f);
        sr.rect(x, y, pw, ph);
        // Outer border (3px)
        sr.setColor(BORDER_OUTER[0], BORDER_OUTER[1], BORDER_OUTER[2], 1f);
        sr.rect(x,          y,          pw, 3);
        sr.rect(x,          y + ph - 3, pw, 3);
        sr.rect(x,          y,          3,  ph);
        sr.rect(x + pw - 3, y,          3,  ph);
        // Inner highlight line
        sr.setColor(BORDER_INNER[0], BORDER_INNER[1], BORDER_INNER[2], 1f);
        sr.rect(x + 3,      y + 3,      pw - 6, 1);
        sr.rect(x + 3,      y + ph - 4, pw - 6, 1);
        sr.rect(x + 3,      y + 3,      1,      ph - 6);
        sr.rect(x + pw - 4, y + 3,      1,      ph - 6);
        sr.end();
        renderCornerBrackets(x, y, pw, ph);
    }

    private void renderCornerBrackets(int x, int y, int pw, int ph) {
        int ins = 7, len = 10;
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(GOLD[0], GOLD[1], GOLD[2], 1f);
        // BL
        sr.rect(x + ins,           y + ins, len, 1);
        sr.rect(x + ins,           y + ins, 1,   len);
        // BR
        sr.rect(x + pw - ins - len, y + ins, len, 1);
        sr.rect(x + pw - ins - 1,   y + ins, 1,   len);
        // TL
        sr.rect(x + ins,           y + ph - ins - 1,   len, 1);
        sr.rect(x + ins,           y + ph - ins - len, 1,   len);
        // TR
        sr.rect(x + pw - ins - len, y + ph - ins - 1,   len, 1);
        sr.rect(x + pw - ins - 1,   y + ph - ins - len, 1,   len);
        sr.end();
    }

    private void divider(int x, int y, int w) {
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(DIVIDER_COL[0], DIVIDER_COL[1], DIVIDER_COL[2], 1f);
        sr.rect(x, y, w, 1);
        sr.end();
    }

    private void colorBtn(int x, int y, int w, int h, BtnStyle style) {
        sr.begin(ShapeRenderer.ShapeType.Filled);
        switch (style) {
            case GREEN    -> sr.setColor(0.13f, 0.40f, 0.17f, 1f);
            case BLUE     -> sr.setColor(0.12f, 0.26f, 0.52f, 1f);
            case RED      -> sr.setColor(0.36f, 0.09f, 0.09f, 1f);
            case DISABLED -> sr.setColor(0.16f, 0.16f, 0.16f, 1f);
            default       -> sr.setColor(0.20f, 0.18f, 0.14f, 1f);  // STONE
        }
        sr.rect(x, y, w, h);
        // Top-left highlight
        switch (style) {
            case GREEN    -> sr.setColor(0.26f, 0.62f, 0.32f, 1f);
            case BLUE     -> sr.setColor(0.20f, 0.42f, 0.74f, 1f);
            case RED      -> sr.setColor(0.56f, 0.16f, 0.16f, 1f);
            case DISABLED -> sr.setColor(0.26f, 0.26f, 0.26f, 1f);
            default       -> sr.setColor(0.34f, 0.30f, 0.20f, 1f);
        }
        sr.rect(x, y + h - 1, w, 1);
        sr.rect(x, y, 1, h);
        // Bottom-right shadow
        sr.setColor(0.05f, 0.05f, 0.05f, 1f);
        sr.rect(x, y, w, 1);
        sr.rect(x + w - 1, y, 1, h);
        sr.end();
    }

    private void renderField(int x, int y, int w, boolean focused) {
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(focused ? 0.13f : 0.09f, focused ? 0.12f : 0.08f, focused ? 0.09f : 0.07f, 1f);
        sr.rect(x, y, w, FIELD_H);
        // Borders
        sr.setColor(0.06f, 0.05f, 0.04f, 1f);
        sr.rect(x, y + FIELD_H - 1, w, 1);
        sr.rect(x, y, 1, FIELD_H);
        sr.setColor(BORDER_INNER[0], BORDER_INNER[1], BORDER_INNER[2], 1f);
        sr.rect(x, y, w, 1);
        sr.rect(x + w - 1, y, 1, FIELD_H);
        // Gold top highlight when focused
        if (focused) {
            sr.setColor(GOLD[0], GOLD[1], GOLD[2], 1f);
            sr.rect(x + 1, y + FIELD_H - 1, w - 2, 1);
        }
        sr.end();
    }

    private void centeredInPanel(String text, int panelX, float baselineY, int panelW) {
        GlyphLayout gl = new GlyphLayout(font, text);
        font.draw(batch, text, panelX + (panelW - gl.width) / 2f, baselineY);
    }

    private void centeredInBtn(String text, int bx, int by, int bw, int bh) {
        GlyphLayout gl = new GlyphLayout(font, text);
        font.draw(batch, text,
            bx + (bw - gl.width) / 2f,
            by + (bh + gl.height) / 2f);
    }

    // =========================================================================
    // Input helpers
    // =========================================================================

    private void appendChar(char c) {
        if (focusEmail) {
            if (Character.isWhitespace(c)) return;
            if (emailBuffer.length() < EMAIL_MAX_LEN) emailBuffer += c;
        } else {
            if (passwordBuffer.length() < PASSWORD_MAX_LEN) passwordBuffer += c;
        }
    }

    private void deleteActiveFieldChar() {
        if (focusEmail) {
            if (!emailBuffer.isEmpty()) emailBuffer = emailBuffer.substring(0, emailBuffer.length() - 1);
        } else {
            if (!passwordBuffer.isEmpty()) passwordBuffer = passwordBuffer.substring(0, passwordBuffer.length() - 1);
        }
    }

    private void updateHeldBackspace(float delta) {
        if (!backspaceHeld || transitioning || savedAccountMode || viewState == ViewState.CHARACTER_SELECT) return;
        if (!Gdx.input.isKeyPressed(Input.Keys.BACKSPACE)) {
            backspaceHeld = false;
            backspaceHeldTime = 0f;
            backspaceRepeatAccumulator = 0f;
            return;
        }
        backspaceHeldTime += delta;
        if (backspaceHeldTime < BACKSPACE_INITIAL_DELAY) return;
        backspaceRepeatAccumulator += delta;
        while (backspaceRepeatAccumulator >= BACKSPACE_REPEAT_INTERVAL) {
            deleteActiveFieldChar();
            backspaceRepeatAccumulator -= BACKSPACE_REPEAT_INTERVAL;
        }
    }

    private boolean isPasteShortcut(int keycode) {
        boolean mod = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)
                   || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)
                   || Gdx.input.isKeyPressed(Input.Keys.SYM);
        return mod && keycode == Input.Keys.V;
    }

    private void pasteFromClipboard() {
        if (savedAccountMode || viewState == ViewState.CHARACTER_SELECT) return;
        String clip = Gdx.app.getClipboard().getContents();
        if (clip == null || clip.isBlank()) return;
        if (focusEmail) {
            String cleaned = clip.replaceAll("\\s+", "");
            int rem = EMAIL_MAX_LEN - emailBuffer.length();
            if (rem > 0) emailBuffer += cleaned.substring(0, Math.min(cleaned.length(), rem));
        } else {
            int rem = PASSWORD_MAX_LEN - passwordBuffer.length();
            if (rem > 0) passwordBuffer += clip.substring(0, Math.min(clip.length(), rem));
        }
    }

    private String truncate(String text, float maxW) {
        if (text == null || text.isEmpty()) return "";
        GlyphLayout gl = new GlyphLayout(font, text);
        if (gl.width <= maxW) return text;
        String s = text;
        while (!s.isEmpty()) {
            s = s.substring(0, s.length() - 1);
            gl.setText(font, s + "...");
            if (gl.width <= maxW) return s + "...";
        }
        return "...";
    }

    // =========================================================================
    // Actions
    // =========================================================================

    private void toggleAudio() {
        if (game.getAudioManager() != null) game.getAudioManager().toggleMute();
    }

    private void submit() {
        String email    = emailBuffer.trim();
        String password = passwordBuffer;
        if (email.isEmpty())   { errorMessage = "Please enter an email address."; return; }
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            errorMessage = "Please enter a valid email address."; return;
        }
        if (password.isEmpty()) { errorMessage = "Please enter a password."; return; }
        errorMessage    = "";
        pendingEmail    = email;
        pendingPassword = password;
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        prefs.putString(PREF_KEY_LAST_EMAIL, email);
        prefs.flush();
        viewState = ViewState.CHARACTER_SELECT;
    }

    private void loginAsGuest() {
        errorMessage    = "";
        pendingEmail    = "guest";
        pendingPassword = "";
        viewState = ViewState.CHARACTER_SELECT;
    }

    private void playSelectedCharacter() {
        if (transitioning) return;
        transitioning = true;
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        prefs.putString(PREF_KEY_SELECTED_WORLD_ID, selectedWorldOption().worldId());
        prefs.flush();
        game.startGame(pendingEmail, pendingPassword, selectedWorldOption().worldId());
        // Do NOT touch any fields after this — hide()/dispose() has already run.
    }

    private void saveAccountDetails() {
        String email    = emailBuffer.trim();
        String password = passwordBuffer;
        if (email.isEmpty())   { errorMessage = "Please enter an email before saving."; return; }
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            errorMessage = "Please enter a valid email address before saving."; return;
        }
        if (password.isEmpty()) { errorMessage = "Please enter a password before saving."; return; }
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        prefs.putString(PREF_KEY_LAST_EMAIL,    email);
        prefs.putString(PREF_KEY_SAVED_EMAIL,   email);
        prefs.putString(PREF_KEY_SAVED_PASSWORD, password);
        prefs.flush();
        savedEmail          = email;
        savedPassword       = password;
        hasSavedCredentials = true;
        savedAccountMode    = true;
        errorMessage        = "Account details saved locally.";
    }

    private void loginSavedAccount() {
        if (!hasSavedCredentials || savedEmail.isBlank() || savedPassword.isBlank()) {
            errorMessage = "No saved account details available."; return;
        }
        emailBuffer    = savedEmail;
        passwordBuffer = savedPassword;
        submit();
    }

    private void clearSavedCredentials() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        prefs.remove(PREF_KEY_SAVED_EMAIL);
        prefs.remove(PREF_KEY_SAVED_PASSWORD);
        prefs.flush();
        savedEmail          = "";
        savedPassword       = "";
        hasSavedCredentials = false;
        savedAccountMode    = false;
        passwordBuffer      = "";
        focusEmail          = true;
        errorMessage        = "Saved account details cleared.";
    }

    private void selectWorld(int index) {
        if (index < 0 || index >= worldOptions.size()) return;
        selectedWorldIndex = index;
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        prefs.putString(PREF_KEY_SELECTED_WORLD_ID, selectedWorldOption().worldId());
        prefs.flush();
        errorMessage = "";
    }

    private int indexForWorldId(String worldId) {
        String normalized = LaunchOptions.normalizeWorldId(worldId, LaunchOptions.defaultWorldId());
        for (int i = 0; i < worldOptions.size(); i++) {
            if (worldOptions.get(i).worldId().equals(normalized)) return i;
        }
        return 0;
    }

    private LaunchOptions.WorldOption selectedWorldOption() {
        if (worldOptions.isEmpty()) return new LaunchOptions.WorldOption(LaunchOptions.defaultWorldId(), "Sandbox");
        if (selectedWorldIndex < 0 || selectedWorldIndex >= worldOptions.size()) selectedWorldIndex = 0;
        return worldOptions.get(selectedWorldIndex);
    }
}
