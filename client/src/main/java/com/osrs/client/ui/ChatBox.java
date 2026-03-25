package com.osrs.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;

import java.util.LinkedList;

/**
 * OSRS-accurate public chat box.
 *
 * Layout (bottom-left corner of screen):
 *   ┌─────────────────────────────────┐
 *   │ Name: message line N            │  ← lines scroll upward (newest at bottom)
 *   │ Name: message line …            │
 *   │ Press Enter to Chat             │  ← input line (or typed text when active)
 *   └─────────────────────────────────┘
 *
 * OSRS details:
 *   - Player names in public chat are shown in blue (#0060FF-ish)
 *   - Message text in white
 *   - System messages (game feedback) in white/light grey
 *   - Background: dark semi-transparent (black, ~72% opacity)
 *   - Input hint "Press Enter to Chat" when idle
 *   - Active input shows typed text with blinking cursor bar
 *   - 80 character maximum per message
 *   - Overhead text lives for 3 seconds (150 × 20ms client ticks)
 */
public class ChatBox {

    // -----------------------------------------------------------------------
    // Layout constants
    // -----------------------------------------------------------------------
    public static final int BOX_W        = 518;   // matches OSRS chat box width
    public static final int CONTENT_H    = 112;   // visible message area height
    public static final int INPUT_H      = 22;    // input bar height
    public static final int TOTAL_H      = CONTENT_H + INPUT_H;
    public static final int VISIBLE_LINES = 8;    // lines visible at once
    public static final int LINE_H       = 14;    // px per message line
    public static final int PAD_X        = 6;
    public static final int PAD_Y        = 3;

    // -----------------------------------------------------------------------
    // OSRS colour constants
    // -----------------------------------------------------------------------
    /** Dark semi-transparent background — matches OSRS chat panel in opaque mode. */
    private static final Color BG_COLOR     = new Color(0.05f, 0.04f, 0.04f, 0.72f);
    /** Thin separator between content area and input bar. */
    private static final Color SEP_COLOR    = new Color(0.2f, 0.2f, 0.2f, 0.8f);
    /** OSRS public chat: player name shown in a medium blue. */
    private static final Color NAME_COLOR   = new Color(0f, 0.38f, 1f, 1f);
    /** Normal message text: white. */
    private static final Color MSG_COLOR    = Color.WHITE;
    /** System/game messages: light grey. */
    private static final Color SYS_COLOR    = new Color(0.75f, 0.75f, 0.75f, 1f);
    /** "Press Enter to Chat" hint: dim grey. */
    private static final Color HINT_COLOR   = new Color(0.5f, 0.5f, 0.5f, 0.9f);
    /** Typed text in input bar: yellow-white (matches OSRS input colour). */
    private static final Color INPUT_COLOR  = new Color(1f, 1f, 0.55f, 1f);

    // -----------------------------------------------------------------------
    // Message model
    // -----------------------------------------------------------------------
    public enum MessageType { PUBLIC, SYSTEM }

    private static class ChatLine {
        final String      senderName;  // empty for SYSTEM
        final String      text;
        final MessageType type;

        ChatLine(String senderName, String text, MessageType type) {
            this.senderName = senderName;
            this.text       = text;
            this.type       = type;
        }
    }

    private final LinkedList<ChatLine> lines     = new LinkedList<>();
    private static final int            MAX_LINES = 100;

    // -----------------------------------------------------------------------
    // Input state
    // -----------------------------------------------------------------------
    private boolean chatActive  = false;
    private String  inputBuffer = "";

    // Cursor blink timer
    private float cursorTimer = 0f;
    private boolean cursorVisible = true;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /** Add a public chat message from a named player. */
    public void addPublicMessage(String senderName, String text) {
        addLine(new ChatLine(senderName, text, MessageType.PUBLIC));
    }

    /** Add a system/game message (no sender name, shown in grey). */
    public void addSystemMessage(String text) {
        addLine(new ChatLine("", text, MessageType.SYSTEM));
    }

    private void addLine(ChatLine line) {
        lines.addLast(line);
        if (lines.size() > MAX_LINES) lines.removeFirst();
    }

    /** Toggle the chat input bar on/off. */
    public void setActive(boolean active) {
        chatActive  = active;
        if (!active) inputBuffer = "";
        cursorTimer = 0f;
        cursorVisible = true;
    }

    public boolean isActive()       { return chatActive; }
    public String  getInputBuffer() { return inputBuffer; }
    public void    setInputBuffer(String buf) { this.inputBuffer = buf; }

    // -----------------------------------------------------------------------
    // Update
    // -----------------------------------------------------------------------
    public void update(float delta) {
        if (!chatActive) return;
        cursorTimer += delta;
        if (cursorTimer >= 0.53f) {   // blink at ~0.53s intervals (OSRS-like)
            cursorTimer = 0f;
            cursorVisible = !cursorVisible;
        }
    }

    // -----------------------------------------------------------------------
    // Render
    // -----------------------------------------------------------------------
    /**
     * Render in screen space (Y=0 at bottom).
     * Call between screenBatch.begin()/end() pairs — this method manages its own begin/end.
     */
    public void render(ShapeRenderer sr, SpriteBatch batch, BitmapFont font,
                       int screenW, int screenH, Matrix4 proj) {
        int boxX = 0;
        int boxY = 0;   // anchored to bottom-left

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // ── Background ───────────────────────────────────────────────────────
        sr.setProjectionMatrix(proj);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(BG_COLOR);
        sr.rect(boxX, boxY, BOX_W, TOTAL_H);
        // Separator line between content and input bar
        sr.setColor(SEP_COLOR);
        sr.rect(boxX, boxY + INPUT_H - 1, BOX_W, 1);
        sr.end();

        // ── Text ─────────────────────────────────────────────────────────────
        batch.setProjectionMatrix(proj);
        batch.begin();

        // Render up to VISIBLE_LINES from the bottom of the list
        int startIdx = Math.max(0, lines.size() - VISIBLE_LINES);
        int count    = lines.size() - startIdx;
        for (int i = 0; i < count; i++) {
            ChatLine line = lines.get(startIdx + i);
            int lineY = boxY + INPUT_H + PAD_Y + i * LINE_H;

            if (line.type == MessageType.SYSTEM) {
                font.setColor(SYS_COLOR);
                font.draw(batch, line.text, boxX + PAD_X, lineY);
            } else {
                // "Name: " in blue, then message text in white
                String nameTag = line.senderName + ": ";
                font.setColor(NAME_COLOR);
                font.draw(batch, nameTag, boxX + PAD_X, lineY);

                GlyphLayout gl = new GlyphLayout(font, nameTag);
                font.setColor(MSG_COLOR);
                font.draw(batch, line.text, boxX + PAD_X + gl.width, lineY);
            }
        }

        // Input bar
        if (chatActive) {
            String cursor = cursorVisible ? "|" : " ";
            font.setColor(INPUT_COLOR);
            font.draw(batch, inputBuffer + cursor, boxX + PAD_X, boxY + INPUT_H - PAD_Y - 1);
        } else {
            font.setColor(HINT_COLOR);
            font.draw(batch, "Press Enter to Chat", boxX + PAD_X, boxY + INPUT_H - PAD_Y - 1);
        }

        batch.end();
        font.setColor(Color.WHITE);
    }
}
