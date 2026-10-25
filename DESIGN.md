# Architecture and Design Specification

## Multithreaded Tic-Tac-Toe: A Two-Player Timed Game

---

## 1. Architectural Overview

The application follows a clean, layered Object-Oriented Architecture with strict separation of concerns between game domain rules, background concurrency/timers, local persistence, and presentation (Swing GUI).

```
+-------------------------------------------------------------+
|                         GameUI (Swing)                      |
|           (Main Frame, History Dialog, Action Handlers)      |
+-------------------------------------------------------------+
                               |
               implements GameEventListener
                               v
+-------------------------------------------------------------+
|                     GameController                          |
|  - Authoritative Lifecycle & Turn Sequencing                |
|  - Atomic Concurrency Synchronization (stateLock)           |
|  - Generation Token Invalidation (currentTurnToken)         |
+-------------------------------------------------------------+
      /              |                     |              \
     v               v                     v               v
+---------+   +--------------+     +---------------+   +--------------+
|  Board  |   |    Player    |     |   TurnTimer   |   | MatchHistory |
|  Model  |   | HumanPlayer  |     | (Background)  |   | (CSV Store)  |
+---------+   +--------------+     +---------------+   +--------------+
                     |
                     v
             +---------------+
             |  Scoreboard   |
             | (Atomic Stats)|
             +---------------+
```

### Architectural Principles Applied:
1. **Model-View-Controller (MVC) Separation**:
   - `Board`, `Player`, `Scoreboard`, and `GameState` have zero awareness of Swing.
   - `GameController` governs game rules and concurrency independent of any graphical framework.
   - `GameUI` is strictly an observer listening to `GameEventListener` callbacks and forwarding user gestures to controller methods.
2. **Defensive Programming & Immutability**:
   - `Board.getGridCopy()` and `Board.getCharArrayCopy()` return defensive deep copies to prevent external callers from corrupting internal board state.
   - `MatchRecord` is an immutable value object.
3. **Thread Confinement and Event Marshalling**:
   - Background threads manage countdown timers and I/O.
   - All Swing component mutations are marshalled to the Swing **Event Dispatch Thread (EDT)** using `SwingUtilities.invokeLater()`.

---

## 2. Package Structure and Class Responsibilities

| Package | Class / Interface | Responsibility |
| :--- | :--- | :--- |
| `com.tictactoe` | `Main` | Application entry point. Configures native Look & Feel, instantiates services, hooks JVM shutdown cleanup, and launches GUI on EDT. |
| `com.tictactoe.model` | `Board` | Encapsulates the 3×3 game grid (`PlayerSymbol[][]`), validates moves, detects wins across rows/columns/diagonals, computes winning lines, and checks draws. |
| `com.tictactoe.model` | `Player` | Abstract base class storing player name and assigned symbol. Defines abstract contract `getPlayerType()`. |
| `com.tictactoe.model` | `HumanPlayer` | Concrete subclass of `Player` representing a human participant. Overrides `getPlayerType()`. |
| `com.tictactoe.model` | `PlayerSymbol` | Enum representing marks: `X('X')`, `O('O')`, and `EMPTY(' ')` with opponent alternation logic. |
| `com.tictactoe.model` | `GameState` | Enum capturing game lifecycle states: `READY`, `PLAYING`, `X_WON`, `O_WON`, `DRAW`, `CLOSED`. |
| `com.tictactoe.model` | `CompletionReason` | Enum describing round completion mode: `NORMAL_WIN`, `DRAW`, or `TIMEOUT`. |
| `com.tictactoe.model` | `Scoreboard` | Thread-safe session score tracker maintaining X wins, O wins, draws, and total rounds using `AtomicInteger`. |
| `com.tictactoe.controller` | `GameController` | Central coordinator. Manages turns, validates moves, stops timers, checks outcomes, records scores, and serializes match history. |
| `com.tictactoe.controller` | `GameEventListener` | Observer interface decoupling controller events from UI presentation. |
| `com.tictactoe.controller` | `GameEventAdapter` | Convenience adapter with no-op default implementations. |
| `com.tictactoe.timer` | `TurnTimer` | Background worker managing turn countdowns using `ScheduledExecutorService` with daemon threads and generation tokens. |
| `com.tictactoe.timer` | `TimerListener` | Callback interface for 1-second countdown ticks and timeout expiration events. |
| `com.tictactoe.persistence`| `MatchRecord` | Immutable record representing a completed match with date, players, winner, completion reason, and duration. |
| `com.tictactoe.persistence`| `MatchHistory` | Local file persistence service. Writes and reads match records to `data/match_history.csv` with RFC 4180 escaping and error recovery. |
| `com.tictactoe.exception` | `TicTacToeException` | Base checked domain exception. |
| `com.tictactoe.exception` | `InvalidMoveException` | Custom checked exception thrown when an out-of-bounds or occupied cell move is attempted. |
| `com.tictactoe.ui` | `GameUI` | Main Swing desktop window containing the 3×3 board, setup form, countdown progress bar, scoreboard cards, and action buttons. |
| `com.tictactoe.ui` | `HistoryDialog` | Modal dialog containing a styled `JTable` for viewing and clearing persistent match history. |
| `com.tictactoe.ui` | `UITheme` | Design tokens: modern dark-slate color palette, typography hierarchy, and spacing constants. |

