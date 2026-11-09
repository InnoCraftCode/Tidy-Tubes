package com.ben.laama.sortpuzzle.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;

import com.ben.laama.sortpuzzle.GameConfig;
import com.ben.laama.sortpuzzle.Main;
import com.ben.laama.sortpuzzle.i18n.Names;
import com.ben.laama.sortpuzzle.i18n.T;
import com.ben.laama.sortpuzzle.audio.SoundManager;
import com.ben.laama.sortpuzzle.logic.Cosmetic;
import com.ben.laama.sortpuzzle.logic.Difficulty;
import com.ben.laama.sortpuzzle.logic.Shop;
import com.ben.laama.sortpuzzle.logic.StarGates;
import com.ben.laama.sortpuzzle.logic.World;
import com.ben.laama.sortpuzzle.save.SaveData;
import com.ben.laama.sortpuzzle.ui.Motion;
import com.ben.laama.sortpuzzle.ui.Palette;
import com.ben.laama.sortpuzzle.ui.Tex;
import com.ben.laama.sortpuzzle.ui.Ui;

/**
 * The world map: a scrolling journey through biomes, one card per chapter of ten levels. Each
 * card carries the biome's name and sky, its progress, and the ten level tiles. Locked chapters
 * are visible but dimmed, so the road ahead is seen without pretending the game ends somewhere.
 */
public final class LevelSelectScreen extends BaseScreen {

    private static final int COLUMNS = 5;
    private static final int CHAPTER = World.CHAPTER_LEVELS;
    /** How many locked chapters to show past the furthest one unlocked. */
    private static final int LOOKAHEAD_CHAPTERS = 3;
    /**
     * How many chapters before the current one the map opens with. Each card owns a sky texture
     * and ten tiles, so the map builds one window at a time instead of all 100 000 chapters;
     * the START / EARLIER / LATER / END rows move the window anywhere from level 1 to 1M.
     */
    private static final int HISTORY_CHAPTERS = 30;
    private static final int WINDOW_CHAPTERS = HISTORY_CHAPTERS + 1 + LOOKAHEAD_CHAPTERS;
    /** How far EARLIER / LATER move the window. */
    private static final int PAGE_CHAPTERS = 30;
    private static final float CARD_WIDTH = 640f;
    private static final float TILE = 108f;
    private static final float TILE_PAD = 5f;

    private final ScrollPane scroll;
    private final Table map;
    private final Array<Texture> skies = new Array<>();
    private Actor currentCard;
    private final Array<Actor> cards = new Array<>();
    private int mapColumns;
    private int currentChapter;
    private int windowFirst;
    private int windowLast;
    private Table topNav;
    private Table bottomNav;
    private final int gateChapter;
    /** Campaign stars, read once per window build. */
    private int starsHeld;

    /**
     * One column of chapter cards on a portrait screen; two side by side once the world is wide
     * enough (a phone on its side or a large foldable), so a landscape screen is not a thin
     * strip with empty margins.
     */
    private void arrangeMap(float worldWidth) {
        int columns = worldWidth >= 2f * CARD_WIDTH + 80f ? 2 : 1;
        if (columns == mapColumns) {
            return;
        }
        mapColumns = columns;
        map.clearChildren();
        if (topNav != null) {
            map.add(topNav).colspan(columns).padTop(6f).padBottom(6f).row();
        }
        for (int i = 0; i < cards.size; i++) {
            boolean firstRow = i < columns;
            boolean endOfRow = (i % columns) == columns - 1;
            com.badlogic.gdx.scenes.scene2d.ui.Cell<Actor> cell = map.add(cards.get(i))
                .width(CARD_WIDTH).padTop(firstRow ? 6f : 18f).top();
            if (columns == 2) {
                cell.padLeft(i % 2 == 1 ? 12f : 0f).padRight(i % 2 == 0 ? 12f : 0f);
            }
            if (endOfRow) {
                cell.row();
            }
        }
        if (bottomNav != null) {
            if (cards.size % columns != 0) {
                map.row();
            }
            map.add(bottomNav).colspan(columns).padTop(18f).row();
        }
    }

