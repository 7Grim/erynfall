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
 *   ACCOUNT_LOGIN    — email + password form (or saved-account quick-login)
 *   CHARACTER_SELECT — shown after credential validation, before game launch
 */
public class LoginScreen extends ScreenAdapter {

    private enum ViewState { ACCOUNT_LOGIN, CHARACTER_SELECT }

    private static final String PREFS_NAME                 = "erynfall-login";
    private static final String PREF_KEY_LAST_EMAIL        = "email";
    private static final String PREF_KEY_SAVED_EMAIL       = "saved_email";
    private static final String PREF_KEY_SAVED_PASSWORD    = "saved_password";
    private static final String PREF_KEY_SELECTED_WORLD_ID = "selected_world_id";
    private static final String PREF_KEY_CHARACTER_NAME    = "character_name";

    // Login panel
    private static final int PANEL_W = 420;
    private static final int PANEL_H = 390;

    // Character-select panel
    private static final int CHAR_PANEL_W    = 450;
    private static final int CHAR_PANEL_H    = 380;
    private static final int CHAR_SLOT_H     = 58;
    private static final int CHAR_SLOT_GAP   = 6;
    private static final int CHAR_PLAY_BTN_W = 70;
    private static final int MAX_MEMBER_CHARS = 3;

    private static final int   FIELD_H          = 28;
    private static final int   BUTTON_H         = 34;
    private static final int   PAD              = 16;
    private static final int   WORLD_TAB_GAP    = 4;
    private static final int   EMAIL_MAX_LEN    = 254;
    private static final int   PASSWORD_MAX_LEN = 128;
    private static final float BACKSPACE_INITIAL_DELAY   = 0.35f;
    private static final float BACKSPACE_REPEAT_INTERVAL = 0.045f;

    private static final int MUTE_BTN_SZ     = 30;
    private static final int MUTE_BTN_MARGIN = 12;

    private final ErynfallGame game;

    private BitmapFont    font;
    private SpriteBatch   batch;
    private ShapeRenderer sr;
    private Matrix4       proj;
    private Texture       backgroundTexture;

    // View state
    private ViewState viewState       = ViewState.ACCOUNT_LOGIN;
    private boolean   savedAccountMode = false;

    // Login form input
    private String  emailBuffer    = "";
    private String  passwordBuffer = "";
    private boolean focusEmail     = true;

    // Credentials held between ACCOUNT_LOGIN and CHARACTER_SELECT
    private String pendingEmail    = "";
    private String pendingPassword = "";

    // Character stub (from prefs until server provides character list)
    private String characterName = "";

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

    private List<LaunchOptions.WorldOption> worldOptions   = List.of();
    private int                             selectedWorldIndex = 0;

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

                // Mute button (always active, fixed to corner)
                int muteX = w - MUTE_BTN_SZ - MUTE_BTN_MARGIN;
                int muteY = MUTE_BTN_MARGIN;
                if (screenX >= muteX && screenX <= muteX + MUTE_BTN_SZ
                 && flippedY >= muteY && flippedY <= muteY + MUTE_BTN_SZ) {
                    if (game.getAudioManager() != null) game.getAudioManager().toggleMute();
                    return true;
                }

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

