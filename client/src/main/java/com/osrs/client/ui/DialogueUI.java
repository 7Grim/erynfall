package com.osrs.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;

import java.util.ArrayList;
import java.util.List;

/**
 * UI for dialogue display and option selection.
 */
public class DialogueUI {
    public static final int PANEL_X = 0;
    public static final int PANEL_Y = 0;
    public static final int PANEL_WIDTH = ChatBox.BOX_W;
    public static final int PANEL_HEIGHT = ChatBox.TOTAL_H;
    public static final int H_PADDING = 8;
    public static final int CONTENT_TOP_PADDING = 8;
    public static final int OPTION_HEIGHT = 18;
    public static final int OPTION_GAP = 2;

    public static final int BORDER_THICKNESS = 2;
    public static final Color DIALOGUE_BG_COLOR = new Color(0f, 0f, 0.1f, 0.75f);
    public static final Color BORDER_COLOR = new Color(0.75f, 0.60f, 0.10f, 1f);
    public static final Color HEADER_COLOR = new Color(1f, 0.92f, 0f, 1f);
    public static final Color NPC_TEXT_COLOR = FontManager.TEXT_CYAN;
    public static final Color OPTION_TEXT_COLOR = Color.WHITE;
    public static final Color OPTION_HOVER_TEXT_COLOR = new Color(1f, 0.78f, 0.10f, 1f);
    public static final Color OPTION_BG_COLOR = new Color(0.10f, 0.10f, 0.16f, 0.72f);
    public static final Color OPTION_HOVER_BG_COLOR = new Color(0.18f, 0.15f, 0.08f, 0.90f);

    public enum DialoguePhase {
        NPC_SPEAKING,
        PLAYER_CHOOSING,
        NPC_RESPONDING,
        CLOSED
    }

    private static final int CONTINUE_W = 168;
    private static final int CONTINUE_H = 18;
    private static final int BACK_W = 80;
    private static final int BACK_H = 18;
    
    public static class DialogueOption {
        public int optionId;
        public String text;
        
        public DialogueOption(int id, String text) {
            this.optionId = id;
            this.text = text;
        }
    }
    
    private String currentDialogueId;
    private int npcId = -1;
    private String npcText = "";
    private List<String> npcTextLines = new ArrayList<>();
    private List<DialogueOption> options = new ArrayList<>();

    private DialoguePhase currentPhase = DialoguePhase.CLOSED;
    private int currentDialogueLineIndex = 0;
    private boolean hasBackButton = false;
    private boolean visible = false;
    
    /**
     * Open a dialogue.
     */
    public void open(String dialogueId, int npcId, String npcText, List<DialogueOption> options) {
        this.currentDialogueId = dialogueId;
        this.npcId = npcId;
        this.npcText = npcText != null ? npcText : "";
        this.npcTextLines = splitLines(this.npcText);
        this.options = new ArrayList<>(options);
        this.currentDialogueLineIndex = 0;
        this.hasBackButton = false;
        if (!npcTextLines.isEmpty()) {
            this.currentPhase = DialoguePhase.NPC_SPEAKING;
        } else if (!this.options.isEmpty()) {
            this.currentPhase = DialoguePhase.PLAYER_CHOOSING;
        } else {
            // Nothing to show — do not open.
            this.currentPhase = DialoguePhase.CLOSED;
            this.visible = false;
            return;
        }
        this.visible = true;
    }

    public void open(String dialogueId, int npcId, List<String> npcTextLines) {
        this.currentDialogueId = dialogueId;
        this.npcId = npcId;
        this.npcTextLines = new ArrayList<>(npcTextLines);
        this.npcText = String.join("\n", this.npcTextLines);
        this.currentDialogueLineIndex = 0;
        this.options.clear();
        this.currentPhase = DialoguePhase.NPC_SPEAKING;
        this.hasBackButton = false;
        this.visible = true;
    }
    
    /**
     * Close dialogue.
     */
    public void close() {
        this.currentPhase = DialoguePhase.CLOSED;
        this.visible = false;
        this.npcText = "";
        this.npcTextLines.clear();
        this.options.clear();
        this.hasBackButton = false;
        this.currentDialogueLineIndex = 0;
        this.npcId = -1;
    }

    /**
     * Advance to next NPC dialogue line. Transitions from NPC_SPEAKING to PLAYER_CHOOSING
     * when all lines have been shown.
     */
    public void advanceToNextLine() {
        if (currentPhase != DialoguePhase.NPC_SPEAKING && currentPhase != DialoguePhase.NPC_RESPONDING) return;

        currentDialogueLineIndex++;
        if (currentDialogueLineIndex >= npcTextLines.size()) {
            if (!options.isEmpty()) {
                currentPhase = DialoguePhase.PLAYER_CHOOSING;
                currentDialogueLineIndex = -1;
            } else {
                close();
            }
        }
    }

    private int countLines(String text) {
        return text.isEmpty() ? 0 : text.split("\\n").length;
    }

