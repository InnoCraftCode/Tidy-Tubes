package com.ben.laama.sortpuzzle.screens;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import com.ben.laama.sortpuzzle.Main;
import com.ben.laama.sortpuzzle.ui.Palette;
import com.ben.laama.sortpuzzle.ui.Ui;

/** Lets players preview and choose an original, gentle ambient loop. */
public final class MusicSelectionScreen extends BaseScreen {

    private static final String[] TITLES = {
        "Quiet Moon", "Soft Horizon", "Stillwater", "Cloud Drift", "Lavender Sky",
        "Gentle Dawn", "Blue Hour", "Slow Bloom", "Starfall", "Deep Rest"
    };
    private static final String[] DESCRIPTIONS = {
        "Warm, slow-moving chords", "A calm open sound", "Soft notes, lots of space",
        "Light melody over a smooth pad", "A mellow, floating theme", "Gentle morning tones",
        "Low-key evening ambience", "A quiet, gradual melody", "Sparse notes for winding down",
        "The softest, slowest loop"
    };

    /** @return the display name of a track, for Settings and anywhere else that shows the choice */
    public static String title(int track) {
        return TITLES[track];
    }

    private final boolean firstLaunch;
    private final int savedTrack;
    private final Table[] trackCards = new Table[TITLES.length];
    private final TextButton confirmButton;
    private int selectedTrack;

    public MusicSelectionScreen(Main game, boolean firstLaunch) {
        super(game);
        this.firstLaunch = firstLaunch;
        savedTrack = game.save().musicTrack;
        selectedTrack = savedTrack;

        Table root = new Table();
        root.setFillParent(true);
        root.pad(22f, 30f, 26f, 30f);
        safeArea.addActor(root);

        if (!firstLaunch) {
            Table header = new Table();
            header.add(Ui.iconButton(skin, "icon-back", this::cancel)).left();
            header.add(new Label("Relaxing music", skin, "title")).expandX();
            header.add().size(Ui.ICON_BUTTON);
            root.add(header).growX().row();
        } else {
            root.add(new Label("Choose your calm", skin, "title")).row();
        }
        String intro = !game.sound().isMusicOn()
            ? "Music is off, so previews are silent. Turn Music on in Settings to hear them."
            : savedTrack >= 0 ? "Current track: " + TITLES[savedTrack]
            : "Pick a gentle loop for focus or winding down. Sound is optional.";
        root.add(new Label(intro, skin, "caption")).padTop(8f).padBottom(18f).row();

        Table tracks = new Table();
        tracks.defaults().width(310f).height(150f).pad(7f);
        for (int i = 0; i < TITLES.length; i++) {
            trackCards[i] = createTrackCard(i);
            tracks.add(trackCards[i]);
            if (i % 2 == 1) tracks.row();
        }
        ScrollPane scroll = new ScrollPane(tracks, skin);
        scroll.setScrollingDisabled(true, false);
        scroll.setFadeScrollBars(false);
        root.add(scroll).grow().row();

        confirmButton = Ui.button(skin, firstLaunch ? "START WITH THIS MUSIC" : "SAVE MUSIC",
            "primary", this::confirm);
        root.add(confirmButton).width(520f).height(78f).padTop(14f);
        updateCards();

        if (selectedTrack >= 0) {
            game.sound().previewMusic(selectedTrack);
        }
    }

    private Table createTrackCard(int index) {
        Table card = Ui.card(skin);
        card.setTouchable(Touchable.enabled);
        card.defaults().left();
        card.add(new Label(TITLES[index], skin, "label")).growX().row();
        card.add(new Label(DESCRIPTIONS[index], skin, "caption"))
            .growX().padTop(6f).row();
        Label hint = new Label("", skin, "caption");
        card.add(hint).padTop(9f).row();
        card.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectedTrack = index;
                updateCards();
                game.sound().previewMusic(index);
            }
        });
        card.setUserObject(hint);
        return card;
    }

    private void updateCards() {
        for (int i = 0; i < trackCards.length; i++) {
            boolean selected = i == selectedTrack;
            boolean current = i == savedTrack;
            String state = selected
                ? (current ? "CURRENT TRACK" : game.sound().isMusicOn() ? "SELECTED  •  PREVIEWING" : "SELECTED")
                : current ? "CURRENT TRACK" : "TAP TO PREVIEW";
            trackCards[i].setBackground(skin.newDrawable("card",
                selected ? Palette.SURFACE_STRONG : Palette.SURFACE));
            ((Label) trackCards[i].getUserObject()).setText(state);
            ((Label) trackCards[i].getUserObject()).setColor(
                selected || current ? Palette.PRIMARY : Palette.TEXT_DIM);
        }
        confirmButton.setDisabled(selectedTrack < 0);
    }

    private void confirm() {
        if (selectedTrack < 0) return;
        game.sound().selectMusicTrack(selectedTrack);
        game.sound().play(com.ben.laama.sortpuzzle.audio.SoundManager.Cue.BUTTON);
        game.finishMusicSelection(firstLaunch);
    }

    private void cancel() {
        if (savedTrack >= 0) {
            game.sound().previewMusic(savedTrack); // back to the saved loop, not the last preview
        }
        game.finishMusicSelection(false);
    }

    @Override
    protected void onBackPressed() {
        if (!firstLaunch) {
            cancel();
        }
    }
}
