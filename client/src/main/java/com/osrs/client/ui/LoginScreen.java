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
 * Login / character-select screen shown before GameScreen.
 *
 * Two states:
 *   ACCOUNT_LOGIN    — two-panel stone UI: LOG IN (left) + CREATE ACCOUNT (right)
 *   CHARACTER_SELECT — character slot picker after credential validation
 */
public class LoginScreen extends ScreenAdapter {

    private enum ViewState { ACCOUNT_LOGIN, CHARACTER_SELECT }

    private static final String PREFS_NAME                 = "erynfall-login";
    private static final String PREF_KEY_LAST_EMAIL        = "email";
    private static final String PREF_KEY_SAVED_EMAIL       = "saved_email";
    private static final String PREF_KEY_SAVED_PASSWORD    = "saved_password";
    private static final String PREF_KEY_SELECTED_WORLD_ID = "selected_world_id";
    private static final String PREF_KEY_CHARACTER_NAME    = "character_name";

    // ── Two-panel login layout ────────────────────────────────────────────────
    private static final int LOGIN_PANEL_W  = 360;
    private static final int LOGIN_PANEL_H  = 430;
    private static final int CREATE_PANEL_W = 250;
    private static final int CREATE_PANEL_H = 430;
    private static final int PANEL_GAP      = 5;     // gap between panels
    private static final int TOTAL_PANEL_W  = LOGIN_PANEL_W + PANEL_GAP + CREATE_PANEL_W; // 615

    // ── Character-select panel ────────────────────────────────────────────────
    private static final int CHAR_PANEL_W    = 480;
    private static final int CHAR_PANEL_H    = 400;
    private static final int CHAR_SLOT_H     = 58;
    private static final int CHAR_SLOT_GAP   = 6;
    private static final int CHAR_PLAY_BTN_W = 70;
    private static final int MAX_MEMBER_CHARS = 3;

    // ── Shared UI constants ───────────────────────────────────────────────────
    private static final int   FIELD_H          = 30;
    private static final int   BUTTON_H         = 36;
    private static final int   PAD              = 18;
    private static final int   EMAIL_MAX_LEN    = 254;
    private static final int   PASSWORD_MAX_LEN = 128;
    private static final float BACKSPACE_INITIAL_DELAY   = 0.35f;
    private static final float BACKSPACE_REPEAT_INTERVAL = 0.045f;

    // ── Bottom bar (world selector, gear, quit) ───────────────────────────────
    private static final int BOTTOM_BTN_H   = 34;
    private static final int BOTTOM_MARGIN  = 16;
    private static final int WORLD_SEL_W    = 190;  // total width of world selector widget
    private static final int WORLD_ARROW_W  = 28;   // width of each ◄/► arrow button
    private static final int GEAR_BTN_W     = 44;
    private static final int GEAR_BTN_H     = 34;
    private static final int QUIT_BTN_W     = 72;
    private static final int QUIT_BTN_H     = 34;

    // ── Mute button (top-right) ───────────────────────────────────────────────
    private static final int MUTE_BTN_W      = 68;
    private static final int MUTE_BTN_H      = 30;
    private static final int MUTE_BTN_MARGIN = 14;

    // ── Stone color palette ───────────────────────────────────────────────────
    // Panel backgrounds
    private static final float[] BG_DARK   = {0.07f, 0.07f, 0.08f};
    // Stone border outer
    private static final float[] STONE_OUTER = {0.18f, 0.16f, 0.13f};
    // Stone border inner highlight
    private static final float[] STONE_INNER = {0.32f, 0.28f, 0.20f};
    // Amber/gold accent
    private static final float[] GOLD        = {0.72f, 0.58f, 0.18f};
    private static final float[] GOLD_DIM    = {0.40f, 0.34f, 0.12f};
    // Section divider
    private static final float[] DIVIDER     = {0.22f, 0.20f, 0.16f};

    // ── Fields ────────────────────────────────────────────────────────────────
    private final ErynfallGame game;

    private BitmapFont    font;
    private SpriteBatch   batch;
    private ShapeRenderer sr;
    private Matrix4       proj;
    private Texture       backgroundTexture;

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

    private List<LaunchOptions.WorldOption> worldOptions       = List.of();
    private int                             selectedWorldIndex  = 0;

    // ── Constructors ──────────────────────────────────────────────────────────

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
        if (game.getAudioManager() != null) {
            game.getAudioManager().playLoginMusic();
        }

        FontManager.initialize();
        font  = FontManager.regular();
        batch = new SpriteBatch();
        sr    = new ShapeRenderer();
        proj  = new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        Path loginBgPath = game.getLaunchOptions().repoRootPath().resolve("art/assets/login-bg.png");
        if (loginBgPath.toFile().exists()) {
            backgroundTexture = new Texture(Gdx.files.absolute(loginBgPath.toString()));
        }

        inputProcessor = new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (transitioning) return true;

                if (keycode == Input.Keys.ENTER) {
                    if (viewState == ViewState.CHARACTER_SELECT) {
                        playSelectedCharacter();
                    } else if (savedAccountMode) {
                        loginSavedAccount();
                    } else {
                        submit();
                    }
                    return true;
                }

                if (keycode == Input.Keys.M) {
                    if (game.getAudioManager() != null) game.getAudioManager().toggleMute();
                    return true;
                }

                if (keycode == Input.Keys.TAB) {
                    if (!savedAccountMode && viewState == ViewState.ACCOUNT_LOGIN) {
                        focusEmail = !focusEmail;
                    }
                    return true;
                }

