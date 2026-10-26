package com.ben.laama.sortpuzzle.logic;

import com.ben.laama.sortpuzzle.GameConfig;
import com.ben.laama.sortpuzzle.model.BoardState;
import com.ben.laama.sortpuzzle.model.Packed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Builds levels by walking <em>backwards</em> from the solved position.
 *
 * <h2>Why backwards</h2>
 * <p>Dealing random balls and then testing the result with a solver is slow and, worse,
 * unbounded — you never know how many boards you will have to throw away. Walking backwards
 * from the goal means every board handed to the player is solvable <em>by construction</em>:
 * undoing the shuffle, step for step, is itself a valid solution. Generation is linear in the
 * shuffle length and never retries.</p>
 *
 * <h2>What counts as a reverse move</h2>
 * <p>A reverse move lifts {@code m} balls of colour {@code c} off tube {@code B} and drops them
 * on tube {@code A}. It is allowed only when the forward move {@code A -> B} would put the
 * board back exactly as it was. That needs two things of the source:</p>
 * <ul>
 *   <li>the {@code m} balls come from {@code B}'s top run, and either leave part of that run
 *       behind or empty {@code B} outright — otherwise {@code B} would end up showing a
 *       different colour and would refuse the balls on the way back;</li>
 *   <li>{@code A} has room for all {@code m};</li>
 * </ul>
 * <p>and one of two things of the destination:</p>
 * <ul>
 *   <li><b>A shows another colour, or is empty.</b> The {@code m} balls become {@code A}'s whole
 *       top run, so the forward move carries all of them back.</li>
 *   <li><b>A already shows colour {@code c}, and {@code B} was full.</b> The forward move then
 *       carries back only as many balls as {@code B} has room for, which is exactly {@code m}.
 *       Easy family to overlook, and it roughly doubles the generator's freedom.</li>
 * </ul>
 *
 * <h2>Staying on a presentable board</h2>
 * <p>Players expect to be handed full tubes of mixed balls plus genuinely empty spares. A free
 * random walk essentially never stops on such a board — it strands balls in half-filled tubes
 * and quietly eats the empty tubes the difficulty curve promised (measured: roughly one tidy
 * board per four thousand steps).</p>
 *
 * <p>So the walk does not step one reverse move at a time. It steps in <em>macro moves</em>: a
 * short chain of reverse moves that starts on a tidy board and ends on a different tidy board,
 * found by a small randomised search. The simplest such chain lifts part of one tube's top run
 * into a spare, refills that tube from a second, and empties the spare onto the second — in
 * effect swapping two blocks between two tubes. Longer chains rotate blocks around three or
 * more tubes. Every link is a reverse move, so the invariant that matters never breaks: the
 * board stays solvable, and the tubes stay full.</p>
 *
 * <p>Everything is driven by a {@link Random} seeded from the level index, so level 37 is the
 * same board on every device, every install, forever.</p>
 */
public final class LevelGenerator {

    /** Reverse moves a single macro move may chain together. */
    private static final int MACRO_MAX_DEPTH = 10;
    /** Positions one macro-move search may expand. */
    private static final int MACRO_NODE_BUDGET = 6_000;
    /** How many times the walk may resume from an earlier board after hitting a dead end. */
    private static final int MAX_DEAD_ENDS = 250;
    /** How many earlier boards the walk keeps around to resume from. */
    private static final int HISTORY_LIMIT = 48;
    /** How many candidate boards get ranked for difficulty. See {@code pickByDifficulty}. */
    private static final int RANK_SAMPLE = 32;
    /** Search budget when sizing up one candidate board. */
    private static final int RANK_NODE_BUDGET = 200_000;

    private LevelGenerator() {
    }

