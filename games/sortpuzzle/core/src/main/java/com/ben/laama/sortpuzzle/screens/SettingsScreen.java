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
    private static final DateTimeFormatter REMINDER_DAY =
        DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);
    private static final DateTimeFormatter REMINDER_TIME =
        DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

    public SettingsScreen(Main game) {
        super(game);
        background.setIntensity(0.6f);

        Table root = new Table();
        root.setFillParent(true);
        safeArea.addActor(root);

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
        // Two independent switches: effects only, music only, or both off.
        row(audio, "icon-sound", "Sound effects",
            new Ui.Toggle(skin, game.sound().areEffectsOn(), on -> {
                game.sound().setEffectsEnabled(on);
                game.sound().play(SoundManager.Cue.BUTTON); // silent when just switched off
            }));
        audio.add(Ui.divider(skin)).height(2f).growX().colspan(3).padTop(14f).padBottom(14f).row();
        row(audio, "icon-music", "Music",
            new Ui.Toggle(skin, game.sound().isMusicOn(), game.sound()::setMusicEnabled));
        audio.add(Ui.divider(skin)).height(2f).growX().colspan(3).padTop(14f).padBottom(14f).row();
        // The saved track's name is the button caption, so the choice stays visible here.
        int track = game.save().musicTrack;
        row(audio, "icon-music", "Music track",
            Ui.button(skin, track >= 0 ? MusicSelectionScreen.title(track) : "CHOOSE", "ghost",
                game::goMusicSelection));
        audio.add(Ui.divider(skin)).height(2f).growX().colspan(3).padTop(14f).padBottom(14f).row();
        row(audio, "icon-sound", "Vibration",
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
            row(audio, "icon-info", "Daily puzzle reminder", reminderToggle[0]);
            // Says when the next one is due, so "I got nothing today" has an answer.
            audio.add(nextReminder).colspan(3).width(CARD_WIDTH - 52f).left().padTop(4f).row();
            if (game.reminders().supportsTest()) {
                // Debug builds only: proves the channel, permission and receiver on this device.
                audio.add(Ui.button(skin, "TEST NOTIFICATION IN 15 s", "ghost", () -> {
                    game.reminders().scheduleTest(15);
                    Ui.toast(stage, skin, "Leave the game: a test reminder arrives in 15 s",
                        Palette.TEXT_DIM);
                })).colspan(3).left().padTop(10f).height(52f).row();
            }
        }
        list.add(audio).row();

        // ---- game ----------------------------------------------------------------
        Table play = Ui.card(skin);
        play.defaults().left();
        row(play, "icon-tube", "Shop & styles", Ui.button(skin, "OPEN", "ghost", game::goShop));
        play.add(Ui.divider(skin)).height(2f).growX().colspan(3).padTop(14f).padBottom(14f).row();
        row(play, "icon-help", "How to play", Ui.button(skin, "SHOW", "ghost", this::showHowToPlay));
        if (game.services().isAvailable()) {
            play.add(Ui.divider(skin)).height(2f).growX().colspan(3).padTop(14f).padBottom(14f).row();
            row(play, "icon-play", "Google Play Games", playGamesControl());
        }
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
        boolean playGames = game.services().isAvailable();
        about.add(new Label(playGames
                ? "Free with ads  •  Play Games optional  •  Plays offline"
                : "Free with ads  •  No account  •  Plays offline", skin, "caption"))
            .left().padTop(4f).row();
        about.add(Ui.paragraph(skin, "Your progress is stored only on this device. "
            + "Rewarded videos are always optional. Ads are served by Google AdMob, which may use "
            + "your device's advertising ID"
            + (playGames ? "; leaderboards use your Google Play Games profile only if you sign in"
                : "")
            + ". Anonymous usage statistics and crash reports may be collected; see the privacy policy.",
            "caption", CARD_WIDTH - 52f))
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

    /** Signed in: who; signed out: a sign-in button. Play Games is opt-in, never required. */
    private com.badlogic.gdx.scenes.scene2d.Actor playGamesControl() {
        if (game.services().isSignedIn()) {
            String name = game.services().playerName();
            return new Label(name.isEmpty() ? "Signed in" : name, skin, "caption");
        }
        TextButton signIn = Ui.button(skin, "SIGN IN", "ghost", () -> { });
        signIn.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                signIn.setDisabled(true);
                game.services().signIn(ok -> {
                    signIn.setDisabled(false);
                    signIn.setText(ok ? "SIGNED IN" : "SIGN IN");
                    if (ok) {
                        game.syncLeaderboards();
                    }
                    Ui.toast(stage, skin, ok ? "Signed in to Google Play Games."
                        : "Couldn't sign in to Play Games. Try again later.", Palette.TEXT_DIM);
                });
            }
        });
        return signIn;
    }

    /** The rule in one line, and when the first one would come if the player left now. */
    private String nextReminderText() {
        String rule = "A nudge " + ReminderPlanner.FIRST_DELAY_MINUTES + " min after you leave, then every "
            + ReminderPlanner.INTERVAL_HOURS + " h. Quiet " + ReminderPlanner.QUIET_START_HOUR + ":00-"
            + String.format(Locale.ENGLISH, "%02d", ReminderPlanner.QUIET_END_HOUR) + ":00.";
        if (!game.save().remindersOn || !game.reminders().hasPermission()) {
            return "Off. " + rule;
        }
        ZonedDateTime now = ZonedDateTime.now();
        List<Reminder> plan = ReminderPlanner.plan(now, game.save().lastDailyWinDate,
            game.save().lastDailyStartDate, game.save().dailyWinStreak, game.save().currentLevel);
        if (plan.isEmpty()) {
            return rule;
        }
        ZonedDateTime at = Instant.ofEpochMilli(plan.get(0).atMillis).atZone(ZoneId.systemDefault());
        long days = java.time.temporal.ChronoUnit.DAYS.between(now.toLocalDate(), at.toLocalDate());
        String day = days == 0 ? "today" : days == 1 ? "tomorrow" : at.format(REMINDER_DAY);
        return rule + " If you left now: " + day + " " + at.format(REMINDER_TIME) + ".";
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
                    Ui.toast(stage, skin, "Progress reset", Palette.TEXT_DIM);
                }
            }
        };
        dialog.setMovable(false);
        Table content = dialog.getContentTable();
        content.pad(30f, 34f, 10f, 34f);
        content.add(new Label("Reset progress?", skin, "title")).row();
        content.add(Ui.paragraph(skin, "Levels, coins, boosters and bought styles go back to "
            + "zero. This cannot be undone.", "dim", 420f)).width(420f).padTop(8f).row();
        dialog.getButtonTable().pad(14f, 26f, 26f, 26f).defaults().pad(0f, 7f, 0f, 7f);
        dialog.button(Ui.button(skin, "KEEP", "ghost", () -> { }), Boolean.FALSE);
        dialog.button(Ui.button(skin, "RESET", "danger", () -> { }), Boolean.TRUE);
        dialog.show(stage);
    }
}