    @Override
    protected void onResized(float worldWidth, float worldHeight) {
        arrangeMap(worldWidth);
    }

    public LevelSelectScreen(Main game) {
        this(game, 0);
    }

    /**
     * @param gateChapter a star-gated chapter the player just tried to enter, to explain on
     *                    arrival how many stars it still needs; {@code 0} for a normal visit
     */
    public LevelSelectScreen(Main game, int gateChapter) {
        super(game);
        this.gateChapter = gateChapter;
        SaveData save = game.save();
        background.setIntensity(0.5f);

        currentChapter = World.chapterOf(save.highestUnlocked);

        // The biome dressing is applied in show(), see themeOverride().

        Table root = new Table();
        root.setFillParent(true);
        safeArea.addActor(root);

        // -------------------------------------------------------------- header
        Table header = new Table();
        header.pad(24f, 26f, 8f, 26f);
        header.add(Ui.iconButton(skin, "icon-back", game::goHome)).size(Ui.ICON_BUTTON).left();
        Table titles = new Table();
        titles.add(new Label(T.r("World"), skin, "title")).row();
        World.Biome here = World.biomeFor(save.currentLevel);
        titles.add(new Label(T.f("{0}  •  {1} levels cleared", Names.name(here.name), save.completedCount()),
            skin, "caption")).padTop(-2f);
        header.add(titles).expandX();
        header.add().size(Ui.ICON_BUTTON);
        root.add(header).growX().row();

        // ----------------------------------------------------------------- map
        map = new Table();
        map.top();
        map.pad(6f, 0f, 40f, 0f);
        scroll = new ScrollPane(map, skin);
        buildWindow(Math.max(1, currentChapter - HISTORY_CHAPTERS));

        scroll.setFadeScrollBars(true);
        scroll.setScrollbarsOnTop(true);
        scroll.setScrollingDisabled(true, false);
        scroll.setOverscroll(false, true);
        root.add(scroll).grow().row();
    }

    /**
     * Builds the cards of one window of chapters, starting at {@code first}. Only a window is
     * ever built (each card owns a texture and ten tiles); the navigation rows above and below
     * move it, so every chapter from 1 to the last one is reachable without freezing the map.
     */
    private void buildWindow(int first) {
        int lastWorld = World.chapterOf(GameConfig.MAX_LEVEL);
        int start = Math.max(1, Math.min(first, lastWorld - WINDOW_CHAPTERS + 1));
        int end = Math.min(lastWorld, start + WINDOW_CHAPTERS - 1);
        windowFirst = start;
        windowLast = end;

        for (Texture sky : skies) {
            sky.dispose();
        }
        skies.clear();
        cards.clear();
        currentCard = null;
        SaveData save = game.save();
        starsHeld = save.totalStars();
        for (int chapter = start; chapter <= end; chapter++) {
            Actor card = chapterCard(World.biomeOfChapter(chapter), save);
            if (chapter == currentChapter) {
                currentCard = card;
            }
            cards.add(card);
        }
        topNav = windowFirst > 1 ? navRow(
            navButton(T.r("START"), () -> jumpTo(1)),
            navButton(T.r("EARLIER"), () -> jumpTo(windowFirst - PAGE_CHAPTERS)),
            currentCard == null ? navButton(T.r("MY CHAPTER"), this::jumpHome) : null) : null;
        bottomNav = windowLast < lastWorld ? navRow(
            currentCard == null ? navButton(T.r("MY CHAPTER"), this::jumpHome) : null,
            navButton(T.r("LATER"), () -> jumpTo(windowFirst + PAGE_CHAPTERS)),
            navButton(T.r("END"), () -> jumpTo(lastWorld))) : null;
        mapColumns = 0; // force a re-layout
        arrangeMap(stage.getWidth());

        // Entrance: only the cards around where the map opens cascade in (one before the
        // focus, then the next five); off-screen cards just appear, nobody sees them arrive.
        int focus = currentCard != null ? Math.max(0, cards.indexOf(currentCard, true) - 1) : 0;
        for (int i = focus; i < Math.min(cards.size, focus + 6); i++) {
            Motion.enter(cards.get(i), i - focus);
        }
        if (topNav != null) {
            Motion.enter(topNav, 0);
        }
    }

