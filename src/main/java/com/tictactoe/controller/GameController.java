package com.tictactoe.controller;

import com.tictactoe.exception.InvalidMoveException;
import com.tictactoe.model.*;
import com.tictactoe.persistence.MatchHistory;
import com.tictactoe.persistence.MatchRecord;
import com.tictactoe.timer.TimerListener;
import com.tictactoe.timer.TurnTimer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Authoritative controller coordinating the game lifecycle, turn sequencing,
 * thread-safe move resolution, timeout enforcement, scoreboard updates, and persistence.
 *
 * Demonstrates:
 * - Thread synchronization protecting shared game state
 * - Prevention of move-timeout race conditions
 * - Generation token invalidation
 * - Observer pattern decoupling UI from business rules
 * - Safe resource cleanup
 */
public class GameController implements TimerListener {
    public static final int DEFAULT_TIMEOUT_SECONDS = 10;
    public static final int MIN_TIMEOUT_SECONDS = 3;
    public static final int MAX_TIMEOUT_SECONDS = 60;

    private final Object stateLock = new Object();

    private final Board board;
    private final Scoreboard scoreboard;
    private final TurnTimer turnTimer;
    private final ScheduledExecutorService aiExecutor;
    private final MatchHistory matchHistory;
    private final List<GameEventListener> listeners;

    private Player playerX;
    private Player playerO;
    private Player currentPlayer;
    private GameState gameState;
    private int turnTimeoutSeconds;

    // Generation token to uniquely identify active turn and reject stale events
    private long currentTurnToken = 0;
    private long roundStartTimeMs = 0;
    private boolean roundCompleted = false;

    /**
     * Default constructor creating standard board, scoreboard, timer, and null history.
     */
    public GameController() {
        this(new Board(), new Scoreboard(), null, null);
    }

