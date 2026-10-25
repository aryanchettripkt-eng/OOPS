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
 * Implements GameEventListener to receive state updates from GameController.
 * Enforces that all Swing GUI updates are marshalled onto the Event Dispatch Thread (EDT)
 * using SwingUtilities.invokeLater.
 */
public class GameUI extends JFrame implements GameEventListener {

    private final GameController controller;

    // Board cell buttons
    private final JButton[][] cellButtons = new JButton[Board.SIZE][Board.SIZE];

    // Setup input fields
    private JTextField playerXField;
    private JTextField playerOField;
    private JSpinner timeoutSpinner;
    private JButton startMatchBtn;
    private JButton historyBtn;

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
    private JButton newRoundBtn;
    private JButton resetScoresBtn;

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
        setSize(850, 750);
        setMinimumSize(new Dimension(720, 680));
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.BG_DARK);
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
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.BG_SURFACE);
        panel.setBorder(new EmptyBorder(12, 20, 12, 20));

        JLabel appTitle = new JLabel("TIC-TAC-TOE : TIMED MULTITHREADED");
        appTitle.setFont(UITheme.FONT_TITLE);
        appTitle.setForeground(UITheme.TEXT_PRIMARY);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controls.setOpaque(false);

        // Player X input
        JLabel xTag = new JLabel("X:");
        xTag.setFont(UITheme.FONT_BOLD);
        xTag.setForeground(UITheme.COLOR_X);
        playerXField = new JTextField("Player X", 8);
        playerXField.setFont(UITheme.FONT_REGULAR);

        // Player O input
        JLabel oTag = new JLabel("O:");
        oTag.setFont(UITheme.FONT_BOLD);
        oTag.setForeground(UITheme.COLOR_O);
        playerOField = new JTextField("Player O", 8);
        playerOField.setFont(UITheme.FONT_REGULAR);

        // Timeout spinner
        JLabel timeoutTag = new JLabel("Timer(s):");
        timeoutTag.setFont(UITheme.FONT_BOLD);
        timeoutTag.setForeground(UITheme.TEXT_SECONDARY);
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(
                GameController.DEFAULT_TIMEOUT_SECONDS,
                GameController.MIN_TIMEOUT_SECONDS,
                GameController.MAX_TIMEOUT_SECONDS,
                1
        );
        timeoutSpinner = new JSpinner(spinnerModel);
        timeoutSpinner.setFont(UITheme.FONT_REGULAR);
        ((JSpinner.DefaultEditor) timeoutSpinner.getEditor()).getTextField().setColumns(3);

        // Action buttons
        startMatchBtn = createButton("Start Match", UITheme.COLOR_ACCENT, Color.WHITE);
        startMatchBtn.addActionListener(e -> handleStartMatch());

        historyBtn = createButton("Match History", UITheme.BG_CARD, UITheme.TEXT_PRIMARY);
        historyBtn.addActionListener(e -> showHistoryDialog());

        controls.add(xTag);
        controls.add(playerXField);
        controls.add(oTag);
        controls.add(playerOField);
        controls.add(timeoutTag);
        controls.add(timeoutSpinner);
        controls.add(startMatchBtn);
        controls.add(historyBtn);

        panel.add(appTitle, BorderLayout.WEST);
        panel.add(controls, BorderLayout.EAST);
        return panel;
    }

    private JPanel createCenterPanel() {
        JPanel centerContainer = new JPanel(new BorderLayout(10, 10));
        centerContainer.setBackground(UITheme.BG_DARK);
        centerContainer.setBorder(new EmptyBorder(16, 24, 16, 16));

        // Status & Countdown Banner
        JPanel bannerPanel = new JPanel(new BorderLayout(10, 8));
        bannerPanel.setBackground(UITheme.BG_SURFACE);
        bannerPanel.setBorder(new EmptyBorder(12, 16, 12, 16));

        currentPlayerLabel = new JLabel("Configure players above and click Start Match");
        currentPlayerLabel.setFont(UITheme.FONT_HEADER);
        currentPlayerLabel.setForeground(UITheme.TEXT_PRIMARY);

        JPanel timerBadge = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        timerBadge.setOpaque(false);

        JLabel timerIcon = new JLabel("Time:");
        timerIcon.setFont(UITheme.FONT_REGULAR);
        timerIcon.setForeground(UITheme.TEXT_SECONDARY);

        timerLabel = new JLabel("--s");
        timerLabel.setFont(UITheme.FONT_TIMER);
        timerLabel.setForeground(UITheme.COLOR_WIN);

        timerBadge.add(timerIcon);
        timerBadge.add(timerLabel);

        bannerPanel.add(currentPlayerLabel, BorderLayout.WEST);
        bannerPanel.add(timerBadge, BorderLayout.EAST);

        // Timer progress bar
        timerProgressBar = new JProgressBar(0, GameController.DEFAULT_TIMEOUT_SECONDS);
        timerProgressBar.setValue(GameController.DEFAULT_TIMEOUT_SECONDS);
        timerProgressBar.setForeground(UITheme.COLOR_WIN);
        timerProgressBar.setBackground(UITheme.BG_CARD);
        timerProgressBar.setBorderPainted(false);
        timerProgressBar.setPreferredSize(new Dimension(100, 4));
        bannerPanel.add(timerProgressBar, BorderLayout.SOUTH);

        centerContainer.add(bannerPanel, BorderLayout.NORTH);

        // 3x3 Grid Board Panel
        JPanel boardPanel = new JPanel(new GridLayout(Board.SIZE, Board.SIZE, 8, 8));
        boardPanel.setBackground(UITheme.BG_DARK);

        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                final int r = row;
                final int c = col;
                JButton cell = new JButton("");
                cell.setFont(UITheme.FONT_BOARD_CELL);
                cell.setFocusPainted(false);
                cell.setBackground(UITheme.BG_CELL);
                cell.setForeground(UITheme.TEXT_PRIMARY);
                cell.setBorder(BorderFactory.createLineBorder(UITheme.BG_CARD, 2));
                cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                cell.addActionListener(e -> {
                    // Send move to authoritative controller
                    controller.handleMove(r, c);
                });

                cellButtons[row][col] = cell;
                boardPanel.add(cell);
            }
        }

        centerContainer.add(boardPanel, BorderLayout.CENTER);

        // Status message strip
        statusBannerLabel = new JLabel("Welcome! Ready to play.");
        statusBannerLabel.setFont(UITheme.FONT_REGULAR);
        statusBannerLabel.setForeground(UITheme.TEXT_SECONDARY);
        statusBannerLabel.setBorder(new EmptyBorder(6, 4, 4, 4));
        centerContainer.add(statusBannerLabel, BorderLayout.SOUTH);

        return centerContainer;
    }

    private JPanel createSidePanel() {
        JPanel sidePanel = new JPanel(new BorderLayout());
        sidePanel.setBackground(UITheme.BG_DARK);
        sidePanel.setPreferredSize(new Dimension(220, 0));
        sidePanel.setBorder(new EmptyBorder(16, 0, 16, 20));

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(UITheme.BG_SURFACE);
        card.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("SCOREBOARD");
        title.setFont(UITheme.FONT_HEADER);
        title.setForeground(UITheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(16));

        xWinsLabel = createStatItem(card, "Player X Wins", "0", UITheme.COLOR_X);
        card.add(Box.createVerticalStrut(12));
        oWinsLabel = createStatItem(card, "Player O Wins", "0", UITheme.COLOR_O);
        card.add(Box.createVerticalStrut(12));
        drawsLabel = createStatItem(card, "Draws", "0", UITheme.TEXT_SECONDARY);
        card.add(Box.createVerticalStrut(12));
        totalRoundsLabel = createStatItem(card, "Total Rounds", "0", UITheme.COLOR_WIN);

        card.add(Box.createVerticalGlue());

        // Quick tips box
        JPanel tipsBox = new JPanel();
        tipsBox.setLayout(new BoxLayout(tipsBox, BoxLayout.Y_AXIS));
        tipsBox.setBackground(UITheme.BG_CARD);
        tipsBox.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel tipsTitle = new JLabel("Game Rules:");
        tipsTitle.setFont(UITheme.FONT_BOLD);
        tipsTitle.setForeground(UITheme.TEXT_PRIMARY);
        tipsBox.add(tipsTitle);
        tipsBox.add(Box.createVerticalStrut(6));

        JLabel rule1 = new JLabel("• X always moves first");
        rule1.setFont(UITheme.FONT_REGULAR);
        rule1.setForeground(UITheme.TEXT_SECONDARY);
        tipsBox.add(rule1);

        JLabel rule2 = new JLabel("• Beat the turn timer");
        rule2.setFont(UITheme.FONT_REGULAR);
        rule2.setForeground(UITheme.TEXT_SECONDARY);
        tipsBox.add(rule2);

        JLabel rule3 = new JLabel("• Timeout = forfeit round");
        rule3.setFont(UITheme.FONT_REGULAR);
        rule3.setForeground(UITheme.TEXT_SECONDARY);
        tipsBox.add(rule3);

        card.add(tipsBox);

        sidePanel.add(card, BorderLayout.CENTER);
        return sidePanel;
    }

    private JLabel createStatItem(JPanel container, String title, String value, Color color) {
        JLabel tLbl = new JLabel(title);
        tLbl.setFont(UITheme.FONT_REGULAR);
        tLbl.setForeground(UITheme.TEXT_SECONDARY);
        tLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel vLbl = new JLabel(value);
        vLbl.setFont(UITheme.FONT_SCORE);
        vLbl.setForeground(color);
        vLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        container.add(tLbl);
        container.add(Box.createVerticalStrut(2));
        container.add(vLbl);
        return vLbl;
    }

    private JPanel createBottomControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        panel.setBackground(UITheme.BG_SURFACE);
        panel.setBorder(new EmptyBorder(6, 20, 6, 20));

        newRoundBtn = createButton("New Round", UITheme.COLOR_ACCENT, Color.WHITE);
        newRoundBtn.addActionListener(e -> controller.startNewRound());

        resetScoresBtn = createButton("Reset Scores", UITheme.BG_CARD, UITheme.TEXT_PRIMARY);
        resetScoresBtn.addActionListener(e -> handleResetScores());

        JButton exitBtn = createButton("Exit", new Color(0x7F, 0x1D, 0x1D), Color.WHITE);
        exitBtn.addActionListener(e -> handleExit());

        panel.add(newRoundBtn);
        panel.add(resetScoresBtn);
        panel.add(exitBtn);

        return panel;
    }

    private JButton createButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(UITheme.FONT_BOLD);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg.darker(), 1),
                BorderFactory.createEmptyBorder(7, 14, 7, 14)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
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
                "Reset scoreboard to 0-0?\n(Current session scores will be cleared, persistent history remains intact)",
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
            // Enable or disable board buttons based on active play
            for (int r = 0; r < Board.SIZE; r++) {
                for (int c = 0; c < Board.SIZE; c++) {
                    cellButtons[r][c].setEnabled(isPlaying || !cellButtons[r][c].getText().isEmpty());
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
            JButton cell = cellButtons[row][col];
            cell.setText(String.valueOf(symbol.getChar()));
            cell.setForeground((symbol == PlayerSymbol.X) ? UITheme.COLOR_X : UITheme.COLOR_O);
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

            // Highlight winning line cells if present
            if (winningLine != null) {
                for (int[] coord : winningLine) {
                    JButton cell = cellButtons[coord[0]][coord[1]];
                    cell.setBackground(UITheme.COLOR_WIN_BG);
                    cell.setBorder(BorderFactory.createLineBorder(UITheme.COLOR_WIN, 3));
                }
            }

            String message;
            if (finalState == GameState.DRAW) {
                message = "Round ended in a DRAW!";
                currentPlayerLabel.setText(message);
                currentPlayerLabel.setForeground(UITheme.TEXT_SECONDARY);
            } else {
                String winDesc = (reason == CompletionReason.TIMEOUT) ? " [TIMEOUT FORFEIT]" : "";
                message = String.format("VICTORY! %s (%s) WON THE ROUND!%s",
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
                    JButton cell = cellButtons[r][c];
                    cell.setText("");
                    cell.setBackground(UITheme.BG_CELL);
                    cell.setBorder(BorderFactory.createLineBorder(UITheme.BG_CARD, 2));
                    cell.setEnabled(true);
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
