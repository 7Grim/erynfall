package com.osrs.shared;

public class ItemDefinition {
    public int id;
    public String name = "Unknown item";
    public String examine = "";
    public boolean stackable;
    public boolean equipable;
    public int equipSlot = -1;  // -1 = not equipable; 0-10 = EquipmentSlot constant
    public boolean consumable;
    public int consumeHeal;     // HP healed (food/potion)
    public int attackBonus;
    public int strengthBonus;
    public int defenceBonus;

    /** Bit flags for client display: 0x1=equippable 0x2=consumable 0x4=stackable */
    public int getFlags() {
        int f = 0;
        if (equipable)  f |= 0x1;
        if (consumable) f |= 0x2;
        if (stackable)  f |= 0x4;
        return f;
    }
}
