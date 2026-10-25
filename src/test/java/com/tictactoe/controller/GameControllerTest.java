package com.tictactoe.controller;

import com.tictactoe.model.CompletionReason;
import com.tictactoe.model.GameState;
import com.tictactoe.model.Player;
import com.tictactoe.model.PlayerSymbol;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GameController Unit Tests")
class GameControllerTest {

    private GameController controller;

    @BeforeEach
    void setUp() {
        controller = new GameController();
        controller.startNewMatch("Alice", "Bob", 10);
    }

    @AfterEach
    void tearDown() {
        controller.shutdown();
    }

    @Test
    @DisplayName("Initial match setup configures players, state, and initial turn")
    void testInitialMatchSetup() {
        assertEquals("Alice", controller.getPlayerX().getName());
        assertEquals(PlayerSymbol.X, controller.getPlayerX().getSymbol());
        assertEquals("Bob", controller.getPlayerO().getName());
        assertEquals(PlayerSymbol.O, controller.getPlayerO().getSymbol());

        assertEquals(GameState.PLAYING, controller.getGameState());
        assertEquals(controller.getPlayerX(), controller.getCurrentPlayer());
        assertFalse(controller.isRoundCompleted());

        assertEquals(0, controller.getScoreboard().getXWins());
        assertEquals(0, controller.getScoreboard().getOWins());
        assertEquals(0, controller.getScoreboard().getDraws());
        assertEquals(0, controller.getScoreboard().getTotalRounds());
    }

    @Test
    @DisplayName("Players alternate turns correctly after valid moves")
    void testPlayerAlternation() {
        assertEquals(controller.getPlayerX(), controller.getCurrentPlayer());

        boolean move1 = controller.handleMove(0, 0);
        assertTrue(move1);
        assertEquals(controller.getPlayerO(), controller.getCurrentPlayer());
        assertEquals(PlayerSymbol.X, controller.getBoard().getCell(0, 0));

        boolean move2 = controller.handleMove(1, 1);
        assertTrue(move2);
        assertEquals(controller.getPlayerX(), controller.getCurrentPlayer());
        assertEquals(PlayerSymbol.O, controller.getBoard().getCell(1, 1));
    }

    @Test
    @DisplayName("Horizontal win ends round and updates scoreboard exactly once")
    void testWinHandling() {
        AtomicReference<GameState> finalState = new AtomicReference<>();
        AtomicReference<CompletionReason> endReason = new AtomicReference<>();
        AtomicReference<Player> winnerRef = new AtomicReference<>();

        controller.addListener(new GameEventAdapter() {
            @Override
            public void onRoundEnded(GameState state, CompletionReason reason, Player winner, int[][] line) {
                finalState.set(state);
                endReason.set(reason);
                winnerRef.set(winner);
            }
        });

        /*
         * X moves (0,0), O moves (1,0)
         * X moves (0,1), O moves (1,1)
         * X moves (0,2) -> X wins row 0
         */
        controller.handleMove(0, 0); // X
        controller.handleMove(1, 0); // O
        controller.handleMove(0, 1); // X
        controller.handleMove(1, 1); // O
        controller.handleMove(0, 2); // X wins

        assertEquals(GameState.X_WON, controller.getGameState());
        assertTrue(controller.isRoundCompleted());
        assertEquals(GameState.X_WON, finalState.get());
        assertEquals(CompletionReason.NORMAL_WIN, endReason.get());
        assertEquals("Alice", winnerRef.get().getName());

        assertEquals(1, controller.getScoreboard().getXWins());
        assertEquals(0, controller.getScoreboard().getOWins());
        assertEquals(0, controller.getScoreboard().getDraws());
        assertEquals(1, controller.getScoreboard().getTotalRounds());

        // Moves rejected after round ends
        boolean lateMove = controller.handleMove(2, 2);
        assertFalse(lateMove);
        assertEquals(1, controller.getScoreboard().getTotalRounds(), "Scoreboard must not update on rejected move");
    }

    @Test
    @DisplayName("Draw ends round and updates scoreboard exactly once")
    void testDrawHandling() {
        AtomicReference<GameState> finalState = new AtomicReference<>();
        AtomicReference<CompletionReason> endReason = new AtomicReference<>();

        controller.addListener(new GameEventAdapter() {
            @Override
            public void onRoundEnded(GameState state, CompletionReason reason, Player winner, int[][] line) {
                finalState.set(state);
                endReason.set(reason);
            }
        });

        /*
         * X O X
         * X X O
         * O X O
         */
        controller.handleMove(0, 0); // X
        controller.handleMove(0, 1); // O
        controller.handleMove(0, 2); // X
        controller.handleMove(1, 2); // O
        controller.handleMove(1, 0); // X
        controller.handleMove(2, 0); // O
        controller.handleMove(1, 1); // X
        controller.handleMove(2, 2); // O
        controller.handleMove(2, 1); // X (draw)

        assertEquals(GameState.DRAW, controller.getGameState());
        assertTrue(controller.isRoundCompleted());
        assertEquals(GameState.DRAW, finalState.get());
        assertEquals(CompletionReason.DRAW, endReason.get());

        assertEquals(0, controller.getScoreboard().getXWins());
        assertEquals(0, controller.getScoreboard().getOWins());
        assertEquals(1, controller.getScoreboard().getDraws());
        assertEquals(1, controller.getScoreboard().getTotalRounds());
    }

