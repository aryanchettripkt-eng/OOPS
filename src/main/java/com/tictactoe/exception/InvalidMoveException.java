package com.tictactoe.exception;

/**
 * Exception thrown when a move cannot be made due to:
 * - Coordinates outside valid grid bounds
 * - Target cell being already occupied
 * - Game not being in active PLAYING state
 */
public class InvalidMoveException extends TicTacToeException {
    private final int row;
    private final int col;

    public InvalidMoveException(String message) {
        super(message);
        this.row = -1;
        this.col = -1;
    }

    public InvalidMoveException(int row, int col, String message) {
        super(String.format("Invalid move at (%d, %d): %s", row, col, message));
        this.row = row;
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }
}
