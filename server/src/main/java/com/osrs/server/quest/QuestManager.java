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
    private final Map<String, Integer> taskProgress = new HashMap<>(); // questId:taskId -> current count
    
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

        Quest playerQuest = copyQuest(quest);
        quests.put(playerQuest.id, playerQuest);
        questProgress.put(quest.id, 0);
        LOG.info("Player {} started quest: {}", playerId, quest.name);
    }
    
    /**
     * Mark a task as complete within a quest.
     */
    public void completeTask(int questId, String taskId) {
        addTaskProgress(questId, taskId, Integer.MAX_VALUE);
    }

    /**
     * Increment progress for a task by amount; returns true if the task is now complete.
     */
    public boolean addTaskProgress(int questId, String taskId, int amount) {
        Quest quest = quests.get(questId);
        if (quest == null) {
            LOG.warn("Player {} doesn't have quest {}", playerId, questId);
            return false;
        }

        for (Quest.Task task : quest.tasks) {
            if (task.id.equals(taskId)) {
                if (task.completed) {
                    return true;
                }

                String key = taskKey(questId, taskId);
                int current = taskProgress.getOrDefault(key, 0);
                int next = Math.min(task.quantity, current + Math.max(1, amount));
                taskProgress.put(key, next);

                if (next >= task.quantity) {
                    task.completed = true;
                    questProgress.put(questId, questProgress.getOrDefault(questId, 0) + 1);
                    LOG.info("Player {} completed task {} in quest {}", 
                        playerId, taskId, questId);
                    return true;
                }

                return false;
            }
        }

        LOG.warn("Player {} has no task {} in quest {}", playerId, taskId, questId);
        return false;
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

    /**
     * Returns current numeric progress for a task (0..quantity).
     */
    public int getTaskProgress(int questId, String taskId) {
        Quest quest = quests.get(questId);
        if (quest == null) return 0;
        for (Quest.Task task : quest.tasks) {
            if (task.id.equals(taskId)) {
                return taskProgress.getOrDefault(taskKey(questId, taskId), 0);
            }
        }
        return 0;
    }

    private static String taskKey(int questId, String taskId) {
        return questId + ":" + taskId;
    }

    private static Quest copyQuest(Quest source) {
        Quest copy = new Quest(source.id, source.name, source.description);
        for (Quest.Task task : source.tasks) {
            copy.addTask(new Quest.Task(
                task.id,
                task.type,
                task.description,
                task.targetEntityId,
                task.quantity,
                task.rewardXp
            ));
        }
        return copy;
    }
}
