package com.ben.laama.sortpuzzle;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

import com.ben.laama.sortpuzzle.ads.AdPacing;
import com.ben.laama.sortpuzzle.ads.AdProvider;
import com.ben.laama.sortpuzzle.ads.AdProviders;
import com.ben.laama.sortpuzzle.logic.LeaderboardScores;
import com.ben.laama.sortpuzzle.logic.ReminderPlanner;
import com.ben.laama.sortpuzzle.notify.NoOpReminders;
import com.ben.laama.sortpuzzle.notify.Reminders;
import com.ben.laama.sortpuzzle.services.Analytics;
import com.ben.laama.sortpuzzle.services.GameServices;
import com.ben.laama.sortpuzzle.services.NoOpAnalytics;
import com.ben.laama.sortpuzzle.services.NoOpGameServices;
import com.ben.laama.sortpuzzle.services.NoOpShare;
import com.ben.laama.sortpuzzle.services.Share;
import com.ben.laama.sortpuzzle.audio.SoundManager;
import com.ben.laama.sortpuzzle.logic.DailyChallenge;
import com.ben.laama.sortpuzzle.logic.ChallengeRules;
import com.ben.laama.sortpuzzle.save.SaveData;
import com.ben.laama.sortpuzzle.screens.GameScreen;
import com.ben.laama.sortpuzzle.screens.BaseScreen;
import com.ben.laama.sortpuzzle.screens.HomeScreen;
import com.ben.laama.sortpuzzle.screens.LevelSelectScreen;
import com.ben.laama.sortpuzzle.screens.MusicSelectionScreen;
import com.ben.laama.sortpuzzle.screens.ProgressScreen;
import com.ben.laama.sortpuzzle.screens.SettingsScreen;
import com.ben.laama.sortpuzzle.screens.ShopScreen;
import com.ben.laama.sortpuzzle.screens.SplashScreen;
import com.ben.laama.sortpuzzle.ui.Cosmetics;
import com.ben.laama.sortpuzzle.ui.SkinFactory;
import com.ben.laama.sortpuzzle.ui.TubeTextures;

import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * Application entry point and screen router.
 *
 * <p>Owns the things every screen needs — the shared {@link SpriteBatch}, the procedurally
 * built {@link Skin}, the tube texture cache and the player's {@link SaveData} — plus the audio
 * and advertising services.</p>
 */
public final class Main extends Game {

    private final AdProvider ads;
    private final AdPacing pacing = new AdPacing(
        GameConfig.INTERSTITIAL_INITIAL_BREAKS_TO_SKIP,
        GameConfig.INTERSTITIAL_EVERY_N_LEVELS,
        GameConfig.INTERSTITIAL_MIN_INTERVAL_MS, System::currentTimeMillis);

    private SpriteBatch batch;
    private Skin skin;
    private TubeTextures tubes;
    private SaveData save;
    private SoundManager sound;

    private final GameServices services;
    private final Analytics analytics;
    private final Reminders reminders;

    /** Desktop / test entry point: the provider follows {@code AdsConfig}, no services. */
    public Main() {
        this(AdProviders.forDesktop(), null, null);
    }

    /** Platform entry point: the launcher injects the provider it got from {@code AdProviders}. */
    public Main(AdProvider ads) {
        this(ads, null, null);
    }

    /**
     * Platform entry point with optional services. {@code null} for any argument means "none":
     * the inert implementation is used and the game behaves exactly as without it.
     */
    public Main(AdProvider ads, GameServices services, Analytics analytics) {
        this(ads, services, analytics, null);
    }

    /** Platform entry point with reminders; {@code reminders} posts the daily-puzzle notifications. */
    public Main(AdProvider ads, GameServices services, Analytics analytics, Reminders reminders) {
        this(ads, services, analytics, reminders, null);
    }

