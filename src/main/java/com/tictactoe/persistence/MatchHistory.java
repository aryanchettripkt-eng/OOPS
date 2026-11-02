package com.tictactoe.persistence;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service class managing persistent storage of completed match records in CSV format.
 * Demonstrates:
 * - Java NIO (java.nio.file.Path, Files)
 * - Explicit UTF-8 character encoding
 * - Safe append mode without corrupting existing records
 * - Resilient error handling for missing files, corrupt records, and I/O failures
 */
public class MatchHistory implements MatchStore {
    public static final String DEFAULT_FILE_NAME = "match_history.csv";
    public static final String CSV_HEADER = "Timestamp,PlayerX,PlayerO,Winner,WinnerSymbol,Reason,DurationSeconds";

    private final Path filePath;

    /**
     * Default constructor using {@code match_history.csv} in the project's data folder.
     */
    public MatchHistory() {
        this(AppPaths.dataFile(DEFAULT_FILE_NAME));
    }

    /**
     * Overloaded constructor accepting a custom file path.
     * Demonstrates method/constructor overloading and enables isolated unit tests.
     *
     * @param filePath custom path to CSV file
     */
    public MatchHistory(Path filePath) {
        this.filePath = (filePath != null) ? filePath : AppPaths.dataFile(DEFAULT_FILE_NAME);
    }

    public Path getFilePath() {
        return filePath;
    }

    /**
     * The CSV format has no match grouping, so this returns 0 and records nothing.
     */
    @Override
    public long startMatch(LocalDateTime startedAt, String playerXName, String playerOName,
                           boolean vsAI, int turnTimeoutSeconds) {
        return 0L;
    }

    /**
     * Appends the round as a CSV row. The match identifier is ignored by the CSV format.
     */
    @Override
    public void recordRound(long matchId, MatchRecord round) throws IOException {
        appendRecord(round);
    }

    /**
     * Appends a new match record to the CSV file.
     * If the file or parent directories do not exist, they are created automatically.
     * Writes the CSV header if creating a new file.
     *
     * @param record the completed match record to append
     * @throws IOException if a disk write failure occurs
     */
    public synchronized void appendRecord(MatchRecord record) throws IOException {
        if (record == null) {
            throw new IllegalArgumentException("Record cannot be null");
        }

        // Ensure parent directory exists
        Path parent = filePath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        boolean fileExists = Files.exists(filePath) && Files.size(filePath) > 0;

        try (BufferedWriter writer = Files.newBufferedWriter(
                filePath,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {

            // If new file, write header first
            if (!fileExists) {
                writer.write(CSV_HEADER);
                writer.newLine();
            }

            writer.write(record.toCsvLine());
            writer.newLine();
            writer.flush();
        }
    }

    /**
     * Loads all match records from the CSV file.
     * If the file does not exist, returns an empty list gracefully.
     * Malformed rows are skipped without throwing exceptions.
     *
     * @return unmodifiable list of loaded MatchRecord items, ordered oldest to newest
     * @throws IOException if a fatal read error occurs
     */
    @Override
    public synchronized List<MatchRecord> loadAllRecords() throws IOException {
        if (!Files.exists(filePath)) {
            return Collections.emptyList();
        }

        List<MatchRecord> records = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                // Skip header line if present
                if (isFirstLine) {
                    isFirstLine = false;
                    if (line.equalsIgnoreCase(CSV_HEADER) || line.startsWith("Timestamp,")) {
                        continue;
                    }
                }

                try {
                    MatchRecord record = MatchRecord.fromCsvLine(line);
                    records.add(record);
                } catch (IllegalArgumentException ex) {
                    // Log malformed row and continue reading remaining valid rows
                    System.err.println("Skipping malformed CSV line in " + filePath + ": " + ex.getMessage());
                }
            }
        }

        return Collections.unmodifiableList(records);
    }

    /**
     * Clears all match history records by deleting the file or truncating it.
     *
     * @throws IOException if file cannot be deleted
     */
    @Override
    public synchronized void clearHistory() throws IOException {
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }
    }
}
