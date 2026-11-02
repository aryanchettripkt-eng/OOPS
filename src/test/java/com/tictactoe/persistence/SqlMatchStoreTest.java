package com.tictactoe.persistence;

import com.tictactoe.model.CompletionReason;
import com.tictactoe.model.PlayerSymbol;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SqlMatchStore SQLite Persistence Tests")
class SqlMatchStoreTest {

    @TempDir
    Path tempDir;

    private SqlMatchStore store;

    @BeforeEach
    void setUp() throws IOException {
        store = new SqlMatchStore(tempDir.resolve("nested").resolve("test.db"));
    }

    @AfterEach
    void tearDown() throws IOException {
        store.close();
    }

    private static MatchRecord round(LocalDateTime at, String x, String o, String winner, PlayerSymbol sym,
                                     CompletionReason reason, long seconds) {
        return new MatchRecord(at, x, o, winner, sym, reason, seconds);
    }

    @Test
    @DisplayName("Creates the database file and parent folders on open")
    void createsDatabaseFile() {
        assertTrue(Files.exists(store.getDbPath()));
    }

    @Test
    @DisplayName("Round written under a match is read back with all fields intact")
    void roundRoundTrip() throws IOException {
        long matchId = store.startMatch(LocalDateTime.of(2026, 11, 2, 9, 0, 0), "Alice", "Bob", false, 10);
        store.recordRound(matchId, round(LocalDateTime.of(2026, 11, 2, 9, 1, 5), "Alice", "Bob", "Alice",
                PlayerSymbol.X, CompletionReason.NORMAL_WIN, 42));

        List<MatchRecord> loaded = store.loadAllRecords();
        assertEquals(1, loaded.size());
        MatchRecord r = loaded.get(0);
        assertEquals("2026-11-02 09:01:05", r.getFormattedTimestamp());
        assertEquals("Alice", r.getPlayerXName());
        assertEquals("Bob", r.getPlayerOName());
        assertEquals("Alice", r.getWinnerName());
        assertEquals(PlayerSymbol.X, r.getWinnerSymbol());
        assertEquals(CompletionReason.NORMAL_WIN, r.getReason());
        assertEquals(42, r.getDurationSeconds());
    }

    @Test
    @DisplayName("Draws store no winner and load back as Draw")
    void drawRoundTrip() throws IOException {
        long matchId = store.startMatch(LocalDateTime.now(), "Alice", "Bob", false, 10);
        store.recordRound(matchId, round(LocalDateTime.now(), "Alice", "Bob", null, PlayerSymbol.EMPTY,
                CompletionReason.DRAW, 30));

        MatchRecord r = store.loadAllRecords().get(0);
        assertEquals("Draw", r.getWinnerName());
        assertEquals(PlayerSymbol.EMPTY, r.getWinnerSymbol());
        assertEquals(CompletionReason.DRAW, r.getReason());
    }

    @Test
    @DisplayName("Running scores on the match reflect X wins, O wins and draws")
    void scoresAccumulatePerMatch() throws IOException {
        long matchId = store.startMatch(LocalDateTime.now(), "Alice", "Bob", false, 10);
        store.recordRound(matchId, round(LocalDateTime.now(), "Alice", "Bob", "Alice", PlayerSymbol.X,
                CompletionReason.NORMAL_WIN, 10));
        store.recordRound(matchId, round(LocalDateTime.now(), "Alice", "Bob", "Bob", PlayerSymbol.O,
                CompletionReason.TIMEOUT, 10));
        store.recordRound(matchId, round(LocalDateTime.now(), "Alice", "Bob", "Draw", PlayerSymbol.EMPTY,
                CompletionReason.DRAW, 10));
        store.recordRound(matchId, round(LocalDateTime.now(), "Alice", "Bob", "Alice", PlayerSymbol.X,
                CompletionReason.NORMAL_WIN, 10));

        List<SqlMatchStore.MatchSummary> matches = store.loadMatchSummaries();
        assertEquals(1, matches.size());
        SqlMatchStore.MatchSummary m = matches.get(0);
        assertEquals(matchId, m.id());
        assertEquals(2, m.xWins());
        assertEquals(1, m.oWins());
        assertEquals(1, m.draws());
        assertEquals(10, m.turnTimeoutSeconds());
        assertFalse(m.vsAI());
    }

