package com.osrs.server.world;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.osrs.shared.ItemDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemLoader {
    private static final Logger LOG = LoggerFactory.getLogger(ItemLoader.class);

    public static Map<Integer, ItemDefinition> loadItems() throws Exception {
        Map<Integer, ItemDefinition> items = new HashMap<>();
        InputStream is = ItemLoader.class.getClassLoader().getResourceAsStream("items.yaml");
        if (is == null) {
            LOG.warn("items.yaml not found — no item definitions loaded");
            return items;
        }
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Map<String, Object> yaml = mapper.readValue(is, Map.class);
        is.close();

        List<Map<String, Object>> itemList = (List<Map<String, Object>>) yaml.get("items");
        if (itemList == null) return items;

        for (Map<String, Object> raw : itemList) {
            ItemDefinition def = new ItemDefinition();
            def.id           = getInt(raw, "id", 0);
            def.name         = getString(raw, "name", "Unknown item");
            def.examine      = getString(raw, "examine", "");
            def.stackable    = getBool(raw, "stackable", false);
            def.equipable    = getBool(raw, "equipable", false);
            def.equipSlot    = getInt(raw, "equip_slot", -1);
            def.consumable   = getBool(raw, "consumable", false);
            def.consumeHeal  = getInt(raw, "consume_heal", 0);
            def.attackBonus  = getInt(raw, "attack_bonus", 0);
            def.strengthBonus= getInt(raw, "strength_bonus", 0);
            def.defenceBonus = getInt(raw, "defence_bonus", 0);
            if (def.id > 0) items.put(def.id, def);
        }
        LOG.info("Loaded {} item definitions", items.size());
        return items;
    }

    private static int getInt(Map<String, Object> m, String k, int d) {
        Object v = m.get(k); if (v == null) return d;
        return v instanceof Number ? ((Number) v).intValue() : d;
    }
    private static String getString(Map<String, Object> m, String k, String d) {
        Object v = m.get(k); return v != null ? v.toString() : d;
    }
    private static boolean getBool(Map<String, Object> m, String k, boolean d) {
        Object v = m.get(k); if (v instanceof Boolean) return (Boolean) v; return d;
    }
}
