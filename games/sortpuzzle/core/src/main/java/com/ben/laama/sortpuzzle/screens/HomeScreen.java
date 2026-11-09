package com.ben.laama.sortpuzzle.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;

import com.ben.laama.sortpuzzle.GameConfig;
import com.ben.laama.sortpuzzle.Main;
import com.ben.laama.sortpuzzle.i18n.T;
import com.ben.laama.sortpuzzle.ads.AdProvider;
import com.ben.laama.sortpuzzle.audio.SoundManager;
import com.ben.laama.sortpuzzle.logic.DailyChallenge;
import com.ben.laama.sortpuzzle.logic.LevelNumbers;
import com.ben.laama.sortpuzzle.logic.DailyRewards;
import com.ben.laama.sortpuzzle.logic.Difficulty;
import com.ben.laama.sortpuzzle.logic.ReminderAskPolicy;
import com.ben.laama.sortpuzzle.logic.Shop;
import com.ben.laama.sortpuzzle.logic.StarGates;
import com.ben.laama.sortpuzzle.logic.World;
import com.ben.laama.sortpuzzle.save.SaveData;
import com.ben.laama.sortpuzzle.ui.BrandMark;
import com.ben.laama.sortpuzzle.ui.Motion;
import com.ben.laama.sortpuzzle.ui.Palette;
import com.ben.laama.sortpuzzle.ui.Ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Title screen: brand, resume (with its biome), daily puzzle, navigation, daily reward. */
public final class HomeScreen extends BaseScreen {

    private final Label coinsLabel;
    private final Label boostersLabel;
    private TextButton dailyBonusButton;
    private ImageButton soundButton;
    private Table coinChip;
    private int shownCoins;
    private TextButton claimButton;
    private Table calendar;

    /** Built once; {@link #arrange} places them in one column (portrait) or two (landscape). */
    private final Table root;
    private final Table bar;
    private final Table brand;
    private final Button play;
    private final Table dailyCard;
    private final Table navigation;
    /** Widest the WORLD / GOALS / SHOP row may get: the screen minus margins (720 - 2 x 40). */
    private static final float NAVIGATION_MAX_WIDTH = 640f;
    private final Table chips;
    private final Table rewardCard;
    private Boolean arrangedLandscape;

