package com.tictactoe.ui;

import com.tictactoe.controller.GameController;
import com.tictactoe.model.GameState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@DisplayName("GameUI Swing Component Tests")
class GameUITest {

    private GameController controller;
    private GameUI ui;

    @BeforeEach
    void setUp() throws InterruptedException, InvocationTargetException {
        assumeFalse(GraphicsEnvironment.isHeadless(), "GUI tests need a display; skipped in headless mode");

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
        assumeFalse(GraphicsEnvironment.isHeadless(), "GUI tests need a display; skipped in headless mode");

        assertNotNull(ui);
        assertEquals("Multithreaded Tic-Tac-Toe: A Two-Player Timed Game", ui.getTitle());
        assertTrue(ui.getWidth() >= 700);
        assertTrue(ui.getHeight() >= 650);
    }

    @Test
    @DisplayName("Starting match updates UI state and begins active round")
    void testStartMatchUpdatesUI() throws InterruptedException, InvocationTargetException {
        assumeFalse(GraphicsEnvironment.isHeadless(), "GUI tests need a display; skipped in headless mode");

        SwingUtilities.invokeAndWait(() -> {
            controller.startNewMatch("Alice", "Bob", 15);
        });

        // Verify controller state reflected in UI
        assertEquals("Alice", controller.getPlayerX().getName());
        assertEquals("Bob", controller.getPlayerO().getName());
        assertEquals(15, controller.getTurnTimeoutSeconds());
    }

    @Test
    @DisplayName("Board cells stay disabled until Start Match is pressed")
    void testBoardDisabledBeforeStartMatch() throws InterruptedException, InvocationTargetException {
        assumeFalse(GraphicsEnvironment.isHeadless(), "GUI tests need a display; skipped in headless mode");

        java.util.List<CellButton> cells = collectCells(ui);
        assertEquals(9, cells.size());
        for (CellButton cell : cells) {
            assertFalse(cell.isEnabled(), "Cell must not be clickable before a match starts");
        }
        assertEquals(GameState.READY, controller.getGameState());
    }

    @Test
    @DisplayName("Start Match enables the board and begins a round")
    void testStartMatchEnablesBoard() throws InterruptedException, InvocationTargetException {
        assumeFalse(GraphicsEnvironment.isHeadless(), "GUI tests need a display; skipped in headless mode");

        SwingUtilities.invokeAndWait(() -> controller.startNewMatch("Alice", "Bob", 15, false));
        SwingUtilities.invokeAndWait(() -> { }); // let queued state-change callbacks run

        assertEquals(GameState.PLAYING, controller.getGameState());
        for (CellButton cell : collectCells(ui)) {
            assertTrue(cell.isEnabled(), "Cells must be clickable once a match is running");
        }
    }

    private static java.util.List<CellButton> collectCells(java.awt.Container root) {
        java.util.List<CellButton> found = new java.util.ArrayList<>();
        for (java.awt.Component c : root.getComponents()) {
            if (c instanceof CellButton cell) {
                found.add(cell);
            } else if (c instanceof java.awt.Container child) {
                found.addAll(collectCells(child));
            }
        }
        return found;
    }
}