    /** Full platform entry point; {@code share} opens the platform share sheet. */
    public Main(AdProvider ads, GameServices services, Analytics analytics, Reminders reminders,
                Share share) {
        this.ads = ads == null ? AdProviders.forDesktop() : ads;
        this.services = services == null ? new NoOpGameServices() : services;
        this.analytics = analytics == null ? new NoOpAnalytics() : analytics;
        this.reminders = reminders == null ? new NoOpReminders() : reminders;
        this.share = share == null ? new NoOpShare() : share;
    }

    private final Share share;

    /** Share sheet for result cards; inert off Android. */
    public Share share() {
        return share;
    }

    @Override
    public void create() {
        Gdx.app.setLogLevel(Application.LOG_INFO);
        batch = new SpriteBatch();
        skin = SkinFactory.create();
        tubes = new TubeTextures();
        save = SaveData.load();
        save.countSession();
        // The player is here: whatever was scheduled is now stale, and shown ones are noise.
        reminders.cancelAll();
        Cosmetics.apply(skin, tubes, save);
        sound = new SoundManager(save);
        sound.startMusic();
        ads.initialize();
        // A player who signs in later than their first wins still gets their bests on the boards.
        services.initialize(signedIn -> {
            if (signedIn) {
                syncLeaderboards();
            }
        });
        setScreen(save.musicTrack < 0
            ? new MusicSelectionScreen(this, true)
            : new SplashScreen(this));
    }

    // ------------------------------------------------------------- services
    public SpriteBatch batch() {
        return batch;
    }

    public Skin skin() {
        return skin;
    }

    public TubeTextures tubes() {
        return tubes;
    }

    public SaveData save() {
        return save;
    }

    public SoundManager sound() {
        return sound;
    }

    public AdProvider ads() {
        return ads;
    }

    /** Sign-in and leaderboards; inert off Android or without a Play Games configuration. */
    public GameServices services() {
        return services;
    }

    /** Product analytics; inert off Android or without a Firebase configuration. */
    public Analytics analytics() {
        return analytics;
    }

    /** Daily-puzzle local notifications; inert off Android. */
    public Reminders reminders() {
        return reminders;
    }

    private boolean reminderAskTaken;

    /** @return {@code true} the first time per session: one soft-ask per session at most */
    public boolean takeReminderAskSlot() {
        if (reminderAskTaken) return false;
        reminderAskTaken = true;
        return true;
    }

    /**
     * Turns reminders on, asking the system first when needed. After a "Don't allow" the prompt
     * cannot be shown again, so the app's notification settings open instead; {@link #resume}
     * switches reminders on if the player allows them there.
     *
     * @param done receives whether reminders are now on (render thread)
     */
    public void enableReminders(java.util.function.Consumer<Boolean> done) {
        if (reminders.hasPermission()) {
            save.setRemindersOn(true);
            done.accept(true);
        } else if (save.reminderSystemDenied) {
            reminders.openSettings();
            done.accept(false);
        } else {
            reminders.requestPermission(granted -> {
                if (granted) {
                    save.setRemindersOn(true);
                } else {
                    save.markReminderSystemDenied();
                }
                done.accept(granted);
            });
        }
    }

    // ------------------------------------------------------------- lifecycle
    /** Leaving the game: plan the next reminders from what the player has (not) done today. */
    @Override
    public void pause() {
        super.pause();
        if (save != null && save.remindersOn && reminders.hasPermission()) {
            reminders.schedule(ReminderPlanner.plan(ZonedDateTime.now(), save.lastDailyWinDate,
                save.lastDailyStartDate, save.dailyWinStreak, save.currentLevel));
        }
    }