    public HomeScreen(Main game) {
        super(game);
        SaveData save = game.save();

        root = new Table();
        root.setFillParent(true);
        safeArea.addActor(root);

        // ---- top bar: quick mute on the left, settings on the right ----------------
        bar = new Table();
        soundButton = Ui.iconButton(skin, soundIcon(), () -> {
            game.sound().toggleMute();
            swapSoundIcon();
            game.sound().play(SoundManager.Cue.BUTTON);
        });
        ImageButton settings = Ui.iconButton(skin, "icon-settings", () -> {
            game.sound().play(SoundManager.Cue.BUTTON);
            game.goSettings();
        });
        // Campaign stars, centred at the top; "90 / 180" while a star gate is ahead.
        int stars = save.totalStars();
        int target = StarGates.nextTarget(save);
        Label starsLabel = new Label(target > 0 ? stars + " / " + target : String.valueOf(stars),
            skin, "gold");
        Table starChip = Ui.chip(skin, "icon-star", Palette.GOLD, starsLabel);
        // The purse sits next to the stars in the header (refreshPurse updates it).
        coinsLabel = new Label(String.valueOf(save.coins), skin, "gold");
        shownCoins = save.coins;
        coinChip = Ui.chip(skin, "icon-coin", Palette.GOLD, coinsLabel);
        Table wallet = new Table();
        wallet.add(starChip).padRight(10f);
        wallet.add(coinChip);
        bar.add(soundButton).size(Ui.ICON_BUTTON).left();
        bar.add(wallet).expandX();
        bar.add(settings).size(Ui.ICON_BUTTON).right();

        // ---- brand ---------------------------------------------------------------
        BrandMark mark = new BrandMark(skin, game.tubes());
        mark.setScale(0.78f);
        brand = new Table();
        brand.add(mark).size(mark.getWidth() * 0.78f, mark.getHeight() * 0.78f)
            .padTop(8f).row();
        brand.add(new Label(GameConfig.APP_NAME, skin, "display")).padTop(2f).row();
        brand.add(new Label(T.r(GameConfig.TAGLINE), skin, "dim")).padTop(0f).row();

        // ---- primary action: where the player is on the map ---------------------
        int level = save.currentLevel;
        World.Biome biome = World.biomeFor(level);
        String caption = T.f("{0}  •  {1} colours", T.raw(biome.name), Difficulty.colorsFor(level));
        play = Ui.twoLineButton(skin, "primary", T.f("CONTINUE  •  LEVEL {0}", LevelNumbers.format(level)), caption, () -> {
            game.sound().play(SoundManager.Cue.BUTTON);
            game.goToLevel(game.save().currentLevel);
        });
        Button levels = Ui.iconTextButton(skin, "icon-grid", T.r("WORLD"), "ghost", () -> {
            game.sound().play(SoundManager.Cue.BUTTON);
            game.goLevelSelect();
        });
        Button progress = Ui.iconTextButton(skin, "icon-goal", T.r("GOALS"), "ghost", () -> {
            game.sound().play(SoundManager.Cue.BUTTON);
            game.goProgress();
        });
        // The shop button turns gold when a new item has come within reach of the purse.
        boolean shopNews = !Shop.newlyAffordable(save).isEmpty();
        Button shopButton = Ui.iconTextButton(skin, "icon-shop", shopNews ? T.r("SHOP • NEW") : T.r("SHOP"),
            shopNews ? "gold" : "ghost", () -> {
                game.sound().play(SoundManager.Cue.BUTTON);
                game.goShop();
            });
        dailyCard = dailyChallengeCard(save);
        navigation = new Table();
        // Widths follow the words (WORLD / MONDE / OBJETIVOS...); a row longer than the cards
        // above it shrinks its text instead of running off the screen.
        navigation.add(levels).minWidth(130f).height(64f).padRight(10f);
        navigation.add(progress).minWidth(130f).height(64f).padRight(10f);
        navigation.add(shopButton).minWidth(160f).height(64f);
        Ui.shrinkTextToFit(navigation, NAVIGATION_MAX_WIDTH);

        // ---- purse chips ---------------------------------------------------------
        // Stars and coins live in the header; these chips are tubes and cleared levels.
        boostersLabel = new Label(T.f("{0} tubes", save.boosters), skin, "label");
        Label cleared = new Label(T.f("{0} cleared", save.completedCount()), skin, "label");
        chips = new Table();
        chips.add(Ui.chip(skin, "icon-tube", Palette.PRIMARY, boostersLabel)).padRight(10f);
        chips.add(Ui.chip(skin, "icon-check", Palette.SUCCESS, cleared));

        // ---- daily reward calendar --------------------------------------------------
        rewardCard = dailyRewardCard(save);
        arrange(isLandscape());

        // Entrance: top to bottom, one beat apart; then the main action breathes gently.
        Motion.cascade(bar, brand, play, dailyCard, navigation, chips, rewardCard);
        Motion.breathe(play);

        root.getColor().a = 0f;
        root.addAction(Actions.fadeIn(0.28f, Interpolation.fade));

        if (game.reminders().consumeOpenDailyRequest()) {
            // Opened by tapping a reminder: go straight to what it was about.
            stage.addAction(Actions.run(game::goToDailyChallenge));
        } else if (game.reminders().isSupported() && game.takeReminderAskSlot()
            && ReminderAskPolicy.shouldAsk(save.sessions, save.reminderAsks,
                save.reminderNextAskDay, LocalDate.now().toEpochDay(), save.remindersOn,
                save.reminderSystemDenied)) {
            // Calm moment, returning player: explain first, then (on Yes) the system prompt.
            stage.addAction(Actions.delay(0.7f, Actions.run(this::showReminderAsk)));
        }
    }

