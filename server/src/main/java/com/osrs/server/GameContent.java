package com.osrs.server;

import com.osrs.server.quest.DialogueEngine;
import com.osrs.server.quest.Quest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Game content initialization (quests, dialogues, etc.).
 * Loads Tutorial Island content.
 */
public class GameContent {
    
    private static final Logger LOG = LoggerFactory.getLogger(GameContent.class);
    
    private final DialogueEngine dialogueEngine;
    private final Map<Integer, String> npcInitialDialogueIds;
    
    public GameContent() {
        this.dialogueEngine = new DialogueEngine();
        this.npcInitialDialogueIds = new HashMap<>();
    }
    
    /**
     * Initialize Tutorial Island quests and dialogues.
     */
    public void initializeTutorialIsland() {
        LOG.info("Initializing Tutorial Island content");
        
        // Create Tutorial Quest
        Quest tutorialQuest = new Quest(1, "Tutorial Island", "Learn the basics of RuneScape");
        tutorialQuest.addTask(new Quest.Task("speak_guide", Quest.TaskType.DIALOGUE, 
            "Speak to the Tutorial Guide", 1, 1, 10));
        tutorialQuest.addTask(new Quest.Task("kill_rats", Quest.TaskType.KILL, 
            "Kill 5 rats", 3, 5, 50));
        tutorialQuest.addTask(new Quest.Task("collect_logs", Quest.TaskType.COLLECT, 
            "Collect 10 logs", 101, 10, 50));
        
        LOG.info("Registered Tutorial Quest: {} (ID: {})", tutorialQuest.name, tutorialQuest.id);
        
        // Create Dialogues
        DialogueEngine.Dialogue intro = new DialogueEngine.Dialogue("dialogue_tutorial_intro", 1,
            "Welcome to Tutorial Island, adventurer!");
        intro.addOption(new DialogueEngine.DialogueOption(1, "Tell me about combat", 
            "dialogue_combat_intro"));
        intro.addOption(new DialogueEngine.DialogueOption(2, "I'll explore on my own", null));
        dialogueEngine.registerDialogue(intro);
        npcInitialDialogueIds.put(1, intro.id);
        
        DialogueEngine.Dialogue combatIntro = new DialogueEngine.Dialogue("dialogue_combat_intro", 1,
            "Combat is essential! You'll develop Attack, Strength, and Defence skills.");
        combatIntro.addOption(new DialogueEngine.DialogueOption(1, "Where can I practice?", 
            "dialogue_combat_location"));
        combatIntro.addOption(new DialogueEngine.DialogueOption(2, "Back to intro", 
            "dialogue_tutorial_intro"));
        dialogueEngine.registerDialogue(combatIntro);
        
        DialogueEngine.Dialogue combatLocation = new DialogueEngine.Dialogue("dialogue_combat_location", 1,
            "Head east to find some rats. They're perfect for practice!");
        combatLocation.addOption(new DialogueEngine.DialogueOption(1, "Thanks!", null));
        dialogueEngine.registerDialogue(combatLocation);

        DialogueEngine.Dialogue instructorIntro = new DialogueEngine.Dialogue("dialogue_combat_instructor", 2,
            "Ready to learn how to fight properly?");
        instructorIntro.addOption(new DialogueEngine.DialogueOption(1, "Yes, teach me", "dialogue_combat_lesson"));
        instructorIntro.addOption(new DialogueEngine.DialogueOption(2, "Not now", null));
        dialogueEngine.registerDialogue(instructorIntro);
        npcInitialDialogueIds.put(2, instructorIntro.id);

        DialogueEngine.Dialogue lesson = new DialogueEngine.Dialogue("dialogue_combat_lesson", 2,
            "Good! Go defeat 5 rats and return to me.");
        lesson.addOption(new DialogueEngine.DialogueOption(1, "I'll do it", null));
        dialogueEngine.registerDialogue(lesson);
        
        LOG.info("Registered {} NPC dialogue entry points", npcInitialDialogueIds.size());
    }
    
    public DialogueEngine getDialogueEngine() {
        return dialogueEngine;
    }

    public String getInitialDialogueIdForNpc(int npcId) {
        return npcInitialDialogueIds.get(npcId);
    }
}
