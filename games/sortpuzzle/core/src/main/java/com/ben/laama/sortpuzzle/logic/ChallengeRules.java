package com.ben.laama.sortpuzzle.logic;

/** Optional, combinable rules selected by the player before a challenge run. */
public final class ChallengeRules {

    public static final ChallengeRules CLASSIC = new ChallengeRules(false, false, false, false);

    private final boolean noUndo;
    private final boolean noHints;
    private final boolean noBoosters;
    private final boolean parMoveLimit;

    public ChallengeRules(boolean noUndo, boolean noHints, boolean noBoosters,
                          boolean parMoveLimit) {
        this.noUndo = noUndo;
        this.noHints = noHints;
        this.noBoosters = noBoosters;
        this.parMoveLimit = parMoveLimit;
    }

    public boolean noUndo() {
        return noUndo;
    }

    public boolean noHints() {
        return noHints;
    }

    public boolean noBoosters() {
        return noBoosters;
    }

    public boolean parMoveLimit() {
        return parMoveLimit;
    }

    public boolean isClassic() {
        return !noUndo && !noHints && !noBoosters && !parMoveLimit;
    }

    public String summary() {
        if (isClassic()) {
            return "Classic";
        }
        StringBuilder summary = new StringBuilder();
        append(summary, noUndo, "No undo");
        append(summary, noHints, "No hints");
        append(summary, noBoosters, "No boosters");
        append(summary, parMoveLimit, "Par limit");
        return summary.toString();
    }

    private static void append(StringBuilder summary, boolean active, String label) {
        if (!active) {
            return;
        }
        if (summary.length() > 0) {
            summary.append("  •  ");
        }
        summary.append(label);
    }
}
