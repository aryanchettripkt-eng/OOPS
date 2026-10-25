package com.tictactoe.persistence;

import com.tictactoe.model.CompletionReason;
import com.tictactoe.model.PlayerSymbol;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/**
 * Immutable value object representing a completed round of Tic-Tac-Toe.
 * Encapsulates match metadata including timestamp, players, outcome,
 * completion reason, and duration.
 */
public final class MatchRecord {
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final LocalDateTime timestamp;
    private final String playerXName;
    private final String playerOName;
    private final String winnerName;
    private final PlayerSymbol winnerSymbol;
    private final CompletionReason reason;
    private final long durationSeconds;

    public MatchRecord(LocalDateTime timestamp,
                       String playerXName,
                       String playerOName,
                       String winnerName,
                       PlayerSymbol winnerSymbol,
                       CompletionReason reason,
                       long durationSeconds) {
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        this.playerXName = validateName(playerXName, "Player X name");
        this.playerOName = validateName(playerOName, "Player O name");
        this.winnerName = (winnerName == null || winnerName.isBlank()) ? "Draw" : winnerName.trim();
        this.winnerSymbol = Objects.requireNonNull(winnerSymbol, "Winner symbol cannot be null");
        this.reason = Objects.requireNonNull(reason, "Completion reason cannot be null");
        this.durationSeconds = Math.max(0, durationSeconds);
    }

    private static String validateName(String name, String field) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be null or blank");
        }
        return name.trim();
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getFormattedTimestamp() {
        return timestamp.format(FORMATTER);
    }

    public String getPlayerXName() {
        return playerXName;
    }

    public String getPlayerOName() {
        return playerOName;
    }

    public String getWinnerName() {
        return winnerName;
    }

    public PlayerSymbol getWinnerSymbol() {
        return winnerSymbol;
    }

    public CompletionReason getReason() {
        return reason;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public String getFormattedDuration() {
        long minutes = durationSeconds / 60;
        long seconds = durationSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * Serializes this record into an RFC 4180 compliant CSV line.
     * Escapes fields containing commas, double quotes, or newlines.
     *
     * @return comma-separated record string
     */
    public String toCsvLine() {
        return String.join(",",
                escapeCsv(getFormattedTimestamp()),
                escapeCsv(playerXName),
                escapeCsv(playerOName),
                escapeCsv(winnerName),
                escapeCsv(winnerSymbol.name()),
                escapeCsv(reason.name()),
                String.valueOf(durationSeconds)
        );
    }

    /**
     * Parses a CSV line into a MatchRecord instance.
     *
     * @param line CSV line
     * @return MatchRecord parsed from line
     * @throws IllegalArgumentException if line is malformed or invalid
     */
    public static MatchRecord fromCsvLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            throw new IllegalArgumentException("CSV line cannot be null or empty");
        }

        String[] tokens = parseCsvLine(line);
        if (tokens.length < 7) {
            throw new IllegalArgumentException("Malformed CSV line, expected 7 columns but got " + tokens.length);
        }

        LocalDateTime parsedTimestamp;
        try {
            parsedTimestamp = LocalDateTime.parse(tokens[0], FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid timestamp format: " + tokens[0], e);
        }

        String xName = tokens[1];
        String oName = tokens[2];
        String winName = tokens[3];

        PlayerSymbol symbol;
        try {
            symbol = PlayerSymbol.valueOf(tokens[4].toUpperCase());
        } catch (IllegalArgumentException e) {
            symbol = PlayerSymbol.EMPTY;
        }

        CompletionReason parsedReason;
        try {
            parsedReason = CompletionReason.valueOf(tokens[5].toUpperCase());
        } catch (IllegalArgumentException e) {
            parsedReason = CompletionReason.NORMAL_WIN;
        }

        long duration;
        try {
            duration = Long.parseLong(tokens[6]);
        } catch (NumberFormatException e) {
            duration = 0;
        }

        return new MatchRecord(parsedTimestamp, xName, oName, winName, symbol, parsedReason, duration);
    }

    /**
     * RFC 4180 CSV escaping utility.
     */
    public static String escapeCsv(String input) {
        if (input == null) {
            return "";
        }
        if (input.contains(",") || input.contains("\"") || input.contains("\n") || input.contains("\r")) {
            return "\"" + input.replace("\"", "\"\"") + "\"";
        }
        return input;
    }

    /**
     * RFC 4180 CSV parser handling quoted strings with escaped quotes.
     */
    public static String[] parseCsvLine(String line) {
        java.util.List<String> result = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        int i = 0;

        while (i < line.length()) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++; // skip escaped quote
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    result.add(current.toString().trim());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
            i++;
        }
        result.add(current.toString().trim());
        return result.toArray(new String[0]);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MatchRecord that = (MatchRecord) o;
        return durationSeconds == that.durationSeconds &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(playerXName, that.playerXName) &&
                Objects.equals(playerOName, that.playerOName) &&
                Objects.equals(winnerName, that.winnerName) &&
                winnerSymbol == that.winnerSymbol &&
                reason == that.reason;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, playerXName, playerOName, winnerName, winnerSymbol, reason, durationSeconds);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (X) vs %s (O) -> Winner: %s (%s, %s)",
                getFormattedTimestamp(), playerXName, playerOName, winnerName, reason, getFormattedDuration());
    }
}
