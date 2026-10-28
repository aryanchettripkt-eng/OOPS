package com.tictactoe.model;

import com.tictactoe.controller.GameController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AIPlayer Model and Concurrency Tests")
class AIPlayerTest {

    private AIPlayer aiPlayer;
    private Board board;
    private GameController controller;

    @BeforeEach
    void setUp() {
        aiPlayer = new AIPlayer("Computer (AI)", PlayerSymbol.O);
        board = new Board();
    }

    @AfterEach
    void tearDown() {
        if (controller != null) {
            controller.shutdown();
        }
    }

    @Test
    @DisplayName("AIPlayer correctly extends Player and overrides getPlayerType")
    void testAIPlayerInheritance() {
        assertTrue(aiPlayer instanceof Player);
        assertEquals("Computer (AI)", aiPlayer.getName());
        assertEquals(PlayerSymbol.O, aiPlayer.getSymbol());
        assertEquals("AI", aiPlayer.getPlayerType());
    }

    @Test
    @DisplayName("AI takes winning move when available")
    void testAIWinningMove() throws Exception {
        /*
         * Setup: O has cells (0,0) and (0,1). (0,2) is free.
         * AI should choose (0,2) to complete row win.
         */
        board.makeMove(0, 0, PlayerSymbol.O);
        board.makeMove(0, 1, PlayerSymbol.O);
        board.makeMove(1, 0, PlayerSymbol.X);
        board.makeMove(1, 1, PlayerSymbol.X);

        int[] move = aiPlayer.calculateMove(board);
        assertNotNull(move);
        assertEquals(0, move[0]);
        assertEquals(2, move[1]);
    }

    @Test
    @DisplayName("AI blocks opponent winning move")
    void testAIBlockingMove() throws Exception {
        /*
         * Setup: X has cells (2,0) and (2,1). (2,2) is free.
         * AI (O) should block at (2,2).
         */
        board.makeMove(2, 0, PlayerSymbol.X);
        board.makeMove(2, 1, PlayerSymbol.X);
        board.makeMove(0, 0, PlayerSymbol.O);

        int[] move = aiPlayer.calculateMove(board);
        assertNotNull(move);
        assertEquals(2, move[0]);
        assertEquals(2, move[1]);
    }

    @Test
    @DisplayName("AI takes center cell on empty board")
    void testAITakesCenter() {
        int[] move = aiPlayer.calculateMove(board);
        assertNotNull(move);
        assertEquals(1, move[0]);
        assertEquals(1, move[1]);
    }

    @Test
    @DisplayName("GameController automatically triggers AI move asynchronously")
    void testControllerAutoAIMove() throws Exception {
        controller = new GameController();
        controller.startNewMatch("Human", "Bot", 10, true);

        assertTrue(controller.isVsAI());
        assertEquals(GameState.PLAYING, controller.getGameState());

        // Human makes move at (0, 0)
        boolean moveAccepted = controller.handleMove(0, 0);
        assertTrue(moveAccepted);

        // Turn should have transitioned to Bot (O)
        assertEquals("Bot", controller.getCurrentPlayer().getName());
        assertTrue(controller.getCurrentPlayer() instanceof AIPlayer);

        // Wait up to 2 seconds for AI worker thread to asynchronously make its move
        long deadline = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < deadline && controller.getCurrentPlayer() instanceof AIPlayer) {
            Thread.sleep(50);
        }

        // After AI move, turn should be back to Human (X) and move count should be 2
        assertEquals(PlayerSymbol.X, controller.getCurrentPlayer().getSymbol());
        assertEquals(2, controller.getBoard().getMoveCount());
    }
}
