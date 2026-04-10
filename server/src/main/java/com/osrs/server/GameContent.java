package com.osrs.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.osrs.server.quest.DialogueEngine;
import com.osrs.server.quest.Quest;
import com.osrs.shared.ShopDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;

/**
 * Game content initialization (quests, dialogues, etc.).
 * Loads Tutorial Island content.
 */
public class GameContent {
    
    private static final Logger LOG = LoggerFactory.getLogger(GameContent.class);
    
    private final DialogueEngine dialogueEngine;
    private final Map<Integer, String> npcInitialDialogueIds;
    private final Map<Integer, Quest> questDefinitions;
    private final Map<String, ShopDefinition> shopDefinitionsById;
    private final Map<String, ShopDefinition> shopDefinitionsByNpcName;
    
    public GameContent() {
        this.dialogueEngine = new DialogueEngine();
        this.npcInitialDialogueIds = new HashMap<>();
        this.questDefinitions = new LinkedHashMap<>();
        this.shopDefinitionsById = new LinkedHashMap<>();
        this.shopDefinitionsByNpcName = new LinkedHashMap<>();
    }
    
    /**
     * Initialize Tutorial Island quests and dialogues.
     */
    public void initializeTutorialIsland() {
        LOG.info("Initializing Tutorial Island content");

        int dialogueCount = loadDialoguesFromYaml();
        int questCount = loadQuestsFromYaml();
        int shopCount = loadShopsFromYaml();

        LOG.info("Loaded {} dialogues, {} quest definitions, {} shops, {} NPC dialogue entry points",
            dialogueCount, questCount, shopCount, npcInitialDialogueIds.size());
    }

