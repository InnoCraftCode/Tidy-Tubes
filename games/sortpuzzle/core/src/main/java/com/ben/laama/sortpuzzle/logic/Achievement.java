package com.ben.laama.sortpuzzle.logic;

import com.ben.laama.sortpuzzle.i18n.T;

/** Offline achievement definitions, progress and one-time coin rewards. */
public enum Achievement {
    FIRST_CLEAR(T.k("First clear"), T.k("Complete a campaign level"), 1, 20),
    TEN_CLEARS(T.k("Getting tidy"), T.k("Complete 10 campaign levels"), 10, 50),
    PAR_SOLVER(T.k("Par solver"), T.k("Finish a campaign level at par"), 1, 25),
    STAR_COLLECTOR(T.k("Star collector"), T.k("Collect 30 campaign stars"), 30, 75),
    CHALLENGE_WINNER(T.k("Challenge accepted"), T.k("Win a custom challenge"), 1, 35);

    private final String title;
    private final String description;
    private final int target;
    private final int reward;

    Achievement(String title, String description, int target, int reward) {
        this.title = title;
        this.description = description;
        this.target = target;
        this.reward = reward;
    }

    public int id() {
        return ordinal();
    }

    public int bit() {
        return 1 << ordinal();
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public int target() {
        return target;
    }

    public int reward() {
        return reward;
    }

    public int progress(int clears, int parFinishes, int stars, int challengeWins) {
        switch (this) {
            case FIRST_CLEAR:
            case TEN_CLEARS:
                return clears;
            case PAR_SOLVER:
                return parFinishes;
            case STAR_COLLECTOR:
                return stars;
            case CHALLENGE_WINNER:
                return challengeWins;
            default:
                throw new IllegalStateException("unknown achievement: " + this);
        }
    }
}