    private boolean handleLoginTouch(int screenX, int flippedY, int w, int h) {
        int pX     = (w - PANEL_W) / 2;
        int pY     = (h - PANEL_H) / 2;
        int fieldW = PANEL_W - PAD * 2;
        int fx     = pX + PAD;

        int worldSelectorY   = pY + PANEL_H - 78;
        int efBottom         = pY + PANEL_H - 122;
        int pfBottom         = pY + PANEL_H - 184;
        int loginBtnY        = pY + 122;
        int saveBtnY         = pY + 80;
        int savedPrimaryBtnY = pY + 122;
        int savedSecBtnY     = pY + 80;

        boolean inX = screenX >= fx && screenX <= fx + fieldW;

        // World tab clicks
        if (!LaunchOptions.isRemoteServerTarget() && worldOptions.size() > 1) {
            if (flippedY >= worldSelectorY && flippedY <= worldSelectorY + BUTTON_H) {
                int totalGap = WORLD_TAB_GAP * (worldOptions.size() - 1);
                int tabW     = (fieldW - totalGap) / worldOptions.size();
                for (int i = 0; i < worldOptions.size(); i++) {
                    int tabX = fx + i * (tabW + WORLD_TAB_GAP);
                    if (screenX >= tabX && screenX <= tabX + tabW) {
                        selectWorld(i);
                        return true;
                    }
                }
            }
        }

        if (savedAccountMode) {
            if (inX && flippedY >= savedPrimaryBtnY && flippedY <= savedPrimaryBtnY + BUTTON_H) {
                loginSavedAccount();
                return true;
            }
            if (inX && flippedY >= savedSecBtnY && flippedY <= savedSecBtnY + BUTTON_H) {
                clearSavedCredentials();
                return true;
            }
            return false;
        }

        if (inX && flippedY >= loginBtnY && flippedY <= loginBtnY + BUTTON_H) {
            submit();
            return true;
        }
        if (inX && flippedY >= saveBtnY && flippedY <= saveBtnY + BUTTON_H) {
            saveAccountDetails();
            return true;
        }
        if (inX && flippedY >= efBottom && flippedY <= efBottom + FIELD_H) {
            focusEmail = true;
            return true;
        }
        if (inX && flippedY >= pfBottom && flippedY <= pfBottom + FIELD_H) {
            focusEmail = false;
            return true;
        }
        return false;
    }

    private boolean handleCharSelectTouch(int screenX, int flippedY, int w, int h) {
        int pX     = (w - CHAR_PANEL_W) / 2;
        int pY     = (h - CHAR_PANEL_H) / 2;
        int fieldW = CHAR_PANEL_W - PAD * 2;
        int fx     = pX + PAD;

        int worldTabY = pY + 46;

        // World tab clicks
        if (!LaunchOptions.isRemoteServerTarget() && worldOptions.size() > 1) {
            if (flippedY >= worldTabY && flippedY <= worldTabY + BUTTON_H) {
                int totalGap = WORLD_TAB_GAP * (worldOptions.size() - 1);
                int tabW     = (fieldW - totalGap) / worldOptions.size();
                for (int i = 0; i < worldOptions.size(); i++) {
                    int tabX = fx + i * (tabW + WORLD_TAB_GAP);
                    if (screenX >= tabX && screenX <= tabX + tabW) {
                        selectWorld(i);
                        return true;
                    }
                }
            }
        }

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

        Gdx.gl.glClearColor(0.06f, 0.05f, 0.05f, 1f);
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
            renderLoginPanel(w, h);
        }

