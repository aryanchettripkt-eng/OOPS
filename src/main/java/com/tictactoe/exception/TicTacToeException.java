package com.tictactoe.exception;

/**
 * Base checked exception for domain-specific errors in the Tic-Tac-Toe application.
 * Demonstrates custom exception hierarchy in Java.
 */
public class TicTacToeException extends Exception {
    public TicTacToeException(String message) {
        super(message);
    }

    public TicTacToeException(String message, Throwable cause) {
        super(message, cause);
    }
}
