package com.tictactoe;

import com.tictactoe.controller.GameController;
import com.tictactoe.model.Board;
import com.tictactoe.model.Scoreboard;
import com.tictactoe.persistence.MatchHistory;
import com.tictactoe.ui.GameUI;
import com.tictactoe.ui.UITheme;

import javax.swing.*;

/**
 * Application entry point for Multithreaded Tic-Tac-Toe.
 *
 * Responsibilities:
 * - Configure the platform UI Look and Feel with high-contrast text defaults
 * - Instantiate core models and persistence services
 * - Construct the authoritative GameController
 * - Launch the Swing interface strictly on the Event Dispatch Thread (EDT)
 * - Register a JVM runtime shutdown hook for clean executor termination
 */
public class Main {

    public static void main(String[] args) {
        // Apply system look and feel for native platform integration with high-contrast fallbacks
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Could not set System Look & Feel: " + e.getMessage());
        }

        // Global font & contrast defaults to prevent dark-on-dark rendering
        UIManager.put("Label.font", UITheme.FONT_BODY);
        UIManager.put("Label.foreground", UITheme.TEXT_PRIMARY);
        UIManager.put("Panel.background", UITheme.BG_CANVAS);
        UIManager.put("OptionPane.background", UITheme.BG_SURFACE);
        UIManager.put("OptionPane.messageForeground", UITheme.TEXT_PRIMARY);
        UIManager.put("Button.font", UITheme.FONT_BODY_BOLD);

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
