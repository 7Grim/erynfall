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
 * OSRS-accurate chat box — bottom-left corner of screen.
 *
 * Vertical layout (Y=0 at bottom):
 *   ┌──────────────────────────────────────────────────────────────────┐ ← TOTAL_H
 *   │                    message area (CONTENT_H)                      │
 *   ├──────────────────────────────────────────────────────────────────┤
 *   │  All │ Game │ Public │ Private │ Channel │ Clan │ Trade │ Report │ ← TAB_H
 *   ├──────────────────────────────────────────────────────────────────┤
 *   │  Press Enter to Chat  (or typed input)                           │ ← INPUT_H
 *   └──────────────────────────────────────────────────────────────────┘ ← Y=0
 */
public class ChatBox {

    // -----------------------------------------------------------------------
    // Layout constants — all public so GameScreen can do hit-testing
    // -----------------------------------------------------------------------
    public static final int BOX_W         = 519;
    public static final int INPUT_H       = 22;     // input bar
    public static final int TAB_H         = 22;     // tab strip
    public static final int CONTENT_H     = 140;    // message area
    public static final int TOTAL_H       = CONTENT_H + TAB_H + INPUT_H; // 184

    public static final int LINE_H        = 16;     // px per rendered line (at CHAT_SCALE)
    public static final int VISIBLE_LINES = 8;      // max simultaneous lines
    public static final int PAD_X         = 6;
    public static final int PAD_Y         = 8;      // vertical padding inside message area

    private static final int   SCROLLBAR_W   = 8;
    private static final float CHAT_SCALE    = 0.85f;
    private static final float TAB_SCALE     = 0.78f;

    // Tab geometry — left-aligned, 7 equal-width tabs + Report button on right
    private static final int TAB_BTN_H   = 16;
    private static final int TAB_GAP     = 2;
    private static final int REPORT_W    = 55;
    // TAB_W fills space between left edge and report button minus gaps
    // (BOX_W - 2 - REPORT_W - TAB_GAP) / 7 rounded to integer
    private static final int TAB_W       = 64;  // 7*64+6*2+2+55+2 = 519 ✓

    // -----------------------------------------------------------------------
    // Colours
    // -----------------------------------------------------------------------
    private static final Color BG           = new Color(0.05f, 0.04f, 0.04f, 0.82f);
    private static final Color SEP          = new Color(0.38f, 0.30f, 0.12f, 1f);
    private static final Color NAME_COL     = new Color(0f,    0.38f, 1f,    1f);
    private static final Color MSG_COL      = Color.WHITE;
    private static final Color SYS_COL      = new Color(0.95f, 0.95f, 0.80f, 1f);
    private static final Color HINT_COL     = new Color(0.50f, 0.50f, 0.50f, 0.90f);
    private static final Color INPUT_COL    = new Color(1f,    1f,    0.55f, 1f);
    private static final Color TS_COL       = new Color(0.55f, 0.55f, 0.55f, 1f);
    private static final Color PRIVATE_COL  = new Color(0.82f, 0.92f, 1f,    1f);

    private static final Color TAB_ACT_BG   = new Color(0.32f, 0.25f, 0.08f, 1f);
    private static final Color TAB_IDL_BG   = new Color(0.11f, 0.09f, 0.07f, 1f);
    private static final Color TAB_ACT_BR   = new Color(1f,    0.85f, 0.10f, 1f);
    private static final Color TAB_IDL_BR   = new Color(0.42f, 0.35f, 0.12f, 1f);
    private static final Color TAB_ACT_LBL  = Color.WHITE;
    private static final Color TAB_IDL_LBL  = new Color(0.80f, 0.80f, 0.75f, 1f);
    private static final Color DOT_ON       = new Color(0.20f, 0.72f, 0.18f, 1f);
    private static final Color DOT_OFF      = new Color(0.40f, 0.40f, 0.38f, 1f);