    @Test
    @DisplayName("Timeout awards win to opponent and updates score exactly once")
    void testTimeoutHandling() {
        AtomicReference<GameState> finalState = new AtomicReference<>();
        AtomicReference<CompletionReason> endReason = new AtomicReference<>();
        AtomicReference<Player> winnerRef = new AtomicReference<>();

        controller.addListener(new GameEventAdapter() {
            @Override
            public void onRoundEnded(GameState state, CompletionReason reason, Player winner, int[][] line) {
                finalState.set(state);
                endReason.set(reason);
                winnerRef.set(winner);
            }
        });

        // Alice (X) starts. Timeout fires on current turn token.
        long activeToken = controller.getCurrentTurnToken();
        controller.onTimeout(activeToken);

        // Bob (O) wins by forfeit
        assertEquals(GameState.O_WON, controller.getGameState());
        assertTrue(controller.isRoundCompleted());
        assertEquals(GameState.O_WON, finalState.get());
        assertEquals(CompletionReason.TIMEOUT, endReason.get());
        assertEquals("Bob", winnerRef.get().getName());

        assertEquals(0, controller.getScoreboard().getXWins());
        assertEquals(1, controller.getScoreboard().getOWins());
        assertEquals(0, controller.getScoreboard().getDraws());
        assertEquals(1, controller.getScoreboard().getTotalRounds());

        // Further timeout calls on old token are ignored
        controller.onTimeout(activeToken);
        assertEquals(1, controller.getScoreboard().getTotalRounds());
    }

    @Test
    @DisplayName("Stale timer callbacks with outdated tokens are ignored")
    void testStaleTimerCallbackIgnored() {
        long oldToken = controller.getCurrentTurnToken();

        // Alice makes a valid move -> advances token
        controller.handleMove(0, 0);
        long newToken = controller.getCurrentTurnToken();
        assertNotEquals(oldToken, newToken);

        // A delayed callback with oldToken arrives now
        controller.onTimeout(oldToken);

        // Game must still be PLAYING with Bob's turn!
        assertEquals(GameState.PLAYING, controller.getGameState());
        assertEquals("Bob", controller.getCurrentPlayer().getName());
        assertEquals(0, controller.getScoreboard().getTotalRounds());
    }

    @Test
    @DisplayName("New round resets board, clears completion, and preserves cumulative scores")
    void testNewRoundPreservesScores() {
        // Round 1: Alice wins
        controller.handleMove(0, 0); // X
        controller.handleMove(1, 0); // O
        controller.handleMove(0, 1); // X
        controller.handleMove(1, 1); // O
        controller.handleMove(0, 2); // X wins

        assertEquals(1, controller.getScoreboard().getXWins());
        assertEquals(1, controller.getScoreboard().getTotalRounds());

        // Start new round
        controller.startNewRound();

        assertEquals(GameState.PLAYING, controller.getGameState());
        assertFalse(controller.isRoundCompleted());
        assertEquals(controller.getPlayerX(), controller.getCurrentPlayer());
        assertEquals(0, controller.getBoard().getMoveCount());
        assertEquals(PlayerSymbol.EMPTY, controller.getBoard().getCell(0, 0));

        // Cumulative scores preserved
        assertEquals(1, controller.getScoreboard().getXWins());
        assertEquals(0, controller.getScoreboard().getOWins());
        assertEquals(0, controller.getScoreboard().getDraws());
        assertEquals(1, controller.getScoreboard().getTotalRounds());
    }

    @Test
    @DisplayName("Reset scores zeroes cumulative statistics")
    void testResetScores() {
        controller.onTimeout(controller.getCurrentTurnToken());
        assertEquals(1, controller.getScoreboard().getTotalRounds());

        controller.resetScores();
        assertEquals(0, controller.getScoreboard().getXWins());
        assertEquals(0, controller.getScoreboard().getOWins());
        assertEquals(0, controller.getScoreboard().getDraws());
        assertEquals(0, controller.getScoreboard().getTotalRounds());
    }

    @Test
    @DisplayName("Invalid move rejected on occupied cell and keeps turn unchanged")
    void testInvalidMoveRejected() {
        controller.handleMove(0, 0); // X placed at (0, 0)
        assertEquals(controller.getPlayerO(), controller.getCurrentPlayer());

        // Bob attempts to play in already occupied cell (0, 0)
        boolean moveResult = controller.handleMove(0, 0);
        assertFalse(moveResult);

        // Turn must still belong to Bob (O)
        assertEquals(controller.getPlayerO(), controller.getCurrentPlayer());
    }
}