    @SuppressWarnings("unchecked")
    private int loadDialoguesFromYaml() {
        try (InputStream is = openContentStream("dialogue.yaml")) {
            if (is == null) {
                LOG.warn("dialogue.yaml not found; dialogue system will have no conversations");
                return 0;
            }

            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            Map<String, Object> root = mapper.readValue(is, Map.class);
            Object dialoguesObj = root.get("dialogues");
            if (!(dialoguesObj instanceof Map<?, ?> dialoguesMapRaw)) {
                LOG.warn("dialogue.yaml missing 'dialogues' map");
                return 0;
            }

            Map<String, Map<String, Object>> dialoguesMap = (Map<String, Map<String, Object>>) dialoguesMapRaw;
            Set<String> dialogueIds = dialoguesMap.keySet();
            int loaded = 0;

            for (Map.Entry<String, Map<String, Object>> entry : dialoguesMap.entrySet()) {
                String dialogueId = entry.getKey();
                Map<String, Object> data = entry.getValue();
                int npcId = getInt(data, "npc_id", -1);
                String npcText = getString(data, "npc_says", "");
                if (npcId < 0 || npcText.isEmpty()) {
                    LOG.warn("Skipping invalid dialogue '{}' (npc_id={}, npc_says length={})",
                        dialogueId, npcId, npcText.length());
                    continue;
                }

                DialogueEngine.Dialogue dialogue = new DialogueEngine.Dialogue(dialogueId, npcId, npcText);
                Object optionsObj = data.get("options");
                if (optionsObj instanceof List<?> options) {
                    for (Object optionObj : options) {
                        if (!(optionObj instanceof Map<?, ?> optionMapRaw)) continue;
                        Map<String, Object> optionMap = (Map<String, Object>) optionMapRaw;
                        int optionId = getInt(optionMap, "option_id", -1);
                        String text = getString(optionMap, "text", "");
                        String nextDialogue = getNullableString(optionMap.get("next_dialogue"));
                        if (optionId < 0 || text.isEmpty()) {
                            LOG.warn("Skipping invalid option in dialogue '{}' (option_id={}, text='{}')",
                                dialogueId, optionId, text);
                            continue;
                        }
                        dialogue.addOption(new DialogueEngine.DialogueOption(optionId, text, nextDialogue));
                    }
                }

                for (DialogueEngine.DialogueOption option : dialogue.options) {
                    if (option.nextDialogue != null && !dialogueIds.contains(option.nextDialogue)) {
                        LOG.warn("Dialogue '{}' option {} points to missing next_dialogue '{}' — ending dialogue instead",
                            dialogueId, option.optionId, option.nextDialogue);
                        option.nextDialogue = null;
                    }
                }

                dialogueEngine.registerDialogue(dialogue);
                npcInitialDialogueIds.putIfAbsent(npcId, dialogueId);
                loaded++;
            }

            return loaded;
        } catch (Exception e) {
            LOG.error("Failed to load dialogue.yaml", e);
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private int loadQuestsFromYaml() {
        try (InputStream is = openContentStream("quests.yaml")) {
            if (is == null) {
                LOG.warn("quests.yaml not found; quest definitions disabled");
                return 0;
            }

            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            Map<String, Object> root = mapper.readValue(is, Map.class);
            Object questsObj = root.get("quests");
            if (!(questsObj instanceof List<?> questList)) {
                LOG.warn("quests.yaml missing 'quests' list");
                return 0;
            }

            int loaded = 0;
            for (Object questObj : questList) {
                if (!(questObj instanceof Map<?, ?> questMapRaw)) continue;
                Map<String, Object> questMap = (Map<String, Object>) questMapRaw;

                int id = getInt(questMap, "id", -1);
                String name = getString(questMap, "name", "");
                String description = getString(questMap, "description", "");
                if (id < 0 || name.isEmpty()) {
                    LOG.warn("Skipping invalid quest (id={}, name='{}')", id, name);
                    continue;
                }

                Quest quest = new Quest(id, name, description);
                quest.questPointsReward = getInt(questMap, "quest_points", 0);
                quest.miniquest = Boolean.TRUE.equals(questMap.get("miniquest"));
                quest.rewardSkillIndex = getInt(questMap, "reward_skill_index", 3);
                Object tasksObj = questMap.get("tasks");
                if (tasksObj instanceof List<?> tasks) {
                    for (Object taskObj : tasks) {
                        if (!(taskObj instanceof Map<?, ?> taskMapRaw)) continue;
                        Map<String, Object> taskMap = (Map<String, Object>) taskMapRaw;

                        String taskId = getString(taskMap, "id", "");
                        String typeStr = getString(taskMap, "type", "").toLowerCase();
                        String taskDesc = getString(taskMap, "description", taskId.replace('_', ' '));
                        int quantity = getInt(taskMap, "count", 1);
                        int rewardXp = getInt(taskMap, "reward_xp", 0);
                        int target = getInt(taskMap, "npc_id", getInt(taskMap, "item_id", 0));

                        Quest.TaskType taskType;
                        try {
                            taskType = switch (typeStr) {
                                case "dialogue" -> Quest.TaskType.DIALOGUE;
                                case "kill" -> Quest.TaskType.KILL;
                                case "collect" -> Quest.TaskType.COLLECT;
                                case "action" -> Quest.TaskType.ACTION;
                                case "equip" -> Quest.TaskType.EQUIP;
                                case "hand_in" -> Quest.TaskType.HAND_IN;
                                default -> throw new IllegalArgumentException();
                            };
                        } catch (IllegalArgumentException e) {
                            LOG.warn("Skipping task '{}' in quest {} due to unknown type '{}'", taskId, id, typeStr);
                            continue;
                        }

                        if (taskId.isEmpty()) {
                            LOG.warn("Skipping unnamed task in quest {}", id);
                            continue;
                        }

                        quest.addTask(new Quest.Task(taskId, taskType, taskDesc, target, quantity, rewardXp));
                    }
                }

                questDefinitions.put(id, quest);
                loaded++;
            }

            return loaded;
        } catch (Exception e) {
            LOG.error("Failed to load quests.yaml", e);
            return 0;
        }
    }

    private InputStream openContentStream(String fileName) throws IOException {
        InputStream classpathStream = GameContent.class.getClassLoader().getResourceAsStream(fileName);
        if (classpathStream != null) {
            return classpathStream;
        }

        Path fromRepoRoot = Paths.get("assets", "data", fileName);
        if (Files.exists(fromRepoRoot)) {
            return Files.newInputStream(fromRepoRoot);
        }

        Path fromServerModule = Paths.get("..", "assets", "data", fileName);
        if (Files.exists(fromServerModule)) {
            return Files.newInputStream(fromServerModule);
        }

        return null;
    }

    private static int getInt(Map<String, Object> map, String key, int defaultValue) {
        if (!map.containsKey(key)) return defaultValue;
        Object val = map.get(key);
        if (val instanceof Integer integer) return integer;
        if (val instanceof Number number) return number.intValue();
        return defaultValue;
    }

    private static String getString(Map<String, Object> map, String key, String defaultValue) {
        if (!map.containsKey(key)) return defaultValue;
        Object val = map.get(key);
        return val == null ? defaultValue : val.toString();
    }

    private static String getNullableString(Object value) {
        if (value == null) return null;
        String asString = value.toString();
        return asString.isBlank() ? null : asString;
    }

    @SuppressWarnings("unchecked")
    private int loadShopsFromYaml() {
        shopDefinitionsById.clear();
        shopDefinitionsByNpcName.clear();

        try (InputStream is = openContentStream("shops.yaml")) {
            if (is == null) {
                LOG.info("shops.yaml not found; no data-driven shops loaded");
                return 0;
            }

            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            Map<String, Object> root = mapper.readValue(is, Map.class);
            Object shopsObj = root.get("shops");
            if (!(shopsObj instanceof List<?> shopsList)) {
                LOG.warn("shops.yaml missing 'shops' list");
                return 0;
            }

            int loaded = 0;
            for (Object shopObj : shopsList) {
                if (!(shopObj instanceof Map<?, ?> shopMapRaw)) {
                    continue;
                }
                Map<String, Object> shopMap = (Map<String, Object>) shopMapRaw;
                String id = getString(shopMap, "id", "").trim();
                String name = getString(shopMap, "name", "").trim();
                String npcName = getString(shopMap, "npc_name", "").trim();
                if (id.isEmpty() || name.isEmpty() || npcName.isEmpty()) {
                    LOG.warn("Skipping invalid shop definition (id='{}', name='{}', npc_name='{}')", id, name, npcName);
                    continue;
                }

                List<ShopDefinition.StockEntry> stockEntries = new java.util.ArrayList<>();
                Object stockObj = shopMap.get("stock");
                if (stockObj instanceof List<?> stockList) {
                    for (Object stockEntryObj : stockList) {
                        if (!(stockEntryObj instanceof Map<?, ?> stockMapRaw)) {
                            continue;
                        }
                        Map<String, Object> stockMap = (Map<String, Object>) stockMapRaw;
                        int itemId = getInt(stockMap, "item_id", -1);
                        int quantity = Math.max(0, getInt(stockMap, "quantity", 0));
                        Integer price = stockMap.containsKey("price")
                            ? Integer.valueOf(getInt(stockMap, "price", 0))
                            : null;
                        if (itemId <= 0 || quantity <= 0) {
                            LOG.warn("Skipping invalid stock entry in shop '{}' (item_id={}, quantity={})", id, itemId, quantity);
                            continue;
                        }
                        if (price != null && price <= 0) {
                            price = null;
                        }
                        stockEntries.add(new ShopDefinition.StockEntry(itemId, quantity, price));
                    }
                }

                if (stockEntries.isEmpty()) {
                    LOG.warn("Skipping shop '{}' because it has no valid stock entries", id);
                    continue;
                }

                ShopDefinition shop = new ShopDefinition(id, name, npcName, List.copyOf(stockEntries));
                shopDefinitionsById.put(id, shop);
                shopDefinitionsByNpcName.put(npcName.toLowerCase(Locale.ROOT), shop);
                loaded++;
            }

            return loaded;
        } catch (Exception e) {
            LOG.error("Failed to load shops.yaml", e);
            return 0;
        }
    }
    
    public DialogueEngine getDialogueEngine() {
        return dialogueEngine;
    }

    public String getInitialDialogueIdForNpc(int npcId) {
        return npcInitialDialogueIds.get(npcId);
    }

    public Map<Integer, Quest> getQuestDefinitions() {
        return new LinkedHashMap<>(questDefinitions);
    }

    public ShopDefinition getShopByNpcName(String npcName) {
        if (npcName == null || npcName.isBlank()) {
            return null;
        }
        return shopDefinitionsByNpcName.get(npcName.toLowerCase(Locale.ROOT));
    }

    public ShopDefinition getShopById(String shopId) {
        if (shopId == null || shopId.isBlank()) {
            return null;
        }
        return shopDefinitionsById.get(shopId);
    }
}