    @Test
    @DisplayName("Timeouts are stored with their reason so they can be queried")
    void timeoutsStoredWithReason() throws IOException {
        long matchId = store.startMatch(LocalDateTime.now(), "Alice", "Bob", false, 5);
        store.recordRound(matchId, round(LocalDateTime.now(), "Alice", "Bob", "Bob", PlayerSymbol.O,
                CompletionReason.TIMEOUT, 5));

        MatchRecord r = store.loadAllRecords().get(0);
        assertEquals(CompletionReason.TIMEOUT, r.getReason());
        assertEquals("Bob", r.getWinnerName());
    }

    @Test
    @DisplayName("Players are stored once and shared across matches")
    void playersReusedAcrossMatches() throws IOException {
        long m1 = store.startMatch(LocalDateTime.now(), "Alice", "Computer (AI)", true, 10);
        long m2 = store.startMatch(LocalDateTime.now(), "Alice", "Bob", false, 15);
        store.recordRound(m1, round(LocalDateTime.now(), "Alice", "Computer (AI)", "Alice", PlayerSymbol.X,
                CompletionReason.NORMAL_WIN, 3));
        store.recordRound(m2, round(LocalDateTime.now(), "Alice", "Bob", "Bob", PlayerSymbol.O,
                CompletionReason.NORMAL_WIN, 4));

        assertEquals(2, store.loadMatchSummaries().size());
        assertTrue(store.loadMatchSummaries().stream().anyMatch(SqlMatchStore.MatchSummary::vsAI));
        assertEquals(2, store.loadAllRecords().size());
    }

    @Test
    @DisplayName("Recording a round for an unknown match is rejected and stores nothing")
    void unknownMatchRejected() throws IOException {
        assertThrows(IOException.class, () -> store.recordRound(9999L,
                round(LocalDateTime.now(), "Alice", "Bob", "Alice", PlayerSymbol.X,
                        CompletionReason.NORMAL_WIN, 1)));
        assertTrue(store.loadAllRecords().isEmpty());
    }

    @Test
    @DisplayName("Clear history removes all rounds and matches, and reopened database is empty")
    void clearHistoryRemovesEverything() throws IOException {
        long matchId = store.startMatch(LocalDateTime.now(), "Alice", "Bob", false, 10);
        store.recordRound(matchId, round(LocalDateTime.now(), "Alice", "Bob", "Alice", PlayerSymbol.X,
                CompletionReason.NORMAL_WIN, 9));

        store.clearHistory();
        assertTrue(store.loadAllRecords().isEmpty());
        assertTrue(store.loadMatchSummaries().isEmpty());
    }

    @Test
    @DisplayName("Legacy CSV import runs once and is not repeated after history is cleared")
    void legacyCsvImportIsOneShot() throws IOException {
        Path csvPath = tempDir.resolve("legacy.csv");
        Files.writeString(csvPath, MatchHistory.CSV_HEADER + "\n"
                + "2026-10-25 14:15:30,Alice,Bob,Alice,X,NORMAL_WIN,18\n"
                + "2026-10-25 14:20:12,Alice,Computer (AI),Computer (AI),O,TIMEOUT,10\n"
                + "2026-10-25 14:25:44,Charlie,Diana,Draw,EMPTY,DRAW,35\n");
        MatchHistory csv = new MatchHistory(csvPath);

        assertEquals(3, store.importLegacyCsv(csv));
        assertEquals(3, store.loadAllRecords().size());
        assertEquals(3, store.loadMatchSummaries().size());
        assertTrue(store.loadMatchSummaries().stream().anyMatch(SqlMatchStore.MatchSummary::vsAI));

        store.clearHistory();
        assertEquals(0, store.importLegacyCsv(csv), "second import must not re-add cleared CSV rows");
        assertTrue(store.loadAllRecords().isEmpty());
    }

    @Test
    @DisplayName("Data survives closing and reopening the database")
    void dataPersistsAcrossReopen() throws IOException {
        Path db = store.getDbPath();
        long matchId = store.startMatch(LocalDateTime.now(), "Alice", "Bob", false, 10);
        store.recordRound(matchId, round(LocalDateTime.now(), "Alice", "Bob", "Alice", PlayerSymbol.X,
                CompletionReason.NORMAL_WIN, 7));
        store.close();

        try (SqlMatchStore reopened = new SqlMatchStore(db)) {
            assertEquals(1, reopened.loadAllRecords().size());
            assertEquals(1, reopened.loadMatchSummaries().get(0).xWins());
        }
        store = new SqlMatchStore(db); // re-open for tearDown
    }
}
