package com.tictactoe.model;

/**
 * Represents a symbol placed on the Tic-Tac-Toe board.
 * Demonstrates Java enums with custom attributes and methods.
 */
public enum PlayerSymbol {
    X('X'),
    O('O'),
    EMPTY(' ');

    private final char symbolChar;

    PlayerSymbol(char symbolChar) {
        this.symbolChar = symbolChar;
    }

    public char getChar() {
        return symbolChar;
    }

    public PlayerSymbol getOpponent() {
        if (this == X) {
            return O;
        } else if (this == O) {
            return X;
        }
        return EMPTY;
    }

    @Override
    public String toString() {
        return String.valueOf(symbolChar);
    }
}
