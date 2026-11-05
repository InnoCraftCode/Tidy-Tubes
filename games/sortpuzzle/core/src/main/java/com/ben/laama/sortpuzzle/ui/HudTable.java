package com.ben.laama.sortpuzzle.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;

import com.ben.laama.sortpuzzle.logic.Level;

/**
 * The chrome around the board: back, level badge and purse along the top, one status pill
 * under them, and four round verbs along the bottom with a caption each.
 *
 * <p>Owns no game state — it renders what it is told and reports taps back through
 * {@link Listener}. In particular it does not know what a hint costs or whether the player has
 * any left; it just draws the caption it is handed.</p>
 */
public final class HudTable extends Table {

    /** Taps the screen has to react to. */
    public interface Listener {
        void onBack();

        void onUndo();

        void onRestart();

        void onBooster();

        void onHint();

        /** The small speaker button under the purse: sound effects on/off. */
        void onToggleEffects();

        /** The small note button under the purse: music on/off. */
        void onToggleMusic();
    }

    /** Diameter of the two audio buttons: smaller than the verbs, they are secondary. */
    private static final float AUDIO_BUTTON = 56f;

    /** Landscape: world units the top bar reserves above the board. */
    public static final float LANDSCAPE_TOP = 150f;
    /** Landscape: width of each side column (status + stock on the left, verbs on the right). */
    public static final float LANDSCAPE_SIDE = 360f;
    /** Portrait: units reserved above and below the board. */
    public static final float PORTRAIT_TOP = 220f;
    public static final float PORTRAIT_BOTTOM = 150f;

    private final Table topBar;
    private final Table stock;
    private final Table[] verbs;
    private Boolean landscape;

    private final Label levelLabel;
    private final Label bandLabel;
    private final Label movesLabel;
    private final Label statusLabel;
    private final Label statusValue;
    private final Image statusIcon;
    private final Cell<Label> statusValueCell;
    private boolean parShown;
    private final Label coinsLabel;
    private final Table purse;
    private final Container<Label> movesBox;
    private final Table statusPill;
    private int shownCoins = Integer.MIN_VALUE;
    private int shownMoves = -1;
    private final ImageButton undoButton;
    private final ImageButton resetButton;
    private final ImageButton boosterButton;
    private final ImageButton hintButton;
    private final Label undoCaption;
    private final Label resetCaption;
    private final Label boosterCaption;
    private final Label hintCaption;
    /** The stock strip under the status pill: tubes, undos, resets left. */
    private final Label tubeStock;
    private final Label undoStock;
    private final Label resetStock;
    private final Table tubeChip;
    private final Table undoChip;
    private final Table resetChip;
    private int shownTubes = -1;
    private int shownUndos = -1;
    private int shownResets = -1;
    private final Skin skin;
    private final ImageButton effectsButton;
    private final ImageButton musicButton;