    private static final Color REPORT_BG    = new Color(0.18f, 0.07f, 0.07f, 1f);
    private static final Color REPORT_BR    = new Color(0.65f, 0.22f, 0.12f, 1f);
    private static final Color REPORT_LBL   = new Color(0.88f, 0.60f, 0.55f, 1f);

    private static final Color SCR_TRACK    = new Color(0.12f, 0.10f, 0.08f, 0.95f);
    private static final Color SCR_HANDLE   = new Color(0.52f, 0.44f, 0.18f, 1f);

    // -----------------------------------------------------------------------
    // Enums
    // -----------------------------------------------------------------------
    public enum MessageType    { PUBLIC, SYSTEM, PRIVATE }
    public enum ChatTab        { ALL, GAME, PUBLIC, PRIVATE, CHANNEL, CLAN, TRADE }
    public enum ChatChannelFilter { ALL_MESSAGES, PUBLIC_CHAT, SYSTEM_MESSAGES, PRIVATE_CHAT }

    // -----------------------------------------------------------------------
    // Message data model
    // -----------------------------------------------------------------------
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private static class ChatLine {
        final String      senderName;
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
        final String      timestamp;   // non-empty only on the first line of a message
        final String      namePrefix;  // e.g. "PlayerName: "
        final String      text;
        final boolean     continuation;

        WrappedLine(MessageType type, String ts, String namePrefix, String text, boolean cont) {
            this.type         = type;
            this.timestamp    = ts;
            this.namePrefix   = namePrefix;
            this.text         = text;
            this.continuation = cont;
        }
    }

    private final LinkedList<ChatLine> lines = new LinkedList<>();
    private static final int MAX_LINES = 200;

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------
    private final boolean[] tabEnabled = { true, true, true, false, false, false, false };

    private ChatTab           activeTab      = ChatTab.ALL;
    private ChatChannelFilter channelFilter  = ChatChannelFilter.ALL_MESSAGES;
    private boolean           chatActive     = false;
    private String            inputBuffer    = "";
    private float             cursorTimer    = 0f;
    private boolean           cursorVisible  = true;

    /** Lines scrolled up from the bottom. 0 = showing newest messages. */
    private int scrollOffset = 0;

    // Wrapped-line cache — rebuilt only when messages or active tab changes.
    private List<WrappedLine> wrappedCache  = new ArrayList<>();
    private int               cachedCount   = -1;
    private ChatTab           cachedTab     = null;

    /** Single reusable GlyphLayout — eliminates per-frame allocations. */
    private final GlyphLayout gl = new GlyphLayout();

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    public void addPublicMessage(String senderName, String text) {
        addLine(new ChatLine(senderName, text, MessageType.PUBLIC));
    }

    public void addSystemMessage(String text) {
        addLine(new ChatLine("", text, MessageType.SYSTEM));
    }

    public void addPrivateMessage(String senderName, String text) {
        addLine(new ChatLine(senderName, text, MessageType.PRIVATE));
    }

    private void addLine(ChatLine line) {
        lines.addLast(line);
        if (lines.size() > MAX_LINES) lines.removeFirst();
        cachedCount = -1; // invalidate wrap cache
    }

    public void setActive(boolean active) {
        chatActive  = active;
        if (!active) inputBuffer = "";
        cursorTimer   = 0f;
        cursorVisible = true;
    }

    public boolean isActive()       { return chatActive; }
    public String  getInputBuffer() { return inputBuffer; }
    public void    setInputBuffer(String buf) { this.inputBuffer = buf; }
    public ChatTab getActiveTab()   { return activeTab; }

    /**
     * Scroll chat history. Positive = scroll up (older), negative = scroll down (newer).
     * Called from GameScreen when the mouse wheel fires over the chat area.
     */
    public void handleScroll(int amount) {
        scrollOffset = Math.max(0, scrollOffset + amount);
        // max is clamped in render() once we know the total line count
    }

    // -----------------------------------------------------------------------
    // Update
    // -----------------------------------------------------------------------
    public void update(float delta) {
        if (!chatActive) return;
        cursorTimer += delta;
        if (cursorTimer >= 0.53f) {
            cursorTimer   = 0f;
            cursorVisible = !cursorVisible;
        }
    }