    private void jumpTo(int first) {
        game.sound().play(SoundManager.Cue.BUTTON);
        buildWindow(first);
        scrollToFocus();
    }

    private void jumpHome() {
        jumpTo(Math.max(1, currentChapter - HISTORY_CHAPTERS));
    }

    private TextButton navButton(String text, Runnable action) {
        return Ui.button(skin, text, "ghost", action);
    }

    private Table navRow(TextButton... buttons) {
        Table row = new Table();
        for (TextButton button : buttons) {
            if (button != null) {
                row.add(button).height(64f).pad(0f, 6f, 0f, 6f);
            }
        }
        return row;
    }

    /** Scrolls to the player's chapter when it is in the window, else to the top. */
    private void scrollToFocus() {
        stage.act(0f);
        scroll.validate();
        map.validate();
        float y = 0f;
        if (currentCard != null) {
            float fromTop = map.getHeight() - currentCard.getY() - currentCard.getHeight();
            y = Math.max(0f, fromTop - 24f);
        }
        scroll.setScrollY(y);
        scroll.updateVisualScroll();
    }

    /** Long level numbers (up to 1000000) shrink to fit inside their tile instead of overflowing. */
    private static void fitToTile(Label number) {
        float max = TILE - 16f;
        float width = number.getPrefWidth();
        if (width > max) {
            Ui.setTextScale(number, max / width);
        }
    }