        renderMuteButton(w, h);
    }

    // =========================================================================
    // ACCOUNT_LOGIN render
    // =========================================================================

    private void renderLoginPanel(int w, int h) {
        int panelX = (w - PANEL_W) / 2;
        int panelY = (h - PANEL_H) / 2;
        int fieldW = PANEL_W - PAD * 2;
        int fx     = panelX + PAD;

        // Y anchors (all from panelY bottom)
        int worldSelectorY   = panelY + PANEL_H - 78;
        int efBottom         = panelY + PANEL_H - 122;
        int pfBottom         = panelY + PANEL_H - 184;
        int loginBtnY        = panelY + 122;
        int saveBtnY         = panelY + 80;
        int savedPrimaryBtnY = panelY + 122;
        int savedSecBtnY     = panelY + 80;

        // Panel background
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.09f, 0.07f, 0.05f, 1f);
        sr.rect(panelX, panelY, PANEL_W, PANEL_H);
        sr.end();

        // Double gold border
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.38f, 0.32f, 0.13f, 1f);
        sr.rect(panelX,               panelY,               PANEL_W, 2);
        sr.rect(panelX,               panelY + PANEL_H - 2, PANEL_W, 2);
        sr.rect(panelX,               panelY,               2,       PANEL_H);
        sr.rect(panelX + PANEL_W - 2, panelY,               2,       PANEL_H);
        sr.setColor(0.72f, 0.62f, 0.26f, 1f);
        sr.rect(panelX + 2,               panelY + 2,               PANEL_W - 4, 1);
        sr.rect(panelX + 2,               panelY + PANEL_H - 3,     PANEL_W - 4, 1);
        sr.rect(panelX + 2,               panelY + 2,               1,           PANEL_H - 4);
        sr.rect(panelX + PANEL_W - 3,     panelY + 2,               1,           PANEL_H - 4);
        sr.end();

        renderCornerBrackets(panelX, panelY, PANEL_W, PANEL_H);

        // Separator below title
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.38f, 0.32f, 0.13f, 1f);
        sr.rect(panelX + PAD, panelY + PANEL_H - 46, PANEL_W - PAD * 2, 1);
        sr.end();

        // World tabs (shapes only)
        renderWorldTabShapes(fx, worldSelectorY, fieldW);

        if (!savedAccountMode) {
            renderInputFields(fx, efBottom, pfBottom, fieldW);
            renderButton(fx, loginBtnY, fieldW, BUTTON_H, true, false);
            renderButton(fx, saveBtnY,  fieldW, BUTTON_H, true, true);
        } else {
            renderButton(fx, savedPrimaryBtnY, fieldW, BUTTON_H, true, false);
            renderButton(fx, savedSecBtnY,     fieldW, BUTTON_H, true, true);
        }

        // Text layer
        batch.setProjectionMatrix(proj);
        batch.begin();

        // Title
        font.setColor(0.96f, 0.82f, 0.10f, 1f);
        GlyphLayout titleLayout = new GlyphLayout(font, "ERYNFALL");
        font.draw(batch, "ERYNFALL",
            panelX + (PANEL_W - titleLayout.width) / 2f,
            panelY + PANEL_H - 14);

        // Subtitle
        font.setColor(0.58f, 0.50f, 0.32f, 1f);
        String subtitle = savedAccountMode ? "Saved account" : "Enter the Frontier";
        GlyphLayout subLayout = new GlyphLayout(font, subtitle);
        font.draw(batch, subtitle,
            panelX + (PANEL_W - subLayout.width) / 2f,
            panelY + PANEL_H - 30);

        // World label + tab text
        font.setColor(0.78f, 0.72f, 0.58f, 1f);
        font.draw(batch, "World:", fx, worldSelectorY + BUTTON_H + 14);
        renderWorldTabText(fx, worldSelectorY, fieldW);

        if (!savedAccountMode) {
            // Field labels
            font.setColor(0.78f, 0.72f, 0.58f, 1f);
            font.draw(batch, "Email address:", fx, efBottom + FIELD_H + 14);
            font.draw(batch, "Password:", fx, pfBottom + FIELD_H + 14);

            // Field text
            String cur = cursorBlink < 0.5f ? "|" : " ";
            font.setColor(1f, 1f, 1f, 1f);
            font.draw(batch, emailBuffer + (focusEmail ? cur : ""), fx + 6, efBottom + FIELD_H - 8);
            font.draw(batch, "*".repeat(passwordBuffer.length()) + (!focusEmail ? cur : ""), fx + 6, pfBottom + FIELD_H - 8);

            // Button labels
            font.setColor(0.98f, 0.94f, 0.80f, 1f);
            drawCenteredText("Login", fx, loginBtnY, fieldW, BUTTON_H);
            font.setColor(0.80f, 0.74f, 0.58f, 1f);
            drawCenteredText("Save account details", fx, saveBtnY, fieldW, BUTTON_H);
        } else {
            font.setColor(0.78f, 0.72f, 0.58f, 1f);
            font.draw(batch, "Saved locally on this device:", fx, panelY + PANEL_H - 90);
            font.setColor(1f, 1f, 1f, 1f);
            font.draw(batch, truncateToWidth(savedEmail, fieldW - 12), fx + 6, panelY + PANEL_H - 112);

            font.setColor(0.98f, 0.94f, 0.80f, 1f);
            String loginAsLabel = "Login as " + savedEmail;
            drawCenteredText(truncateToWidth(loginAsLabel, fieldW - 12), fx, savedPrimaryBtnY, fieldW, BUTTON_H);
            font.setColor(0.80f, 0.74f, 0.58f, 1f);
            drawCenteredText("Use different account", fx, savedSecBtnY, fieldW, BUTTON_H);
        }

        if (!errorMessage.isEmpty()) {
            font.setColor(0.95f, 0.28f, 0.22f, 1f);
            font.draw(batch, truncateToWidth(errorMessage, fieldW), fx, panelY + 52);
        }

        font.setColor(0.45f, 0.38f, 0.25f, 1f);
        String hint = savedAccountMode
            ? "Enter to login   |   Click below to switch account"
            : "Tab  |  Click to focus  |  Enter to login";
        font.draw(batch, truncateToWidth(hint, fieldW), fx, panelY + 28);

        batch.end();
        font.setColor(Color.WHITE);
    }

    private void renderInputFields(int fx, int efBottom, int pfBottom, int fieldW) {
        sr.begin(ShapeRenderer.ShapeType.Filled);
        float[] eBg = focusEmail ? new float[]{0.16f, 0.13f, 0.08f} : new float[]{0.09f, 0.07f, 0.04f};
        sr.setColor(eBg[0], eBg[1], eBg[2], 1f);
        sr.rect(fx, efBottom, fieldW, FIELD_H);
        float[] pBg = !focusEmail ? new float[]{0.16f, 0.13f, 0.08f} : new float[]{0.09f, 0.07f, 0.04f};
        sr.setColor(pBg[0], pBg[1], pBg[2], 1f);
        sr.rect(fx, pfBottom, fieldW, FIELD_H);
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.05f, 0.04f, 0.02f, 1f);
        sr.rect(fx, efBottom + FIELD_H - 1, fieldW, 1);
        sr.rect(fx, efBottom, 1, FIELD_H);
        sr.setColor(0.36f, 0.30f, 0.16f, 1f);
        sr.rect(fx, efBottom, fieldW, 1);
        sr.rect(fx + fieldW - 1, efBottom, 1, FIELD_H);
        if (focusEmail) {
            sr.setColor(0.72f, 0.62f, 0.26f, 1f);
            sr.rect(fx + 1, efBottom + FIELD_H - 1, fieldW - 2, 1);
        }
        sr.setColor(0.05f, 0.04f, 0.02f, 1f);
        sr.rect(fx, pfBottom + FIELD_H - 1, fieldW, 1);
        sr.rect(fx, pfBottom, 1, FIELD_H);
        sr.setColor(0.36f, 0.30f, 0.16f, 1f);
        sr.rect(fx, pfBottom, fieldW, 1);
        sr.rect(fx + fieldW - 1, pfBottom, 1, FIELD_H);
        if (!focusEmail) {
            sr.setColor(0.72f, 0.62f, 0.26f, 1f);
            sr.rect(fx + 1, pfBottom + FIELD_H - 1, fieldW - 2, 1);
        }
        sr.end();
    }

    // =========================================================================
    // CHARACTER_SELECT render
    // =========================================================================

    private void renderCharacterSelect(int w, int h) {
        int panelX = (w - CHAR_PANEL_W) / 2;
        int panelY = (h - CHAR_PANEL_H) / 2;
        int fieldW = CHAR_PANEL_W - PAD * 2;
        int fx     = panelX + PAD;

        int worldTabY  = panelY + 46;
        // firstSlotY = bottom of slot 0 (top slot on screen)
        // Separator at panelY + CHAR_PANEL_H - 46 = panelY + 334
        // Slot 0 bottom = 334 - 8 - CHAR_SLOT_H = 334 - 66 = 268 => panelY + 268
        int firstSlotY = panelY + CHAR_PANEL_H - 112;  // = panelY + 268

        // Panel background
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.09f, 0.07f, 0.05f, 1f);
        sr.rect(panelX, panelY, CHAR_PANEL_W, CHAR_PANEL_H);
        sr.end();

        // Double gold border
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.38f, 0.32f, 0.13f, 1f);
        sr.rect(panelX,                   panelY,                   CHAR_PANEL_W, 2);
        sr.rect(panelX,                   panelY + CHAR_PANEL_H - 2, CHAR_PANEL_W, 2);
        sr.rect(panelX,                   panelY,                   2,            CHAR_PANEL_H);
        sr.rect(panelX + CHAR_PANEL_W - 2, panelY,                  2,            CHAR_PANEL_H);
        sr.setColor(0.72f, 0.62f, 0.26f, 1f);
        sr.rect(panelX + 2,                   panelY + 2,                   CHAR_PANEL_W - 4, 1);
        sr.rect(panelX + 2,                   panelY + CHAR_PANEL_H - 3,   CHAR_PANEL_W - 4, 1);
        sr.rect(panelX + 2,                   panelY + 2,                   1,                CHAR_PANEL_H - 4);
        sr.rect(panelX + CHAR_PANEL_W - 3,    panelY + 2,                   1,                CHAR_PANEL_H - 4);
        sr.end();

        renderCornerBrackets(panelX, panelY, CHAR_PANEL_W, CHAR_PANEL_H);

        // Separator
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.38f, 0.32f, 0.13f, 1f);
        sr.rect(panelX + PAD, panelY + CHAR_PANEL_H - 46, CHAR_PANEL_W - PAD * 2, 1);
        sr.end();

        // World tabs
        renderWorldTabShapes(fx, worldTabY, fieldW);

        // Character slot shapes
        for (int i = 0; i < MAX_MEMBER_CHARS; i++) {
            int sy = firstSlotY - i * (CHAR_SLOT_H + CHAR_SLOT_GAP);
            renderCharacterSlotShape(fx, sy, fieldW, i == 0);
        }

        // Text layer
        batch.setProjectionMatrix(proj);
        batch.begin();

        // Title
        font.setColor(0.96f, 0.82f, 0.10f, 1f);
        GlyphLayout titleLayout = new GlyphLayout(font, "ERYNFALL");
        font.draw(batch, "ERYNFALL",
            panelX + (CHAR_PANEL_W - titleLayout.width) / 2f,
            panelY + CHAR_PANEL_H - 14);

        // Subtitle
        font.setColor(0.58f, 0.50f, 0.32f, 1f);
        GlyphLayout subLayout = new GlyphLayout(font, "Choose Your Character");
        font.draw(batch, "Choose Your Character",
            panelX + (CHAR_PANEL_W - subLayout.width) / 2f,
            panelY + CHAR_PANEL_H - 30);

        // World label + tab text
        font.setColor(0.78f, 0.72f, 0.58f, 1f);
        font.draw(batch, "World:", fx, worldTabY + BUTTON_H + 14);
        renderWorldTabText(fx, worldTabY, fieldW);

        // Slot text
        for (int i = 0; i < MAX_MEMBER_CHARS; i++) {
            int sy = firstSlotY - i * (CHAR_SLOT_H + CHAR_SLOT_GAP);
            renderCharacterSlotText(fx, sy, fieldW, i == 0);
        }

        // "Use different account" link
        font.setColor(0.45f, 0.38f, 0.25f, 1f);
        GlyphLayout diffLayout = new GlyphLayout(font, "Use different account");
        font.draw(batch, "Use different account",
            panelX + (CHAR_PANEL_W - diffLayout.width) / 2f,
            panelY + 26);

        batch.end();
        font.setColor(Color.WHITE);
    }

    private void renderCharacterSlotShape(int fx, int sy, int fieldW, boolean active) {
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(active ? 0.13f : 0.09f, active ? 0.11f : 0.08f, active ? 0.07f : 0.06f, 1f);
        sr.rect(fx, sy, fieldW, CHAR_SLOT_H);

        // Border
        sr.setColor(active ? 0.55f : 0.30f, active ? 0.46f : 0.26f, active ? 0.18f : 0.12f, 1f);
        sr.rect(fx, sy, fieldW, 1);
        sr.rect(fx, sy + CHAR_SLOT_H - 1, fieldW, 1);
        sr.rect(fx, sy, 1, CHAR_SLOT_H);
        sr.rect(fx + fieldW - 1, sy, 1, CHAR_SLOT_H);

        // Left accent bar
        sr.setColor(active ? 0.80f : 0.25f, active ? 0.38f : 0.22f, active ? 0.12f : 0.18f, 1f);
        sr.rect(fx + 1, sy + 1, 3, CHAR_SLOT_H - 2);
        sr.end();

        int btnX = fx + fieldW - CHAR_PLAY_BTN_W - 6;
        int btnY = sy + (CHAR_SLOT_H - BUTTON_H) / 2;
        renderButton(btnX, btnY, CHAR_PLAY_BTN_W, BUTTON_H, active, false);

        if (!active) {
            // Padlock body + shackle arc
            int lx = btnX + CHAR_PLAY_BTN_W / 2 - 3;
            int ly = btnY + (BUTTON_H - 9) / 2;
            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(0.40f, 0.40f, 0.40f, 1f);
            sr.rect(lx, ly, 6, 5);
            sr.end();
            sr.begin(ShapeRenderer.ShapeType.Line);
            sr.setColor(0.40f, 0.40f, 0.40f, 1f);
            sr.arc(lx + 3, ly + 5, 3f, 0f, 180f, 6);
            sr.end();
        }
    }

    private void renderCharacterSlotText(int fx, int sy, int fieldW, boolean active) {
        int textX    = fx + 8 + 3;
        int btnX     = fx + fieldW - CHAR_PLAY_BTN_W - 6;
        int maxTextW = btnX - textX - 8;

        if (active) {
            String name = characterName.isEmpty()
                ? (pendingEmail.contains("@") ? pendingEmail.split("@")[0] : pendingEmail)
                : characterName;
            font.setColor(1f, 1f, 1f, 1f);
            font.draw(batch, truncateToWidth(name, maxTextW), textX, sy + CHAR_SLOT_H - 14);
            font.setColor(0.55f, 0.50f, 0.38f, 1f);
            font.draw(batch, "Lv. \u2014", textX, sy + CHAR_SLOT_H - 32);

            int btnY = sy + (CHAR_SLOT_H - BUTTON_H) / 2;
            font.setColor(0.98f, 0.94f, 0.80f, 1f);
            GlyphLayout lyt = new GlyphLayout(font, "PLAY");
            font.draw(batch, "PLAY",
                btnX + (CHAR_PLAY_BTN_W - lyt.width) / 2f,
                btnY + (BUTTON_H + lyt.height) / 2f);
        } else {
            font.setColor(0.40f, 0.37f, 0.28f, 1f);
            font.draw(batch, "Members Only", textX, sy + CHAR_SLOT_H - 20);

            int btnY = sy + (CHAR_SLOT_H - BUTTON_H) / 2;
            font.setColor(0.40f, 0.40f, 0.40f, 1f);
            GlyphLayout lyt = new GlyphLayout(font, "LOCKED");
            font.draw(batch, "LOCKED",
                btnX + (CHAR_PLAY_BTN_W - lyt.width) / 2f,
                btnY + (BUTTON_H + lyt.height) / 2f);
        }
    }

    // =========================================================================
    // World tab helpers
    // =========================================================================

    private void renderWorldTabShapes(int fx, int tabRowY, int fieldW) {
        if (LaunchOptions.isRemoteServerTarget() || worldOptions.size() <= 1) return;

        int totalGap = WORLD_TAB_GAP * (worldOptions.size() - 1);
        int tabW     = (fieldW - totalGap) / worldOptions.size();

        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < worldOptions.size(); i++) {
            boolean active = i == selectedWorldIndex;
            int tabX = fx + i * (tabW + WORLD_TAB_GAP);
            sr.setColor(active ? 0.52f : 0.20f, active ? 0.42f : 0.17f, active ? 0.18f : 0.10f, 1f);
            sr.rect(tabX, tabRowY, tabW, BUTTON_H);
            // Top accent line
            sr.setColor(active ? 0.80f : 0.36f, active ? 0.68f : 0.30f, active ? 0.30f : 0.16f, 1f);
            sr.rect(tabX, tabRowY + BUTTON_H - 2, tabW, 2);
        }
        sr.end();
    }

    private void renderWorldTabText(int fx, int tabRowY, int fieldW) {
        if (worldOptions.isEmpty()) return;

        if (LaunchOptions.isRemoteServerTarget() || worldOptions.size() == 1) {
            String lbl = worldOptions.isEmpty() ? "" : worldOptions.get(0).displayLabel();
            font.setColor(0.78f, 0.72f, 0.58f, 1f);
            GlyphLayout lyt = new GlyphLayout(font, lbl);
            font.draw(batch, lbl, fx, tabRowY + (BUTTON_H + lyt.height) / 2f);
            return;
        }

        int totalGap = WORLD_TAB_GAP * (worldOptions.size() - 1);
        int tabW     = (fieldW - totalGap) / worldOptions.size();

        for (int i = 0; i < worldOptions.size(); i++) {
            boolean active = i == selectedWorldIndex;
            int tabX  = fx + i * (tabW + WORLD_TAB_GAP);
            String lbl = truncateToWidth(worldOptions.get(i).displayLabel(), tabW - 8);
            GlyphLayout lyt = new GlyphLayout(font, lbl);
            font.setColor(active ? 1f : 0.60f, active ? 1f : 0.55f, active ? 1f : 0.40f, 1f);
            font.draw(batch, lbl,
                tabX + (tabW - lyt.width) / 2f,
                tabRowY + (BUTTON_H + lyt.height) / 2f);
        }
    }

    // =========================================================================
    // Shared visual helpers
    // =========================================================================

    private void renderCornerBrackets(int panelX, int panelY, int panelW, int panelH) {
        int inset = 4;
        int len   = 6;
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.72f, 0.62f, 0.26f, 1f);
        // Bottom-left
        sr.rect(panelX + inset,           panelY + inset, len, 1);
        sr.rect(panelX + inset,           panelY + inset, 1,   len);
        // Bottom-right
        sr.rect(panelX + panelW - inset - len, panelY + inset, len, 1);
        sr.rect(panelX + panelW - inset - 1,   panelY + inset, 1,   len);
        // Top-left
        sr.rect(panelX + inset,           panelY + panelH - inset - 1,   len, 1);
        sr.rect(panelX + inset,           panelY + panelH - inset - len, 1,   len);
        // Top-right
        sr.rect(panelX + panelW - inset - len, panelY + panelH - inset - 1,   len, 1);
        sr.rect(panelX + panelW - inset - 1,   panelY + panelH - inset - len, 1,   len);
        sr.end();
    }

    /** Primary button when secondary=false, dimmer secondary style when secondary=true. */
    private void renderButton(int x, int y, int width, int height, boolean enabled, boolean secondary) {
        sr.begin(ShapeRenderer.ShapeType.Filled);
        if (!enabled) {
            sr.setColor(0.22f, 0.22f, 0.22f, 1f);
        } else if (secondary) {
            sr.setColor(0.28f, 0.24f, 0.14f, 1f);
        } else {
            sr.setColor(0.45f, 0.37f, 0.20f, 1f);
        }
        sr.rect(x, y, width, height);

        // Highlight top + left edge
        if (enabled) {
            sr.setColor(secondary ? 0.44f : 0.65f, secondary ? 0.38f : 0.55f, secondary ? 0.22f : 0.30f, 1f);
        } else {
            sr.setColor(0.36f, 0.36f, 0.36f, 1f);
        }
        sr.rect(x, y + height - 1, width, 1);
        sr.rect(x, y, 1, height);

        // Shadow bottom + right edge
        sr.setColor(enabled ? 0.22f : 0.12f, enabled ? 0.18f : 0.12f, enabled ? 0.10f : 0.12f, 1f);
        sr.rect(x, y, width, 1);
        sr.rect(x + width - 1, y, 1, height);
        sr.end();
    }

    private void drawCenteredText(String text, int fx, int btnY, int fieldW, int btnH) {
        GlyphLayout lyt = new GlyphLayout(font, text);
        font.draw(batch, text,
            fx + (fieldW - lyt.width) / 2f,
            btnY + (btnH + lyt.height) / 2f);
    }

    // =========================================================================
    // Mute button
    // =========================================================================

    private void renderMuteButton(int w, int h) {
        boolean muted = game.getAudioManager() == null || game.getAudioManager().isMusicMuted();
        int bx = w - MUTE_BTN_SZ - MUTE_BTN_MARGIN;
        int by = MUTE_BTN_MARGIN;
        int cx = bx + MUTE_BTN_SZ / 2;
        int cy = by + MUTE_BTN_SZ / 2;

        sr.setProjectionMatrix(proj);

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(muted ? 0.18f : 0.08f, muted ? 0.08f : 0.07f, 0.06f, 0.92f);
        sr.rect(bx, by, MUTE_BTN_SZ, MUTE_BTN_SZ);
        sr.setColor(0.55f, 0.46f, 0.18f, 1f);
        sr.rect(bx,                    by,                    MUTE_BTN_SZ, 1);
        sr.rect(bx,                    by + MUTE_BTN_SZ - 1, MUTE_BTN_SZ, 1);
        sr.rect(bx,                    by,                    1, MUTE_BTN_SZ);
        sr.rect(bx + MUTE_BTN_SZ - 1, by,                    1, MUTE_BTN_SZ);
        sr.setColor(muted ? 0.55f : 0.85f, muted ? 0.52f : 0.82f, muted ? 0.45f : 0.70f, 1f);
        sr.rect(cx - 1, cy - 4, 4, 8);
        sr.triangle(cx - 1, cy - 4, cx - 1, cy + 4, cx - 6, cy);
        sr.end();

        if (!muted) {
            sr.begin(ShapeRenderer.ShapeType.Line);
            sr.setColor(0.85f, 0.82f, 0.70f, 1f);
            sr.arc(cx + 3, cy, 5f, -50f, 100f, 7);
            sr.arc(cx + 3, cy, 9f, -42f, 84f, 9);
            sr.end();
        } else {
            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(0.82f, 0.18f, 0.14f, 1f);
            sr.rectLine(cx - 7, cy - 7, cx + 7, cy + 7, 2f);
            sr.end();
        }

        batch.setProjectionMatrix(proj);
        batch.begin();
        font.getData().setScale(0.60f);
        font.setColor(0.35f, 0.32f, 0.24f, 1f);
        font.draw(batch, "[M]", bx + 4, by - 2);
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
