package com.osrs.shared;

/**
 * Represents a player character.
 * Extends Entity with player-specific properties like stats, inventory, quests.
 */
public class Player extends Entity {

    // Equipment (11 slots using EquipmentSlot constants)
    private int[] equipment = new int[EquipmentSlot.COUNT];

    // Inventory (28 slots — OSRS standard)
    private int[] inventoryItemIds = new int[28];
    private int[] inventoryQuantities = new int[28];
    
    // Combat state
    private int combatTarget = -1;
    private long lastAttackTick = 0;
    private CombatStyle combatStyle = CombatStyle.AGGRESSIVE;
    /** Attack range (tiles) based on currently equipped weapon. Default 1 = melee. */
    private int weaponAttackRange = 1;
    /** PID — lower value = higher priority in simultaneous action resolution. Re-randomized every 100-150 OSRS ticks. */
    private long pid = 0;

    // Dialogue state
    private int dialogueTarget = -1;

    // Server-authoritative skilling action state
    private SkillingAction skillingAction = SkillingAction.NONE;
    private int skillingTargetNpcId = -1;
    private long skillingNextAttemptTick = 0;
    private long skillingNextMoveTick = 0;
    private boolean skillingActiveAnnounced = false;

    // -----------------------------------------------------------------------
    // Skill indices: 0=Attack 1=Strength 2=Defence 3=Hitpoints 4=Ranged 5=Magic
    // -----------------------------------------------------------------------
    public static final int SKILL_ATTACK    = 0;
    public static final int SKILL_STRENGTH  = 1;
    public static final int SKILL_DEFENCE   = 2;
    public static final int SKILL_HITPOINTS = 3;
    public static final int SKILL_RANGED    = 4;
    public static final int SKILL_MAGIC     = 5;
    public static final int SKILL_PRAYER      = 6;
    public static final int SKILL_WOODCUTTING = 7;
    public static final int SKILL_FISHING     = 8;
    public static final int SKILL_COOKING     = 9;
    public static final int SKILL_COUNT       = 10;

    /** Total XP accumulated per skill. */
    private final long[] skillXp    = new long[SKILL_COUNT];
    /** Current level per skill (1–99, derived from XP). */
    private final int[]  skillLevel = new int[SKILL_COUNT];

    // OSRS XP table: xpTable[level-1] = XP needed to reach that level.
    private static final long[] XP_TABLE = buildXpTable();

    private static long[] buildXpTable() {
        long[] table = new long[99];
        table[0] = 0;
        long points = 0;
        for (int lvl = 1; lvl < 99; lvl++) {
            points += (long) Math.floor(lvl + 300.0 * Math.pow(2.0, lvl / 7.0));
            table[lvl] = (long) Math.floor(points / 4.0);
        }
        return table;
    }
    
    public Player(int id, String name, int x, int y) {
        super(id, name, x, y);
        this.health = 10;
        this.maxHealth = 10;
        for (int i = 0; i < SKILL_COUNT; i++) skillLevel[i] = 1;
        // Hitpoints starts at level 10 in OSRS (1,154 XP = level 10)
        skillXp[SKILL_HITPOINTS]    = 1154L;
        skillLevel[SKILL_HITPOINTS] = 10;
    }
    
    public int getCombatTarget() {
        return combatTarget;
    }
    
    public void setCombatTarget(int entityId) {
        this.combatTarget = entityId;
    }
    
    public boolean isInCombat() {
        return combatTarget >= 0;
    }
    
    public long getLastAttackTick() {
        return lastAttackTick;
    }
    
    public void setLastAttackTick(long tick) {
        this.lastAttackTick = tick;
    }
    
    public void setEquipment(int slot, int itemId) {
        if (slot >= 0 && slot < equipment.length) {
            equipment[slot] = itemId;
        }
    }
    
    public int getEquipment(int slot) {
        return (slot >= 0 && slot < equipment.length) ? equipment[slot] : 0;
    }
    
    public void setInventoryItem(int slot, int itemId, int quantity) {
        if (slot >= 0 && slot < 28) {
            inventoryItemIds[slot] = itemId;
            inventoryQuantities[slot] = quantity;
        }
    }

    public int getInventoryItemId(int slot) {
        return (slot >= 0 && slot < 28) ? inventoryItemIds[slot] : 0;
    }

    public int getInventoryQuantity(int slot) {
        return (slot >= 0 && slot < 28) ? inventoryQuantities[slot] : 0;
    }

    /**
     * Returns the index of the first empty inventory slot, or -1 if full.
     */
    public int getFirstEmptySlot() {
        for (int i = 0; i < 28; i++) {
            if (inventoryItemIds[i] == 0) return i;
        }
        return -1;
    }

    /**
     * Returns true if every inventory slot is occupied.
     */
    public boolean isInventoryFull() {
        return getFirstEmptySlot() < 0;
    }
    
