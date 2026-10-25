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

@DisplayName("MatchHistory Persistence Unit Tests")
class MatchHistoryTest {

    @TempDir
    Path tempDir;

    private Path testCsvPath;
    private MatchHistory matchHistory;

    @BeforeEach
    void setUp() {
        testCsvPath = tempDir.resolve("test_history.csv");
        matchHistory = new MatchHistory(testCsvPath);
    }

    @AfterEach
    void tearDown() throws IOException {
        matchHistory.clearHistory();
    }

    @Test
    @DisplayName("Missing history file returns empty list gracefully")
    void testMissingHistoryFile() throws IOException {
        assertFalse(Files.exists(testCsvPath));
        List<MatchRecord> records = matchHistory.loadAllRecords();
        assertNotNull(records);
        assertTrue(records.isEmpty());
    }

    @Test
    @DisplayName("Write and read single match record correctly")
    void testWriteAndReadSingleRecord() throws IOException {
        LocalDateTime now = LocalDateTime.of(2026, 10, 25, 14, 30, 0);
        MatchRecord record = new MatchRecord(
                now,
                "Alice",
                "Bob",
                "Alice",
                PlayerSymbol.X,
                CompletionReason.NORMAL_WIN,
                42
        );

        matchHistory.appendRecord(record);
        assertTrue(Files.exists(testCsvPath));

        List<MatchRecord> loaded = matchHistory.loadAllRecords();
        assertEquals(1, loaded.size());

        MatchRecord retrieved = loaded.get(0);
        assertEquals("Alice", retrieved.getPlayerXName());
        assertEquals("Bob", retrieved.getPlayerOName());
        assertEquals("Alice", retrieved.getWinnerName());
        assertEquals(PlayerSymbol.X, retrieved.getWinnerSymbol());
        assertEquals(CompletionReason.NORMAL_WIN, retrieved.getReason());
        assertEquals(42, retrieved.getDurationSeconds());
        assertEquals("00:42", retrieved.getFormattedDuration());
        assertEquals("2026-10-25 14:30:00", retrieved.getFormattedTimestamp());
    }

    @Test
    @DisplayName("Append multiple match records without corrupting existing records")
    void testAppendMultipleRecords() throws IOException {
        LocalDateTime time1 = LocalDateTime.of(2026, 10, 25, 10, 0, 0);
        LocalDateTime time2 = LocalDateTime.of(2026, 10, 25, 10, 5, 0);

        MatchRecord record1 = new MatchRecord(time1, "Alice", "Bob", "Alice", PlayerSymbol.X, CompletionReason.NORMAL_WIN, 35);
        MatchRecord record2 = new MatchRecord(time2, "Alice", "Bob", "Draw", PlayerSymbol.EMPTY, CompletionReason.DRAW, 60);

        matchHistory.appendRecord(record1);
        matchHistory.appendRecord(record2);

        List<MatchRecord> loaded = matchHistory.loadAllRecords();
        assertEquals(2, loaded.size());
        assertEquals("Alice", loaded.get(0).getWinnerName());
        assertEquals("Draw", loaded.get(1).getWinnerName());
        assertEquals(CompletionReason.DRAW, loaded.get(1).getReason());
    }

    @Test
    @DisplayName("Handle player names containing commas, quotes, and whitespace safely")
    void testCsvEscapingForSpecialCharacters() throws IOException {
        LocalDateTime now = LocalDateTime.of(2026, 10, 25, 12, 0, 0);
        String complexNameX = "Smith, John \"The Great\"";
        String complexNameO = "O'Connor, Mary, Esq.";

        MatchRecord record = new MatchRecord(
                now,
                complexNameX,
                complexNameO,
                complexNameX,
                PlayerSymbol.X,
                CompletionReason.TIMEOUT,
                15
        );

        matchHistory.appendRecord(record);

        List<MatchRecord> loaded = matchHistory.loadAllRecords();
        assertEquals(1, loaded.size());

        MatchRecord retrieved = loaded.get(0);
        assertEquals(complexNameX, retrieved.getPlayerXName());
        assertEquals(complexNameO, retrieved.getPlayerOName());
        assertEquals(complexNameX, retrieved.getWinnerName());
        assertEquals(CompletionReason.TIMEOUT, retrieved.getReason());
    }

    @Test
    @DisplayName("Malformed CSV rows are skipped without throwing exceptions")
    void testMalformedRecordHandling() throws IOException {
        // Write header and one valid record, one corrupted record, and another valid record
        String content = MatchHistory.CSV_HEADER + "\n" +
                "2026-10-25 10:00:00,Alice,Bob,Alice,X,NORMAL_WIN,25\n" +
                "corrupted,data,only,four,tokens\n" +
                "2026-10-25 10:05:00,Charlie,Dave,Dave,O,TIMEOUT,12\n";

        Files.writeString(testCsvPath, content);

        List<MatchRecord> loaded = matchHistory.loadAllRecords();
        assertEquals(2, loaded.size(), "Should load 2 valid records and skip the malformed line");
        assertEquals("Alice", loaded.get(0).getPlayerXName());
        assertEquals("Charlie", loaded.get(1).getPlayerXName());
    }

    @Test
    @DisplayName("Clear history removes previous persistent data")
    void testClearHistory() throws IOException {
        MatchRecord record = new MatchRecord(
                LocalDateTime.now(), "Alice", "Bob", "Alice", PlayerSymbol.X, CompletionReason.NORMAL_WIN, 10
        );
        matchHistory.appendRecord(record);
        assertTrue(Files.exists(testCsvPath));

        matchHistory.clearHistory();
        assertFalse(Files.exists(testCsvPath));
        assertTrue(matchHistory.loadAllRecords().isEmpty());
    }
}