    /**
     * Show player choice options. Transition from any phase to PLAYER_CHOOSING.
     * @param allowBack If true, adds a "Back" button to return to previous dialogue.
     */
    public void showOptions(List<DialogueOption> options, boolean allowBack) {
        this.options = new ArrayList<>(options);
        this.currentPhase = DialoguePhase.PLAYER_CHOOSING;
        this.hasBackButton = allowBack;
        this.currentDialogueLineIndex = -1;
        this.visible = true;
    }

    /**
     * Player selected an option. Sends choice to server.
     * Server should send next dialogue state or response based on choice.
     */
    public void selectOption(int optionIndex) {
        if (optionIndex < 0 || optionIndex >= options.size()) return;
        DialogueOption selected = options.get(optionIndex);
        if (selected.optionId < 0) return;
        // No local state transition here; server controls the next phase.
    }
    
    /**
     * Get selected option (if any).
     */
    public DialogueOption getSelectedOption(int mouseX, int mouseY) {
        if (currentPhase != DialoguePhase.PLAYER_CHOOSING) {
            return null;
        }
        if (hasBackButton && isBackButtonHit(mouseX, mouseY)) {
            return new DialogueOption(-1, "Back");
        }
        int index = getHoveredOptionIndex(mouseX, mouseY);
        if (index < 0 || index >= options.size()) {
            return null;
        }
        return options.get(index);
    }

    public int getHoveredOptionIndex(int mouseX, int mouseY) {
        if (!visible || currentPhase != DialoguePhase.PLAYER_CHOOSING) {
            return -1;
        }

        int optionX = PANEL_X + H_PADDING;
        int firstOptionY = PANEL_Y + PANEL_HEIGHT - 8 - CONTENT_TOP_PADDING - OPTION_HEIGHT;
        int optionW = PANEL_WIDTH - (H_PADDING * 2);

        for (int i = 0; i < options.size(); i++) {
            int y = firstOptionY - i * (OPTION_HEIGHT + OPTION_GAP);

            boolean inX = mouseX >= optionX && mouseX < optionX + optionW;
            boolean inY = mouseY >= y && mouseY < y + OPTION_HEIGHT;
            if (inX && inY) {
                return i;
            }
        }

        return -1;
    }

    public boolean isContinueButtonHit(int mouseX, int mouseY) {
        if (!visible) return false;
        if (currentPhase != DialoguePhase.NPC_SPEAKING && currentPhase != DialoguePhase.NPC_RESPONDING) {
            return false;
        }
        int bx = continueButtonX();
        int by = continueButtonY();
        return mouseX >= bx && mouseX < bx + CONTINUE_W
            && mouseY >= by && mouseY < by + CONTINUE_H;
    }

    private boolean isBackButtonHit(int mouseX, int mouseY) {
        int bx = backButtonX();
        int by = backButtonY();
        return mouseX >= bx && mouseX < bx + BACK_W
            && mouseY >= by && mouseY < by + BACK_H;
    }