    /** One biome: its sky, name, progress line and the ten tiles. */
    private Actor chapterCard(World.Biome biome, SaveData save) {
        Cosmetic theme = Cosmetic.byId(biome.themeId);
        Color top = Color.valueOf(theme != null ? theme.hex(0) : "222B5E");
        Color bottom = Color.valueOf(theme != null ? theme.hex(1) : "0B0E24");
        Color lightA = Color.valueOf(theme != null ? theme.hex(2) : "3FB7C9");
        Color lightB = Color.valueOf(theme != null ? theme.hex(3) : "7B5CE6");
        int stars = starsHeld;
        int needed = StarGates.requiredStars(biome.chapter);
        boolean starLocked = !StarGates.isOpenFor(save, biome.chapter, stars);
        boolean reached = save.highestUnlocked >= biome.firstLevel && !starLocked;
        boolean done = save.highestUnlocked > biome.lastLevel;
        float progress = World.progressIn(biome, save.highestUnlocked);

        Table card = new Table();
        card.setBackground(skin.newDrawable("card", reached ? Palette.SURFACE : Color.valueOf("FFFFFF0A")));
        card.pad(0f, 0f, 16f, 0f);

        // Sky band: the biome's gradient with its two lights, the name on top of it.
        Texture sky = Tex.verticalGradient(48, top, bottom);
        skies.add(sky);
        Stack band = new Stack();
        Image gradient = new Image(new TextureRegionDrawable(new TextureRegion(sky)));
        band.add(gradient);
        Table lights = new Table();
        lights.add(glow(lightA)).size(220f).pad(-80f, -40f, -80f, 160f);
        lights.add(glow(lightB)).size(180f).pad(-60f, 160f, -60f, -40f).expandX().right();
        band.add(lights);
        Table titleRow = new Table();
        titleRow.pad(14f, 22f, 12f, 22f);
        Table names = new Table();
        Label chapterLabel = new Label(T.f("CHAPTER {0}  •  LEVELS {1}–{2}", biome.chapter,
            String.valueOf(biome.firstLevel), String.valueOf(biome.lastLevel)), skin, "small");
        chapterLabel.setColor(Palette.TEXT_DIM);
        names.add(chapterLabel).left().row();
        Label name = new Label(T.show(Names.name(biome.name)), skin, "heading");
        names.add(name).left().padTop(-2f);
        titleRow.add(names).left().expandX();
        String state = done ? T.r("COMPLETE") : reached ? Math.round(progress * CHAPTER) + " / " + CHAPTER
            : T.r("LOCKED");
        Label stateLabel = new Label(state, skin, "small");
        stateLabel.setColor(done ? Palette.SUCCESS : reached ? Palette.PRIMARY : Palette.TEXT_DIM);
        if (starLocked) {
            // Star gate: "★ 123 / 180" in gold, the stars held over the stars needed.
            Table gate = new Table();
            gate.add(new Image(skin.newDrawable("icon-star", Palette.GOLD))).size(20f).padRight(6f);
            Label count = new Label(stars + " / " + needed, skin, "small");
            count.setColor(Palette.GOLD);
            gate.add(count);
            titleRow.add(gate).right();
        } else if (!reached) {
            Table locked = new Table();
            locked.add(new Image(skin.newDrawable("icon-lock", Palette.TEXT_DIM))).size(18f).padRight(6f);
            locked.add(stateLabel);
            titleRow.add(locked).right();
        } else {
            titleRow.add(stateLabel).right();
        }
        band.add(titleRow);
        card.add(band).size(CARD_WIDTH, 92f).row();

        // Progress bar under the sky.
        Table bar = new Table();
        bar.setBackground(skin.newDrawable("white", Color.valueOf("FFFFFF14")));
        Table fill = new Table();
        fill.setBackground(skin.newDrawable("white", done ? Palette.SUCCESS : Palette.PRIMARY));
        bar.add(fill).width(Math.max(0f, (CARD_WIDTH - 44f) * progress)).height(4f).left().expandX();
        card.add(bar).width(CARD_WIDTH - 44f).height(4f).padTop(10f).row();

        // Colours line: what this chapter asks for.
        int first = Difficulty.colorsFor(biome.firstLevel);
        int last = Difficulty.colorsFor(biome.lastLevel);
        String colours = first == last ? T.rawf("{0} colours", first) : T.rawf("{0}–{1} colours", first, last);
        card.add(new Label(T.f("{0}  •  {1}", colours, T.raw(Difficulty.bandName(biome.firstLevel))), skin, "caption"))
            .left().padLeft(22f).padTop(10f).row();
        if (starLocked) {
            Label gateLine = new Label(T.f("{0} more stars to open this chapter", needed - stars),
                skin, "caption");
            gateLine.setColor(Palette.GOLD);
            card.add(gateLine).left().padLeft(22f).padTop(4f).row();
        }

        // The tiles.
        Table tiles = new Table();
        for (int level = biome.firstLevel; level <= biome.lastLevel; level++) {
            tiles.add(tile(level)).size(TILE, TILE).pad(TILE_PAD);
            if ((level - biome.firstLevel) % COLUMNS == COLUMNS - 1) tiles.row();
        }
        card.add(tiles).padTop(8f).row();

        // The skin this chapter gives when cleared (world rewards in the shop).
        Cosmetic reward = chapterReward(biome.chapter);
        if (reward != null) {
            Table line = new Table();
            line.add(new Image(skin.newDrawable(done ? "icon-check" : "icon-grid",
                done ? Palette.SUCCESS : Palette.GOLD))).size(20f).padRight(8f);
            Label text = new Label(T.f(done ? T.k("Unlocked: {0}  •  {1}") : T.k("Clear to unlock: {0}  •  {1}"),
                Names.name(reward.displayName()), T.raw(reward.category().title())), skin, "caption");
            text.setColor(done ? Palette.TEXT_DIM : Palette.GOLD);
            line.add(text);
            card.add(line).left().padLeft(22f).padTop(6f).row();
        }
        if (!reached) {
            card.getColor().a = 0.7f;
        }
        return card;
    }

    private Image glow(Color color) {
        Image image = new Image(skin.getDrawable("glow"));
        image.setColor(color.r, color.g, color.b, 0.5f);
        return image;
    }