    /**
     * @param level 1-based level index
     * @return the level's board, with {@link Level#PAR_UNKNOWN} par (cheap, no solver)
     */
    public static Level generate(int level) {
        int colors = Difficulty.colorsFor(level);
        int emptyTubes = Difficulty.emptyTubesFor(level);
        int depth = Difficulty.shuffleDepthFor(level);

        BoardState board = shuffle(BoardState.solved(colors, emptyTubes), depth,
            new Random(seedFor(level)), Difficulty.scrambleRatioFor(level));

        return new Level(level, board, colors, emptyTubes, depth, Level.PAR_UNKNOWN);
    }

    /**
     * Same board as {@link #generate}, plus the optimal move count.
     *
     * <p>The search is bounded; if the budget runs out, par falls back to the length of the
     * first solution found, which is always an upper bound and never zero for a shuffled
     * board. Deterministic either way.</p>
     *
     * @param level      1-based level index
     * @param nodeBudget search budget for the optimal pass
     */
    public static Level generateWithPar(int level, long nodeBudget) {
        Level base = generate(level);
        Solver.Result optimal = Solver.findOptimal(base.start(), nodeBudget);
        if (optimal.solved()) {
            return base.withPar(optimal.moveCount());
        }
        Solver.Result any = Solver.findAny(base.start(), GameConfig.ANY_SOLUTION_NODE_BUDGET);
        return base.withPar(any.solved() ? any.moveCount() : Level.PAR_UNKNOWN);
    }

    /** Convenience overload using {@link GameConfig#PAR_SEARCH_NODE_BUDGET}. */
    public static Level generateWithPar(int level) {
        return generateWithPar(level, GameConfig.PAR_SEARCH_NODE_BUDGET);
    }