    // -----------------------------------------------------------------------
    // Render
    // -----------------------------------------------------------------------
    public void render(ShapeRenderer sr, SpriteBatch batch, BitmapFont font,
                       int screenW, int screenH, Matrix4 proj) {

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // ── Background ────────────────────────────────────────────────────────
        sr.setProjectionMatrix(proj);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(BG);
        sr.rect(0, 0, BOX_W, TOTAL_H);
        // Thin gold separator lines between the three zones
        sr.setColor(SEP);
        sr.rect(0, INPUT_H - 1,         BOX_W, 1); // input ↔ tabs
        sr.rect(0, INPUT_H + TAB_H - 1, BOX_W, 1); // tabs ↔ messages
        sr.end();

        // ── Font scale for all chat rendering ─────────────────────────────────
        font.getData().setScale(CHAT_SCALE);

        // ── Rebuild wrap cache if stale ────────────────────────────────────────
        if (cachedCount != lines.size() || cachedTab != activeTab) {
            wrappedCache = buildWrappedLines(font);
            cachedCount  = lines.size();
            cachedTab    = activeTab;
        }

        int totalLines = wrappedCache.size();
        int maxScroll  = Math.max(0, totalLines - VISIBLE_LINES);
        scrollOffset   = Math.min(scrollOffset, maxScroll);

        // ── Scrollbar ─────────────────────────────────────────────────────────
        if (maxScroll > 0) {
            renderScrollbar(sr, maxScroll, totalLines, proj);
        }

        // ── Tab buttons ───────────────────────────────────────────────────────
        renderTabButtons(sr, batch, font, proj);

        // ── Messages ──────────────────────────────────────────────────────────
        //
        // baseLine = LibGDX y-coordinate (top of cap-height) for the bottom-most
        // visible line. Text extends DOWNWARD from baseLine, so we need baseLine
        // to be at least one full font-line above the top of the tab strip.
        //
        // Tab strip top = INPUT_H + TAB_H = 44.
        // baseLine = INPUT_H + TAB_H + PAD_Y + LINE_H = 44 + 8 + 16 = 68.
        // Worst-case bottom of bottom-line text ≈ 68 − 17 ≈ 51 > 44. ✓
        //
        int baseLine  = INPUT_H + TAB_H + PAD_Y + LINE_H;
        int startIdx  = Math.max(0, totalLines - VISIBLE_LINES - scrollOffset);
        int count     = Math.min(VISIBLE_LINES, totalLines - startIdx);

        // Text area width — leave room for scrollbar when it's present
        float textW = BOX_W - PAD_X * 2f - (maxScroll > 0 ? SCROLLBAR_W + 3 : 0);

        batch.setProjectionMatrix(proj);
        batch.begin();

        for (int i = 0; i < count; i++) {
            // i=0 → bottom/newest line; i=count-1 → top/oldest line
            WrappedLine line = wrappedCache.get(startIdx + count - 1 - i);
            int lineY = baseLine + i * LINE_H;
            float x   = PAD_X;

            // Timestamp prefix on the first line of each message
            if (!line.continuation && line.timestamp != null && !line.timestamp.isEmpty()) {
                String ts = "[" + line.timestamp + "] ";
                font.setColor(TS_COL);
                font.draw(batch, ts, x, lineY);
                gl.setText(font, ts);
                x += gl.width;
            }

            if (line.type == MessageType.SYSTEM) {
                font.setColor(SYS_COL);
                font.draw(batch, line.text, x, lineY);

            } else {
                // Name prefix on first line; indent continuation lines to same X
                if (!line.namePrefix.isEmpty()) {
                    if (!line.continuation) {
                        font.setColor(NAME_COL);
                        font.draw(batch, line.namePrefix, x, lineY);
                    }
                    gl.setText(font, line.namePrefix);
                    x += gl.width;
                }

                font.setColor(line.type == MessageType.PRIVATE ? PRIVATE_COL : MSG_COL);
                font.draw(batch, line.text, x, lineY);
            }
        }

        // ── Input bar ─────────────────────────────────────────────────────────
        if (chatActive) {
            String cursor = cursorVisible ? "|" : " ";
            font.setColor(INPUT_COL);
            font.draw(batch, inputBuffer + cursor, PAD_X, INPUT_H - 5);
        } else {
            font.setColor(HINT_COL);
            font.draw(batch, "Press Enter to Chat", PAD_X, INPUT_H - 5);
        }

        batch.end();

        // Reset font state
        font.getData().setScale(FontManager.getScale(FontManager.FontContext.BASE_UI));
        font.setColor(Color.WHITE);
    }