    public void render(ShapeRenderer sr, SpriteBatch batch, BitmapFont font,
                       Matrix4 proj, int mouseX, int mouseY) {
        if (!visible || currentPhase == DialoguePhase.CLOSED) return;

        int optionX = PANEL_X + H_PADDING;
        // Option i=0 sits just below the header; options go downward (decreasing Y).
        int firstOptionY = PANEL_Y + PANEL_HEIGHT - 8 - CONTENT_TOP_PADDING - OPTION_HEIGHT;
        int optionW = PANEL_WIDTH - (H_PADDING * 2);

        sr.setProjectionMatrix(proj);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(DIALOGUE_BG_COLOR);
        sr.rect(PANEL_X, PANEL_Y, PANEL_WIDTH, PANEL_HEIGHT);
        sr.setColor(0.25f, 0.22f, 0.12f, 0.85f);
        sr.rect(PANEL_X, PANEL_Y + ChatBox.INPUT_H, PANEL_WIDTH, 1);
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(BORDER_COLOR);
        sr.rect(PANEL_X, PANEL_Y, PANEL_WIDTH, BORDER_THICKNESS);
        sr.rect(PANEL_X, PANEL_Y + PANEL_HEIGHT - BORDER_THICKNESS, PANEL_WIDTH, BORDER_THICKNESS);
        sr.rect(PANEL_X, PANEL_Y, BORDER_THICKNESS, PANEL_HEIGHT);
        sr.rect(PANEL_X + PANEL_WIDTH - BORDER_THICKNESS, PANEL_Y, BORDER_THICKNESS, PANEL_HEIGHT);
        sr.end();

        if (currentPhase == DialoguePhase.PLAYER_CHOOSING) {
            int hovered = getHoveredOptionIndex(mouseX, mouseY);
            sr.begin(ShapeRenderer.ShapeType.Filled);
            for (int i = 0; i < options.size(); i++) {
                int y = firstOptionY - i * (OPTION_HEIGHT + OPTION_GAP);
                sr.setColor(i == hovered ? OPTION_HOVER_BG_COLOR : OPTION_BG_COLOR);
                sr.rect(optionX, y, optionW, OPTION_HEIGHT);
            }
            if (hasBackButton) {
                int bx = backButtonX();
                int by = backButtonY();
                sr.setColor(isBackButtonHit(mouseX, mouseY) ? OPTION_HOVER_BG_COLOR : OPTION_BG_COLOR);
                sr.rect(bx, by, BACK_W, BACK_H);
            }
            sr.end();
        } else {
            int bx = continueButtonX();
            int by = continueButtonY();
            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(isContinueButtonHit(mouseX, mouseY) ? OPTION_HOVER_BG_COLOR : OPTION_BG_COLOR);
            sr.rect(bx, by, CONTINUE_W, CONTINUE_H);
            sr.end();
        }

        batch.setProjectionMatrix(proj);
        batch.begin();

        if (currentPhase == DialoguePhase.PLAYER_CHOOSING) {
            font.getData().setScale(FontManager.getScale(FontManager.FontContext.SMALL_LABEL));
            font.setColor(HEADER_COLOR);
            font.draw(batch, "Choose Option", PANEL_X + 8, PANEL_Y + PANEL_HEIGHT - 8);

            font.getData().setScale(FontManager.getScale(FontManager.FontContext.TOOLTIP));
            int hovered = getHoveredOptionIndex(mouseX, mouseY);
            for (int i = 0; i < options.size(); i++) {
                int y = firstOptionY - i * (OPTION_HEIGHT + OPTION_GAP);
                font.setColor(i == hovered ? OPTION_HOVER_TEXT_COLOR : OPTION_TEXT_COLOR);
                font.draw(batch, (i + 1) + ". " + options.get(i).text, optionX + 8, y + 14);
            }

            if (hasBackButton) {
                font.getData().setScale(FontManager.getScale(FontManager.FontContext.SMALL_LABEL));
                font.setColor(isBackButtonHit(mouseX, mouseY) ? OPTION_HOVER_TEXT_COLOR : OPTION_TEXT_COLOR);
                font.draw(batch, "Back", backButtonX() + 8, backButtonY() + 14);
            }
        } else {
            font.getData().setScale(FontManager.getScale(FontManager.FontContext.TOOLTIP));
            font.setColor(NPC_TEXT_COLOR);
            String currentLine = getCurrentNpcLine();
            GlyphLayout layout = new GlyphLayout(font, currentLine, NPC_TEXT_COLOR,
                optionW, -1, true);
            font.draw(batch, layout, optionX, PANEL_Y + PANEL_HEIGHT - 20);

            font.getData().setScale(FontManager.getScale(FontManager.FontContext.SMALL_LABEL));
            font.setColor(isContinueButtonHit(mouseX, mouseY) ? OPTION_HOVER_TEXT_COLOR : HEADER_COLOR);
            font.draw(batch, "Click here to continue", continueButtonX() + 8, continueButtonY() + 14);
        }

        batch.end();
        font.getData().setScale(FontManager.getScale(FontManager.FontContext.BASE_UI));
        font.setColor(Color.WHITE);
    }
    
    public boolean isVisible() {
        return visible;
    }

    /** Returns true if (mouseX, mouseY) is within the dialogue panel area. */
    public boolean isOverDialogue(int mouseX, int mouseY) {
        return mouseX >= PANEL_X && mouseX < PANEL_X + PANEL_WIDTH
            && mouseY >= PANEL_Y && mouseY < PANEL_Y + PANEL_HEIGHT;
    }
    
    public String getNpcText() {
        return npcText;
    }
    
    public List<DialogueOption> getOptions() {
        return options;
    }

    public List<DialogueOption> getCurrentOptions() {
        return options;
    }

    public DialoguePhase getCurrentPhase() {
        return currentPhase;
    }

    public boolean hasBackButton() {
        return hasBackButton;
    }

    public int getCurrentDialogueLineIndex() {
        return currentDialogueLineIndex;
    }
    
    public int getNpcId() {
        return npcId;
    }
    
    public String getCurrentDialogueId() {
        return currentDialogueId;
    }

    private List<String> splitLines(String text) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isEmpty()) return out;
        for (String s : text.split("\\n")) {
            if (!s.isEmpty()) out.add(s);
        }
        if (out.isEmpty()) out.add(text);
        return out;
    }

    private String getCurrentNpcLine() {
        if (npcTextLines.isEmpty()) return "";
        if (currentDialogueLineIndex < 0) return npcTextLines.get(0);
        int idx = Math.min(currentDialogueLineIndex, npcTextLines.size() - 1);
        return npcTextLines.get(idx);
    }

    private int continueButtonX() {
        return PANEL_X + (PANEL_WIDTH - CONTINUE_W) / 2;
    }

    private int continueButtonY() {
        return PANEL_Y + ChatBox.INPUT_H + 2;
    }

    private int backButtonX() {
        return PANEL_X + H_PADDING;
    }

    private int backButtonY() {
        // Back sits below the last numbered option (option index = options.size()-1).
        int firstOptionY = PANEL_Y + PANEL_HEIGHT - 8 - CONTENT_TOP_PADDING - OPTION_HEIGHT;
        return firstOptionY - options.size() * (OPTION_HEIGHT + OPTION_GAP);
    }
}
