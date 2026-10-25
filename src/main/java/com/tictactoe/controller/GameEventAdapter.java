package com.tictactoe.controller;

import com.tictactoe.model.CompletionReason;
import com.tictactoe.model.GameState;
import com.tictactoe.model.Player;
import com.tictactoe.model.PlayerSymbol;
import com.tictactoe.model.Scoreboard;

/**
 * Adapter class implementing GameEventListener with no-op default methods.
 * Useful for test cases and partial listeners.
 */
public class GameEventAdapter implements GameEventListener {
    @Override
    public void onGameStateChanged(GameState newState) {}

    @Override
    public void onTurnChanged(Player currentPlayer) {}

    @Override
    public void onMoveMade(int row, int col, PlayerSymbol symbol) {}

    @Override
    public void onTimerTick(int secondsRemaining) {}

    @Override
    public void onRoundEnded(GameState finalState, CompletionReason reason, Player winner, int[][] winningLine) {}

    @Override
    public void onBoardReset() {}

    @Override
    public void onScoreboardUpdated(Scoreboard scoreboard) {}

    @Override
    public void onStatusMessage(String message) {}

    @Override
    public void onError(String errorMessage) {}
}
