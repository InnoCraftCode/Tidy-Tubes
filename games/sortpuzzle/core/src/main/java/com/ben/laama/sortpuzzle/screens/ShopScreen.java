package com.ben.laama.sortpuzzle.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ObjectMap;

import com.ben.laama.sortpuzzle.Main;
import com.ben.laama.sortpuzzle.i18n.Names;
import com.ben.laama.sortpuzzle.i18n.T;
import com.ben.laama.sortpuzzle.audio.SoundManager;
import com.ben.laama.sortpuzzle.logic.Cosmetic;
import com.ben.laama.sortpuzzle.logic.Shop;
import com.ben.laama.sortpuzzle.logic.StarRoad;
import com.ben.laama.sortpuzzle.save.SaveData;
import com.ben.laama.sortpuzzle.services.AnalyticsEvents;
import com.ben.laama.sortpuzzle.ui.BallActor;
import com.ben.laama.sortpuzzle.ui.Cosmetics;
import com.ben.laama.sortpuzzle.ui.Palette;
import com.ben.laama.sortpuzzle.ui.Tex;
import com.ben.laama.sortpuzzle.ui.TubeActor;
import com.ben.laama.sortpuzzle.ui.Ui;

import java.util.List;

/**
 * The cosmetic shop: ball palettes, ball finishes, tube styles and themes, paid with the coins
 * levels already pay out. A live board at the top wears whatever card is tapped (and, for a
 * theme, so does the backdrop) before anything is bought; buying equips at once.
 *
 * <p>Every decision - owned, affordable, equipped - comes from {@link Shop}. This screen only
 * draws the answers, with every action spelled out in words, and asks for a confirmation
 * before coins move.</p>
 */
public final class ShopScreen extends BaseScreen {

    /** Root pads 28 a side on the 720 world: 664 available, the grid's gutters included. */
    private static final float CONTENT_WIDTH = 636f;
    private static final float CARD_GAP = 14f;
    private static final float CARD_WIDTH = (CONTENT_WIDTH - CARD_GAP) * 0.5f;
    private static final float PREVIEW_BALL = 46f;
    private static final float CARD_RISE = 18f;

    private final SaveData save;
    private final Label coinsLabel;
    private final Label previewCaption;
    private final Label sectionTitle;
    private final Label sectionCount;
    private final Table grid = new Table();
    private ScrollPane gridScroll;
    /** Skin tabs only: which slice of the 100 items the grid shows. */
    private Shop.Filter filter = Shop.Filter.ALL;
    private final Table filterRow = new Table();
    private Table[] filterChips;
    private Label[] filterLabels;
    private final Group preview = new Group();
    /** The cosmetic categories, then one more segment for boosters. */
    private static final int BOOSTERS_TAB = Cosmetic.Category.values().length;
    private final Table[] tabs = new Table[BOOSTERS_TAB + 1];
    private final Label[] tabLabels = new Label[BOOSTERS_TAB + 1];
    private final ObjectMap<String, Texture> swatchTextures = new ObjectMap<>();
    private Cosmetic.Category tab = Cosmetic.Category.BALL_PALETTE;
    /** When set, the grid shows booster packs instead of a cosmetic category. */
    private boolean boostersTab;
    private boolean rewardedTubeAdInFlight;
    private TextButton rewardedTubeButton;
    private float rewardedTubeRefreshTimer;
    /** The item being previewed, or {@code null} when the preview shows the equipped set. */
    private Cosmetic previewed;

    public ShopScreen(Main game) {
        this(game, Cosmetic.Category.BALL_PALETTE);
    }

