package com.tictactoe.ui;

import com.tictactoe.model.PlayerSymbol;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Custom-rendered grid cell button for the 3x3 Tic-Tac-Toe board.
 * Provides custom anti-aliased painting to ensure crystal-clear symbols,
 * consistent hover feedback, and highlight states across all operating systems.
 */
public class CellButton extends JButton {

    private PlayerSymbol symbol = PlayerSymbol.EMPTY;
    private boolean isHovered = false;
    private boolean isWinningCell = false;

    public CellButton() {
        super();
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled() && symbol == PlayerSymbol.EMPTY) {
                    isHovered = true;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }
        });
    }

    public void setSymbol(PlayerSymbol symbol) {
        this.symbol = (symbol != null) ? symbol : PlayerSymbol.EMPTY;
        repaint();
    }

    public PlayerSymbol getSymbol() {
        return symbol;
    }

    public void setWinningCell(boolean winningCell) {
        this.isWinningCell = winningCell;
        repaint();
    }

    public void reset() {
        this.symbol = PlayerSymbol.EMPTY;
        this.isHovered = false;
        this.isWinningCell = false;
        setEnabled(true);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int width = getWidth();
        int height = getHeight();
        int arc = 16;

        // 1. Determine background color
        Color bgColor;
        Color borderColor;
        int borderWidth = 2;

        if (isWinningCell) {
            bgColor = UITheme.CELL_BG_WIN;
            borderColor = UITheme.CELL_BORDER_WIN;
            borderWidth = 4;
        } else if (symbol == PlayerSymbol.X) {
            bgColor = UITheme.CELL_BG_X;
            borderColor = UITheme.CELL_BORDER_X;
        } else if (symbol == PlayerSymbol.O) {
            bgColor = UITheme.CELL_BG_O;
            borderColor = UITheme.CELL_BORDER_O;
        } else if (isHovered && isEnabled()) {
            bgColor = UITheme.CELL_BG_HOVER;
            borderColor = UITheme.BORDER_FOCUS;
        } else {
            bgColor = UITheme.CELL_BG_DEFAULT;
            borderColor = UITheme.CELL_BORDER_DEFAULT;
        }

        // Fill background
        g2.setColor(bgColor);
        g2.fillRoundRect(2, 2, width - 4, height - 4, arc, arc);

        // Draw border
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(borderWidth));
        g2.drawRoundRect(2, 2, width - 4, height - 4, arc, arc);

        // 2. Draw Symbol
        if (symbol != PlayerSymbol.EMPTY) {
            Color symbolColor;
            if (isWinningCell) {
                symbolColor = UITheme.COLOR_WIN;
            } else if (symbol == PlayerSymbol.X) {
                symbolColor = UITheme.COLOR_X;
            } else {
                symbolColor = UITheme.COLOR_O;
            }

            g2.setColor(symbolColor);
            g2.setFont(UITheme.FONT_CELL_SYMBOL);

            FontMetrics fm = g2.getFontMetrics();
            String text = String.valueOf(symbol.getChar());
            int textX = (width - fm.stringWidth(text)) / 2;
            int textY = (height - fm.getHeight()) / 2 + fm.getAscent();

            g2.drawString(text, textX, textY);
        }

        g2.dispose();
    }
}
