package com.osrs.server.quest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Quest definition (loaded from YAML).
 * Represents a single quest with tasks and rewards.
 */
public class Quest {

    public enum QuestStatus {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED
    }
    
    public enum TaskType {
        DIALOGUE,
        KILL,
        COLLECT,
        ACTION,
        EQUIP,
        HAND_IN        // ← new: fires only when all other quest tasks are complete
    }
    
    public static class Task {
        public String id;
        public TaskType type;
        public String description;
        public int targetEntityId;  // NPC ID or item ID
        public int quantity;
        public int rewardXp;
        public boolean completed;
        
        public Task(String id, TaskType type, String desc, int target, int qty, int xp) {
            this.id = id;
            this.type = type;
            this.description = desc;
            this.targetEntityId = target;
            this.quantity = qty;
            this.rewardXp = xp;
            this.completed = false;
        }
    }
    
    public int id;
    public String name;
    public String description;
    public List<Task> tasks;
    public int totalRewardXp;
    public int rewardSkillIndex = 3;  // default = Hitpoints (SKILL_HITPOINTS = 3)
    public int questPointsReward;
    public boolean miniquest;
    public QuestStatus status;
    public boolean completed;
    
    public Quest(int id, String name, String desc) {
        this.id = id;
        this.name = name;
        this.description = desc;
        this.tasks = new ArrayList<>();
        this.totalRewardXp = 0;
        this.questPointsReward = 0;
        this.miniquest = false;
        this.status = QuestStatus.NOT_STARTED;
        this.completed = false;
    }
    
    public void addTask(Task task) {
        tasks.add(task);
        totalRewardXp += task.rewardXp;
    }
    
    public boolean allTasksCompleted() {
        return tasks.stream().allMatch(t -> t.completed);
    }
    
    public int getCompletedTaskCount() {
        return (int) tasks.stream().filter(t -> t.completed).count();
    }
}