    // -----------------------------------------------------------------------
    // Scrollbar
    // -----------------------------------------------------------------------
    private void renderScrollbar(ShapeRenderer sr, int maxScroll, int totalLines, Matrix4 proj) {
        int trackX      = BOX_W - SCROLLBAR_W - 1;
        int trackBottom = INPUT_H + TAB_H + PAD_Y;
        int trackTop    = TOTAL_H - PAD_Y;
        int trackH      = trackTop - trackBottom;
        if (trackH < 8) return;

        int handleH    = Math.max(16, (VISIBLE_LINES * trackH) / Math.max(1, totalLines));
        int handleRange = trackH - handleH;
        float ratio    = maxScroll > 0 ? (float) scrollOffset / maxScroll : 0f;
        int handleY    = trackBottom + Math.round(ratio * handleRange);

        sr.setProjectionMatrix(proj);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(SCR_TRACK);
        sr.rect(trackX, trackBottom, SCROLLBAR_W, trackH);
        sr.setColor(SCR_HANDLE);
        sr.rect(trackX + 1, handleY, SCROLLBAR_W - 2, handleH);
        sr.end();
    }

    // -----------------------------------------------------------------------
    // Tab buttons
    // -----------------------------------------------------------------------
    private void renderTabButtons(ShapeRenderer sr, SpriteBatch batch,
                                   BitmapFont font, Matrix4 proj) {
        ChatTab[] tabs = ChatTab.values();
        int tabY       = INPUT_H + 2;                     // bottom of tab buttons
        int reportX    = BOX_W - REPORT_W - 2;            // Report button left edge

        // ── Filled backgrounds ────────────────────────────────────────────────
        sr.setProjectionMatrix(proj);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        int x = 1;
        for (int i = 0; i < tabs.length; i++) {
            boolean active = (activeTab == tabs[i]);
            sr.setColor(active ? TAB_ACT_BG : TAB_IDL_BG);
            sr.rect(x, tabY, TAB_W, TAB_BTN_H);

            // Status dot (not on ALL tab)
            if (i > 0) {
                sr.setColor(tabEnabled[i] ? DOT_ON : DOT_OFF);
                sr.circle(x + 5f, tabY + TAB_BTN_H / 2f, 2.5f, 8);
            }
            x += TAB_W + TAB_GAP;
        }
        // Report button
        sr.setColor(REPORT_BG);
        sr.rect(reportX, tabY, REPORT_W, TAB_BTN_H);
        sr.end();

        // ── Borders ───────────────────────────────────────────────────────────
        sr.begin(ShapeRenderer.ShapeType.Line);
        x = 1;
        for (int i = 0; i < tabs.length; i++) {
            boolean active = (activeTab == tabs[i]);
            sr.setColor(active ? TAB_ACT_BR : TAB_IDL_BR);
            sr.rect(x, tabY, TAB_W, TAB_BTN_H);
            x += TAB_W + TAB_GAP;
        }
        sr.setColor(REPORT_BR);
        sr.rect(reportX, tabY, REPORT_W, TAB_BTN_H);
        sr.end();

        // ── Labels ───────────────────────────────────────────────────────────
        batch.setProjectionMatrix(proj);
        batch.begin();
        font.getData().setScale(TAB_SCALE);

        x = 1;
        for (int i = 0; i < tabs.length; i++) {
            boolean active  = (activeTab == tabs[i]);
            String  label   = tabLabel(tabs[i]);
            float   dotOffset = i > 0 ? 7f : 0f;
            gl.setText(font, label);
            float lx = x + dotOffset + (TAB_W - dotOffset - gl.width) / 2f;
            font.setColor(active ? TAB_ACT_LBL : TAB_IDL_LBL);
            font.draw(batch, label, lx, tabY + TAB_BTN_H - 3);
            x += TAB_W + TAB_GAP;
        }

        gl.setText(font, "Report");
        font.setColor(REPORT_LBL);
        font.draw(batch, "Report", reportX + (REPORT_W - gl.width) / 2f, tabY + TAB_BTN_H - 3);

        // Restore scale to CHAT_SCALE (render() will reset to BASE_UI after batch.end)
        font.getData().setScale(CHAT_SCALE);
        batch.end();
    }