    /** The soft-ask card: why, what, how often - then the system prompt only after "Yes". */
    private void showReminderAsk() {
        com.badlogic.gdx.scenes.scene2d.ui.Dialog dialog = Ui.dialog(skin, T.r("Daily puzzle reminder"),
            choice -> {
                if ("yes".equals(choice)) {
                    game.enableReminders(on -> { });
                } else {
                    game.save().declineReminderAsk(
                        ReminderAskPolicy.nextAskAfterDecline(LocalDate.now().toEpochDay()));
                }
            });
        dialog.getContentTable().add(Ui.paragraph(skin,
            T.r("Want a nudge when you leave a level or today's puzzle unfinished, and a few gentle "
                + "reminders during the day? Never at night, and you can switch it off any time."),
            "dim", 420f))
            .width(420f).padTop(6f).row();
        dialog.button(Ui.button(skin, T.r("NOT NOW"), "ghost", () -> { }), "no");
        dialog.button(Ui.button(skin, T.r("YES, REMIND ME"), "primary", () -> { }), "yes");
        dialog.show(stage);
    }

    // ------------------------------------------------------------------ layout
    /**
     * Portrait: one 500-unit column, the reward calendar pinned to the bottom. Landscape: the
     * brand, the main action and the navigation on the left, the two daily cards on the right,
     * both columns vertically centred, so nothing is pushed off an 800-unit-high screen.
     */
    private void arrange(boolean landscape) {
        if (arrangedLandscape != null && arrangedLandscape == landscape) {
            return;
        }
        arrangedLandscape = landscape;
        root.clearChildren();
        if (!landscape) {
            root.pad(14f, 36f, 18f, 36f);
            root.add(bar).growX().row();
            root.add(brand).row();
            root.add(play).width(500f).padTop(18f).row();
            root.add(dailyCard).width(500f).padTop(14f).row();
            root.add(navigation).padTop(2f).row();
            root.add(chips).padTop(4f).row();
            root.add().expandY().row();
            root.add(rewardCard).width(500f).row();
            return;
        }
        root.pad(14f, 48f, 18f, 48f);
        root.add(bar).growX().colspan(2).row();

        Table left = new Table();
        left.add(brand).row();
        left.add(play).width(520f).padTop(18f).row();
        left.add(navigation).padTop(12f).row();
        left.add(chips).padTop(10f).row();

        Table right = new Table();
        right.add(dailyCard).width(520f).row();
        right.add(rewardCard).width(520f).padTop(18f).row();

        root.add(left).expand().center().padRight(24f);
        root.add(right).expand().center().padLeft(24f).row();
    }

    @Override
    protected void onResized(float worldWidth, float worldHeight) {
        arrange(isLandscape());
    }