---

## 3. Game State Model and Invariants

### State Transition Diagram

```
                 [startNewMatch]
                        |
                        v
                     +----+
                     |READY|
                     +----+
                        |
                 [startNewRound]
                        |
                        v
               +-----------------+
        +----->|     PLAYING     |<-----+
        |      +-----------------+      |
[Valid Move]      |     |     |     [Valid Move]
(Turn Alternates) |     |     |  (Turn Alternates)
        +---------+     |     +---------+
                        |
        +---------------+---------------+
        |               |               |
 [3 in a Row]      [9 Occupied]     [Timeout]
        |               |               |
        v               v               v
    +-------+        +----+         +-------+
    | X_WON |        |DRAW|         | O_WON |
    | O_WON |        +----+         | X_WON |
    +-------+                       +-------+
        \               |               /
         \              |              /
          v             v             v
       [Scoreboard & Match History Updated (Once)]
                        |
                        +---> [New Round: Returns to PLAYING]
                        |
                        +---> [Exit: Transitions to CLOSED]
```

### Invariants Enforced by Architecture
1. **Cell Immutability**: Once a cell contains `X` or `O`, it can never be rewritten during that round.
2. **Turn Alternation**: Only the current active player can place a mark; a successful move advances the turn at most once.
3. **Terminal Inactivity**: No moves are accepted once a round transitions to `X_WON`, `O_WON`, `DRAW`, or `CLOSED`.
4. **Single-Resolution Guarantee**: Every round result (win, draw, or timeout forfeiture) updates scoreboard tallies and appends to persistent match history exactly once.
5. **Generational Timer Invalidation**: Stale timeout callbacks from previous turns or rounds cannot trigger forfeits on subsequent turns.
6. **EDT Safety**: All Swing component updates occur strictly on the Swing Event Dispatch Thread.

---

## 4. Multithreading and Concurrency Strategy

### 4.1 Background Worker Design
Turn countdowns run on a dedicated background thread managed by a `ScheduledExecutorService`.
- A custom `ThreadFactory` creates daemon threads (`setDaemon(true)`), guaranteeing that background timer threads will never prevent the JVM from shutting down cleanly.
- Periodic 1-second ticks update the remaining seconds counter and notify listeners.

### 4.2 Race Condition Resolution: Move vs Timeout
A classic concurrency challenge occurs when a player clicks a cell at the exact sub-millisecond instant that their turn timer countdown reaches zero.

**Resolution Strategy**:
`GameController` maintains an authoritative monitor lock `private final Object stateLock = new Object();`.
Both `handleMove(row, col)` and `onTimeout(turnToken)` synchronize on this lock:

```java
// Thread 1 (EDT / Player Click):
public boolean handleMove(int row, int col) {
    synchronized (stateLock) {
        if (gameState != GameState.PLAYING || roundCompleted) {
            return false;
        }
        turnTimer.stop(); // Invalidate timer immediately
        board.makeMove(row, col, currentPlayer.getSymbol());
        ...
    }
}

// Thread 2 (Background Timer Worker):
@Override
public void onTimeout(long turnToken) {
    synchronized (stateLock) {
        if (turnToken != currentTurnToken || gameState != GameState.PLAYING || roundCompleted) {
            return; // Dropped if stale or round already completed
        }
        completeRound(finalState, CompletionReason.TIMEOUT, winner, null);
    }
}
```

- **Scenario 1 (Move arrives first)**: The move acquires `stateLock`. The timer is stopped and `currentTurnToken` is advanced. When the background timeout thread acquires `stateLock`, `turnToken != currentTurnToken` evaluates to true, and the timeout is discarded.
- **Scenario 2 (Timeout arrives first)**: The timeout acquires `stateLock`. `roundCompleted` is set to `true` and the round concludes. When the player's click acquires `stateLock`, `gameState != GameState.PLAYING` evaluates to true, and the late move is cleanly rejected.

### 4.3 Generational Tokens
Every time a turn begins or a round restarts, `currentTurnToken` is incremented monotonically.
- When `TurnTimer` schedules a countdown, it captures the current token.
- On each tick and on timeout, the callback checks whether the token matches `activeTurnToken` and `currentTurnToken`.
- If a turn ended early (e.g., player moved in 2 seconds), the old task cannot fire on the next player's turn.

---

## 5. Persistence Strategy

- **Format**: Comma-Separated Values (CSV) compliant with RFC 4180.
- **Header**: `Timestamp,PlayerX,PlayerO,Winner,WinnerSymbol,Reason,DurationSeconds`
- **Encoding**: Explicit `StandardCharsets.UTF_8` to ensure cross-platform consistency.
- **Resilience**:
  - Missing files or folders are automatically created on first append.
  - Corrupted or truncated rows are gracefully caught and skipped without aborting the read process.
  - Player names containing commas, quotes, or newlines are properly quoted and escaped.
