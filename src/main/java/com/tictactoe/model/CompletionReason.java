package com.tictactoe.model;

/**
 * Enumeration indicating how a round finished.
 */
public enum CompletionReason {
    NORMAL_WIN("Normal Win"),
    DRAW("Draw"),
    TIMEOUT("Turn Timeout");

    private final String displayName;

    CompletionReason(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
