package com.tictactoe.model;

import java.util.Objects;

/**
 * Abstract base class representing a player in the Tic-Tac-Toe game.
 * Demonstrates abstraction, encapsulation with protected constructors,
 * and method overriding.
 */
public abstract class Player {
    private final String name;
    private final PlayerSymbol symbol;

    /**
     * Protected constructor to be invoked by concrete subclasses.
     *
     * @param name   the player's display name (must not be null or blank)
     * @param symbol the symbol assigned to this player (X or O)
     */
    protected Player(String name, PlayerSymbol symbol) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Player name cannot be null or blank.");
        }
        if (symbol == null || symbol == PlayerSymbol.EMPTY) {
            throw new IllegalArgumentException("Player symbol must be X or O.");
        }
        this.name = name.trim();
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public PlayerSymbol getSymbol() {
        return symbol;
    }

    /**
     * Abstract method returning the player type descriptor.
     * Concrete subclasses must implement this.
     *
     * @return description of the player type (e.g. "Human")
     */
    public abstract String getPlayerType();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return Objects.equals(name, player.name) && symbol == player.symbol;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, symbol);
    }

    @Override
    public String toString() {
        return String.format("%s (%s, %s)", name, symbol, getPlayerType());
    }
}
