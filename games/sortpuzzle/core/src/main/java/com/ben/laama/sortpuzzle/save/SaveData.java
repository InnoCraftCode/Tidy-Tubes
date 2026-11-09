package com.ben.laama.sortpuzzle.save;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

import com.ben.laama.sortpuzzle.GameConfig;
import com.ben.laama.sortpuzzle.logic.Achievement;
import com.ben.laama.sortpuzzle.logic.Assists;
import com.ben.laama.sortpuzzle.logic.Cosmetic;
import com.ben.laama.sortpuzzle.logic.WeeklyGoals;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

/**
 * Persistent player progress. Serialised as a single JSON blob inside libGDX
 * {@link Preferences}, which keeps the on-disk format portable across desktop and Android.
 *
 * <p>Fields are public and non-final because libGDX {@link Json} writes them reflectively;
 * {@code transient} members are skipped by the serialiser. New fields get a default when an
 * older blob is read, so adding one never needs a migration.</p>
 *
 * <p>Nothing here leaves the device: no account, no identifier, no network.</p>
 */
public class SaveData {

    private static final String PREFS_NAME = "com.ben.laama.sortpuzzle.save";
    private static final String KEY_PAYLOAD = "payload";

    /** Bumped whenever the layout changes in a way that needs migration. */
    public static final int CURRENT_VERSION = 1;

    // ------------------------------------------------------------------ state
    public int version = CURRENT_VERSION;
    /** Level the "Play" button resumes at. 1-based. */
    public int currentLevel = 1;
    /** Highest level the player is allowed to open. 1-based. */
    public int highestUnlocked = 1;
    public int coins = 0;
    /** Permanent extra-tube boosters bought with coins. */
    public int boosters = 0;
    /** Bought or earned undos beyond the free ones each level starts with. */
    public int undos = 0;
    /** Bought or earned resets beyond the free one each level starts with. */
    public int resets = 0;
    /** UTC epoch millis of the last confirmed rewarded extra-tube bonus, or zero if none. */
    public long lastRewardedTubeAtMillis = 0L;
    /**
     * Legacy "all audio off" flag from saves written before 2026-11-03. Folded into
     * {@link #effectsOff} + {@link #musicOff} by {@link #migrateAudioFlags()} on load and always
     * false afterwards. Kept as a field so old JSON still deserialises into it.
     */
    public boolean muted = false;
    /** Sound effects off (independent of the music). */
    public boolean effectsOff = false;
    /** Music bed off (independent of the effects). */
    public boolean musicOff = false;
    /** Selected original ambient loop; -1 means the first-launch choice is still pending. */
    public int musicTrack = -1;
    /** Interface language code ({@code i18n.Language}); "" until the first-launch picker. */
    public String language = "";
    public boolean vibrate = true;
    /** The "how to play" card was shown once. */
    public boolean tutorialSeen = false;
    /** Best move count per level, index = level - 1, {@code 0} = never finished. */
    public int[] bestMoves = new int[0];
    /** Best star rating per level, index = level - 1. */
    public int[] bestStars = new int[0];
    /** Latest daily challenge date claimed, in ISO UTC date format. */
    public String lastDailyWinDate = "";
    /** Latest optional rewarded bonus date claimed, in ISO UTC date format. */
    public String lastDailyRewardedBonusDate = "";
    /** Local ISO date of the shop's "free coins" videos, and how many were rewarded that day. */
    public String adCoinsDate = "";
    public int adCoinsCount = 0;
    public int dailyWinStreak = 0;
    /** ISO UTC date of the last daily puzzle the player opened (to tell "started" from "not played"). */
    public String lastDailyStartDate = "";
    /** Daily-puzzle reminders wanted by the player (the system permission is checked separately). */
    public boolean remindersOn = false;
    /** In-game "Turn on reminders?" cards answered "Not now". */
    public int reminderAsks = 0;
    /** Earliest local epoch day for the next soft-ask. */
    public long reminderNextAskDay = 0L;
    /** The system notification prompt was answered "Don't allow": never soft-ask again. */
    public boolean reminderSystemDenied = false;
    /** Launches so far, this one included. */
    public int sessions = 0;
    public boolean dailyPaletteUnlocked = false;
    /** Selected ball palette: 0 is the default palette, 1 is the daily reward. */
    public int ballPalette = 0;
    public boolean colorLabels = false;
    /** Shop items bought with coins, by {@link Cosmetic#id()}. Free items are never listed. */
    public String[] ownedCosmetics = new String[0];
    /** Equipped item id per category; {@code ""} means the category's free default. */
    public String equippedBallPalette = "";
    public String equippedBallFinish = "";
    public String equippedTube = "";
    public String equippedTheme = "";
    /** Shop items already announced as affordable, so each is announced once. */
    public String[] shopNotified = new String[0];
    /** ISO local date of the last daily-reward claim, "" if never. */
    public String dailyRewardDate = "";
    /** Calendar day (1..7) claimed on that date, 0 if never. */
    public int dailyRewardDay = 0;
    public String weeklyKey = "";
    public int weeklyCampaignWins = 0;
    public int weeklyParFinishes = 0;
    public int weeklyMoves = 0;
    public int weeklyClaimedMask = 0;
    public int achievementMask = 0;
    public int campaignParFinishes = 0;
    public int challengeWins = 0;

