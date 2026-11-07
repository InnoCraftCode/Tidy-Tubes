package com.ben.laama.sortpuzzle.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;

import com.ben.laama.sortpuzzle.Main;
import com.ben.laama.sortpuzzle.audio.SoundManager;
import com.ben.laama.sortpuzzle.i18n.ArabicText;
import com.ben.laama.sortpuzzle.i18n.Language;
import com.ben.laama.sortpuzzle.i18n.T;
import com.ben.laama.sortpuzzle.ui.Palette;
import com.ben.laama.sortpuzzle.ui.Ui;

/**
 * The language picker: a card listing every language with its flag, its name written in
 * itself and, under it, the language and country in the current interface language
 * ("Arabic · Saudi Arabia"). Shown before anything else on the first launch, with the device's
 * language preselected, and from Settings afterwards.
 */
public final class LanguageScreen extends BaseScreen {

    private final boolean firstLaunch;
    private final Language current;
    private final Table[] rows = new Table[Language.values().length];
    private final Image[] checks = new Image[rows.length];
    private Language selected;

    public LanguageScreen(Main game, boolean firstLaunch) {
        super(game);
        this.firstLaunch = firstLaunch;
        this.current = T.language();
        this.selected = current;

        Table root = new Table();
        root.setFillParent(true);
        root.pad(24f);
        safeArea.addActor(root);

        Table card = Ui.card(skin);
        card.setBackground(skin.newDrawable("card", Palette.CARD));
        card.pad(30f, 26f, 26f, 26f);

        Table header = new Table();
        if (!firstLaunch) {
            header.add(Ui.iconButton(skin, "icon-back", this::cancel)).size(Ui.ICON_BUTTON);
        }
        Label title = new Label(T.r("Choose your language"), skin, "title");
        title.setAlignment(Align.center);
        header.add(title).expandX();
        if (!firstLaunch) {
            header.add().size(Ui.ICON_BUTTON);
        }
        card.add(header).growX().row();
        card.add(new Label(T.r("You can change it any time in Settings."), skin, "caption"))
            .padTop(4f).padBottom(16f).row();

        Table list = new Table();
        Language[] languages = Language.values();
        for (int i = 0; i < languages.length; i++) {
            rows[i] = row(languages[i], i);
            list.add(rows[i]).growX().height(84f).padBottom(8f).row();
        }
        ScrollPane scroll = new ScrollPane(list, skin);
        scroll.setScrollingDisabled(true, false);
        scroll.setFadeScrollBars(true);
        card.add(scroll).width(540f).growY().row();

        TextButton confirm = Ui.button(skin, firstLaunch ? T.r("CONTINUE") : T.r("SAVE"), "primary",
            this::confirm);
        card.add(confirm).width(460f).height(78f).padTop(16f);

        root.add(card).width(600f).maxHeight(1180f).growY();
        refresh();
    }

    private Table row(Language language, int index) {
        Table row = new Table();
        row.setTouchable(Touchable.enabled);
        row.pad(10f, 18f, 10f, 18f);
        row.add(new Image(skin.getDrawable("flag-" + language.flag))).size(66f, 44f).padRight(18f);

        Table names = new Table();
        names.left(); // content against the flag, not centred in the row (mirrored in RTL)
        names.defaults().left();
        // Written in itself, whatever the interface language (Arabic is shaped either way).
        Label own = new Label(ArabicText.visual(language.nativeName), skin, "label");
        names.add(own).row();
        Label described = new Label(T.f("{0} · {1}", T.raw(englishName(language)),
            T.raw(countryName(language))), skin, "caption");
        names.add(described).padTop(-2f);
        row.add(names).expandX().fillX().left();

        Image check = new Image(skin.newDrawable("icon-check", Palette.PRIMARY));
        checks[index] = check;
        row.add(check).size(30f).padLeft(10f);

        Ui.click(row, () -> {
            selected = language;
            game.sound().play(SoundManager.Cue.BUTTON);
            refresh();
        });
        return row;
    }

    private void refresh() {
        Language[] languages = Language.values();
        for (int i = 0; i < languages.length; i++) {
            boolean on = languages[i] == selected;
            rows[i].setBackground(skin.newDrawable("card-sm", on ? Palette.SURFACE_STRONG
                : new Color(1f, 1f, 1f, 0.04f)));
            checks[i].setVisible(on);
        }
    }

    private void confirm() {
        game.sound().play(SoundManager.Cue.BUTTON);
        if (!firstLaunch && selected == current) {
            game.goSettings();
            return;
        }
        game.changeLanguage(selected, firstLaunch);
    }

    private void cancel() {
        game.goSettings();
    }

    @Override
    protected void onBackPressed() {
        if (!firstLaunch) {
            cancel();
        }
    }

    /** English name of the language: a translation key, shown in the interface language. */
    static String englishName(Language language) {
        switch (language) {
            case EN: return T.k("English");
            case FR: return T.k("French");
            case AR: return T.k("Arabic");
            case ES: return T.k("Spanish");
            case PT: return T.k("Portuguese");
            case DE: return T.k("German");
            case ID: return T.k("Indonesian");
            case TR: return T.k("Turkish");
            default: return language.name();
        }
    }

    /** English name of the flag's country: a translation key too. */
    static String countryName(Language language) {
        switch (language.flag) {
            case "GB": return T.k("United Kingdom");
            case "FR": return T.k("France");
            case "SA": return T.k("Saudi Arabia");
            case "ES": return T.k("Spain");
            case "BR": return T.k("Brazil");
            case "DE": return T.k("Germany");
            case "ID": return T.k("Indonesia");
            case "TR": return T.k("Türkiye");
            default: return language.flag;
        }
    }
}
