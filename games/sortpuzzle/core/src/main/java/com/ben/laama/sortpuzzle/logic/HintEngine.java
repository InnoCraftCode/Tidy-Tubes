package com.ben.laama.sortpuzzle.logic;

import com.ben.laama.sortpuzzle.GameConfig;
import com.ben.laama.sortpuzzle.model.BoardState;

/**
 * Turns the solver into advice.
 *
 * <p>The solvers already work from any position, not just the opening, so the whole hint
 * feature is a thin layer on top of them. What this class adds is the honesty: it tries for an
 * exact answer, settles for a workable one, and distinguishes "there is no way out of here"
 * from "I ran out of time looking" — three outcomes the player experiences very differently.</p>
 *
 * <p>The searches are bounded but not instant on a ten-colour board. Call this off the render
 * thread.</p>
 */
public final class HintEngine {

    private HintEngine() {
    }

    /** Advice using the budgets in {@link GameConfig}. */
    public static Hint advise(BoardState state) {
        return advise(state, GameConfig.HINT_OPTIMAL_NODE_BUDGET,
            GameConfig.ANY_SOLUTION_NODE_BUDGET);
    }

    /**
     * @param state          the position the player is looking at
     * @param optimalBudget  node budget for the exact search
     * @param fallbackBudget node budget for the "any route will do" search
     */
    public static Hint advise(BoardState state, long optimalBudget, long fallbackBudget) {
        if (WinChecker.isWon(state)) {
            return Hint.alreadyWon();
        }

        // First choice: the shortest path. Gives the player a move and a number they can trust.
        Solver.Result optimal = Solver.findOptimal(state, optimalBudget);
        if (optimal.solved()) {
            return Hint.move(optimal.moves().get(0), optimal.moveCount(), true);
        }

        // Second choice: any path. The move is still guaranteed to lead to a win; only the count
        // loses its edge, becoming an upper bound.
        Solver.Result fallback = Solver.findAny(state, fallbackBudget);
        if (fallback.solved()) {
            return Hint.move(fallback.moves().get(0), fallback.moveCount(), false);
        }

        // Nothing found. Whether that is a proof or a timeout matters: one means the player has
        // painted themselves into a corner and needs to undo, the other means we simply do not
        // know, and telling them to undo would be a lie.
        return fallback.budgetExhausted() ? Hint.unknown() : Hint.undo();
    }
}
