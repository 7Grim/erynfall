package com.osrs.shared;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a player character.
 * Extends Entity with player-specific properties like stats, inventory, quests.
 */
public class Player extends Entity {

    public static class BankSlot {
        private final int slotIndex;
        private final int tabIndex;
        private final int itemId;
        private final long quantity;
        private final boolean placeholder;

        public BankSlot(int slotIndex, int tabIndex, int itemId, long quantity, boolean placeholder) {
            this.slotIndex = slotIndex;
            this.tabIndex = tabIndex;
            this.itemId = itemId;
            this.quantity = quantity;
            this.placeholder = placeholder;
        }

        public int getSlotIndex() {
            return slotIndex;
        }

        public int getTabIndex() {
            return tabIndex;
        }

        public int getItemId() {
            return itemId;
        }

        public long getQuantity() {
            return quantity;
        }

        public boolean isPlaceholder() {
            return placeholder;
        }
    }

    // Equipment (11 slots using EquipmentSlot constants)
    private int[] equipment = new int[EquipmentSlot.COUNT];

    // Inventory (28 slots — OSRS standard)
    private int[] inventoryItemIds = new int[28];
    private int[] inventoryQuantities = new int[28];
    private final List<BankSlot> bankSlots = new java.util.ArrayList<>();
    
    // Combat state
    private int combatTarget = -1;
    private long lastAttackTick = 0;
    private CombatStyle combatStyle = CombatStyle.AGGRESSIVE;
    private boolean autoRetaliate = true;
    /** Attack range (tiles) based on currently equipped weapon. Default 1 = melee. */
    private int weaponAttackRange = 1;
    /** Selected combat spell ID (SpellRegistry); -1 = no spell selected. */
    private int selectedSpellId = -1;
    /**
     * Quantity of item stacked in the AMMO slot (slot 3).
     * All other equipment slots hold a single item (quantity is implicitly 1).
     * Decremented on each ranged attack; slot is cleared when it reaches 0.
     */
    private int ammoQuantity = 0;
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
    private String skillingMetadata = "";

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
    public static final int SKILL_MINING       = 10;
    public static final int SKILL_SMITHING     = 11;
    public static final int SKILL_FIREMAKING   = 12;
    public static final int SKILL_CRAFTING     = 13;
    public static final int SKILL_RUNECRAFTING = 14;
    public static final int SKILL_FLETCHING    = 15;
    public static final int SKILL_AGILITY      = 16;
    public static final int SKILL_HERBLORE     = 17;
    public static final int SKILL_THIEVING     = 18;
    public static final int SKILL_SLAYER       = 19;
    public static final int SKILL_FARMING      = 20;
    public static final int SKILL_HUNTER       = 21;
    public static final int SKILL_CONSTRUCTION = 22;
    public static final int SKILL_COUNT        = 23;

    private boolean member = false;
    private boolean adminToolsEnabled = false;

    // Friends list data
    private final Set<Long> friends = new HashSet<>();
    private final Set<Long> blockedPlayers = new HashSet<>();
    private boolean friendsListVisible = false;

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
        // Hitpoints starts at level 10 in OSRS (1,154 XP = level 10; stored as tenths = 11540)
        skillXp[SKILL_HITPOINTS]    = 11540L;
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

    public void clearBank() {
        bankSlots.clear();
    }

    public List<BankSlot> getBankSlots() {
        return Collections.unmodifiableList(bankSlots);
    }

    public void setBankSlots(List<BankSlot> slots) {
        bankSlots.clear();
        if (slots != null) {
            bankSlots.addAll(slots);
        }
    }

    public BankSlot getBankSlot(int slotIndex) {
        for (BankSlot slot : bankSlots) {
            if (slot.getSlotIndex() == slotIndex) {
                return slot;
            }
        }
        return null;
    }

    public int findBankSlotByItemId(int itemId) {
        for (BankSlot slot : bankSlots) {
            if (slot.getItemId() == itemId) {
                return slot.getSlotIndex();
            }
        }
        return -1;
    }

    public int getOccupiedBankSlotCount() {
        return bankSlots.size();
    }

    public int getFirstFreeBankSlot() {
        for (int i = 0; i < getBankCapacity(); i++) {
            if (getBankSlot(i) == null) {
                return i;
            }
        }
        return -1;
    }

    public void setBankSlot(int slotIndex, int tabIndex, int itemId, long quantity, boolean placeholder) {
        removeBankSlot(slotIndex);
        if (itemId <= 0 || quantity <= 0) {
            return;
        }
        bankSlots.add(new BankSlot(slotIndex, tabIndex, itemId, quantity, placeholder));
    }

    public void removeBankSlot(int slotIndex) {
        bankSlots.removeIf(slot -> slot.getSlotIndex() == slotIndex);
    }

