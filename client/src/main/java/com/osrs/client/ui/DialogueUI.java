package com.osrs.client.ui;

import com.badlogic.gdx.graphics.Color;

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
    public static final Color NPC_TEXT_COLOR = new Color(0f, 0.38f, 1f, 1f);
    public static final Color OPTION_TEXT_COLOR = Color.WHITE;
    public static final Color OPTION_HOVER_TEXT_COLOR = new Color(1f, 0.78f, 0.10f, 1f);
    public static final Color OPTION_BG_COLOR = new Color(0.10f, 0.10f, 0.16f, 0.72f);
    public static final Color OPTION_HOVER_BG_COLOR = new Color(0.18f, 0.15f, 0.08f, 0.90f);
    
    public static class DialogueOption {
        public int optionId;
        public String text;
        
        public DialogueOption(int id, String text) {
            this.optionId = id;
            this.text = text;
        }
    }
    
    private String currentDialogueId;
    private int npcId;
    private String npcText;
    private List<DialogueOption> options = new ArrayList<>();
    private boolean visible = false;
    
    /**
     * Open a dialogue.
     */
    public void open(String dialogueId, int npcId, String npcText, List<DialogueOption> options) {
        this.currentDialogueId = dialogueId;
        this.npcId = npcId;
        this.npcText = npcText;
        this.options = new ArrayList<>(options);
        this.visible = true;
    }
    
    /**
     * Close dialogue.
     */
    public void close() {
        this.visible = false;
        this.options.clear();
    }
    
    /**
     * Get selected option (if any).
     */
    public DialogueOption getSelectedOption(int mouseX, int mouseY) {
        int index = getHoveredOptionIndex(mouseX, mouseY);
        if (index < 0 || index >= options.size()) {
            return null;
        }
        return options.get(index);
    }

    public int getHoveredOptionIndex(int mouseX, int mouseY) {
        if (!visible) {
            return -1;
        }

        int optionX = PANEL_X + H_PADDING;
        int optionY = PANEL_Y + ChatBox.INPUT_H + CONTENT_TOP_PADDING;
        int optionW = PANEL_WIDTH - (H_PADDING * 2);

        for (int i = 0; i < options.size(); i++) {
            int y = optionY + i * (OPTION_HEIGHT + OPTION_GAP);

            boolean inX = mouseX >= optionX && mouseX < optionX + optionW;
            boolean inY = mouseY >= y && mouseY < y + OPTION_HEIGHT;
            if (inX && inY) {
                return i;
            }
        }

        return -1;
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
    
    public int getNpcId() {
        return npcId;
    }
    
    public String getCurrentDialogueId() {
        return currentDialogueId;
    }
}
