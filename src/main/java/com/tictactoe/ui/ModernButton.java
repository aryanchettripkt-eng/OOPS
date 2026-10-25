package com.tictactoe.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Modern custom-painted button with smooth hover effects, rounded corners,
 * and high-contrast typography.
 */
public class ModernButton extends JButton {

    private final Color baseColor;
    private final Color hoverColor;
    private final Color pressedColor;
    private boolean isHovered = false;
    private boolean isPressed = false;

    public ModernButton(String text, Color baseColor) {
        this(text, baseColor, Color.WHITE);
    }

    public ModernButton(String text, Color baseColor, Color textColor) {
        super(text);
        this.baseColor = baseColor;
        this.hoverColor = brighten(baseColor, 0.15f);
        this.pressedColor = baseColor.darker();

        setFont(UITheme.FONT_BODY_BOLD);
        setForeground(textColor);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    isHovered = true;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled()) {
                    isPressed = true;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                repaint();
            }
        });
    }

    private Color brighten(Color c, float factor) {
        int r = Math.min(255, (int) (c.getRed() + (255 - c.getRed()) * factor));
        int g = Math.min(255, (int) (c.getGreen() + (255 - c.getGreen()) * factor));
        int b = Math.min(255, (int) (c.getBlue() + (255 - c.getBlue()) * factor));
        return new Color(r, g, b);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        Color paintBg;
        if (!isEnabled()) {
            paintBg = new Color(0x94, 0xA3, 0xB8);
        } else if (isPressed) {
            paintBg = pressedColor;
        } else if (isHovered) {
            paintBg = hoverColor;
        } else {
            paintBg = baseColor;
        }

        int width = getWidth();
        int height = getHeight();
        int arc = 10;

        g2.setColor(paintBg);
        g2.fillRoundRect(0, 0, width, height, arc, arc);

        // Center text
        g2.setFont(getFont());
        g2.setColor(getForeground());
        FontMetrics fm = g2.getFontMetrics();
        int textX = (width - fm.stringWidth(getText())) / 2;
        int textY = (height - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(getText(), textX, textY);

        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        return new Dimension(size.width + 24, Math.max(38, size.height + 12));
    }
}