    public HudTable(Skin skin, Listener listener) {
        this.skin = skin;
        setFillParent(true);
        top();

        // ---------------------------------------------------------------- top bar
        ImageButton back = Ui.iconButton(skin, "icon-back", listener::onBack);

        levelLabel = new Label("", skin, "heading");
        bandLabel = new Label("", skin, "caption");
        movesLabel = new Label("", skin, "dim");
        levelLabel.setAlignment(Align.center);
        bandLabel.setAlignment(Align.center);
        movesLabel.setAlignment(Align.center);

        // Counters sit in transformable boxes so they can pop when their value changes.
        movesBox = new Container<>(movesLabel);
        movesBox.setTransform(true);

        Table counters = new Table();
        counters.add(levelLabel).row();
        counters.add(bandLabel).padTop(-2f).row();
        counters.add(movesBox).padTop(2f);

        coinsLabel = new Label("0", skin, "gold");
        purse = Ui.chip(skin, "icon-coin", Palette.GOLD, coinsLabel);
        purse.setTransform(true);

        // Effects and music switches sit under the purse: reachable mid-level without opening
        // Settings, and small enough not to compete with the board.
        effectsButton = Ui.iconButton(skin, "icon-sound", Color.valueOf("FFFFFF18"), Palette.TEXT,
            AUDIO_BUTTON, listener::onToggleEffects);
        musicButton = Ui.iconButton(skin, "icon-music", Color.valueOf("FFFFFF18"), Palette.TEXT,
            AUDIO_BUTTON, listener::onToggleMusic);
        Table audio = new Table();
        audio.add(effectsButton).size(AUDIO_BUTTON).padRight(10f);
        audio.add(musicButton).size(AUDIO_BUTTON);
        Table right = new Table();
        right.add(purse).right().row();
        right.add(audio).right().padTop(10f);

        topBar = new Table();
        topBar.pad(20f, 22f, 6f, 22f);
        // Same width as the right column (purse + audio) so the level title stays centred.
        topBar.add(back).left().top().size(Ui.ICON_BUTTON).width(96f).padRight(26f).left();
        topBar.add(counters).expandX().top();
        topBar.add(right).right().top().width(122f);

        // --------------------------------------------------------------- status
        // Icon + label + accent value on a pill tinted with the accent colour, e.g.
        // [star] Best possible  9 moves  - gold for the target, mint for the assist, etc.
        statusIcon = new Image(skin.newDrawable("icon-star", Palette.GOLD));
        statusLabel = new Label("", skin, "caption");
        statusValue = new Label("", skin, "label");
        statusPill = new Table();
        statusPill.setTransform(true); // so it can pop when the target arrives
        statusPill.pad(8f, 16f, 8f, 20f);
        statusPill.add(statusIcon).size(24f).padRight(10f);
        statusPill.add(statusLabel);
        statusValueCell = statusPill.add(statusValue).padLeft(10f);

        // ----------------------------------------------------------- stock strip
        // What the player has left to spend this level: tubes, undos, resets. Each chip pops
        // when its count changes, so a use is seen being taken.
        tubeStock = new Label("", skin, "label");
        undoStock = new Label("", skin, "label");
        resetStock = new Label("", skin, "label");
        tubeChip = Ui.chip(skin, "icon-tube", Palette.GOLD, tubeStock);
        undoChip = Ui.chip(skin, "icon-undo", Palette.TEXT, undoStock);
        resetChip = Ui.chip(skin, "icon-restart", Palette.TEXT, resetStock);
        for (Table chip : new Table[] {tubeChip, undoChip, resetChip}) {
            chip.setTransform(true);
            chip.pad(6f, 12f, 6f, 14f);
        }
        stock = new Table();
        stock.add(tubeChip).padRight(8f);
        stock.add(undoChip).padRight(8f);
        stock.add(resetChip);

        // ------------------------------------------------------------- bottom bar
        undoButton = Ui.iconButton(skin, "icon-undo", listener::onUndo);
        resetButton = Ui.iconButton(skin, "icon-restart", listener::onRestart);
        boosterButton = Ui.iconButton(skin, "icon-tube", Color.valueOf("FFC85728"), Palette.GOLD,
            Ui.ICON_BUTTON, listener::onBooster);
        hintButton = Ui.iconButton(skin, "icon-hint", Palette.PRIMARY, Palette.ON_PRIMARY,
            Ui.ICON_BUTTON, listener::onHint);
        undoCaption = new Label("Undo", skin, "caption");
        resetCaption = new Label("Reset", skin, "caption");
        boosterCaption = new Label("", skin, "caption");
        hintCaption = new Label("", skin, "caption");

        verbs = new Table[] {
            verb(skin, undoButton, undoCaption),
            verb(skin, resetButton, resetCaption),
            verb(skin, boosterButton, boosterCaption),
            verb(skin, hintButton, hintCaption)};

        setLandscape(false);
    }

