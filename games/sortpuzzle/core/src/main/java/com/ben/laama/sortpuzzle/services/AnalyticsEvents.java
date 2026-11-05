package com.ben.laama.sortpuzzle.services;

import java.util.regex.Pattern;

/**
 * The only event and parameter names the game logs. Centralised so they can be checked against
 * the provider's rules (Firebase: 1-40 chars, letters/digits/underscore, must start with a
 * letter, no reserved {@code firebase_}/{@code google_}/{@code ga_} prefixes) by a unit test
 * instead of failing silently on a device.
 */
public final class AnalyticsEvents {

    // ------------------------------------------------------------------ events
    public static final String LEVEL_START = "level_start";
    public static final String LEVEL_COMPLETE = "level_complete";
    public static final String LEVEL_OUT_OF_MOVES = "level_out_of_moves";
    public static final String DAILY_COMPLETE = "daily_complete";
    public static final String HINTS_UNLOCKED = "hints_unlocked";
    public static final String REWARDED_EXTRA_MOVES = "rewarded_extra_moves";
    public static final String PLAY_GAMES_SIGN_IN = "play_games_sign_in";
    public static final String LEADERBOARD_OPEN = "leaderboard_open";
    public static final String SHOP_OPEN = "shop_open";
    public static final String SHOP_PURCHASE = "shop_purchase";
    public static final String SHOP_EQUIP = "shop_equip";
    public static final String SHARE_RESULT = "share_result";
    public static final String BREATHE_START = "breathe_start";
    public static final String BREATHE_DONE = "breathe_done";      // result: done | skipped
    public static final String REWARDED_DOUBLE_COINS = "rewarded_double_coins";
    public static final String REWARDED_FREE_COINS = "rewarded_free_coins";

    // -------------------------------------------------------------- parameters
    public static final String P_LEVEL = "level";
    public static final String P_MOVES = "moves";
    public static final String P_PAR = "par";
    public static final String P_STARS = "stars";
    public static final String P_PERFECT = "perfect";
    public static final String P_MODE = "mode";          // campaign | daily | challenge
    public static final String P_SOURCE = "source";      // e.g. rewarded | coins | free
    public static final String P_RESULT = "result";      // ok | failed
    public static final String P_ITEM = "item";          // cosmetic id, e.g. theme.sakura
    public static final String P_PRICE = "price";        // coins paid
    public static final String P_COINS = "coins";        // balance after the event

    public static final String[] EVENTS = {
        LEVEL_START, LEVEL_COMPLETE, LEVEL_OUT_OF_MOVES, DAILY_COMPLETE, HINTS_UNLOCKED,
        REWARDED_EXTRA_MOVES, PLAY_GAMES_SIGN_IN, LEADERBOARD_OPEN, SHOP_OPEN, SHOP_PURCHASE,
        SHOP_EQUIP, SHARE_RESULT, BREATHE_START, BREATHE_DONE, REWARDED_DOUBLE_COINS,
        REWARDED_FREE_COINS
    };
    public static final String[] PARAMS = {
        P_LEVEL, P_MOVES, P_PAR, P_STARS, P_PERFECT, P_MODE, P_SOURCE, P_RESULT, P_ITEM, P_PRICE,
        P_COINS
    };

    public static final int MAX_NAME_LENGTH = 40;
    public static final int MAX_PARAMS_PER_EVENT = 25;
    public static final int MAX_STRING_VALUE_LENGTH = 100;

    private static final Pattern NAME = Pattern.compile("[A-Za-z][A-Za-z0-9_]{0,39}");

    private AnalyticsEvents() {
    }

    /** @return whether {@code name} is acceptable as a Firebase event or parameter name */
    public static boolean isValidName(String name) {
        if (name == null || !NAME.matcher(name).matches()) {
            return false;
        }
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        return !lower.startsWith("firebase_") && !lower.startsWith("google_")
            && !lower.startsWith("ga_");
    }

    /**
     * @return whether an alternating key/value list is well formed: even length, valid names,
     *         supported value types, at most {@link #MAX_PARAMS_PER_EVENT} pairs
     */
    public static boolean isValidParams(Object... keyValues) {
        if (keyValues == null) {
            return true;
        }
        if (keyValues.length % 2 != 0 || keyValues.length / 2 > MAX_PARAMS_PER_EVENT) {
            return false;
        }
        for (int i = 0; i < keyValues.length; i += 2) {
            if (!(keyValues[i] instanceof String) || !isValidName((String) keyValues[i])) {
                return false;
            }
            Object value = keyValues[i + 1];
            boolean ok = value instanceof Integer || value instanceof Long
                || value instanceof Boolean || value instanceof Double
                || (value instanceof String && ((String) value).length() <= MAX_STRING_VALUE_LENGTH);
            if (!ok) {
                return false;
            }
        }
        return true;
    }
}
