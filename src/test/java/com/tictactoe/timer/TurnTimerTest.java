package com.tictactoe.timer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TurnTimer Locking Tests")
class TurnTimerTest {

    @Test
    @DisplayName("Timeout callback runs without holding the timer monitor")
    void testTimeoutCallbackDoesNotHoldTimerLock() throws InterruptedException {
        AtomicReference<TurnTimer> timerRef = new AtomicReference<>();
        AtomicBoolean holdsLockInCallback = new AtomicBoolean(true);
        CountDownLatch timedOut = new CountDownLatch(1);

        TurnTimer timer = new TurnTimer(new TimerListener() {
            @Override
            public void onTimerTick(int secondsRemaining) {
            }

            @Override
            public void onTimeout(long turnToken) {
                holdsLockInCallback.set(Thread.holdsLock(timerRef.get()));
                timedOut.countDown();
            }
        });
        timerRef.set(timer);

        try {
            timer.start(1L, 1);
            assertTrue(timedOut.await(5, TimeUnit.SECONDS), "Timeout callback should fire");
            assertFalse(holdsLockInCallback.get(), "Timer monitor must be released before the callback");
        } finally {
            timer.shutdown();
        }
    }

    @Test
    @DisplayName("Callback can call stop() on the timer without deadlocking")
    void testCallbackMayStopTimer() throws InterruptedException {
        AtomicReference<TurnTimer> timerRef = new AtomicReference<>();
        CountDownLatch finished = new CountDownLatch(1);

        TurnTimer timer = new TurnTimer(new TimerListener() {
            @Override
            public void onTimerTick(int secondsRemaining) {
            }

            @Override
            public void onTimeout(long turnToken) {
                timerRef.get().stop();
                assertEquals(-1L, timerRef.get().getActiveTurnToken());
                finished.countDown();
            }
        });
        timerRef.set(timer);

        try {
            timer.start(7L, 1);
            assertTrue(finished.await(5, TimeUnit.SECONDS), "stop() inside the callback should return");
        } finally {
            timer.shutdown();
        }
    }
}
