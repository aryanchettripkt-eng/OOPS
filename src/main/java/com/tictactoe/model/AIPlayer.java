package com.tictactoe.model;

/**
 * Concrete implementation of an AI (Computer) player.
 * Demonstrates:
 * - Inheritance: Extends the abstract {@link Player} class
 * - Polymorphism: Overrides {@link #getPlayerType()} returning "AI"
 * - Intelligent Heuristic Decision Making: Evaluates board state to win or block
 */
public class AIPlayer extends Player {

    /**
     * Constructs an AI player with the specified name and symbol.
     *
     * @param name   display name (e.g., "Computer (AI)")
     * @param symbol player mark (X or O)
     */
    public AIPlayer(String name, PlayerSymbol symbol) {
        super(name, symbol);
    }

    @Override
    public String getPlayerType() {
        return "AI";
    }

    /**
     * Calculates the best move for this AI player on the given board.
     * Strategy:
     * 1. Immediate Win: Complete any row, column, or diagonal.
     * 2. Immediate Block: Prevent opponent from winning on their next turn.
     * 3. Center Control: Take the center cell (1, 1) if available.
     * 4. Corner Control: Take available corner cells.
     * 5. Open Cell Fallback: Take any remaining unoccupied cell.
     *
     * @param board current board state
     * @return int array [row, col] or null if board is full
     */
    public int[] calculateMove(Board board) {
        if (board == null || board.isFull()) {
            return null;
        }

        PlayerSymbol mySymbol = getSymbol();
        PlayerSymbol opponentSymbol = mySymbol.getOpponent();

        // 1. Win if possible
        int[] winningMove = findWinningMove(board, mySymbol);
        if (winningMove != null) {
            return winningMove;
        }

        // 2. Block opponent's winning move
        int[] blockingMove = findWinningMove(board, opponentSymbol);
        if (blockingMove != null) {
            return blockingMove;
        }

        // 3. Take center square (1, 1) if free
        if (board.isCellEmpty(1, 1)) {
            return new int[]{1, 1};
        }

        // 4. Take available corner squares
        int[][] corners = {{0, 0}, {0, 2}, {2, 0}, {2, 2}};
        for (int[] corner : corners) {
            if (board.isCellEmpty(corner[0], corner[1])) {
                return corner;
            }
        }

        // 5. Take available edge squares
        int[][] edges = {{0, 1}, {1, 0}, {1, 2}, {2, 1}};
        for (int[] edge : edges) {
            if (board.isCellEmpty(edge[0], edge[1])) {
                return edge;
            }
        }

        // 6. Fallback scan for any remaining empty square
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                if (board.isCellEmpty(r, c)) {
                    return new int[]{r, c};
                }
            }
        }

        return null;
    }

    /**
     * Scans for a single move that immediately completes a winning line for the given symbol.
     */
    private int[] findWinningMove(Board board, PlayerSymbol symbol) {
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                if (board.isCellEmpty(r, c)) {
                    if (isWinningCoord(board, r, c, symbol)) {
                        return new int[]{r, c};
                    }
                }
            }
        }
        return null;
    }

    /**
     * Evaluates if placing symbol at (r, c) would complete 3-in-a-row.
     */
    private boolean isWinningCoord(Board board, int r, int c, PlayerSymbol symbol) {
        // Check row r
        boolean rowWin = true;
        for (int col = 0; col < Board.SIZE; col++) {
            PlayerSymbol s = (col == c) ? symbol : board.getCell(r, col);
            if (s != symbol) {
                rowWin = false;
                break;
            }
        }
        if (rowWin) return true;

        // Check column c
        boolean colWin = true;
        for (int row = 0; row < Board.SIZE; row++) {
            PlayerSymbol s = (row == r) ? symbol : board.getCell(row, c);
            if (s != symbol) {
                colWin = false;
                break;
            }
        }
        if (colWin) return true;

        // Check main diagonal (top-left to bottom-right)
        if (r == c) {
            boolean diagWin = true;
            for (int i = 0; i < Board.SIZE; i++) {
                PlayerSymbol s = (i == r) ? symbol : board.getCell(i, i);
                if (s != symbol) {
                    diagWin = false;
                    break;
                }
            }
            if (diagWin) return true;
        }

        // Check anti-diagonal (top-right to bottom-left)
        if (r + c == Board.SIZE - 1) {
            boolean antiDiagWin = true;
            for (int i = 0; i < Board.SIZE; i++) {
                PlayerSymbol s = (i == r) ? symbol : board.getCell(i, Board.SIZE - 1 - i);
                if (s != symbol) {
                    antiDiagWin = false;
                    break;
                }
            }
            if (antiDiagWin) return true;
        }

        return false;
    }
}
