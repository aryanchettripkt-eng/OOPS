package com.tictactoe.model;

/**
 * Enumeration representing the possible states of a Tic-Tac-Toe match.
 * Enforces explicit, safe state transitions across the game lifecycle.
 */
public enum GameState {
    /**
     * Initial setup phase or waiting to start a round.
     */
    READY,

    /**
     * Round is in progress: board is accepting moves and turn timers are active.
     */
    PLAYING,

    /**
     * Round finished with Player X winning (normal win or timeout forfeiture).
     */
    X_WON,

    /**
     * Round finished with Player O winning (normal win or timeout forfeiture).
     */
    O_WON,

    /**
     * Round finished in a draw (all cells occupied without 3-in-a-row).
     */
    DRAW,

    /**
     * Game session has been closed or terminated.
     */
    CLOSED;

    /**
     * Checks if the state indicates a concluded round.
     *
     * @return true if game is in a terminal round state
     */
    public boolean isGameOver() {
        return this == X_WON || this == O_WON || this == DRAW;
    }
}
