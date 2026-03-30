package com.osrs.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.osrs.client.ErynfallGame;

/**
 * Login / registration screen shown before GameScreen.
 *
 * Layout: dark background, centred panel, two input fields (email / password),
 * Tab to switch fields, Enter to submit, error message line at bottom.
 *
 * Email rules: valid email address.
 * Password: any chars, asterisked display.
 */
public class LoginScreen extends ScreenAdapter {

    private static final int PANEL_W         = 400;
    private static final int PANEL_H         = 290;
    private static final int FIELD_H         = 28;
    private static final int BUTTON_H        = 34;
    private static final int PAD             = 16;
    private static final int EMAIL_MAX_LEN    = 254;
    private static final int PASSWORD_MAX_LEN = 128;

    private final ErynfallGame game;

    private BitmapFont    font;
    private SpriteBatch   batch;
    private ShapeRenderer sr;
    private Matrix4       proj;

    // Input state
    private String emailBuffer = "";
    private String passwordBuffer = "";
    private boolean focusEmail = true;   // false = password field focused

    private String errorMessage = "";
    private float   cursorBlink  = 0f;
    private boolean transitioning = false;  // set true when screen switch is in flight
    private InputAdapter inputProcessor;

    public LoginScreen(ErynfallGame game) {
        this(game, "");
    }

    public LoginScreen(ErynfallGame game, String initialErrorMessage) {
        this.game = game;
        this.errorMessage = initialErrorMessage == null ? "" : initialErrorMessage;
    }

