package com.tictactoe.persistence;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Storage contract for match and round history.
 * Implemented by the SQL store (primary) and the legacy CSV store (kept for import and fallback).
 */
public interface MatchStore {

    /**
     * Opens a new match and returns its identifier.
     * Stores that do not group rounds into matches may return 0.
     *
     * @param startedAt         when the match began
     * @param playerXName       Player X display name
     * @param playerOName       Player O display name
     * @param vsAI              true if Player O is the computer
     * @param turnTimeoutSeconds turn timeout used for this match
     * @return the match identifier to pass to {@link #recordRound(long, MatchRecord)}
     * @throws IOException if the write fails
     */
    long startMatch(LocalDateTime startedAt, String playerXName, String playerOName,
                    boolean vsAI, int turnTimeoutSeconds) throws IOException;

    /**
     * Records one completed round belonging to the given match.
     *
     * @param matchId identifier returned by {@link #startMatch}
     * @param round   the completed round
     * @throws IOException if the write fails
     */
    void recordRound(long matchId, MatchRecord round) throws IOException;

    /**
     * Loads every recorded round, oldest first.
     *
     * @return unmodifiable list of rounds
     * @throws IOException if the read fails
     */
    List<MatchRecord> loadAllRecords() throws IOException;

    /**
     * Deletes all stored history.
     *
     * @throws IOException if the delete fails
     */
    void clearHistory() throws IOException;
}
