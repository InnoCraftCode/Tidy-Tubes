package com.ben.laama.sortpuzzle.screens;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;

import com.ben.laama.sortpuzzle.Main;
import com.ben.laama.sortpuzzle.audio.SoundManager;
import com.ben.laama.sortpuzzle.logic.Difficulty;
import com.ben.laama.sortpuzzle.save.SaveData;
import com.ben.laama.sortpuzzle.ui.Palette;
import com.ben.laama.sortpuzzle.ui.Ui;

/**
 * Scrolling grid of levels, in chapters of ten.
 *
 * <p>There is no fixed level count — the generator will produce level 10 000 quite happily — so
 * the grid shows everything unlocked plus a run of locked levels ahead, which is enough to feel
 * like a map without pretending the game ends somewhere.</p>
 */
public final class LevelSelectScreen extends BaseScreen {

    private static final int COLUMNS = 5;
    private static final int CHAPTER = 10;
    /** How many locked levels to show past the furthest one unlocked. */
    private static final int LOOKAHEAD = 30;
    private static final int MINIMUM_SHOWN = 50;
    private static final float TILE = 112f;
    private static final float TILE_PAD = 6f;

    private final ScrollPane scroll;
    private final Table grid;
    private Actor currentTile;

    public LevelSelectScreen(Main game) {
        super(game);
        SaveData save = game.save();

        int lastLevel = Math.max(MINIMUM_SHOWN, save.highestUnlocked + LOOKAHEAD);
        lastLevel = ((lastLevel + CHAPTER - 1) / CHAPTER) * CHAPTER;

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // -------------------------------------------------------------- header
        Table header = new Table();
        header.pad(24f, 26f, 8f, 26f);
        header.add(Ui.iconButton(skin, "icon-back", game::goHome)).size(Ui.ICON_BUTTON).left();
        Table titles = new Table();
        titles.add(new Label("Levels", skin, "title")).row();
        titles.add(new Label(save.completedCount() + " cleared  •  "
            + Difficulty.colorsFor(save.highestUnlocked) + " colours now", skin, "caption")).padTop(-2f);
        header.add(titles).expandX();
        header.add().size(Ui.ICON_BUTTON);
        root.add(header).growX().row();

        // ---------------------------------------------------------------- grid
        grid = new Table();
        grid.pad(6f, 10f, 30f, 10f);
        for (int level = 1; level <= lastLevel; level++) {
            if (level % CHAPTER == 1) {
                int last = level + CHAPTER - 1;
                String colours = Difficulty.colorsFor(level) == Difficulty.colorsFor(last)
                    ? Difficulty.colorsFor(level) + " colours"
                    : Difficulty.colorsFor(level) + "–" + Difficulty.colorsFor(last) + " colours";
                Label chapter = new Label(level + "–" + last + "   •   " + colours
                    + "   •   " + Difficulty.bandName(level), skin, "caption");
                chapter.setAlignment(Align.left);
                grid.add(chapter).colspan(COLUMNS).left().padLeft(10f)
                    .padTop(level == 1 ? 6f : 26f).padBottom(8f).row();
            }
            Actor tile = tile(level);
            if (level == save.currentLevel) {
                currentTile = tile;
            }
            grid.add(tile).size(TILE, TILE).pad(TILE_PAD);
            if (level % COLUMNS == 0) {
                grid.row();
            }
        }

        scroll = new ScrollPane(grid, skin);
        scroll.setFadeScrollBars(true);
        scroll.setScrollingDisabled(true, false);
        scroll.setOverscroll(false, true);
        root.add(scroll).grow().row();
    }

    private Actor tile(int level) {
        SaveData save = game.save();
        boolean unlocked = save.isUnlocked(level);
        boolean completed = save.isCompleted(level);
        boolean current = level == save.currentLevel;

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
            content.add(lock).size(28f).row();
            Label number = new Label(String.valueOf(level), skin, "caption");
            content.add(number).padTop(2f);
        } else {
            Label number = new Label(String.valueOf(level),
                new Label.LabelStyle(skin.getFont("heading"), textStyle.fontColor));
            content.add(number).row();
            String caption;
            if (completed) {
                caption = save.bestMovesFor(level) + " mv";
            } else if (current) {
                caption = "next";
            } else {
                caption = Difficulty.colorsFor(level) + " col";
            }
            Label sub = new Label(caption,
                new Label.LabelStyle(skin.getFont("small"), textStyle.fontColor));
            sub.getColor().a = 0.75f;
            content.add(sub).padTop(-4f);
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
        // Open on the level the player is actually up to, not at level 1.
        stage.act(0f);
        scroll.validate();
        grid.validate();
        if (currentTile != null) {
            float fromTop = grid.getHeight() - currentTile.getY() - currentTile.getHeight();
            scroll.setScrollY(Math.max(0f, fromTop - scroll.getHeight() * 0.35f));
            scroll.updateVisualScroll();
        }
    }
}
