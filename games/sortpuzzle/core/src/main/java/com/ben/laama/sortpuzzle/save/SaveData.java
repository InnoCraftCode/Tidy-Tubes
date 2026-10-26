package com.ben.laama.sortpuzzle.save;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

import com.ben.laama.sortpuzzle.GameConfig;

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
    /** All audio off. */
    public boolean muted = false;
    /** Music off while effects stay on. */
    public boolean musicOff = false;
    public boolean vibrate = true;
    /** The "how to play" card was shown once. */
    public boolean tutorialSeen = false;
    /** Best move count per level, index = level - 1, {@code 0} = never finished. */
    public int[] bestMoves = new int[0];

    private transient Preferences prefs;

    // ------------------------------------------------------------- lifecycle
    /** Reads the save from disk, falling back to a fresh profile if it is missing or corrupt. */
    public static SaveData load() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        String payload = prefs.getString(KEY_PAYLOAD, null);
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
        tutorialSeen = false;
        bestMoves = new int[0];
        flush();
    }

    private void sanitise() {
        version = CURRENT_VERSION;
        if (currentLevel < 1) currentLevel = 1;
        if (highestUnlocked < 1) highestUnlocked = 1;
        if (currentLevel > highestUnlocked) currentLevel = highestUnlocked;
        if (coins < 0) coins = 0;
        if (boosters < 0) boosters = 0;
        if (bestMoves == null) bestMoves = new int[0];
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
        boolean firstClear = bestMoves[i] == 0;
        if (firstClear || moves < bestMoves[i]) {
            bestMoves[i] = moves;
        }

        if (level + 1 > highestUnlocked) {
            highestUnlocked = level + 1;
        }
        currentLevel = Math.min(level + 1, highestUnlocked);

        int earned = GameConfig.COINS_PER_WIN;
        if (parMoves > 0 && moves <= parMoves) {
            earned += GameConfig.COINS_PAR_BONUS;
        }
        if (!firstClear) {
            earned = Math.max(5, earned / 3); // replays pay less
        }
        coins += earned;
        flush();
        return earned;
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

    /** @return {@code true} when a booster was bought. */
    public boolean buyBooster() {
        if (!spendCoins(GameConfig.BOOSTER_PRICE)) return false;
        boosters++;
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

    public void setMuted(boolean value) {
        muted = value;
        flush();
    }

    public void setMusicOff(boolean value) {
        musicOff = value;
        flush();
    }

    public void markTutorialSeen() {
        if (!tutorialSeen) {
            tutorialSeen = true;
            flush();
        }
    }
}
