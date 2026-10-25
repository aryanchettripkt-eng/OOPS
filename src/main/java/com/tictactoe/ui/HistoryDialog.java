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
 * Features a high-contrast JTable, refresh capability, and history clearing.
 */
public class HistoryDialog extends JDialog {

    private final MatchHistory matchHistory;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JLabel statusLabel;

    public HistoryDialog(Frame parent, MatchHistory matchHistory) {
        super(parent, "Match History Records", true);
        this.matchHistory = matchHistory;

        setSize(820, 500);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(UITheme.BG_CANVAS);
        setLayout(new BorderLayout(10, 10));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(UITheme.BG_SURFACE);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_LIGHT),
                new EmptyBorder(16, 24, 16, 24)
        ));

        JLabel titleLabel = new JLabel("Completed Match History");
        titleLabel.setFont(UITheme.FONT_APP_TITLE);
        titleLabel.setForeground(UITheme.TEXT_PRIMARY);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        statusLabel = new JLabel("Loaded from data/match_history.csv");
        statusLabel.setFont(UITheme.FONT_BODY_BOLD);
        statusLabel.setForeground(UITheme.TEXT_SECONDARY);
        headerPanel.add(statusLabel, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Table
        String[] columnNames = {"Date & Time", "Player X", "Player O", "Winner", "Reason", "Duration"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setFont(UITheme.FONT_BODY);
        table.setRowHeight(34);
        table.setBackground(UITheme.BG_SURFACE);
        table.setForeground(UITheme.TEXT_PRIMARY);
        table.setGridColor(UITheme.BORDER_LIGHT);
        table.setSelectionBackground(new Color(0xEE, 0xF2, 0xFF));
        table.setSelectionForeground(UITheme.TEXT_PRIMARY);
        table.setShowGrid(true);

        // Header styling
        JTableHeader header = table.getTableHeader();
        header.setFont(UITheme.FONT_BODY_BOLD);
        header.setBackground(UITheme.BG_CARD);
        header.setForeground(UITheme.TEXT_PRIMARY);
        header.setPreferredSize(new Dimension(100, 38));

        // Center align text with alternate row shading
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSel, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, isSel, hasFocus, row, col);
                setHorizontalAlignment(JLabel.CENTER);
                if (!isSel) {
                    c.setBackground(row % 2 == 0 ? UITheme.BG_SURFACE : UITheme.BG_CARD);
                    c.setForeground(UITheme.TEXT_PRIMARY);
                }
                return c;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(UITheme.BG_SURFACE);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT, 1));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(UITheme.BG_CANVAS);
        centerPanel.setBorder(new EmptyBorder(12, 24, 12, 24));
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // Bottom action buttons
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 12));
        bottomPanel.setBackground(UITheme.BG_SURFACE);
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_LIGHT),
                new EmptyBorder(10, 24, 12, 24)
        ));

        ModernButton refreshBtn = new ModernButton("Refresh", UITheme.COLOR_SECONDARY_BTN);
        refreshBtn.addActionListener(e -> loadData());

        ModernButton clearBtn = new ModernButton("Clear History", new Color(0xDC, 0x26, 0x26));
        clearBtn.addActionListener(e -> handleClearHistory());

        ModernButton closeBtn = new ModernButton("Close", UITheme.COLOR_PRIMARY_BTN);
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
}