    // -----------------------------------------------------------------------
    // Skill access
    // -----------------------------------------------------------------------

    /** Returns current level for the given skill index (0-5). */
    public int getSkillLevel(int skillIdx) {
        return (skillIdx >= 0 && skillIdx < SKILL_COUNT) ? skillLevel[skillIdx] : 1;
    }

    /** Returns total XP for the given skill index. */
    public long getSkillXp(int skillIdx) {
        return (skillIdx >= 0 && skillIdx < SKILL_COUNT) ? skillXp[skillIdx] : 0;
    }

    /** Directly sets XP and recomputes level — used when loading from DB. */
    public void setSkillXp(int skillIdx, long xp) {
        if (skillIdx < 0 || skillIdx >= SKILL_COUNT) return;
        skillXp[skillIdx]   = xp;
        skillLevel[skillIdx] = levelFromXp(xp);
    }

    /**
     * Awards XP to a skill. Recomputes the level and returns true if a
     * level-up occurred (caller should broadcast SkillUpdate with leveled_up=true).
     */
    public boolean addSkillXp(int skillIdx, long amount) {
        if (skillIdx < 0 || skillIdx >= SKILL_COUNT || amount <= 0) return false;
        skillXp[skillIdx] += amount;
        int newLevel = levelFromXp(skillXp[skillIdx]);
        boolean leveledUp = newLevel > skillLevel[skillIdx];
        skillLevel[skillIdx] = newLevel;
        return leveledUp;
    }

    private static int levelFromXp(long xp) {
        int level = 1;
        for (int i = 1; i < 99; i++) {
            if (xp >= XP_TABLE[i]) level = i + 1;
            else break;
        }
        return Math.min(level, 99);
    }

    // Convenience shortcuts used by CombatEngine
    public int getAttackLevel()   { return skillLevel[SKILL_ATTACK]; }
    public int getStrengthLevel() { return skillLevel[SKILL_STRENGTH]; }
    public int getDefenceLevel()  { return skillLevel[SKILL_DEFENCE]; }

    // -----------------------------------------------------------------------
    // Combat style
    // -----------------------------------------------------------------------

    public CombatStyle getCombatStyle() { return combatStyle; }
    public void setCombatStyle(CombatStyle style) { this.combatStyle = style; }

    public int getWeaponAttackRange() { return weaponAttackRange; }
    public void setWeaponAttackRange(int range) { this.weaponAttackRange = range; }

    /**
     * Effective attack range: weapon range + 2 if using Longrange style (capped at 10).
     * OSRS: Longrange adds +2 tiles to ranged weapons; melee Defensive style does NOT add range.
     */
    public int getAttackRange() {
        if (combatStyle == CombatStyle.DEFENSIVE && weaponAttackRange > 1) {
            return Math.min(10, weaponAttackRange + 2);
        }
        return weaponAttackRange;
    }

    public long getPid() { return pid; }
    public void setPid(long pid) { this.pid = pid; }

    // -----------------------------------------------------------------------
    // Dialogue
    // -----------------------------------------------------------------------

    public int getDialogueTarget() { return dialogueTarget; }
    public void setDialogueTarget(int npcId) { this.dialogueTarget = npcId; }
    public boolean isInDialogue() { return dialogueTarget >= 0; }

    // -----------------------------------------------------------------------
    // Skilling action state
    // -----------------------------------------------------------------------

    public SkillingAction getSkillingAction() { return skillingAction; }
    public int getSkillingTargetNpcId() { return skillingTargetNpcId; }
    public long getSkillingNextAttemptTick() { return skillingNextAttemptTick; }
    public long getSkillingNextMoveTick() { return skillingNextMoveTick; }
    public boolean isSkillingActiveAnnounced() { return skillingActiveAnnounced; }

    public boolean isSkilling() {
        return skillingAction != SkillingAction.NONE && skillingTargetNpcId >= 0;
    }

    public void startSkillingAction(SkillingAction action, int targetNpcId, long nextAttemptTick) {
        this.skillingAction = action == null ? SkillingAction.NONE : action;
        this.skillingTargetNpcId = targetNpcId;
        this.skillingNextAttemptTick = Math.max(0L, nextAttemptTick);
        this.skillingNextMoveTick = 0L;
        this.skillingActiveAnnounced = false;
    }

    public void setSkillingNextAttemptTick(long tick) {
        this.skillingNextAttemptTick = Math.max(0L, tick);
    }

    public void setSkillingNextMoveTick(long tick) {
        this.skillingNextMoveTick = Math.max(0L, tick);
    }

    public void clearSkillingAction() {
        this.skillingAction = SkillingAction.NONE;
        this.skillingTargetNpcId = -1;
        this.skillingNextAttemptTick = 0L;
        this.skillingNextMoveTick = 0L;
        this.skillingActiveAnnounced = false;
    }

    public void markSkillingActiveAnnounced() {
        this.skillingActiveAnnounced = true;
    }
}