    @Override
    public void show() {
        font  = new BitmapFont();
        batch = new SpriteBatch();
        sr    = new ShapeRenderer();
        proj  = new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        inputProcessor = new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (transitioning) {
                    return true;
                }

                if (keycode == Input.Keys.ENTER) {
                    submit();
                    return true;
                }

                if (keycode == Input.Keys.TAB) {
                    focusEmail = !focusEmail;
                    return true;
                }

                if (keycode == Input.Keys.BACKSPACE) {
                    if (focusEmail) {
                        if (!emailBuffer.isEmpty()) {
                            emailBuffer = emailBuffer.substring(0, emailBuffer.length() - 1);
                        }
                    } else {
                        if (!passwordBuffer.isEmpty()) {
                            passwordBuffer = passwordBuffer.substring(0, passwordBuffer.length() - 1);
                        }
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
                if (transitioning) {
                    return true;
                }
                if (character < 32 || character == 127) {
                    return false;
                }
                appendChar(character);
                return true;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (button != Input.Buttons.LEFT || transitioning) return false;
                int w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
                int pX = (w - PANEL_W) / 2;
                int pY = (h - PANEL_H) / 2;
                int flippedY = h - screenY;           // LibGDX Y=0 is at bottom
                int fieldW   = PANEL_W - PAD * 2;
                int btnY     = pY + 70;               // login button rect bottom -- must match render()
                int efBottom = pY + PANEL_H - 100;    // email field rect bottom -- must match render()
                int pfBottom = pY + PANEL_H - 160;    // password field rect bottom -- must match render()
                boolean inX  = screenX >= pX + PAD && screenX <= pX + PAD + fieldW;

                // Click login button
                if (inX && flippedY >= btnY && flippedY <= btnY + BUTTON_H) {
                    submit();
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
        };

        // Pre-fill email from last successful login
        Preferences prefs = Gdx.app.getPreferences("erynfall-login");
        String savedEmail = prefs.getString("email", "");
        if (!savedEmail.isBlank()) {
            emailBuffer = savedEmail;
            focusEmail  = false;  // saved email pre-filled -- jump cursor to password field
        }

        Gdx.input.setInputProcessor(inputProcessor);
    }

    @Override
    public void render(float delta) {
        if (transitioning) return;

        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        cursorBlink = (cursorBlink + delta) % 1.0f;

        // Update projection for current size
        proj.setToOrtho2D(0, 0, w, h);

        // -- Background --
        Gdx.gl.glClearColor(0.04f, 0.04f, 0.06f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        int panelX = (w - PANEL_W) / 2;
        int panelY = (h - PANEL_H) / 2;
        int fieldW = PANEL_W - PAD * 2;
        int fx     = panelX + PAD;           // field rect X

        // Field rect bottoms (Y of the bottom-left corner of each field rect)
        int efBottom = panelY + PANEL_H - 100;   // email field
        int pfBottom = panelY + PANEL_H - 160;   // password field
        int btnY     = panelY + 70;              // login button

        sr.setProjectionMatrix(proj);

        // -- Panel background --
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.10f, 0.08f, 0.05f, 1f);
        sr.rect(panelX, panelY, PANEL_W, PANEL_H);
        sr.end();

        // -- Double gold border (outer dark gold, inner bright gold) --
        sr.begin(ShapeRenderer.ShapeType.Filled);
        // Outer border -- dark gold, 2px
        sr.setColor(0.38f, 0.32f, 0.13f, 1f);
        sr.rect(panelX,              panelY,              PANEL_W, 2);
        sr.rect(panelX,              panelY + PANEL_H - 2, PANEL_W, 2);
        sr.rect(panelX,              panelY,              2,       PANEL_H);
        sr.rect(panelX + PANEL_W - 2, panelY,             2,       PANEL_H);
        // Inner border -- bright OSRS gold, 1px inset
        sr.setColor(0.72f, 0.62f, 0.26f, 1f);
        sr.rect(panelX + 2,              panelY + 2,              PANEL_W - 4, 1);
        sr.rect(panelX + 2,              panelY + PANEL_H - 3,    PANEL_W - 4, 1);
        sr.rect(panelX + 2,              panelY + 2,              1,           PANEL_H - 4);
        sr.rect(panelX + PANEL_W - 3,   panelY + 2,              1,           PANEL_H - 4);
        sr.end();

        // -- Separator line below title area --
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.38f, 0.32f, 0.13f, 1f);
        sr.rect(panelX + PAD, panelY + PANEL_H - 40, PANEL_W - PAD * 2, 1);
        sr.end();

        // -- Input field backgrounds with inset bevel --
        sr.begin(ShapeRenderer.ShapeType.Filled);
        // Email field
        float[] emailBg = focusEmail
            ? new float[]{0.16f, 0.13f, 0.08f}
            : new float[]{0.09f, 0.07f, 0.04f};
        sr.setColor(emailBg[0], emailBg[1], emailBg[2], 1f);
        sr.rect(fx, efBottom, fieldW, FIELD_H);
        // Password field
        float[] passBg = !focusEmail
            ? new float[]{0.16f, 0.13f, 0.08f}
            : new float[]{0.09f, 0.07f, 0.04f};
        sr.setColor(passBg[0], passBg[1], passBg[2], 1f);
        sr.rect(fx, pfBottom, fieldW, FIELD_H);
        sr.end();

        // -- Field inset bevel borders --
        sr.begin(ShapeRenderer.ShapeType.Filled);
        // Email inset: dark top+left edges (shadow), lighter bottom+right (highlight)
        sr.setColor(0.05f, 0.04f, 0.02f, 1f);
        sr.rect(fx, efBottom + FIELD_H - 1, fieldW, 1);  // top
        sr.rect(fx, efBottom,               1,      FIELD_H); // left
        sr.setColor(0.36f, 0.30f, 0.16f, 1f);
        sr.rect(fx, efBottom, fieldW, 1);                 // bottom
        sr.rect(fx + fieldW - 1, efBottom, 1, FIELD_H);  // right
        // Email active glow (gold top edge when focused)
        if (focusEmail) {
            sr.setColor(0.72f, 0.62f, 0.26f, 1f);
            sr.rect(fx + 1, efBottom + FIELD_H - 1, fieldW - 2, 1);
        }
        // Password inset
        sr.setColor(0.05f, 0.04f, 0.02f, 1f);
        sr.rect(fx, pfBottom + FIELD_H - 1, fieldW, 1);
        sr.rect(fx, pfBottom,               1,      FIELD_H);
        sr.setColor(0.36f, 0.30f, 0.16f, 1f);
        sr.rect(fx, pfBottom, fieldW, 1);
        sr.rect(fx + fieldW - 1, pfBottom, 1, FIELD_H);
        if (!focusEmail) {
            sr.setColor(0.72f, 0.62f, 0.26f, 1f);
            sr.rect(fx + 1, pfBottom + FIELD_H - 1, fieldW - 2, 1);
        }
        sr.end();

        // -- Login button (OSRS raised brown) --
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.42f, 0.35f, 0.20f, 1f);          // button body
        sr.rect(fx, btnY, fieldW, BUTTON_H);
        sr.setColor(0.60f, 0.52f, 0.30f, 1f);          // top highlight
        sr.rect(fx, btnY + BUTTON_H - 1, fieldW, 1);
        sr.rect(fx, btnY, 1, BUTTON_H);                // left highlight
        sr.setColor(0.22f, 0.18f, 0.10f, 1f);          // bottom shadow
        sr.rect(fx, btnY, fieldW, 1);
        sr.rect(fx + fieldW - 1, btnY, 1, BUTTON_H);   // right shadow
        sr.end();

        // -- Text --
        batch.setProjectionMatrix(proj);
        batch.begin();

        // Title -- "Erynfall" in OSRS orange-gold, above the panel
        font.setColor(0.98f, 0.75f, 0.15f, 1f);
        GlyphLayout titleLayout = new GlyphLayout(font, "Erynfall");
        font.draw(batch, "Erynfall",
            panelX + (PANEL_W - titleLayout.width) / 2f,
            panelY + PANEL_H - 14);

        // Subtitle inside panel
        font.setColor(0.55f, 0.50f, 0.38f, 1f);
        GlyphLayout subLayout = new GlyphLayout(font, "Login to your account");
        font.draw(batch, "Login to your account",
            panelX + (PANEL_W - subLayout.width) / 2f,
            panelY + PANEL_H - 28);

        // Field labels (above each field)
        font.setColor(0.78f, 0.72f, 0.58f, 1f);
        font.draw(batch, "Email address:", fx, efBottom + FIELD_H + 14);
        font.draw(batch, "Password:", fx, pfBottom + FIELD_H + 14);

        // Field text contents
        String cursor = cursorBlink < 0.5f ? "|" : " ";
        font.setColor(1f, 1f, 1f, 1f);
        String emailDisplay = emailBuffer + (focusEmail ? cursor : "");
        font.draw(batch, emailDisplay, fx + 6, efBottom + FIELD_H - 8);
        String passDisplay  = "*".repeat(passwordBuffer.length()) + (!focusEmail ? cursor : "");
        font.draw(batch, passDisplay, fx + 6, pfBottom + FIELD_H - 8);

        // Login button label -- centred
        font.setColor(0.98f, 0.94f, 0.80f, 1f);
        GlyphLayout btnLayout = new GlyphLayout(font, "Login");
        font.draw(batch, "Login",
            fx + (fieldW - btnLayout.width) / 2f,
            btnY + (BUTTON_H + btnLayout.height) / 2f);

        // Error message
        if (!errorMessage.isEmpty()) {
            font.setColor(0.95f, 0.28f, 0.22f, 1f);
            font.draw(batch, errorMessage, fx, panelY + 48);
        }

        // Hint -- muted, at bottom of panel
        font.setColor(0.40f, 0.37f, 0.28f, 1f);
        font.draw(batch, "Tab  |  Click to focus  |  Enter to login", fx, panelY + 22);

        batch.end();
        font.setColor(Color.WHITE);
    }

