package com.ben.laama.sortpuzzle.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;

import com.ben.laama.sortpuzzle.GameConfig;
import com.ben.laama.sortpuzzle.Main;
import com.ben.laama.sortpuzzle.audio.SoundManager;
import com.ben.laama.sortpuzzle.ui.Palette;
import com.ben.laama.sortpuzzle.ui.Ui;

/**
 * Settings and about: sound, music, how to play, reset, the consent platform's privacy options
 * when required, and the facts a store reviewer or a player wants to see — version, ads,
 * no account, offline play, privacy policy.
 */
public final class SettingsScreen extends BaseScreen {

    private static final float CARD_WIDTH = 560f;

    public SettingsScreen(Main game) {
        super(game);
        background.setIntensity(0.6f);

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        Table header = new Table();
        header.pad(24f, 26f, 8f, 26f);
        header.add(Ui.iconButton(skin, "icon-back", game::goHome)).size(Ui.ICON_BUTTON).left();
        header.add(new Label("Settings", skin, "title")).expandX();
        header.add().size(Ui.ICON_BUTTON);
        root.add(header).growX().row();

        Table list = new Table();
        list.pad(10f, 0f, 40f, 0f).defaults().width(CARD_WIDTH).padTop(18f);

        // ---- audio ---------------------------------------------------------------
        Table audio = Ui.card(skin);
        audio.defaults().left();
        row(audio, "icon-sound", "Sound effects",
            new Ui.Toggle(skin, !game.sound().isMuted(), on -> {
                game.sound().setSoundEnabled(on);
                game.sound().play(SoundManager.Cue.BUTTON);
            }));
        audio.add(Ui.divider(skin)).height(2f).growX().colspan(3).padTop(14f).padBottom(14f).row();
        // Reflects the music flag itself, not "music audible": with effects off the toggle still
        // shows the choice that will apply once sound comes back.
        row(audio, "icon-music", "Music",
            new Ui.Toggle(skin, !game.save().musicOff, game.sound()::setMusicEnabled));
        list.add(audio).row();

        // ---- game ----------------------------------------------------------------
        Table play = Ui.card(skin);
        play.defaults().left();
        row(play, "icon-help", "How to play", Ui.button(skin, "SHOW", "ghost", this::showHowToPlay));
        play.add(Ui.divider(skin)).height(2f).growX().colspan(3).padTop(14f).padBottom(14f).row();
        row(play, "icon-trash", "Reset progress", Ui.button(skin, "RESET", "danger", this::confirmReset));
        list.add(play).row();

        // ---- privacy -------------------------------------------------------------
        if (game.ads().isPrivacyOptionsRequired()) {
            Table privacyCard = Ui.card(skin);
            privacyCard.defaults().left();
            row(privacyCard, "icon-info", "Privacy options",
                Ui.button(skin, "MANAGE", "ghost", () -> game.ads().showPrivacyOptions()));
            list.add(privacyCard).row();
        }

        // ---- about ---------------------------------------------------------------
        Table about = Ui.card(skin);
        about.add(new Label(GameConfig.APP_NAME + "  " + GameConfig.VERSION_NAME, skin, "label")).left().row();
        about.add(new Label("Free with ads  •  No account  •  Plays offline", skin, "caption"))
            .left().padTop(4f).row();
        about.add(Ui.paragraph(skin, "Your progress is stored only on this device. "
            + "Rewarded videos are always optional. Ads are served by Google AdMob, which may use "
            + "your device's advertising ID; see the privacy policy.", "caption", CARD_WIDTH - 52f))
            .left().padTop(10f).width(CARD_WIDTH - 52f).row();
        if (!GameConfig.PRIVACY_POLICY_URL.isEmpty()) {
            TextButton privacy = Ui.button(skin, "PRIVACY POLICY", "ghost",
                () -> Gdx.net.openURI(GameConfig.PRIVACY_POLICY_URL));
            about.add(privacy).left().padTop(14f).row();
        }
        about.add(new Label("Typeface: Poppins (SIL Open Font License)", skin, "caption"))
            .left().padTop(12f).row();
        list.add(about).row();

        ScrollPane scroll = new ScrollPane(list, skin);
        scroll.setScrollingDisabled(true, false);
        scroll.setFadeScrollBars(true);
        root.add(scroll).grow().row();
    }

    private void row(Table card, String icon, String text, com.badlogic.gdx.scenes.scene2d.Actor control) {
        card.add(new Image(skin.newDrawable(icon, Palette.TEXT_DIM))).size(30f).padRight(16f);
        card.add(new Label(text, skin, "default")).expandX().left();
        card.add(control).right().row();
    }

    private void showHowToPlay() {
        GameScreen.showHowToPlay(stage, skin, null);
    }

    private void confirmReset() {
        Dialog dialog = new Dialog("", skin, "dialog") {
            @Override
            protected void result(Object choice) {
                if (Boolean.TRUE.equals(choice)) {
                    game.save().reset();
                    game.sound().play(SoundManager.Cue.INVALID);
                    Ui.toast(stage, skin, "Progress reset", Palette.TEXT_DIM);
                }
            }
        };
        dialog.setMovable(false);
        Table content = dialog.getContentTable();
        content.pad(30f, 34f, 10f, 34f);
        content.add(new Label("Reset progress?", skin, "title")).row();
        content.add(Ui.paragraph(skin, "Levels, coins and boosters go back to zero. "
            + "This cannot be undone.", "dim", 420f)).width(420f).padTop(8f).row();
        dialog.getButtonTable().pad(14f, 26f, 26f, 26f).defaults().pad(0f, 7f, 0f, 7f);
        dialog.button(Ui.button(skin, "KEEP", "ghost", () -> { }), Boolean.FALSE);
        dialog.button(Ui.button(skin, "RESET", "danger", () -> { }), Boolean.TRUE);
        dialog.show(stage);
    }
}
