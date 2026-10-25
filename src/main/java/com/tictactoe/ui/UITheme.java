package com.tictactoe.ui;

import java.awt.*;

/**
 * Design constants and cohesive color palette for the Swing user interface.
 * Implements a modern dark-slate theme with high-contrast accent colors.
 */
public final class UITheme {

    private UITheme() {}

    // Color Palette
    public static final Color BG_DARK = new Color(0x0F, 0x17, 0x2A);        // Slate 900
    public static final Color BG_SURFACE = new Color(0x1E, 0x29, 0x3B);     // Slate 800
    public static final Color BG_CARD = new Color(0x33, 0x41, 0x55);        // Slate 700
    public static final Color BG_CELL = new Color(0x1E, 0x29, 0x3B);        // Cell normal
    public static final Color BG_CELL_HOVER = new Color(0x27, 0x37, 0x4D);  // Cell hover

    public static final Color TEXT_PRIMARY = new Color(0xF8, 0xFA, 0xFC);   // Slate 50
    public static final Color TEXT_SECONDARY = new Color(0x94, 0xA3, 0xB8); // Slate 400
    public static final Color TEXT_MUTED = new Color(0x64, 0x74, 0x8B);     // Slate 500

    public static final Color COLOR_X = new Color(0x38, 0xBD, 0xF8);        // Sky Blue (X)
    public static final Color COLOR_O = new Color(0xF4, 0x3F, 0x5E);        // Rose Red (O)
    public static final Color COLOR_WIN = new Color(0x10, 0xB9, 0x81);      // Emerald Green
    public static final Color COLOR_WIN_BG = new Color(0x06, 0x4E, 0x3B);   // Deep Emerald
    public static final Color COLOR_WARNING = new Color(0xF5, 0x9E, 0x0B);  // Amber
    public static final Color COLOR_ACCENT = new Color(0x63, 0x66, 0xF1);   // Indigo Accent

    // Fonts
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_BOARD_CELL = new Font("Segoe UI", Font.BOLD, 52);
    public static final Font FONT_TIMER = new Font("Segoe UI", Font.BOLD, 24);
    public static final Font FONT_SCORE = new Font("Segoe UI", Font.BOLD, 18);
}
