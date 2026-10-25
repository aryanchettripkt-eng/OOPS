package com.tictactoe.ui;

import java.awt.*;

/**
 * Modern, high-contrast design system for the Multithreaded Tic-Tac-Toe application.
 * Uses a crisp, clean layout with maximum contrast ratios for effortless readability.
 */
public final class UITheme {

    private UITheme() {}

    // Canvas & Surface Colors
    public static final Color BG_CANVAS = new Color(0xF1, 0xF5, 0xF9);     // Slate 100 (Clean, bright backdrop)
    public static final Color BG_SURFACE = new Color(0xFF, 0xFF, 0xFF);    // Pure White (Cards & Header)
    public static final Color BG_CARD = new Color(0xF8, 0xFA, 0xFC);       // Slate 50 (Inner card container)
    public static final Color BORDER_LIGHT = new Color(0xCB, 0xD5, 0xE1);  // Slate 300 (Subtle borders)
    public static final Color BORDER_FOCUS = new Color(0x63, 0x66, 0xF1);  // Indigo (Active input focus)

    // Cell States
    public static final Color CELL_BG_DEFAULT = new Color(0xFF, 0xFF, 0xFF);
    public static final Color CELL_BG_HOVER = new Color(0xEE, 0xF2, 0xF6);
    public static final Color CELL_BG_X = new Color(0xEF, 0xF6, 0xFF);     // Soft Blue
    public static final Color CELL_BG_O = new Color(0xFE, 0xF2, 0xF2);     // Soft Red
    public static final Color CELL_BG_WIN = new Color(0xD1, 0xFA, 0xE5);   // Emerald Mint

    public static final Color CELL_BORDER_DEFAULT = new Color(0x94, 0xA3, 0xB8); // Slate 400
    public static final Color CELL_BORDER_X = new Color(0x60, 0xA5, 0xFA);       // Blue 400
    public static final Color CELL_BORDER_O = new Color(0xF8, 0x71, 0x71);       // Red 400
    public static final Color CELL_BORDER_WIN = new Color(0x05, 0x96, 0x69);     // Emerald 600

    // Text Colors (High Contrast)
    public static final Color TEXT_PRIMARY = new Color(0x0F, 0x17, 0x2A);   // Slate 900 (Deep, ultra-sharp)
    public static final Color TEXT_SECONDARY = new Color(0x33, 0x41, 0x55); // Slate 700 (High contrast label)
    public static final Color TEXT_MUTED = new Color(0x64, 0x74, 0x8B);     // Slate 500
    public static final Color TEXT_LIGHT = new Color(0xFF, 0xFF, 0xFF);     // White for colored buttons

    // Player Brand Colors
    public static final Color COLOR_X = new Color(0x1D, 0x4E, 0xD8);        // Rich Royal Blue
    public static final Color COLOR_O = new Color(0xDC, 0x26, 0x26);        // Rich Crimson Red
    public static final Color COLOR_WIN = new Color(0x04, 0x78, 0x57);      // Deep Emerald
    public static final Color COLOR_WARNING = new Color(0xD9, 0x77, 0x06);  // Deep Amber
    public static final Color COLOR_PRIMARY_BTN = new Color(0x4F, 0x46, 0xE5);// Indigo Accent
    public static final Color COLOR_SECONDARY_BTN = new Color(0x33, 0x41, 0x55); // Dark Slate

    // Typography Hierarchy
    public static final Font FONT_APP_TITLE = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_SECTION = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_INPUT = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_TIMER = new Font("Segoe UI", Font.BOLD, 28);
    public static final Font FONT_SCORE_LABEL = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_SCORE_VALUE = new Font("Segoe UI", Font.BOLD, 26);
    public static final Font FONT_CELL_SYMBOL = new Font("Segoe UI", Font.BOLD, 60);
}
