package com.tictactoe.model;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe scoreboard tracking cumulative session statistics:
 * Player X wins, Player O wins, Draws, and Total completed rounds.
 * Uses AtomicInteger to guarantee lock-free thread safety and atomic updates.
 */
public class Scoreboard {
    private final AtomicInteger xWins = new AtomicInteger(0);
    private final AtomicInteger oWins = new AtomicInteger(0);
    private final AtomicInteger draws = new AtomicInteger(0);
    private final AtomicInteger totalRounds = new AtomicInteger(0);

    public Scoreboard() {
    }

    /**
     * Records a win for the specified player symbol.
     * Atomically increments the player's win tally and the total rounds count.
     *
     * @param symbol winning symbol (X or O)
     */
    public synchronized void recordWin(PlayerSymbol symbol) {
        if (symbol == PlayerSymbol.X) {
            xWins.incrementAndGet();
            totalRounds.incrementAndGet();
        } else if (symbol == PlayerSymbol.O) {
            oWins.incrementAndGet();
            totalRounds.incrementAndGet();
        } else {
            throw new IllegalArgumentException("Cannot record win for EMPTY symbol");
        }
    }

    /**
     * Records a draw outcome.
     * Atomically increments the draw count and the total rounds count.
     */
    public synchronized void recordDraw() {
        draws.incrementAndGet();
        totalRounds.incrementAndGet();
    }

    /**
     * Resets all score tallies to zero.
     */
    public synchronized void reset() {
        xWins.set(0);
        oWins.set(0);
        draws.set(0);
        totalRounds.set(0);
    }

    public int getXWins() {
        return xWins.get();
    }

    public int getOWins() {
        return oWins.get();
    }

    public int getDraws() {
        return draws.get();
    }

    public int getTotalRounds() {
        return totalRounds.get();
    }

    @Override
    public String toString() {
        return String.format("Scoreboard [X: %d | O: %d | Draws: %d | Total: %d]",
                getXWins(), getOWins(), getDraws(), getTotalRounds());
    }
}
