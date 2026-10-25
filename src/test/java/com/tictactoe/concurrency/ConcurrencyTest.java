package com.tictactoe.concurrency;

import com.tictactoe.controller.GameController;
import com.tictactoe.model.GameState;
import com.tictactoe.model.Scoreboard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Multithreading and Concurrency Tests")
class ConcurrencyTest {

    private GameController controller;

    @BeforeEach
    void setUp() {
        controller = new GameController();
        controller.startNewMatch("Player 1", "Player 2", 10);
    }

    @AfterEach
    void tearDown() {
        controller.shutdown();
    }

    @Test
    @DisplayName("Move and Timeout racing to resolve the same turn results in exactly one winner and one score update")
    void testMoveAndTimeoutRace() throws InterruptedException {
        int trials = 50;
        for (int i = 0; i < trials; i++) {
            GameController trialController = new GameController();
            trialController.startNewMatch("P1", "P2", 10);

            long turnToken = trialController.getCurrentTurnToken();
            CountDownLatch startGate = new CountDownLatch(1);
            CountDownLatch doneGate = new CountDownLatch(2);

            AtomicBoolean moveAccepted = new AtomicBoolean(false);

            // Thread 1: attempts to make a move
            Thread moveThread = new Thread(() -> {
                try {
                    startGate.await();
                    boolean res = trialController.handleMove(0, 0);
                    moveAccepted.set(res);
                } catch (InterruptedException ignored) {
                } finally {
                    doneGate.countDown();
                }
            });

            // Thread 2: attempts to deliver timeout for the same turn
            Thread timeoutThread = new Thread(() -> {
                try {
                    startGate.await();
                    trialController.onTimeout(turnToken);
                } catch (InterruptedException ignored) {
                } finally {
                    doneGate.countDown();
                }
            });

            moveThread.start();
            timeoutThread.start();

            // Release both threads simultaneously
            startGate.countDown();
            assertTrue(doneGate.await(3, TimeUnit.SECONDS), "Both threads must complete within timeout");

            Scoreboard sb = trialController.getScoreboard();
            int totalRounds = sb.getTotalRounds();

            if (moveAccepted.get()) {
                // Move succeeded: either round continued (0 rounds ended) or, if timeout resolved afterwards, it was rejected
                // If move was accepted, turn transitioned to O (or if timeout won after move, token was already invalidated)
                assertTrue(totalRounds == 0 || totalRounds == 1);
            } else {
                // Timeout resolved first: round must be over (totalRounds == 1)
                assertEquals(1, totalRounds);
                assertEquals(GameState.O_WON, trialController.getGameState());
            }

            trialController.shutdown();
        }
    }

    @Test
    @DisplayName("Simultaneous moves on the same cell: exactly one succeeds")
    void testSimultaneousMovesOnSameCell() throws InterruptedException {
        int threadsCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadsCount);
        try {
            CyclicBarrier barrier = new CyclicBarrier(threadsCount);
            AtomicInteger successCount = new AtomicInteger(0);

            for (int i = 0; i < threadsCount; i++) {
                executor.submit(() -> {
                    try {
                        barrier.await();
                        if (controller.handleMove(1, 1)) {
                            successCount.incrementAndGet();
                        }
                    } catch (Exception ignored) {
                    }
                });
            }

            executor.shutdown();
            assertTrue(executor.awaitTermination(3, TimeUnit.SECONDS));

            assertEquals(1, successCount.get(), "Exactly one move on cell (1, 1) should succeed");
            assertEquals(1, controller.getBoard().getMoveCount());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("Outdated timer events from previous rounds are safely discarded")
    void testOutdatedTimerEventsFromPreviousRound() {
        long round1Token = controller.getCurrentTurnToken();

        // Round 1 completes via winning moves
        controller.handleMove(0, 0); // X
        controller.handleMove(1, 0); // O
        controller.handleMove(0, 1); // X
        controller.handleMove(1, 1); // O
        controller.handleMove(0, 2); // X wins

        assertEquals(1, controller.getScoreboard().getTotalRounds());

        // Start Round 2
        controller.startNewRound();
        assertEquals(GameState.PLAYING, controller.getGameState());
        assertEquals(0, controller.getBoard().getMoveCount());

        // Simulate a late timeout arrival from Round 1
        controller.onTimeout(round1Token);

        // State must remain unaffected: still PLAYING in Round 2
        assertEquals(GameState.PLAYING, controller.getGameState());
        assertEquals(1, controller.getScoreboard().getTotalRounds(), "Total rounds should remain 1");
    }

    @Test
    @DisplayName("Shutdown while timer is actively counting terminates resources cleanly")
    void testShutdownWithActiveTimer() throws InterruptedException {
        GameController activeController = new GameController();
        activeController.startNewMatch("A", "B", 10);

        // Timer is running
        assertTrue(activeController.getTurnTimer().getActiveTurnToken() > 0);

        // Call shutdown
        activeController.shutdown();

        assertEquals(GameState.CLOSED, activeController.getGameState());
        assertEquals(-1, activeController.getTurnTimer().getActiveTurnToken());

        // Further moves rejected
        assertFalse(activeController.handleMove(0, 0));
    }
}