    /** Back in the game: cancel the plan; a tapped reminder opens today's puzzle. */
    @Override
    public void resume() {
        super.resume();
        reminders.cancelAll();
        if (save != null && !save.remindersOn && save.reminderSystemDenied
            && reminders.hasPermission()) {
            save.setRemindersOn(true); // allowed from system settings while we were away
        }
        Screen current = getScreen();
        if (current instanceof HomeScreen || current instanceof GameScreen
            || current instanceof SettingsScreen || current instanceof LevelSelectScreen
            || current instanceof ProgressScreen || current instanceof ShopScreen) {
            if (reminders.consumeOpenDailyRequest()) {
                goToDailyChallenge();
            }
        } // splash / first-launch music picker: HomeScreen picks the request up when it opens
    }

    /**
     * Pushes the saved bests to the all-time boards (highest level, total stars). Called after
     * any successful sign-in; per-win submissions happen in {@code GameScreen}. Inert when
     * signed out. The daily board is only ever fed by an actual daily win.
     */
    public void syncLeaderboards() {
        if (save == null || !services.isSignedIn()) {
            return;
        }
        services.submitScore(GameServices.Leaderboard.HIGHEST_LEVEL,
            LeaderboardScores.highestLevel(save.highestUnlocked));
        services.submitScore(GameServices.Leaderboard.TOTAL_STARS,
            LeaderboardScores.totalStars(save.totalStars()));
    }

    /** Session-wide interstitial frequency caps. */
    public AdPacing pacing() {
        return pacing;
    }

    // ----------------------------------------------------------- navigation
    public void goHome() {
        switchTo(new HomeScreen(this));
    }

    public void goLevelSelect() {
        switchTo(new LevelSelectScreen(this));
    }

    public void goSettings() {
        switchTo(new SettingsScreen(this));
    }

    public void goMusicSelection() {
        switchTo(new MusicSelectionScreen(this, false));
    }

    public void finishMusicSelection(boolean firstLaunch) {
        switchTo(firstLaunch ? new SplashScreen(this) : new SettingsScreen(this));
    }

    public void goToLevel(int levelIndex) {
        switchTo(new GameScreen(this, levelIndex));
    }

    public void goToDailyChallenge() {
        goToDailyChallenge(DailyChallenge.todayUtc());
    }

    public void goToDailyChallenge(LocalDate date) {
        switchTo(new GameScreen(this, date));
    }

    public void goProgress() {
        switchTo(new ProgressScreen(this));
    }

    /** The cosmetic shop: palettes, finishes, tube styles and themes, paid with coins. */
    public void goShop() {
        switchTo(new ShopScreen(this));
    }

    public void goToChallenge(ChallengeRules rules) {
        switchTo(new GameScreen(this, save.currentLevel, rules));
    }

    public void goToChallenges() {
        goProgress();
    }

    /**
     * Replaces the active screen and disposes the old one.
     *
     * <p>Deferred to the start of the next frame so it is always safe to call from inside a
     * scene2d listener — the outgoing stage is never disposed mid-dispatch.</p>
     */
    public void switchTo(final Screen next) {
        boolean leaving = pendingScreen != null;
        if (leaving && pendingScreen != next) {
            pendingScreen.dispose(); // superseded before it was ever shown
        }
        pendingScreen = next;
        if (leaving) {
            return; // the fade already running will land on the latest target
        }
        Screen current = getScreen();
        if (current instanceof BaseScreen) {
            // Exit is a short fade-out, so screens never cut; the new one fades in on show().
            ((BaseScreen) current).leave(this::swapToPending);
        } else {
            swapToPending();
        }
    }

    private Screen pendingScreen;

    private void swapToPending() {
        Gdx.app.postRunnable(() -> {
            Screen previous = getScreen();
            Screen next = pendingScreen;
            pendingScreen = null;
            setScreen(next);
            if (previous != null) {
                previous.dispose();
            }
        });
    }

    @Override
    public void dispose() {
        Screen current = getScreen();
        if (current != null) {
            current.dispose();
        }
        setScreen(null);
        if (sound != null) sound.dispose();
        if (tubes != null) tubes.dispose();
        if (skin != null) skin.dispose();
        if (batch != null) batch.dispose();
        ads.dispose();
        super.dispose();
    }
}
