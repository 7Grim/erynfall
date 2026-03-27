package com.osrs.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

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
    public enum ViewFilter { ALL, GAME, PUBLIC }

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Color TAB_ACTIVE = new Color(0.25f, 0.25f, 0.32f, 0.95f);
    private static final Color TAB_IDLE = new Color(0.12f, 0.12f, 0.16f, 0.82f);
    private static final Color TS_COLOR = new Color(0.62f, 0.62f, 0.62f, 1f);
    private static final int TAB_W = 42;
    private static final int TAB_H = 13;
    private static final int TAB_GAP = 4;

    private static class ChatLine {
        final String      senderName;  // empty for SYSTEM
        final String      text;
        final MessageType type;
        final String      timestamp;

        ChatLine(String senderName, String text, MessageType type) {
            this.senderName = senderName;
            this.text       = text;
            this.type       = type;
            this.timestamp  = LocalTime.now().format(TS_FMT);
        }
    }

    private static class WrappedLine {
        final MessageType type;
        final String timestamp;
        final String namePrefix;
        final String text;
        final boolean continuation;

        WrappedLine(MessageType type, String timestamp, String namePrefix, String text, boolean continuation) {
            this.type = type;
            this.timestamp = timestamp;
            this.namePrefix = namePrefix;
            this.text = text;
            this.continuation = continuation;
        }
    }

    private final LinkedList<ChatLine> lines     = new LinkedList<>();
    private static final int            MAX_LINES = 100;

    // -----------------------------------------------------------------------
    // Input state
    // -----------------------------------------------------------------------
    private boolean chatActive  = false;
    private String  inputBuffer = "";
    private ViewFilter viewFilter = ViewFilter.ALL;

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
    public ViewFilter getViewFilter() { return viewFilter; }

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

        int tabStartX = tabStartX();
        for (int i = 0; i < 3; i++) {
            int x = tabStartX + i * (TAB_W + TAB_GAP);
            boolean active = viewFilter.ordinal() == i;
            sr.setColor(active ? TAB_ACTIVE : TAB_IDLE);
            sr.rect(x, boxY + 4, TAB_W, TAB_H);
        }
        sr.end();

        // ── Text ─────────────────────────────────────────────────────────────
        batch.setProjectionMatrix(proj);
        batch.begin();

        // Render up to VISIBLE_LINES from the bottom of wrapped output.
        List<WrappedLine> wrapped = buildWrappedLines(font);
        int startIdx = Math.max(0, wrapped.size() - VISIBLE_LINES);
        int count = wrapped.size() - startIdx;
        for (int i = 0; i < count; i++) {
            WrappedLine line = wrapped.get(startIdx + i);
            int lineY = boxY + INPUT_H + PAD_Y + i * LINE_H;
            float x = boxX + PAD_X;

            if (!line.continuation && line.timestamp != null && !line.timestamp.isEmpty()) {
                font.setColor(TS_COLOR);
                String ts = "[" + line.timestamp + "] ";
                font.draw(batch, ts, x, lineY);
                x += new GlyphLayout(font, ts).width;
            }

            if (line.type == MessageType.SYSTEM) {
                font.setColor(SYS_COLOR);
                font.draw(batch, line.text, x, lineY);
            } else {
                float textX = x;
                if (!line.continuation && !line.namePrefix.isEmpty()) {
                    font.setColor(NAME_COLOR);
                    font.draw(batch, line.namePrefix, x, lineY);
                    GlyphLayout gl = new GlyphLayout(font, line.namePrefix);
                    textX += gl.width;
                } else if (line.continuation && !line.namePrefix.isEmpty()) {
                    GlyphLayout gl = new GlyphLayout(font, line.namePrefix);
                    textX += gl.width;
                }

                font.setColor(MSG_COLOR);
                font.draw(batch, line.text, textX, lineY);
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

        font.setColor(HINT_COLOR);
        font.draw(batch, "All", tabStartX + 11, boxY + 14);
        font.draw(batch, "Game", tabStartX + (TAB_W + TAB_GAP) + 8, boxY + 14);
        font.draw(batch, "Public", tabStartX + 2 * (TAB_W + TAB_GAP) + 5, boxY + 14);

        batch.end();
        font.setColor(Color.WHITE);
    }

    private List<WrappedLine> buildWrappedLines(BitmapFont font) {
        float totalWidth = BOX_W - (PAD_X * 2f);
        List<WrappedLine> out = new ArrayList<>();

        for (ChatLine line : lines) {
            if (viewFilter == ViewFilter.GAME && line.type != MessageType.SYSTEM) {
                continue;
            }
            if (viewFilter == ViewFilter.PUBLIC && line.type != MessageType.PUBLIC) {
                continue;
            }

            String tsPrefix = "[" + line.timestamp + "] ";
            float tsWidth = new GlyphLayout(font, tsPrefix).width;

            if (line.type == MessageType.SYSTEM) {
                List<String> chunks = wrapText(font, line.text, Math.max(32f, totalWidth - tsWidth));
                if (chunks.isEmpty()) chunks = List.of("");
                for (int i = 0; i < chunks.size(); i++) {
                    out.add(new WrappedLine(
                        MessageType.SYSTEM,
                        i == 0 ? line.timestamp : "",
                        "",
                        chunks.get(i),
                        i > 0
                    ));
                }
                continue;
            }

            String namePrefix = line.senderName + ": ";
            float prefixWidth = new GlyphLayout(font, namePrefix).width;
            float firstLineWidth = Math.max(24f, totalWidth - tsWidth - prefixWidth);

            List<String> chunks = wrapText(font, line.text, totalWidth - prefixWidth);
            if (chunks.isEmpty()) chunks = List.of("");

            // Rewrap first chunk with first-line reduced width so name and text fit.
            List<String> firstWrapped = wrapText(font, chunks.get(0), firstLineWidth);
            if (firstWrapped.isEmpty()) firstWrapped = List.of("");

            out.add(new WrappedLine(MessageType.PUBLIC, line.timestamp, namePrefix, firstWrapped.get(0), false));
            for (int i = 1; i < firstWrapped.size(); i++) {
                out.add(new WrappedLine(MessageType.PUBLIC, "", namePrefix, firstWrapped.get(i), true));
            }

            for (int i = 1; i < chunks.size(); i++) {
                List<String> continuation = wrapText(font, chunks.get(i), totalWidth - prefixWidth);
                if (continuation.isEmpty()) continuation = List.of("");
                for (String c : continuation) {
                    out.add(new WrappedLine(MessageType.PUBLIC, "", namePrefix, c, true));
                }
            }
        }

        return out;
    }

    private List<String> wrapText(BitmapFont font, String text, float maxWidth) {
        List<String> linesOut = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return linesOut;
        }

        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        GlyphLayout gl = new GlyphLayout();

        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            gl.setText(font, candidate);
            if (gl.width <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
            } else {
                if (!current.isEmpty()) {
                    linesOut.add(current.toString());
                    current.setLength(0);
                    current.append(word);
                } else {
                    // Single long token: hard-break by characters.
                    String remainder = word;
                    while (!remainder.isEmpty()) {
                        int cut = remainder.length();
                        while (cut > 1) {
                            gl.setText(font, remainder.substring(0, cut));
                            if (gl.width <= maxWidth) break;
                            cut--;
                        }
                        linesOut.add(remainder.substring(0, cut));
                        remainder = remainder.substring(cut);
                    }
                }
            }
        }

        if (!current.isEmpty()) {
            linesOut.add(current.toString());
        }

        return linesOut;
    }

    /**
     * Handle a click in chatbox-local coordinates. Returns true if click was consumed.
     */
    public boolean handleClick(int mouseX, int mouseY) {
        if (mouseY < 0 || mouseY > TOTAL_H || mouseX < 0 || mouseX > BOX_W) return false;

        int tabStartX = tabStartX();
        int tabY = 4;
        if (mouseY >= tabY && mouseY <= tabY + TAB_H) {
            for (int i = 0; i < 3; i++) {
                int x = tabStartX + i * (TAB_W + TAB_GAP);
                if (mouseX >= x && mouseX <= x + TAB_W) {
                    viewFilter = ViewFilter.values()[i];
                    return true;
                }
            }
        }
        return false;
    }

    private int tabStartX() {
        return BOX_W - (TAB_W * 3 + TAB_GAP * 2 + 8);
    }
}