    public ShopScreen(Main game, Cosmetic.Category openOn) {
        super(game);
        save = game.save();
        background.setIntensity(0.45f);
        game.analytics().event(AnalyticsEvents.SHOP_OPEN, AnalyticsEvents.P_COINS, save.coins);

        Table root = new Table();
        root.setFillParent(true);
        root.pad(22f, 28f, 16f, 28f);
        safeArea.addActor(root);

        // ---- header: back, title + promise, purse ----------------------------------------
        Table header = new Table();
        header.add(Ui.iconButton(skin, "icon-back", this::goBack)).size(Ui.ICON_BUTTON).left();
        Table titles = new Table();
        titles.add(new Label(T.r("Shop"), skin, "title")).row();
        titles.add(new Label(T.r("Styles are looks only  •  Tubes+ help you play"), skin, "caption"))
            .padTop(-2f);
        header.add(titles).expandX();
        coinsLabel = new Label(String.valueOf(save.coins), skin, "gold");
        header.add(Ui.chip(skin, "icon-coin", Palette.GOLD, coinsLabel)).right();
        root.add(header).growX().row();

        // ---- hero: the live board on a soft glow, with what it is wearing ----------------
        float tubeWidth = TubeActor.widthFor(PREVIEW_BALL);
        float tubeHeight = TubeActor.heightFor(PREVIEW_BALL);
        preview.setSize(tubeWidth * 3f + 44f, tubeHeight + 20f);
        Image halo = new Image(skin.getDrawable("glow"));
        halo.setColor(Palette.PRIMARY.r, Palette.PRIMARY.g, Palette.PRIMARY.b, 0.22f);
        Stack hero = new Stack();
        Table haloBox = new Table();
        haloBox.add(halo).size(preview.getWidth() * 1.9f, preview.getHeight() * 1.9f);
        hero.add(haloBox);
        Table previewBox = new Table();
        previewBox.add(preview).size(preview.getWidth(), preview.getHeight());
        hero.add(previewBox);
        root.add(hero).size(preview.getWidth() * 1.9f, preview.getHeight() + 56f).padTop(-6f).row();
        previewCaption = new Label("", skin, "caption");
        previewCaption.setAlignment(Align.center);
        root.add(previewCaption).padTop(-22f).row();

        // ---- segmented tabs ---------------------------------------------------------------
        Table segments = new Table();
        segments.setBackground(skin.newDrawable("pill", Palette.SURFACE));
        segments.pad(5f);
        Cosmetic.Category[] categories = Cosmetic.Category.values();
        for (int i = 0; i <= BOOSTERS_TAB; i++) {
            final boolean boosters = i == BOOSTERS_TAB;
            final Cosmetic.Category category = boosters ? null : categories[i];
            Table segment = new Table();
            segment.setTouchable(Touchable.enabled);
            Label label = new Label(boosters ? T.r("TUBES+") : T.show(T.raw(category.title()).toUpperCase(new java.util.Locale(T.language().code))), skin, "label");
            // Five equal segments: a long word ("OBERFLÄCHE") shrinks to fit its pill.
            float segmentWidth = (CONTENT_WIDTH - 10f) / (BOOSTERS_TAB + 1);
            float room = segmentWidth - 16f;
            if (label.getPrefWidth() > room) {
                Ui.setTextScale(label, Math.max(0.7f, room / label.getPrefWidth()));
            }
            segment.add(label);
            segment.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (boostersTab == boosters && (boosters || tab == category)) return;
                    game.sound().play(SoundManager.Cue.BUTTON);
                    boostersTab = boosters;
                    if (!boosters) tab = category;
                    previewed = null;
                    refresh();
                }
            });
            tabs[i] = segment;
            tabLabels[i] = label;
            segments.add(segment).width((CONTENT_WIDTH - 10f) / (BOOSTERS_TAB + 1)).height(52f);
        }
        root.add(segments).width(CONTENT_WIDTH).padTop(16f).row();

        // ---- section line + grid ------------------------------------------------------------
        Table section = new Table();
        sectionTitle = new Label("", skin, "heading");
        sectionCount = new Label("", skin, "caption");
        section.add(sectionTitle).left().expandX();
        section.add(sectionCount).right();
        root.add(section).width(CONTENT_WIDTH).padTop(16f).row();

        // ---- filters: ALL / OWNED / FOR COINS / TO EARN (skin tabs only) ------------------------
        Shop.Filter[] filters = Shop.Filter.values();
        filterChips = new Table[filters.length];
        filterLabels = new Label[filters.length];
        for (int i = 0; i < filters.length; i++) {
            final Shop.Filter value = filters[i];
            Table chip = new Table();
            chip.setTouchable(Touchable.enabled);
            chip.pad(0f, 14f, 0f, 14f);
            Label label = new Label(T.r(value.label()), skin, "small");
            chip.add(label);
            chip.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (filter == value) return;
                    game.sound().play(SoundManager.Cue.BUTTON);
                    filter = value;
                    refresh();
                    scrollCatalogueTo(0f);
                }
            });
            filterChips[i] = chip;
            filterLabels[i] = label;
            // 52 units: the "pill" nine-patch needs it (shorter ones draw a seam).
            filterRow.add(chip).height(52f).padRight(i < filters.length - 1 ? 8f : 0f);
        }
        filterRow.left();
        root.add(filterRow).width(CONTENT_WIDTH).padTop(10f).row();

        grid.top();
        grid.defaults().width(CARD_WIDTH).pad(CARD_GAP * 0.5f).top();
        ScrollPane scroll = new ScrollPane(grid, skin);
        gridScroll = scroll;
        scroll.setScrollingDisabled(true, false);
        scroll.setFadeScrollBars(true);
        // The skin's scrollbar is a 52-unit pill: drawn over the content, not beside it, or the
        // two-column grid loses that width and both columns get clipped.
        scroll.setScrollbarsOnTop(true);
        root.add(scroll).width(CONTENT_WIDTH + CARD_GAP).grow().padTop(4f).row();

        tab = openOn == null ? Cosmetic.Category.BALL_PALETTE : openOn;
        refresh();

        root.getColor().a = 0f;
        root.addAction(Actions.fadeIn(0.28f, Interpolation.fade));
    }

    // ------------------------------------------------------------------ drawing
    private void refresh() {
        coinsLabel.setText(String.valueOf(save.coins));
        Cosmetic.Category[] categories = Cosmetic.Category.values();
        for (int i = 0; i <= BOOSTERS_TAB; i++) {
            boolean active = i == BOOSTERS_TAB ? boostersTab : !boostersTab && categories[i] == tab;
            tabs[i].setBackground(active ? skin.newDrawable("pill", Palette.PRIMARY) : null);
            tabLabels[i].setColor(active ? Palette.ON_PRIMARY : Palette.TEXT_DIM);
        }
        Shop.Filter[] filters = Shop.Filter.values();
        for (int i = 0; i < filters.length; i++) {
            boolean active = filters[i] == filter;
            filterChips[i].setBackground(skin.newDrawable("pill",
                active ? Palette.SURFACE_STRONG : Palette.SURFACE));
            filterLabels[i].setColor(active ? Palette.PRIMARY : Palette.TEXT_DIM);
        }
        filterRow.setVisible(!boostersTab);
        if (boostersTab) {
            refreshBoosters();
            return;
        }
        List<Cosmetic> all = Cosmetic.of(tab);
        int owned = 0;
        for (Cosmetic item : all) {
            if (Shop.isOwned(save, item)) owned++;
        }
        List<Cosmetic> items = Shop.filter(save, all, filter);
        sectionTitle.setText(T.r(sectionName(tab)));
        sectionCount.setText(T.f("{0} / {1} owned", owned, all.size()));

        grid.clearChildren();
        for (int i = 0; i < items.size(); i++) {
            Actor card = card(items.get(i));
            card.getColor().a = 0f;
            card.addAction(Actions.sequence(
                Actions.delay(0.03f * Math.min(i, 12)), // 100 cards: only the first screenful cascades
                Actions.parallel(
                    Actions.fadeIn(0.22f, Interpolation.fade),
                    Actions.sequence(Actions.moveBy(0f, -CARD_RISE),
                        Actions.moveBy(0f, CARD_RISE, 0.26f, Interpolation.pow2Out)))));
            grid.add(card);
            if (i % 2 == 1) grid.row();
        }
        if (items.isEmpty()) {
            String empty = filter == Shop.Filter.OWNED ? T.r("Nothing owned here yet: buy or earn one.")
                : filter == Shop.Filter.COINS ? T.r("You own every item coins can buy here.")
                : T.r("Every reward of this tab is already yours.");
            grid.add(new Label(empty, skin, "caption")).colspan(2).width(CONTENT_WIDTH).padTop(30f);
        }
        rebuildPreview();
    }

    private static String sectionName(Cosmetic.Category category) {
        switch (category) {
            case BALL_PALETTE:
                return T.k("Ball colours");
            case BALL_FINISH:
                return T.k("Ball finishes");
            case TUBE:
                return T.k("Tube styles");
            default:
                return T.k("Themes");
        }
    }

    private Actor card(final Cosmetic item) {
        boolean owned = Shop.isOwned(save, item);
        boolean equipped = Shop.isEquipped(save, item);
        boolean selected = item == previewed || (previewed == null && equipped);

        // The item in use wears a mint outline, drawn over its own background so the card
        // stays a plain grid child (a wrapper would shift its padding).
        final Drawable outline = equipped ? skin.newDrawable("card-outline", Palette.PRIMARY) : null;
        Table card = new Table() {
            @Override
            protected void drawBackground(com.badlogic.gdx.graphics.g2d.Batch batch,
                                          float parentAlpha, float x, float y) {
                super.drawBackground(batch, parentAlpha, x, y);
                if (outline != null) {
                    Color c = getColor();
                    batch.setColor(c.r, c.g, c.b, c.a * parentAlpha);
                    outline.draw(batch, x, y, getWidth(), getHeight());
                }
            }
        };
        card.setBackground(skin.newDrawable("card", selected ? Palette.SURFACE_STRONG : Palette.SURFACE));
        card.pad(16f, 18f, 16f, 18f);
        card.setTouchable(Touchable.enabled);
        card.defaults().left();

        // visual zone: what the item looks like, big enough to judge
        card.add(visual(item)).height(92f).growX().row();

        // name + state badge
        // The name gives way to the badge: a long name (or translation) ends in "..." instead of
        // widening the card past its grid column, which pushed badges onto the next card.
        Table title = new Table();
        addCardName(title, T.show(Names.name(item.displayName())));
        if (equipped) {
            title.add(badge(T.r("EQUIPPED"), Palette.PRIMARY, Palette.ON_PRIMARY)).right();
        } else if (owned) {
            title.add(badge(T.r("OWNED"), Palette.SURFACE_STRONG, Palette.TEXT)).right();
        } else if (item.unlock() == Cosmetic.Unlock.STARS) {
            // Star-road reward: the badge says the price in stars, not in coins.
            title.add(starBadge(StarRoad.starsRequired(item))).right();
        } else if (item.unlock() == Cosmetic.Unlock.CHAPTER) {
            // World reward: which chapter of the map gives it.
            title.add(badge(T.f("CH {0}", item.requirement()), Palette.PRIMARY, Palette.ON_PRIMARY)).right();
        } else if (!item.isForSale()) {
            Image lock = new Image(skin.newDrawable("icon-lock", Palette.TEXT_DIM));
            title.add(lock).size(22f).right();
        } else if (item.price() >= 1500) {
            title.add(badge(T.r("PRESTIGE"), Palette.GOLD, Palette.ON_GOLD)).right();
        }
        card.add(title).width(CARD_WIDTH - 36f).padTop(10f).row();

        Label description = new Label(T.show(owned || item.isForSale() ? Names.description(item.description())
            : Shop.unlockHint(item)), skin, "caption");
        description.setWrap(true);
        card.add(description).width(CARD_WIDTH - 36f).padTop(4f).row();

        // one action, in words
        Actor action;
        if (equipped) {
            // Not a button: a calm full-width mark that this is the one in use.
            Table inUse = new Table();
            inUse.setBackground(skin.newDrawable("pill", Palette.SURFACE_STRONG));
            Image check = new Image(skin.newDrawable("icon-check", Palette.PRIMARY));
            inUse.add(check).size(22f).padRight(8f);
            Label inUseLabel = new Label(T.r("IN USE"), skin, "label");
            inUseLabel.setColor(Palette.PRIMARY);
            inUse.add(inUseLabel);
            action = inUse;
        } else if (owned) {
            action = Ui.button(skin, T.r("EQUIP"), "ghost", () -> equip(item));
        } else if (item.isForSale()) {
            Button buy = Ui.iconTextButton(skin, "icon-coin", T.f("BUY  {0}", item.price()), "gold",
                () -> confirmPurchase(item));
            buy.getColor().a = Shop.canBuy(save, item) ? 1f : 0.55f;
            action = buy;
        } else if (item.unlock() == Cosmetic.Unlock.STARS) {
            // How far along the star road the player is for this skin: [star] 42 / 60.
            // Not a button (nothing to tap): a pill like "IN USE", with the count in gold.
            Table road = new Table();
            road.setBackground(skin.newDrawable("pill", Palette.SURFACE_STRONG));
            road.add(new Image(skin.newDrawable("icon-star", Palette.GOLD))).size(22f).padRight(8f);
            Label count = new Label(Shop.starCount(save, item), skin, "label");
            count.setColor(Palette.GOLD);
            road.add(count);
            action = road;
        } else if (item.unlock() == Cosmetic.Unlock.CHAPTER) {
            // Where on the map it is earned: [map] Chapter 12.
            Table world = new Table();
            world.setBackground(skin.newDrawable("pill", Palette.SURFACE_STRONG));
            world.add(new Image(skin.newDrawable("icon-grid", Palette.PRIMARY))).size(22f).padRight(8f);
            Label chapterLabel = new Label(T.f("Chapter {0}", item.requirement()), skin, "label");
            chapterLabel.setColor(Palette.PRIMARY);
            world.add(chapterLabel);
            action = world;
        } else {
            TextButton locked = Ui.button(skin, T.r("LOCKED"), "secondary", () -> { });
            locked.setDisabled(true);
            action = locked;
        }
        card.add(action).growX().height(54f).padTop(12f).row();

        final Actor actionActor = action;
        card.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (event.getTarget() == actionActor || event.getTarget().isDescendantOf(actionActor)) {
                    return; // the button handles itself
                }
                game.sound().play(SoundManager.Cue.PICK);
                previewed = item;
                refresh();
            }
        });

        return card;
    }

    /**
     * A card's name, left of its badge. It gives way to the badge: a long name or translation
     * ends in "..." instead of widening the card past its grid column (badges and buttons then
     * spilled onto the neighbouring card).
     */
    private void addCardName(Table titleRow, String text) {
        Label name = new Label(text, skin, "label");
        name.setEllipsis(true);
        titleRow.add(name).left().expandX().fillX().minWidth(0f).padRight(8f);
    }

    private Table badge(String text, Color background, Color textColor) {
        Table pill = new Table();
        pill.setBackground(skin.newDrawable("pill", background));
        pill.pad(3f, 10f, 3f, 10f);
        Label label = new Label(text, skin, "small");
        label.setColor(textColor);
        pill.add(label);
        return pill;
    }

    /** Gold pill with a star icon and the stars a star-road skin needs. */
    private Table starBadge(int stars) {
        Table pill = new Table();
        pill.setBackground(skin.newDrawable("pill", Palette.GOLD));
        pill.pad(3f, 10f, 3f, 10f);
        pill.add(new Image(skin.newDrawable("icon-star", Palette.ON_GOLD))).size(16f).padRight(4f);
        Label label = new Label(String.valueOf(stars), skin, "small");
        label.setColor(Palette.ON_GOLD);
        pill.add(label);
        return pill;
    }

    /** A category-specific sample: all ten dots, three finished balls, a real tube, a sky. */
    private Actor visual(Cosmetic item) {
        Table zone = new Table();
        switch (item.category()) {
            case BALL_PALETTE: {
                Table dots = new Table();
                for (int i = 0; i < item.hexCount(); i++) {
                    Image dot = new Image(skin.newDrawable("circle", Color.valueOf(item.hex(i))));
                    dots.add(dot).size(30f).pad(4f);
                    if (i == 4) dots.row();
                }
                zone.add(dots);
                break;
            }
            case BALL_FINISH: {
                Texture texture = swatch("finish:" + item.id(), () -> Tex.ball(128, item.id()));
                Drawable sprite = new TextureRegionDrawable(new TextureRegion(texture));
                int[] colours = {1, 3, 0};
                for (int i = 0; i < colours.length; i++) {
                    Image ball = new Image(sprite);
                    ball.setColor(Palette.ball(colours[i]));
                    zone.add(ball).size(62f).padLeft(i == 0 ? 0f : 10f);
                }
                break;
            }
            case TUBE: {
                // A real tube in that style, with three balls, next to its rim colour.
                Group tube = miniTube(item);
                zone.add(tube).size(tube.getWidth(), tube.getHeight()).padRight(16f);
                Table legend = new Table();
                legend.add(new Image(skin.newDrawable("circle", Color.valueOf(item.hex(1)))))
                    .size(22f).padRight(8f);
                legend.add(new Label(T.r("rim"), skin, "caption")).row();
                Image body = new Image(skin.newDrawable("circle", Color.valueOf(item.hex(0))));
                legend.add(body).size(22f).padRight(8f).padTop(6f);
                legend.add(new Label(T.r("glass"), skin, "caption")).padTop(6f);
                zone.add(legend).left();
                break;
            }
            default: {
                Texture gradient = swatch("theme:" + item.id(), () -> Tex.verticalGradient(
                    48, Color.valueOf(item.hex(0)), Color.valueOf(item.hex(1))));
                Stack sky = new Stack();
                sky.add(new Image(new TextureRegionDrawable(new TextureRegion(gradient))));
                Table lights = new Table();
                for (int i = 2; i < 4; i++) {
                    Image glow = new Image(skin.getDrawable("glow"));
                    Color c = Color.valueOf(item.hex(i));
                    glow.setColor(c.r, c.g, c.b, 0.75f);
                    lights.add(glow).size(70f).pad(i == 2 ? -14f : 6f, 10f, -14f, 10f);
                }
                sky.add(lights);
                Table frame = new Table();
                frame.add(sky).size(CARD_WIDTH - 36f, 84f);
                zone.add(frame);
                break;
            }
        }
        return zone;
    }

    private Group miniTube(Cosmetic style) {
        float ball = 22f;
        float width = TubeActor.widthFor(ball);
        float height = TubeActor.heightFor(ball);
        // Draw this one tube in its own style, whatever is equipped: swap the shared colours
        // around the group's draw. Cheap: two colour sets, no texture.
        Group group = new Group() {
            @Override
            public void draw(com.badlogic.gdx.graphics.g2d.Batch batch, float parentAlpha) {
                Cosmetic current = Palette.tubeStyle();
                Palette.applyTubeStyle(style);
                super.draw(batch, parentAlpha);
                Palette.applyTubeStyle(current);
            }
        };
        group.setSize(width, height);
        TubeActor tube = new TubeActor(skin, game.tubes(), 0, ball);
        tube.setSize(width, height);
        tube.setHome(0f, 0f);
        tube.setTouchable(Touchable.disabled);
        group.addActor(tube);
        int[] colours = {2, 4, 1};
        for (int slot = 0; slot < colours.length; slot++) {
            BallActor b = new BallActor(skin, colours[slot], ball);
            b.setPosition(tube.ballX(), tube.ballY(slot));
            group.addActor(b);
        }
        return group;
    }

    private Texture swatch(String key, java.util.function.Supplier<Texture> make) {
        Texture texture = swatchTextures.get(key);
        if (texture == null) {
            texture = make.get();
            swatchTextures.put(key, texture);
        }
        return texture;
    }

    /**
     * Three tubes with balls, built from the current renderer state: the equipped items plus,
     * on top, whatever card is being previewed. Rebuilt on every change, never per frame.
     */
    private void rebuildPreview() {
        Cosmetics.apply(skin, game.tubes(), save);
        if (previewed != null) {
            Cosmetics.applyItem(skin, game.tubes(), previewed);
        }
        background.refresh(skin);

        preview.clearChildren();
        float tubeWidth = TubeActor.widthFor(PREVIEW_BALL);
        float tubeHeight = TubeActor.heightFor(PREVIEW_BALL);
        float gap = 22f;
        float x0 = (preview.getWidth() - (tubeWidth * 3f + gap * 2f)) * 0.5f;
        int[][] contents = {{0, 1, 2, 3}, {4, 5, 6}, {7, 8}};
        Group balls = new Group();
        for (int t = 0; t < 3; t++) {
            TubeActor tube = new TubeActor(skin, game.tubes(), t, PREVIEW_BALL);
            tube.setSize(tubeWidth, tubeHeight);
            tube.setHome(x0 + t * (tubeWidth + gap), 10f);
            tube.setTouchable(Touchable.disabled);
            preview.addActor(tube);
            for (int slot = 0; slot < contents[t].length; slot++) {
                BallActor ball = new BallActor(skin, contents[t][slot], PREVIEW_BALL, save.colorLabels);
                ball.setPosition(tube.ballX(), tube.ballY(slot));
                balls.addActor(ball);
            }
        }
        preview.addActor(balls);
        preview.setOrigin(Align.center);
        preview.setTransform(true);
        preview.setScale(0.96f);
        preview.addAction(Actions.scaleTo(1f, 1f, 0.22f, Interpolation.swingOut));

        if (previewed != null) {
            previewCaption.setText(T.f("Previewing  {0}", Names.name(previewed.displayName())));
            previewCaption.setColor(Palette.PRIMARY);
        } else {
            previewCaption.setText(T.f("Wearing  {0}  •  {1}  •  {2}  •  {3}",
                Names.name(Shop.equipped(save, Cosmetic.Category.BALL_PALETTE).displayName()),
                Names.name(Shop.equipped(save, Cosmetic.Category.BALL_FINISH).displayName()),
                Names.name(Shop.equipped(save, Cosmetic.Category.TUBE).displayName()),
                Names.name(Shop.equipped(save, Cosmetic.Category.THEME).displayName())));
            previewCaption.setColor(Palette.TEXT_DIM);
        }
    }

    // ----------------------------------------------------------------- boosters
    /** Extra tubes: one, a pack of five, a pack of twelve, and the daily video. */
    private void refreshBoosters() {
        sectionTitle.setText(T.r("Extra tubes"));
        sectionCount.setText(T.f("{0} in stock  •  {1} free per level", save.boosters,
            com.ben.laama.sortpuzzle.GameConfig.FREE_BOOSTERS_PER_LEVEL));
        grid.clearChildren();
        java.util.List<Actor> cards = new java.util.ArrayList<>();
        cards.add(freeCoinsCard()); // first: the free way to fill the purse
        for (com.ben.laama.sortpuzzle.logic.BoosterPacks pack
            : com.ben.laama.sortpuzzle.logic.BoosterPacks.values()) {
            cards.add(boosterCard(pack));
        }
        cards.add(rewardedTubeCard());
        // Undos and resets: a few are free every level; these top up the stock.
        cards.add(assistPackCard(com.ben.laama.sortpuzzle.logic.Assists.UNDO, "icon-undo"));
        cards.add(assistPackCard(com.ben.laama.sortpuzzle.logic.Assists.RESET, "icon-restart"));
        for (int i = 0; i < cards.size(); i++) {
            grid.add(enter(cards.get(i), i));
            if (i % 2 == 1) grid.row(); // two cards a row, always
        }
        rebuildPreview();
    }

    private Table assistPackCard(final com.ben.laama.sortpuzzle.logic.Assists assist, String icon) {
        Table card = new Table();
        card.setBackground(skin.newDrawable("card", Palette.SURFACE));
        card.pad(16f, 18f, 16f, 18f);
        card.defaults().left();
        Table glyphs = new Table();
        for (int k = 0; k < Math.min(assist.packSize(), 5); k++) {
            glyphs.add(new Image(skin.newDrawable(icon, Palette.TEXT))).size(k == 0 ? 40f : 30f).padRight(4f);
        }
        card.add(glyphs).height(92f).growX().row();
        Table title = new Table();
        String packLabel = assist.packLabel();
        addCardName(title, T.show(packLabel.substring(0, 1).toUpperCase(new java.util.Locale(T.language().code))
            + packLabel.substring(1)));
        title.add(badge(T.f("×{0} in stock", save.stockOf(assist)), Palette.SURFACE_STRONG, Palette.TEXT)).right();
        card.add(title).width(CARD_WIDTH - 36f).padTop(10f).row();
        Label description = new Label(assist == com.ben.laama.sortpuzzle.logic.Assists.UNDO
                ? T.f("{0} free every level. Extra ones for when a long move needs rethinking.", assist.freePerLevel())
                : T.f("{0} free every level. Extra ones for a fresh start without the wait.", assist.freePerLevel()),
            skin, "caption");
        description.setWrap(true);
        card.add(description).width(CARD_WIDTH - 36f).padTop(4f).row();
        Button buy = Ui.iconTextButton(skin, "icon-coin", T.f("BUY  {0}", assist.packPrice()), "gold", () -> {
            if (save.coins < assist.packPrice()) {
                game.sound().play(SoundManager.Cue.INVALID);
                Ui.toast(stage, skin, T.f("{0} more coins needed  •  clear levels to earn them",
                    assist.packPrice() - save.coins), Palette.TEXT_DIM);
                return;
            }
            if (save.buyAssistPack(assist)) {
                game.sound().play(SoundManager.Cue.COIN);
                game.analytics().event(AnalyticsEvents.SHOP_PURCHASE, AnalyticsEvents.P_ITEM,
                    "assist." + assist.id(), AnalyticsEvents.P_PRICE, assist.packPrice(),
                    AnalyticsEvents.P_COINS, save.coins);
                refresh();
                game.afterPurchase(() -> Ui.toast(stage, skin, T.f("+{0} in stock", assist.packLabel()),
                    Palette.SUCCESS));
            }
        });
        buy.getColor().a = save.coins >= assist.packPrice() ? 1f : 0.55f;
        card.add(buy).growX().height(54f).padTop(12f).row();
        // The free way, same as for tubes: a short video adds a few.
        Button watch = Ui.iconTextButton(skin, "icon-play", T.f("WATCH  •  +{0}", assist.videoReward()),
            "ghost", () -> watchForAssist(assist));
        card.add(watch).growX().height(50f).padTop(8f).row();
        return card;
    }

    private boolean assistAdInFlight;

    /** Rewarded video for undos or resets: one at a time, granted only on the SDK's callback. */
    private void watchForAssist(final com.ben.laama.sortpuzzle.logic.Assists assist) {
        if (assistAdInFlight) return;
        if (!game.ads().isRewardedReady()) {
            Ui.toast(stage, skin, T.r("Reward video isn't ready yet. Try again later."), Palette.GOLD);
            return;
        }
        assistAdInFlight = true;
        game.sound().play(SoundManager.Cue.BUTTON);
        game.pacing().onFullScreenAdShown();
        game.ads().showRewarded(new com.ben.laama.sortpuzzle.ads.AdProvider.RewardListener() {
            @Override
            public void onRewarded(int coins) {
                assistAdInFlight = false;
                save.addAssist(assist, assist.videoReward());
                if (game.getScreen() == ShopScreen.this) {
                    game.sound().play(SoundManager.Cue.TUBE);
                    refresh();
                    Ui.toast(stage, skin, T.f("+{0} in stock", assist.videoLabel()), Palette.SUCCESS);
                }
            }

            @Override
            public void onFailed() {
                assistAdInFlight = false;
                if (game.getScreen() == ShopScreen.this) {
                    Ui.toast(stage, skin, T.r("No reward this time. Try again later."), Palette.GOLD);
                }
            }
        });
    }

    private Actor enter(Actor card, int index) {
        card.getColor().a = 0f;
        card.addAction(Actions.sequence(
            Actions.delay(0.03f * index),
            Actions.parallel(
                Actions.fadeIn(0.22f, Interpolation.fade),
                Actions.sequence(Actions.moveBy(0f, -CARD_RISE),
                    Actions.moveBy(0f, CARD_RISE, 0.26f, Interpolation.pow2Out)))));
        return card;
    }

    private Table boosterCard(final com.ben.laama.sortpuzzle.logic.BoosterPacks pack) {
        Table card = new Table();
        card.setBackground(skin.newDrawable("card", Palette.SURFACE));
        card.pad(16f, 18f, 16f, 18f);
        card.defaults().left();

        // Visual: as many little tube glyphs as the pack holds (capped to a tidy row).
        Table glyphs = new Table();
        int shown = Math.min(pack.count(), 6);
        for (int k = 0; k < shown; k++) {
            Image tube = new Image(skin.newDrawable("icon-tube", Palette.PRIMARY));
            glyphs.add(tube).size(k == 0 ? 40f : 32f).padRight(2f);
        }
        if (pack.count() > shown) {
            Label more = new Label("+" + (pack.count() - shown), skin, "label");
            more.setColor(Palette.PRIMARY);
            glyphs.add(more).padLeft(6f);
        }
        card.add(glyphs).height(92f).growX().row();

        Table title = new Table();
        addCardName(title, T.r(pack.title()));
        if (pack.savingsPercent() > 0) {
            title.add(badge("-" + pack.savingsPercent() + "%", Palette.SUCCESS, Palette.ON_PRIMARY)).right();
        }
        card.add(title).width(CARD_WIDTH - 36f).padTop(10f).row();
        Label description = new Label(pack.count() == 1
            ? T.r("One extra tube to use whenever a level gets tight.")
            : T.f("{0} extra tubes, {1} coins each.", pack.count(), pack.price() / pack.count()),
            skin, "caption");
        description.setWrap(true);
        card.add(description).width(CARD_WIDTH - 36f).padTop(4f).row();

        Button buy = Ui.iconTextButton(skin, "icon-coin", T.f("BUY  {0}", pack.price()), "gold", () -> {
            if (!pack.canBuy(save)) {
                game.sound().play(SoundManager.Cue.INVALID);
                Ui.toast(stage, skin, T.f("{0} more coins needed  •  clear levels to earn them",
                    pack.price() - save.coins), Palette.TEXT_DIM);
                return;
            }
            if (pack.buy(save)) {
                game.sound().play(SoundManager.Cue.TUBE);
                game.analytics().event(AnalyticsEvents.SHOP_PURCHASE, AnalyticsEvents.P_ITEM,
                    "booster." + pack.count(), AnalyticsEvents.P_PRICE, pack.price(),
                    AnalyticsEvents.P_COINS, save.coins);
                refresh();
                game.afterPurchase(() -> Ui.toast(stage, skin, T.f("+{0} in stock",
                    com.ben.laama.sortpuzzle.logic.Assists.TUBE.countLabel(pack.count())), Palette.SUCCESS));
            }
        });
        buy.getColor().a = pack.canBuy(save) ? 1f : 0.55f;
        card.add(buy).growX().height(54f).padTop(12f).row();
        return card;
    }

    /** Local ISO date: the "free coins" cap resets at the player's midnight. */
    private static String todayKey() {
        return java.time.LocalDate.now().toString();
    }

    /**
     * Free coins for a video, a few times a day: keeps the long cosmetic economy moving for
     * players who will not pay, and is the shop's main rewarded placement.
     */
    private Table freeCoinsCard() {
        Table card = new Table();
        card.setBackground(skin.newDrawable("card", Palette.SURFACE));
        card.pad(16f, 18f, 16f, 18f);
        card.defaults().left();
        Table glyphs = new Table();
        glyphs.add(new Image(skin.newDrawable("icon-play", Palette.GOLD))).size(40f).padRight(8f);
        glyphs.add(new Image(skin.newDrawable("icon-coin", Palette.GOLD))).size(40f);
        card.add(glyphs).height(92f).growX().row();
        Table title = new Table();
        addCardName(title, T.r("Free coins"));
        int left = save.adCoinsLeft(todayKey());
        title.add(badge(T.f("{0} / {1} today", left, com.ben.laama.sortpuzzle.GameConfig.AD_COINS_PER_DAY),
            Palette.SURFACE_STRONG, Palette.TEXT)).right();
        card.add(title).width(CARD_WIDTH - 36f).padTop(10f).row();
        Label description = new Label(T.f("Watch a short video, get {0} coins. Always optional.",
            com.ben.laama.sortpuzzle.GameConfig.AD_COINS_REWARD), skin, "caption");
        description.setWrap(true);
        card.add(description).width(CARD_WIDTH - 36f).padTop(4f).row();
        final TextButton watch = Ui.button(skin, left > 0
            ? T.f("WATCH  •  +{0} COINS", com.ben.laama.sortpuzzle.GameConfig.AD_COINS_REWARD)
            : T.r("BACK TOMORROW"), left > 0 ? "gold" : "secondary", () -> { });
        watch.setDisabled(left <= 0);
        Ui.click(watch, () -> watchForCoins(watch));
        card.add(watch).growX().height(54f).padTop(12f).row();
        return card;
    }

    private void watchForCoins(TextButton watch) {
        if (watch.isDisabled() || save.adCoinsLeft(todayKey()) <= 0) {
            return;
        }
        if (!game.ads().isRewardedReady()) {
            Ui.toast(stage, skin, T.r("Reward video isn't ready yet. Try again later."), Palette.GOLD);
            return;
        }
        watch.setDisabled(true);
        watch.setText(T.r("VIDEO IN PROGRESS..."));
        game.sound().play(SoundManager.Cue.BUTTON);
        game.pacing().onFullScreenAdShown();
        game.ads().showRewarded(new com.ben.laama.sortpuzzle.ads.AdProvider.RewardListener() {
            @Override
            public void onRewarded(int coins) {
                // Credited here only, once per confirmed reward, within today's cap.
                boolean granted = save.claimAdCoins(todayKey());
                if (granted) {
                    game.analytics().event(
                        com.ben.laama.sortpuzzle.services.AnalyticsEvents.REWARDED_FREE_COINS,
                        com.ben.laama.sortpuzzle.services.AnalyticsEvents.P_COINS, save.coins);
                }
                if (game.getScreen() == ShopScreen.this) {
                    if (granted) {
                        game.sound().play(SoundManager.Cue.COIN);
                        Ui.toast(stage, skin, T.f("+{0} coins added",
                            com.ben.laama.sortpuzzle.GameConfig.AD_COINS_REWARD), Palette.SUCCESS);
                    }
                    refresh();
                }
            }

            @Override
            public void onFailed() {
                if (game.getScreen() == ShopScreen.this) {
                    refresh();
                    Ui.toast(stage, skin, T.r("No reward this time. Try again later."), Palette.GOLD);
                }
            }
        });
    }

    /** The free way: one tube for a video, at most once every 90 seconds. */
    private Table rewardedTubeCard() {
        Table card = new Table();
        card.setBackground(skin.newDrawable("card", Palette.SURFACE));
        card.pad(16f, 18f, 16f, 18f);
        card.defaults().left();
        Table glyphs = new Table();
        glyphs.add(new Image(skin.newDrawable("icon-play", Palette.GOLD))).size(40f).padRight(8f);
        glyphs.add(new Image(skin.newDrawable("icon-tube", Palette.PRIMARY))).size(40f);
        card.add(glyphs).height(92f).growX().row();
        card.add(new Label(T.r("Free tube"), skin, "label")).padTop(10f).row();
        Label description = new Label(T.r("Watch a short video, get one extra tube. Always optional."),
            skin, "caption");
        description.setWrap(true);
        card.add(description).width(CARD_WIDTH - 36f).padTop(4f).row();
        rewardedTubeButton = Ui.button(skin, "", "ghost", this::watchForExtraTube);
        card.add(rewardedTubeButton).growX().height(54f).padTop(12f).row();
        refreshRewardedTubeButton();
        return card;
    }

    private void watchForExtraTube() {
        long now = System.currentTimeMillis();
        if (save.rewardedTubeCooldownRemaining(now) > 0L || rewardedTubeAdInFlight) {
            refreshRewardedTubeButton();
            return;
        }
        if (!game.ads().isRewardedReady()) {
            Ui.toast(stage, skin, T.r("Reward video isn't ready yet. Try again later."), Palette.GOLD);
            return;
        }
        rewardedTubeAdInFlight = true;
        refreshRewardedTubeButton();
        game.sound().play(SoundManager.Cue.BUTTON);
        game.pacing().onFullScreenAdShown();
        game.ads().showRewarded(new com.ben.laama.sortpuzzle.ads.AdProvider.RewardListener() {
            @Override
            public void onRewarded(int coins) {
                boolean granted = save.claimRewardedTube(System.currentTimeMillis());
                rewardedTubeAdInFlight = false;
                if (game.getScreen() == ShopScreen.this) {
                    if (granted) {
                        game.sound().play(SoundManager.Cue.TUBE);
                        Ui.toast(stage, skin, T.r("One extra tube added to your stock"), Palette.SUCCESS);
                    }
                    refresh();
                }
            }

            @Override
            public void onFailed() {
                rewardedTubeAdInFlight = false;
                if (game.getScreen() == ShopScreen.this) {
                    refreshRewardedTubeButton();
                    Ui.toast(stage, skin, T.r("No reward this time. Try again later."), Palette.GOLD);
                }
            }
        });
    }

    private void refreshRewardedTubeButton() {
        if (rewardedTubeButton == null) return;
        if (rewardedTubeAdInFlight) {
            rewardedTubeButton.setText(T.r("VIDEO IN PROGRESS..."));
            rewardedTubeButton.setDisabled(true);
            return;
        }
        long remaining = save.rewardedTubeCooldownRemaining(System.currentTimeMillis());
        if (remaining > 0L) {
            rewardedTubeButton.setText(T.f("NEXT FREE TUBE IN {0} s", (remaining + 999L) / 1000L));
            rewardedTubeButton.setDisabled(true);
        } else {
            rewardedTubeButton.setText(T.r("WATCH VIDEO  •  +1 TUBE"));
            rewardedTubeButton.setDisabled(false);
        }
    }

    @Override
    public void render(float delta) {
        if (boostersTab) {
            rewardedTubeRefreshTimer += delta;
            if (rewardedTubeRefreshTimer >= 1f) {
                rewardedTubeRefreshTimer = 0f;
                refreshRewardedTubeButton();
            }
        }
        super.render(delta);
    }

    // ------------------------------------------------------------------ actions
    private void equip(Cosmetic item) {
        if (Shop.equip(save, item)) {
            game.sound().play(SoundManager.Cue.DROP);
            game.analytics().event(AnalyticsEvents.SHOP_EQUIP, AnalyticsEvents.P_ITEM, item.id());
            previewed = null;
            refresh();
            Ui.toast(stage, skin, T.f("{0} is now in use", Names.name(item.displayName())), Palette.SUCCESS);
        }
    }

    private void confirmPurchase(final Cosmetic item) {
        if (!Shop.canBuy(save, item)) {
            game.sound().play(SoundManager.Cue.INVALID);
            int missing = item.price() - save.coins;
            Ui.toast(stage, skin, T.f("{0} more coins needed  •  clear levels to earn them", missing),
                Palette.TEXT_DIM);
            return;
        }
        previewed = item;
        refresh();
        Dialog dialog = Ui.dialog(skin, T.f("Buy {0}?", Names.name(item.displayName())), choice -> {
            if ("buy".equals(choice)) {
                buy(item);
            }
        });
        dialog.getContentTable().add(Ui.paragraph(skin,
            T.f("{0} coins.  You have {1}, leaving {2}.", item.price(), save.coins,
                save.coins - item.price()), "dim", 400f)).width(400f).padTop(6f).row();
        dialog.button(Ui.button(skin, T.r("NOT NOW"), "ghost", () -> { }), "no");
        dialog.button(Ui.button(skin, T.f("BUY  {0}", item.price()), "gold", () -> { }), "buy");
        dialog.show(stage);
    }

    private void buy(Cosmetic item) {
        Shop.PurchaseResult result = Shop.buy(save, item);
        if (result != Shop.PurchaseResult.BOUGHT) {
            game.sound().play(SoundManager.Cue.INVALID);
            refresh();
            return;
        }
        game.sound().play(SoundManager.Cue.COIN);
        game.analytics().event(AnalyticsEvents.SHOP_PURCHASE, AnalyticsEvents.P_ITEM, item.id(),
            AnalyticsEvents.P_PRICE, item.price(), AnalyticsEvents.P_COINS, save.coins);
        previewed = null;
        refresh();
        game.afterPurchase(() -> Ui.toast(stage, skin,
            T.f("{0} is yours and in use", Names.name(item.displayName())), Palette.SUCCESS));
    }

    private void goBack() {
        game.sound().play(SoundManager.Cue.BUTTON);
        game.goHome();
    }

    @Override
    protected void onBackPressed() {
        goBack();
    }

    /** Scrolls the catalogue grid (0 top .. 1 bottom); used by the design-review captures. */
    public void scrollCatalogueTo(float percent) {
        if (gridScroll == null) return;
        gridScroll.layout();
        gridScroll.setScrollPercentY(percent);
        gridScroll.updateVisualScroll();
    }

    @Override
    public void hide() {
        // The next screen puts the renderer back to its own look in show(), whatever was
        // being previewed here.
        super.hide();
    }

    @Override
    protected void applyLook() {
        super.applyLook();
        if (previewed != null) {
            Cosmetics.applyItem(skin, game.tubes(), previewed);
            background.refresh(skin);
        }
    }

    @Override
    public void dispose() {
        for (Texture texture : swatchTextures.values()) {
            texture.dispose();
        }
        swatchTextures.clear();
        super.dispose();
    }
}
