# 25 Comprehensive Viva Questions and Answers

## Multithreaded Tic-Tac-Toe: A Two-Player Timed Game

This document provides 25 likely viva and oral exam questions along with concise, technically rigorous answers, directly mapped to the implementation of this project.

---

### Category 1: Object-Oriented Programming (OOP) Principles

#### Q1: How does this project demonstrate Abstraction?
**Answer**: Abstraction is demonstrated through the `Player` abstract base class and the `GameEventListener` interface. The `Player` class defines generic player properties (`name`, `symbol`) and an abstract method `getPlayerType()`, hiding concrete implementation details. External consumers interact with the `Player` contract without needing to know specific subclass mechanics.

#### Q2: Where is Inheritance implemented, and why was it chosen?
**Answer**: The `HumanPlayer` class extends the abstract `Player` class (`public class HumanPlayer extends Player`). This models an "is-a" relationship where a human participant is a specialized form of a player, inheriting core validation and symbol assignment while implementing `getPlayerType()`.

#### Q3: What is Encapsulation, and how is it enforced across this codebase?
**Answer**: Encapsulation is the bundling of data and methods that operate on that data, while restricting direct access to internal state. In `Board.java`, the 2D grid array `private final PlayerSymbol[][] grid;` is private. Callers cannot mutate cells directly; they must call `makeMove()`, which validates bounds and occupancy. Additionally, `getGridCopy()` returns a defensive deep copy so callers cannot mutate the internal array.

#### Q4: How does this project demonstrate Method Overriding?
**Answer**:
1. `HumanPlayer.java` overrides the abstract method `getPlayerType()` defined in `Player`.
2. `Player.java` and `MatchRecord.java` override standard `Object` methods: `toString()`, `equals()`, and `hashCode()`.
3. `GameEventAdapter.java` overrides interface methods from `GameEventListener` with default no-op behaviors.

#### Q5: Where is Method Overloading demonstrated in the project?
**Answer**: Method overloading occurs when multiple methods in the same class share the same name with different parameter signatures:
1. `Board.java` provides `isOccupied(int row, int col)` (coordinate-based) and `isOccupied(int index)` (linear index 0–8).
2. `GameController.java` provides a default constructor `GameController()` and an overloaded constructor `GameController(board, scoreboard, timer, history)` for dependency injection.
3. `MatchHistory.java` provides a default constructor `MatchHistory()` and an overloaded `MatchHistory(Path filePath)`.

#### Q6: Why did you use Java Enums instead of integer constants or strings for game state?
**Answer**: Enums like `GameState`, `PlayerSymbol`, and `CompletionReason` provide compile-time type safety. They eliminate invalid values (e.g. passing `"WINNER"` or `-1`), provide clear semantics, support custom helper methods (`isGameOver()`, `getOpponent()`), and prevent silent string typos from causing runtime bugs.

---

### Category 2: Exception Handling

#### Q7: What is the difference between checked and unchecked exceptions? Which one did you create?
**Answer**:
- **Checked exceptions** (extending `java.lang.Exception`) must be explicitly declared in a method's `throws` clause or handled via `try-catch` at compile time.
- **Unchecked exceptions** (extending `java.lang.RuntimeException`) represent programming bugs or unexpected errors that the compiler does not force you to catch.
- In this project, `InvalidMoveException` extends `TicTacToeException` (which extends `Exception`), making it a **checked exception**. This forces the controller and UI to explicitly anticipate and handle illegal moves.

#### Q8: What context does `InvalidMoveException` encapsulate?
**Answer**: `InvalidMoveException` stores the specific `row` and `col` where the violation took place, along with an explanatory error message (e.g. out of bounds or cell already occupied).

#### Q9: How are file I/O exceptions handled to prevent application crashes?
**Answer**: In `MatchHistory.java`, file read and write operations use `try-with-resources` blocks catching `IOException`. When loading match records, corrupted or malformed lines throw `IllegalArgumentException`, which is caught locally inside the loop to skip the bad record without aborting the loading of remaining valid matches.

---

### Category 3: File Handling and Persistence

#### Q10: Why did you use `java.nio.file` instead of legacy `java.io.File`?
**Answer**: `java.nio.file` (NIO.2) introduced in Java 7 provides superior file operations via `Path` and `Files`. It offers explicit character encoding support, atomic file operations, better exception reporting, and streamlined streaming utilities compared to legacy `java.io.File`.

#### Q11: How do you handle special characters (commas, quotes) in player names during CSV serialization?
**Answer**: In `MatchRecord.java`, the `escapeCsv()` method adheres to RFC 4180: if a player name contains commas, double quotes, or newlines, the entire field is enclosed in double quotes (`"..."`), and any internal double quote is escaped by doubling it (`""`).

#### Q12: Why is the match history appended rather than completely rewritten on every round?
**Answer**: Appending using `StandardOpenOption.APPEND` only writes the single new record to the end of the file. This is an $O(1)$ disk operation, prevents memory overhead of serializing past matches, and avoids data loss if a crash occurs during writing.

