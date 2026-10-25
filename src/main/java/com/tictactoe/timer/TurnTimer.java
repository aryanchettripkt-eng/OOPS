package com.tictactoe.timer;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages background turn countdowns and timeout dispatching.
 * Demonstrates Java concurrency using ScheduledExecutorService, ThreadFactory,
 * daemon threads, and atomic state invalidation with generation tokens.
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
    public synchronized void start(long turnToken, int timeoutSeconds) {
        stop(); // Cancel existing task if any

        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException("Timeout seconds must be greater than zero");
        }

        this.activeTurnToken = turnToken;
        this.secondsRemaining.set(timeoutSeconds);

        // Immediately notify listener of starting seconds
        if (listener != null) {
            listener.onTimerTick(timeoutSeconds);
        }

        // Schedule periodic 1-second countdown task
        this.currentTask = scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    int remaining;
                    synchronized (TurnTimer.this) {
                        // Check if this task is still active for the intended turn
                        if (activeTurnToken != turnToken || Thread.currentThread().isInterrupted()) {
                            return;
                        }
                        remaining = secondsRemaining.decrementAndGet();
                    }

                    if (remaining > 0) {
                        if (listener != null) {
                            listener.onTimerTick(remaining);
                        }
                    } else {
                        // Timeout reached!
                        synchronized (TurnTimer.this) {
                            if (activeTurnToken == turnToken) {
                                stop(); // Stop task
                                if (listener != null) {
                                    listener.onTimeout(turnToken);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // Prevent unhandled exceptions from terminating scheduler
                    System.err.println("Exception in TurnTimer task: " + e.getMessage());
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Atomically stops and cancels the current countdown task and invalidates the token.
     */
    public synchronized void stop() {
        if (currentTask != null) {
            currentTask.cancel(true);
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
