package com.tictactoe.controller;

import com.tictactoe.model.CompletionReason;
import com.tictactoe.model.GameState;
import com.tictactoe.model.Player;
import com.tictactoe.model.PlayerSymbol;
import com.tictactoe.model.Scoreboard;

/**
 * Observer listener interface for receiving game events from the GameController.
 * Decouples game logic and thread coordination from GUI presentation.
 */
public interface GameEventListener {

    void onGameStateChanged(GameState newState);

    void onTurnChanged(Player currentPlayer);

    void onMoveMade(int row, int col, PlayerSymbol symbol);

    void onTimerTick(int secondsRemaining);

    void onRoundEnded(GameState finalState, CompletionReason reason, Player winner, int[][] winningLine);

    void onBoardReset();

    void onScoreboardUpdated(Scoreboard scoreboard);

    void onStatusMessage(String message);

    void onError(String errorMessage);
}
