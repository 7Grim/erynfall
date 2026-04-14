package com.osrs.shared;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class RunecraftRegistry {

    public static final int RUNE_ESSENCE_ITEM_ID = 1436;

    public record AltarDef(String altarName,
                           int runeItemId,
                           String runeName,
                           int talismanItemId,
                           String talismanName,
                           int levelRequirement,
                           int xpTenthsPerEssence) {}

    private static final List<AltarDef> F2P_ALTARS = List.of(
        new AltarDef("Air Altar", 556, "Air rune", 1438, "Air talisman", 1, 50),
        new AltarDef("Mind Altar", 558, "Mind rune", 1448, "Mind talisman", 2, 55),
        new AltarDef("Water Altar", 555, "Water rune", 1452, "Water talisman", 5, 60),
        new AltarDef("Earth Altar", 557, "Earth rune", 1440, "Earth talisman", 9, 65),
        new AltarDef("Fire Altar", 554, "Fire rune", 1442, "Fire talisman", 14, 70),
        new AltarDef("Body Altar", 559, "Body rune", 1446, "Body talisman", 20, 75)
    );

    private static final Map<String, AltarDef> BY_ALTAR_NAME = F2P_ALTARS.stream()
        .collect(Collectors.toMap(a -> a.altarName().toLowerCase(), a -> a));

    private RunecraftRegistry() {
    }

    public static List<AltarDef> f2pAltars() {
        return F2P_ALTARS;
    }

    public static AltarDef getByAltarName(String altarName) {
        if (altarName == null || altarName.isBlank()) {
            return null;
        }
        return BY_ALTAR_NAME.get(altarName.trim().toLowerCase());
    }
}
