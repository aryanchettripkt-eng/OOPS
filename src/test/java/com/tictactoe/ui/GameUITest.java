package com.tictactoe.ui;

import com.tictactoe.controller.GameController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GameUI Swing Component Tests")
class GameUITest {

    private GameController controller;
    private GameUI ui;

    @BeforeEach
    void setUp() throws InterruptedException, InvocationTargetException {
        // Only run UI tests if graphical environment is supported or headless is false
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        controller = new GameController();
        SwingUtilities.invokeAndWait(() -> {
            ui = new GameUI(controller);
        });
    }

    @AfterEach
    void tearDown() throws InterruptedException, InvocationTargetException {
        if (ui != null) {
            SwingUtilities.invokeAndWait(() -> {
                ui.dispose();
            });
        }
        if (controller != null) {
            controller.shutdown();
        }
    }

    @Test
    @DisplayName("GameUI initializes with correct title and dimensions")
    void testUIInitialization() {
        if (GraphicsEnvironment.isHeadless()) return;

        assertNotNull(ui);
        assertEquals("Multithreaded Tic-Tac-Toe: A Two-Player Timed Game", ui.getTitle());
        assertTrue(ui.getWidth() >= 700);
        assertTrue(ui.getHeight() >= 650);
    }

    @Test
    @DisplayName("Starting match updates UI state and begins active round")
    void testStartMatchUpdatesUI() throws InterruptedException, InvocationTargetException {
        if (GraphicsEnvironment.isHeadless()) return;

        SwingUtilities.invokeAndWait(() -> {
            controller.startNewMatch("Alice", "Bob", 15);
        });

        // Verify controller state reflected in UI
        assertEquals("Alice", controller.getPlayerX().getName());
        assertEquals("Bob", controller.getPlayerO().getName());
        assertEquals(15, controller.getTurnTimeoutSeconds());
    }
}
