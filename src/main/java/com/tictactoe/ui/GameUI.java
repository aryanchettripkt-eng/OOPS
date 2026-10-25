package com.tictactoe.ui;

import com.tictactoe.controller.GameController;
import com.tictactoe.controller.GameEventListener;
import com.tictactoe.model.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Main graphical interface for the Multithreaded Tic-Tac-Toe desktop application.
 *
 * Implements a modern, high-contrast visual design system ensuring complete,
 * effortless readability of all text, buttons, timers, and game boards on any display.
 *
 * All state updates are marshalled onto the Swing Event Dispatch Thread (EDT).
 */
public class GameUI extends JFrame implements GameEventListener {

    private final GameController controller;

    // Board cell buttons
    private final CellButton[][] cellButtons = new CellButton[Board.SIZE][Board.SIZE];

    // Setup input fields
    private JTextField playerXField;
    private JTextField playerOField;
    private JSpinner timeoutSpinner;
    private ModernButton startMatchBtn;
    private ModernButton historyBtn;

    // Status & Timer components
    private JLabel currentPlayerLabel;
    private JLabel timerLabel;
    private JProgressBar timerProgressBar;
    private JLabel statusBannerLabel;

    // Scoreboard labels
    private JLabel xWinsLabel;
    private JLabel oWinsLabel;
    private JLabel drawsLabel;
    private JLabel totalRoundsLabel;

    // Control buttons
    private ModernButton newRoundBtn;
    private ModernButton resetScoresBtn;