    // -----------------------------------------------------------------------
    // Click handling
    // -----------------------------------------------------------------------
    /** Handle a click in screen-space coordinates (Y=0 at bottom). */
    public boolean handleClick(int mouseX, int mouseY) {
        return handleClick(mouseX, mouseY, false);
    }

    /** Handle a click; rightClick=true toggles tab status dots. */
    public boolean handleClick(int mouseX, int mouseY, boolean rightClick) {
        if (mouseY < 0 || mouseY > TOTAL_H || mouseX < 0 || mouseX > BOX_W) return false;

        int tabY = INPUT_H + 2;
        if (mouseY < tabY || mouseY > tabY + TAB_BTN_H) return false;

        // Main tab buttons
        int x = 1;
        ChatTab[] tabs = ChatTab.values();
        for (int i = 0; i < tabs.length; i++) {
            if (mouseX >= x && mouseX <= x + TAB_W) {
                boolean onDot = i > 0 && mouseX <= x + 10;
                if (rightClick && onDot) {
                    tabEnabled[i] = !tabEnabled[i];
                    cachedCount   = -1; // filter changed, rebuild
                    return true;
                }
                activeTab     = tabs[i];
                channelFilter = toFilter(activeTab);
                cachedCount   = -1;
                return true;
            }
            x += TAB_W + TAB_GAP;
        }

        // Report button — no network action yet, just consume click
        int reportX = BOX_W - REPORT_W - 2;
        if (mouseX >= reportX && mouseX <= reportX + REPORT_W) {
            return true;
        }

        return false;
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------
    private ChatChannelFilter toFilter(ChatTab tab) {
        return switch (tab) {
            case GAME    -> ChatChannelFilter.SYSTEM_MESSAGES;
            case PUBLIC  -> ChatChannelFilter.PUBLIC_CHAT;
            case PRIVATE -> ChatChannelFilter.PRIVATE_CHAT;
            default      -> ChatChannelFilter.ALL_MESSAGES;
        };
    }

    private boolean shouldDisplayLine(MessageType type) {
        if (activeTab == ChatTab.ALL) return true;
        if (!tabEnabled[activeTab.ordinal()]) return false;
        return switch (activeTab) {
            case GAME    -> type == MessageType.SYSTEM;
            case PUBLIC  -> type == MessageType.PUBLIC;
            case PRIVATE -> type == MessageType.PRIVATE;
            default      -> false;
        };
    }

    private String tabLabel(ChatTab tab) {
        return switch (tab) {
            case ALL     -> "All";
            case GAME    -> "Game";
            case PUBLIC  -> "Public";
            case PRIVATE -> "Private";
            case CHANNEL -> "Channel";
            case CLAN    -> "Clan";
            case TRADE   -> "Trade";
        };
    }

    /**
     * Rebuild the flat list of display lines from all stored ChatLines.
     * Uses {@link #gl} for text measurement — no additional allocations.
     * Result is cached; only call when messages or active tab changes.
     */
    private List<WrappedLine> buildWrappedLines(BitmapFont font) {
        // Available text width: subtract scrollbar space conservatively
        float totalW = BOX_W - PAD_X * 2f - SCROLLBAR_W - 3;
        List<WrappedLine> out = new ArrayList<>();

        for (ChatLine line : lines) {
            if (!shouldDisplayLine(line.type)) continue;

            String tsStr   = "[" + line.timestamp + "] ";
            gl.setText(font, tsStr);
            float tsW = gl.width;

            if (line.type == MessageType.SYSTEM) {
                List<String> chunks = wrapText(font, line.text, Math.max(32f, totalW - tsW));
                if (chunks.isEmpty()) chunks = List.of("");
                for (int i = 0; i < chunks.size(); i++) {
                    out.add(new WrappedLine(MessageType.SYSTEM,
                        i == 0 ? line.timestamp : "", "", chunks.get(i), i > 0));
                }
                continue;
            }

            // Public / Private: "Name: " prefix on every displayed line (for indent)
            String namePrefix = line.senderName + ": ";
            gl.setText(font, namePrefix);
            float prefixW = gl.width;

            // First physical line: timestamp + name + text (reduced available width)
            float line1W = Math.max(24f, totalW - tsW - prefixW);
            // Continuation lines: name indent + text (full width minus prefix)
            float contW  = Math.max(24f, totalW - prefixW);

            // Word-split the full message
            List<String> words = splitWords(line.text);
            int wi = 0;
            boolean first = true;

            while (wi < words.size() || (first && words.isEmpty())) {
                float maxW = first ? line1W : contW;
                StringBuilder sb = new StringBuilder();

                while (wi < words.size()) {
                    String candidate = sb.length() == 0 ? words.get(wi) : sb + " " + words.get(wi);
                    gl.setText(font, candidate);
                    if (gl.width <= maxW) {
                        sb.setLength(0);
                        sb.append(candidate);
                        wi++;
                    } else if (sb.length() == 0) {
                        // Single word wider than available: hard-break it
                        sb.append(hardBreak(font, words.get(wi), maxW));
                        wi++;
                        break;
                    } else {
                        break;
                    }
                }

                String segment = sb.toString();
                if (first) {
                    out.add(new WrappedLine(line.type, line.timestamp, namePrefix, segment, false));
                    first = false;
                } else {
                    out.add(new WrappedLine(line.type, "", namePrefix, segment, true));
                }

                if (words.isEmpty()) break;
            }
        }

        return out;
    }

    /** Split text into words (preserves non-empty tokens). */
    private List<String> splitWords(String text) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isEmpty()) return out;
        for (String w : text.split(" ")) {
            if (!w.isEmpty()) out.add(w);
        }
        return out;
    }

    /** Hard-break a single word that is too wide to fit on one line. */
    private String hardBreak(BitmapFont font, String word, float maxW) {
        int cut = word.length();
        while (cut > 1) {
            gl.setText(font, word.substring(0, cut));
            if (gl.width <= maxW) break;
            cut--;
        }
        return word.substring(0, cut);
    }

    /**
     * Word-wrap {@code text} to fit within {@code maxWidth} pixels.
     * Uses {@link #gl} for measurement — no extra GlyphLayout allocations.
     */
    private List<String> wrapText(BitmapFont font, String text, float maxWidth) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isEmpty()) return out;

        String[] words    = text.split(" ");
        StringBuilder cur = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty()) continue;
            String candidate = cur.length() == 0 ? word : cur + " " + word;
            gl.setText(font, candidate);
            if (gl.width <= maxWidth) {
                cur.setLength(0);
                cur.append(candidate);
            } else {
                if (cur.length() > 0) {
                    out.add(cur.toString());
                    cur.setLength(0);
                    cur.append(word);
                } else {
                    // Word wider than line: hard-break
                    String remainder = word;
                    while (!remainder.isEmpty()) {
                        int cut = remainder.length();
                        while (cut > 1) {
                            gl.setText(font, remainder.substring(0, cut));
                            if (gl.width <= maxWidth) break;
                            cut--;
                        }
                        out.add(remainder.substring(0, cut));
                        remainder = remainder.substring(cut);
                    }
                }
            }
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }
}
