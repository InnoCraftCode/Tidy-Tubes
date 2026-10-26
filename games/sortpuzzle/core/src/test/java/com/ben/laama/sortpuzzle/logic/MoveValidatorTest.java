package com.ben.laama.sortpuzzle.logic;

import com.ben.laama.sortpuzzle.model.BoardState;
import com.ben.laama.sortpuzzle.model.Move;
import com.ben.laama.sortpuzzle.model.Tube;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("MoveValidator")
class MoveValidatorTest {

    @Test
    @DisplayName("balls may land on an empty tube")
    void emptyDestinationAccepts() {
        BoardState board = BoardState.of(Tube.of(1, 2), Tube.empty());
        assertTrue(MoveValidator.canMove(board, 0, 1));
    }

    @Test
    @DisplayName("balls may land on a matching colour")
    void matchingDestinationAccepts() {
        BoardState board = BoardState.of(Tube.of(1, 2), Tube.of(2));
        assertTrue(MoveValidator.canMove(board, 0, 1));
    }

    @Test
    @DisplayName("balls may not land on a different colour")
    void mismatchedDestinationRejects() {
        BoardState board = BoardState.of(Tube.of(1, 2), Tube.of(5));
        assertFalse(MoveValidator.canMove(board, 0, 1));
        assertNull(MoveValidator.moveFor(board, 0, 1));
    }

    @Test
    @DisplayName("a full destination refuses everything")
    void fullDestinationRejects() {
        BoardState board = BoardState.of(Tube.of(2), Tube.full(2));
        assertFalse(MoveValidator.canMove(board, 0, 1));
    }

    @Test
    @DisplayName("an empty source has nothing to give, and no tube moves onto itself")
    void degenerateMoves() {
        BoardState board = BoardState.of(Tube.empty(), Tube.of(1));
        assertFalse(MoveValidator.canMove(board, 0, 1));
        assertFalse(MoveValidator.canMove(board, 1, 1));
        assertFalse(MoveValidator.canMove(board, 1, 7));
        assertFalse(MoveValidator.canMove(board, -1, 0));
    }

    @Test
    @DisplayName("the whole contiguous run travels together")
    void wholeRunMoves() {
        BoardState board = BoardState.of(Tube.of(0, 3, 3, 3), Tube.empty());
        Move move = MoveValidator.moveFor(board, 0, 1);

        assertNotNull(move);
        assertEquals(3, move.count());
        assertEquals(3, move.colorIndex());
        assertEquals(1, board.apply(move).tube(0).size());
    }

    @Test
    @DisplayName("a run larger than the space left is clipped to what fits")
    void runIsClippedToFreeSpace() {
        BoardState board = BoardState.of(Tube.of(3, 3, 3, 3), Tube.of(3, 3));
        Move move = MoveValidator.moveFor(board, 0, 1);

        assertNotNull(move);
        assertEquals(2, move.count());
        BoardState after = board.apply(move);
        assertEquals(2, after.tube(0).size());
        assertTrue(after.tube(1).isComplete());
    }

    @Test
    @DisplayName("only the top run moves, not same-coloured balls buried below")
    void buriedBallsStayPut() {
        BoardState board = BoardState.of(Tube.of(3, 1, 3, 3), Tube.empty());
        Move move = MoveValidator.moveFor(board, 0, 1);

        assertNotNull(move);
        assertEquals(2, move.count());
        assertEquals(2, board.apply(move).tube(0).size());
    }

    @Test
    @DisplayName("legalMoves lists exactly the moves canMove accepts")
    void legalMovesAgreesWithCanMove() {
        BoardState board = BoardState.of(
            Tube.of(0, 1, 1), Tube.of(1), Tube.full(2), Tube.empty());

        List<Move> moves = MoveValidator.legalMoves(board);
        int expected = 0;
        for (int from = 0; from < board.tubeCount(); from++) {
            for (int to = 0; to < board.tubeCount(); to++) {
                if (from != to && MoveValidator.canMove(board, from, to)) {
                    expected++;
                }
            }
        }
        assertEquals(expected, moves.size());
        assertTrue(MoveValidator.hasLegalMove(board));

        for (Move move : moves) {
            assertTrue(MoveValidator.canMove(board, move.from(), move.to()));
        }
    }

    @Test
    @DisplayName("useful moves exclude churn: emptying finished tubes or renaming a tube")
    void usefulnessFiltersChurn() {
        BoardState board = BoardState.of(
            Tube.full(2), Tube.of(2), Tube.of(1, 1), Tube.of(0, 1), Tube.empty());

        assertTrue(MoveValidator.canMove(board, 0, 1), "legal, but pointless");
        assertFalse(MoveValidator.isUseful(board, 0, 1), "never unpack a finished tube");

        assertTrue(MoveValidator.canMove(board, 2, 4));
        assertFalse(MoveValidator.isUseful(board, 2, 4), "single colour into an empty tube");

        assertTrue(MoveValidator.isUseful(board, 3, 2), "merging onto a matching colour counts");
        assertTrue(MoveValidator.isUseful(board, 3, 4), "freeing a buried colour counts");
    }

    @Test
    @DisplayName("a board with no legal move reports none")
    void deadBoard() {
        BoardState board = BoardState.of(Tube.of(0, 1, 0, 1), Tube.of(1, 0, 1, 0));
        assertFalse(MoveValidator.hasLegalMove(board));
        assertTrue(MoveValidator.legalMoves(board).isEmpty());
    }
}
