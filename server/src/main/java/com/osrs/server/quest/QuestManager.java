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
        playerQuest.status = Quest.QuestStatus.IN_PROGRESS;
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

                    if (quest.allTasksCompleted()) {
                        quest.completed = true;
                        quest.status = Quest.QuestStatus.COMPLETED;
                    }

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

    /**
     * Applies persisted DB state to a started quest.
     * status: 0=not started, 1=in progress, 2=completed.
     */
    public void applyPersistedState(int questId, int status, int completedObjectivesBitmask) {
        Quest quest = quests.get(questId);
        if (quest == null) return;

        int completedCount = 0;
        for (int i = 0; i < quest.tasks.size(); i++) {
            Quest.Task task = quest.tasks.get(i);
            boolean done = (completedObjectivesBitmask & (1 << i)) != 0;
            task.completed = done;
            taskProgress.put(taskKey(questId, task.id), done ? task.quantity : 0);
            if (done) completedCount++;
        }
        questProgress.put(questId, completedCount);

        if (status <= 0) {
            quest.status = Quest.QuestStatus.NOT_STARTED;
            quest.completed = false;
        } else if (status == 1) {
            quest.status = Quest.QuestStatus.IN_PROGRESS;
            quest.completed = quest.allTasksCompleted();
            if (quest.completed) quest.status = Quest.QuestStatus.COMPLETED;
        } else {
            quest.status = Quest.QuestStatus.COMPLETED;
            quest.completed = true;
            for (Quest.Task task : quest.tasks) {
                task.completed = true;
                taskProgress.put(taskKey(questId, task.id), task.quantity);
            }
            questProgress.put(questId, quest.tasks.size());
        }
    }

    /**
     * Applies persisted per-task count progress.
     */
    public void applyPersistedTaskProgress(int questId, String taskId, int progressCount) {
        Quest quest = quests.get(questId);
        if (quest == null) return;

        for (Quest.Task task : quest.tasks) {
            if (!task.id.equals(taskId)) continue;

            int clamped = Math.max(0, Math.min(task.quantity, progressCount));
            taskProgress.put(taskKey(questId, taskId), clamped);
            task.completed = clamped >= task.quantity;
            recomputeQuestProgress(quest);
            return;
        }
    }

    public int getCompletedObjectivesBitmask(int questId) {
        Quest quest = quests.get(questId);
        if (quest == null) return 0;
        int bitmask = 0;
        for (int i = 0; i < quest.tasks.size() && i < 31; i++) {
            if (quest.tasks.get(i).completed) {
                bitmask |= (1 << i);
            }
        }
        return bitmask;
    }

    public int getStatusCode(int questId) {
        Quest quest = quests.get(questId);
        if (quest == null) return 0;
        return switch (quest.status) {
            case NOT_STARTED -> 0;
            case IN_PROGRESS -> 1;
            case COMPLETED -> 2;
        };
    }

    private void recomputeQuestProgress(Quest quest) {
        int completedCount = 0;
        boolean anyProgress = false;
        for (Quest.Task task : quest.tasks) {
            int current = taskProgress.getOrDefault(taskKey(quest.id, task.id), 0);
            if (current > 0) anyProgress = true;
            if (task.completed) completedCount++;
        }
        questProgress.put(quest.id, completedCount);

        if (quest.allTasksCompleted()) {
            quest.completed = true;
            quest.status = Quest.QuestStatus.COMPLETED;
            return;
        }

        quest.completed = false;
        if (anyProgress && quest.status == Quest.QuestStatus.NOT_STARTED) {
            quest.status = Quest.QuestStatus.IN_PROGRESS;
        }
    }

    private static String taskKey(int questId, String taskId) {
        return questId + ":" + taskId;
    }

    private static Quest copyQuest(Quest source) {
        Quest copy = new Quest(source.id, source.name, source.description);
        copy.questPointsReward = source.questPointsReward;
        copy.miniquest = source.miniquest;
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

    public int getTotalQuestPoints() {
        int total = 0;
        for (Quest quest : quests.values()) {
            if (quest.status == Quest.QuestStatus.COMPLETED) {
                total += Math.max(0, quest.questPointsReward);
            }
        }
        return total;
    }
}
