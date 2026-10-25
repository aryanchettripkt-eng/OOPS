package com.tictactoe.timer;

/**
 * Callback listener interface for turn timer countdown and timeout notifications.
 */
public interface TimerListener {

    /**
     * Fired every second during an active player's turn.
     *
     * @param secondsRemaining number of seconds remaining before timeout
     */
    void onTimerTick(int secondsRemaining);

    /**
     * Fired when the countdown reaches zero without a move being accepted.
     *
     * @param turnToken the generation token for the expired turn
     */
    void onTimeout(long turnToken);
}
