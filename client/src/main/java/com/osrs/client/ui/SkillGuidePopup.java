package com.osrs.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.Gdx;

import java.util.List;

public class SkillGuidePopup {

    private static final int PANEL_W = 520;
    private static final int PANEL_H = 360;
    private static final int HEADER_H = 34;
    private static final int NAV_W = 146;
    private static final int CONTENT_PAD = 12;
    private static final int CLOSE_SIZE = 20;

    private static final Color OVERLAY = new Color(0.05f, 0.04f, 0.03f, 0.72f);
    private static final Color PANEL_BG = new Color(0.79f, 0.70f, 0.52f, 0.98f);
    private static final Color PANEL_BORDER = new Color(0.42f, 0.30f, 0.15f, 1f);
    private static final Color HEADER_BG = new Color(0.62f, 0.48f, 0.28f, 1f);
    private static final Color NAV_BG = new Color(0.66f, 0.54f, 0.35f, 1f);
    private static final Color CONTENT_BG = new Color(0.85f, 0.77f, 0.60f, 1f);
    private static final Color NAV_SELECTED = new Color(0.86f, 0.72f, 0.35f, 1f);
    private static final Color CLOSE_BG = new Color(0.52f, 0.22f, 0.16f, 1f);
    private static final Color CLOSE_X = new Color(0.92f, 0.18f, 0.12f, 1f);

    private static final String[] SKILL_NAMES = {
        "Attack", "Strength", "Defence", "Hitpoints", "Ranged", "Magic",
        "Prayer", "Woodcutting", "Fishing", "Cooking", "Mining", "Smithing", "Firemaking",
        "Crafting", "Runecrafting", "Fletching", "Agility", "Herblore", "Thieving",
        "Slayer", "Farming", "Hunter", "Construction"
    };

    public record GuideSection(String title) {}

    public record GuideEntry(String title, String subtitle, String meta) {}

    public interface SkillGuideProvider {
        String getTitle(int skillIdx);
        List<GuideSection> getSections(int skillIdx);
        void renderSectionContent(ShapeRenderer shapeRenderer,
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
                                  float scrollOffset);
        default float getSectionContentHeight(int skillIdx, int level, int sectionIdx, float contentW) {
            return 0f;
        }
    }

    private boolean visible;
    private int skillIdx = -1;
    private int level;
    private long totalXp;
    private int selectedSectionIdx;
    private float scrollOffset;
    private SkillGuideProvider provider;
    private List<GuideSection> sections = List.of(new GuideSection("Overview"));

    private int panelX;
    private int panelY;
    private int contentX;
    private int contentY;
    private int contentW;
    private int contentH;
    private int closeX;
    private int closeY;
    private int navX;
    private int navY;
    private int navW;
    private int navH;
    private float lastContentHeight;

    public void show(int skillIdx, int level, long totalXp) {
        this.skillIdx = skillIdx;
        this.level = level;
        this.totalXp = totalXp;
        this.selectedSectionIdx = 0;
        this.scrollOffset = 0f;
        this.provider = SkillGuideRegistry.get(skillIdx);
        if (provider != null) {
            List<GuideSection> providerSections = provider.getSections(skillIdx);
            this.sections = providerSections == null || providerSections.isEmpty()
                ? List.of(new GuideSection("Overview"))
                : providerSections;
        } else {
            this.sections = List.of(new GuideSection("Overview"));
        }
        this.visible = true;
    }

    public void dismiss() {
        visible = false;
    }

    public boolean isVisible() {
        return visible;
    }

    public int getSkillIdx() {
        return skillIdx;
    }

    public int getSelectedSectionIdx() {
        return selectedSectionIdx;
    }

    public void handleScroll(float amountY) {
        if (!visible) {
            return;
        }
        scrollOffset += amountY * 24f;
        clampScroll();
    }

    public boolean handleClick(int screenX, int screenY) {
        if (!visible) {
            return false;
        }
        boolean insidePanel = screenX >= panelX && screenX <= panelX + PANEL_W
            && screenY >= panelY && screenY <= panelY + PANEL_H;
        if (!insidePanel) {
            dismiss();
            return true;
        }

        if (screenX >= closeX && screenX <= closeX + CLOSE_SIZE
            && screenY >= closeY && screenY <= closeY + CLOSE_SIZE) {
            dismiss();
            return true;
        }

        if (screenX >= navX && screenX <= navX + navW && screenY >= navY && screenY <= navY + navH) {
            int itemH = 30;
            int relFromTop = navY + navH - screenY;
            int idx = relFromTop / itemH;
            if (idx >= 0 && idx < sections.size()) {
                selectedSectionIdx = idx;
                scrollOffset = 0f;
                clampScroll();
            }
            return true;
        }

        return true;
    }

