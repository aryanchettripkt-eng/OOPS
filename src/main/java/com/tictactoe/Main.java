package com.tictactoe;

import com.tictactoe.controller.GameController;
import com.tictactoe.model.Board;
import com.tictactoe.model.Scoreboard;
import com.tictactoe.persistence.MatchHistory;
import com.tictactoe.ui.GameUI;

import javax.swing.*;

/**
 * Application entry point for Multithreaded Tic-Tac-Toe.
 *
 * Responsibilities:
 * - Configure the platform UI Look and Feel
 * - Instantiate core models and persistence services
 * - Construct the authoritative GameController
 * - Launch the Swing interface strictly on the Event Dispatch Thread (EDT)
 * - Register a JVM runtime shutdown hook for clean executor termination
 */
public class Main {

    public static void main(String[] args) {
        // Apply system look and feel for native platform integration
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Fall back to standard cross-platform look and feel if system is unavailable
            System.err.println("Could not set System Look & Feel: " + e.getMessage());
        }

        // Initialize core services
        Board board = new Board();
        Scoreboard scoreboard = new Scoreboard();
        MatchHistory matchHistory = new MatchHistory();
        GameController controller = new GameController(board, scoreboard, null, matchHistory);

        // Register runtime shutdown hook to ensure background threads stop if JVM exits abruptly
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("JVM shutdown hook triggered: Stopping background threads...");
            controller.shutdown();
        }, "Shutdown-Hook-Thread"));

        // Launch GUI on Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            try {
                GameUI ui = new GameUI(controller);
                ui.setVisible(true);
            } catch (Exception e) {
                System.err.println("Fatal error launching Game UI: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