    // ------------------------------------------------------------ daily puzzle
    private Table dailyChallengeCard(SaveData save) {
        LocalDate today = DailyChallenge.todayUtc();
        String dateKey = DailyChallenge.dateKey(today);
        boolean completedToday = save.hasWonDailyChallenge(dateKey);
        int streak = activeDailyStreak(save, today);

        Table card = Ui.card(skin);
        Table heading = new Table();
        Table copy = new Table();
        copy.add(new Label(T.r("TODAY'S PUZZLE"), skin, "label")).left().row();
        copy.add(new Label(T.r("Same daily puzzle for everyone"), skin, "caption"))
            .left().padTop(1f);
        heading.add(copy).left().expandX();
        Label date = new Label(T.show(today.format(DateTimeFormatter.ofPattern("d MMM", new Locale(T.language().code)))),
            skin, "caption");
        date.setColor(Palette.GOLD);
        heading.add(date).right();
        card.add(heading).growX().row();

        Table streakRow = new Table();
        for (int day = 0; day < GameConfig.DAILY_PALETTE_STREAK; day++) {
            Image dot = new Image(skin.newDrawable("circle",
                day < streak ? Palette.GOLD : Palette.SURFACE_STRONG));
            streakRow.add(dot).size(12f).padRight(5f);
        }
        String streakText = save.dailyPaletteUnlocked
            ? T.r("ALT COLOURS UNLOCKED")
            : T.f("{0} / {1} DAYS TO NEW COLOURS", streak, GameConfig.DAILY_PALETTE_STREAK);
        streakRow.add(new Label(streakText, skin, "caption")).left().padLeft(4f).expandX();
        streakRow.add(new Label(completedToday ? T.r("CLAIMED") : "+" + GameConfig.DAILY_CHALLENGE_COINS,
            skin, "gold")).right();
        Ui.shrinkTextToFit(streakRow, 440f); // card is 500 wide, 26 padding each side
        card.add(streakRow).growX().padTop(14f).row();

        TextButton playDaily = Ui.button(skin,
            completedToday ? T.r("REPLAY PUZZLE") : T.r("PLAY DAILY"),
            "gold", () -> {
                game.sound().play(SoundManager.Cue.BUTTON);
                game.goToDailyChallenge();
            });
        dailyBonusButton = Ui.button(skin,
            save.hasClaimedDailyRewardedBonus(dateKey)
                ? T.r("BONUS CLAIMED") : T.f("WATCH  •  +{0}", GameConfig.DAILY_REWARDED_BONUS_COINS),
            "ghost", this::claimDailyBonus);
        dailyBonusButton.setDisabled(save.hasClaimedDailyRewardedBonus(dateKey));
        Table actions = new Table();
        actions.add(playDaily).growX().height(68f).padRight(10f);
        actions.add(dailyBonusButton).growX().height(68f);
        card.add(actions).growX().padTop(16f);

        card.getColor().a = 0f;
        card.addAction(Actions.sequence(
            Actions.delay(0.08f),
            Actions.fadeIn(0.22f, Interpolation.fade)));
        return card;
    }

    private void claimDailyBonus() {
        LocalDate today = DailyChallenge.todayUtc();
        String dateKey = DailyChallenge.dateKey(today);
        SaveData save = game.save();
        if (save.hasClaimedDailyRewardedBonus(dateKey)) {
            refreshDailyBonusButton();
            return;
        }
        if (!game.ads().isRewardedReady()) {
            Ui.toast(stage, skin, T.r("Reward video isn't ready yet. Try again later."), Palette.GOLD);
            return;
        }

        game.sound().play(SoundManager.Cue.BUTTON);
        dailyBonusButton.setDisabled(true);
        game.pacing().onFullScreenAdShown();
        game.ads().showRewarded(new AdProvider.RewardListener() {
            @Override
            public void onRewarded(int coins) {
                String earnedDate = DailyChallenge.dateKey(DailyChallenge.todayUtc());
                if (save.claimDailyRewardedBonus(earnedDate)) {
                    if (game.getScreen() == HomeScreen.this) {
                        refreshPurse();
                        refreshDailyBonusButton();
                        game.sound().play(SoundManager.Cue.COIN);
                        Ui.toast(stage, skin,
                            T.f("+{0} coins added", GameConfig.DAILY_REWARDED_BONUS_COINS),
                            Palette.SUCCESS);
                    }
                } else if (game.getScreen() == HomeScreen.this) {
                    refreshDailyBonusButton();
                }
            }

            @Override
            public void onFailed() {
                if (game.getScreen() == HomeScreen.this) {
                    dailyBonusButton.setDisabled(false);
                    Ui.toast(stage, skin,
                        T.r("No reward this time. You can try again later."), Palette.GOLD);
                }
            }
        });
    }

    private void refreshDailyBonusButton() {
        String today = DailyChallenge.dateKey(DailyChallenge.todayUtc());
        boolean claimed = game.save().hasClaimedDailyRewardedBonus(today);
        dailyBonusButton.setText(claimed
            ? T.r("BONUS CLAIMED") : T.f("WATCH  •  +{0}", GameConfig.DAILY_REWARDED_BONUS_COINS));
        dailyBonusButton.setDisabled(claimed);
    }

