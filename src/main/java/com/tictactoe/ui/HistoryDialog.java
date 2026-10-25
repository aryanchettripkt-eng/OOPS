package com.tictactoe.ui;

import com.tictactoe.persistence.MatchHistory;
import com.tictactoe.persistence.MatchRecord;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.io.IOException;
import java.util.List;

/**
 * Modal dialog displaying persistent match records loaded from CSV.
 * Features a modern JTable, refresh capability, and history clearing.
 */
public class HistoryDialog extends JDialog {

    private final MatchHistory matchHistory;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JLabel statusLabel;

    public HistoryDialog(Frame parent, MatchHistory matchHistory) {
        super(parent, "Match History", true);
        this.matchHistory = matchHistory;

        setSize(780, 480);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(UITheme.BG_DARK);
        setLayout(new BorderLayout(10, 10));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(UITheme.BG_SURFACE);
        headerPanel.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel titleLabel = new JLabel("Completed Match History");
        titleLabel.setFont(UITheme.FONT_TITLE);
        titleLabel.setForeground(UITheme.TEXT_PRIMARY);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        statusLabel = new JLabel("Records loaded from local CSV");
        statusLabel.setFont(UITheme.FONT_REGULAR);
        statusLabel.setForeground(UITheme.TEXT_SECONDARY);
        headerPanel.add(statusLabel, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Table
        String[] columnNames = {"Date & Time", "Player X", "Player O", "Winner", "Reason", "Duration"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Read-only table
            }
        };

        table = new JTable(tableModel);
        table.setFont(UITheme.FONT_REGULAR);
        table.setRowHeight(32);
        table.setBackground(UITheme.BG_SURFACE);
        table.setForeground(UITheme.TEXT_PRIMARY);
        table.setGridColor(UITheme.BG_CARD);
        table.setSelectionBackground(UITheme.COLOR_ACCENT);
        table.setSelectionForeground(Color.WHITE);
        table.setShowGrid(true);

        // Header styling
        JTableHeader header = table.getTableHeader();
        header.setFont(UITheme.FONT_HEADER);
        header.setBackground(UITheme.BG_CARD);
        header.setForeground(UITheme.TEXT_PRIMARY);
        header.setPreferredSize(new Dimension(100, 36));

        // Center align text
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(UITheme.BG_SURFACE);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BG_CARD));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(UITheme.BG_DARK);
        centerPanel.setBorder(new EmptyBorder(10, 20, 10, 20));
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // Bottom action buttons
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        bottomPanel.setBackground(UITheme.BG_DARK);
        bottomPanel.setBorder(new EmptyBorder(5, 20, 12, 20));

        JButton refreshBtn = createStyledButton("Refresh", UITheme.BG_CARD, UITheme.TEXT_PRIMARY);
        refreshBtn.addActionListener(e -> loadData());

        JButton clearBtn = createStyledButton("Clear History", new Color(0x99, 0x1B, 0x1B), Color.WHITE);
        clearBtn.addActionListener(e -> handleClearHistory());

        JButton closeBtn = createStyledButton("Close", UITheme.COLOR_ACCENT, Color.WHITE);
        closeBtn.addActionListener(e -> dispose());

        bottomPanel.add(refreshBtn);
        bottomPanel.add(clearBtn);
        bottomPanel.add(closeBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        loadData();
    }

    private void loadData() {
        tableModel.setRowCount(0);
        if (matchHistory == null) {
            statusLabel.setText("No history persistence configured");
            return;
        }

        try {
            List<MatchRecord> records = matchHistory.loadAllRecords();
            if (records.isEmpty()) {
                statusLabel.setText("No match records found.");
            } else {
                statusLabel.setText(records.size() + " matches recorded.");
                // Show newest records first
                for (int i = records.size() - 1; i >= 0; i--) {
                    MatchRecord r = records.get(i);
                    tableModel.addRow(new Object[]{
                            r.getFormattedTimestamp(),
                            r.getPlayerXName(),
                            r.getPlayerOName(),
                            r.getWinnerName(),
                            r.getReason().getDisplayName(),
                            r.getFormattedDuration()
                    });
                }
            }
        } catch (IOException e) {
            statusLabel.setText("Error loading records: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Failed to load match history: " + e.getMessage(),
                    "I/O Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleClearHistory() {
        if (matchHistory == null) return;

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete all match history records?\nThis action cannot be undone.",
                "Confirm Clear History",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                matchHistory.clearHistory();
                loadData();
                JOptionPane.showMessageDialog(this,
                        "Match history cleared successfully.",
                        "History Cleared", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                        "Failed to clear history: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JButton createStyledButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(UITheme.FONT_BOLD);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg.darker(), 1),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
