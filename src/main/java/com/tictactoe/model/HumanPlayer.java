package com.tictactoe.model;

/**
 * Concrete implementation of a human player.
 * Demonstrates inheritance and method overriding of getPlayerType().
 */
public class HumanPlayer extends Player {

    /**
     * Constructs a human player with the specified name and symbol.
     *
     * @param name   the player's display name
     * @param symbol the symbol (X or O)
     */
    public HumanPlayer(String name, PlayerSymbol symbol) {
        super(name, symbol);
    }

    @Override
    public String getPlayerType() {
        return "Human";
    }
}