    /**
     * Overloaded constructor allowing injection of services.
     * Demonstrates constructor overloading and dependency injection.
     *
     * @param board        board model
     * @param scoreboard   scoreboard tracker
     * @param turnTimer    turn timer (if null, a default TurnTimer is created)
     * @param matchHistory match history store (may be null)
     */
    public GameController(Board board, Scoreboard scoreboard, TurnTimer turnTimer, MatchHistory matchHistory) {
        this.board = (board != null) ? board : new Board();
        this.scoreboard = (scoreboard != null) ? scoreboard : new Scoreboard();
        this.turnTimer = (turnTimer != null) ? turnTimer : new TurnTimer(this);
        this.aiExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "AI-Worker-Thread");
            thread.setDaemon(true);
            return thread;
        });
        this.matchHistory = matchHistory;
        this.listeners = new CopyOnWriteArrayList<>();

        this.playerX = new HumanPlayer("Player X", PlayerSymbol.X);
        this.playerO = new HumanPlayer("Player O", PlayerSymbol.O);
        this.currentPlayer = playerX;
        this.gameState = GameState.READY;
        this.turnTimeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
    }

    // Listener management
    public void addListener(GameEventListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(GameEventListener listener) {
        listeners.remove(listener);
    }

    /**
     * Starts a new game session with custom player names and turn timeout.
     * Defaults to human vs human match.
     *
     * @param nameX          Player X's name
     * @param nameO          Player O's name
     * @param timeoutSeconds turn timeout in seconds
     */
    public void startNewMatch(String nameX, String nameO, int timeoutSeconds) {
        startNewMatch(nameX, nameO, timeoutSeconds, false);
    }

    /**
     * Overloaded method to start a new game session specifying whether Player O is an AI opponent.
     * Demonstrates method overloading and polymorphism.
     *
     * @param nameX          Player X's name
     * @param nameO          Player O's name
     * @param timeoutSeconds turn timeout in seconds
     * @param vsAI           true if Player O should be an AI player
     */
    public void startNewMatch(String nameX, String nameO, int timeoutSeconds, boolean vsAI) {
        synchronized (stateLock) {
            String sanitizedX = (nameX == null || nameX.isBlank()) ? "Player X" : nameX.trim();
            String sanitizedO = (nameO == null || nameO.isBlank()) ? (vsAI ? "Computer (AI)" : "Player O") : nameO.trim();

            if (timeoutSeconds < MIN_TIMEOUT_SECONDS || timeoutSeconds > MAX_TIMEOUT_SECONDS) {
                timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
            }

            this.playerX = new HumanPlayer(sanitizedX, PlayerSymbol.X);
            this.playerO = vsAI ? new AIPlayer(sanitizedO, PlayerSymbol.O) : new HumanPlayer(sanitizedO, PlayerSymbol.O);
            this.turnTimeoutSeconds = timeoutSeconds;

            scoreboard.reset();
            notifyScoreboardUpdated();

            startNewRound();
        }
    }

    /**
     * Starts a fresh round on the current board without resetting cumulative scores.
     */
    public void startNewRound() {
        synchronized (stateLock) {
            if (gameState == GameState.CLOSED) {
                return;
            }

            // Invalidate current timer
            turnTimer.stop();
            currentTurnToken++;

            board.reset();
            roundCompleted = false;
            currentPlayer = playerX;
            gameState = GameState.PLAYING;
            roundStartTimeMs = System.currentTimeMillis();

            notifyBoardReset();
            notifyGameStateChanged();
            notifyTurnChanged();
            notifyStatusMessage(String.format("Round started! %s (%s) moves first. [%ds turn timer]",
                    playerX.getName(), playerX.getSymbol(), turnTimeoutSeconds));

            // Start timer for Player X
            turnTimer.start(currentTurnToken, turnTimeoutSeconds);

            // If Player X is AI, trigger AI move
            triggerAIMoveIfApplicable(currentTurnToken);
        }
    }

    /**
     * Attempts to make a move at the specified row and column.
     * Synchronized to guarantee atomic validation, move placement, timer invalidation,
     * and round completion.
     *
     * @param row row index (0-2)
     * @param col col index (0-2)
     * @return true if move was accepted
     */
    public boolean handleMove(int row, int col) {
        synchronized (stateLock) {
            if (gameState == GameState.READY) {
                // Auto-start round on move so board is immediately interactive
                startNewRound();
            }

            if (gameState != GameState.PLAYING || roundCompleted) {
                notifyError("Move rejected: Game is not in active PLAYING state.");
                return false;
            }

            // Immediately stop/invalidate the active timer for this turn
            turnTimer.stop();

            try {
                board.makeMove(row, col, currentPlayer.getSymbol());
            } catch (InvalidMoveException e) {
                // If move was invalid, restart timer with remaining seconds and inform user
                notifyError(e.getMessage());
                turnTimer.start(currentTurnToken, Math.max(1, turnTimer.getSecondsRemaining()));
                return false;
            }

            // Move was successful
            notifyMoveMade(row, col, currentPlayer.getSymbol());

            // 1. Check Win
            if (board.checkWin(currentPlayer.getSymbol())) {
                int[][] winningLine = board.getWinningLine(currentPlayer.getSymbol());
                GameState finalState = (currentPlayer.getSymbol() == PlayerSymbol.X) ? GameState.X_WON : GameState.O_WON;
                completeRound(finalState, CompletionReason.NORMAL_WIN, currentPlayer, winningLine);
                return true;
            }

            // 2. Check Draw
            if (board.isDraw()) {
                completeRound(GameState.DRAW, CompletionReason.DRAW, null, null);
                return true;
            }

            // 3. Switch turn to opponent
            currentPlayer = (currentPlayer == playerX) ? playerO : playerX;
            currentTurnToken++;

            notifyTurnChanged();
            notifyStatusMessage(String.format("%s's turn (%s)", currentPlayer.getName(), currentPlayer.getSymbol()));

            // Start fresh timer for next player
            turnTimer.start(currentTurnToken, turnTimeoutSeconds);

            // Trigger AI move if next player is AI
            triggerAIMoveIfApplicable(currentTurnToken);
            return true;
        }
    }

    /**
     * Schedules an asynchronous AI move if the current active player is an AIPlayer.
     * Uses stateLock and generation tokens to guarantee race-free coordination.
     *
     * @param turnToken active generation token
     */
    private void triggerAIMoveIfApplicable(final long turnToken) {
        if (currentPlayer instanceof AIPlayer aiPlayer) {
            notifyStatusMessage(String.format("%s is thinking...", aiPlayer.getName()));
            aiExecutor.schedule(() -> {
                synchronized (stateLock) {
                    if (turnToken != currentTurnToken || gameState != GameState.PLAYING || roundCompleted) {
                        return;
                    }
                    int[] bestMove = aiPlayer.calculateMove(board);
                    if (bestMove != null) {
                        handleMove(bestMove[0], bestMove[1]);
                    }
                }
            }, 450, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Callback from TurnTimer when turn countdown reaches zero.
     * Synchronized to guarantee safe resolution against concurrent move attempts.
     *
     * @param turnToken generation token of expired turn
     */
    @Override
    public void onTimeout(long turnToken) {
        synchronized (stateLock) {
            // Guard: Reject stale callbacks or timeouts when round is already finished
            if (turnToken != currentTurnToken || gameState != GameState.PLAYING || roundCompleted) {
                return;
            }

            // Current player forfeits round; opponent is declared winner
            Player timedOutPlayer = currentPlayer;
            Player winner = (timedOutPlayer == playerX) ? playerO : playerX;
            GameState finalState = (winner == playerX) ? GameState.X_WON : GameState.O_WON;

            notifyStatusMessage(String.format("Time expired! %s timed out. %s wins by forfeit!",
                    timedOutPlayer.getName(), winner.getName()));

            completeRound(finalState, CompletionReason.TIMEOUT, winner, null);
        }
    }

    @Override
    public void onTimerTick(int secondsRemaining) {
        // Forward timer ticks to UI listeners
        for (GameEventListener listener : listeners) {
            listener.onTimerTick(secondsRemaining);
        }
    }

    /**
     * Atomically finalizes the current round.
     * Must be called while holding stateLock.
     */
    private void completeRound(GameState finalState, CompletionReason reason, Player winner, int[][] winningLine) {
        if (roundCompleted) {
            return;
        }
        roundCompleted = true;
        gameState = finalState;
        turnTimer.stop();

        long durationSeconds = Math.max(0, (System.currentTimeMillis() - roundStartTimeMs) / 1000);

        // Update scoreboard exactly once
        if (reason == CompletionReason.DRAW) {
            scoreboard.recordDraw();
        } else if (winner != null) {
            scoreboard.recordWin(winner.getSymbol());
        }

        // Persist to match history
        if (matchHistory != null) {
            try {
                MatchRecord record = new MatchRecord(
                        LocalDateTime.now(),
                        playerX.getName(),
                        playerO.getName(),
                        winner != null ? winner.getName() : "Draw",
                        winner != null ? winner.getSymbol() : PlayerSymbol.EMPTY,
                        reason,
                        durationSeconds
                );
                matchHistory.appendRecord(record);
            } catch (Exception e) {
                notifyError("Failed to save match history: " + e.getMessage());
            }
        }

        notifyScoreboardUpdated();
        notifyRoundEnded(finalState, reason, winner, winningLine);
        notifyGameStateChanged();
    }

    /**
     * Resets cumulative scoreboard tallies.
     */
    public void resetScores() {
        synchronized (stateLock) {
            scoreboard.reset();
            notifyScoreboardUpdated();
            notifyStatusMessage("Scoreboard has been reset.");
        }
    }

    /**
     * Shuts down background timer threads, AI worker threads, and cleans up resources.
     */
    public void shutdown() {
        synchronized (stateLock) {
            gameState = GameState.CLOSED;
            turnTimer.shutdown();
            aiExecutor.shutdownNow();
            notifyGameStateChanged();
        }
    }

    /**
     * Checks if current match is configured against an AI opponent.
     *
     * @return true if Player O or Player X is an AIPlayer
     */
    public boolean isVsAI() {
        synchronized (stateLock) {
            return (playerX instanceof AIPlayer) || (playerO instanceof AIPlayer);
        }
    }

    // Accessor methods
    public Board getBoard() {
        return board;
    }

    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    public TurnTimer getTurnTimer() {
        return turnTimer;
    }

    public MatchHistory getMatchHistory() {
        return matchHistory;
    }

    public Player getPlayerX() {
        synchronized (stateLock) {
            return playerX;
        }
    }

    public Player getPlayerO() {
        synchronized (stateLock) {
            return playerO;
        }
    }

    public Player getCurrentPlayer() {
        synchronized (stateLock) {
            return currentPlayer;
        }
    }

    public GameState getGameState() {
        synchronized (stateLock) {
            return gameState;
        }
    }

    public int getTurnTimeoutSeconds() {
        return turnTimeoutSeconds;
    }

    public long getCurrentTurnToken() {
        synchronized (stateLock) {
            return currentTurnToken;
        }
    }

    public boolean isRoundCompleted() {
        synchronized (stateLock) {
            return roundCompleted;
        }
    }

    // Listener notification helpers
    private void notifyGameStateChanged() {
        for (GameEventListener l : listeners) {
            l.onGameStateChanged(gameState);
        }
    }

    private void notifyTurnChanged() {
        for (GameEventListener l : listeners) {
            l.onTurnChanged(currentPlayer);
        }
    }

    private void notifyMoveMade(int r, int c, PlayerSymbol s) {
        for (GameEventListener l : listeners) {
            l.onMoveMade(r, c, s);
        }
    }

    private void notifyRoundEnded(GameState s, CompletionReason r, Player w, int[][] line) {
        for (GameEventListener l : listeners) {
            l.onRoundEnded(s, r, w, line);
        }
    }

    private void notifyBoardReset() {
        for (GameEventListener l : listeners) {
            l.onBoardReset();
        }
    }

    private void notifyScoreboardUpdated() {
        for (GameEventListener l : listeners) {
            l.onScoreboardUpdated(scoreboard);
        }
    }

    private void notifyStatusMessage(String msg) {
        for (GameEventListener l : listeners) {
            l.onStatusMessage(msg);
        }
    }

    private void notifyError(String err) {
        for (GameEventListener l : listeners) {
            l.onError(err);
        }
    }
}
