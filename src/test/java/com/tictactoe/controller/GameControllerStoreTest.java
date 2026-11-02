package com.tictactoe.controller;

import com.tictactoe.model.CompletionReason;
import com.tictactoe.model.PlayerSymbol;
import com.tictactoe.persistence.SqlMatchStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GameController writes matches and rounds to the SQL store")
class GameControllerStoreTest {

    @TempDir
    Path tempDir;

    private SqlMatchStore store;
    private GameController controller;

    @BeforeEach
    void setUp() throws IOException {
        store = new SqlMatchStore(tempDir.resolve("game.db"));
        controller = new GameController(null, null, null, store);
    }

    @AfterEach
    void tearDown() throws IOException {
        controller.shutdown();
        store.close();
    }

    @Test
    @DisplayName("A won round is stored under the current match with the updated score")
    void wonRoundIsStored() throws IOException {
        controller.startNewMatch("Alice", "Bob", 10);
        // X wins the top row: X(0,0) O(1,0) X(0,1) O(1,1) X(0,2)
        controller.handleMove(0, 0);
        controller.handleMove(1, 0);
        controller.handleMove(0, 1);
        controller.handleMove(1, 1);
        assertTrue(controller.handleMove(0, 2));

        assertEquals(1, store.loadMatchSummaries().size());
        SqlMatchStore.MatchSummary match = store.loadMatchSummaries().get(0);
        assertEquals("Alice", match.playerXName());
        assertEquals("Bob", match.playerOName());
        assertEquals(1, match.xWins());
        assertEquals(0, match.oWins());

        assertEquals(1, store.loadAllRecords().size());
        assertEquals(PlayerSymbol.X, store.loadAllRecords().get(0).getWinnerSymbol());
        assertEquals(CompletionReason.NORMAL_WIN, store.loadAllRecords().get(0).getReason());
    }

    @Test
    @DisplayName("Each new match gets its own row; rounds within a match share it")
    void newMatchOpensNewRow() throws IOException {
        controller.startNewMatch("Alice", "Bob", 10);
        controller.handleMove(0, 0);
        controller.handleMove(1, 0);
        controller.handleMove(0, 1);
        controller.handleMove(1, 1);
        controller.handleMove(0, 2); // round 1 won by X

        controller.startNewRound();
        controller.handleMove(0, 0);
        controller.handleMove(1, 0);
        controller.handleMove(0, 1);
        controller.handleMove(1, 1);
        controller.handleMove(0, 2); // round 2 won by X in same match

        assertEquals(1, store.loadMatchSummaries().size());
        assertEquals(2, store.loadMatchSummaries().get(0).xWins());
        assertEquals(2, store.loadAllRecords().size());

        controller.startNewMatch("Alice", "Bob", 10);
        assertEquals(2, store.loadMatchSummaries().size());
    }
}