    public int getBankCapacity() {
        return 1000;
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

    public static long xpForLevelTenths(int level) {
        int clamped = Math.max(1, Math.min(99, level));
        return XP_TABLE[clamped - 1] * 10L;
    }

    public static long clampTenthsXp(long xpTenths) {
        return Math.max(0L, Math.min(2_000_000_000L, xpTenths));
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
        long wholeXp = xp / 10;  // XP stored as tenths; XP_TABLE uses whole values
        for (int i = 1; i < 99; i++) {
            if (wholeXp >= XP_TABLE[i]) level = i + 1;
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
    public boolean isAutoRetaliate() { return autoRetaliate; }
    public void setAutoRetaliate(boolean enabled) { this.autoRetaliate = enabled; }

    public int getWeaponAttackRange() { return weaponAttackRange; }
    public void setWeaponAttackRange(int range) { this.weaponAttackRange = range; }
    public int getAmmoQuantity() { return ammoQuantity; }
    public void setAmmoQuantity(int qty) { this.ammoQuantity = Math.max(0, qty); }
    public int getSelectedSpellId() { return selectedSpellId; }
    public void setSelectedSpellId(int id) { this.selectedSpellId = id; }

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
    public String getSkillingMetadata() { return skillingMetadata == null ? "" : skillingMetadata; }

    public boolean isSkilling() {
        return skillingAction != SkillingAction.NONE && skillingTargetNpcId >= 0;
    }

    public void startSkillingAction(SkillingAction action, int targetNpcId, long nextAttemptTick) {
        this.skillingAction = action == null ? SkillingAction.NONE : action;
        this.skillingTargetNpcId = targetNpcId;
        this.skillingNextAttemptTick = Math.max(0L, nextAttemptTick);
        this.skillingNextMoveTick = 0L;
        this.skillingActiveAnnounced = false;
        this.skillingMetadata = "";
    }

    public void setSkillingNextAttemptTick(long tick) {
        this.skillingNextAttemptTick = Math.max(0L, tick);
    }

    public void setSkillingNextMoveTick(long tick) {
        this.skillingNextMoveTick = Math.max(0L, tick);
    }

    public void setSkillingMetadata(String skillingMetadata) {
        this.skillingMetadata = skillingMetadata == null ? "" : skillingMetadata;
    }

    public void clearSkillingAction() {
        this.skillingAction = SkillingAction.NONE;
        this.skillingTargetNpcId = -1;
        this.skillingNextAttemptTick = 0L;
        this.skillingNextMoveTick = 0L;
        this.skillingActiveAnnounced = false;
        this.skillingMetadata = "";
    }

    public void markSkillingActiveAnnounced() {
        this.skillingActiveAnnounced = true;
    }

    public boolean isMember() { return member; }
    public void setMember(boolean member) { this.member = member; }
    public boolean isAdminToolsEnabled() { return adminToolsEnabled; }
    public void setAdminToolsEnabled(boolean adminToolsEnabled) { this.adminToolsEnabled = adminToolsEnabled; }

    // -----------------------------------------------------------------------
    // Prayer points
    // -----------------------------------------------------------------------

    // -----------------------------------------------------------------------
    // Active prayers
    // -----------------------------------------------------------------------

    /** IDs of currently active prayers. Empty when no prayer is active. */
    private final java.util.Set<Integer> activePrayers = new java.util.HashSet<>();

    public boolean isPrayerActive(int id)   { return activePrayers.contains(id); }
    public void activatePrayer(int id)      { activePrayers.add(id); }
    public void deactivatePrayer(int id)    { activePrayers.remove(id); }
    public void deactivateAllPrayers()      { activePrayers.clear(); }
    public boolean hasAnyActivePrayer()     { return !activePrayers.isEmpty(); }
    public java.util.Set<Integer> getActivePrayers() {
        return java.util.Collections.unmodifiableSet(activePrayers);
    }

    /** Current prayer points. Range: 0 .. getMaxPrayerPoints(). */
    private int prayerPoints = 0;

    public int getPrayerPoints()  { return prayerPoints; }
    public int getMaxPrayerPoints() { return skillLevel[SKILL_PRAYER]; }
    public void setPrayerPoints(int points) {
        this.prayerPoints = Math.max(0, Math.min(points, getMaxPrayerPoints()));
    }

    // -----------------------------------------------------------------------
    // Friends list
    // -----------------------------------------------------------------------

    public boolean hasFriend(long playerId) {
        return friends.contains(playerId);
    }

    public boolean isBlocked(long playerId) {
        return blockedPlayers.contains(playerId);
    }

    public void addFriend(long playerId) {
        friends.add(playerId);
        blockedPlayers.remove(playerId);
        friendsListVisible = true;
    }

    public void removeFriend(long playerId) {
        friends.remove(playerId);
        if (!friendsListVisible) {
            friendsListVisible = true;
        }
    }

    public void clearFriends() {
        friends.clear();
    }

    public Set<Long> getFriends() {
        return Collections.unmodifiableSet(friends);
    }

    public Set<Long> getBlockedPlayers() {
        return Collections.unmodifiableSet(blockedPlayers);
    }

    public void blockPlayer(long playerId) {
        blockedPlayers.add(playerId);
        friends.remove(playerId);
    }

    public void removeFromBlock(long playerId) {
        blockedPlayers.remove(playerId);
    }

    public boolean getFriendsListVisible() {
        return friendsListVisible;
    }

    public void setFriendsListVisible(boolean visible) {
        this.friendsListVisible = visible;
    }
}