---

### Category 4: Multithreading and Concurrency

#### Q13: Why did you choose `ScheduledExecutorService` for the turn timer instead of `Thread.sleep()` in a loop?
**Answer**: `Thread.sleep()` inside a thread blocks that thread, requires manual loop conditions, and makes precise timing and safe cancellation difficult. `ScheduledExecutorService` is part of `java.util.concurrent`, manages thread lifecycles efficiently, executes periodic tasks with fixed-rate scheduling, and provides robust `ScheduledFuture.cancel()` mechanisms.

#### Q14: Why must the timer thread be set as a Daemon thread?
**Answer**: A standard non-daemon thread keeps the Java Virtual Machine running even if all user windows are closed. Setting `thread.setDaemon(true)` via a custom `ThreadFactory` ensures that the timer thread terminates automatically when the main application shuts down.

#### Q15: What is the Swing Event Dispatch Thread (EDT), and why should timer tasks never touch Swing components directly?
**Answer**: Swing is single-threaded; its components are not thread-safe. All drawing, layout, and event handling must take place on the Event Dispatch Thread (EDT). If a background timer thread directly modifies a `JLabel` or `JButton`, it causes race conditions, visual tearing, and unpredictable deadlocks. We use `SwingUtilities.invokeLater()` to marshal updates safely onto the EDT.

#### Q16: What is a Race Condition, and where could one occur in this application?
**Answer**: A race condition occurs when the outcome of a program depends on the non-deterministic relative execution timing of multiple threads. In this game, a race condition could occur if a player clicks a cell at the exact microsecond their turn timer expires.

#### Q17: How does your code guarantee that only one event (the move or the timeout) resolves a turn?
**Answer**: Both `handleMove()` and `onTimeout()` synchronize on an internal monitor object `private final Object stateLock = new Object();` in `GameController`. Because the methods are mutually exclusive under `stateLock`:
- If the move acquires the lock first, the timer is stopped, the mark is placed, and the generation token is incremented. The later timeout fails the token check and is dropped.
- If the timeout acquires the lock first, `roundCompleted` is set to `true` and the round ends. The later move sees `gameState != GameState.PLAYING` and is rejected.

#### Q18: What is a "Generation Token" (Turn Token) and why is it used?
**Answer**: `currentTurnToken` is a monotonically increasing `long` counter. Every time a turn starts or a round restarts, the token increments. When a timer task is scheduled, it is bound to that token. If a callback arrives after a move has already been made or a round restarted, the token mismatch (`turnToken != currentTurnToken`) immediately invalidates the stale task.

#### Q19: Why did you use `AtomicInteger` in `Scoreboard.java`?
**Answer**: `AtomicInteger` uses hardware-level Compare-And-Swap (CAS) instructions to provide thread-safe, atomic increments (`incrementAndGet()`) without the overhead of heavy locks.

#### Q20: How do you prevent deadlocks in this multithreaded application?
**Answer**: Deadlocks occur when threads hold locks and wait for each other in a cyclic dependency. We prevent deadlocks by:
1. Maintaining a single monitor lock (`stateLock`) in `GameController` (no nested lock acquisition).
2. Releasing locks before firing long-running callbacks.
3. Keeping the Swing EDT free of blocking calls (`await()`, `join()`, `sleep()`).

---

### Category 5: GUI, Architecture, and Testing

#### Q21: What is the Observer Pattern, and how is it used here?
**Answer**: The Observer pattern defines a one-to-many dependency between objects. `GameController` acts as the Subject maintaining a list of `GameEventListener` observers. When game events occur (move placed, turn switched, round ended), the controller notifies registered listeners without having direct coupling to the Swing GUI classes.

#### Q22: What happens if a player runs out of time?
**Answer**: Per the project specification, the player who runs out of time forfeits the round. Their opponent is declared the winner with `CompletionReason.TIMEOUT`, the scoreboard is updated once, and the match is saved to history.

#### Q23: How does the "New Round" button differ from "Start Match / Reset Scores"?
**Answer**:
- **New Round**: Resets the 3×3 board and starts a fresh turn for Player X, but **preserves** the cumulative scoreboard wins/draws.
- **Reset Scores**: Zeroes the session scoreboard tallies back to 0-0-0 after asking the user for confirmation via a dialog.

#### Q24: How did you test concurrency without relying on unpredictable `Thread.sleep()`?
**Answer**: In `ConcurrencyTest.java`, deterministic synchronization constructs such as `CountDownLatch` and `CyclicBarrier` were used to release multiple threads simultaneously at a shared starting gate. This guarantees simultaneous contention on `handleMove` and `onTimeout` to reliably test thread safety.

#### Q25: Why is `MatchRecord` an immutable class?
**Answer**: `MatchRecord` represents a historical event that has already occurred. Making it immutable (all fields `final`, no setters, defensive constructor copying) guarantees that match history records cannot be tampered with or corrupted across threads once created.
