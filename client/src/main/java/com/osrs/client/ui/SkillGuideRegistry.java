package com.osrs.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Align;
import com.osrs.shared.FishingRegistry;
import com.osrs.shared.WoodcuttingRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SkillGuideRegistry {

    private static final int SKILL_WOODCUTTING = 7;
    private static final int SKILL_FISHING = 8;
    private static final Map<Integer, SkillGuidePopup.SkillGuideProvider> PROVIDERS = new HashMap<>();

    static {
        register(SKILL_WOODCUTTING, new WoodcuttingGuideProvider());
        register(SKILL_FISHING, new FishingGuideProvider());
    }

    private SkillGuideRegistry() {
    }

    public static void register(int skillIdx, SkillGuidePopup.SkillGuideProvider provider) {
        if (provider == null) {
            PROVIDERS.remove(skillIdx);
        } else {
            PROVIDERS.put(skillIdx, provider);
        }
    }

    public static SkillGuidePopup.SkillGuideProvider get(int skillIdx) {
        return PROVIDERS.get(skillIdx);
    }

    private static String formatXpTenths(int xpTenths) {
        if (xpTenths % 10 == 0) {
            return Integer.toString(xpTenths / 10);
        }
        return (xpTenths / 10) + "." + Math.abs(xpTenths % 10);
    }

    private static final class WoodcuttingGuideProvider implements SkillGuidePopup.SkillGuideProvider {

        private static final List<SkillGuidePopup.GuideSection> SECTIONS = List.of(
            new SkillGuidePopup.GuideSection("Introduction"),
            new SkillGuidePopup.GuideSection("Trees"),
            new SkillGuidePopup.GuideSection("Axes")
        );

        private static final Color TEXT_MAIN = new Color(0.24f, 0.16f, 0.06f, 1f);
        private static final Color TEXT_LOCKED = new Color(0.45f, 0.36f, 0.24f, 1f);
        private static final Color TEXT_UNLOCKED = new Color(0.22f, 0.14f, 0.06f, 1f);
        private static final Color TEXT_NEXT = new Color(0.48f, 0.28f, 0.04f, 1f);
        private static final Color ROW_UNLOCKED = new Color(0.88f, 0.81f, 0.67f, 1f);
        private static final Color ROW_LOCKED = new Color(0.78f, 0.71f, 0.57f, 1f);
        private static final Color ROW_NEXT = new Color(0.94f, 0.82f, 0.50f, 1f);

        @Override
        public String getTitle(int skillIdx) {
            return "Woodcutting";
        }

        @Override
        public List<SkillGuidePopup.GuideSection> getSections(int skillIdx) {
            return SECTIONS;
        }

        @Override
        public void renderSectionContent(ShapeRenderer shapeRenderer,
                                         SpriteBatch batch,
                                         BitmapFont font,
                                         Matrix4 projection,
                                         int skillIdx,
                                         int level,
                                         long totalXp,
                                         int sectionIdx,
                                         float contentX,
                                         float contentY,
                                         float contentW,
                                         float contentH,
                                         float scrollOffset) {
            if (sectionIdx == 0) {
                renderIntroduction(shapeRenderer, batch, font, projection, contentX, contentY, contentW, contentH);
            } else if (sectionIdx == 1) {
                renderTrees(shapeRenderer, batch, font, projection, level, contentX, contentY, contentW, contentH, scrollOffset);
            } else if (sectionIdx == 2) {
                renderAxes(shapeRenderer, batch, font, projection, level, contentX, contentY, contentW, contentH, scrollOffset);
            }
        }

        @Override
        public float getSectionContentHeight(int skillIdx, int level, int sectionIdx, float contentW) {
            if (sectionIdx == 1) {
                return 18f + WoodcuttingRegistry.trees().size() * 32f + 12f;
            }
            if (sectionIdx == 2) {
                return 18f + WoodcuttingRegistry.axes().size() * 32f + 12f;
            }
            return 236f;
        }

        private void renderIntroduction(ShapeRenderer shapeRenderer,
                                        SpriteBatch batch,
                                        BitmapFont font,
                                        Matrix4 projection,
                                        float x,
                                        float y,
                                        float w,
                                        float h) {
            final float blockH = 82f;
            final float top = y + h - 14f;
            final float blockX = x + 8f;
            final float blockW = w - 16f;
            final float dividerX = x + 10f;
            final float dividerW = w - 20f;
            final float iconX = x + 20f;
            final float textX = x + 62f;
            final float textW = w - 86f;
            final float textTopPad = 16f;
            final String[] texts = {
                "Chop trees to collect logs used in crafting and firemaking.",
                "As your Woodcutting level rises, your chop success improves.",
                "Better axes cut faster. Axes work from inventory even when you do not meet the Attack level to equip them."
            };

            shapeRenderer.setProjectionMatrix(projection);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            for (int i = 0; i < 3; i++) {
                float by = top - (i + 1) * blockH;
                shapeRenderer.setColor(0.92f, 0.84f, 0.69f, 1f);
                shapeRenderer.rect(blockX, by, blockW, blockH - 8f);
                shapeRenderer.setColor(0.64f, 0.50f, 0.30f, 1f);
                shapeRenderer.rect(dividerX, by + blockH - 16f, dividerW, 2f);
            }
            drawTreeIcon(shapeRenderer, iconX, top - blockH + 12f);
            ItemIconRenderer.drawItemIcon(shapeRenderer, iconX, top - blockH * 2 + 12f, 1511);
            ItemIconRenderer.drawItemIcon(shapeRenderer, iconX, top - blockH * 3 + 12f, 1351);
            shapeRenderer.end();

            GlyphLayout wrapped = new GlyphLayout();
            batch.setProjectionMatrix(projection);
            batch.begin();
            font.getData().setScale(0.68f);
            font.setColor(TEXT_MAIN);
            for (int i = 0; i < 3; i++) {
                float by = top - (i + 1) * blockH;
                float blockInnerTop = by + blockH - 8f;
                wrapped.setText(font, texts[i], TEXT_MAIN, textW, Align.left, true);
                font.draw(batch, wrapped, textX, blockInnerTop - textTopPad);
            }
            font.getData().setScale(1f);
            font.setColor(Color.WHITE);
            batch.end();
        }

        private void renderTrees(ShapeRenderer shapeRenderer,
                                 SpriteBatch batch,
                                 BitmapFont font,
                                 Matrix4 projection,
                                 int level,
                                 float x,
                                 float y,
                                 float w,
                                 float h,
                                 float scrollOffset) {
            float rowH = 32f;
            int nextReq = findNextTreeLevel(level);

            shapeRenderer.setProjectionMatrix(projection);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            float yCursor = y + h - 14f + scrollOffset;
            for (WoodcuttingRegistry.TreeTier tree : WoodcuttingRegistry.trees()) {
                float rowY = yCursor - rowH;
                boolean visible = rowY + rowH >= y && rowY <= y + h;
                if (visible) {
                    boolean unlocked = level >= tree.levelRequirement();
                    boolean next = !unlocked && tree.levelRequirement() == nextReq;
                    if (next) {
                        shapeRenderer.setColor(ROW_NEXT);
                    } else if (unlocked) {
                        shapeRenderer.setColor(ROW_UNLOCKED);
                    } else {
                        shapeRenderer.setColor(ROW_LOCKED);
                    }
                    shapeRenderer.rect(x + 8, rowY + 2, w - 16, rowH - 4);
                    ItemIconRenderer.drawItemIcon(shapeRenderer, x + 72, rowY + 1, tree.logItemId());
                }
                yCursor -= rowH;
            }
            shapeRenderer.end();

            batch.setProjectionMatrix(projection);
            batch.begin();
            font.getData().setScale(0.68f);
            yCursor = y + h - 14f + scrollOffset;
            for (WoodcuttingRegistry.TreeTier tree : WoodcuttingRegistry.trees()) {
                float rowY = yCursor - rowH;
                boolean visible = rowY + rowH >= y && rowY <= y + h;
                if (visible) {
                    boolean unlocked = level >= tree.levelRequirement();
                    boolean next = !unlocked && tree.levelRequirement() == nextReq;
                    if (next) {
                        font.setColor(TEXT_NEXT);
                    } else if (unlocked) {
                        font.setColor(TEXT_UNLOCKED);
                    } else {
                        font.setColor(TEXT_LOCKED);
                    }
                    font.draw(batch, "Lv " + tree.levelRequirement(), x + 16, rowY + 21);
                    font.draw(batch, tree.name(), x + 116, rowY + 21);
                    font.draw(batch, formatXpTenths(tree.xpTenths()) + " xp", x + w - 88, rowY + 21);
                }
                yCursor -= rowH;
            }
            batch.end();
        }

        private void renderAxes(ShapeRenderer shapeRenderer,
                                SpriteBatch batch,
                                BitmapFont font,
                                Matrix4 projection,
                                int level,
                                float x,
                                float y,
                                float w,
                                float h,
                                float scrollOffset) {
            float rowH = 32f;
            int nextReq = findNextAxeLevel(level);

            shapeRenderer.setProjectionMatrix(projection);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            float yCursor = y + h - 14f + scrollOffset;
            for (WoodcuttingRegistry.AxeTier axe : WoodcuttingRegistry.axes()) {
                float rowY = yCursor - rowH;
                boolean visible = rowY + rowH >= y && rowY <= y + h;
                if (visible) {
                    boolean unlocked = level >= axe.woodcuttingLevel();
                    boolean next = !unlocked && axe.woodcuttingLevel() == nextReq;
                    if (next) {
                        shapeRenderer.setColor(ROW_NEXT);
                    } else if (unlocked) {
                        shapeRenderer.setColor(ROW_UNLOCKED);
                    } else {
                        shapeRenderer.setColor(ROW_LOCKED);
                    }
                    shapeRenderer.rect(x + 8, rowY + 2, w - 16, rowH - 4);
                    ItemIconRenderer.drawItemIcon(shapeRenderer, x + 72, rowY + 1, axe.itemId());
                }
                yCursor -= rowH;
            }
            shapeRenderer.end();

            batch.setProjectionMatrix(projection);
            batch.begin();
            font.getData().setScale(0.68f);
            yCursor = y + h - 14f + scrollOffset;
            for (WoodcuttingRegistry.AxeTier axe : WoodcuttingRegistry.axes()) {
                float rowY = yCursor - rowH;
                boolean visible = rowY + rowH >= y && rowY <= y + h;
                if (visible) {
                    boolean unlocked = level >= axe.woodcuttingLevel();
                    boolean next = !unlocked && axe.woodcuttingLevel() == nextReq;
                    if (next) {
                        font.setColor(TEXT_NEXT);
                    } else if (unlocked) {
                        font.setColor(TEXT_UNLOCKED);
                    } else {
                        font.setColor(TEXT_LOCKED);
                    }
                    font.draw(batch, "Lv " + axe.woodcuttingLevel(), x + 16, rowY + 21);
                    font.draw(batch, axe.name(), x + 116, rowY + 21);
                    font.draw(batch, "Atk " + axe.attackLevelToEquip(), x + w - 88, rowY + 21);
                }
                yCursor -= rowH;
            }
            batch.end();
        }

        private static int findNextTreeLevel(int level) {
            for (WoodcuttingRegistry.TreeTier tree : WoodcuttingRegistry.trees()) {
                if (tree.levelRequirement() > level) {
                    return tree.levelRequirement();
                }
            }
            return -1;
        }

        private static int findNextAxeLevel(int level) {
            for (WoodcuttingRegistry.AxeTier axe : WoodcuttingRegistry.axes()) {
                if (axe.woodcuttingLevel() > level) {
                    return axe.woodcuttingLevel();
                }
            }
            return -1;
        }

        private static void drawTreeIcon(ShapeRenderer sr, float slotLeft, float slotBottom) {
            float x = slotLeft + 4f;
            float y = slotBottom + 3f;
            sr.setColor(0.46f, 0.28f, 0.12f, 1f);
            sr.rect(x + 11, y + 2, 5, 13);
            sr.setColor(0.20f, 0.56f, 0.22f, 1f);
            sr.rect(x + 5, y + 12, 17, 10);
            sr.setColor(0.28f, 0.66f, 0.30f, 1f);
            sr.rect(x + 7, y + 18, 13, 7);
        }
    }

    private static final class FishingGuideProvider implements SkillGuidePopup.SkillGuideProvider {

        private static final List<SkillGuidePopup.GuideSection> SECTIONS = List.of(
            new SkillGuidePopup.GuideSection("Introduction"),
            new SkillGuidePopup.GuideSection("Fish"),
            new SkillGuidePopup.GuideSection("Equipment")
        );

        private static final Color TEXT_MAIN = new Color(0.24f, 0.16f, 0.06f, 1f);
        private static final Color TEXT_LOCKED = new Color(0.45f, 0.36f, 0.24f, 1f);
        private static final Color TEXT_UNLOCKED = new Color(0.22f, 0.14f, 0.06f, 1f);
        private static final Color TEXT_NEXT = new Color(0.48f, 0.28f, 0.04f, 1f);
        private static final Color ROW_UNLOCKED = new Color(0.88f, 0.81f, 0.67f, 1f);
        private static final Color ROW_LOCKED = new Color(0.78f, 0.71f, 0.57f, 1f);
        private static final Color ROW_NEXT = new Color(0.94f, 0.82f, 0.50f, 1f);

        @Override
        public String getTitle(int skillIdx) {
            return "Fishing";
        }

        @Override
        public List<SkillGuidePopup.GuideSection> getSections(int skillIdx) {
            return SECTIONS;
        }

        @Override
        public void renderSectionContent(ShapeRenderer shapeRenderer,
                                         SpriteBatch batch,
                                         BitmapFont font,
                                         Matrix4 projection,
                                         int skillIdx,
                                         int level,
                                         long totalXp,
                                         int sectionIdx,
                                         float contentX,
                                         float contentY,
                                         float contentW,
                                         float contentH,
                                         float scrollOffset) {
            if (sectionIdx == 0) {
                renderIntroduction(shapeRenderer, batch, font, projection, contentX, contentY, contentW, contentH);
            } else if (sectionIdx == 1) {
                renderFish(shapeRenderer, batch, font, projection, level, contentX, contentY, contentW, contentH, scrollOffset);
            } else if (sectionIdx == 2) {
                renderEquipment(shapeRenderer, batch, font, projection, level, contentX, contentY, contentW, contentH, scrollOffset);
            }
        }

        @Override
        public float getSectionContentHeight(int skillIdx, int level, int sectionIdx, float contentW) {
            if (sectionIdx == 1) {
                return 18f + fishProgression().size() * 32f + 12f;
            }
            if (sectionIdx == 2) {
                return 18f + FishingRegistry.tools().size() * 36f + 12f;
            }
            return 236f;
        }

        private void renderIntroduction(ShapeRenderer shapeRenderer,
                                        SpriteBatch batch,
                                        BitmapFont font,
                                        Matrix4 projection,
                                        float x,
                                        float y,
                                        float w,
                                        float h) {
            final float blockH = 82f;
            final float top = y + h - 14f;
            final float blockX = x + 8f;
            final float blockW = w - 16f;
            final float dividerX = x + 10f;
            final float dividerW = w - 20f;
            final float iconX = x + 20f;
            final float textX = x + 62f;
            final float textW = w - 86f;
            final float textTopPad = 16f;
            final String[] texts = {
                "Fishing lets you catch raw fish from shared fishing spots using different methods.",
                "As your Fishing level rises, you unlock better fish and improve your success rate.",
                "Some methods need consumables such as bait or feathers. Most fish can later be cooked for food."
            };

            shapeRenderer.setProjectionMatrix(projection);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            for (int i = 0; i < 3; i++) {
                float by = top - (i + 1) * blockH;
                shapeRenderer.setColor(0.92f, 0.84f, 0.69f, 1f);
                shapeRenderer.rect(blockX, by, blockW, blockH - 8f);
                shapeRenderer.setColor(0.64f, 0.50f, 0.30f, 1f);
                shapeRenderer.rect(dividerX, by + blockH - 16f, dividerW, 2f);
            }
            ItemIconRenderer.drawItemIcon(shapeRenderer, iconX, top - blockH + 12f, 303);
            ItemIconRenderer.drawItemIcon(shapeRenderer, iconX, top - blockH * 2 + 12f, 317);
            ItemIconRenderer.drawItemIcon(shapeRenderer, iconX, top - blockH * 3 + 12f, 307);
            shapeRenderer.end();

            GlyphLayout wrapped = new GlyphLayout();
            batch.setProjectionMatrix(projection);
            batch.begin();
            font.getData().setScale(0.68f);
            font.setColor(TEXT_MAIN);
            for (int i = 0; i < 3; i++) {
                float by = top - (i + 1) * blockH;
                float blockInnerTop = by + blockH - 8f;
                wrapped.setText(font, texts[i], TEXT_MAIN, textW, Align.left, true);
                font.draw(batch, wrapped, textX, blockInnerTop - textTopPad);
            }
            font.getData().setScale(1f);
            font.setColor(Color.WHITE);
            batch.end();
        }

        private void renderFish(ShapeRenderer shapeRenderer,
                                SpriteBatch batch,
                                BitmapFont font,
                                Matrix4 projection,
                                int level,
                                float x,
                                float y,
                                float w,
                                float h,
                                float scrollOffset) {
            List<FishingRegistry.CatchTier> fish = fishProgression();
            float rowH = 32f;
            int nextReq = findNextFishLevel(level, fish);

            shapeRenderer.setProjectionMatrix(projection);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            float yCursor = y + h - 14f + scrollOffset;
            for (FishingRegistry.CatchTier catchTier : fish) {
                float rowY = yCursor - rowH;
                boolean visible = rowY + rowH >= y && rowY <= y + h;
                if (visible) {
                    boolean unlocked = level >= catchTier.levelRequirement();
                    boolean next = !unlocked && catchTier.levelRequirement() == nextReq;
                    shapeRenderer.setColor(next ? ROW_NEXT : unlocked ? ROW_UNLOCKED : ROW_LOCKED);
                    shapeRenderer.rect(x + 8, rowY + 2, w - 16, rowH - 4);
                    ItemIconRenderer.drawItemIcon(shapeRenderer, x + 72, rowY + 1, catchTier.itemId());
                }
                yCursor -= rowH;
            }
            shapeRenderer.end();

            batch.setProjectionMatrix(projection);
            batch.begin();
            font.getData().setScale(0.68f);
            yCursor = y + h - 14f + scrollOffset;
            for (FishingRegistry.CatchTier catchTier : fish) {
                float rowY = yCursor - rowH;
                boolean visible = rowY + rowH >= y && rowY <= y + h;
                if (visible) {
                    boolean unlocked = level >= catchTier.levelRequirement();
                    boolean next = !unlocked && catchTier.levelRequirement() == nextReq;
                    font.setColor(next ? TEXT_NEXT : unlocked ? TEXT_UNLOCKED : TEXT_LOCKED);
                    font.draw(batch, "Lv " + catchTier.levelRequirement(), x + 16, rowY + 21);
                    font.draw(batch, catchTier.name(), x + 116, rowY + 21);
                    font.draw(batch, formatXpTenths(catchTier.xpTenths()) + " xp", x + w - 88, rowY + 21);
                }
                yCursor -= rowH;
            }
            font.getData().setScale(1f);
            font.setColor(Color.WHITE);
            batch.end();
        }

        private void renderEquipment(ShapeRenderer shapeRenderer,
                                     SpriteBatch batch,
                                     BitmapFont font,
                                     Matrix4 projection,
                                     int level,
                                     float x,
                                     float y,
                                     float w,
                                     float h,
                                     float scrollOffset) {
            float rowH = 36f;
            int nextReq = findNextToolLevel(level);

            shapeRenderer.setProjectionMatrix(projection);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            float yCursor = y + h - 14f + scrollOffset;
            for (FishingRegistry.Tool tool : FishingRegistry.tools()) {
                float rowY = yCursor - rowH;
                boolean visible = rowY + rowH >= y && rowY <= y + h;
                if (visible) {
                    boolean unlocked = level >= tool.unlockLevel();
                    boolean next = !unlocked && tool.unlockLevel() == nextReq;
                    shapeRenderer.setColor(next ? ROW_NEXT : unlocked ? ROW_UNLOCKED : ROW_LOCKED);
                    shapeRenderer.rect(x + 8, rowY + 2, w - 16, rowH - 4);
                    ItemIconRenderer.drawItemIcon(shapeRenderer, x + 72, rowY + 3, tool.itemId());
                }
                yCursor -= rowH;
            }
            shapeRenderer.end();

            batch.setProjectionMatrix(projection);
            batch.begin();
            font.getData().setScale(0.66f);
            yCursor = y + h - 14f + scrollOffset;
            for (FishingRegistry.Tool tool : FishingRegistry.tools()) {
                float rowY = yCursor - rowH;
                boolean visible = rowY + rowH >= y && rowY <= y + h;
                if (visible) {
                    boolean unlocked = level >= tool.unlockLevel();
                    boolean next = !unlocked && tool.unlockLevel() == nextReq;
                    font.setColor(next ? TEXT_NEXT : unlocked ? TEXT_UNLOCKED : TEXT_LOCKED);
                    font.draw(batch, "Lv " + tool.unlockLevel(), x + 16, rowY + 24);
                    font.draw(batch, tool.name(), x + 116, rowY + 24);
                    String req = tool.consumableItemId() == 313 ? "Uses bait"
                        : tool.consumableItemId() == 314 ? "Uses feathers"
                        : "No bait";
                    font.draw(batch, req, x + w - 92, rowY + 24);
                }
                yCursor -= rowH;
            }
            font.getData().setScale(1f);
            font.setColor(Color.WHITE);
            batch.end();
        }

        private static List<FishingRegistry.CatchTier> fishProgression() {
            Map<Integer, FishingRegistry.CatchTier> byItem = new HashMap<>();
            for (FishingRegistry.SpotType spot : FishingRegistry.spotTypes()) {
                for (FishingRegistry.SpotAction action : spot.actions()) {
                    for (FishingRegistry.CatchTier catchTier : action.catches()) {
                        byItem.putIfAbsent(catchTier.itemId(), catchTier);
                    }
                }
            }
            List<FishingRegistry.CatchTier> fish = new java.util.ArrayList<>(byItem.values());
            fish.sort((a, b) -> {
                int byLevel = Integer.compare(a.levelRequirement(), b.levelRequirement());
                if (byLevel != 0) {
                    return byLevel;
                }
                return Integer.compare(a.itemId(), b.itemId());
            });
            return fish;
        }

        private static int findNextFishLevel(int level, List<FishingRegistry.CatchTier> fish) {
            for (FishingRegistry.CatchTier catchTier : fish) {
                if (catchTier.levelRequirement() > level) {
                    return catchTier.levelRequirement();
                }
            }
            return -1;
        }

        private static int findNextToolLevel(int level) {
            for (FishingRegistry.Tool tool : FishingRegistry.tools()) {
                if (tool.unlockLevel() > level) {
                    return tool.unlockLevel();
                }
            }
            return -1;
        }
    }
}