    public void render(ShapeRenderer shapeRenderer,
                       SpriteBatch batch,
                       BitmapFont font,
                       int screenW,
                       int screenH,
                       Matrix4 projection) {
        if (!visible) {
            return;
        }

        panelX = (screenW - PANEL_W) / 2;
        panelY = (screenH - PANEL_H) / 2;
        closeX = panelX + PANEL_W - CLOSE_SIZE - 8;
        closeY = panelY + PANEL_H - CLOSE_SIZE - 7;
        navX = panelX + PANEL_W - NAV_W - 8;
        navY = panelY + 10;
        navW = NAV_W;
        navH = PANEL_H - HEADER_H - 16;
        contentX = panelX + CONTENT_PAD;
        contentY = panelY + CONTENT_PAD;
        contentW = PANEL_W - NAV_W - CONTENT_PAD * 2 - 12;
        contentH = PANEL_H - HEADER_H - CONTENT_PAD * 2;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(projection);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(OVERLAY);
        shapeRenderer.rect(0, 0, screenW, screenH);
        shapeRenderer.setColor(PANEL_BG);
        shapeRenderer.rect(panelX, panelY, PANEL_W, PANEL_H);
        shapeRenderer.setColor(HEADER_BG);
        shapeRenderer.rect(panelX + 2, panelY + PANEL_H - HEADER_H - 2, PANEL_W - 4, HEADER_H);
        shapeRenderer.setColor(NAV_BG);
        shapeRenderer.rect(navX, navY, navW, navH);
        shapeRenderer.setColor(CONTENT_BG);
        shapeRenderer.rect(contentX, contentY, contentW, contentH);
        shapeRenderer.setColor(CLOSE_BG);
        shapeRenderer.rect(closeX, closeY, CLOSE_SIZE, CLOSE_SIZE);

        int itemH = 30;
        for (int i = 0; i < sections.size(); i++) {
            int y = navY + navH - (i + 1) * itemH;
            if (y < navY) {
                break;
            }
            if (i == selectedSectionIdx) {
                shapeRenderer.setColor(NAV_SELECTED);
                shapeRenderer.rect(navX + 3, y + 2, navW - 6, itemH - 4);
            }
        }
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(PANEL_BORDER);
        shapeRenderer.rect(panelX, panelY, PANEL_W, PANEL_H);
        shapeRenderer.rect(panelX + 2, panelY + 2, PANEL_W - 4, PANEL_H - 4);
        shapeRenderer.rect(contentX, contentY, contentW, contentH);
        shapeRenderer.rect(navX, navY, navW, navH);
        shapeRenderer.rect(closeX, closeY, CLOSE_SIZE, CLOSE_SIZE);
        shapeRenderer.setColor(CLOSE_X);
        shapeRenderer.line(closeX + 5, closeY + 5, closeX + CLOSE_SIZE - 5, closeY + CLOSE_SIZE - 5);
        shapeRenderer.line(closeX + CLOSE_SIZE - 5, closeY + 5, closeX + 5, closeY + CLOSE_SIZE - 5);
        shapeRenderer.end();

        String title = provider != null ? provider.getTitle(skillIdx) : null;
        if (title == null || title.isBlank()) {
            title = (skillIdx >= 0 && skillIdx < SKILL_NAMES.length) ? SKILL_NAMES[skillIdx] : "Skill Guide";
        }

        batch.setProjectionMatrix(projection);
        batch.begin();
        font.getData().setScale(0.95f);
        font.setColor(0.20f, 0.12f, 0.04f, 1f);
        font.draw(batch, title + " Guide", panelX + 12, panelY + PANEL_H - 9);
        font.getData().setScale(0.72f);
        font.setColor(0.28f, 0.18f, 0.07f, 1f);
        font.draw(batch, "Level " + level + "   XP " + formatXp(totalXp), panelX + 14, panelY + PANEL_H - 24);
        font.getData().setScale(0.74f);
        for (int i = 0; i < sections.size(); i++) {
            int y = navY + navH - i * itemH - 10;
            if (y - itemH < navY) {
                break;
            }
            if (i == selectedSectionIdx) {
                font.setColor(0.18f, 0.12f, 0.04f, 1f);
            } else {
                font.setColor(0.32f, 0.20f, 0.08f, 1f);
            }
            font.draw(batch, sections.get(i).title(), navX + 10, y);
        }
        batch.end();

        if (provider != null && selectedSectionIdx >= 0 && selectedSectionIdx < sections.size()) {
            lastContentHeight = provider.getSectionContentHeight(skillIdx, level, selectedSectionIdx, contentW);
            clampScroll();
            Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
            Gdx.gl.glScissor(contentX + 1, contentY + 1, contentW - 2, contentH - 2);
            try {
                provider.renderSectionContent(shapeRenderer, batch, font, projection, skillIdx, level, totalXp,
                    selectedSectionIdx, contentX, contentY, contentW, contentH, scrollOffset);
            } finally {
                Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
            }
        } else {
            Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
            Gdx.gl.glScissor(contentX + 1, contentY + 1, contentW - 2, contentH - 2);
            try {
                renderFallbackContent(batch, font, projection);
            } finally {
                Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
            }
            lastContentHeight = 0f;
            clampScroll();
        }

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
    }

    private void renderFallbackContent(SpriteBatch batch, BitmapFont font, Matrix4 projection) {
        batch.setProjectionMatrix(projection);
        batch.begin();
        font.getData().setScale(0.82f);
        font.setColor(0.24f, 0.16f, 0.06f, 1f);
        font.draw(batch, "No guide available yet.", contentX + 12, contentY + contentH - 18);
        batch.end();
    }

    private void clampScroll() {
        float maxScroll = Math.max(0f, lastContentHeight - contentH);
        if (scrollOffset < 0f) {
            scrollOffset = 0f;
        }
        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
        }
    }

    private static String formatXp(long xp) {
        String s = Long.toString(xp);
        StringBuilder out = new StringBuilder();
        int start = s.length() % 3;
        if (start > 0) {
            out.append(s, 0, start);
        }
        for (int i = start; i < s.length(); i += 3) {
            if (out.length() > 0) {
                out.append(',');
            }
            out.append(s, i, i + 3);
        }
        return out.toString();
    }
}
