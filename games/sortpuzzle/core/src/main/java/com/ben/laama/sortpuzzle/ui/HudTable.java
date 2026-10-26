package com.ben.laama.sortpuzzle.ui;

import com.badlogic.gdx.graphics.Color;
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
    }

    private final Label levelLabel;
    private final Label bandLabel;
    private final Label movesLabel;
    private final Label statusLabel;
    private final Label coinsLabel;
    private final Table statusPill;
    private final ImageButton undoButton;
    private final ImageButton boosterButton;
    private final ImageButton hintButton;
    private final Label boosterCaption;
    private final Label hintCaption;

    public HudTable(Skin skin, Listener listener) {
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

        Table counters = new Table();
        counters.add(levelLabel).row();
        counters.add(bandLabel).padTop(-2f).row();
        counters.add(movesLabel).padTop(2f);

        coinsLabel = new Label("0", skin, "gold");
        Table purse = Ui.chip(skin, "icon-coin", Palette.GOLD, coinsLabel);

        Table top = new Table();
        top.pad(20f, 22f, 6f, 22f);
        top.add(back).left().size(Ui.ICON_BUTTON).width(96f).left();
        top.add(counters).expandX();
        top.add(purse).right().width(96f).right();

        add(top).growX().row();

        // --------------------------------------------------------------- status
        statusLabel = new Label("", skin, "caption");
        statusPill = new Table();
        statusPill.setBackground(skin.newDrawable("pill", Color.valueOf("FFFFFF0E")));
        statusPill.pad(6f, 16f, 6f, 16f);
        statusPill.add(statusLabel);
        add(statusPill).padTop(4f).row();

        // ------------------------------------------------------------- bottom bar
        undoButton = Ui.iconButton(skin, "icon-undo", listener::onUndo);
        ImageButton restart = Ui.iconButton(skin, "icon-restart", listener::onRestart);
        boosterButton = Ui.iconButton(skin, "icon-tube", Color.valueOf("FFC85728"), Palette.GOLD,
            Ui.ICON_BUTTON, listener::onBooster);
        hintButton = Ui.iconButton(skin, "icon-hint", Palette.PRIMARY, Palette.ON_PRIMARY,
            Ui.ICON_BUTTON, listener::onHint);
        boosterCaption = new Label("", skin, "caption");
        hintCaption = new Label("", skin, "caption");

        Table bottom = new Table();
        bottom.pad(10f, 16f, 0f, 16f).defaults().pad(0f, 14f, 0f, 14f).uniformX();
        bottom.add(verb(skin, undoButton, new Label("Undo", skin, "caption")));
        bottom.add(verb(skin, restart, new Label("Reset", skin, "caption")));
        bottom.add(verb(skin, boosterButton, boosterCaption));
        bottom.add(verb(skin, hintButton, hintCaption));

        add().expandY().row();
        add(bottom).padBottom(26f).row();
    }

    private static Table verb(Skin skin, ImageButton button, Label caption) {
        Table stack = new Table();
        stack.add(button).size(Ui.ICON_BUTTON).row();
        stack.add(caption).padTop(6f);
        return stack;
    }

    // --------------------------------------------------------------------- header
    public void setLevel(int level, String band) {
        levelLabel.setText("Level " + level);
        bandLabel.setText(band);
    }

    /**
     * @param used  moves played so far
     * @param limit the move limit, or {@code 0} while par is still being computed
     */
    public void setMoves(int used, int limit) {
        if (limit <= 0) {
            movesLabel.setText(used == 1 ? "1 move" : used + " moves");
            movesLabel.setColor(Palette.TEXT_DIM);
            return;
        }
        movesLabel.setText(used + " / " + limit + " moves");
        int left = limit - used;
        // Go amber with a few moves to spare, red on the last one — the player should see the
        // wall coming rather than hit it.
        movesLabel.setColor(left <= 1 ? Palette.DANGER
            : left <= 4 ? Palette.GOLD : Palette.TEXT_DIM);
    }

    public void setCoins(int coins) {
        coinsLabel.setText(String.valueOf(coins));
    }

    // ---------------------------------------------------------------- status line
    /** @param par {@link Level#PAR_UNKNOWN} while the solver is still thinking */
    public void showPar(int par) {
        statusLabel.setColor(Palette.TEXT_DIM);
        statusLabel.setText(par == Level.PAR_UNKNOWN
            ? "Best possible: solving…" : "Best possible: " + par + " moves");
    }

    /**
     * The assist indicator.
     *
     * @param remaining moves still needed to finish from the current position
     * @param exact     {@code false} when the solver could only find a route, not the shortest
     *                  one, in which case the number is an upper bound and says so
     */
    public void showRemaining(int remaining, boolean exact) {
        statusLabel.setColor(Palette.PRIMARY);
        statusLabel.setText(remaining + (exact ? " moves left" : " moves left (at most)"));
    }

    public void showStatus(String message, Color color) {
        statusLabel.setColor(color);
        statusLabel.setText(message);
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