    private transient Preferences prefs;

    // ------------------------------------------------------------- lifecycle
    /** Reads the save from disk, falling back to a fresh profile if it is missing or corrupt. */
    public static SaveData load() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        String payload = prefs.getString(KEY_PAYLOAD, null);
        boolean hadSavedProfile = payload != null && !payload.isEmpty();
        SaveData data = null;
        if (payload != null && !payload.isEmpty()) {
            try {
                Json json = new Json();
                // A newer version may have written fields this build does not know (staged
                // rollout rolled back, downgrade): skip them instead of wiping the profile.
                json.setIgnoreUnknownFields(true);
                data = json.fromJson(SaveData.class, payload);
            } catch (RuntimeException corrupt) {
                Gdx.app.error("SaveData", "Corrupt save discarded: " + corrupt.getMessage());
            }
        }
        if (data == null) {
            data = new SaveData();
        }
        data.prefs = prefs;
        data.sanitise();
        if (hadSavedProfile && data.musicTrack < 0) {
            data.musicTrack = 0;
            data.flush();
        }
        if (data.evaluateAchievements() > 0) {
            data.flush();
        }
        return data;
    }

    /** Writes the save to disk immediately. Cheap enough to call on every meaningful change. */
    public void flush() {
        if (prefs == null) {
            return; // headless / unit-test instance
        }
        Json json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        prefs.putString(KEY_PAYLOAD, json.toJson(this));
        prefs.flush();
    }

    /** Wipes all progress. Settings (sound, music) are kept. */
    public void reset() {
        version = CURRENT_VERSION;
        currentLevel = 1;
        highestUnlocked = 1;
        coins = 0;
        boosters = 0;
        undos = 0;
        resets = 0;
        lastRewardedTubeAtMillis = 0L;
        tutorialSeen = false;
        bestMoves = new int[0];
        bestStars = new int[0];
        lastDailyWinDate = "";
        lastDailyStartDate = "";
        lastDailyRewardedBonusDate = "";
        adCoinsDate = "";
        adCoinsCount = 0;
        dailyWinStreak = 0;
        dailyPaletteUnlocked = false;
        ballPalette = 0;
        ownedCosmetics = new String[0];
        equippedBallPalette = "";
        equippedBallFinish = "";
        equippedTube = "";
        equippedTheme = "";
        shopNotified = new String[0];
        dailyRewardDate = "";
        dailyRewardDay = 0;
        weeklyKey = "";
        weeklyCampaignWins = 0;
        weeklyParFinishes = 0;
        weeklyMoves = 0;
        weeklyClaimedMask = 0;
        achievementMask = 0;
        campaignParFinishes = 0;
        challengeWins = 0;
        flush();
    }

    private void sanitise() {
        version = CURRENT_VERSION;
        if (currentLevel < 1) currentLevel = 1;
        if (highestUnlocked < 1) highestUnlocked = 1;
        if (highestUnlocked > GameConfig.MAX_LEVEL) highestUnlocked = GameConfig.MAX_LEVEL;
        if (currentLevel > highestUnlocked) currentLevel = highestUnlocked;
        if (coins < 0) coins = 0;
        if (boosters < 0) boosters = 0;
        if (undos < 0) undos = 0;
        if (resets < 0) resets = 0;
        if (lastRewardedTubeAtMillis < 0) lastRewardedTubeAtMillis = 0L;
        if (bestMoves == null) bestMoves = new int[0];
        if (bestStars == null) bestStars = new int[0];
        if (dailyWinStreak < 0) dailyWinStreak = 0;
        if (musicTrack < -1 || musicTrack >= GameConfig.RELAXING_MUSIC_TRACK_COUNT) {
            musicTrack = -1;
        }
        migrateAudioFlags();
        if (language == null || com.ben.laama.sortpuzzle.i18n.Language.fromCode(language) == null) {
            language = "";
        }
        if (lastDailyWinDate == null) lastDailyWinDate = "";
        if (lastDailyStartDate == null) lastDailyStartDate = "";
        reminderAsks = Math.max(0, reminderAsks);
        sessions = Math.max(0, sessions);
        if (lastDailyRewardedBonusDate == null) lastDailyRewardedBonusDate = "";
        if (adCoinsDate == null) adCoinsDate = "";
        adCoinsCount = Math.max(0, Math.min(GameConfig.AD_COINS_PER_DAY, adCoinsCount));
        if (weeklyKey == null) weeklyKey = "";
        weeklyCampaignWins = Math.max(0, weeklyCampaignWins);
        weeklyParFinishes = Math.max(0, weeklyParFinishes);
        weeklyMoves = Math.max(0, weeklyMoves);
        campaignParFinishes = Math.max(0, campaignParFinishes);
        challengeWins = Math.max(0, challengeWins);
        if (ballPalette != 0 && !isCosmeticUnlocked(ballPalette)) ballPalette = 0;
        if (ballPalette < 0 || ballPalette > 2 || !isCosmeticUnlocked(ballPalette)) ballPalette = 0;
        migrateCosmetics();
    }

    // --------------------------------------------------------------- cosmetics
    /**
     * Fills the 1.1.0 cosmetic fields from an older save: nulls become empty, and the integer
     * palette choice (0..2) becomes the equipped palette id. Idempotent.
     *
     * @return whether the legacy palette was carried over
     */
    boolean migrateCosmetics() {
        if (ownedCosmetics == null) ownedCosmetics = new String[0];
        if (shopNotified == null) shopNotified = new String[0];
        if (equippedBallFinish == null) equippedBallFinish = "";
        if (equippedTube == null) equippedTube = "";
        if (equippedTheme == null) equippedTheme = "";
        if (equippedBallPalette == null) equippedBallPalette = "";
        if (equippedBallPalette.isEmpty() && ballPalette != 0) {
            equippedBallPalette = Cosmetic.legacyBallPalette(ballPalette).id();
            return true;
        }
        return false;
    }

    /** @return whether a shop item was bought (free and progression items are not listed) */
    public boolean ownsCosmetic(String id) {
        return contains(ownedCosmetics, id);
    }

    public void addOwnedCosmetic(String id) {
        if (id == null || id.isEmpty() || ownsCosmetic(id)) return;
        ownedCosmetics = append(ownedCosmetics, id);
        flush();
    }

    /** @return the equipped item id for the category, {@code ""} for the free default */
    public String equippedCosmetic(Cosmetic.Category category) {
        switch (category) {
            case BALL_PALETTE:
                return equippedBallPalette;
            case BALL_FINISH:
                return equippedBallFinish;
            case TUBE:
                return equippedTube;
            default:
                return equippedTheme;
        }
    }

    /**
     * Records the equipped item. Ownership is the shop's business
     * ({@link com.ben.laama.sortpuzzle.logic.Shop#equip}); this only stores the choice.
     */
    public void setEquippedCosmetic(Cosmetic.Category category, String id) {
        String value = id == null ? "" : id;
        switch (category) {
            case BALL_PALETTE:
                equippedBallPalette = value;
                // Keep the pre-1.1.0 field in step so a downgraded build still reads the choice.
                ballPalette = Cosmetic.BALL_PASTEL.id().equals(value) ? 1
                    : Cosmetic.BALL_NEON.id().equals(value) ? 2 : 0;
                break;
            case BALL_FINISH:
                equippedBallFinish = value;
                break;
            case TUBE:
                equippedTube = value;
                break;
            default:
                equippedTheme = value;
                break;
        }
        flush();
    }

    public boolean isShopNotified(String id) {
        return contains(shopNotified, id);
    }

    /** The player has been told this item is affordable; it will not be announced again. */
    public void markShopNotified(String id) {
        if (id == null || id.isEmpty() || isShopNotified(id)) return;
        shopNotified = append(shopNotified, id);
        flush();
    }

    private static boolean contains(String[] values, String id) {
        if (values == null || id == null) return false;
        for (String value : values) {
            if (id.equals(value)) return true;
        }
        return false;
    }

    private static String[] append(String[] values, String id) {
        String[] grown = Arrays.copyOf(values, values.length + 1);
        grown[values.length] = id;
        return grown;
    }

    // ---------------------------------------------------------------- queries
    public boolean isUnlocked(int level) {
        return level >= 1 && level <= highestUnlocked;
    }

    public boolean isCompleted(int level) {
        return bestMovesFor(level) > 0;
    }

    /** @return the best (lowest) move count recorded for {@code level}, or {@code 0}. */
    public int bestMovesFor(int level) {
        int i = level - 1;
        return (i >= 0 && i < bestMoves.length) ? bestMoves[i] : 0;
    }

    /** @return how many levels have been cleared at least once */
    public int completedCount() {
        int count = 0;
        for (int moves : bestMoves) {
            if (moves > 0) count++;
        }
        return count;
    }

    public int bestStarsFor(int level) {
        int i = level - 1;
        return i >= 0 && i < bestStars.length ? bestStars[i] : 0;
    }

    public int totalStars() {
        int total = 0;
        for (int stars : bestStars) {
            total += Math.max(0, Math.min(3, stars));
        }
        return total;
    }

    public boolean hasWonDailyChallenge(String date) {
        return date != null && date.equals(lastDailyWinDate);
    }

    public boolean hasClaimedDailyRewardedBonus(String date) {
        return date != null && date.equals(lastDailyRewardedBonusDate);
    }

    /**
     * Pays the optional rewarded bonus once per UTC date, and only after the ad reward callback.
     *
     * @return whether the bonus was newly claimed
     */
    public boolean claimDailyRewardedBonus(String date) {
        final LocalDate claimDate;
        try {
            claimDate = LocalDate.parse(date);
        } catch (DateTimeParseException | NullPointerException invalid) {
            throw new IllegalArgumentException("date must be an ISO-8601 calendar date", invalid);
        }
        if (!lastDailyRewardedBonusDate.isEmpty()) {
            LocalDate previous;
            try {
                previous = LocalDate.parse(lastDailyRewardedBonusDate);
            } catch (DateTimeParseException invalidSave) {
                Gdx.app.error("SaveData", "Invalid saved rewarded bonus date");
                previous = null;
            }
            if (previous != null && !claimDate.isAfter(previous)) {
                return false;
            }
        }
        lastDailyRewardedBonusDate = claimDate.toString();
        coins += GameConfig.DAILY_REWARDED_BONUS_COINS;
        flush();
        return true;
    }

    /**
     * Pays a daily reward once, advances a consecutive UTC-day streak and unlocks the palette
     * at the configured streak. Older or duplicate dates cannot reset or repay the streak.
     *
     * @return coins awarded, or zero when this date was already claimed / was older
     */
    public int recordDailyWin(String date) {
        final LocalDate wonDate;
        try {
            wonDate = LocalDate.parse(date);
        } catch (DateTimeParseException | NullPointerException invalid) {
            throw new IllegalArgumentException("date must be an ISO-8601 calendar date", invalid);
        }
        if (!lastDailyWinDate.isEmpty()) {
            LocalDate previous;
            try {
                previous = LocalDate.parse(lastDailyWinDate);
            } catch (DateTimeParseException invalidSave) {
                Gdx.app.error("SaveData", "Invalid saved daily date; restarting daily streak");
                previous = null;
            }
            if (previous != null && !wonDate.isAfter(previous)) {
                return 0;
            }
            dailyWinStreak = previous != null && wonDate.equals(previous.plusDays(1))
                ? dailyWinStreak + 1 : 1;
        } else {
            dailyWinStreak = 1;
        }
        lastDailyWinDate = wonDate.toString();
        coins += GameConfig.DAILY_CHALLENGE_COINS;
        if (dailyWinStreak >= GameConfig.DAILY_PALETTE_STREAK) {
            dailyPaletteUnlocked = true;
        }
        flush();
        return GameConfig.DAILY_CHALLENGE_COINS;
    }

    /** Pre-1.1.0 palette selection by index; kept for the progression unlocks. */
    public void setBallPalette(int palette) {
        if (!isCosmeticUnlocked(palette)) {
            throw new IllegalArgumentException("palette is not unlocked");
        }
        setEquippedCosmetic(Cosmetic.Category.BALL_PALETTE,
            palette == 0 ? "" : Cosmetic.legacyBallPalette(palette).id());
    }

    public boolean isCosmeticUnlocked(int palette) {
        if (palette == 0) return true;
        if (palette == 1) return dailyPaletteUnlocked;
        return palette == 2 && totalStars() >= GameConfig.STAR_PALETTE_UNLOCK;
    }

    public void setColorLabels(boolean enabled) {
        colorLabels = enabled;
        flush();
    }

    public void beginWeeklyGoals(LocalDate date) {
        String key = WeeklyGoals.weekKey(date);
        if (!key.equals(weeklyKey)) {
            weeklyKey = key;
            weeklyCampaignWins = 0;
            weeklyParFinishes = 0;
            weeklyMoves = 0;
            weeklyClaimedMask = 0;
            flush();
        }
    }

    public int weeklyProgress(int goal) {
        return WeeklyGoals.progress(goal, weeklyCampaignWins, weeklyParFinishes, weeklyMoves);
    }

    /**
     * Adds campaign activity to this week's goals, then claims each newly completed goal once.
     * @return all coins awarded for goals completed by this event
     */
    public int recordWeeklyCampaignWin(LocalDate date, int moves, boolean atPar) {
        return recordWeeklyCampaignResult(date, moves, true, atPar);
    }

    public int recordWeeklyCampaignResult(LocalDate date, int moves, boolean firstClear,
                                          boolean atPar) {
        if (moves < 1) throw new IllegalArgumentException("moves must be positive");
        beginWeeklyGoals(date);
        if (firstClear) {
            weeklyCampaignWins++;
        }
        weeklyMoves += moves;
        if (atPar) weeklyParFinishes++;
        int reward = claimCompletedWeeklyGoals();
        flush();
        return reward;
    }

    public int claimCompletedWeeklyGoals() {
        int earned = 0;
        for (int goal = 0; goal < WeeklyGoals.GOAL_COUNT; goal++) {
            int bit = 1 << goal;
            if ((weeklyClaimedMask & bit) == 0
                && weeklyProgress(goal) >= WeeklyGoals.target(goal)) {
                weeklyClaimedMask |= bit;
                earned += GameConfig.WEEKLY_GOAL_REWARD;
            }
        }
        coins += earned;
        if (earned > 0) flush();
        return earned;
    }

    public boolean isAchievementUnlocked(Achievement achievement) {
        return (achievementMask & achievement.bit()) != 0;
    }

    public int achievementProgress(Achievement achievement) {
        return achievement.progress(completedCount(), campaignParFinishes, totalStars(), challengeWins);
    }

    public int unlockedAchievementCount() {
        return Integer.bitCount(achievementMask);
    }

    private int evaluateAchievements() {
        int earned = 0;
        for (Achievement achievement : Achievement.values()) {
            if (!isAchievementUnlocked(achievement)
                && achievementProgress(achievement) >= achievement.target()) {
                achievementMask |= achievement.bit();
                earned += achievement.reward();
            }
        }
        coins += earned;
        return earned;
    }

    public int recordChallengeWin() {
        challengeWins++;
        int earned = evaluateAchievements();
        flush();
        return earned;
    }

    // ---------------------------------------------------------------- mutation
    /**
     * Records a win, unlocks the next level and pays out coins.
     *
     * @param level     the level just completed, 1-based
     * @param moves     how many moves the player used
     * @param parMoves  the optimal move count, or {@code 0} when unknown
     * @return the number of coins awarded
     */
    public int recordWin(int level, int moves, int parMoves) {
        int i = level - 1;
        if (i >= bestMoves.length) {
            bestMoves = Arrays.copyOf(bestMoves, i + 16);
        }
        if (i >= bestStars.length) {
            bestStars = Arrays.copyOf(bestStars, i + 16);
        }
        boolean firstClear = bestMoves[i] == 0;
        if (firstClear || moves < bestMoves[i]) {
            bestMoves[i] = moves;
        }
        bestStars[i] = Math.max(bestStars[i], com.ben.laama.sortpuzzle.logic.LevelRating
            .starsFor(moves, parMoves));

        int next = Math.min(level + 1, GameConfig.MAX_LEVEL); // the last level has no successor
        if (next > highestUnlocked) {
            highestUnlocked = next;
        }
        currentLevel = Math.min(next, highestUnlocked);

        int earned = GameConfig.COINS_PER_WIN;
        boolean atPar = parMoves > 0 && moves <= parMoves;
        if (atPar) {
            earned += GameConfig.COINS_PAR_BONUS;
            campaignParFinishes++;
        }
        if (!firstClear) {
            earned = Math.max(5, earned / 3); // replays pay less
        }
        coins += earned;
        earned += recordWeeklyCampaignResult(LocalDate.now(java.time.ZoneOffset.UTC), moves,
            firstClear, atPar);
        earned += evaluateAchievements();
        flush();
        return earned;
    }

    /** @return shop "free coins" videos still rewarded today ({@code dateKey} = local ISO date) */
    public int adCoinsLeft(String dateKey) {
        if (dateKey == null || !dateKey.equals(adCoinsDate)) {
            return GameConfig.AD_COINS_PER_DAY;
        }
        return Math.max(0, GameConfig.AD_COINS_PER_DAY - adCoinsCount);
    }

    /**
     * Credits one confirmed "free coins" video. Call only from the SDK's reward callback.
     *
     * @return false (and nothing credited) once today's cap is reached
     */
    public boolean claimAdCoins(String dateKey) {
        if (dateKey == null || dateKey.isEmpty() || adCoinsLeft(dateKey) <= 0) {
            return false;
        }
        if (!dateKey.equals(adCoinsDate)) {
            adCoinsDate = dateKey;
            adCoinsCount = 0;
        }
        adCoinsCount++;
        coins += GameConfig.AD_COINS_REWARD;
        flush();
        return true;
    }

    public void addCoins(int amount) {
        if (amount <= 0) return;
        coins += amount;
        flush();
    }

    /** @return {@code true} when the player could afford it and the coins were deducted. */
    public boolean spendCoins(int amount) {
        if (amount <= 0 || coins < amount) return false;
        coins -= amount;
        flush();
        return true;
    }

    /**
     * Claims today's calendar reward (see {@link com.ben.laama.sortpuzzle.logic.DailyRewards}).
     *
     * @return the day index (1..7) paid out, or 0 when today was already claimed
     */
    public int claimDailyReward(LocalDate today) {
        if (dailyRewardDate == null) dailyRewardDate = "";
        int day = com.ben.laama.sortpuzzle.logic.DailyRewards.dayFor(dailyRewardDate, dailyRewardDay, today);
        if (day == 0) {
            return 0;
        }
        coins += com.ben.laama.sortpuzzle.logic.DailyRewards.coinsFor(day);
        boosters += com.ben.laama.sortpuzzle.logic.DailyRewards.boostersFor(day);
        dailyRewardDate = today.toString();
        dailyRewardDay = day;
        flush();
        return day;
    }

    // ---------------------------------------------------------------- assists
    /** Stock of an assist: tubes share the booster count, undos and resets their own. */
    public int stockOf(Assists assist) {
        switch (assist) {
            case TUBE:
                return boosters;
            case UNDO:
                return undos;
            default:
                return resets;
        }
    }

    public void addAssist(Assists assist, int count) {
        if (count <= 0) return;
        switch (assist) {
            case TUBE:
                boosters += count;
                break;
            case UNDO:
                undos += count;
                break;
            default:
                resets += count;
                break;
        }
        flush();
    }

    /** @return {@code true} when one was in stock and is now used */
    public boolean consumeAssist(Assists assist) {
        if (stockOf(assist) <= 0) return false;
        switch (assist) {
            case TUBE:
                boosters--;
                break;
            case UNDO:
                undos--;
                break;
            default:
                resets--;
                break;
        }
        flush();
        return true;
    }

    /** Buys one pack of the assist with coins. */
    public boolean buyAssistPack(Assists assist) {
        if (!spendCoins(assist.packPrice())) return false;
        addAssist(assist, assist.packSize());
        return true;
    }

    /** Adds bought boosters (a pack) after the coins were taken. */
    public boolean buyBoosters(int count, int price) {
        if (count <= 0 || !spendCoins(price)) return false;
        boosters += count;
        flush();
        return true;
    }

    /** @return {@code true} when a booster was bought. */
    public boolean buyBooster() {
        if (!spendCoins(GameConfig.BOOSTER_PRICE)) return false;
        boosters++;
        flush();
        return true;
    }

    public long rewardedTubeCooldownRemaining(long nowMillis) {
        if (lastRewardedTubeAtMillis <= 0L) return 0L;
        long elapsed = Math.max(0L, nowMillis - lastRewardedTubeAtMillis);
        return Math.max(0L, GameConfig.REWARDED_TUBE_COOLDOWN_MS - elapsed);
    }

    /** Grants one permanent tube only after a confirmed ad and if its cooldown has elapsed. */
    public boolean claimRewardedTube(long nowMillis) {
        if (rewardedTubeCooldownRemaining(nowMillis) > 0L) return false;
        boosters += GameConfig.REWARDED_TUBE_BONUS;
        lastRewardedTubeAtMillis = nowMillis;
        flush();
        return true;
    }

    /** @return {@code true} when a permanent booster was available and consumed. */
    public boolean consumeBooster() {
        if (boosters <= 0) return false;
        boosters--;
        flush();
        return true;
    }

    /**
     * Folds the legacy all-audio {@code muted} flag into the two independent switches, so a
     * player who had silenced everything stays silenced after the update.
     *
     * @return whether anything changed
     */
    public boolean migrateAudioFlags() {
        if (!muted) return false;
        effectsOff = true;
        musicOff = true;
        muted = false;
        return true;
    }

    public void setEffectsOff(boolean value) {
        effectsOff = value;
        flush();
    }

    // ---------------------------------------------------------------- reminders
    public void countSession() {
        sessions++;
        flush();
    }

    public void markDailyStarted(String date) {
        if (date == null || date.equals(lastDailyStartDate)) return;
        lastDailyStartDate = date;
        flush();
    }

    public void setRemindersOn(boolean value) {
        remindersOn = value;
        if (value) {
            reminderSystemDenied = false;
        }
        flush();
    }

    /** The player answered "Not now" on the soft-ask card. */
    public void declineReminderAsk(long nextAskDay) {
        reminderAsks++;
        reminderNextAskDay = nextAskDay;
        flush();
    }

    /** The system prompt said no: reminders off, and no more soft-asks. */
    public void markReminderSystemDenied() {
        remindersOn = false;
        reminderSystemDenied = true;
        flush();
    }

    public void setMusicOff(boolean value) {
        musicOff = value;
        flush();
    }

    public void setMusicTrack(int track) {
        if (track < 0 || track >= GameConfig.RELAXING_MUSIC_TRACK_COUNT) {
            throw new IllegalArgumentException("unknown music track: " + track);
        }
        musicTrack = track;
        flush();
    }

    /** Stores the interface language picked by the player. */
    public void setLanguage(com.ben.laama.sortpuzzle.i18n.Language chosen) {
        if (chosen == null) {
            throw new IllegalArgumentException("language must not be null");
        }
        language = chosen.code;
        flush();
    }

    public void setVibrate(boolean value) {
        vibrate = value;
        flush();
    }

    public void markTutorialSeen() {
        if (!tutorialSeen) {
            tutorialSeen = true;
            flush();
        }
    }
}
