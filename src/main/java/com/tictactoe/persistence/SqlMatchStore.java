package com.tictactoe.persistence;

import com.tictactoe.model.CompletionReason;
import com.tictactoe.model.PlayerSymbol;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Embedded SQLite implementation of {@link MatchStore}.
 *
 * Schema:
 * - players  : one row per distinct player name
 * - matches  : one row per started match, with running scores (X wins, O wins, draws)
 * - rounds   : one row per completed round, including winner, reason (NORMAL_WIN / DRAW / TIMEOUT)
 *              and duration. A TIMEOUT round means the loser of that round ran out of time.
 * - app_meta : key/value flags, used to record that the legacy CSV import has run
 *
 * All access goes through one connection guarded by {@code synchronized}, so it is safe to call
 * from the controller (under its state lock) and from the history dialog on the EDT.
 */
public final class SqlMatchStore implements MatchStore, AutoCloseable {

    public static final String DEFAULT_DB_NAME = "tictactoe.db";
    static final String CSV_IMPORT_FLAG = "csv_imported";

    private static final String[] SCHEMA = {
            "PRAGMA foreign_keys = ON",
            """
            CREATE TABLE IF NOT EXISTS players (
                id   INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE
            )""",
            """
            CREATE TABLE IF NOT EXISTS matches (
                id                   INTEGER PRIMARY KEY AUTOINCREMENT,
                started_at           TEXT    NOT NULL,
                player_x_id          INTEGER NOT NULL REFERENCES players(id),
                player_o_id          INTEGER NOT NULL REFERENCES players(id),
                vs_ai                INTEGER NOT NULL DEFAULT 0,
                turn_timeout_seconds INTEGER NOT NULL,
                x_wins               INTEGER NOT NULL DEFAULT 0,
                o_wins               INTEGER NOT NULL DEFAULT 0,
                draws                INTEGER NOT NULL DEFAULT 0
            )""",
            """
            CREATE TABLE IF NOT EXISTS rounds (
                id               INTEGER PRIMARY KEY AUTOINCREMENT,
                match_id         INTEGER NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
                round_number     INTEGER NOT NULL,
                played_at        TEXT    NOT NULL,
                player_x_id      INTEGER NOT NULL REFERENCES players(id),
                player_o_id      INTEGER NOT NULL REFERENCES players(id),
                winner_id        INTEGER REFERENCES players(id),
                winner_symbol    TEXT    NOT NULL,
                reason           TEXT    NOT NULL,
                duration_seconds INTEGER NOT NULL,
                UNIQUE (match_id, round_number)
            )""",
            "CREATE INDEX IF NOT EXISTS idx_rounds_played_at ON rounds(played_at)",
            """
            CREATE TABLE IF NOT EXISTS app_meta (
                key   TEXT PRIMARY KEY,
                value TEXT NOT NULL
            )"""
    };

    /**
     * Summary of one match with its running scores.
     */
    public record MatchSummary(long id, LocalDateTime startedAt, String playerXName, String playerOName,
                               boolean vsAI, int turnTimeoutSeconds, int xWins, int oWins, int draws) {
    }

    private final Path dbPath;
    private final Connection connection;

