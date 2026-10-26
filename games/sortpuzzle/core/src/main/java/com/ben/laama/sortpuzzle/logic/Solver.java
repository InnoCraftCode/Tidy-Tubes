package com.ben.laama.sortpuzzle.logic;

import com.ben.laama.sortpuzzle.model.BoardState;
import com.ben.laama.sortpuzzle.model.Move;
import com.ben.laama.sortpuzzle.model.Packed;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ball-sort search, in two flavours.
 *
 * <ul>
 *   <li>{@link #findAny} — depth-first graph search with heavy move ordering and symmetry
 *       reduction. Answers "is this solvable, and how" quickly. Used by the test suite to
 *       prove that generated levels really are solvable.</li>
 *   <li>{@link #findOptimal} — IDA* with an admissible heuristic. Answers "what is the
 *       shortest solution", which is what par-moves are computed from.</li>
 * </ul>
 *
 * <h2>Representation</h2>
 * <p>Both searches run on {@code int[]} arrays of packed tubes rather than on
 * {@link BoardState}, and positions are memoised under an order-independent key, because
 * tubes are interchangeable: a board and the same board with two tubes swapped are the same
 * puzzle.</p>
 *
 * <h2>Pruning that is safe</h2>
 * <p>Two classes of move are never part of a shortest solution and are dropped outright:
 * taking balls back out of a finished tube, and tipping an already single-coloured tube into
 * an empty one (which only renames a tube). Identical tubes are also collapsed, so a position
 * with three empty tubes generates one "move to an empty tube", not three.</p>
 *
 * <p>Everything here is pure Java — no libGDX, fully unit-testable, and deterministic.</p>
 */
public final class Solver {

    /** Hard ceiling on the search stack, guarding against pathological inputs. */
    private static final int MAX_DEPTH = 20_000;

    private Solver() {
    }

    // ------------------------------------------------------------------ result
    /** Outcome of a search. */
    public static final class Result {

        private final List<Move> moves;
        private final long nodes;
        private final boolean budgetExhausted;

        private Result(List<Move> moves, long nodes, boolean budgetExhausted) {
            this.moves = moves;
            this.nodes = nodes;
            this.budgetExhausted = budgetExhausted;
        }

        public boolean solved() {
            return moves != null;
        }

        /** @return the solution, or an empty list when none was found */
        public List<Move> moves() {
            return moves == null ? Collections.emptyList() : moves;
        }

        public int moveCount() {
            return moves == null ? 0 : moves.size();
        }

        /** @return how many positions the search expanded */
        public long nodes() {
            return nodes;
        }

        /** @return {@code true} when the search gave up on budget rather than on proof */
        public boolean budgetExhausted() {
            return budgetExhausted;
        }

        @Override
        public String toString() {
            return solved()
                ? "solved in " + moveCount() + " moves (" + nodes + " nodes)"
                : (budgetExhausted ? "budget exhausted" : "proven unsolvable")
                  + " (" + nodes + " nodes)";
        }
    }

    // ----------------------------------------------------------------- findAny
    /**
     * Finds <em>a</em> solution, not necessarily the shortest.
     *
     * @param state      position to solve
     * @param nodeBudget maximum positions to expand before giving up
     */
    public static Result findAny(BoardState state, long nodeBudget) {
        int[] root = state.toEncodedArray();
        if (isSolved(root)) {
            return new Result(Collections.emptyList(), 0L, false);
        }

        Set<Key> visited = new HashSet<>();
        visited.add(Key.of(root));

        Deque<Frame> stack = new ArrayDeque<>();
        stack.push(new Frame(root, orderedMoves(root), NO_MOVE));

        long nodes = 0;
        while (!stack.isEmpty()) {
            if (nodes >= nodeBudget) {
                return new Result(null, nodes, true);
            }
            Frame frame = stack.peek();
            if (frame.next >= frame.moves.length || stack.size() >= MAX_DEPTH) {
                stack.pop();
                continue;
            }
            int packedMove = frame.moves[frame.next++];
            int[] child = applyPacked(frame.state, packedMove);
            nodes++;

            if (!visited.add(Key.of(child))) {
                continue;
            }
            if (isSolved(child)) {
                return new Result(reconstruct(stack, packedMove), nodes, false);
            }
            stack.push(new Frame(child, orderedMoves(child), packedMove));
        }
        return new Result(null, nodes, false);
    }

    /** Walks the live stack bottom-up to turn the current path into a move list. */
    private static List<Move> reconstruct(Deque<Frame> stack, int lastMove) {
        List<Move> path = new ArrayList<>(stack.size() + 1);
        Iterator<Frame> bottomUp = stack.descendingIterator();
        while (bottomUp.hasNext()) {
            Frame frame = bottomUp.next();
            if (frame.arrivedBy != NO_MOVE) {
                path.add(unpack(frame.arrivedBy));
            }
        }
        path.add(unpack(lastMove));
        return path;
    }

    // ------------------------------------------------------------- findOptimal
    /**
     * Finds the shortest solution using IDA*.
     *
     * <p>The heuristic is the number of extra tubes each colour is spread across: a colour
     * living in three tubes needs at least two more moves to be gathered. Every move changes
     * that total by at most one, which makes the estimate admissible and therefore the answer
     * optimal.</p>
     *
     * @param state      position to solve
     * @param nodeBudget maximum positions to expand across all iterations
     */
    public static Result findOptimal(BoardState state, long nodeBudget) {
        int[] root = state.toEncodedArray();
        if (isSolved(root)) {
            return new Result(Collections.emptyList(), 0L, false);
        }

        int threshold = heuristic(root);
        long[] nodes = {0L};
        int[] path = new int[MAX_DEPTH];
        int[] solutionLength = {0};

        while (threshold < MAX_DEPTH) {
            Map<Key, Integer> seen = new HashMap<>();
            int next = search(root, 0, threshold, path, seen, nodes, nodeBudget, solutionLength);
            if (next == FOUND) {
                List<Move> moves = new ArrayList<>(solutionLength[0]);
                for (int i = 0; i < solutionLength[0]; i++) {
                    moves.add(unpack(path[i]));
                }
                return new Result(moves, nodes[0], false);
            }
            if (next == BUDGET) {
                return new Result(null, nodes[0], true);
            }
            if (next == INFINITY) {
                // Nothing left to expand at this bound and nothing was cut by the transposition
                // table either: the position really has no solution.
                return new Result(null, nodes[0], false);
            }
            threshold = Math.max(next, threshold + 1);
        }
        return new Result(null, nodes[0], true);
    }

    private static final int FOUND = -1;
    private static final int BUDGET = -2;
    private static final int INFINITY = Integer.MAX_VALUE;

    /**
     * One IDA* iteration.
     *
     * <p>The transposition table needs care: when a child is skipped because the same position
     * was already reached at least as cheaply, that subtree has <em>not</em> been proven
     * hopeless, so the node must not report "no solution beyond this bound". It reports
     * {@code threshold + 1} instead, which keeps the next bound moving without ever claiming a
     * false proof of unsolvability.</p>
     *
     * @return {@link #FOUND}, {@link #BUDGET}, {@link #INFINITY}, or the smallest f-value that
     *         exceeded {@code threshold} — the next iteration's bound
     */
    private static int search(int[] state, int depth, int threshold, int[] path,
                              Map<Key, Integer> seen, long[] nodes, long budget,
                              int[] solutionLength) {
        int f = depth + heuristic(state);
        if (f > threshold) {
            return f;
        }
        if (isSolved(state)) {
            solutionLength[0] = depth;
            return FOUND;
        }
        if (nodes[0] >= budget) {
            return BUDGET;
        }
        if (depth + 1 >= MAX_DEPTH) {
            return INFINITY;
        }

        int minimum = INFINITY;
        boolean cutByTable = false;

        for (int packedMove : orderedMoves(state)) {
            int[] child = applyPacked(state, packedMove);
            nodes[0]++;

            Key key = Key.of(child);
            Integer best = seen.get(key);
            if (best != null && best <= depth + 1) {
                cutByTable = true;
                continue; // already reached this position at least as cheaply
            }
            seen.put(key, depth + 1);

            path[depth] = packedMove;
            int result = search(child, depth + 1, threshold, path, seen, nodes, budget,
                solutionLength);
            if (result == FOUND || result == BUDGET) {
                return result;
            }
            if (result < minimum) {
                minimum = result;
            }
        }

        if (cutByTable && minimum > threshold + 1) {
            minimum = threshold + 1;
        }
        return minimum;
    }

    /**
     * Admissible lower bound: for every colour, the number of tubes it is scattered across
     * minus one. A single move can merge at most one such split.
     */
    static int heuristic(int[] tubes) {
        int estimate = 0;
        // Bit set per colour of the tubes it appears in, collapsed to a count.
        int[] tubesPerColor = new int[16];
        for (int tube : tubes) {
            int size = Packed.size(tube);
            int seenMask = 0;
            for (int slot = 0; slot < size; slot++) {
                int color = Packed.slot(tube, slot);
                int bit = 1 << color;
                if ((seenMask & bit) == 0) {
                    seenMask |= bit;
                    tubesPerColor[color]++;
                }
            }
        }
        for (int count : tubesPerColor) {
            if (count > 1) {
                estimate += count - 1;
            }
        }
        return estimate;
    }

    // ------------------------------------------------------------ move machinery
    private static final int NO_MOVE = -1;

    /** Packs a move into one int: {@code from | to << 8 | count << 16 | colour << 24}. */
    private static int pack(int from, int to, int count, int color) {
        return (from & 0xFF) | ((to & 0xFF) << 8) | ((count & 0xFF) << 16) | ((color & 0xFF) << 24);
    }

    private static Move unpack(int packed) {
        return new Move(packed & 0xFF, (packed >>> 8) & 0xFF,
            (packed >>> 16) & 0xFF, (packed >>> 24) & 0xFF);
    }

    private static int[] applyPacked(int[] state, int packedMove) {
        int from = packedMove & 0xFF;
        int to = (packedMove >>> 8) & 0xFF;
        int count = (packedMove >>> 16) & 0xFF;
        int color = (packedMove >>> 24) & 0xFF;
        int[] next = Arrays.copyOf(state, state.length);
        next[from] = Packed.pop(next[from], count);
        next[to] = Packed.push(next[to], color, count);
        return next;
    }

    /**
     * Every move worth trying, best first.
     *
     * <p>Ordering is what makes the difference between solving a ten-colour board in
     * milliseconds and not solving it at all: finish a tube if you can, then free a tube, then
     * merge onto an existing colour, and only spend an empty tube as a last resort.</p>
     */
    static int[] orderedMoves(int[] tubes) {
        int count = tubes.length;
        int[] moves = new int[count * count];
        int[] scores = new int[count * count];
        int found = 0;

        // Collapse interchangeable tubes: identical contents produce identical subtrees.
        int[] seenSources = new int[count];
        int seenSourceCount = 0;

        for (int from = 0; from < count; from++) {
            int fromEncoded = tubes[from];
            if (fromEncoded == Packed.EMPTY || Packed.isComplete(fromEncoded)) {
                continue;
            }
            if (contains(seenSources, seenSourceCount, fromEncoded)) {
                continue;
            }
            seenSources[seenSourceCount++] = fromEncoded;

            int run = Packed.topRun(fromEncoded);
            int size = Packed.size(fromEncoded);
            int color = Packed.top(fromEncoded);
            boolean sourceUniform = run == size;

            int[] seenTargets = new int[count];
            int seenTargetCount = 0;

            for (int to = 0; to < count; to++) {
                if (to == from) {
                    continue;
                }
                int toEncoded = tubes[to];
                int moved = MoveValidator.transferCount(fromEncoded, toEncoded);
                if (moved == 0) {
                    continue;
                }
                boolean targetEmpty = toEncoded == Packed.EMPTY;
                if (targetEmpty && sourceUniform) {
                    continue; // pure relabelling, never useful
                }
                if (contains(seenTargets, seenTargetCount, toEncoded)) {
                    continue;
                }
                seenTargets[seenTargetCount++] = toEncoded;

                int score = moved;
                int resultingTarget = Packed.push(toEncoded, color, moved);
                if (Packed.isComplete(resultingTarget)) {
                    score += 1000;
                }
                if (moved == size) {
                    score += 500; // the source tube is freed entirely
                }
                if (!targetEmpty) {
                    score += 300;
                } else {
                    score -= 200;
                }
                moves[found] = pack(from, to, moved, color);
                scores[found] = score;
                found++;
            }
        }

        return sortByScoreDescending(moves, scores, found);
    }

    private static boolean contains(int[] values, int size, int needle) {
        for (int i = 0; i < size; i++) {
            if (values[i] == needle) {
                return true;
            }
        }
        return false;
    }

    /** Insertion sort: the list is tiny (tens of entries at most) and this keeps it stable. */
    private static int[] sortByScoreDescending(int[] moves, int[] scores, int size) {
        for (int i = 1; i < size; i++) {
            int move = moves[i];
            int score = scores[i];
            int j = i - 1;
            while (j >= 0 && scores[j] < score) {
                moves[j + 1] = moves[j];
                scores[j + 1] = scores[j];
                j--;
            }
            moves[j + 1] = move;
            scores[j + 1] = score;
        }
        return Arrays.copyOf(moves, size);
    }

    private static boolean isSolved(int[] tubes) {
        for (int tube : tubes) {
            if (tube != Packed.EMPTY && !Packed.isComplete(tube)) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ helpers
    private static final class Frame {
        final int[] state;
        final int[] moves;
        final int arrivedBy;
        int next;

        Frame(int[] state, int[] moves, int arrivedBy) {
            this.state = state;
            this.moves = moves;
            this.arrivedBy = arrivedBy;
        }
    }

    /**
     * Order-independent position key. Tube encodings are sorted before packing, so two boards
     * differing only by a permutation of tubes hash and compare equal.
     */
    private static final class Key {

        private final long[] words;
        private final int hash;

        private Key(long[] words) {
            this.words = words;
            this.hash = Arrays.hashCode(words);
        }

        static Key of(int[] tubes) {
            int[] sorted = Arrays.copyOf(tubes, tubes.length);
            Arrays.sort(sorted);
            long[] words = new long[(sorted.length + 3) >>> 2];
            for (int i = 0; i < sorted.length; i++) {
                words[i >>> 2] |= ((long) (sorted[i] & 0xFFFF)) << ((i & 3) << 4);
            }
            return new Key(words);
        }

        @Override
        public boolean equals(Object other) {
            return this == other
                || (other instanceof Key && Arrays.equals(words, ((Key) other).words));
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
