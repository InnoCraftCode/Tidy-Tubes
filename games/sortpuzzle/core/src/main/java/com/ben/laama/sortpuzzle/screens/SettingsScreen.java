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
import com.ben.laama.sortpuzzle.i18n.ArabicText;
import com.ben.laama.sortpuzzle.i18n.T;
import com.ben.laama.sortpuzzle.logic.ReminderPlanner;
import com.ben.laama.sortpuzzle.notify.Reminder;
import com.ben.laama.sortpuzzle.ui.Cosmetics;
import com.ben.laama.sortpuzzle.ui.Palette;
import com.ben.laama.sortpuzzle.ui.Ui;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Settings and about: sound, music, how to play, reset, the consent platform's privacy options
 * when required, and the facts a store reviewer or a player wants to see — version, ads,
 * no account, offline play, privacy policy.
 */
public final class SettingsScreen extends BaseScreen {

    private static final float CARD_WIDTH = 560f;
    private static final DateTimeFormatter REMINDER_TIME =
        DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);

    public SettingsScreen(Main game) {
        super(game);
        background.setIntensity(0.6f);

        Table root = new Table();
        root.setFillParent(true);
        safeArea.addActor(root);

        Table header = new Table();
        header.pad(24f, 26f, 8f, 26f);
        header.add(Ui.iconButton(skin, "icon-back", game::goHome)).size(Ui.ICON_BUTTON).left();
        header.add(new Label(T.r("Settings"), skin, "title")).expandX();
        header.add().size(Ui.ICON_BUTTON);
        root.add(header).growX().row();

        Table list = new Table();
        list.pad(10f, 0f, 40f, 0f).defaults().width(CARD_WIDTH).padTop(18f);

        // ---- language ------------------------------------------------------------
        Table languageCard = Ui.card(skin);
        languageCard.defaults().left();
        Table current = new Table();
        current.add(new Image(skin.getDrawable("flag-" + T.language().flag))).size(42f, 28f).padRight(10f);
        current.add(new Label(ArabicText.visual(T.language().nativeName), skin, "label"));
        com.badlogic.gdx.scenes.scene2d.ui.Button change = new com.badlogic.gdx.scenes.scene2d.ui.Button(
            skin.get("ghost", TextButton.TextButtonStyle.class));
        change.add(current).pad(10f, 18f, 10f, 18f);
        Ui.click(change, game::goLanguage);
        row(languageCard, "icon-info", T.r("Language"), change);
        list.add(languageCard).row();

        // ---- audio ---------------------------------------------------------------
        Table audio = Ui.card(skin);
        audio.defaults().left();
        // Two independent switches: effects only, music only, or both off.
        row(audio, "icon-sound", T.r("Sound effects"),
            new Ui.Toggle(skin, game.sound().areEffectsOn(), on -> {
                game.sound().setEffectsEnabled(on);
                game.sound().play(SoundManager.Cue.BUTTON); // silent when just switched off
            }));
        audio.add(Ui.divider(skin)).height(2f).growX().colspan(3).padTop(14f).padBottom(14f).row();
        row(audio, "icon-music", T.r("Music"),
            new Ui.Toggle(skin, game.sound().isMusicOn(), game.sound()::setMusicEnabled));
        audio.add(Ui.divider(skin)).height(2f).growX().colspan(3).padTop(14f).padBottom(14f).row();
        // The saved track's name is the button caption, so the choice stays visible here.
        int track = game.save().musicTrack;
        row(audio, "icon-music", T.r("Music track"),
            Ui.button(skin, track >= 0 ? MusicSelectionScreen.title(track) : T.r("CHOOSE"), "ghost",
                game::goMusicSelection));
        audio.add(Ui.divider(skin)).height(2f).growX().colspan(3).padTop(14f).padBottom(14f).row();
        row(audio, "icon-sound", T.r("Vibration"),
            new Ui.Toggle(skin, game.save().vibrate, game.save()::setVibrate));
        if (game.reminders().isSupported()) {
            // One gentle notification a day at most, about the daily puzzle only.
            audio.add(Ui.divider(skin)).height(2f).growX().colspan(3).padTop(14f).padBottom(14f).row();
            final Ui.Toggle[] reminderToggle = new Ui.Toggle[1];
            final Label nextReminder = new Label(nextReminderText(), skin, "caption");
            // Wrapped inside the card: unwrapped, this long line widened the whole card past
            // CARD_WIDTH and pushed every row of it off the left edge.
            nextReminder.setWrap(true);
            reminderToggle[0] = new Ui.Toggle(skin,
                game.save().remindersOn && game.reminders().hasPermission(), on -> {
                    if (on) {
                        game.enableReminders(enabled -> {
                            reminderToggle[0].setOn(enabled);
                            nextReminder.setText(nextReminderText());
                        });
                    } else {
                        game.save().setRemindersOn(false);
                        game.reminders().cancelAll();
                        nextReminder.setText(nextReminderText());
                    }
                });
            row(audio, "icon-info", T.r("Daily puzzle reminder"), reminderToggle[0]);
            // Says when the next one is due, so "I got nothing today" has an answer.
            audio.add(nextReminder).colspan(3).width(CARD_WIDTH - 52f).left().padTop(4f).row();
            if (game.reminders().supportsTest()) {
                // Debug builds only: proves the channel, permission and receiver on this device.
                audio.add(Ui.button(skin, T.r("TEST NOTIFICATION IN 15 s"), "ghost", () -> {
                    game.reminders().scheduleTest(15);
                    Ui.toast(stage, skin, T.r("Leave the game: a test reminder arrives in 15 s"),
                        Palette.TEXT_DIM);
                })).colspan(3).left().padTop(10f).height(52f).row();
            }
        }
        list.add(audio).row();

        // ---- game ----------------------------------------------------------------
        Table play = Ui.card(skin);
        play.defaults().left();
        row(play, "icon-tube", T.r("Shop & styles"), Ui.button(skin, T.r("OPEN"), "ghost", game::goShop));
        play.add(Ui.divider(skin)).height(2f).growX().colspan(3).padTop(14f).padBottom(14f).row();
        row(play, "icon-help", T.r("How to play"), Ui.button(skin, T.r("SHOW"), "ghost", this::showHowToPlay));
        if (game.services().isAvailable()) {
            play.add(Ui.divider(skin)).height(2f).growX().colspan(3).padTop(14f).padBottom(14f).row();
            row(play, "icon-play", "Google Play Games", playGamesControl());
        }
        play.add(Ui.divider(skin)).height(2f).growX().colspan(3).padTop(14f).padBottom(14f).row();
        row(play, "icon-trash", T.r("Reset progress"), Ui.button(skin, T.r("RESET"), "danger", this::confirmReset));
        play.add(Ui.divider(skin)).height(2f).growX().colspan(3).padTop(14f).padBottom(14f).row();
        // No ad on quit: AdMob forbids interstitials when the user exits the app.
        row(play, "icon-close", T.r("Quit game"), Ui.button(skin, T.r("QUIT"), "ghost", Gdx.app::exit));
        list.add(play).row();

        // ---- privacy -------------------------------------------------------------
        if (game.ads().isPrivacyOptionsRequired()) {
            Table privacyCard = Ui.card(skin);
            privacyCard.defaults().left();
            row(privacyCard, "icon-info", T.r("Privacy options"),
                Ui.button(skin, T.r("MANAGE"), "ghost", () -> game.ads().showPrivacyOptions()));
            list.add(privacyCard).row();
        }

        // ---- about ---------------------------------------------------------------
        Table about = Ui.card(skin);
        about.add(new Label(GameConfig.APP_NAME + "  " + GameConfig.VERSION_NAME, skin, "label")).left().row();
        boolean playGames = game.services().isAvailable();
        about.add(new Label(playGames
                ? T.r("Free with ads  •  Play Games optional  •  Plays offline")
                : T.r("Free with ads  •  No account  •  Plays offline"), skin, "caption"))
            .left().padTop(4f).row();
        about.add(Ui.paragraph(skin, playGames
            ? T.r("Your progress is stored only on this device. Rewarded videos are always optional. "
                + "Ads are served by Google AdMob, which may use your device's advertising ID; "
                + "leaderboards use your Google Play Games profile only if you sign in. Anonymous usage "
                + "statistics and crash reports may be collected; see the privacy policy.")
            : T.r("Your progress is stored only on this device. Rewarded videos are always optional. "
                + "Ads are served by Google AdMob, which may use your device's advertising ID. Anonymous "
                + "usage statistics and crash reports may be collected; see the privacy policy."),
            "caption", CARD_WIDTH - 52f))
            .left().padTop(10f).width(CARD_WIDTH - 52f).row();
        if (!GameConfig.PRIVACY_POLICY_URL.isEmpty()) {
            TextButton privacy = Ui.button(skin, T.r("PRIVACY POLICY"), "ghost",
                () -> Gdx.net.openURI(GameConfig.PRIVACY_POLICY_URL));
            about.add(privacy).left().padTop(14f).row();
        }
        // Wrapped: on one line, longer translations widened the card past CARD_WIDTH.
        Label typefaces = Ui.paragraph(skin,
            T.r("Typefaces: Poppins and Noto Sans Arabic (SIL Open Font License)"), "caption", CARD_WIDTH - 52f);
        typefaces.setAlignment(com.badlogic.gdx.utils.Align.left);
        about.add(typefaces).width(CARD_WIDTH - 52f).left().padTop(12f).row();
        list.add(about).row();
        // Cards float in one after the other, top to bottom.
        for (int i = 0; i < list.getChildren().size; i++) {
            com.ben.laama.sortpuzzle.ui.Motion.enter(list.getChildren().get(i), i);
        }

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

    /** Signed in: who; signed out: a sign-in button. Play Games is opt-in, never required. */
    private com.badlogic.gdx.scenes.scene2d.Actor playGamesControl() {
        if (game.services().isSignedIn()) {
            String name = game.services().playerName();
            return new Label(name.isEmpty() ? T.r("Signed in") : T.show(name), skin, "caption");
        }
        TextButton signIn = Ui.button(skin, T.r("SIGN IN"), "ghost", () -> { });
        signIn.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                signIn.setDisabled(true);
                game.services().signIn(ok -> {
                    signIn.setDisabled(false);
                    signIn.setText(ok ? T.r("SIGNED IN") : T.r("SIGN IN"));
                    if (ok) {
                        game.syncLeaderboards();
                    }
                    Ui.toast(stage, skin, ok ? T.r("Signed in to Google Play Games.")
                        : T.r("Couldn't sign in to Play Games. Try again later."), Palette.TEXT_DIM);
                });
            }
        });
        return signIn;
    }

    /** The rule in one line, and when the first one would come if the player left now. */
    private String nextReminderText() {
        String rule = T.rawf("A nudge {0} min after you leave, then every {1} h. Quiet {2}:00-{3}:00.",
            ReminderPlanner.FIRST_DELAY_MINUTES, ReminderPlanner.INTERVAL_HOURS,
            ReminderPlanner.QUIET_START_HOUR, String.format(Locale.ROOT, "%02d", ReminderPlanner.QUIET_END_HOUR));
        if (!game.save().remindersOn || !game.reminders().hasPermission()) {
            return T.f("Off. {0}", rule);
        }
        ZonedDateTime now = ZonedDateTime.now();
        List<Reminder> plan = ReminderPlanner.plan(now, game.save().lastDailyWinDate,
            game.save().lastDailyStartDate, game.save().dailyWinStreak, game.save().currentLevel);
        if (plan.isEmpty()) {
            return T.show(rule);
        }
        ZonedDateTime at = Instant.ofEpochMilli(plan.get(0).atMillis).atZone(ZoneId.systemDefault());
        long days = java.time.temporal.ChronoUnit.DAYS.between(now.toLocalDate(), at.toLocalDate());
        String day = days == 0 ? T.raw("today") : days == 1 ? T.raw("tomorrow")
            : at.format(DateTimeFormatter.ofPattern("EEE", new Locale(T.language().code)));
        return T.f("{0} If you left now: {1} {2}.", rule, day, at.format(REMINDER_TIME));
    }

    private void confirmReset() {
        Dialog dialog = new Dialog("", skin, "dialog") {
            @Override
            protected void result(Object choice) {
                if (Boolean.TRUE.equals(choice)) {
                    game.save().reset();
                    Cosmetics.apply(skin, game.tubes(), game.save()); // back to the free looks
                    background.refresh(skin);
                    game.sound().play(SoundManager.Cue.INVALID);
                    Ui.toast(stage, skin, T.r("Progress reset"), Palette.TEXT_DIM);
                }
            }
        };
        dialog.setMovable(false);
        Table content = dialog.getContentTable();
        content.pad(30f, 34f, 10f, 34f);
        content.add(new Label(T.r("Reset progress?"), skin, "title")).row();
        content.add(Ui.paragraph(skin, T.r("Levels, coins, boosters and bought styles go back to "
            + "zero. This cannot be undone."), "dim", 420f)).width(420f).padTop(8f).row();
        dialog.getButtonTable().pad(14f, 26f, 26f, 26f).defaults().pad(0f, 7f, 0f, 7f);
        dialog.button(Ui.button(skin, T.r("KEEP"), "ghost", () -> { }), Boolean.FALSE);
        dialog.button(Ui.button(skin, T.r("RESET"), "danger", () -> { }), Boolean.TRUE);
        dialog.show(stage);
    }
}
