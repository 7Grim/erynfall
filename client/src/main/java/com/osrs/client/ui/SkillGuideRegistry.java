package com.osrs.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Align;
import com.osrs.shared.FishingRegistry;
import com.osrs.shared.MiningRegistry;
import com.osrs.shared.WeaponRegistry;
import com.osrs.shared.WoodcuttingRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SkillGuideRegistry {

    private static final int SKILL_ATTACK      = 0;
    private static final int SKILL_STRENGTH    = 1;
    private static final int SKILL_DEFENCE     = 2;
    private static final int SKILL_HITPOINTS   = 3;
    private static final int SKILL_WOODCUTTING = 7;
    private static final int SKILL_FISHING = 8;
    private static final int SKILL_MINING = 10;
    private static final Map<Integer, SkillGuidePopup.SkillGuideProvider> PROVIDERS = new HashMap<>();

    static {
        register(SKILL_ATTACK, new AttackGuideProvider());
        register(SKILL_STRENGTH, new StrengthGuideProvider());
        register(SKILL_DEFENCE, new DefenceGuideProvider());
        register(SKILL_HITPOINTS, new HitpointsGuideProvider());
        register(SKILL_WOODCUTTING, new WoodcuttingGuideProvider());
        register(SKILL_FISHING, new FishingGuideProvider());
        register(SKILL_MINING, new MiningGuideProvider());
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

    private static final class MiningGuideProvider implements SkillGuidePopup.SkillGuideProvider {

        private static final List<SkillGuidePopup.GuideSection> SECTIONS = List.of(
            new SkillGuidePopup.GuideSection("Introduction"),
            new SkillGuidePopup.GuideSection("Rocks"),
            new SkillGuidePopup.GuideSection("Pickaxes")
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
            return "Mining";
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
                renderRocks(shapeRenderer, batch, font, projection, level, contentX, contentY, contentW, contentH, scrollOffset);
            } else if (sectionIdx == 2) {
                renderPickaxes(shapeRenderer, batch, font, projection, level, contentX, contentY, contentW, contentH, scrollOffset);
            }
        }

        @Override
        public float getSectionContentHeight(int skillIdx, int level, int sectionIdx, float contentW) {
            if (sectionIdx == 1) {
                return 18f + MiningRegistry.rocks().size() * 36f + 12f;
            }
            if (sectionIdx == 2) {
                return 18f + MiningRegistry.pickaxes().size() * 36f + 12f;
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
                "Mine rocks to collect ores used in Smithing. Each ore type requires a minimum Mining level.",
                "As your Mining level rises, your chance of successfully mining each swing improves. Higher-level ores give more XP.",
                "Better pickaxes mine faster. Pickaxes work from inventory even when you lack the Attack level to equip them."
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
            drawRockIcon(shapeRenderer, iconX, top - blockH + 12f);
            ItemIconRenderer.drawItemIcon(shapeRenderer, iconX, top - blockH * 2 + 12f, 436);
            ItemIconRenderer.drawItemIcon(shapeRenderer, iconX, top - blockH * 3 + 12f, 1265);
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

        private void renderRocks(ShapeRenderer shapeRenderer,
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
            int nextReq = findNextRockLevel(level);

            shapeRenderer.setProjectionMatrix(projection);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            float yCursor = y + h - 14f + scrollOffset;
            for (MiningRegistry.RockTier rock : MiningRegistry.rocks()) {
                float rowY = yCursor - rowH;
                boolean visible = rowY + rowH >= y && rowY <= y + h;
                if (visible) {
                    boolean unlocked = level >= rock.levelRequirement();
                    boolean next = !unlocked && rock.levelRequirement() == nextReq;
                    shapeRenderer.setColor(next ? ROW_NEXT : unlocked ? ROW_UNLOCKED : ROW_LOCKED);
                    shapeRenderer.rect(x + 8, rowY + 2, w - 16, rowH - 4);
                    ItemIconRenderer.drawItemIcon(shapeRenderer, x + 72, rowY + 3, rock.oreItemId());
                }
                yCursor -= rowH;
            }
            shapeRenderer.end();

            batch.setProjectionMatrix(projection);
            batch.begin();
            font.getData().setScale(0.68f);
            yCursor = y + h - 14f + scrollOffset;
            for (MiningRegistry.RockTier rock : MiningRegistry.rocks()) {
                float rowY = yCursor - rowH;
                boolean visible = rowY + rowH >= y && rowY <= y + h;
                if (visible) {
                    boolean unlocked = level >= rock.levelRequirement();
                    boolean next = !unlocked && rock.levelRequirement() == nextReq;
                    font.setColor(next ? TEXT_NEXT : unlocked ? TEXT_UNLOCKED : TEXT_LOCKED);
                    font.draw(batch, "Lv " + rock.levelRequirement(), x + 16, rowY + 26);
                    font.draw(batch, rock.name(), x + 116, rowY + 26);
                    font.draw(batch, formatXpTenths(rock.xpTenths()) + " xp", x + w - 88, rowY + 26);
                    // Second line: depletion behaviour hint
                    font.getData().setScale(0.60f);
                    Color sub = next ? TEXT_NEXT : unlocked ? TEXT_LOCKED : TEXT_LOCKED;
                    font.setColor(sub.r, sub.g, sub.b, 0.75f);
                    String depletion = rock.depletionType() == MiningRegistry.DepletionType.SINGLE_ORE
                        ? "Always depletes"
                        : "1/" + rock.depletionChanceDenominator() + " depletion";
                    font.draw(batch, depletion, x + 116, rowY + 13);
                    font.getData().setScale(0.68f);
                }
                yCursor -= rowH;
            }
            font.getData().setScale(1f);
            font.setColor(Color.WHITE);
            batch.end();
        }

        private void renderPickaxes(ShapeRenderer shapeRenderer,
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
            int nextReq = findNextPickaxeLevel(level);

            shapeRenderer.setProjectionMatrix(projection);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            float yCursor = y + h - 14f + scrollOffset;
            for (MiningRegistry.PickaxeTier pick : MiningRegistry.pickaxes()) {
                float rowY = yCursor - rowH;
                boolean visible = rowY + rowH >= y && rowY <= y + h;
                if (visible) {
                    boolean unlocked = level >= pick.miningLevel();
                    boolean next = !unlocked && pick.miningLevel() == nextReq;
                    shapeRenderer.setColor(next ? ROW_NEXT : unlocked ? ROW_UNLOCKED : ROW_LOCKED);
                    shapeRenderer.rect(x + 8, rowY + 2, w - 16, rowH - 4);
                    ItemIconRenderer.drawItemIcon(shapeRenderer, x + 72, rowY + 3, pick.itemId());
                }
                yCursor -= rowH;
            }
            shapeRenderer.end();

            batch.setProjectionMatrix(projection);
            batch.begin();
            font.getData().setScale(0.68f);
            yCursor = y + h - 14f + scrollOffset;
            for (MiningRegistry.PickaxeTier pick : MiningRegistry.pickaxes()) {
                float rowY = yCursor - rowH;
                boolean visible = rowY + rowH >= y && rowY <= y + h;
                if (visible) {
                    boolean unlocked = level >= pick.miningLevel();
                    boolean next = !unlocked && pick.miningLevel() == nextReq;
                    font.setColor(next ? TEXT_NEXT : unlocked ? TEXT_UNLOCKED : TEXT_LOCKED);
                    font.draw(batch, "Lv " + pick.miningLevel(), x + 16, rowY + 26);
                    font.draw(batch, pick.name(), x + 116, rowY + 26);
                    font.draw(batch, "Atk " + pick.attackLevelToEquip(), x + w - 88, rowY + 26);
                    // Second line: wield vs use note for dragon (atk 60 requirement is notable)
                    font.getData().setScale(0.60f);
                    Color sub = next ? TEXT_NEXT : unlocked ? TEXT_LOCKED : TEXT_LOCKED;
                    font.setColor(sub.r, sub.g, sub.b, 0.75f);
                    String note = pick.miningLevel() != pick.attackLevelToEquip()
                        ? "Works from inventory"
                        : "Works from inventory";
                    font.draw(batch, note, x + 116, rowY + 13);
                    font.getData().setScale(0.68f);
                }
                yCursor -= rowH;
            }
            font.getData().setScale(1f);
            font.setColor(Color.WHITE);
            batch.end();
        }

        private static int findNextRockLevel(int level) {
            for (MiningRegistry.RockTier rock : MiningRegistry.rocks()) {
                if (rock.levelRequirement() > level) {
                    return rock.levelRequirement();
                }
            }
            return -1;
        }

        private static int findNextPickaxeLevel(int level) {
            for (MiningRegistry.PickaxeTier pick : MiningRegistry.pickaxes()) {
                if (pick.miningLevel() > level) {
                    return pick.miningLevel();
                }
            }
            return -1;
        }

        private static void drawRockIcon(ShapeRenderer sr, float slotLeft, float slotBottom) {
            float x = slotLeft + 4f;
            float y = slotBottom + 3f;
            // Draw a simple rock shape
            sr.setColor(0.55f, 0.50f, 0.46f, 1f);
            sr.rect(x + 4, y + 2, 18, 16);
            sr.setColor(0.68f, 0.63f, 0.58f, 1f);
            sr.rect(x + 7, y + 12, 10, 6);
            sr.setColor(0.42f, 0.38f, 0.35f, 1f);
            sr.rect(x + 4, y + 2, 4, 4);
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

    // =========================================================================
    // Attack
    // =========================================================================

    private static final class AttackGuideProvider implements SkillGuidePopup.SkillGuideProvider {

        private static final List<SkillGuidePopup.GuideSection> SECTIONS = List.of(
            new SkillGuidePopup.GuideSection("Introduction"),
            new SkillGuidePopup.GuideSection("Weapons"),
            new SkillGuidePopup.GuideSection("Combat Styles")
        );

        private static final Color TEXT_MAIN     = new Color(0.24f, 0.16f, 0.06f, 1f);
        private static final Color TEXT_LOCKED   = new Color(0.45f, 0.36f, 0.24f, 1f);
        private static final Color TEXT_UNLOCKED = new Color(0.22f, 0.14f, 0.06f, 1f);
        private static final Color TEXT_NEXT     = new Color(0.48f, 0.28f, 0.04f, 1f);
        private static final Color ROW_UNLOCKED  = new Color(0.88f, 0.81f, 0.67f, 1f);
        private static final Color ROW_LOCKED    = new Color(0.78f, 0.71f, 0.57f, 1f);
        private static final Color ROW_NEXT      = new Color(0.94f, 0.82f, 0.50f, 1f);

        // Combat style data for the Styles tab
        private static final String[] STYLE_NAMES = {
            "Accurate", "Aggressive", "Defensive", "Controlled"
        };
        private static final String[] STYLE_XP = {
            "4 Attack XP + 1.33 HP XP",
            "4 Strength XP + 1.33 HP XP",
            "4 Defence XP + 1.33 HP XP",
            "1.33 Atk/Str/Def + 1.33 HP XP"
        };
        private static final String[] STYLE_BONUS = {
            "+3 effective Attack level",
            "+3 effective Strength level",
            "+3 effective Defence level",
            "+1 to Attack, Strength and Defence"
        };
        // Representative item icon per style row (scimitar tiers shown as examples)
        private static final int[] STYLE_ICON_IDS = { 1333, 1329, 1325, 1321 };

        @Override
        public String getTitle(int skillIdx) {
            return "Attack";
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
                renderWeapons(shapeRenderer, batch, font, projection, level, contentX, contentY, contentW, contentH, scrollOffset);
            } else if (sectionIdx == 2) {
                renderStyles(shapeRenderer, batch, font, projection, level, contentX, contentY, contentW, contentH, scrollOffset);
            }
        }

        @Override
        public float getSectionContentHeight(int skillIdx, int level, int sectionIdx, float contentW) {
            if (sectionIdx == 1) {
                return 18f + WeaponRegistry.weapons().size() * 36f + 12f;
            }
            if (sectionIdx == 2) {
                return 18f + STYLE_NAMES.length * 60f + 12f;
            }
            return 236f;
        }

        private void renderIntroduction(ShapeRenderer sr, SpriteBatch batch, BitmapFont font,
                                         Matrix4 proj, float x, float y, float w, float h) {
            final float blockH = 82f;
            final float top    = y + h - 14f;
            final float blockX = x + 8f;
            final float blockW = w - 16f;
            final float dividerX = x + 10f;
            final float dividerW = w - 20f;
            final float iconX  = x + 20f;
            final float textX  = x + 62f;
            final float textW  = w - 86f;
            final float textTopPad = 16f;
            final String[] texts = {
                "Attack determines your accuracy in melee combat. Higher Attack means fewer misses against tough enemies.",
                "Each Attack level unlocks stronger weapons. Scimitars are the fastest (4-tick); longswords offer higher strength bonuses.",
                "Combat style affects which skill gains XP. Choose Accurate to train Attack, Aggressive for Strength, or Defensive for Defence."
            };

            sr.setProjectionMatrix(proj);
            sr.begin(ShapeRenderer.ShapeType.Filled);
            for (int i = 0; i < 3; i++) {
                float by = top - (i + 1) * blockH;
                sr.setColor(0.92f, 0.84f, 0.69f, 1f);
                sr.rect(blockX, by, blockW, blockH - 8f);
                sr.setColor(0.64f, 0.50f, 0.30f, 1f);
                sr.rect(dividerX, by + blockH - 16f, dividerW, 2f);
            }
            // Icons: rune scimitar, mithril scimitar, bronze scimitar
            ItemIconRenderer.drawItemIcon(sr, iconX, top - blockH       + 12f, 1333);
            ItemIconRenderer.drawItemIcon(sr, iconX, top - blockH * 2f  + 12f, 1329);
            ItemIconRenderer.drawItemIcon(sr, iconX, top - blockH * 3f  + 12f, 1321);
            sr.end();

            GlyphLayout wrapped = new GlyphLayout();
            batch.setProjectionMatrix(proj);
            batch.begin();
            font.getData().setScale(0.68f);
            font.setColor(TEXT_MAIN);
            for (int i = 0; i < 3; i++) {
                float by = top - (i + 1) * blockH;
                wrapped.setText(font, texts[i], TEXT_MAIN, textW, Align.left, true);
                font.draw(batch, wrapped, textX, by + blockH - 8f - textTopPad);
            }
            font.getData().setScale(1f);
            font.setColor(Color.WHITE);
            batch.end();
        }

        private void renderWeapons(ShapeRenderer sr, SpriteBatch batch, BitmapFont font,
                                    Matrix4 proj, int level,
                                    float x, float y, float w, float h, float scrollOffset) {
            float rowH    = 36f;
            int nextReq = findNextWeaponLevel(level);

            sr.setProjectionMatrix(proj);
            sr.begin(ShapeRenderer.ShapeType.Filled);
            float yCursor = y + h - 14f + scrollOffset;
            for (WeaponRegistry.WeaponTier weapon : WeaponRegistry.weapons()) {
                float rowY   = yCursor - rowH;
                boolean visible = rowY + rowH >= y && rowY <= y + h;
                if (visible) {
                    boolean unlocked = level >= weapon.attackReq();
                    boolean next     = !unlocked && weapon.attackReq() == nextReq;
                    sr.setColor(next ? ROW_NEXT : unlocked ? ROW_UNLOCKED : ROW_LOCKED);
                    sr.rect(x + 8, rowY + 2, w - 16, rowH - 4);
                    ItemIconRenderer.drawItemIcon(sr, x + 72, rowY + 3, weapon.itemId());
                }
                yCursor -= rowH;
            }
            sr.end();

            batch.setProjectionMatrix(proj);
            batch.begin();
            font.getData().setScale(0.68f);
            yCursor = y + h - 14f + scrollOffset;
            for (WeaponRegistry.WeaponTier weapon : WeaponRegistry.weapons()) {
                float rowY   = yCursor - rowH;
                boolean visible = rowY + rowH >= y && rowY <= y + h;
                if (visible) {
                    boolean unlocked = level >= weapon.attackReq();
                    boolean next     = !unlocked && weapon.attackReq() == nextReq;
                    font.setColor(next ? TEXT_NEXT : unlocked ? TEXT_UNLOCKED : TEXT_LOCKED);
                    font.draw(batch, "Lv " + weapon.attackReq(), x + 16, rowY + 26);
                    font.draw(batch, weapon.name(),              x + 116, rowY + 26);
                    font.draw(batch, "+" + weapon.strengthBonus() + " str", x + w - 88, rowY + 26);
                    // Second line: attack speed
                    font.getData().setScale(0.60f);
                    Color sub = next ? TEXT_NEXT : TEXT_LOCKED;
                    font.setColor(sub.r, sub.g, sub.b, 0.75f);
                    font.draw(batch, weapon.attackSpeedOsrsTicks() + "-tick speed", x + 116, rowY + 13);
                    font.getData().setScale(0.68f);
                }
                yCursor -= rowH;
            }
            font.getData().setScale(1f);
            font.setColor(Color.WHITE);
            batch.end();
        }

        private void renderStyles(ShapeRenderer sr, SpriteBatch batch, BitmapFont font,
                                   Matrix4 proj, int level,
                                   float x, float y, float w, float h, float scrollOffset) {
            float rowH = 60f;

            sr.setProjectionMatrix(proj);
            sr.begin(ShapeRenderer.ShapeType.Filled);
            float yCursor = y + h - 14f + scrollOffset;
            for (int i = 0; i < STYLE_NAMES.length; i++) {
                float rowY   = yCursor - rowH;
                boolean visible = rowY + rowH >= y && rowY <= y + h;
                if (visible) {
                    sr.setColor(ROW_UNLOCKED);
                    sr.rect(x + 8, rowY + 2, w - 16, rowH - 4);
                    ItemIconRenderer.drawItemIcon(sr, x + 72, rowY + 14, STYLE_ICON_IDS[i]);
                }
                yCursor -= rowH;
            }
            sr.end();

            batch.setProjectionMatrix(proj);
            batch.begin();
            font.getData().setScale(0.68f);
            yCursor = y + h - 14f + scrollOffset;
            for (int i = 0; i < STYLE_NAMES.length; i++) {
                float rowY   = yCursor - rowH;
                boolean visible = rowY + rowH >= y && rowY <= y + h;
                if (visible) {
                    font.setColor(TEXT_UNLOCKED);
                    font.draw(batch, STYLE_NAMES[i], x + 116, rowY + rowH - 14f);
                    font.getData().setScale(0.60f);
                    font.setColor(TEXT_MAIN.r, TEXT_MAIN.g, TEXT_MAIN.b, 0.88f);
                    font.draw(batch, STYLE_XP[i],    x + 116, rowY + rowH - 28f);
                    font.draw(batch, STYLE_BONUS[i],  x + 116, rowY + rowH - 42f);
                    font.getData().setScale(0.68f);
                }
                yCursor -= rowH;
            }
            font.getData().setScale(1f);
            font.setColor(Color.WHITE);
            batch.end();
        }

        private static int findNextWeaponLevel(int level) {
            for (WeaponRegistry.WeaponTier weapon : WeaponRegistry.weapons()) {
                if (weapon.attackReq() > level) {
                    return weapon.attackReq();
                }
            }
            return -1;
        }
    }

    // =========================================================================
    // Strength
    // =========================================================================

    private static final class StrengthGuideProvider implements SkillGuidePopup.SkillGuideProvider {

        private static final List<SkillGuidePopup.GuideSection> SECTIONS = List.of(
            new SkillGuidePopup.GuideSection("Introduction"),
            new SkillGuidePopup.GuideSection("Weapons"),
            new SkillGuidePopup.GuideSection("Max Hit")
        );

        private static final Color TEXT_MAIN     = new Color(0.24f, 0.16f, 0.06f, 1f);
        private static final Color TEXT_LOCKED   = new Color(0.45f, 0.36f, 0.24f, 1f);
        private static final Color TEXT_UNLOCKED = new Color(0.22f, 0.14f, 0.06f, 1f);
        private static final Color TEXT_NEXT     = new Color(0.48f, 0.28f, 0.04f, 1f);
        private static final Color ROW_UNLOCKED  = new Color(0.88f, 0.81f, 0.67f, 1f);
        private static final Color ROW_LOCKED    = new Color(0.78f, 0.71f, 0.57f, 1f);
        private static final Color ROW_CURRENT   = new Color(0.94f, 0.82f, 0.50f, 1f);

        // Key level milestones shown in the Max Hit reference table.
        // Chosen to cover early, mid and late-game progression without needing scroll.
        private static final int[] MAX_HIT_LEVELS = { 1, 10, 20, 30, 40, 50, 60, 70, 80, 90, 99 };

        // Strength bonus values used as reference columns in the Max Hit table.
        // +0 = no weapon (bare fists); +67 = rune scimitar (best non-dragon str bonus).
        private static final int REF_STR_BONUS_NONE  = 0;
        private static final int REF_STR_BONUS_RUNE  = 67; // rune scimitar

        @Override
        public String getTitle(int skillIdx) {
            return "Strength";
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
                renderIntroduction(shapeRenderer, batch, font, projection, level, contentX, contentY, contentW, contentH);
            } else if (sectionIdx == 1) {
                renderWeapons(shapeRenderer, batch, font, projection, level, contentX, contentY, contentW, contentH, scrollOffset);
            } else if (sectionIdx == 2) {
                renderMaxHit(shapeRenderer, batch, font, projection, level, contentX, contentY, contentW, contentH, scrollOffset);
            }
        }

        @Override
        public float getSectionContentHeight(int skillIdx, int level, int sectionIdx, float contentW) {
            if (sectionIdx == 1) {
                return 18f + WeaponRegistry.weapons().size() * 36f + 12f;
            }
            if (sectionIdx == 2) {
                return 18f + MAX_HIT_LEVELS.length * 28f + 12f;
            }
            return 236f;
        }

        private void renderIntroduction(ShapeRenderer sr, SpriteBatch batch, BitmapFont font,
                                         Matrix4 proj, int level, float x, float y, float w, float h) {
            final float blockH    = 82f;
            final float top       = y + h - 14f;
            final float blockX    = x + 8f;
            final float blockW    = w - 16f;
            final float dividerX  = x + 10f;
            final float dividerW  = w - 20f;
            final float iconX     = x + 20f;
            final float textX     = x + 62f;
            final float textW     = w - 86f;
            final float textTopPad = 16f;

            // Compute the player's current max hit as a live stat to show in the intro text.
            int maxNone = WeaponRegistry.maxHit(level, REF_STR_BONUS_NONE);
            int maxRune = WeaponRegistry.maxHit(level, REF_STR_BONUS_RUNE);

            final String[] texts = {
                "Strength increases your melee maximum hit. Every point of Strength raises your damage output through the max hit formula: floor(0.5 + (level+8) x (strBonus+64) / 640).",
                "Train Strength by attacking with the Aggressive style (4 Str XP + 1.33 HP XP per damage), or use Controlled to split XP across Attack, Strength and Defence.",
                "At your current level " + level + ": max hit bare-fisted = " + maxNone +
                    ", max hit with rune scimitar = " + maxRune + "."
            };

            sr.setProjectionMatrix(proj);
            sr.begin(ShapeRenderer.ShapeType.Filled);
            for (int i = 0; i < 3; i++) {
                float by = top - (i + 1) * blockH;
                sr.setColor(0.92f, 0.84f, 0.69f, 1f);
                sr.rect(blockX, by, blockW, blockH - 8f);
                sr.setColor(0.64f, 0.50f, 0.30f, 1f);
                sr.rect(dividerX, by + blockH - 16f, dividerW, 2f);
            }
            // Icons: rune scimitar, adamant scimitar, mithril scimitar (str progression)
            ItemIconRenderer.drawItemIcon(sr, iconX, top - blockH       + 12f, 1333);
            ItemIconRenderer.drawItemIcon(sr, iconX, top - blockH * 2f  + 12f, 1331);
            ItemIconRenderer.drawItemIcon(sr, iconX, top - blockH * 3f  + 12f, 1329);
            sr.end();

            GlyphLayout wrapped = new GlyphLayout();
            batch.setProjectionMatrix(proj);
            batch.begin();
            font.getData().setScale(0.68f);
            font.setColor(TEXT_MAIN);
            for (int i = 0; i < 3; i++) {
                float by = top - (i + 1) * blockH;
                wrapped.setText(font, texts[i], TEXT_MAIN, textW, Align.left, true);
                font.draw(batch, wrapped, textX, by + blockH - 8f - textTopPad);
            }
            font.getData().setScale(1f);
            font.setColor(Color.WHITE);
            batch.end();
        }

        /**
         * Weapons tab: shows all weapons sorted by strength bonus (descending).
         * This is distinct from the Attack guide which sorts by attack requirement.
         * Notable: rune scimitar (+67 str) has a higher strength bonus than dragon
         * scimitar (+66 str), which surprises most players.
         */
        private void renderWeapons(ShapeRenderer sr, SpriteBatch batch, BitmapFont font,
                                    Matrix4 proj, int level,
                                    float x, float y, float w, float h, float scrollOffset) {
            float rowH = 36f;

            sr.setProjectionMatrix(proj);
            sr.begin(ShapeRenderer.ShapeType.Filled);
            float yCursor = y + h - 14f + scrollOffset;
            for (WeaponRegistry.WeaponTier weapon : WeaponRegistry.weapons()) {
                float rowY = yCursor - rowH;
                boolean visible = rowY + rowH >= y && rowY <= y + h;
                if (visible) {
                    boolean unlocked = level >= 1; // Strength doesn't gate weapons; Attack does
                    sr.setColor(ROW_UNLOCKED);
                    sr.rect(x + 8, rowY + 2, w - 16, rowH - 4);
                    ItemIconRenderer.drawItemIcon(sr, x + 72, rowY + 3, weapon.itemId());
                }
                yCursor -= rowH;
            }
            sr.end();

            batch.setProjectionMatrix(proj);
            batch.begin();
            font.getData().setScale(0.68f);
            yCursor = y + h - 14f + scrollOffset;
            for (WeaponRegistry.WeaponTier weapon : WeaponRegistry.weapons()) {
                float rowY = yCursor - rowH;
                boolean visible = rowY + rowH >= y && rowY <= y + h;
                if (visible) {
                    font.setColor(TEXT_UNLOCKED);
                    font.draw(batch, "+" + weapon.strengthBonus() + " str", x + 16,  rowY + 26);
                    font.draw(batch, weapon.name(),                          x + 116, rowY + 26);
                    font.draw(batch, "Atk " + weapon.attackReq(),            x + w - 88, rowY + 26);
                    // Second line: attack speed in OSRS ticks
                    font.getData().setScale(0.60f);
                    font.setColor(TEXT_LOCKED.r, TEXT_LOCKED.g, TEXT_LOCKED.b, 0.75f);
                    font.draw(batch, weapon.attackSpeedOsrsTicks() + "-tick speed", x + 116, rowY + 13);
                    font.getData().setScale(0.68f);
                }
                yCursor -= rowH;
            }
            font.getData().setScale(1f);
            font.setColor(Color.WHITE);
            batch.end();
        }

        /**
         * Max Hit reference table: shows max hit at key level milestones with two
         * reference weapons (bare fists +0 str, rune scimitar +67 str).
         * The row matching the player's current level is highlighted.
         */
        private void renderMaxHit(ShapeRenderer sr, SpriteBatch batch, BitmapFont font,
                                   Matrix4 proj, int level,
                                   float x, float y, float w, float h, float scrollOffset) {
            float rowH = 28f;

            sr.setProjectionMatrix(proj);
            sr.begin(ShapeRenderer.ShapeType.Filled);
            float yCursor = y + h - 14f + scrollOffset;
            for (int refLv : MAX_HIT_LEVELS) {
                float rowY = yCursor - rowH;
                boolean visible = rowY + rowH >= y && rowY <= y + h;
                if (visible) {
                    // Highlight the row whose milestone the player has just reached.
                    boolean isCurrent = isCurrentMilestone(level, refLv);
                    sr.setColor(isCurrent ? ROW_CURRENT : ROW_UNLOCKED);
                    sr.rect(x + 8, rowY + 2, w - 16, rowH - 4);
                    // Rune scimitar icon in the centre column
                    ItemIconRenderer.drawItemIcon(sr, x + 72, rowY - 4, 1333);
                }
                yCursor -= rowH;
            }
            sr.end();

            batch.setProjectionMatrix(proj);
            batch.begin();
            font.getData().setScale(0.68f);
            yCursor = y + h - 14f + scrollOffset;
            for (int refLv : MAX_HIT_LEVELS) {
                float rowY = yCursor - rowH;
                boolean visible = rowY + rowH >= y && rowY <= y + h;
                if (visible) {
                    boolean isCurrent = isCurrentMilestone(level, refLv);
                    font.setColor(isCurrent ? TEXT_NEXT : TEXT_UNLOCKED);
                    int hitBare = WeaponRegistry.maxHit(refLv, REF_STR_BONUS_NONE);
                    int hitRune = WeaponRegistry.maxHit(refLv, REF_STR_BONUS_RUNE);
                    font.draw(batch, "Lv " + refLv,       x + 16,      rowY + 18);
                    font.draw(batch, "Max: " + hitRune,   x + 116,     rowY + 18);
                    font.getData().setScale(0.60f);
                    font.setColor(TEXT_LOCKED.r, TEXT_LOCKED.g, TEXT_LOCKED.b, 0.75f);
                    font.draw(batch, "Bare: " + hitBare,  x + w - 88,  rowY + 18);
                    font.getData().setScale(0.68f);
                }
                yCursor -= rowH;
            }
            font.getData().setScale(1f);
            font.setColor(Color.WHITE);
            batch.end();
        }

        /**
         * Returns true if {@code refLv} is the highest milestone in MAX_HIT_LEVELS
         * that the player has reached (i.e. their "current bracket").
         */
        private static boolean isCurrentMilestone(int playerLevel, int refLv) {
            // Find the highest milestone at or below player level
            int bracket = MAX_HIT_LEVELS[0];
            for (int lv : MAX_HIT_LEVELS) {
                if (lv <= playerLevel) {
                    bracket = lv;
                }
            }
            return refLv == bracket;
        }
    }

    // =========================================================================
    // Defence
    // =========================================================================

    private static final class DefenceGuideProvider implements SkillGuidePopup.SkillGuideProvider {

        private static final List<SkillGuidePopup.GuideSection> SECTIONS = List.of(
            new SkillGuidePopup.GuideSection("Introduction"),
            new SkillGuidePopup.GuideSection("Armour"),
            new SkillGuidePopup.GuideSection("Shields")
        );

        private static final Color TEXT_MAIN     = new Color(0.24f, 0.16f, 0.06f, 1f);
        private static final Color TEXT_LOCKED   = new Color(0.45f, 0.36f, 0.24f, 1f);
        private static final Color TEXT_UNLOCKED = new Color(0.22f, 0.14f, 0.06f, 1f);
        private static final Color TEXT_NEXT     = new Color(0.48f, 0.28f, 0.04f, 1f);
        private static final Color ROW_UNLOCKED  = new Color(0.88f, 0.81f, 0.67f, 1f);
        private static final Color ROW_LOCKED    = new Color(0.78f, 0.71f, 0.57f, 1f);
        private static final Color ROW_NEXT      = new Color(0.94f, 0.82f, 0.50f, 1f);

        /**
         * One entry per armour tier shown in the guide.
         * slashDef is the platebody's slash defence bonus — used as the
         * headline stat since slash is the most common attack type in OSRS.
         */
        private record ArmourTier(int defReq, String name, int itemId, int slashDef) {}

        private static final List<ArmourTier> BODY_TIERS = List.of(
            new ArmourTier( 1, "Bronze platebody",  1119,  17),
            new ArmourTier( 1, "Iron platebody",    2000,  28),
            new ArmourTier( 5, "Steel platebody",   1085,  42),
            new ArmourTier(10, "Black platebody",   1125,  49),
            new ArmourTier(20, "Mithril platebody", 1129,  61),
            new ArmourTier(30, "Adamant platebody", 1133,  79),
            new ArmourTier(40, "Rune platebody",    1127,  97),
            new ArmourTier(60, "Dragon chainbody",  3140,  80)
        );

        private static final List<ArmourTier> SHIELD_TIERS = List.of(
            new ArmourTier( 1, "Wooden shield",      1173,   3),
            new ArmourTier( 1, "Bronze sq shield",   1175,   8),
            new ArmourTier( 1, "Iron sq shield",     1177,  15),
            new ArmourTier( 5, "Steel sq shield",    1193,  24),
            new ArmourTier(10, "Black sq shield",    1195,  29),
            new ArmourTier(20, "Mithril sq shield",  1197,  35),
            new ArmourTier(30, "Adamant sq shield",  1199,  43),
            new ArmourTier(40, "Rune sq shield",     1185,  54),
            new ArmourTier(60, "Dragon sq shield",  11286,  65)
        );

        @Override
        public String getTitle(int skillIdx) {
            return "Defence";
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
                renderArmour(shapeRenderer, batch, font, projection, level, contentX, contentY, contentW, contentH, scrollOffset, BODY_TIERS);
            } else if (sectionIdx == 2) {
                renderArmour(shapeRenderer, batch, font, projection, level, contentX, contentY, contentW, contentH, scrollOffset, SHIELD_TIERS);
            }
        }

        @Override
        public float getSectionContentHeight(int skillIdx, int level, int sectionIdx, float contentW) {
            if (sectionIdx == 1) {
                return 18f + BODY_TIERS.size()   * 36f + 12f;
            }
            if (sectionIdx == 2) {
                return 18f + SHIELD_TIERS.size() * 36f + 12f;
            }
            return 236f;
        }

        private void renderIntroduction(ShapeRenderer sr, SpriteBatch batch, BitmapFont font,
                                         Matrix4 proj, float x, float y, float w, float h) {
            final float blockH    = 82f;
            final float top       = y + h - 14f;
            final float blockX    = x + 8f;
            final float blockW    = w - 16f;
            final float dividerX  = x + 10f;
            final float dividerW  = w - 20f;
            final float iconX     = x + 20f;
            final float textX     = x + 62f;
            final float textW     = w - 86f;
            final float textTopPad = 16f;
            final String[] texts = {
                "Defence reduces the chance of being hit. Your defence roll is: (level + 8) x (armour bonus + 64). The higher your roll, the more hits you block.",
                "Train Defence by fighting with the Defensive style (4 Defence XP + 1.33 HP XP), or use Controlled to split XP across Attack, Strength and Defence.",
                "Each armour tier requires a minimum Defence level. Rune armour (level 40) is the best standard set. Dragon chainbody and sq shield unlock at level 60."
            };

            sr.setProjectionMatrix(proj);
            sr.begin(ShapeRenderer.ShapeType.Filled);
            for (int i = 0; i < 3; i++) {
                float by = top - (i + 1) * blockH;
                sr.setColor(0.92f, 0.84f, 0.69f, 1f);
                sr.rect(blockX, by, blockW, blockH - 8f);
                sr.setColor(0.64f, 0.50f, 0.30f, 1f);
                sr.rect(dividerX, by + blockH - 16f, dividerW, 2f);
            }
            // Icons: rune platebody, rune sq shield, adamant platebody
            ItemIconRenderer.drawItemIcon(sr, iconX, top - blockH       + 12f, 1127);
            ItemIconRenderer.drawItemIcon(sr, iconX, top - blockH * 2f  + 12f, 1185);
            ItemIconRenderer.drawItemIcon(sr, iconX, top - blockH * 3f  + 12f, 1133);
            sr.end();

            GlyphLayout wrapped = new GlyphLayout();
            batch.setProjectionMatrix(proj);
            batch.begin();
            font.getData().setScale(0.68f);
            font.setColor(TEXT_MAIN);
            for (int i = 0; i < 3; i++) {
                float by = top - (i + 1) * blockH;
                wrapped.setText(font, texts[i], TEXT_MAIN, textW, Align.left, true);
                font.draw(batch, wrapped, textX, by + blockH - 8f - textTopPad);
            }
            font.getData().setScale(1f);
            font.setColor(Color.WHITE);
            batch.end();
        }

        /**
         * Shared renderer for both the Armour and Shields tabs.
         * Each row shows: defence req | item icon | item name | slash defence bonus.
         * Second line shows the tier (Bronze / Iron / Steel …) in smaller text.
         * Rows are coloured by whether the player's Defence level meets the requirement.
         */
        private void renderArmour(ShapeRenderer sr, SpriteBatch batch, BitmapFont font,
                                    Matrix4 proj, int level,
                                    float x, float y, float w, float h, float scrollOffset,
                                    List<ArmourTier> tiers) {
            float rowH  = 36f;
            int nextReq = findNextTierLevel(level, tiers);

            sr.setProjectionMatrix(proj);
            sr.begin(ShapeRenderer.ShapeType.Filled);
            float yCursor = y + h - 14f + scrollOffset;
            for (ArmourTier tier : tiers) {
                float rowY   = yCursor - rowH;
                boolean visible = rowY + rowH >= y && rowY <= y + h;
                if (visible) {
                    boolean unlocked = level >= tier.defReq();
                    boolean next     = !unlocked && tier.defReq() == nextReq;
                    sr.setColor(next ? ROW_NEXT : unlocked ? ROW_UNLOCKED : ROW_LOCKED);
                    sr.rect(x + 8, rowY + 2, w - 16, rowH - 4);
                    ItemIconRenderer.drawItemIcon(sr, x + 72, rowY + 3, tier.itemId());
                }
                yCursor -= rowH;
            }
            sr.end();

            batch.setProjectionMatrix(proj);
            batch.begin();
            font.getData().setScale(0.68f);
            yCursor = y + h - 14f + scrollOffset;
            for (ArmourTier tier : tiers) {
                float rowY   = yCursor - rowH;
                boolean visible = rowY + rowH >= y && rowY <= y + h;
                if (visible) {
                    boolean unlocked = level >= tier.defReq();
                    boolean next     = !unlocked && tier.defReq() == nextReq;
                    font.setColor(next ? TEXT_NEXT : unlocked ? TEXT_UNLOCKED : TEXT_LOCKED);
                    font.draw(batch, "Lv " + tier.defReq(), x + 16,      rowY + 26);
                    font.draw(batch, tier.name(),            x + 116,     rowY + 26);
                    font.draw(batch, "+" + tier.slashDef() + " slash",
                                                             x + w - 88,  rowY + 26);
                    // Second line: note about dragon chainbody quirk (lower slash than rune)
                    font.getData().setScale(0.60f);
                    Color sub = next ? TEXT_NEXT : TEXT_LOCKED;
                    font.setColor(sub.r, sub.g, sub.b, 0.75f);
                    String note = tier.itemId() == 3140
                        ? "Lower than rune — prioritise platebody"
                        : "Slash defence bonus";
                    font.draw(batch, note, x + 116, rowY + 13);
                    font.getData().setScale(0.68f);
                }
                yCursor -= rowH;
            }
            font.getData().setScale(1f);
            font.setColor(Color.WHITE);
            batch.end();
        }

        private static int findNextTierLevel(int level, List<ArmourTier> tiers) {
            for (ArmourTier tier : tiers) {
                if (tier.defReq() > level) {
                    return tier.defReq();
                }
            }
            return -1;
        }
    }

    // -----------------------------------------------------------------------
    // Hitpoints guide
    // -----------------------------------------------------------------------

    private static final class HitpointsGuideProvider implements SkillGuidePopup.SkillGuideProvider {

        private static final List<SkillGuidePopup.GuideSection> SECTIONS = List.of(
            new SkillGuidePopup.GuideSection("Introduction"),
            new SkillGuidePopup.GuideSection("Food"),
            new SkillGuidePopup.GuideSection("HP Scaling")
        );

        private static final Color TEXT_MAIN     = new Color(0.24f, 0.16f, 0.06f, 1f);
        private static final Color TEXT_LOCKED   = new Color(0.45f, 0.36f, 0.24f, 1f);
        private static final Color TEXT_UNLOCKED = new Color(0.22f, 0.14f, 0.06f, 1f);
        private static final Color TEXT_NEXT     = new Color(0.48f, 0.28f, 0.04f, 1f);
        private static final Color ROW_UNLOCKED  = new Color(0.88f, 0.81f, 0.67f, 1f);
        private static final Color ROW_LOCKED    = new Color(0.78f, 0.71f, 0.57f, 1f);
        private static final Color ROW_NEXT      = new Color(0.94f, 0.82f, 0.50f, 1f);
        private static final Color HP_RED        = new Color(0.82f, 0.08f, 0.08f, 1f);

        /** Food item: itemId, display name, heal amount. */
        private record FoodTier(int itemId, String name, int heal) {}

        private static final List<FoodTier> FOODS = List.of(
            new FoodTier(315, "Shrimps",  3),
            new FoodTier(361, "Tuna",    10),
            new FoodTier(385, "Shark",   20)
        );

        /** Key levels shown in the HP Scaling reference table. */
        private static final int[] SCALE_LEVELS = {10, 20, 30, 40, 50, 60, 70, 80, 90, 99};

        @Override
        public String getTitle(int skillIdx) {
            return "Hitpoints";
        }

        @Override
        public List<SkillGuidePopup.GuideSection> getSections(int skillIdx) {
            return SECTIONS;
        }

        @Override
        public void renderSectionContent(ShapeRenderer sr, SpriteBatch batch, BitmapFont font,
                                         Matrix4 proj, int skillIdx, int level, long totalXp,
                                         int sectionIdx, float x, float y, float w, float h,
                                         float scrollOffset) {
            if (sectionIdx == 0) {
                renderIntroduction(sr, batch, font, proj, level, x, y, w, h);
            } else if (sectionIdx == 1) {
                renderFood(sr, batch, font, proj, level, x, y, w, h, scrollOffset);
            } else {
                renderHpScaling(sr, batch, font, proj, level, x, y, w, h, scrollOffset);
            }
        }

        @Override
        public float getSectionContentHeight(int skillIdx, int level, int sectionIdx, float contentW) {
            if (sectionIdx == 1) return 18f + FOODS.size() * 32f + 12f;
            if (sectionIdx == 2) return 18f + SCALE_LEVELS.length * 32f + 12f;
            return 236f;
        }

        private void renderIntroduction(ShapeRenderer sr, SpriteBatch batch, BitmapFont font,
                                        Matrix4 proj, int level, float x, float y, float w, float h) {
            final float blockH     = 82f;
            final float top        = y + h - 14f;
            final float blockX     = x + 8f;
            final float blockW     = w - 16f;
            final float dividerX   = x + 10f;
            final float dividerW   = w - 20f;
            final float iconX      = x + 20f;
            final float textX      = x + 62f;
            final float textW      = w - 86f;
            final float textTopPad = 16f;

            final String[] texts = {
                "Hitpoints starts at level 10 — the only skill that begins above level 1. "
                    + "Your maximum HP equals your Hitpoints level directly.",
                "Hitpoints XP is earned passively during all combat — every 1 damage dealt "
                    + "rewards 1.33 HP XP, regardless of your combat style.",
                "Current max HP: " + level + " at level " + level
                    + ". HP regenerates 1 point every 100 seconds. Eating food restores HP instantly."
            };

            sr.setProjectionMatrix(proj);
            sr.begin(ShapeRenderer.ShapeType.Filled);
            for (int i = 0; i < 3; i++) {
                float by = top - (i + 1) * blockH;
                sr.setColor(0.92f, 0.84f, 0.69f, 1f);
                sr.rect(blockX, by, blockW, blockH - 8f);
                sr.setColor(0.64f, 0.50f, 0.30f, 1f);
                sr.rect(dividerX, by + blockH - 16f, dividerW, 2f);
            }
            // Block 1: red HP orb icon
            float ib1y = top - blockH + 12f;
            sr.setColor(HP_RED);
            sr.circle(iconX + 12f, ib1y + 14f, 10f, 14);
            sr.setColor(1f, 0.5f, 0.5f, 0.6f);
            sr.circle(iconX + 10f, ib1y + 16f, 4f, 10);
            // Block 2: rune scimitar (combat XP hint)
            ItemIconRenderer.drawItemIcon(sr, iconX, top - blockH * 2 + 12f, 1333);
            // Block 3: shrimps (food hint)
            ItemIconRenderer.drawItemIcon(sr, iconX, top - blockH * 3 + 12f, 315);
            sr.end();

            GlyphLayout wrapped = new GlyphLayout();
            batch.setProjectionMatrix(proj);
            batch.begin();
            font.getData().setScale(0.68f);
            font.setColor(TEXT_MAIN);
            for (int i = 0; i < 3; i++) {
                float by = top - (i + 1) * blockH;
                wrapped.setText(font, texts[i], TEXT_MAIN, textW, Align.left, true);
                font.draw(batch, wrapped, textX, by + blockH - 8f - textTopPad);
            }
            font.getData().setScale(1f);
            font.setColor(Color.WHITE);
            batch.end();
        }

        private void renderFood(ShapeRenderer sr, SpriteBatch batch, BitmapFont font,
                                Matrix4 proj, int level, float x, float y, float w, float h,
                                float scrollOffset) {
            float rowH = 32f;

            sr.setProjectionMatrix(proj);
            sr.begin(ShapeRenderer.ShapeType.Filled);
            float yCursor = y + h - 14f + scrollOffset;
            for (FoodTier food : FOODS) {
                float rowY = yCursor - rowH;
                if (rowY + rowH >= y && rowY <= y + h) {
                    sr.setColor(ROW_UNLOCKED);
                    sr.rect(x + 8, rowY + 2, w - 16, rowH - 4);
                    ItemIconRenderer.drawItemIcon(sr, x + 72, rowY + 3, food.itemId());
                }
                yCursor -= rowH;
            }
            sr.end();

            batch.setProjectionMatrix(proj);
            batch.begin();
            font.getData().setScale(0.68f);
            yCursor = y + h - 14f + scrollOffset;
            for (FoodTier food : FOODS) {
                float rowY = yCursor - rowH;
                if (rowY + rowH >= y && rowY <= y + h) {
                    font.setColor(TEXT_UNLOCKED);
                    font.draw(batch, food.name(),                x + 16,    rowY + 26);
                    font.draw(batch, "+" + food.heal() + " HP",  x + w - 88, rowY + 26);
                    font.getData().setScale(0.60f);
                    font.setColor(TEXT_LOCKED.r, TEXT_LOCKED.g, TEXT_LOCKED.b, 0.75f);
                    font.draw(batch, "Heals " + food.heal() + " hitpoints", x + 116, rowY + 13);
                    font.getData().setScale(0.68f);
                }
                yCursor -= rowH;
            }
            font.getData().setScale(1f);
            font.setColor(Color.WHITE);
            batch.end();
        }

        private void renderHpScaling(ShapeRenderer sr, SpriteBatch batch, BitmapFont font,
                                     Matrix4 proj, int level, float x, float y, float w, float h,
                                     float scrollOffset) {
            float rowH = 32f;
            int nextLevel = -1;
            for (int lv : SCALE_LEVELS) {
                if (lv > level) { nextLevel = lv; break; }
            }

            sr.setProjectionMatrix(proj);
            sr.begin(ShapeRenderer.ShapeType.Filled);
            float yCursor = y + h - 14f + scrollOffset;
            for (int lv : SCALE_LEVELS) {
                float rowY = yCursor - rowH;
                if (rowY + rowH >= y && rowY <= y + h) {
                    boolean unlocked = level >= lv;
                    boolean next     = !unlocked && lv == nextLevel;
                    sr.setColor(next ? ROW_NEXT : unlocked ? ROW_UNLOCKED : ROW_LOCKED);
                    sr.rect(x + 8, rowY + 2, w - 16, rowH - 4);
                    // Mini HP bar proportional to lv/99
                    float barW = 28f;
                    float barH = 6f;
                    float barX = x + 62f;
                    float barY = rowY + (rowH - barH) / 2f;
                    sr.setColor(0.30f, 0.04f, 0.04f, 1f);
                    sr.rect(barX, barY, barW, barH);
                    sr.setColor(HP_RED);
                    sr.rect(barX, barY, barW * lv / 99f, barH);
                }
                yCursor -= rowH;
            }
            sr.end();

            batch.setProjectionMatrix(proj);
            batch.begin();
            font.getData().setScale(0.68f);
            yCursor = y + h - 14f + scrollOffset;
            for (int lv : SCALE_LEVELS) {
                float rowY = yCursor - rowH;
                if (rowY + rowH >= y && rowY <= y + h) {
                    boolean unlocked = level >= lv;
                    boolean next     = !unlocked && lv == nextLevel;
                    boolean current  = level == lv || (lv == 10 && level <= 10);
                    font.setColor(next ? TEXT_NEXT : unlocked ? TEXT_UNLOCKED : TEXT_LOCKED);
                    font.draw(batch, "Lv " + lv,       x + 16,     rowY + 26);
                    font.draw(batch, lv + " max HP",    x + w - 88, rowY + 26);
                    font.getData().setScale(0.60f);
                    Color sub = next ? TEXT_NEXT : TEXT_LOCKED;
                    font.setColor(sub.r, sub.g, sub.b, 0.75f);
                    font.draw(batch, current ? "< your level" : "Max HP = level", x + 116, rowY + 13);
                    font.getData().setScale(0.68f);
                }
                yCursor -= rowH;
            }
            font.getData().setScale(1f);
            font.setColor(Color.WHITE);
            batch.end();
        }
    }
}
