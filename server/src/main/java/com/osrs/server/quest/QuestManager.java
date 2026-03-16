package com.osrs.server.quest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages quests for a single player.
 * Tracks progress, completion, and rewards.
 */
public class QuestManager {
    
    private static final Logger LOG = LoggerFactory.getLogger(QuestManager.class);
    
    private final int playerId;
    private final Map<Integer, Quest> quests = new HashMap<>();
    private final Map<Integer, Integer> questProgress = new HashMap<>(); // Quest ID -> completed task count
    
    public QuestManager(int playerId) {
        this.playerId = playerId;
    }
    
    /**
     * Start a quest for this player.
     */
    public void startQuest(Quest quest) {
        if (quests.containsKey(quest.id)) {
            LOG.warn("Player {} already has quest {}", playerId, quest.id);
            return;
        }
        
        quests.put(quest.id, quest);
        questProgress.put(quest.id, 0);
        LOG.info("Player {} started quest: {}", playerId, quest.name);
    }
    
    /**
     * Mark a task as complete within a quest.
     */
    public void completeTask(int questId, String taskId) {
        Quest quest = quests.get(questId);
        if (quest == null) {
            LOG.warn("Player {} doesn't have quest {}", playerId, questId);
            return;
        }
        
        for (Quest.Task task : quest.tasks) {
            if (task.id.equals(taskId)) {
                if (!task.completed) {
                    task.completed = true;
                    questProgress.put(questId, questProgress.get(questId) + 1);
                    LOG.info("Player {} completed task {} in quest {}", 
                        playerId, taskId, questId);
                }
                break;
            }
        }
    }
    
    /**
     * Check if quest is complete (all tasks done).
     */
    public boolean isQuestComplete(int questId) {
        Quest quest = quests.get(questId);
        return quest != null && quest.allTasksCompleted();
    }
    
    /**
     * Get a quest by ID.
     */
    public Quest getQuest(int questId) {
        return quests.get(questId);
    }
    
    /**
     * Get all quests for this player.
     */
    public Map<Integer, Quest> getQuests() {
        return new HashMap<>(quests);
    }
    
    /**
     * Get completion progress (0-100%).
     */
    public int getQuestProgress(int questId) {
        Quest quest = quests.get(questId);
        if (quest == null) {
            return 0;
        }
        
        if (quest.tasks.isEmpty()) {
            return 0;
        }
        
        int completed = (int) quest.tasks.stream().filter(t -> t.completed).count();
        return (completed * 100) / quest.tasks.size();
    }
}
