package com.osrs.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
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

    private static final int PANEL_W  = 320;
    private static final int PANEL_H  = 200;
    private static final int FIELD_H  = 24;
    private static final int PAD      = 12;
    private static final int EMAIL_MAX_LEN = 254;
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
        };
        Gdx.input.setInputProcessor(inputProcessor);
    }

    @Override
    public void render(float delta) {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();

        cursorBlink = (cursorBlink + delta) % 1.0f;

        // setScreen() was called from input callbacks — resources may already be disposed; stop here.
        if (transitioning) return;

        // Clear
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.05f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        int panelX = (w - PANEL_W) / 2;
        int panelY = (h - PANEL_H) / 2;

        // Background panel
        sr.setProjectionMatrix(proj);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.12f, 0.10f, 0.08f, 1f);
        sr.rect(panelX, panelY, PANEL_W, PANEL_H);
        sr.end();

        // Panel border
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(0.6f, 0.55f, 0.2f, 1f);  // OSRS gold border
        sr.rect(panelX, panelY, PANEL_W, PANEL_H);
        sr.end();

        // Field positions (from panel bottom)
        int fieldW    = PANEL_W - PAD * 2;
        int emailY = panelY + PANEL_H - 70;
        int passwordY = emailY - 50;

        // Draw field backgrounds
        sr.begin(ShapeRenderer.ShapeType.Filled);
        // Email field
        Color emailBg = focusEmail ? new Color(0.2f, 0.18f, 0.12f, 1f) : new Color(0.15f, 0.13f, 0.10f, 1f);
        sr.setColor(emailBg);
        sr.rect(panelX + PAD, emailY - FIELD_H, fieldW, FIELD_H);
        // Password field
        Color passBg = !focusEmail ? new Color(0.2f, 0.18f, 0.12f, 1f) : new Color(0.15f, 0.13f, 0.10f, 1f);
        sr.setColor(passBg);
        sr.rect(panelX + PAD, passwordY - FIELD_H, fieldW, FIELD_H);
        sr.end();

        // Field borders
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(focusEmail ? Color.YELLOW : Color.GRAY);
        sr.rect(panelX + PAD, emailY - FIELD_H, fieldW, FIELD_H);
        sr.setColor(!focusEmail ? Color.YELLOW : Color.GRAY);
        sr.rect(panelX + PAD, passwordY - FIELD_H, fieldW, FIELD_H);
        sr.end();

        // Text
        batch.setProjectionMatrix(proj);
        batch.begin();

        // Title
        font.setColor(new Color(0.9f, 0.8f, 0.2f, 1f));  // OSRS gold
        font.draw(batch, "Erynfall Login", panelX + PAD, panelY + PANEL_H - PAD - 4);

        // Labels
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "Email:", panelX + PAD, emailY + 2);
        font.draw(batch, "Password:", panelX + PAD, passwordY + 2);

        // Field contents
        String cursor = cursorBlink < 0.5f ? "|" : "";
        font.setColor(Color.WHITE);
        String emailDisplay = emailBuffer + (focusEmail ? cursor : "");
        font.draw(batch, emailDisplay, panelX + PAD + 4, emailY - 4);
        String passDisplay = "*".repeat(passwordBuffer.length()) + (!focusEmail ? cursor : "");
        font.draw(batch, passDisplay, panelX + PAD + 4, passwordY - 4);

        // Hint
        font.setColor(Color.GRAY);
        font.draw(batch, "Tab: switch field   Enter: login", panelX + PAD, panelY + 30);

        // Error message
        if (!errorMessage.isEmpty()) {
            font.setColor(Color.RED);
            font.draw(batch, errorMessage, panelX + PAD, panelY + 16);
        }

        batch.end();
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

        errorMessage   = "";
        transitioning  = true;
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
