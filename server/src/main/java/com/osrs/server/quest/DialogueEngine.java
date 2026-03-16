package com.osrs.server.quest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dialogue system for NPC conversations.
 * Tracks dialogue state per player + NPC.
 */
public class DialogueEngine {
    
    public static class DialogueOption {
        public int optionId;
        public String text;
        public String nextDialogue;
        
        public DialogueOption(int id, String text, String next) {
            this.optionId = id;
            this.text = text;
            this.nextDialogue = next;
        }
    }
    
    public static class Dialogue {
        public String id;
        public int npcId;
        public String npcText;
        public List<DialogueOption> options;
        
        public Dialogue(String id, int npcId, String text) {
            this.id = id;
            this.npcId = npcId;
            this.npcText = text;
            this.options = new ArrayList<>();
        }
        
        public void addOption(DialogueOption option) {
            options.add(option);
        }
    }
    
    private final Map<String, Dialogue> dialogues = new HashMap<>();
    private final Map<Integer, String> playerDialogueState = new HashMap<>(); // Player ID -> current dialogue ID
    
    /**
     * Register a dialogue in the system.
     */
    public void registerDialogue(Dialogue dialogue) {
        dialogues.put(dialogue.id, dialogue);
    }
    
    /**
     * Start a dialogue with a player.
     */
    public Dialogue startDialogue(int playerId, String initialDialogueId) {
        playerDialogueState.put(playerId, initialDialogueId);
        return getDialogue(initialDialogueId);
    }
    
    /**
     * Get current dialogue for a player.
     */
    public Dialogue getCurrentDialogue(int playerId) {
        String dialogueId = playerDialogueState.get(playerId);
        if (dialogueId == null) {
            return null;
        }
        return dialogues.get(dialogueId);
    }
    
    /**
     * Player selects an option; progress dialogue.
     */
    public Dialogue selectOption(int playerId, int optionId) {
        Dialogue current = getCurrentDialogue(playerId);
        if (current == null) {
            return null;
        }
        
        for (DialogueOption option : current.options) {
            if (option.optionId == optionId && option.nextDialogue != null) {
                playerDialogueState.put(playerId, option.nextDialogue);
                return getDialogue(option.nextDialogue);
            }
        }
        
        // End dialogue if no valid next
        playerDialogueState.remove(playerId);
        return null;
    }
    
    /**
     * End dialogue for a player.
     */
    public void endDialogue(int playerId) {
        playerDialogueState.remove(playerId);
    }
    
    /**
     * Get dialogue by ID.
     */
    public Dialogue getDialogue(String dialogueId) {
        return dialogues.get(dialogueId);
    }
}
