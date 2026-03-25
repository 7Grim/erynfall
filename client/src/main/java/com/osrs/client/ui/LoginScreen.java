package com.osrs.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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
 * Layout: dark background, centred panel, two input fields (username / password),
 * Tab to switch fields, Enter to submit, error message line at bottom.
 *
 * Username rules: 1–12 chars, letters/numbers/spaces only.
 * Password: any chars, asterisked display.
 */
public class LoginScreen extends ScreenAdapter {

    private static final int PANEL_W  = 320;
    private static final int PANEL_H  = 200;
    private static final int FIELD_H  = 24;
    private static final int PAD      = 12;

    private final ErynfallGame game;

    private BitmapFont    font;
    private SpriteBatch   batch;
    private ShapeRenderer sr;
    private Matrix4       proj;

    // Input state
    private String usernameBuffer = "";
    private String passwordBuffer = "";
    private boolean focusUsername = true;   // false = password field focused

    private String errorMessage = "";
    private float   cursorBlink  = 0f;
    private boolean transitioning = false;  // set true when screen switch is in flight

    public LoginScreen(ErynfallGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        font  = new BitmapFont();
        batch = new SpriteBatch();
        sr    = new ShapeRenderer();
        proj  = new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    @Override
    public void render(float delta) {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();

        cursorBlink = (cursorBlink + delta) % 1.0f;
        handleInput();

        // setScreen() was called inside handleInput() — our resources are now disposed; stop here.
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
        int usernameY = panelY + PANEL_H - 70;
        int passwordY = usernameY - 50;

        // Draw field backgrounds
        sr.begin(ShapeRenderer.ShapeType.Filled);
        // Username field
        Color userBg = focusUsername ? new Color(0.2f, 0.18f, 0.12f, 1f) : new Color(0.15f, 0.13f, 0.10f, 1f);
        sr.setColor(userBg);
        sr.rect(panelX + PAD, usernameY - FIELD_H, fieldW, FIELD_H);
        // Password field
        Color passBg = !focusUsername ? new Color(0.2f, 0.18f, 0.12f, 1f) : new Color(0.15f, 0.13f, 0.10f, 1f);
        sr.setColor(passBg);
        sr.rect(panelX + PAD, passwordY - FIELD_H, fieldW, FIELD_H);
        sr.end();

        // Field borders
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(focusUsername ? Color.YELLOW : Color.GRAY);
        sr.rect(panelX + PAD, usernameY - FIELD_H, fieldW, FIELD_H);
        sr.setColor(!focusUsername ? Color.YELLOW : Color.GRAY);
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
        font.draw(batch, "Username:", panelX + PAD, usernameY + 2);
        font.draw(batch, "Password:", panelX + PAD, passwordY + 2);

        // Field contents
        String cursor = cursorBlink < 0.5f ? "|" : "";
        font.setColor(Color.WHITE);
        String userDisplay = usernameBuffer + (focusUsername ? cursor : "");
        font.draw(batch, userDisplay, panelX + PAD + 4, usernameY - 4);
        String passDisplay = "*".repeat(passwordBuffer.length()) + (!focusUsername ? cursor : "");
        font.draw(batch, passDisplay, panelX + PAD + 4, passwordY - 4);

        // Hint
        font.setColor(Color.GRAY);
        font.draw(batch, "Tab: switch field   Enter: login / register", panelX + PAD, panelY + 30);

        // Error message
        if (!errorMessage.isEmpty()) {
            font.setColor(Color.RED);
            font.draw(batch, errorMessage, panelX + PAD, panelY + 16);
        }

        batch.end();
    }

    private void handleInput() {
        // Enter: submit
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            submit();
            return;
        }

        // Tab: switch focus
        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            focusUsername = !focusUsername;
            return;
        }

        // Backspace
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
            if (focusUsername) {
                if (!usernameBuffer.isEmpty())
                    usernameBuffer = usernameBuffer.substring(0, usernameBuffer.length() - 1);
            } else {
                if (!passwordBuffer.isEmpty())
                    passwordBuffer = passwordBuffer.substring(0, passwordBuffer.length() - 1);
            }
            return;
        }

        // Letters A-Z
        for (int key = Input.Keys.A; key <= Input.Keys.Z; key++) {
            if (Gdx.input.isKeyJustPressed(key)) {
                char c = Input.Keys.toString(key).charAt(0);
                if (!Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                 && !Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) {
                    c = Character.toLowerCase(c);
                }
                appendChar(c);
                return;
            }
        }

        // Digits 0-9 (key codes 7-16 in LibGDX)
        for (int key = Input.Keys.NUM_0; key <= Input.Keys.NUM_9; key++) {
            if (Gdx.input.isKeyJustPressed(key)) {
                char c = (char) ('0' + (key - Input.Keys.NUM_0));
                appendChar(c);
                return;
            }
        }

        // Space (username only — OSRS allows spaces in display names)
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && focusUsername) {
            appendChar(' ');
        }
    }

    private void appendChar(char c) {
        if (focusUsername) {
            if (usernameBuffer.length() < 12)
                usernameBuffer += c;
        } else {
            if (passwordBuffer.length() < 64)
                passwordBuffer += c;
        }
    }

    private void submit() {
        String username = usernameBuffer.trim();
        String password = passwordBuffer;

        if (username.isEmpty()) {
            errorMessage = "Please enter a username.";
            return;
        }
        if (username.length() > 12) {
            errorMessage = "Username must be 12 characters or less.";
            return;
        }
        if (password.isEmpty()) {
            errorMessage = "Please enter a password.";
            return;
        }

        errorMessage   = "";
        transitioning  = true;
        game.startGame(username, password);
        // Do NOT touch any fields after this line — hide()/dispose() has already run.
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        if (font  != null) { font.dispose();  font  = null; }
        if (batch != null) { batch.dispose(); batch = null; }
        if (sr    != null) { sr.dispose();    sr    = null; }
    }
}
