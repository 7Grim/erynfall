package com.osrs.server;

import com.osrs.server.network.NettyServer;
import com.osrs.server.network.PlayerSession;
import com.osrs.server.quest.Quest;
import com.osrs.server.quest.QuestManager;
import com.osrs.server.world.World;
import com.osrs.shared.Player;
import com.osrs.shared.SkillingAction;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Deterministic starter-zone regression test.
 *
 * No real network, no DB. Uses EmbeddedChannel + tickOnce() to drive the game
 * loop directly and verify core gameplay invariants:
 *  - Arrival Guide NPC is reachable from spawn
 *  - Woodcutting a tree awards XP
 *  - collect_logs quest task advances when logs land in inventory
 */
class StarterZoneGameplayTest {

    // Tree NPC id=10 (definitionId=100 "Tree") at (46, 106) in main_world/world.yml
    private static final int TREE_NPC_ID = 10;
    private static final int TREE_X = 46;
    private static final int TREE_Y = 106;

    // Arrival Guide at (56, 95); player spawns at (56, 96)
    private static final int GUIDE_NPC_ID = 1;

    // Bronze axe item ID from WoodcuttingRegistry
    private static final int BRONZE_AXE_ID = 1351;

    // Regular log item ID from WoodcuttingRegistry.TREE
    private static final int LOG_ITEM_ID = 1511;

    private static final int PLAYER_ID = 100001;
    private static final int SESSION_ID = 1;

    // ~5000 ticks gives many woodcutting roll attempts (chop attempt every 4 ticks)
    private static final int MAX_TICKS = 5000;

    private World world;
    private GameContent gameContent;
    private NettyServer nettyServer;
    private Player player;
    private PlayerSession session;
    private GameLoop loop;

    @BeforeEach
    void setUp() throws Exception {
        world = new World();
        gameContent = new GameContent();
        gameContent.initializeForWorld("main_world");

        // tickIntervalNs irrelevant — we call tickOnce() directly, never start()
        nettyServer = new NettyServer(0, 1, 1, world, gameContent);

        // Spawn at tutorial island start; give bronze axe in slot 0
        player = new Player(PLAYER_ID, "TestPlayer", 56, 96);
        player.setInventoryItem(0, BRONZE_AXE_ID, 1);
        world.getPlayers().put(PLAYER_ID, player);

        EmbeddedChannel channel = new EmbeddedChannel();
        session = new PlayerSession(SESSION_ID, channel);
        session.setPlayer(player);
        session.setAuthenticated(true);

        QuestManager questManager = new QuestManager(PLAYER_ID);
        Map<Integer, Quest> questDefs = gameContent.getQuestDefinitions();
        Quest tutorialQuest = questDefs.get(QuestManager.STARTER_QUEST_ID);
        assertNotNull(tutorialQuest, "Tutorial quest (id=1) must be present in quests.yaml");
        questManager.startQuest(tutorialQuest);
        session.setQuestManager(questManager);

        nettyServer.getSessions().put(SESSION_ID, session);

        loop = new GameLoop(3_906_250L, world, nettyServer);
    }

    @Test
    void arrivalGuideIsAdjacentToSpawn() {
        // Guide at (56,95), player spawns at (56,96) — chebyshev distance 1
        var guide = world.getNPC(GUIDE_NPC_ID);
        assertNotNull(guide, "Arrival Guide NPC must be present in world");

        int dx = Math.abs(player.getX() - guide.getX());
        int dy = Math.abs(player.getY() - guide.getY());
        assertTrue(Math.max(dx, dy) <= 1,
            "Player spawn must be within 1 tile of Arrival Guide for immediate interaction");
    }

    @Test
    void woodcuttingAwardsXpAndAdvancesQuestTask() {
        // Move player adjacent to the tree
        player.setPosition(TREE_X + 1, TREE_Y);

        long xpBefore = player.getSkillXp(Player.SKILL_WOODCUTTING);

        // Start woodcutting action targeting tree NPC 10
        player.startSkillingAction(SkillingAction.WOODCUTTING, TREE_NPC_ID, 0L);

        // Drive ticks until XP gained or budget exhausted
        boolean xpGained = false;
        for (int tick = 0; tick < MAX_TICKS; tick++) {
            loop.tickOnce();
            if (player.getSkillXp(Player.SKILL_WOODCUTTING) > xpBefore) {
                xpGained = true;
                break;
            }
        }

        assertTrue(xpGained,
            "Woodcutting a Tree must award XP within " + MAX_TICKS + " ticks");

        // Quest collect_logs task (item_id=1511, count=5) should have progressed
        QuestManager qm = session.getQuestManager();
        int logTaskProgress = qm.getTaskProgress(QuestManager.STARTER_QUEST_ID, "collect_logs");
        assertTrue(logTaskProgress > 0,
            "collect_logs quest task must advance when regular logs are obtained");
    }
}