    /** @return the world-reward skin for that chapter, or {@code null} past the last one */
    private static Cosmetic chapterReward(int chapter) {
        for (Cosmetic item : Cosmetic.ALL) {
            if (item.unlock() == Cosmetic.Unlock.CHAPTER && item.requirement() == chapter) {
                return item;
            }
        }
        return null;
    }

    private Actor tile(int level) {
        SaveData save = game.save();
        // Progress and the chapter's star gate (stars counted once per window, not per tile).
        boolean unlocked = save.isUnlocked(level)
            && StarGates.isOpenFor(save, World.chapterOf(level), starsHeld);
        boolean completed = save.isCompleted(level);
        boolean current = level == save.currentLevel && unlocked;

        String style = completed ? "tile-done" : current ? "tile-current"
            : unlocked ? "tile-open" : "tile-locked";
        Button button = new Button(skin.get(style, TextButton.TextButtonStyle.class));
        TextButton.TextButtonStyle textStyle = skin.get(style, TextButton.TextButtonStyle.class);
        button.setDisabled(!unlocked);
        if (unlocked) {
            Ui.click(button, () -> {
                game.sound().play(SoundManager.Cue.BUTTON);
                game.goToLevel(level);
            });
        }

        Table content = new Table();
        if (!unlocked) {
            Image lock = new Image(skin.newDrawable("icon-lock", Palette.TEXT_DISABLED));
            content.add(lock).size(26f).row();
            // Exact numbers on the map: "999.6k" on ten tiles in a row would be unreadable.
            Label number = new Label(String.valueOf(level), skin, "caption");
            fitToTile(number);
            content.add(number).padTop(2f);
        } else {
            Label number = new Label(String.valueOf(level),
                new Label.LabelStyle(skin.getFont("heading"), textStyle.fontColor));
            fitToTile(number);
            content.add(number).row();
            int stars = completed ? save.bestStarsFor(level) : 0;
            if (stars > 0) {
                // The rating, as tiny stars: the font has no star glyph.
                Table row = new Table();
                for (int i = 0; i < 3; i++) {
                    Image star = new Image(skin.newDrawable("icon-star",
                        i < stars ? Palette.GOLD : Color.valueOf("FFFFFF33")));
                    row.add(star).size(14f).pad(0f, 1f, 0f, 1f);
                }
                content.add(row).padTop(-2f);
            } else {
                String caption = completed ? T.f("{0} mv", save.bestMovesFor(level))
                    : current ? T.r("next") : T.f("{0} col", Difficulty.colorsFor(level));
                Label sub = new Label(caption,
                    new Label.LabelStyle(skin.getFont("small"), textStyle.fontColor));
                sub.getColor().a = 0.8f;
                content.add(sub).padTop(-4f);
            }
        }
        button.add(content);

        if (current && !completed) {
            button.setTransform(true);
            button.setOrigin(Align.center);
            button.addAction(Actions.forever(Actions.sequence(
                Actions.scaleTo(1.06f, 1.06f, 0.9f, Interpolation.sine),
                Actions.scaleTo(1f, 1f, 0.9f, Interpolation.sine))));
        }
        return button;
    }

    @Override
    public void show() {
        super.show();
        // Open on the chapter the player is in, not at the top of the world.
        scrollToFocus();
        if (gateChapter > 0) {
            int missing = StarGates.requiredStars(gateChapter) - game.save().totalStars();
            if (missing > 0) {
                Ui.toast(stage, skin, T.f("{0} more stars to open this chapter", missing), Palette.GOLD);
            }
        }
    }

    @Override
    public void hide() {
        super.hide(); // the next screen applies its own look in show()
    }

    /** The map wears the biome the player is in, unless they chose a theme of their own. */
    @Override
    protected Cosmetic themeOverride() {
        SaveData save = game.save();
        if (Shop.equipped(save, Cosmetic.Category.THEME) != Cosmetic.defaultFor(Cosmetic.Category.THEME)) {
            return null;
        }
        return Cosmetic.byId(World.biomeFor(save.currentLevel).themeId);
    }

    @Override
    public void dispose() {
        for (Texture sky : skies) {
            sky.dispose();
        }
        skies.clear();
        super.dispose();
    }
}
