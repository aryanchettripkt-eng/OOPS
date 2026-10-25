package com.tictactoe.model;

import com.tictactoe.exception.InvalidMoveException;

/**
 * Represents the 3x3 Tic-Tac-Toe board.
 * Demonstrates 2D arrays, encapsulation, defensive copying, and game logic algorithms.
 */
public class Board {
    public static final int SIZE = 3;

    private final PlayerSymbol[][] grid;
    private int moveCount;

    /**
     * Constructs a new empty 3x3 board.
     */
    public Board() {
        this.grid = new PlayerSymbol[SIZE][SIZE];
        reset();
    }

    /**
     * Resets all cells on the board to EMPTY and resets the move counter.
     */
    public void reset() {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                grid[row][col] = PlayerSymbol.EMPTY;
            }
        }
        this.moveCount = 0;
    }

    /**
     * Checks if coordinates fall within the valid 3x3 grid bounds.
     *
     * @param row 0-indexed row
     * @param col 0-indexed column
     * @return true if row and col are between 0 and 2 inclusive
     */
    public boolean isValidCoordinate(int row, int col) {
        return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
    }

    /**
     * Checks if a cell is unoccupied.
     *
     * @param row row index
     * @param col column index
     * @return true if coordinate is valid and cell is EMPTY
     */
    public boolean isCellEmpty(int row, int col) {
        return isValidCoordinate(row, col) && grid[row][col] == PlayerSymbol.EMPTY;
    }

    /**
     * Checks if a cell is occupied. Demonstrates method overloading with coordinate and linear index forms.
     *
     * @param row row index
     * @param col column index
     * @return true if coordinate is valid and not EMPTY
     */
    public boolean isOccupied(int row, int col) {
        return isValidCoordinate(row, col) && grid[row][col] != PlayerSymbol.EMPTY;
    }

    /**
     * Overloaded method to check if a cell is occupied by 1D linear index (0 to 8).
     * Demonstrates method overloading.
     *
     * @param index linear index from 0 to 8
     * @return true if cell at (index / 3, index % 3) is occupied
     */
    public boolean isOccupied(int index) {
        if (index < 0 || index >= SIZE * SIZE) {
            return false;
        }
        return isOccupied(index / SIZE, index % SIZE);
    }

    /**
     * Places a symbol on the board at the specified coordinates.
     *
     * @param row    row index (0-2)
     * @param col    column index (0-2)
     * @param symbol symbol to place (X or O)
     * @return true if move was successfully placed
     * @throws InvalidMoveException if coordinates are out of bounds, cell is occupied, or symbol is invalid
     */
    public boolean makeMove(int row, int col, PlayerSymbol symbol) throws InvalidMoveException {
        if (!isValidCoordinate(row, col)) {
            throw new InvalidMoveException(row, col, "Coordinates must be between 0 and " + (SIZE - 1) + ".");
        }
        if (grid[row][col] != PlayerSymbol.EMPTY) {
            throw new InvalidMoveException(row, col, "Cell is already occupied by " + grid[row][col] + ".");
        }
        if (symbol == null || symbol == PlayerSymbol.EMPTY) {
            throw new InvalidMoveException(row, col, "Cannot place null or EMPTY symbol.");
        }

        grid[row][col] = symbol;
        moveCount++;
        return true;
    }

    /**
     * Retrieves the symbol present at the given coordinate.
     *
     * @param row row index
     * @param col col index
     * @return symbol at the cell
     * @throws IllegalArgumentException if coordinates are out of bounds
     */
    public PlayerSymbol getCell(int row, int col) {
        if (!isValidCoordinate(row, col)) {
            throw new IllegalArgumentException(String.format("Invalid coordinates: (%d, %d)", row, col));
        }
        return grid[row][col];
    }

    /**
     * Checks if the specified symbol has a winning 3-in-a-row on the board.
     *
     * @param symbol the symbol to test (X or O)
     * @return true if the symbol forms a complete row, column, or diagonal
     */
    public boolean checkWin(PlayerSymbol symbol) {
        if (symbol == null || symbol == PlayerSymbol.EMPTY) {
            return false;
        }
        return getWinningLine(symbol) != null;
    }

    /**
     * Returns the 3 coordinate pairs comprising the winning line for the symbol,
     * or null if no winning line exists.
     *
     * @param symbol symbol to test
     * @return int[3][2] coordinates {{r1,c1}, {r2,c2}, {r3,c3}} or null
     */
    public int[][] getWinningLine(PlayerSymbol symbol) {
        if (symbol == null || symbol == PlayerSymbol.EMPTY) {
            return null;
        }

        // Check 3 rows
        for (int r = 0; r < SIZE; r++) {
            if (grid[r][0] == symbol && grid[r][1] == symbol && grid[r][2] == symbol) {
                return new int[][]{{r, 0}, {r, 1}, {r, 2}};
            }
        }

        // Check 3 columns
        for (int c = 0; c < SIZE; c++) {
            if (grid[0][c] == symbol && grid[1][c] == symbol && grid[2][c] == symbol) {
                return new int[][]{{0, c}, {1, c}, {2, c}};
            }
        }

        // Check main diagonal (top-left to bottom-right)
        if (grid[0][0] == symbol && grid[1][1] == symbol && grid[2][2] == symbol) {
            return new int[][]{{0, 0}, {1, 1}, {2, 2}};
        }

        // Check anti-diagonal (top-right to bottom-left)
        if (grid[0][2] == symbol && grid[1][1] == symbol && grid[2][0] == symbol) {
            return new int[][]{{0, 2}, {1, 1}, {2, 0}};
        }

        return null;
    }

    /**
     * Checks whether all 9 cells are occupied.
     *
     * @return true if board has 9 moves
     */
    public boolean isFull() {
        return moveCount == SIZE * SIZE;
    }

    /**
     * Checks if the game has ended in a draw (board is full with no winner).
     *
     * @return true if board is full and neither X nor O has won
     */
    public boolean isDraw() {
        return isFull() && !checkWin(PlayerSymbol.X) && !checkWin(PlayerSymbol.O);
    }

    /**
     * Returns the total number of moves made in the current round.
     *
     * @return move count (0 to 9)
     */
    public int getMoveCount() {
        return moveCount;
    }

    /**
     * Returns a defensive copy of the grid as PlayerSymbol[][].
     * Callers cannot modify the internal board state.
     *
     * @return 3x3 deep copy of PlayerSymbol
     */
    public PlayerSymbol[][] getGridCopy() {
        PlayerSymbol[][] copy = new PlayerSymbol[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++) {
            System.arraycopy(grid[r], 0, copy[r], 0, SIZE);
        }
        return copy;
    }

    /**
     * Returns a defensive copy of the board as a char[][] primitive array.
     * Fulfills syllabus specification for char[][] representation.
     *
     * @return 3x3 char array of symbols ('X', 'O', ' ')
     */
    public char[][] getCharArrayCopy() {
        char[][] copy = new char[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                copy[r][c] = grid[r][c].getChar();
            }
        }
        return copy;
    }

    /**
     * Generates a human-readable ASCII representation of the board.
     *
     * @return formatted 3x3 grid string
     */
    public String getFormattedBoard() {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < SIZE; r++) {
            sb.append(" ");
            for (int c = 0; c < SIZE; c++) {
                sb.append(grid[r][c].getChar());
                if (c < SIZE - 1) {
                    sb.append(" | ");
                }
            }
            sb.append("\n");
            if (r < SIZE - 1) {
                sb.append("---+---+---\n");
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return getFormattedBoard();
    }
}