    /**
     * Arranges the chrome for the orientation. Portrait: everything stacked above the board and
     * the four verbs in a row below it. Landscape: a slim top bar, status and stock in a left
     * column, the verbs as a 2x2 pad in a right column (thumb reach), the board in between.
     * The widgets are the same instances either way, so no state is lost on a rotation.
     */
    public void setLandscape(boolean wide) {
        if (landscape != null && landscape == wide) {
            return;
        }
        landscape = wide;
        clearChildren();
        add(topBar).growX().row();
        if (!wide) {
            add(statusPill).padTop(4f).row();
            add(stock).padTop(8f).row();
            Table bottom = new Table();
            bottom.pad(10f, 16f, 0f, 16f).defaults().pad(0f, 14f, 0f, 14f).uniformX();
            for (Table verb : verbs) {
                bottom.add(verb);
            }
            add().expandY().row();
            add(bottom).padBottom(26f).row();
            return;
        }
        Table left = new Table();
        left.add(statusPill).row();
        left.add(stock).padTop(12f).row();
        Table pad = new Table();
        pad.defaults().pad(12f, 14f, 12f, 14f).uniform();
        pad.add(verbs[0]);
        pad.add(verbs[1]).row();
        pad.add(verbs[2]);
        pad.add(verbs[3]);
        Table middle = new Table();
        middle.add(left).width(LANDSCAPE_SIDE).expandY().center();
        middle.add().expand();
        middle.add(pad).width(LANDSCAPE_SIDE).expandY().center();
        add(middle).grow().padBottom(20f).row();
    }

    private static Table verb(Skin skin, ImageButton button, Label caption) {
        Table stack = new Table();
        stack.add(button).size(Ui.ICON_BUTTON).row();
        stack.add(caption).padTop(6f);
        return stack;
    }

    // ---------------------------------------------------------------------- audio
    /** Shows each switch's state: crossed-out, dimmed glyph when that channel is off. */
    public void setAudio(boolean effectsOn, boolean musicOn) {
        setGlyph(effectsButton, effectsOn ? "icon-sound" : "icon-sound-off", effectsOn);
        setGlyph(musicButton, musicOn ? "icon-music" : "icon-music-off", musicOn);
    }

    private void setGlyph(ImageButton button, String icon, boolean on) {
        button.getStyle().imageUp = skin.newDrawable(icon, on ? Palette.TEXT : Palette.TEXT_DIM);
    }

    // ---------------------------------------------------------------------- stock
    /**
     * Updates the stock strip and the verb captions. A chip pops when its count moves, and
     * reads dim at zero so "none left" is visible before the tap.
     */
    public void setStock(int tubes, int undos, int resets) {
        stockChip(tubeChip, tubeStock, tubes, shownTubes);
        stockChip(undoChip, undoStock, undos, shownUndos);
        stockChip(resetChip, resetStock, resets, shownResets);
        shownTubes = tubes;
        shownUndos = undos;
        shownResets = resets;
        boosterCaption.setText(tubes > 0 ? "Tube ×" + tubes : "Tube");
        undoCaption.setText(undos > 0 ? "Undo ×" + undos : "Undo");
        resetCaption.setText(resets > 0 ? "Reset ×" + resets : "Reset");
    }

    private static void stockChip(Table chip, Label label, int count, int shown) {
        label.setText("×" + count);
        label.setColor(count > 0 ? Palette.TEXT : Palette.TEXT_DISABLED);
        chip.getColor().a = count > 0 ? 1f : 0.55f;
        if (shown >= 0 && count != shown) {
            pop(chip, count < shown ? 0.88f : 1.2f);
        }
    }

    // --------------------------------------------------------------------- header
    public void setLevel(int level, String band) {
        setLevel("Level " + level, band);
    }

    public void setLevel(String title, String band) {
        levelLabel.setText(title);
        bandLabel.setText(band);
    }

    /**
     * @param used  moves played so far
     * @param limit the move limit, or {@code 0} while par is still being computed
     */
    public void setMoves(int used, int limit) {
        // A tick per move played; a firmer one when the counter turns red (last move).
        boolean played = shownMoves >= 0 && used > shownMoves;
        shownMoves = used;
        if (limit <= 0) {
            movesLabel.setText(used == 1 ? "1 move" : used + " moves");
            movesLabel.setColor(Palette.TEXT_DIM);
            if (played) {
                pop(movesBox, 1.12f);
            }
            return;
        }
        movesLabel.setText(used + " / " + limit + " moves");
        int left = limit - used;
        // Go amber with a few moves to spare, red on the last one — the player should see the
        // wall coming rather than hit it.
        movesLabel.setColor(left <= 1 ? Palette.DANGER
            : left <= 4 ? Palette.GOLD : Palette.TEXT_DIM);
        if (played) {
            pop(movesBox, left <= 1 ? 1.3f : 1.12f);
        }
    }

