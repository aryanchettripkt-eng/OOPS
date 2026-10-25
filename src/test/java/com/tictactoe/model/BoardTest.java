package com.tictactoe.model;

import com.tictactoe.exception.InvalidMoveException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Board Model Unit Tests")
class BoardTest {

    private Board board;

    @BeforeEach
    void setUp() {
        board = new Board();
    }

    @Test
    @DisplayName("Empty board initialization has all empty cells and zero move count")
    void testEmptyBoardInitialization() {
        assertEquals(0, board.getMoveCount());
        assertFalse(board.isFull());
        assertFalse(board.isDraw());
        assertFalse(board.checkWin(PlayerSymbol.X));
        assertFalse(board.checkWin(PlayerSymbol.O));

        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                assertEquals(PlayerSymbol.EMPTY, board.getCell(r, c));
                assertTrue(board.isCellEmpty(r, c));
                assertFalse(board.isOccupied(r, c));
            }
        }
    }

    @Test
    @DisplayName("Valid move places symbol, increases move count, and marks cell occupied")
    void testValidMove() throws InvalidMoveException {
        boolean success = board.makeMove(1, 1, PlayerSymbol.X);
        assertTrue(success);
        assertEquals(PlayerSymbol.X, board.getCell(1, 1));
        assertEquals(1, board.getMoveCount());
        assertTrue(board.isOccupied(1, 1));
        assertFalse(board.isCellEmpty(1, 1));
    }

    @Test
    @DisplayName("Invalid row or column coordinates throw InvalidMoveException")
    void testInvalidCoordinates() {
        assertThrows(InvalidMoveException.class, () -> board.makeMove(-1, 0, PlayerSymbol.X));
        assertThrows(InvalidMoveException.class, () -> board.makeMove(0, -1, PlayerSymbol.X));
        assertThrows(InvalidMoveException.class, () -> board.makeMove(3, 1, PlayerSymbol.X));
        assertThrows(InvalidMoveException.class, () -> board.makeMove(1, 3, PlayerSymbol.X));
        assertThrows(InvalidMoveException.class, () -> board.makeMove(99, 99, PlayerSymbol.X));
    }

    @Test
    @DisplayName("Occupied cell rejection throws InvalidMoveException")
    void testOccupiedCellRejection() throws InvalidMoveException {
        board.makeMove(0, 0, PlayerSymbol.X);
        InvalidMoveException ex = assertThrows(InvalidMoveException.class, () ->
                board.makeMove(0, 0, PlayerSymbol.O)
        );
        assertTrue(ex.getMessage().contains("already occupied"));
        assertEquals(PlayerSymbol.X, board.getCell(0, 0));
        assertEquals(1, board.getMoveCount());
    }

    @Test
    @DisplayName("Horizontal win detected across all rows")
    void testHorizontalWins() throws InvalidMoveException {
        for (int row = 0; row < Board.SIZE; row++) {
            board.reset();
            board.makeMove(row, 0, PlayerSymbol.X);
            board.makeMove(row, 1, PlayerSymbol.X);
            assertFalse(board.checkWin(PlayerSymbol.X), "Should not win with only 2 marks");

            board.makeMove(row, 2, PlayerSymbol.X);
            assertTrue(board.checkWin(PlayerSymbol.X), "Row " + row + " should be a win");
            assertFalse(board.checkWin(PlayerSymbol.O));
            assertNotNull(board.getWinningLine(PlayerSymbol.X));
        }
    }

    @Test
    @DisplayName("Vertical win detected across all columns")
    void testVerticalWins() throws InvalidMoveException {
        for (int col = 0; col < Board.SIZE; col++) {
            board.reset();
            board.makeMove(0, col, PlayerSymbol.O);
            board.makeMove(1, col, PlayerSymbol.O);
            assertFalse(board.checkWin(PlayerSymbol.O), "Should not win with only 2 marks");

            board.makeMove(2, col, PlayerSymbol.O);
            assertTrue(board.checkWin(PlayerSymbol.O), "Col " + col + " should be a win");
            assertFalse(board.checkWin(PlayerSymbol.X));
            assertNotNull(board.getWinningLine(PlayerSymbol.O));
        }
    }

    @Test
    @DisplayName("Diagonal win detected on main diagonal (top-left to bottom-right)")
    void testMainDiagonalWin() throws InvalidMoveException {
        board.makeMove(0, 0, PlayerSymbol.X);
        board.makeMove(1, 1, PlayerSymbol.X);
        board.makeMove(2, 2, PlayerSymbol.X);

        assertTrue(board.checkWin(PlayerSymbol.X));
        assertFalse(board.checkWin(PlayerSymbol.O));
        int[][] line = board.getWinningLine(PlayerSymbol.X);
        assertNotNull(line);
        assertArrayEquals(new int[]{0, 0}, line[0]);
        assertArrayEquals(new int[]{1, 1}, line[1]);
        assertArrayEquals(new int[]{2, 2}, line[2]);
    }

    @Test
    @DisplayName("Diagonal win detected on anti-diagonal (top-right to bottom-left)")
    void testAntiDiagonalWin() throws InvalidMoveException {
        board.makeMove(0, 2, PlayerSymbol.O);
        board.makeMove(1, 1, PlayerSymbol.O);
        board.makeMove(2, 0, PlayerSymbol.O);

        assertTrue(board.checkWin(PlayerSymbol.O));
        assertFalse(board.checkWin(PlayerSymbol.X));
        int[][] line = board.getWinningLine(PlayerSymbol.O);
        assertNotNull(line);
        assertArrayEquals(new int[]{0, 2}, line[0]);
        assertArrayEquals(new int[]{1, 1}, line[1]);
        assertArrayEquals(new int[]{2, 0}, line[2]);
    }

    @Test
    @DisplayName("Draw detected when all 9 cells are full without a winner")
    void testDrawDetection() throws InvalidMoveException {
        /*
         * X O X
         * X X O
         * O X O
         */
        board.makeMove(0, 0, PlayerSymbol.X);
        board.makeMove(0, 1, PlayerSymbol.O);
        board.makeMove(0, 2, PlayerSymbol.X);

        board.makeMove(1, 0, PlayerSymbol.X);
        board.makeMove(1, 1, PlayerSymbol.X);
        board.makeMove(1, 2, PlayerSymbol.O);

        board.makeMove(2, 0, PlayerSymbol.O);
        board.makeMove(2, 1, PlayerSymbol.X);
        board.makeMove(2, 2, PlayerSymbol.O);

        assertTrue(board.isFull());
        assertFalse(board.checkWin(PlayerSymbol.X));
        assertFalse(board.checkWin(PlayerSymbol.O));
        assertTrue(board.isDraw());
    }

    @Test
    @DisplayName("No premature win or draw detection")
    void testNoPrematureWinOrDraw() throws InvalidMoveException {
        board.makeMove(0, 0, PlayerSymbol.X);
        board.makeMove(0, 1, PlayerSymbol.O);
        board.makeMove(1, 0, PlayerSymbol.X);

        assertFalse(board.checkWin(PlayerSymbol.X));
        assertFalse(board.checkWin(PlayerSymbol.O));
        assertFalse(board.isDraw());
        assertFalse(board.isFull());
    }

    @Test
    @DisplayName("Reset clears board and restores initial empty state")
    void testReset() throws InvalidMoveException {
        board.makeMove(0, 0, PlayerSymbol.X);
        board.makeMove(1, 1, PlayerSymbol.O);
        assertEquals(2, board.getMoveCount());

        board.reset();
        assertEquals(0, board.getMoveCount());
        assertFalse(board.isOccupied(0, 0));
        assertFalse(board.isOccupied(1, 1));
        assertEquals(PlayerSymbol.EMPTY, board.getCell(0, 0));
    }

    @Test
    @DisplayName("Defensive copies prevent external mutation of internal board state")
    void testDefensiveCopy() throws InvalidMoveException {
        board.makeMove(0, 0, PlayerSymbol.X);

        PlayerSymbol[][] gridCopy = board.getGridCopy();
        gridCopy[0][0] = PlayerSymbol.O;
        gridCopy[1][1] = PlayerSymbol.X;
        assertEquals(PlayerSymbol.X, board.getCell(0, 0), "Internal grid should not be affected by copy mutation");
        assertEquals(PlayerSymbol.EMPTY, board.getCell(1, 1));

        char[][] charCopy = board.getCharArrayCopy();
        assertEquals('X', charCopy[0][0]);
        charCopy[0][0] = 'Z';
        assertEquals(PlayerSymbol.X, board.getCell(0, 0));
    }

    @Test
    @DisplayName("Overloaded isOccupied tests index and coordinate equivalence")
    void testOverloadedIsOccupied() throws InvalidMoveException {
        assertFalse(board.isOccupied(4));
        board.makeMove(1, 1, PlayerSymbol.X); // linear index 1*3 + 1 = 4
        assertTrue(board.isOccupied(1, 1));
        assertTrue(board.isOccupied(4));
        assertFalse(board.isOccupied(-1));
        assertFalse(board.isOccupied(9));
    }
}
