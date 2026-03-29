package com.osrs.client.ui;

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
        if (!visible) {
            return null;
        }

        int optionX = PANEL_X + H_PADDING;
        int optionY = PANEL_Y + ChatBox.INPUT_H + CONTENT_TOP_PADDING;
        int optionW = PANEL_WIDTH - (H_PADDING * 2);

        for (int i = 0; i < options.size(); i++) {
            int y = optionY + i * (OPTION_HEIGHT + OPTION_GAP);

            boolean inX = mouseX >= optionX && mouseX < optionX + optionW;
            boolean inY = mouseY >= y && mouseY < y + OPTION_HEIGHT;
            if (inX && inY) {
                return options.get(i);
            }
        }
        
        return null;
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