    public void setCoins(int coins) {
        boolean changed = shownCoins != Integer.MIN_VALUE && coins != shownCoins;
        boolean gained = coins > shownCoins;
        shownCoins = coins;
        coinsLabel.setText(String.valueOf(coins));
        if (changed) {
            pop(purse, gained ? 1.22f : 0.9f); // swell on a gain, a small dip on a spend
        }
    }

    /** @return {@code out} set to the stage-space centre of the coin purse (where flying coins land) */
    public Vector2 purseCenter(Vector2 out) {
        out.set(purse.getWidth() * 0.5f, purse.getHeight() * 0.5f);
        return purse.localToStageCoordinates(out);
    }

    /** Scale pop that always returns to exactly 1: the HUD's one unit of "something changed". */
    private static void pop(Actor target, float peak) {
        target.setOrigin(Align.center); // sizes are final by now; origin must follow them
        target.clearActions();
        target.setScale(1f);
        target.addAction(Actions.sequence(
            Actions.scaleTo(peak, peak, 0.08f, Interpolation.pow2Out),
            Actions.scaleTo(1f, 1f, 0.18f, Interpolation.swingOut)));
    }

    // ---------------------------------------------------------------- status line
    /** @param par {@link Level#PAR_UNKNOWN} while the solver is still thinking */
    public void showPar(int par) {
        if (par == Level.PAR_UNKNOWN) {
            setStatus("icon-star", Palette.TEXT_DIM, "Best possible", "solving…");
            parShown = false;
            return;
        }
        setStatus("icon-star", Palette.GOLD, "Best possible", par == 1 ? "1 move" : par + " moves");
        if (!parShown) {
            parShown = true;
            // The text just changed width; size the pill now so the pop scales from its centre.
            statusPill.setSize(statusPill.getPrefWidth(), statusPill.getPrefHeight());
            pop(statusPill, 1.08f); // the target just arrived: draw the eye to it once
        }
    }

    /** Restyles the status pill: accent-tinted background, accent icon and value. */
    private void setStatus(String icon, Color accent, String text, String value) {
        Color tint = new Color(accent);
        tint.a = 0.16f;
        statusPill.setBackground(skin.newDrawable("pill", tint));
        statusIcon.setDrawable(skin.newDrawable(icon, accent));
        statusLabel.setColor(Palette.TEXT);
        statusLabel.setText(text);
        statusValue.setColor(accent);
        statusValue.setText(value);
        statusValueCell.padLeft(value.isEmpty() ? 0f : 10f);
        statusPill.invalidateHierarchy();
    }

    /**
     * The assist indicator.
     *
     * @param remaining moves still needed to finish from the current position
     * @param exact     {@code false} when the solver could only find a route, not the shortest
     *                  one, in which case the number is an upper bound and says so
     */
    public void showRemaining(int remaining, boolean exact) {
        String moves = remaining == 1 ? "1 move" : remaining + " moves";
        setStatus("icon-hint", Palette.PRIMARY, exact ? "To finish" : "To finish, at most", moves);
    }

    public void showStatus(String message, Color color) {
        setStatus("icon-info", color, message, "");
        statusLabel.setColor(color);
    }

    // -------------------------------------------------------------------- buttons
    public void setUndoEnabled(boolean enabled) {
        undoButton.setDisabled(!enabled);
        undoButton.getColor().a = enabled ? 1f : 0.45f;
    }

    /** @param available how many extra-tube boosters the player can still spend here */
    public void setBoosters(int available) {
        boosterCaption.setText("Tube ×" + available);
        boosterButton.setDisabled(available <= 0);
        boosterButton.getColor().a = available > 0 ? 1f : 0.45f;
    }

    /** @param caption what sits under the hint button — the screen owns that policy, not the HUD */
    public void setHintCaption(String caption) {
        hintCaption.setText(caption);
    }

    public void setHintEnabled(boolean enabled) {
        hintButton.setDisabled(!enabled);
        hintButton.getColor().a = enabled ? 1f : 0.45f;
    }
}
