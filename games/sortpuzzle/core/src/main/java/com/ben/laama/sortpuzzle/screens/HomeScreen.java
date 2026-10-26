package com.ben.laama.sortpuzzle.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;

import com.ben.laama.sortpuzzle.GameConfig;
import com.ben.laama.sortpuzzle.Main;
import com.ben.laama.sortpuzzle.audio.SoundManager;
import com.ben.laama.sortpuzzle.logic.Difficulty;
import com.ben.laama.sortpuzzle.save.SaveData;
import com.ben.laama.sortpuzzle.ui.BrandMark;
import com.ben.laama.sortpuzzle.ui.Palette;
import com.ben.laama.sortpuzzle.ui.Ui;

/** Title screen: brand, resume, level select, progress chips, the small shop and settings. */
public final class HomeScreen extends BaseScreen {

    private final TextButton buyButton;
    private final Label coinsLabel;
    private final Label boosterLabel;
    private ImageButton soundButton;

    public HomeScreen(Main game) {
        super(game);
        SaveData save = game.save();

        Table root = new Table();
        root.setFillParent(true);
        root.pad(26f, 36f, 30f, 36f);
        stage.addActor(root);

        // ---- top bar: quick mute on the left, settings on the right ----------------
        Table bar = new Table();
        soundButton = Ui.iconButton(skin, soundIcon(), () -> {
            game.sound().toggleMute();
            swapSoundIcon();
            game.sound().play(SoundManager.Cue.BUTTON);
        });
        ImageButton settings = Ui.iconButton(skin, "icon-settings", () -> {
            game.sound().play(SoundManager.Cue.BUTTON);
            game.goSettings();
        });
        bar.add(soundButton).size(Ui.ICON_BUTTON).left().expandX();
        bar.add(settings).size(Ui.ICON_BUTTON).right();
        root.add(bar).growX().row();

        // ---- brand ---------------------------------------------------------------
        BrandMark mark = new BrandMark(skin, game.tubes());
        root.add(mark).size(mark.getWidth(), mark.getHeight()).padTop(26f).row();
        root.add(new Label(GameConfig.APP_NAME, skin, "display")).padTop(18f).row();
        root.add(new Label(GameConfig.TAGLINE, skin, "dim")).padTop(2f).row();

        // ---- primary actions -----------------------------------------------------
        int level = save.currentLevel;
        String caption = "Level " + level + "  •  " + Difficulty.colorsFor(level) + " colours";
        Button play = Ui.twoLineButton(skin, "primary", "PLAY", caption, () -> {
            game.sound().play(SoundManager.Cue.BUTTON);
            game.goToLevel(game.save().currentLevel);
        });
        TextButton levels = Ui.button(skin, "LEVELS", "secondary", () -> {
            game.sound().play(SoundManager.Cue.BUTTON);
            game.goLevelSelect();
        });

        root.add(play).width(400f).padTop(44f).row();
        root.add(levels).width(400f).padTop(16f).row();

        // ---- progress chips ------------------------------------------------------
        coinsLabel = new Label(String.valueOf(save.coins), skin, "gold");
        Label cleared = new Label(save.completedCount() + " cleared", skin, "label");
        Table chips = new Table();
        chips.add(Ui.chip(skin, "icon-coin", Palette.GOLD, coinsLabel)).padRight(12f);
        chips.add(Ui.chip(skin, "icon-check", Palette.SUCCESS, cleared));
        root.add(chips).padTop(22f).row();

        // ---- shop ----------------------------------------------------------------
        boosterLabel = new Label("", skin, "caption");
        buyButton = Ui.button(skin, "BUY  " + GameConfig.BOOSTER_PRICE, "gold", this::buyBooster);

        Table shop = Ui.card(skin);
        Table shopText = new Table();
        shopText.add(new Label("Extra tube", skin, "label")).left().row();
        shopText.add(boosterLabel).left().padTop(2f);
        shop.add(shopText).left().expandX();
        shop.add(buyButton).right();

        root.add().expandY().row();
        root.add(shop).width(500f).row();

        refreshShop();

        root.getColor().a = 0f;
        root.addAction(Actions.fadeIn(0.28f, Interpolation.fade));
    }

    private void buyBooster() {
        SaveData save = game.save();
        if (save.buyBooster()) {
            game.sound().play(SoundManager.Cue.COIN);
            Ui.toast(stage, skin, "Extra tube added to your stock", Palette.SUCCESS);
        } else {
            game.sound().play(SoundManager.Cue.INVALID);
            buyButton.addAction(Actions.sequence(
                Actions.moveBy(9f, 0f, 0.05f),
                Actions.moveBy(-18f, 0f, 0.1f),
                Actions.moveBy(9f, 0f, 0.05f)));
            Ui.toast(stage, skin, "Clear levels to earn coins", Palette.TEXT_DIM);
        }
        refreshShop();
    }

    private void refreshShop() {
        SaveData save = game.save();
        coinsLabel.setText(String.valueOf(save.coins));
        boosterLabel.setText(save.boosters + " in stock  •  "
            + GameConfig.FREE_BOOSTERS_PER_LEVEL + " free every level");
        boolean affordable = save.coins >= GameConfig.BOOSTER_PRICE;
        buyButton.setDisabled(!affordable);
        buyButton.getColor().a = affordable ? 1f : 0.55f;
    }

    private String soundIcon() {
        return game.sound().isMuted() ? "icon-sound-off" : "icon-sound";
    }

    private void swapSoundIcon() {
        soundButton.getStyle().imageUp = skin.newDrawable(soundIcon(), Palette.TEXT);
    }

    @Override
    protected void onBackPressed() {
        Gdx.app.exit();
    }
}