    private static int activeDailyStreak(SaveData save, LocalDate today) {
        if (save.dailyWinStreak <= 0 || save.lastDailyWinDate == null
            || save.lastDailyWinDate.isEmpty()) {
            return 0;
        }
        String yesterday = DailyChallenge.dateKey(today.minusDays(1));
        String todayKey = DailyChallenge.dateKey(today);
        return save.lastDailyWinDate.equals(todayKey) || save.lastDailyWinDate.equals(yesterday)
            ? Math.min(save.dailyWinStreak, GameConfig.DAILY_PALETTE_STREAK) : 0;
    }

    // ------------------------------------------------------------ daily reward
    /** Seven dots, today's lit, and one button that says exactly what it pays. */
    private Table dailyRewardCard(SaveData save) {
        Table card = Ui.card(skin);
        card.pad(18f, 24f, 18f, 24f);
        Table heading = new Table();
        // Title over caption: side by side they overflowed the card in longer languages.
        heading.defaults().left();
        heading.add(new Label(T.r("DAILY REWARD"), skin, "label")).expandX().row();
        heading.add(new Label(T.r("Come back every day, it grows"), skin, "caption")).padTop(2f);
        card.add(heading).growX().row();
        calendar = new Table();
        card.add(calendar).growX().padTop(12f).row();
        claimButton = Ui.button(skin, "", "gold", this::claimDailyReward);
        card.add(claimButton).growX().height(60f).padTop(14f).row();
        refreshCalendar();
        return card;
    }

    private void refreshCalendar() {
        SaveData save = game.save();
        LocalDate today = LocalDate.now();
        int claimable = DailyRewards.dayFor(save.dailyRewardDate, save.dailyRewardDay, today);
        int shown = DailyRewards.displayDay(save.dailyRewardDate, save.dailyRewardDay, today);
        calendar.clearChildren();
        for (int day = 1; day <= DailyRewards.DAYS; day++) {
            boolean past = day < shown || (claimable == 0 && day == shown);
            boolean isToday = day == shown;
            Table cell = new Table();
            Color dotColor = past ? Palette.GOLD : isToday ? Palette.PRIMARY : Palette.SURFACE_STRONG;
            Image dot = new Image(skin.newDrawable(past ? "icon-check" : "circle", dotColor));
            cell.add(dot).size(past ? 18f : 16f).row();
            Label label = new Label(DailyRewards.boostersFor(day) > 0 ? T.r("tube") : "+" + DailyRewards.coinsFor(day),
                skin, "small");
            label.setColor(isToday ? Palette.TEXT : Palette.TEXT_DIM);
            cell.add(label).padTop(4f);
            if (isToday && claimable > 0) {
                dot.setOrigin(Align.center);
                dot.addAction(Actions.forever(Actions.sequence(
                    Actions.scaleTo(1.3f, 1.3f, 0.6f, Interpolation.sine),
                    Actions.scaleTo(1f, 1f, 0.6f, Interpolation.sine))));
            }
            calendar.add(cell).expandX();
        }
        if (claimable > 0) {
            claimButton.setText(T.f("CLAIM DAY {0}  •  {1}", claimable, DailyRewards.label(claimable)));
            claimButton.setDisabled(false);
            claimButton.getColor().a = 1f;
        } else {
            claimButton.setText(T.f("DAY {0} CLAIMED  •  BACK TOMORROW", shown));
            claimButton.setDisabled(true);
            claimButton.getColor().a = 0.6f;
        }
    }

    private void claimDailyReward() {
        SaveData save = game.save();
        int day = save.claimDailyReward(LocalDate.now());
        if (day == 0) {
            refreshCalendar();
            return;
        }
        game.sound().play(SoundManager.Cue.COIN);
        refreshPurse();
        refreshCalendar();
        Ui.toast(stage, skin, T.f("Day {0}: {1}  •  see you tomorrow", day, DailyRewards.label(day)),
            Palette.SUCCESS);
    }

    private void refreshPurse() {
        SaveData save = game.save();
        // The header purse rolls up to the new total and pops, so a reward is seen landing.
        Motion.countTo(coinsLabel, shownCoins, save.coins, coinChip);
        shownCoins = save.coins;
        boostersLabel.setText(T.f("{0} tubes", save.boosters));
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