    public GameUI(GameController controller) {
        super("Multithreaded Tic-Tac-Toe: A Two-Player Timed Game");
        this.controller = controller;

        initializeUI();
        controller.addListener(this);

        // Window closing hook for clean resource disposal
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleExit();
            }
        });

        // Sync initial state
        updateScoreboardDisplay(controller.getScoreboard());
    }

    private void initializeUI() {
        setSize(920, 780);
        setMinimumSize(new Dimension(840, 720));
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.BG_CANVAS);
        setLayout(new BorderLayout(0, 0));

        // 1. Top Setup Bar
        add(createTopSetupPanel(), BorderLayout.NORTH);

        // 2. Center Game Area (Status banner + 3x3 Board + Timer)
        add(createCenterPanel(), BorderLayout.CENTER);

        // 3. Right Scoreboard & Stats Panel
        add(createSidePanel(), BorderLayout.EAST);

        // 4. Bottom Control Bar
        add(createBottomControlPanel(), BorderLayout.SOUTH);
    }

    private JPanel createTopSetupPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        panel.setBackground(UITheme.BG_SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_LIGHT),
                new EmptyBorder(8, 12, 8, 12)
        ));

        // Player X input
        JLabel xTag = new JLabel("X:");
        xTag.setFont(UITheme.FONT_BODY_BOLD);
        xTag.setForeground(UITheme.COLOR_X);
        playerXField = createStyledTextField("Player X");

        // Player O input
        JLabel oTag = new JLabel("O:");
        oTag.setFont(UITheme.FONT_BODY_BOLD);
        oTag.setForeground(UITheme.COLOR_O);
        playerOField = createStyledTextField("Player O");

        // Timeout spinner
        JLabel timeoutTag = new JLabel("Timer:");
        timeoutTag.setFont(UITheme.FONT_BODY_BOLD);
        timeoutTag.setForeground(UITheme.TEXT_SECONDARY);

        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(
                GameController.DEFAULT_TIMEOUT_SECONDS,
                GameController.MIN_TIMEOUT_SECONDS,
                GameController.MAX_TIMEOUT_SECONDS,
                1
        );
        timeoutSpinner = new JSpinner(spinnerModel);
        timeoutSpinner.setFont(UITheme.FONT_INPUT);
        timeoutSpinner.setPreferredSize(new Dimension(50, 32));
        JComponent editor = timeoutSpinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor defaultEditor) {
            defaultEditor.getTextField().setFont(UITheme.FONT_INPUT);
            defaultEditor.getTextField().setForeground(UITheme.TEXT_PRIMARY);
            defaultEditor.getTextField().setBackground(UITheme.BG_SURFACE);
        }

        JLabel secSuffix = new JLabel("s");
        secSuffix.setFont(UITheme.FONT_BODY_BOLD);
        secSuffix.setForeground(UITheme.TEXT_MUTED);

        // Action buttons
        startMatchBtn = new ModernButton("Start Match", UITheme.COLOR_PRIMARY_BTN);
        startMatchBtn.addActionListener(e -> handleStartMatch());

        historyBtn = new ModernButton("Match History", UITheme.COLOR_SECONDARY_BTN);
        historyBtn.addActionListener(e -> showHistoryDialog());

        panel.add(xTag);
        panel.add(playerXField);
        panel.add(Box.createHorizontalStrut(4));
        panel.add(oTag);
        panel.add(playerOField);
        panel.add(Box.createHorizontalStrut(4));
        panel.add(timeoutTag);
        panel.add(timeoutSpinner);
        panel.add(secSuffix);
        panel.add(Box.createHorizontalStrut(6));
        panel.add(startMatchBtn);
        panel.add(historyBtn);

        return panel;
    }

    private JTextField createStyledTextField(String defaultText) {
        JTextField tf = new JTextField(defaultText, 8);
        tf.setFont(UITheme.FONT_INPUT);
        tf.setForeground(UITheme.TEXT_PRIMARY);
        tf.setBackground(UITheme.BG_SURFACE);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_LIGHT, 2),
                BorderFactory.createEmptyBorder(3, 6, 3, 6)
        ));
        tf.setPreferredSize(new Dimension(95, 32));
        return tf;
    }

    private JPanel createCenterPanel() {
        JPanel centerContainer = new JPanel(new BorderLayout(12, 12));
        centerContainer.setBackground(UITheme.BG_CANVAS);
        centerContainer.setBorder(new EmptyBorder(16, 24, 16, 16));

        // 1. Status & Countdown Card
        JPanel bannerCard = new JPanel(new BorderLayout(12, 10));
        bannerCard.setBackground(UITheme.BG_SURFACE);
        bannerCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_LIGHT, 1),
                new EmptyBorder(14, 20, 14, 20)
        ));

        currentPlayerLabel = new JLabel("Ready! Click 'Start Match' to begin.");
        currentPlayerLabel.setFont(UITheme.FONT_SECTION);
        currentPlayerLabel.setForeground(UITheme.TEXT_PRIMARY);

        JPanel timerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        timerPanel.setOpaque(false);

        JLabel timerPrefix = new JLabel("Turn Timer:");
        timerPrefix.setFont(UITheme.FONT_BODY_BOLD);
        timerPrefix.setForeground(UITheme.TEXT_SECONDARY);

        timerLabel = new JLabel(GameController.DEFAULT_TIMEOUT_SECONDS + "s");
        timerLabel.setFont(UITheme.FONT_TIMER);
        timerLabel.setForeground(UITheme.COLOR_WIN);

        timerPanel.add(timerPrefix);
        timerPanel.add(timerLabel);

        bannerCard.add(currentPlayerLabel, BorderLayout.WEST);
        bannerCard.add(timerPanel, BorderLayout.EAST);

        // Timer Progress Bar
        timerProgressBar = new JProgressBar(0, GameController.DEFAULT_TIMEOUT_SECONDS);
        timerProgressBar.setValue(GameController.DEFAULT_TIMEOUT_SECONDS);
        timerProgressBar.setForeground(UITheme.COLOR_WIN);
        timerProgressBar.setBackground(new Color(0xE2, 0xE8, 0xF0));
        timerProgressBar.setBorderPainted(false);
        timerProgressBar.setPreferredSize(new Dimension(100, 8));
        bannerCard.add(timerProgressBar, BorderLayout.SOUTH);

        centerContainer.add(bannerCard, BorderLayout.NORTH);

        // 2. 3x3 Grid Board Panel
        JPanel boardPanel = new JPanel(new GridLayout(Board.SIZE, Board.SIZE, 10, 10));
        boardPanel.setBackground(UITheme.BG_CANVAS);

        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                final int r = row;
                final int c = col;
                CellButton cell = new CellButton();
                cell.addActionListener(e -> controller.handleMove(r, c));

                cellButtons[row][col] = cell;
                boardPanel.add(cell);
            }
        }

        centerContainer.add(boardPanel, BorderLayout.CENTER);

        // 3. Status message strip at the bottom
        JPanel statusStrip = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        statusStrip.setBackground(UITheme.BG_SURFACE);
        statusStrip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_LIGHT, 1),
                new EmptyBorder(6, 14, 6, 14)
        ));

        statusBannerLabel = new JLabel("Configure names & click Start Match.");
        statusBannerLabel.setFont(UITheme.FONT_BODY_BOLD);
        statusBannerLabel.setForeground(UITheme.TEXT_SECONDARY);
        statusStrip.add(statusBannerLabel);

        centerContainer.add(statusStrip, BorderLayout.SOUTH);

        return centerContainer;
    }

    private JPanel createSidePanel() {
        JPanel sidePanel = new JPanel(new BorderLayout());
        sidePanel.setBackground(UITheme.BG_CANVAS);
        sidePanel.setPreferredSize(new Dimension(240, 0));
        sidePanel.setBorder(new EmptyBorder(16, 0, 16, 24));

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(UITheme.BG_SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_LIGHT, 1),
                new EmptyBorder(18, 16, 18, 16)
        ));

        JLabel title = new JLabel("SCOREBOARD");
        title.setFont(UITheme.FONT_SECTION);
        title.setForeground(UITheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(16));

        // Stat tiles
        xWinsLabel = createStatTile(card, "Player X Wins", "0", UITheme.COLOR_X, UITheme.CELL_BG_X, UITheme.CELL_BORDER_X);
        card.add(Box.createVerticalStrut(10));
        oWinsLabel = createStatTile(card, "Player O Wins", "0", UITheme.COLOR_O, UITheme.CELL_BG_O, UITheme.CELL_BORDER_O);
        card.add(Box.createVerticalStrut(10));
        drawsLabel = createStatTile(card, "Draws", "0", UITheme.TEXT_SECONDARY, UITheme.BG_CARD, UITheme.BORDER_LIGHT);
        card.add(Box.createVerticalStrut(10));
        totalRoundsLabel = createStatTile(card, "Total Rounds", "0", UITheme.COLOR_WIN, UITheme.CELL_BG_WIN, UITheme.CELL_BORDER_WIN);

        card.add(Box.createVerticalGlue());

        // Quick Rules Box
        JPanel rulesBox = new JPanel();
        rulesBox.setLayout(new BoxLayout(rulesBox, BoxLayout.Y_AXIS));
        rulesBox.setBackground(UITheme.BG_CARD);
        rulesBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_LIGHT, 1),
                new EmptyBorder(10, 12, 10, 12)
        ));

        JLabel rulesTitle = new JLabel("Turn Rules:");
        rulesTitle.setFont(UITheme.FONT_BODY_BOLD);
        rulesTitle.setForeground(UITheme.TEXT_PRIMARY);
        rulesBox.add(rulesTitle);
        rulesBox.add(Box.createVerticalStrut(6));

        addRuleBullet(rulesBox, "• Player X moves first");
        addRuleBullet(rulesBox, "• Countdown resets each turn");
        addRuleBullet(rulesBox, "• Timeout = opponent wins!");

        card.add(rulesBox);

        sidePanel.add(card, BorderLayout.CENTER);
        return sidePanel;
    }

    private void addRuleBullet(JPanel container, String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UITheme.FONT_BODY);
        lbl.setForeground(UITheme.TEXT_SECONDARY);
        container.add(lbl);
        container.add(Box.createVerticalStrut(3));
    }

    private JLabel createStatTile(JPanel container, String title, String value, Color textColor, Color bgColor, Color borderColor) {
        JPanel tile = new JPanel();
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setBackground(bgColor);
        tile.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, 1),
                new EmptyBorder(8, 12, 8, 12)
        ));
        tile.setMaximumSize(new Dimension(210, 68));
        tile.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(UITheme.FONT_SCORE_LABEL);
        titleLbl.setForeground(UITheme.TEXT_SECONDARY);
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel valLbl = new JLabel(value);
        valLbl.setFont(UITheme.FONT_SCORE_VALUE);
        valLbl.setForeground(textColor);
        valLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        tile.add(titleLbl);
        tile.add(Box.createVerticalStrut(2));
        tile.add(valLbl);

        container.add(tile);
        return valLbl;
    }

    private JPanel createBottomControlPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setBackground(UITheme.BG_SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_LIGHT),
                new EmptyBorder(10, 24, 10, 24)
        ));

        // Left side: View Match History
        JPanel leftControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftControls.setOpaque(false);
        ModernButton bottomHistoryBtn = new ModernButton("View Match History", UITheme.COLOR_SECONDARY_BTN);
        bottomHistoryBtn.addActionListener(e -> showHistoryDialog());
        leftControls.add(bottomHistoryBtn);

        // Right side: Round and Game Controls
        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightControls.setOpaque(false);

        newRoundBtn = new ModernButton("New Round", UITheme.COLOR_PRIMARY_BTN);
        newRoundBtn.addActionListener(e -> controller.startNewRound());

        resetScoresBtn = new ModernButton("Reset Scores", UITheme.COLOR_SECONDARY_BTN);
        resetScoresBtn.addActionListener(e -> handleResetScores());

        ModernButton exitBtn = new ModernButton("Exit", new Color(0xDC, 0x26, 0x26));
        exitBtn.addActionListener(e -> handleExit());

        rightControls.add(newRoundBtn);
        rightControls.add(resetScoresBtn);
        rightControls.add(exitBtn);

        panel.add(leftControls, BorderLayout.WEST);
        panel.add(rightControls, BorderLayout.EAST);

        return panel;
    }

    private void handleStartMatch() {
        String x = playerXField.getText().trim();
        String o = playerOField.getText().trim();
        int timeout = (Integer) timeoutSpinner.getValue();

        if (x.isEmpty() || o.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Player names cannot be blank.",
                    "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        timerProgressBar.setMaximum(timeout);
        controller.startNewMatch(x, o, timeout);
    }

    private void handleResetScores() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Reset scoreboard tallies back to 0-0?\n(Current session scores will be cleared; persistent CSV history remains saved.)",
                "Confirm Reset Scores",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            controller.resetScores();
        }
    }

    private void handleExit() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to exit the application?",
                "Exit Confirmation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            controller.shutdown();
            dispose();
            System.exit(0);
        }
    }

    private void showHistoryDialog() {
        HistoryDialog dialog = new HistoryDialog(this, controller.getMatchHistory());
        dialog.setVisible(true);
    }

    // =========================================================================
    // GameEventListener Callbacks (Marshalled onto Swing EDT)
    // =========================================================================

    @Override
    public void onGameStateChanged(GameState newState) {
        SwingUtilities.invokeLater(() -> {
            boolean isPlaying = (newState == GameState.PLAYING);
            for (int r = 0; r < Board.SIZE; r++) {
                for (int c = 0; c < Board.SIZE; c++) {
                    cellButtons[r][c].setEnabled(isPlaying || cellButtons[r][c].getSymbol() != PlayerSymbol.EMPTY);
                }
            }
        });
    }

    @Override
    public void onTurnChanged(Player currentPlayer) {
        SwingUtilities.invokeLater(() -> {
            Color tagColor = (currentPlayer.getSymbol() == PlayerSymbol.X) ? UITheme.COLOR_X : UITheme.COLOR_O;
            currentPlayerLabel.setText(String.format("Current Turn: %s (%s)", currentPlayer.getName(), currentPlayer.getSymbol()));
            currentPlayerLabel.setForeground(tagColor);
        });
    }

    @Override
    public void onMoveMade(int row, int col, PlayerSymbol symbol) {
        SwingUtilities.invokeLater(() -> {
            cellButtons[row][col].setSymbol(symbol);
        });
    }

    @Override
    public void onTimerTick(int secondsRemaining) {
        SwingUtilities.invokeLater(() -> {
            timerLabel.setText(secondsRemaining + "s");
            timerProgressBar.setValue(secondsRemaining);

            if (secondsRemaining > 5) {
                timerLabel.setForeground(UITheme.COLOR_WIN);
                timerProgressBar.setForeground(UITheme.COLOR_WIN);
            } else if (secondsRemaining > 2) {
                timerLabel.setForeground(UITheme.COLOR_WARNING);
                timerProgressBar.setForeground(UITheme.COLOR_WARNING);
            } else {
                timerLabel.setForeground(UITheme.COLOR_O);
                timerProgressBar.setForeground(UITheme.COLOR_O);
            }
        });
    }

    @Override
    public void onRoundEnded(GameState finalState, CompletionReason reason, Player winner, int[][] winningLine) {
        SwingUtilities.invokeLater(() -> {
            timerLabel.setText("0s");
            timerProgressBar.setValue(0);

            // Highlight winning line cells
            if (winningLine != null) {
                for (int[] coord : winningLine) {
                    cellButtons[coord[0]][coord[1]].setWinningCell(true);
                }
            }

            String message;
            if (finalState == GameState.DRAW) {
                message = "Round finished in a DRAW!";
                currentPlayerLabel.setText(message);
                currentPlayerLabel.setForeground(UITheme.TEXT_SECONDARY);
            } else {
                String winDesc = (reason == CompletionReason.TIMEOUT) ? " [TIMEOUT FORFEIT]" : " [VICTORY]";
                message = String.format("%s (%s) WON THE ROUND!%s",
                        winner.getName(), winner.getSymbol(), winDesc);
                currentPlayerLabel.setText(message);
                currentPlayerLabel.setForeground(UITheme.COLOR_WIN);
            }

            statusBannerLabel.setText(message + " Click 'New Round' to play again.");
        });
    }

    @Override
    public void onBoardReset() {
        SwingUtilities.invokeLater(() -> {
            for (int r = 0; r < Board.SIZE; r++) {
                for (int c = 0; c < Board.SIZE; c++) {
                    cellButtons[r][c].reset();
                }
            }
            timerLabel.setText(controller.getTurnTimeoutSeconds() + "s");
            timerProgressBar.setMaximum(controller.getTurnTimeoutSeconds());
            timerProgressBar.setValue(controller.getTurnTimeoutSeconds());
            timerProgressBar.setForeground(UITheme.COLOR_WIN);
        });
    }

    @Override
    public void onScoreboardUpdated(Scoreboard scoreboard) {
        SwingUtilities.invokeLater(() -> updateScoreboardDisplay(scoreboard));
    }

    private void updateScoreboardDisplay(Scoreboard scoreboard) {
        if (scoreboard == null) return;
        xWinsLabel.setText(String.valueOf(scoreboard.getXWins()));
        oWinsLabel.setText(String.valueOf(scoreboard.getOWins()));
        drawsLabel.setText(String.valueOf(scoreboard.getDraws()));
        totalRoundsLabel.setText(String.valueOf(scoreboard.getTotalRounds()));
    }

    @Override
    public void onStatusMessage(String message) {
        SwingUtilities.invokeLater(() -> statusBannerLabel.setText(message));
    }

    @Override
    public void onError(String errorMessage) {
        SwingUtilities.invokeLater(() -> {
            statusBannerLabel.setText("Notice: " + errorMessage);
            Toolkit.getDefaultToolkit().beep();
        });
    }
}