    private void appendChar(char c) {
        if (focusEmail) {
            if (Character.isWhitespace(c)) {
                return;
            }
            if (emailBuffer.length() < EMAIL_MAX_LEN) {
                emailBuffer += c;
            }
        } else {
            if (passwordBuffer.length() < PASSWORD_MAX_LEN) {
                passwordBuffer += c;
            }
        }
    }

    private boolean isPasteShortcut(int keycode) {
        boolean modifierPressed = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)
            || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)
            || Gdx.input.isKeyPressed(Input.Keys.SYM);
        return modifierPressed && keycode == Input.Keys.V;
    }

    private void pasteFromClipboard() {
        String clipboard = Gdx.app.getClipboard().getContents();
        if (clipboard == null || clipboard.isBlank()) {
            return;
        }
        if (focusEmail) {
            String cleaned = clipboard.replaceAll("\\s+", "");
            int remaining = EMAIL_MAX_LEN - emailBuffer.length();
            if (remaining > 0) {
                emailBuffer += cleaned.substring(0, Math.min(cleaned.length(), remaining));
            }
            return;
        }

        int remaining = PASSWORD_MAX_LEN - passwordBuffer.length();
        if (remaining > 0) {
            passwordBuffer += clipboard.substring(0, Math.min(clipboard.length(), remaining));
        }
    }

    private void submit() {
        String email = emailBuffer.trim();
        String password = passwordBuffer;

        if (email.isEmpty()) {
            errorMessage = "Please enter an email.";
            return;
        }
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            errorMessage = "Please enter a valid email address.";
            return;
        }
        if (password.isEmpty()) {
            errorMessage = "Please enter a password.";
            return;
        }

        errorMessage  = "";
        transitioning = true;
        // Persist email for next login; never persist password
        Preferences prefs = Gdx.app.getPreferences("erynfall-login");
        prefs.putString("email", email);
        prefs.flush();
        game.startGame(email, password);
        // Do NOT touch any fields after this line — hide()/dispose() has already run.
    }

    @Override
    public void hide() {
        if (Gdx.input.getInputProcessor() == inputProcessor) {
            Gdx.input.setInputProcessor(null);
        }
        dispose();
    }

    @Override
    public void dispose() {
        if (font  != null) { font.dispose();  font  = null; }
        if (batch != null) { batch.dispose(); batch = null; }
        if (sr    != null) { sr.dispose();    sr    = null; }
    }
}