    /**
     * The per-level seed. Mixed with SplitMix64 so neighbouring levels do not produce
     * near-identical boards.
     */
    public static long seedFor(int level) {
        long z = GameConfig.LEVEL_SEED_BASE + level * 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** Convenience overload that ships the hardest board the walk found. */
    public static BoardState shuffle(BoardState solved, int macroMoves, Random random) {
        return shuffle(solved, macroMoves, random, 1f);
    }

    /**
     * Applies {@code macroMoves} macro moves to a solved board and returns one of the boards
     * seen along the way — the one whose difficulty matches {@code difficultyRatio}.
     *
     * <p>Three things make this more than a loop.</p>
     *
     * <p>Some boards are dead ends: no forward move leads into them, so no reverse move leads
     * out, and the walk cannot continue. When that happens it resumes from an earlier board
     * instead of stopping — a dead end reached after six macro moves would otherwise ship as a
     * level solvable in three.</p>
     *
     * <p>Not every board along the walk is worth shipping: any board that already has a colour
     * finished gives the game away and is skipped.</p>
     *
     * <p>And of the rest, <em>which</em> one ships is the difference between a curve and a
     * plateau. Always taking the most scrambled board sounds right and plays terribly: the
     * colour count only steps up every few levels, so every level inside a band arrives at the
     * same maximum scramble and the player feels nothing change from one level to the next.
     * Instead the candidates are ranked by how long a solution they need, and the board at the
     * requested percentile is the one that ships. Level 1 gets a gentle board, level 4 the
     * hardest board three colours can produce, and the ramp restarts on every new colour.</p>
     *
     * @param solved          a solved board — the walk's starting point
     * @param macroMoves      how many macro moves to take
     * @param random          seeded randomness; the same seed always yields the same board
     * @param difficultyRatio 0 for the easiest board found, 1 for the hardest
     * @return a shuffled board, guaranteed solvable, every non-empty tube full
     */
    public static BoardState shuffle(BoardState solved, int macroMoves, Random random,
                                     float difficultyRatio) {
        int[] current = solved.toEncodedArray();
        String currentKey = keyOf(current);
        String previousKey = null;

        // The starting board belongs in the history: the very first macro move can land in a
        // dead end, and without it there would be nowhere to fall back to.
        List<int[]> history = new ArrayList<>();
        history.add(current);
        List<int[]> candidates = new ArrayList<>();
        int deadEnds = 0;

        for (int step = 0; step < macroMoves; step++) {
            int[] next = findMacroMove(current, currentKey, previousKey, random);
            if (next == null) {
                if (history.isEmpty() || ++deadEnds > MAX_DEAD_ENDS) {
                    break;
                }
                current = history.get(random.nextInt(history.size()));
                currentKey = keyOf(current);
                previousKey = null;
                continue;
            }
            previousKey = currentKey;
            current = next;
            currentKey = keyOf(current);

            if (history.size() < HISTORY_LIMIT) {
                history.add(current);
            } else {
                history.set(random.nextInt(HISTORY_LIMIT), current);
            }
            if (!hasCompleteTube(current)) {
                candidates.add(current);
            }
        }

        int[] chosen = pickByDifficulty(candidates, difficultyRatio, current);
        return BoardState.fromEncoded(emptyTubesLast(chosen));
    }

    /**
     * Ranks the candidate boards by scramble and returns the one at {@code ratio} through the
     * ranking.
     *
     * <p>The sort is by heuristic first and by canonical key second, so it is total and
     * therefore reproducible — two boards that scramble equally always rank in the same order,
     * on every device.</p>
     */
    private static int[] pickByDifficulty(List<int[]> candidates, float ratio, int[] fallback) {
        if (candidates.isEmpty()) {
            return fallback;
        }

        // Ranking costs one search per candidate, and a ten-colour walk produces well over a
        // hundred of them. Forty-eight rungs is already finer than any colour group is long, so
        // sub-sample evenly rather than paying for resolution nobody can feel.
        List<int[]> pool = candidates;
        if (pool.size() > RANK_SAMPLE) {
            List<int[]> sampled = new ArrayList<>(RANK_SAMPLE);
            for (int i = 0; i < RANK_SAMPLE; i++) {
                sampled.add(pool.get((int) ((long) i * (pool.size() - 1) / (RANK_SAMPLE - 1))));
            }
            pool = sampled;
        }

        List<int[]> ranked = new ArrayList<>(pool);
        ranked.sort(Comparator
            .comparingInt(LevelGenerator::rankScore)
            .thenComparing(LevelGenerator::keyOf));

        float clamped = Math.max(0f, Math.min(1f, ratio));
        int index = Math.round(clamped * (ranked.size() - 1));
        return ranked.get(index);
    }

    /**
     * How hard a candidate board is, for ranking only.
     *
     * <p>The length of the first solution the solver finds, which tracks real difficulty far
     * better than the admissible heuristic on its own — the heuristic is a lower bound and
     * routinely rates two boards equal that play nothing alike. The heuristic still breaks ties,
     * and an unsolvable board (which the construction should make impossible) sorts last rather
     * than blowing up.</p>
     */
    private static int rankScore(int[] board) {
        Solver.Result solution = Solver.findAny(BoardState.copyOfEncoded(board), RANK_NODE_BUDGET);
        return solution.solved()
            ? solution.moveCount() * 4 + Solver.heuristic(board)
            : Integer.MAX_VALUE;
    }

    // ------------------------------------------------------------------------- macro moves
    /**
     * Searches for a chain of reverse moves leading from one tidy board to a different one.
     *
     * <p>Iterative deepening rather than plain depth-first: the shortest chains are the ones
     * that mix cleanly, and a straight depth-first search happily burns its whole budget down a
     * single ten-move branch while a three-move swap sits unexplored next to it. The move order
     * is shuffled at every node, so repeated calls from the same board give different results —
     * that randomness is what scrambles the level. The board we came from is excluded, which
     * stops the walk oscillating between two positions.</p>
     *
     * @return the new board, or {@code null} when no macro move exists within the budget
     */
    private static int[] findMacroMove(int[] start, String startKey, String previousKey,
                                       Random random) {
        for (int limit = 1; limit <= MACRO_MAX_DEPTH; limit++) {
            Set<String> visited = new HashSet<>();
            visited.add(startKey);
            int[] budget = {MACRO_NODE_BUDGET};
            int[] found = descend(start, limit, visited, budget, random, startKey, previousKey);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static int[] descend(int[] state, int remainingDepth, Set<String> visited,
                                 int[] budget, Random random, String startKey,
                                 String previousKey) {
        if (remainingDepth <= 0 || budget[0] <= 0) {
            return null;
        }
        List<int[]> moves = reverseMoves(state);
        Collections.shuffle(moves, random);

        for (int[] move : moves) {
            if (budget[0] <= 0) {
                return null;
            }
            budget[0]--;
            int[] child = applyReverse(state, move);
            String key = keyOf(child);
            if (!visited.add(key)) {
                continue;
            }
            if (isTidy(child) && !key.equals(startKey) && !key.equals(previousKey)) {
                return child;
            }
            int[] deeper = descend(child, remainingDepth - 1, visited, budget, random, startKey,
                previousKey);
            if (deeper != null) {
                return deeper;
            }
        }
        return null;
    }

    // --------------------------------------------------------------------------- move rules
    /** Every legal reverse move in {@code tubes}, as {@code {source, amount, target}}. */
    private static List<int[]> reverseMoves(int[] tubes) {
        List<int[]> moves = new ArrayList<>();
        for (int source = 0; source < tubes.length; source++) {
            int sourceEncoded = tubes[source];
            if (sourceEncoded == Packed.EMPTY) {
                continue;
            }
            int run = Packed.topRun(sourceEncoded);
            int size = Packed.size(sourceEncoded);
            int color = Packed.top(sourceEncoded);
            boolean sourceFull = size == Packed.CAPACITY;

            for (int amount = 1; amount <= run; amount++) {
                // Taking the whole top run is only reversible when it empties the tube.
                if (amount == run && size != run) {
                    continue;
                }
                for (int target = 0; target < tubes.length; target++) {
                    if (target == source) {
                        continue;
                    }
                    int targetEncoded = tubes[target];
                    if (Packed.freeSpace(targetEncoded) < amount) {
                        continue;
                    }
                    boolean sameColorOnTop = targetEncoded != Packed.EMPTY
                        && Packed.top(targetEncoded) == color;
                    if (sameColorOnTop && !sourceFull) {
                        continue; // second family needs a full source
                    }
                    moves.add(new int[]{source, amount, target});
                }
            }
        }
        return moves;
    }

    private static int[] applyReverse(int[] tubes, int[] move) {
        int[] next = Arrays.copyOf(tubes, tubes.length);
        int color = Packed.top(next[move[0]]);
        next[move[0]] = Packed.pop(next[move[0]], move[1]);
        next[move[2]] = Packed.push(next[move[2]], color, move[1]);
        return next;
    }

    // ----------------------------------------------------------------------------- helpers
    /** @return {@code true} when every non-empty tube is full — the board shape players expect */
    private static boolean isTidy(int[] tubes) {
        for (int tube : tubes) {
            if (tube != Packed.EMPTY && Packed.size(tube) != Packed.CAPACITY) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasCompleteTube(int[] tubes) {
        for (int tube : tubes) {
            if (Packed.isComplete(tube)) {
                return true;
            }
        }
        return false;
    }

    /** Moves the empty tubes to the end — cosmetic, tubes are interchangeable. */
    private static int[] emptyTubesLast(int[] tubes) {
        int[] ordered = new int[tubes.length];
        int write = 0;
        for (int tube : tubes) {
            if (tube != Packed.EMPTY) {
                ordered[write++] = tube;
            }
        }
        while (write < ordered.length) {
            ordered[write++] = Packed.EMPTY;
        }
        return ordered;
    }

    /** Order-independent position key: tubes are interchangeable. */
    private static String keyOf(int[] tubes) {
        int[] sorted = Arrays.copyOf(tubes, tubes.length);
        Arrays.sort(sorted);
        return Arrays.toString(sorted);
    }
}
