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
    public static final int CONTENT_H    = 116;   // visible message area height
    public static final int TAB_H        = 18;    // filter strip height
    public static final int INPUT_H      = 22;    // input bar height
    public static final int TOTAL_H      = CONTENT_H + TAB_H + INPUT_H;
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
    private static final Color SYS_COLOR    = new Color(0.95f, 0.95f, 0.95f, 1f);
    /** "Press Enter to Chat" hint: dim grey. */
    private static final Color HINT_COLOR   = new Color(0.5f, 0.5f, 0.5f, 0.9f);
    /** Typed text in input bar: yellow-white (matches OSRS input colour). */
    private static final Color INPUT_COLOR  = new Color(1f, 1f, 0.55f, 1f);

    // -----------------------------------------------------------------------
    // Message model
    // -----------------------------------------------------------------------
    public enum MessageType { PUBLIC, SYSTEM, PRIVATE }

    // Tab types and state
    public enum ChatTab { ALL, GAME, PUBLIC, PRIVATE, CHANNEL, CLAN, TRADE }

    /** Per-tab on/off state — true = channel visible, false = hidden. */
    private final boolean[] tabEnabled = {
        true,  // ALL (always on)
        true,  // GAME
        true,  // PUBLIC
        false, // PRIVATE
        false, // CHANNEL
        false, // CLAN
        false  // TRADE
    };

    public enum ChatChannelFilter { ALL_MESSAGES, PUBLIC_CHAT, SYSTEM_MESSAGES, PRIVATE_CHAT }

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Color TAB_ACTIVE = new Color(0.30f, 0.24f, 0.12f, 1f);
    private static final Color TAB_IDLE = new Color(0.13f, 0.11f, 0.09f, 0.95f);
    private static final Color BORDER_ACTIVE = FontManager.TEXT_GOLD;
    private static final Color BORDER_IDLE = new Color(0.65f, 0.52f, 0.10f, 1f);
    private static final Color LABEL_ACTIVE = FontManager.TEXT_WHITE;
    private static final Color LABEL_IDLE = new Color(0.85f, 0.85f, 0.85f, 1f);
    private static final Color TS_COLOR = new Color(0.62f, 0.62f, 0.62f, 1f);
    private static final Color PRIVATE_COLOR = new Color(0.82f, 0.92f, 1f, 1f);
    private static final int TAB_W = 52;
    private static final int TAB_BTN_H = 14;
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
    private ChatTab activeTab = ChatTab.ALL;
    private ChatChannelFilter channelFilter = ChatChannelFilter.ALL_MESSAGES;

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

    /** Add a private/direct message. */
    public void addPrivateMessage(String senderName, String text) {
        addLine(new ChatLine(senderName, text, MessageType.PRIVATE));
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
    public ChatTab getActiveTab() { return activeTab; }

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
        // Separator line between content and tab strip
        sr.setColor(SEP_COLOR);
        sr.rect(boxX, boxY + INPUT_H + TAB_H - 1, BOX_W, 1);
        // Separator line between tab strip and input bar
        sr.setColor(SEP_COLOR);
        sr.rect(boxX, boxY + INPUT_H - 1, BOX_W, 1);

        sr.end();

        renderTabButtons(sr, batch, font, boxX, boxY, proj);

        // ── Text ─────────────────────────────────────────────────────────────
        batch.setProjectionMatrix(proj);
        batch.begin();

        // Render up to VISIBLE_LINES with newest at the bottom (OSRS behavior).
        List<WrappedLine> wrapped = buildWrappedLines(font);
        int visibleCount = Math.min(VISIBLE_LINES, wrapped.size());
        int startIdx = Math.max(0, wrapped.size() - visibleCount);
        int count = wrapped.size() - startIdx;
        for (int i = count - 1; i >= 0; i--) {
            WrappedLine line = wrapped.get(startIdx + i);
            int lineY = boxY + INPUT_H + TAB_H + PAD_Y + (count - 1 - i) * LINE_H;
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

                if (line.type == MessageType.PRIVATE) {
                    font.setColor(PRIVATE_COLOR);
                } else {
                    font.setColor(MSG_COLOR);
                }
                font.draw(batch, line.text, textX, lineY);
            }
        }

        // Input bar
        if (chatActive) {
            String cursor = cursorVisible ? "|" : " ";
            font.setColor(INPUT_COLOR);
            font.draw(batch, inputBuffer + cursor, boxX + PAD_X, boxY + INPUT_H - PAD_Y - 3);
        } else {
            font.setColor(HINT_COLOR);
            font.draw(batch, "Press Enter to Chat", boxX + PAD_X, boxY + INPUT_H - PAD_Y - 3);
        }

        batch.end();
        font.setColor(Color.WHITE);
    }

    // Tab button rendering
    private void renderTabButtons(ShapeRenderer sr, SpriteBatch batch, BitmapFont font,
                                  int boxX, int boxY, Matrix4 proj) {
        int startX = tabStartX();
        int y = boxY + INPUT_H + 2;
        ChatTab[] tabs = ChatTab.values();

        sr.setProjectionMatrix(proj);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < tabs.length; i++) {
            boolean active = (activeTab == tabs[i]);
            sr.setColor(active ? TAB_ACTIVE : TAB_IDLE);
            sr.rect(startX + i * (TAB_W + TAB_GAP), y, TAB_W, TAB_BTN_H);
            // Status dot: green = on, grey = off (not drawn for ALL tab)
            if (i > 0) {
                sr.setColor(tabEnabled[i] ? 0.20f : 0.45f,
                    tabEnabled[i] ? 0.72f : 0.43f,
                    tabEnabled[i] ? 0.18f : 0.38f, 1f);
                sr.circle(startX + i * (TAB_W + TAB_GAP) + 4, y + TAB_BTN_H - 4, 2.5f, 8);
            }
        }
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < tabs.length; i++) {
            boolean active = (activeTab == tabs[i]);
            sr.setColor(active ? BORDER_ACTIVE : BORDER_IDLE);
            sr.rect(startX + i * (TAB_W + TAB_GAP), y, TAB_W, TAB_BTN_H);
        }
        sr.end();

        batch.setProjectionMatrix(proj);
        batch.begin();
        font.getData().setScale(0.78f);
        GlyphLayout gl = new GlyphLayout();
        for (int i = 0; i < tabs.length; i++) {
            boolean active = (activeTab == tabs[i]);
            String label = tabLabel(tabs[i]);
            float bx = startX + i * (TAB_W + TAB_GAP);
            // Shift label right for tabs with status dot
            float labelOffsetX = i > 0 ? 5f : 0f;
            gl.setText(font, label);
            font.setColor(active ? LABEL_ACTIVE : LABEL_IDLE);
            font.draw(batch, label, bx + labelOffsetX + (TAB_W - labelOffsetX - gl.width) / 2f, y + TAB_BTN_H - 3);
        }
        font.getData().setScale(1f);
        batch.end();
    }

    private String tabLabel(ChatTab tab) {
        return switch (tab) {
            case ALL -> "All";
            case GAME -> "Game";
            case PUBLIC -> "Public";
            case PRIVATE -> "Private";
            case CHANNEL -> "Channel";
            case CLAN -> "Clan";
            case TRADE -> "Trade";
        };
    }

    private List<WrappedLine> buildWrappedLines(BitmapFont font) {
        float totalWidth = BOX_W - (PAD_X * 2f);
        List<WrappedLine> out = new ArrayList<>();

        for (ChatLine line : lines) {
            if (!shouldDisplayLine(line.type)) {
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

    private boolean shouldDisplayLine(MessageType type) {
        if (activeTab == ChatTab.ALL) return true;
        if (!tabEnabled[activeTab.ordinal()]) return false;
        if (activeTab == ChatTab.GAME)    return type == MessageType.SYSTEM;
        if (activeTab == ChatTab.PUBLIC)  return type == MessageType.PUBLIC;
        if (activeTab == ChatTab.PRIVATE) return type == MessageType.PRIVATE;
        // CHANNEL, CLAN, TRADE: no messages yet; show nothing
        return false;
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
        return handleClick(mouseX, mouseY, false);
    }

    /**
     * Handle a click in chatbox-local coordinates.
     * Right-click is used to toggle tab status dots.
     */
    public boolean handleClick(int mouseX, int mouseY, boolean rightClick) {
        if (mouseY < 0 || mouseY > TOTAL_H || mouseX < 0 || mouseX > BOX_W) return false;

        int tabStartX = tabStartX();
        int tabY = INPUT_H + 2;
        if (mouseY >= tabY && mouseY <= tabY + TAB_BTN_H) {
            ChatTab[] tabs = ChatTab.values();
            for (int i = 0; i < tabs.length; i++) {
                int x = tabStartX + i * (TAB_W + TAB_GAP);
                if (mouseX >= x && mouseX <= x + TAB_W) {
                    boolean onDotArea = i > 0 && mouseX <= x + 10 && mouseY >= tabY + TAB_BTN_H - 8;
                    if (rightClick && onDotArea) {
                        tabEnabled[i] = !tabEnabled[i];
                        return true;
                    }
                    activeTab = tabs[i];
                    channelFilter = switch (activeTab) {
                        case ALL -> ChatChannelFilter.ALL_MESSAGES;
                        case GAME -> ChatChannelFilter.SYSTEM_MESSAGES;
                        case PUBLIC -> ChatChannelFilter.PUBLIC_CHAT;
                        case PRIVATE -> ChatChannelFilter.PRIVATE_CHAT;
                        default -> ChatChannelFilter.ALL_MESSAGES;
                    };
                    return true;
                }
            }
        }
        return false;
    }

    private int tabStartX() {
        int count = ChatTab.values().length;
        return BOX_W - (TAB_W * count + TAB_GAP * (count - 1) + 10);
    }
}
