package com.osrs.client.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * UI for dialogue display and option selection.
 */
public class DialogueUI {
    
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
        
        // Calculate option position
        // Each option is ~20 pixels tall, starting at Y=200
        int optionStartY = 200;
        int optionHeight = 20;
        
        for (int i = 0; i < options.size(); i++) {
            int optionY = optionStartY + (i * optionHeight);
            
            if (mouseY >= optionY && mouseY < optionY + optionHeight) {
                return options.get(i);
            }
        }
        
        return null;
    }
    
    public boolean isVisible() {
        return visible;
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