    /**
     * Opens (creating if necessary) the SQLite database at the given path and ensures the schema exists.
     *
     * @param dbPath database file path
     * @throws IOException if the file cannot be opened or the schema cannot be created
     */
    public SqlMatchStore(Path dbPath) throws IOException {
        this.dbPath = (dbPath != null) ? dbPath : AppPaths.dataFile(DEFAULT_DB_NAME);
        Path parent = this.dbPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try {
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + this.dbPath.toAbsolutePath());
            try (Statement st = connection.createStatement()) {
                for (String sql : SCHEMA) {
                    st.execute(sql);
                }
            }
        } catch (SQLException e) {
            throw new IOException("Could not open match database at " + this.dbPath + ": " + e.getMessage(), e);
        }
    }

    public Path getDbPath() {
        return dbPath;
    }

    @Override
    public synchronized long startMatch(LocalDateTime startedAt, String playerXName, String playerOName,
                                        boolean vsAI, int turnTimeoutSeconds) throws IOException {
        if (startedAt == null) {
            throw new IllegalArgumentException("startedAt cannot be null");
        }
        try {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                long xId = upsertPlayer(playerXName);
                long oId = upsertPlayer(playerOName);
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO matches (started_at, player_x_id, player_o_id, vs_ai, turn_timeout_seconds) "
                                + "VALUES (?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, startedAt.format(MatchRecord.FORMATTER));
                    ps.setLong(2, xId);
                    ps.setLong(3, oId);
                    ps.setInt(4, vsAI ? 1 : 0);
                    ps.setInt(5, turnTimeoutSeconds);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        long matchId = keys.getLong(1);
                        connection.commit();
                        return matchId;
                    }
                }
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException e) {
            throw new IOException("Failed to start match record: " + e.getMessage(), e);
        }
    }

    @Override
    public synchronized void recordRound(long matchId, MatchRecord round) throws IOException {
        if (round == null) {
            throw new IllegalArgumentException("Round cannot be null");
        }
        try {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                insertRound(matchId, round);
                bumpScore(matchId, round.getWinnerSymbol());
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException e) {
            throw new IOException("Failed to save round: " + e.getMessage(), e);
        }
    }

    @Override
    public synchronized List<MatchRecord> loadAllRecords() throws IOException {
        String sql = """
                SELECT r.played_at, px.name AS x_name, po.name AS o_name, w.name AS winner_name,
                       r.winner_symbol, r.reason, r.duration_seconds
                FROM rounds r
                JOIN players px ON px.id = r.player_x_id
                JOIN players po ON po.id = r.player_o_id
                LEFT JOIN players w ON w.id = r.winner_id
                ORDER BY r.played_at, r.id
                """;
        List<MatchRecord> records = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                records.add(new MatchRecord(
                        LocalDateTime.parse(rs.getString("played_at"), MatchRecord.FORMATTER),
                        rs.getString("x_name"),
                        rs.getString("o_name"),
                        rs.getString("winner_name"),
                        PlayerSymbol.valueOf(rs.getString("winner_symbol")),
                        CompletionReason.valueOf(rs.getString("reason")),
                        rs.getLong("duration_seconds")));
            }
        } catch (SQLException e) {
            throw new IOException("Failed to load match history: " + e.getMessage(), e);
        }
        return Collections.unmodifiableList(records);
    }

    /**
     * Returns every match with its running scores, newest first.
     */
    public synchronized List<MatchSummary> loadMatchSummaries() throws IOException {
        String sql = """
                SELECT m.id, m.started_at, px.name AS x_name, po.name AS o_name, m.vs_ai,
                       m.turn_timeout_seconds, m.x_wins, m.o_wins, m.draws
                FROM matches m
                JOIN players px ON px.id = m.player_x_id
                JOIN players po ON po.id = m.player_o_id
                ORDER BY m.started_at DESC, m.id DESC
                """;
        List<MatchSummary> matches = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                matches.add(new MatchSummary(
                        rs.getLong("id"),
                        LocalDateTime.parse(rs.getString("started_at"), MatchRecord.FORMATTER),
                        rs.getString("x_name"),
                        rs.getString("o_name"),
                        rs.getInt("vs_ai") == 1,
                        rs.getInt("turn_timeout_seconds"),
                        rs.getInt("x_wins"),
                        rs.getInt("o_wins"),
                        rs.getInt("draws")));
            }
        } catch (SQLException e) {
            throw new IOException("Failed to load matches: " + e.getMessage(), e);
        }
        return Collections.unmodifiableList(matches);
    }

    /**
     * Imports rows from the legacy CSV history, once per database.
     * Each CSV row becomes its own match with a single round, so scores and reasons are preserved.
     * Turn timeout is unknown for legacy rows and stored as 0.
     *
     * @param csv legacy CSV store
     * @return number of rounds imported (0 if the import already ran or the CSV is empty)
     * @throws IOException if reading the CSV or writing the database fails
     */
    public synchronized int importLegacyCsv(MatchHistory csv) throws IOException {
        if (csv == null || isFlagSet(CSV_IMPORT_FLAG)) {
            return 0;
        }
        List<MatchRecord> legacy = csv.loadAllRecords();
        try {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                for (MatchRecord record : legacy) {
                    boolean vsAI = record.getPlayerOName().endsWith("(AI)");
                    long matchId = insertMatch(record.getTimestamp(), record.getPlayerXName(),
                            record.getPlayerOName(), vsAI, 0);
                    insertRound(matchId, record);
                    bumpScore(matchId, record.getWinnerSymbol());
                }
                setFlag(CSV_IMPORT_FLAG, "1");
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException e) {
            throw new IOException("Failed to import legacy CSV history: " + e.getMessage(), e);
        }
        return legacy.size();
    }

    @Override
    public synchronized void clearHistory() throws IOException {
        try {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (Statement st = connection.createStatement()) {
                st.executeUpdate("DELETE FROM rounds");
                st.executeUpdate("DELETE FROM matches");
                st.executeUpdate("DELETE FROM players");
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException e) {
            throw new IOException("Failed to clear match history: " + e.getMessage(), e);
        }
    }

    @Override
    public synchronized void close() throws IOException {
        try {
            if (!connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            throw new IOException("Failed to close match database: " + e.getMessage(), e);
        }
    }

    // ---- helpers; all called while holding this object's monitor and inside a transaction ----

    private long upsertPlayer(String name) throws SQLException {
        String trimmed = name.trim();
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO players (name) VALUES (?) ON CONFLICT(name) DO NOTHING")) {
            ps.setString(1, trimmed);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = connection.prepareStatement("SELECT id FROM players WHERE name = ?")) {
            ps.setString(1, trimmed);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private long insertMatch(LocalDateTime startedAt, String xName, String oName,
                             boolean vsAI, int timeoutSeconds) throws SQLException {
        long xId = upsertPlayer(xName);
        long oId = upsertPlayer(oName);
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO matches (started_at, player_x_id, player_o_id, vs_ai, turn_timeout_seconds) "
                        + "VALUES (?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, startedAt.format(MatchRecord.FORMATTER));
            ps.setLong(2, xId);
            ps.setLong(3, oId);
            ps.setInt(4, vsAI ? 1 : 0);
            ps.setInt(5, timeoutSeconds);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private void insertRound(long matchId, MatchRecord round) throws SQLException {
        long xId = upsertPlayer(round.getPlayerXName());
        long oId = upsertPlayer(round.getPlayerOName());
        boolean hasWinner = round.getWinnerSymbol() != PlayerSymbol.EMPTY;
        Long winnerId = hasWinner
                ? upsertPlayer(round.getWinnerName())
                : null;

        int nextRoundNumber;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COALESCE(MAX(round_number), 0) + 1 FROM rounds WHERE match_id = ?")) {
            ps.setLong(1, matchId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                nextRoundNumber = rs.getInt(1);
            }
        }

        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO rounds (match_id, round_number, played_at, player_x_id, player_o_id, winner_id, "
                        + "winner_symbol, reason, duration_seconds) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setLong(1, matchId);
            ps.setInt(2, nextRoundNumber);
            ps.setString(3, round.getFormattedTimestamp());
            ps.setLong(4, xId);
            ps.setLong(5, oId);
            if (winnerId == null) {
                ps.setNull(6, java.sql.Types.INTEGER);
            } else {
                ps.setLong(6, winnerId);
            }
            ps.setString(7, round.getWinnerSymbol().name());
            ps.setString(8, round.getReason().name());
            ps.setLong(9, round.getDurationSeconds());
            ps.executeUpdate();
        }
    }

    private void bumpScore(long matchId, PlayerSymbol winnerSymbol) throws SQLException {
        String column = switch (winnerSymbol) {
            case X -> "x_wins";
            case O -> "o_wins";
            default -> "draws";
        };
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE matches SET " + column + " = " + column + " + 1 WHERE id = ?")) {
            ps.setLong(1, matchId);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("No match with id " + matchId);
            }
        }
    }

    private boolean isFlagSet(String key) throws IOException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT value FROM app_meta WHERE key = ?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IOException("Failed to read app metadata: " + e.getMessage(), e);
        }
    }

    private void setFlag(String key, String value) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO app_meta (key, value) VALUES (?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value")) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        }
    }
}
