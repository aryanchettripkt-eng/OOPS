package com.tictactoe.timer;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages background turn countdowns and timeout dispatching.
 * Demonstrates Java concurrency using ScheduledExecutorService, ThreadFactory,
 * daemon threads, and atomic state invalidation with generation tokens.
 *
 * <p>Locking rule: the timer's monitor is only held to update timer state. The listener
 * is always called after that monitor is released. The listener (GameController) takes
 * its own lock, and calling it while holding this monitor would create a lock cycle
 * with the controller's move handling.
 */
public class TurnTimer {
    private final ScheduledExecutorService scheduler;
    private final TimerListener listener;

    private ScheduledFuture<?> currentTask;
    private long activeTurnToken = -1;
    private final AtomicInteger secondsRemaining = new AtomicInteger(0);

    /**
     * Constructs a TurnTimer with a custom daemon thread factory.
     * Daemon threads ensure the background timer does not prevent the JVM from closing.
     *
     * @param listener listener for tick and timeout callbacks
     */
    public TurnTimer(TimerListener listener) {
        this.listener = listener;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            private final AtomicInteger count = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "TurnTimer-Worker-" + count.getAndIncrement());
                thread.setDaemon(true); // Fulfills requirement: does not block JVM exit
                return thread;
            }
        });
    }

    /**
     * Package-private constructor for testing with custom executor.
     */
    TurnTimer(TimerListener listener, ScheduledExecutorService scheduler) {
        this.listener = listener;
        this.scheduler = scheduler;
    }

    /**
     * Starts a fresh countdown timer for a new turn.
     * Atomically cancels any previously running timer task and updates the generation token.
     *
     * @param turnToken      unique generation token for this turn
     * @param timeoutSeconds number of seconds before timeout
     */
    public void start(long turnToken, int timeoutSeconds) {
        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException("Timeout seconds must be greater than zero");
        }

        synchronized (this) {
            cancelLocked(); // Cancel existing task if any
            this.activeTurnToken = turnToken;
            this.secondsRemaining.set(timeoutSeconds);
            this.currentTask = scheduler.scheduleAtFixedRate(() -> onTick(turnToken), 1, 1, TimeUnit.SECONDS);
        }

        // Notify listener of starting seconds, outside the monitor
        if (listener != null) {
            listener.onTimerTick(timeoutSeconds);
        }
    }

    /**
     * Runs once per second for the given turn. Timer state is updated under the monitor,
     * and the listener is called after the monitor is released.
     */
    private void onTick(long turnToken) {
        try {
            int remaining;
            synchronized (this) {
                // Check if this task is still active for the intended turn
                if (activeTurnToken != turnToken || Thread.currentThread().isInterrupted()) {
                    return;
                }
                remaining = secondsRemaining.decrementAndGet();
                if (remaining <= 0) {
                    cancelLocked(); // Timeout: stop this countdown
                }
            }

            if (listener == null) {
                return;
            }
            if (remaining > 0) {
                listener.onTimerTick(remaining);
            } else {
                listener.onTimeout(turnToken);
            }
        } catch (Exception e) {
            // Prevent unhandled exceptions from terminating scheduler
            System.err.println("Exception in TurnTimer task: " + e.getMessage());
        }
    }

    /**
     * Atomically stops and cancels the current countdown task and invalidates the token.
     */
    public synchronized void stop() {
        cancelLocked();
    }

    /** Caller must hold this monitor. */
    private void cancelLocked() {
        if (currentTask != null) {
            // Do not interrupt: stop() can run inside the tick task itself
            currentTask.cancel(false);
            currentTask = null;
        }
        this.activeTurnToken = -1;
    }

    /**
     * Returns the currently active turn token, or -1 if stopped.
     */
    public synchronized long getActiveTurnToken() {
        return activeTurnToken;
    }

    /**
     * Returns the remaining seconds in the active countdown.
     */
    public int getSecondsRemaining() {
        return secondsRemaining.get();
    }

    /**
     * Shuts down the background scheduler and releases thread resources.
     */
    public void shutdown() {
        stop();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