                if (keycode == Input.Keys.BACKSPACE) {
                    if (!savedAccountMode && viewState == ViewState.ACCOUNT_LOGIN) {
                        deleteActiveFieldChar();
                        backspaceHeld = true;
                        backspaceHeldTime = 0f;
                        backspaceRepeatAccumulator = 0f;
                    }
                    return true;
                }

                if (isPasteShortcut(keycode)) {
                    pasteFromClipboard();
                    return true;
                }

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
                int flippedY = h - screenY;

                // Mute button (top-right, always active)
                int muteX = w - MUTE_BTN_W - MUTE_BTN_MARGIN;
                int muteY = MUTE_BTN_MARGIN;
                if (screenX >= muteX && screenX <= muteX + MUTE_BTN_W
                 && flippedY >= muteY && flippedY <= muteY + MUTE_BTN_H) {
                    if (game.getAudioManager() != null) game.getAudioManager().toggleMute();
                    return true;
                }

                // Bottom bar hits (always active)
                if (handleBottomBarTouch(screenX, flippedY, w, h)) return true;

                if (viewState == ViewState.CHARACTER_SELECT) {
                    return handleCharSelectTouch(screenX, flippedY, w, h);
                }
                return handleLoginTouch(screenX, flippedY, w, h);
            }
        };

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

    @Override
    public void hide() {
        if (Gdx.input.getInputProcessor() == inputProcessor) Gdx.input.setInputProcessor(null);
        dispose();
    }

    @Override
    public void dispose() {
        font = null;
        if (backgroundTexture != null) { backgroundTexture.dispose(); backgroundTexture = null; }
        if (batch != null) { batch.dispose(); batch = null; }
        if (sr    != null) { sr.dispose();    sr    = null; }
    }

    // =========================================================================
    // Touch handlers
    // =========================================================================

    /** World selector arrows, gear button, quit button — always active. */
    private boolean handleBottomBarTouch(int screenX, int flippedY, int w, int h) {
        int barY = BOTTOM_MARGIN;

        // Gear button — bottom-left
        int gearX = BOTTOM_MARGIN;
        if (screenX >= gearX && screenX <= gearX + GEAR_BTN_W
         && flippedY >= barY && flippedY <= barY + GEAR_BTN_H) {
            if (game.getAudioManager() != null) game.getAudioManager().toggleMute();
            return true;
        }

        // Quit button — bottom-right
        int quitX = w - BOTTOM_MARGIN - QUIT_BTN_W;
        if (screenX >= quitX && screenX <= quitX + QUIT_BTN_W
         && flippedY >= barY && flippedY <= barY + QUIT_BTN_H) {
            Gdx.app.exit();
            return true;
        }

        // World selector — bottom-center
        if (!LaunchOptions.isRemoteServerTarget() && worldOptions.size() > 1) {
            int selX = (w - WORLD_SEL_W) / 2;
            if (flippedY >= barY && flippedY <= barY + BOTTOM_BTN_H) {
                // Left arrow
                if (screenX >= selX && screenX <= selX + WORLD_ARROW_W) {
                    selectWorld((selectedWorldIndex - 1 + worldOptions.size()) % worldOptions.size());
                    return true;
                }
                // Right arrow
                int rightArrowX = selX + WORLD_SEL_W - WORLD_ARROW_W;
                if (screenX >= rightArrowX && screenX <= rightArrowX + WORLD_ARROW_W) {
                    selectWorld((selectedWorldIndex + 1) % worldOptions.size());
                    return true;
                }
            }
        }

        return false;
    }

    private boolean handleLoginTouch(int screenX, int flippedY, int w, int h) {
        // Left panel origin
        int pX = (w - TOTAL_PANEL_W) / 2;
        int pY = (h - LOGIN_PANEL_H) / 2;

        int lx     = pX;
        int fieldW = LOGIN_PANEL_W - PAD * 2;
        int fx     = lx + PAD;

        // Right panel origin
        int rx = pX + LOGIN_PANEL_W + PANEL_GAP;

        // ── Left panel (login form) ───────────────────────────────────────────
        int[] loginY  = loginButtonYAnchors(pY);
        int loginBtnY  = loginY[0];
        int saveBtnY   = loginY[1];
        int efBottom   = loginY[2];
        int pfBottom   = loginY[3];
        // Saved-account mode buttons
        int savedPrimaryBtnY = loginBtnY;
        int savedSecBtnY     = saveBtnY;

        boolean inLoginX = screenX >= fx && screenX <= fx + fieldW;

        if (savedAccountMode) {
            if (inLoginX && flippedY >= savedPrimaryBtnY && flippedY <= savedPrimaryBtnY + BUTTON_H) {
                loginSavedAccount();
                return true;
            }
            if (inLoginX && flippedY >= savedSecBtnY && flippedY <= savedSecBtnY + BUTTON_H) {
                clearSavedCredentials();
                return true;
            }
        } else {
            if (inLoginX && flippedY >= loginBtnY && flippedY <= loginBtnY + BUTTON_H) {
                submit();
                return true;
            }
            if (inLoginX && flippedY >= saveBtnY && flippedY <= saveBtnY + BUTTON_H) {
                saveAccountDetails();
                return true;
            }
            if (inLoginX && flippedY >= efBottom && flippedY <= efBottom + FIELD_H) {
                focusEmail = true;
                return true;
            }
            if (inLoginX && flippedY >= pfBottom && flippedY <= pfBottom + FIELD_H) {
                focusEmail = false;
                return true;
            }
        }

        // ── Right panel (create account / guest) ──────────────────────────────
        int rfx    = rx + PAD;
        int rfieldW = CREATE_PANEL_W - PAD * 2;
        boolean inRightX = screenX >= rfx && screenX <= rfx + rfieldW;

        int[] rightY  = rightPanelButtonYAnchors(pY);
        int createBtnY = rightY[0];
        int guestBtnY  = rightY[1];

        if (inRightX && flippedY >= createBtnY && flippedY <= createBtnY + BUTTON_H) {
            errorMessage = "Account creation coming soon.";
            return true;
        }
        if (inRightX && flippedY >= guestBtnY && flippedY <= guestBtnY + BUTTON_H) {
            loginAsGuest();
            return true;
        }

        return false;
    }

    private boolean handleCharSelectTouch(int screenX, int flippedY, int w, int h) {
        int pX = (w - CHAR_PANEL_W) / 2;
        int pY = (h - CHAR_PANEL_H) / 2;
        int fieldW = CHAR_PANEL_W - PAD * 2;
        int fx     = pX + PAD;

        // "Use different account" link
        if (flippedY >= pY + 14 && flippedY <= pY + 32) {
            viewState       = ViewState.ACCOUNT_LOGIN;
            pendingEmail    = "";
            pendingPassword = "";
            errorMessage    = "";
            return true;
        }

        // Slot 0 PLAY button
        int firstSlotY = pY + CHAR_PANEL_H - 112;
        int playBtnX   = fx + fieldW - CHAR_PLAY_BTN_W - 6;
        int playBtnY   = firstSlotY + (CHAR_SLOT_H - BUTTON_H) / 2;
        if (screenX >= playBtnX && screenX <= playBtnX + CHAR_PLAY_BTN_W
         && flippedY >= playBtnY && flippedY <= playBtnY + BUTTON_H) {
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

        if (game.getAudioManager() != null) {
            game.getAudioManager().update(delta);
        }

        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        cursorBlink = (cursorBlink + delta) % 1.0f;
        updateHeldBackspace(delta);
        proj.setToOrtho2D(0, 0, w, h);

        Gdx.gl.glClearColor(0.04f, 0.04f, 0.05f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (backgroundTexture != null) {
            batch.setProjectionMatrix(proj);
            batch.begin();
            batch.setColor(Color.WHITE);
            batch.draw(backgroundTexture, 0, 0, w, h);
            batch.end();
        }

        sr.setProjectionMatrix(proj);

        if (viewState == ViewState.CHARACTER_SELECT) {
            renderCharacterSelect(w, h);
        } else {
            renderLoginPanels(w, h);
        }

        renderBottomBar(w, h);
        renderMuteButton(w, h);
    }

    // =========================================================================
    // ACCOUNT_LOGIN render — two-panel layout
    // =========================================================================

    private void renderLoginPanels(int w, int h) {
        int originX = (w - TOTAL_PANEL_W) / 2;
        int originY = (h - LOGIN_PANEL_H) / 2;

        // Left panel (log in)
        int lx = originX;
        int rx = originX + LOGIN_PANEL_W + PANEL_GAP;

        renderStonePanel(lx, originY, LOGIN_PANEL_W, LOGIN_PANEL_H);
        renderStonePanel(rx, originY, CREATE_PANEL_W, CREATE_PANEL_H);

        renderLeftPanelContent(lx, originY, w, h);
        renderRightPanelContent(rx, originY);
    }

    private void renderLeftPanelContent(int panelX, int panelY, int w, int h) {
        int fieldW = LOGIN_PANEL_W - PAD * 2;
        int fx     = panelX + PAD;

        // Section header separator (below title area)
        drawHorizontalDivider(panelX + PAD, panelY + LOGIN_PANEL_H - 62, fieldW);

        // LOG IN section label area separator
        int loginSectionTop = panelY + LOGIN_PANEL_H - 98;
        drawHorizontalDivider(panelX + PAD, loginSectionTop, fieldW);

        int[] anchors  = loginButtonYAnchors(panelY);
        int loginBtnY  = anchors[0];
        int saveBtnY   = anchors[1];
        int efBottom   = anchors[2];
        int pfBottom   = anchors[3];

        if (!savedAccountMode) {
            renderInputField(fx, efBottom, fieldW, focusEmail);
            renderInputField(fx, pfBottom, fieldW, !focusEmail);
            renderColorButton(fx, loginBtnY, fieldW, BUTTON_H, ButtonColor.GREEN);
            renderColorButton(fx, saveBtnY, fieldW, BUTTON_H, ButtonColor.STONE);
        } else {
            renderColorButton(fx, loginBtnY, fieldW, BUTTON_H, ButtonColor.GREEN);
            renderColorButton(fx, saveBtnY, fieldW, BUTTON_H, ButtonColor.STONE);
        }

        // ── Text ─────────────────────────────────────────────────────────────
        batch.setProjectionMatrix(sr.getProjectionMatrix());
        batch.begin();

        // Panel title: WELCOME
        font.getData().setScale(1.15f);
        font.setColor(0.95f, 0.88f, 0.65f, 1f);
        drawCenteredTextW("WELCOME", panelX, panelY + LOGIN_PANEL_H - 16, LOGIN_PANEL_W);

        // Subtitle / tagline
        font.getData().setScale(0.70f);
        font.setColor(0.52f, 0.48f, 0.36f, 1f);
        String tagline = "Log in or create an account to begin your adventure.";
        drawCenteredTextW(tagline, panelX, panelY + LOGIN_PANEL_H - 36, LOGIN_PANEL_W);

        // LOG IN section header
        font.getData().setScale(0.85f);
        font.setColor(0.78f, 0.70f, 0.45f, 1f);
        drawCenteredTextW("LOG IN", panelX, panelY + LOGIN_PANEL_H - 82, LOGIN_PANEL_W);

        font.getData().setScale(1f);

        if (!savedAccountMode) {
            // Field labels
            font.setColor(0.62f, 0.58f, 0.44f, 1f);
            font.draw(batch, "USERNAME", fx, efBottom + FIELD_H + 16);
            font.draw(batch, "PASSWORD", fx, pfBottom + FIELD_H + 16);

            // Field text (input)
            String cur = cursorBlink < 0.5f ? "|" : " ";
            font.setColor(1f, 1f, 1f, 1f);
            font.draw(batch, truncateToWidth(emailBuffer + (focusEmail ? cur : ""), fieldW - 10), fx + 6, efBottom + FIELD_H - 7);
            font.draw(batch, "*".repeat(passwordBuffer.length()) + (!focusEmail ? cur : ""), fx + 6, pfBottom + FIELD_H - 7);

            // "Forgot password?" link
            font.getData().setScale(0.72f);
            font.setColor(0.82f, 0.62f, 0.18f, 1f);
            font.draw(batch, "Forgot password?", fx, pfBottom - 4);
            font.getData().setScale(1f);

            // Button labels
            font.setColor(0.98f, 0.98f, 0.98f, 1f);
            drawCenteredTextInBtn("LOG IN", fx, loginBtnY, fieldW, BUTTON_H);
            font.setColor(0.62f, 0.58f, 0.42f, 1f);
            drawCenteredTextInBtn("Save account details", fx, saveBtnY, fieldW, BUTTON_H);
        } else {
            // Saved account mode
            font.setColor(0.58f, 0.54f, 0.40f, 1f);
            font.draw(batch, "Saved on this device:", fx, panelY + LOGIN_PANEL_H - 110);
            font.setColor(1f, 1f, 1f, 1f);
            font.draw(batch, truncateToWidth(savedEmail, fieldW - 12), fx + 6, panelY + LOGIN_PANEL_H - 132);

            font.setColor(0.98f, 0.98f, 0.98f, 1f);
            String loginLabel = truncateToWidth("Login as " + savedEmail, fieldW - 16);
            drawCenteredTextInBtn(loginLabel, fx, loginBtnY, fieldW, BUTTON_H);
            font.setColor(0.62f, 0.58f, 0.42f, 1f);
            drawCenteredTextInBtn("Use different account", fx, saveBtnY, fieldW, BUTTON_H);
        }

        // Error message
        if (!errorMessage.isEmpty()) {
            font.getData().setScale(0.80f);
            font.setColor(0.95f, 0.30f, 0.22f, 1f);
            font.draw(batch, truncateToWidth(errorMessage, fieldW), fx, panelY + 46);
            font.getData().setScale(1f);
        }

        // Hint
        font.getData().setScale(0.62f);
        font.setColor(0.36f, 0.33f, 0.24f, 1f);
        String hint = savedAccountMode ? "Enter to login  |  Click below to switch account"
                                       : "Tab / Click to switch field  |  Enter to login";
        font.draw(batch, hint, fx, panelY + 24);
        font.getData().setScale(1f);

        batch.end();
        font.setColor(Color.WHITE);
    }

    private void renderRightPanelContent(int panelX, int panelY) {
        int fieldW = CREATE_PANEL_W - PAD * 2;
        int fx     = panelX + PAD;

        drawHorizontalDivider(panelX + PAD, panelY + CREATE_PANEL_H - 62, fieldW);

        int[] anchors   = rightPanelButtonYAnchors(panelY);
        int createBtnY  = anchors[0];
        int guestBtnY   = anchors[1];

        renderColorButton(fx, createBtnY, fieldW, BUTTON_H, ButtonColor.BLUE);
        renderColorButton(fx, guestBtnY, fieldW, BUTTON_H, ButtonColor.STONE);

        batch.setProjectionMatrix(sr.getProjectionMatrix());
        batch.begin();

        // Panel title: CREATE ACCOUNT
        font.getData().setScale(1.05f);
        font.setColor(0.95f, 0.88f, 0.65f, 1f);
        drawCenteredTextW("CREATE ACCOUNT", panelX, panelY + CREATE_PANEL_H - 16, CREATE_PANEL_W);

        // Description
        font.getData().setScale(0.70f);
        font.setColor(0.52f, 0.48f, 0.36f, 1f);
        String desc1 = "New to Erynfall? Create a free";
        String desc2 = "account to save your progress.";
        GlyphLayout d1 = new GlyphLayout(font, desc1);
        GlyphLayout d2 = new GlyphLayout(font, desc2);
        font.draw(batch, desc1, panelX + (CREATE_PANEL_W - d1.width) / 2f, panelY + CREATE_PANEL_H - 76);
        font.draw(batch, desc2, panelX + (CREATE_PANEL_W - d2.width) / 2f, panelY + CREATE_PANEL_H - 92);
        font.getData().setScale(1f);

        // Button labels
        font.setColor(0.95f, 0.98f, 1.00f, 1f);
        drawCenteredTextInBtn("CREATE ACCOUNT", fx, createBtnY, fieldW, BUTTON_H);
        font.setColor(0.62f, 0.58f, 0.42f, 1f);
        drawCenteredTextInBtn("PLAY AS GUEST", fx, guestBtnY, fieldW, BUTTON_H);

        batch.end();
        font.setColor(Color.WHITE);
    }

    /** Y-anchors for the left login panel buttons/fields. Index: [0]=loginBtnY, [1]=saveBtnY, [2]=efBottom, [3]=pfBottom */
    private int[] loginButtonYAnchors(int panelY) {
        int loginBtnY = panelY + 56;
        int saveBtnY  = panelY + 14;
        int efBottom  = panelY + LOGIN_PANEL_H - 160;
        int pfBottom  = panelY + LOGIN_PANEL_H - 222;
        return new int[]{loginBtnY, saveBtnY, efBottom, pfBottom};
    }

    /** Y-anchors for right panel buttons. Index: [0]=createBtnY, [1]=guestBtnY */
    private int[] rightPanelButtonYAnchors(int panelY) {
        int createBtnY = panelY + 62;
        int guestBtnY  = panelY + 18;
        return new int[]{createBtnY, guestBtnY};
    }

    // =========================================================================
    // CHARACTER_SELECT render
    // =========================================================================

    private void renderCharacterSelect(int w, int h) {
        int panelX = (w - CHAR_PANEL_W) / 2;
        int panelY = (h - CHAR_PANEL_H) / 2;
        int fieldW = CHAR_PANEL_W - PAD * 2;
        int fx     = panelX + PAD;

        renderStonePanel(panelX, panelY, CHAR_PANEL_W, CHAR_PANEL_H);

        drawHorizontalDivider(panelX + PAD, panelY + CHAR_PANEL_H - 62, fieldW);

        int firstSlotY = panelY + CHAR_PANEL_H - 116;
        for (int i = 0; i < MAX_MEMBER_CHARS; i++) {
            int sy = firstSlotY - i * (CHAR_SLOT_H + CHAR_SLOT_GAP);
            renderCharacterSlotShape(fx, sy, fieldW, i == 0);
        }

        batch.setProjectionMatrix(sr.getProjectionMatrix());
        batch.begin();

        // Title
        font.getData().setScale(1.15f);
        font.setColor(0.95f, 0.88f, 0.65f, 1f);
        drawCenteredTextW("CHOOSE CHARACTER", panelX, panelY + CHAR_PANEL_H - 16, CHAR_PANEL_W);
        font.getData().setScale(1f);

        // Subtitle
        font.getData().setScale(0.72f);
        font.setColor(0.52f, 0.48f, 0.36f, 1f);
        drawCenteredTextW("Select your character to enter the world", panelX, panelY + CHAR_PANEL_H - 38, CHAR_PANEL_W);
        font.getData().setScale(1f);

        // Slot text
        for (int i = 0; i < MAX_MEMBER_CHARS; i++) {
            int sy = firstSlotY - i * (CHAR_SLOT_H + CHAR_SLOT_GAP);
            renderCharacterSlotText(fx, sy, fieldW, i == 0);
        }

        // "Use different account" link
        font.getData().setScale(0.72f);
        font.setColor(0.55f, 0.48f, 0.28f, 1f);
        GlyphLayout diffLayout = new GlyphLayout(font, "Use different account");
        font.draw(batch, "Use different account",
            panelX + (CHAR_PANEL_W - diffLayout.width) / 2f,
            panelY + 28);
        font.getData().setScale(1f);

        batch.end();
        font.setColor(Color.WHITE);
    }

    private void renderCharacterSlotShape(int fx, int sy, int fieldW, boolean active) {
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(active ? 0.12f : 0.08f, active ? 0.11f : 0.08f, active ? 0.08f : 0.07f, 1f);
        sr.rect(fx, sy, fieldW, CHAR_SLOT_H);
        // Border
        sr.setColor(active ? 0.42f : 0.22f, active ? 0.36f : 0.19f, active ? 0.14f : 0.10f, 1f);
        sr.rect(fx, sy, fieldW, 1);
        sr.rect(fx, sy + CHAR_SLOT_H - 1, fieldW, 1);
        sr.rect(fx, sy, 1, CHAR_SLOT_H);
        sr.rect(fx + fieldW - 1, sy, 1, CHAR_SLOT_H);
        // Left accent bar
        sr.setColor(active ? GOLD[0] : 0.22f, active ? GOLD[1] : 0.20f, active ? GOLD[2] : 0.16f, 1f);
        sr.rect(fx + 1, sy + 1, 3, CHAR_SLOT_H - 2);
        sr.end();

        int btnX = fx + fieldW - CHAR_PLAY_BTN_W - 8;
        int btnY = sy + (CHAR_SLOT_H - BUTTON_H) / 2;
        renderColorButton(btnX, btnY, CHAR_PLAY_BTN_W, BUTTON_H, active ? ButtonColor.GREEN : ButtonColor.DISABLED);

        if (!active) {
            // Padlock
            int lx = btnX + CHAR_PLAY_BTN_W / 2 - 3;
            int ly = btnY + (BUTTON_H - 9) / 2;
            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(0.38f, 0.38f, 0.38f, 1f);
            sr.rect(lx, ly, 6, 5);
            sr.end();
            sr.begin(ShapeRenderer.ShapeType.Line);
            sr.setColor(0.38f, 0.38f, 0.38f, 1f);
            sr.arc(lx + 3, ly + 5, 3f, 0f, 180f, 6);
            sr.end();
        }
    }

    private void renderCharacterSlotText(int fx, int sy, int fieldW, boolean active) {
        int textX    = fx + 8 + 3;
        int btnX     = fx + fieldW - CHAR_PLAY_BTN_W - 8;
        int maxTextW = btnX - textX - 8;

        if (active) {
            String name = characterName.isEmpty()
                ? (pendingEmail.contains("@") ? pendingEmail.split("@")[0] : pendingEmail)
                : characterName;
            font.setColor(1f, 1f, 1f, 1f);
            font.draw(batch, truncateToWidth(name, maxTextW), textX, sy + CHAR_SLOT_H - 14);
            font.getData().setScale(0.78f);
            font.setColor(0.52f, 0.48f, 0.36f, 1f);
            font.draw(batch, "Lv. \u2014", textX, sy + CHAR_SLOT_H - 32);
            font.getData().setScale(1f);

            int btnY = sy + (CHAR_SLOT_H - BUTTON_H) / 2;
            font.setColor(0.98f, 0.98f, 0.98f, 1f);
            drawCenteredTextInBtn("PLAY", btnX, btnY, CHAR_PLAY_BTN_W, BUTTON_H);
        } else {
            font.getData().setScale(0.78f);
            font.setColor(0.38f, 0.35f, 0.26f, 1f);
            font.draw(batch, "Members Only", textX, sy + CHAR_SLOT_H - 20);
            font.getData().setScale(1f);

            int btnY = sy + (CHAR_SLOT_H - BUTTON_H) / 2;
            font.setColor(0.38f, 0.38f, 0.38f, 1f);
            drawCenteredTextInBtn("LOCKED", btnX, btnY, CHAR_PLAY_BTN_W, BUTTON_H);
        }
    }

    // =========================================================================
    // Bottom bar — world selector, gear, quit
    // =========================================================================

    private void renderBottomBar(int w, int h) {
        int barY = BOTTOM_MARGIN;

        // ── Gear button — bottom-left ─────────────────────────────────────────
        int gearX = BOTTOM_MARGIN;
        renderColorButton(gearX, barY, GEAR_BTN_W, GEAR_BTN_H, ButtonColor.STONE);

        // ── Quit button — bottom-right ────────────────────────────────────────
        int quitX = w - BOTTOM_MARGIN - QUIT_BTN_W;
        renderColorButton(quitX, barY, QUIT_BTN_W, QUIT_BTN_H, ButtonColor.RED);

        // ── World selector — bottom-center ────────────────────────────────────
        int selX = (w - WORLD_SEL_W) / 2;
        renderWorldSelector(selX, barY);

        // ── Text overlay ──────────────────────────────────────────────────────
        batch.setProjectionMatrix(sr.getProjectionMatrix());
        batch.begin();

        font.getData().setScale(0.82f);

        // Gear icon (speaker symbol for mute toggle)
        boolean muted = game.getAudioManager() == null || game.getAudioManager().isMusicMuted();
        font.setColor(muted ? 0.55f : 0.82f, muted ? 0.50f : 0.78f, muted ? 0.38f : 0.55f, 1f);
        drawCenteredTextInBtn(muted ? "OFF" : "ON", gearX, barY, GEAR_BTN_W, GEAR_BTN_H);

        // Quit label
        font.setColor(0.90f, 0.72f, 0.70f, 1f);
        drawCenteredTextInBtn("QUIT", quitX, barY, QUIT_BTN_W, QUIT_BTN_H);

        // World selector text
        renderWorldSelectorText(selX, barY);

        font.getData().setScale(1f);
        batch.end();
        font.setColor(Color.WHITE);
    }

    private void renderWorldSelector(int selX, int selY) {
        if (worldOptions.isEmpty()) return;

        // Left arrow button
        renderColorButton(selX, selY, WORLD_ARROW_W, BOTTOM_BTN_H, ButtonColor.STONE);
        // Center label area
        int centerW = WORLD_SEL_W - WORLD_ARROW_W * 2;
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(BG_DARK[0], BG_DARK[1], BG_DARK[2], 1f);
        sr.rect(selX + WORLD_ARROW_W, selY, centerW, BOTTOM_BTN_H);
        // top/bottom border on center
        sr.setColor(STONE_INNER[0], STONE_INNER[1], STONE_INNER[2], 1f);
        sr.rect(selX + WORLD_ARROW_W, selY + BOTTOM_BTN_H - 1, centerW, 1);
        sr.rect(selX + WORLD_ARROW_W, selY, centerW, 1);
        sr.end();
        // Right arrow button
        renderColorButton(selX + WORLD_SEL_W - WORLD_ARROW_W, selY, WORLD_ARROW_W, BOTTOM_BTN_H, ButtonColor.STONE);
    }

    private void renderWorldSelectorText(int selX, int selY) {
        if (worldOptions.isEmpty()) return;
        String worldLabel = worldOptions.size() <= selectedWorldIndex ? "" : worldOptions.get(selectedWorldIndex).displayLabel();

        // Arrow labels
        font.setColor(GOLD[0], GOLD[1], GOLD[2], 1f);
        drawCenteredTextInBtn("<", selX, selY, WORLD_ARROW_W, BOTTOM_BTN_H);
        drawCenteredTextInBtn(">", selX + WORLD_SEL_W - WORLD_ARROW_W, selY, WORLD_ARROW_W, BOTTOM_BTN_H);

        // World label
        int centerW = WORLD_SEL_W - WORLD_ARROW_W * 2;
        int cx = selX + WORLD_ARROW_W;
        font.setColor(0.82f, 0.76f, 0.55f, 1f);
        GlyphLayout lyt = new GlyphLayout(font, worldLabel);
        font.draw(batch, worldLabel,
            cx + (centerW - lyt.width) / 2f,
            selY + (BOTTOM_BTN_H + lyt.height) / 2f);
    }

    // =========================================================================
    // Shared visual helpers
    // =========================================================================

    private enum ButtonColor { GREEN, BLUE, STONE, RED, DISABLED }

    /** Draws a stone-themed panel with double border and corner brackets. */
    private void renderStonePanel(int x, int y, int pw, int ph) {
        // Background
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(BG_DARK[0], BG_DARK[1], BG_DARK[2], 1f);
        sr.rect(x, y, pw, ph);
        sr.end();

        // Outer stone border (3px)
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(STONE_OUTER[0], STONE_OUTER[1], STONE_OUTER[2], 1f);
        sr.rect(x,          y,          pw, 3);
        sr.rect(x,          y + ph - 3, pw, 3);
        sr.rect(x,          y,          3,  ph);
        sr.rect(x + pw - 3, y,          3,  ph);
        // Inner highlight line (1px inset from outer)
        sr.setColor(STONE_INNER[0], STONE_INNER[1], STONE_INNER[2], 1f);
        sr.rect(x + 3,      y + 3,      pw - 6, 1);
        sr.rect(x + 3,      y + ph - 4, pw - 6, 1);
        sr.rect(x + 3,      y + 3,      1,      ph - 6);
        sr.rect(x + pw - 4, y + 3,      1,      ph - 6);
        sr.end();

        // Corner brackets (gold accent)
        renderCornerBrackets(x, y, pw, ph);
    }

    private void renderCornerBrackets(int x, int y, int pw, int ph) {
        int inset = 6;
        int len   = 10;
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(GOLD[0], GOLD[1], GOLD[2], 1f);
        // Bottom-left
        sr.rect(x + inset,           y + inset, len, 1);
        sr.rect(x + inset,           y + inset, 1,   len);
        // Bottom-right
        sr.rect(x + pw - inset - len, y + inset, len, 1);
        sr.rect(x + pw - inset - 1,   y + inset, 1,   len);
        // Top-left
        sr.rect(x + inset,           y + ph - inset - 1,   len, 1);
        sr.rect(x + inset,           y + ph - inset - len, 1,   len);
        // Top-right
        sr.rect(x + pw - inset - len, y + ph - inset - 1,   len, 1);
        sr.rect(x + pw - inset - 1,   y + ph - inset - len, 1,   len);
        sr.end();
    }

    private void drawHorizontalDivider(int x, int y, int width) {
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(DIVIDER[0], DIVIDER[1], DIVIDER[2], 1f);
        sr.rect(x, y, width, 1);
        sr.end();
    }

    private void renderColorButton(int x, int y, int width, int height, ButtonColor color) {
        sr.begin(ShapeRenderer.ShapeType.Filled);
        switch (color) {
            case GREEN    -> sr.setColor(0.14f, 0.42f, 0.18f, 1f);
            case BLUE     -> sr.setColor(0.12f, 0.28f, 0.55f, 1f);
            case RED      -> sr.setColor(0.38f, 0.10f, 0.10f, 1f);
            case DISABLED -> sr.setColor(0.18f, 0.18f, 0.18f, 1f);
            default       -> sr.setColor(0.22f, 0.20f, 0.16f, 1f);  // STONE
        }
        sr.rect(x, y, width, height);

        // Top highlight edge
        switch (color) {
            case GREEN    -> sr.setColor(0.28f, 0.65f, 0.34f, 1f);
            case BLUE     -> sr.setColor(0.22f, 0.45f, 0.78f, 1f);
            case RED      -> sr.setColor(0.60f, 0.18f, 0.18f, 1f);
            case DISABLED -> sr.setColor(0.28f, 0.28f, 0.28f, 1f);
            default       -> sr.setColor(0.36f, 0.32f, 0.22f, 1f);  // STONE
        }
        sr.rect(x, y + height - 1, width, 1);
        sr.rect(x, y, 1, height);

        // Bottom shadow edge
        sr.setColor(0.06f, 0.06f, 0.06f, 1f);
        sr.rect(x, y, width, 1);
        sr.rect(x + width - 1, y, 1, height);
        sr.end();
    }

    private void renderInputField(int x, int y, int width, boolean focused) {
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(focused ? 0.14f : 0.09f, focused ? 0.13f : 0.08f, focused ? 0.10f : 0.07f, 1f);
        sr.rect(x, y, width, FIELD_H);
        // Border
        sr.setColor(0.06f, 0.05f, 0.04f, 1f);
        sr.rect(x, y + FIELD_H - 1, width, 1);
        sr.rect(x, y, 1, FIELD_H);
        sr.setColor(STONE_INNER[0], STONE_INNER[1], STONE_INNER[2], 1f);
        sr.rect(x, y, width, 1);
        sr.rect(x + width - 1, y, 1, FIELD_H);
        // Focus top highlight
        if (focused) {
            sr.setColor(GOLD[0], GOLD[1], GOLD[2], 1f);
            sr.rect(x + 1, y + FIELD_H - 1, width - 2, 1);
        }
        sr.end();
    }

    private void drawCenteredTextW(String text, int panelX, float yBaseline, int panelW) {
        GlyphLayout lyt = new GlyphLayout(font, text);
        font.draw(batch, text, panelX + (panelW - lyt.width) / 2f, yBaseline);
    }

    private void drawCenteredTextInBtn(String text, int bx, int by, int bw, int bh) {
        GlyphLayout lyt = new GlyphLayout(font, text);
        font.draw(batch, text,
            bx + (bw - lyt.width) / 2f,
            by + (bh + lyt.height) / 2f);
    }

    // =========================================================================
    // Mute button (top-right)
    // =========================================================================

    private void renderMuteButton(int w, int h) {
        boolean muted = game.getAudioManager() == null || game.getAudioManager().isMusicMuted();

        int bx = w - MUTE_BTN_W - MUTE_BTN_MARGIN;
        int by = MUTE_BTN_MARGIN;
        int divX = bx + 32;
        int iconCx = bx + 16;
        int iconCy = by + MUTE_BTN_H / 2;

        sr.setProjectionMatrix(sr.getProjectionMatrix());

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(muted ? 0.14f : 0.08f, 0.07f, 0.07f, 0.92f);
        sr.rect(bx, by, MUTE_BTN_W, MUTE_BTN_H);
        sr.setColor(GOLD_DIM[0], GOLD_DIM[1], GOLD_DIM[2], 1f);
        sr.rect(bx,                  by,                  MUTE_BTN_W, 1);
        sr.rect(bx,                  by + MUTE_BTN_H - 1, MUTE_BTN_W, 1);
        sr.rect(bx,                  by,                  1, MUTE_BTN_H);
        sr.rect(bx + MUTE_BTN_W - 1, by,                  1, MUTE_BTN_H);
        sr.setColor(DIVIDER[0], DIVIDER[1], DIVIDER[2], 1f);
        sr.rect(divX, by + 3, 1, MUTE_BTN_H - 6);
        // Speaker body
        sr.setColor(muted ? 0.50f : 0.82f, muted ? 0.46f : 0.78f, muted ? 0.38f : 0.62f, 1f);
        sr.rect(iconCx - 1, iconCy - 4, 4, 8);
        sr.triangle(iconCx - 1, iconCy - 4, iconCx - 1, iconCy + 4, iconCx - 6, iconCy);
        sr.end();

        if (!muted) {
            sr.begin(ShapeRenderer.ShapeType.Line);
            sr.setColor(0.82f, 0.78f, 0.62f, 1f);
            sr.arc(iconCx + 3, iconCy, 5f, -50f, 100f, 7);
            sr.arc(iconCx + 3, iconCy, 9f, -42f, 84f, 9);
            sr.end();
        } else {
            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(0.78f, 0.16f, 0.12f, 1f);
            sr.rectLine(iconCx - 7, iconCy - 7, iconCx + 7, iconCy + 7, 2f);
            sr.end();
        }

        batch.setProjectionMatrix(sr.getProjectionMatrix());
        batch.begin();
        font.getData().setScale(0.72f);
        String stateLabel = muted ? "OFF" : "ON";
        font.setColor(muted ? 0.55f : 0.92f, muted ? 0.46f : 0.84f, muted ? 0.36f : 0.38f, 1f);
        GlyphLayout stateLyt = new GlyphLayout(font, stateLabel);
        int labelZoneW = MUTE_BTN_W - 32 - 4;
        font.draw(batch, stateLabel, divX + 4 + (labelZoneW - stateLyt.width) / 2f, by + MUTE_BTN_H - 6);
        font.getData().setScale(0.52f);
        font.setColor(0.36f, 0.32f, 0.20f, 1f);
        GlyphLayout mLyt = new GlyphLayout(font, "[M]");
        font.draw(batch, "[M]", divX + 4 + (labelZoneW - mLyt.width) / 2f, by + 10);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
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
        String clipboard = Gdx.app.getClipboard().getContents();
        if (clipboard == null || clipboard.isBlank()) return;
        if (focusEmail) {
            String cleaned = clipboard.replaceAll("\\s+", "");
            int remaining = EMAIL_MAX_LEN - emailBuffer.length();
            if (remaining > 0) emailBuffer += cleaned.substring(0, Math.min(cleaned.length(), remaining));
        } else {
            int remaining = PASSWORD_MAX_LEN - passwordBuffer.length();
            if (remaining > 0) passwordBuffer += clipboard.substring(0, Math.min(clipboard.length(), remaining));
        }
    }

    private String truncateToWidth(String text, float maxWidth) {
        if (text == null || text.isEmpty()) return "";
        GlyphLayout layout = new GlyphLayout(font, text);
        if (layout.width <= maxWidth) return text;
        String candidate = text;
        while (!candidate.isEmpty()) {
            candidate = candidate.substring(0, candidate.length() - 1);
            layout.setText(font, candidate + "...");
            if (layout.width <= maxWidth) return candidate + "...";
        }
        return "...";
    }

    // =========================================================================
    // Actions
    // =========================================================================

    private void submit() {
        String email    = emailBuffer.trim();
        String password = passwordBuffer;

        if (email.isEmpty()) { errorMessage = "Please enter an email."; return; }
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
        pendingEmail    = "guest";
        pendingPassword = "";
        errorMessage    = "";
        viewState = ViewState.CHARACTER_SELECT;
    }

    private void playSelectedCharacter() {
        if (transitioning) return;
        transitioning = true;
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        prefs.putString(PREF_KEY_SELECTED_WORLD_ID, selectedWorldOption().worldId());
        prefs.flush();
        game.startGame(pendingEmail, pendingPassword, selectedWorldOption().worldId());
        // Do NOT touch any fields after this line — hide()/dispose() has already run.
    }

    private void saveAccountDetails() {
        String email    = emailBuffer.trim();
        String password = passwordBuffer;
        if (email.isEmpty()) { errorMessage = "Please enter an email before saving."; return; }
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            errorMessage = "Please enter a valid email address before saving."; return;
        }
        if (password.isEmpty()) { errorMessage = "Please enter a password before saving."; return; }
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        prefs.putString(PREF_KEY_LAST_EMAIL, email);
        prefs.putString(PREF_KEY_SAVED_EMAIL, email);
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
