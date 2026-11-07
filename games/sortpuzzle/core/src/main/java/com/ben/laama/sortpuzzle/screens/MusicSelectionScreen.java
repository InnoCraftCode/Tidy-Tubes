package com.ben.laama.sortpuzzle.screens;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import com.ben.laama.sortpuzzle.Main;
import com.ben.laama.sortpuzzle.i18n.T;
import com.ben.laama.sortpuzzle.ui.Palette;
import com.ben.laama.sortpuzzle.ui.Ui;

/** Lets players preview and choose an original, gentle ambient loop. */
public final class MusicSelectionScreen extends BaseScreen {

    private static final String[] TITLES = {
        T.k("Quiet Moon"), T.k("Soft Horizon"), T.k("Stillwater"), T.k("Cloud Drift"), T.k("Lavender Sky"),
        T.k("Gentle Dawn"), T.k("Blue Hour"), T.k("Slow Bloom"), T.k("Starfall"), T.k("Deep Rest")
    };
    private static final String[] DESCRIPTIONS = {
        T.k("Warm, slow-moving chords"), T.k("A calm open sound"), T.k("Soft notes, lots of space"),
        T.k("Light melody over a smooth pad"), T.k("A mellow, floating theme"), T.k("Gentle morning tones"),
        T.k("Low-key evening ambience"), T.k("A quiet, gradual melody"), T.k("Sparse notes for winding down"),
        T.k("The softest, slowest loop")
    };

    /** @return the display name of a track, for Settings and anywhere else that shows the choice */
    public static String title(int track) {
        return T.r(TITLES[track]);
    }

    /** Text column inside a 310-wide track card with 22 units of padding each side. */
    private static final float CARD_TEXT_WIDTH = 310f - 44f;

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
            header.add(new Label(T.r("Relaxing music"), skin, "title")).expandX();
            header.add().size(Ui.ICON_BUTTON);
            root.add(header).growX().row();
        } else {
            root.add(new Label(T.r("Choose your calm"), skin, "title")).row();
        }
        String intro = !game.sound().isMusicOn()
            ? T.r("Music is off, so previews are silent. Turn Music on in Settings to hear them.")
            : savedTrack >= 0 ? T.f("Current track: {0}", T.raw(TITLES[savedTrack]))
            : T.r("Pick a gentle loop for focus or winding down. Sound is optional.");
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

        confirmButton = Ui.button(skin, firstLaunch ? T.r("START WITH THIS MUSIC") : T.r("SAVE MUSIC"),
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
        // Tighter than the standard card: two caption lines must fit the fixed 150-unit cell.
        card.pad(16f, 22f, 16f, 22f);
        card.defaults().left().width(CARD_TEXT_WIDTH);
        Label title = new Label(T.r(TITLES[index]), skin, "label");
        title.setEllipsis(true);
        card.add(title).row();
        // Wrapped: translations are often longer than the English line and ran out of the card.
        Label description = new Label(T.r(DESCRIPTIONS[index]), skin, "caption");
        description.setWrap(true);
        card.add(description).padTop(4f).row();
        Label hint = new Label("", skin, "caption");
        hint.setEllipsis(true);
        card.add(hint).padTop(6f).expandY().bottom().row();
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
                ? (current ? T.r("CURRENT TRACK") : game.sound().isMusicOn() ? T.r("SELECTED  •  PREVIEWING") : T.r("SELECTED"))
                : current ? T.r("CURRENT TRACK") : T.r("TAP TO PREVIEW");
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
