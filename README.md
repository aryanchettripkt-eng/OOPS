# Multithreaded Tic-Tac-Toe: A Two-Player Timed Game

An academic Object-Oriented Programming (OOP) desktop application implemented in Java demonstrating classes, inheritance, abstraction, exception handling, local CSV persistence, multithreading, and thread synchronization.

---

## 1. Project Overview

**Multithreaded Tic-Tac-Toe** is an interactive desktop game built with Java Swing and Java Concurrency Utilities. Unlike traditional Tic-Tac-Toe, each player's turn is governed by an active countdown timer (default: 10 seconds). If a player fails to make a move before the timer expires, they forfeit the round and their opponent is awarded an immediate victory.

The system is architected using a layered Model-View-Controller (MVC) design where game rules, concurrency coordination, and persistence are completely decoupled from the Swing presentation layer.

---

## 2. Key Features

- **Standard 3×3 Grid Game Logic**: Complete row, column, and diagonal win detection; draw detection upon full board occupancy.
- **Single-Player vs Computer (AI) & Two-Player Pass & Play**: Play against an intelligent computer AI opponent running on background daemon threads, or face off against a friend locally.
- **Instant 1-Click Interactive Play**: Click any cell immediately to start playing; board clicks automatically initialize the match and restart new rounds without confusing lockouts.
- **Dynamic Turn Timer**: Dedicated background countdown worker with real-time remaining-seconds display, color-coded urgency indicators, and an animated progress bar.
- **Race Condition Prevention**: Synchronized monitor locks ensure atomic resolution between player clicks and sub-millisecond turn timeouts.
- **Generation Token Invalidation**: Monotonically increasing generation tokens prevent stale timer callbacks from affecting subsequent turns or rounds.
- **Session Scoreboard**: Real-time tracking of Player X wins, Player O wins, draws, and total completed rounds.
- **Persistent Match History**: Completed rounds are safely appended to a local CSV file (`data/match_history.csv`) with full RFC 4180 escaping and error recovery.
- **History Viewer**: Interactive modal dialog featuring a styled `JTable` to inspect past matches and clear records.
- **Modern Swing GUI**: High-contrast dark slate color palette, bold X/O symbols, winning line highlighting, and responsive action controls.

---

## 3. Technology Stack & Prerequisites

- **Language**: Java 17 LTS or newer (tested with OpenJDK / Oracle JDK 21).
- **GUI Framework**: Java Swing (JDK standard library).
- **Build Tool**: Apache Maven 3.8+ (wrapper scripts provided).
- **Testing**: JUnit 5 (Jupiter 5.10.2).
- **Persistence**: Local CSV file via `java.nio.file`.
- **External Runtime Dependencies**: None beyond the standard Java Development Kit (JDK).

---

## 4. Project Structure

```
multithreaded-tictactoe/
├── pom.xml                                 # Maven project configuration
├── mvn.cmd / mvnw.cmd                      # Maven wrapper command scripts
├── README.md                               # Project documentation & run guide
├── DESIGN.md                               # Architectural and concurrency design
├── VIVA_QUESTIONS.md                       # 25 Viva preparation questions & answers
├── SYLLABUS_MAPPING.md                     # Traceability to academic syllabus
├── data/
│   └── match_history.csv                   # Local persistent CSV store
└── src/
    ├── main/java/com/tictactoe/
    │   ├── Main.java                       # Application entry point (EDT launcher)
    │   ├── controller/
    │   │   ├── GameController.java         # Lifecycle coordinator & state authority
    │   │   ├── GameEventListener.java      # Observer event listener interface
    │   │   └── GameEventAdapter.java       # Convenience adapter implementation
    │   ├── model/
    │   │   ├── Board.java                  # 3x3 grid model & win algorithms
    │   │   ├── Player.java                 # Abstract base class
    │   │   ├── HumanPlayer.java            # Concrete Player subclass
    │   │   ├── AIPlayer.java               # Concrete AI Player subclass with heuristic logic
    │   │   ├── PlayerSymbol.java           # X, O, EMPTY enum with opponent logic
    │   │   ├── GameState.java              # Lifecycle state enum
    │   │   ├── CompletionReason.java       # NORMAL_WIN, DRAW, TIMEOUT enum
    │   │   └── Scoreboard.java             # Thread-safe session score tracker
    │   ├── timer/
    │   │   ├── TurnTimer.java              # Background timer with ScheduledExecutor
    │   │   └── TimerListener.java          # Tick & timeout callback interface
    │   ├── persistence/
    │   │   ├── MatchRecord.java            # Immutable match value object & CSV parser
    │   │   └── MatchHistory.java           # File handling service with RFC 4180 escaping
    │   ├── exception/
    │   │   ├── TicTacToeException.java     # Base checked domain exception
    │   │   └── InvalidMoveException.java   # Checked invalid move exception
    │   └── ui/
    │       ├── GameUI.java                 # Main desktop Swing interface
    │       ├── HistoryDialog.java          # Match history JTable viewer dialog
    │       └── UITheme.java                # Design tokens & color palette
    └── test/java/com/tictactoe/
        ├── model/BoardTest.java            # Board model & win/draw unit tests (13 tests)
        ├── model/AIPlayerTest.java         # AI move logic & concurrency integration tests (5 tests)
        ├── controller/GameControllerTest.java # Controller & turn lifecycle tests (9 tests)
        ├── persistence/MatchHistoryTest.java # CSV read/write/escaping tests (6 tests)
        ├── concurrency/ConcurrencyTest.java # Race condition & multithreading tests (4 tests)
        └── ui/GameUITest.java              # Swing GUI initialization & EDT tests (2 tests)
```

---

## 5. Quick Start (Clone & Play)

Anyone who clones this repository can immediately run the game using any of the following methods:

### Method 1: Instant 1-Click Launch (No Maven Required)
Only a standard **JDK 17+** is needed:
- **Windows**: Double-click `run.bat` or run:
  ```powershell
  .\run.bat
  ```
- **macOS / Linux**: Run:
  ```bash
  chmod +x run.sh && ./run.sh
  ```

### Method 2: Cross-Platform Maven Wrapper (Auto-installs Maven if missing)
- **Windows**:
  ```powershell
  .\mvnw.cmd exec:java
  ```
- **macOS / Linux**:
  ```bash
  ./mvnw exec:java
  ```

### Method 3: Standard Maven (If Maven is already installed)
```powershell
mvn exec:java
```

### Method 4: Packaged JAR
```powershell
.\mvnw.cmd clean package
java -jar target/multithreaded-tictactoe-1.0.0.jar
```

---

## 6. Running Automated Tests

The automated test suite contains 34 unit and concurrency tests covering board validation, player alternation, timeouts, CSV formatting, and multi-threaded race conditions.

To run all automated tests:
```powershell
.\mvn.cmd test
```

To run a specific test class:
```powershell
.\mvn.cmd test -Dtest=ConcurrencyTest
.\mvn.cmd test -Dtest=BoardTest
.\mvn.cmd test -Dtest=GameControllerTest
.\mvn.cmd test -Dtest=MatchHistoryTest
```

---

## 7. How to Play

1. **Player Setup**: Enter custom names for **Player X** and **Player O**, adjust the turn timer (3–60 seconds, default: 10s), and click **Start Match**.
2. **Taking Turns**: Player X makes the first move. Players alternate clicking empty cells on the 3×3 grid.
3. **Beating the Clock**: The active countdown displays remaining seconds and a color-coded bar. If a player fails to move within their allotted time, the opponent is automatically awarded the round!
4. **Winning & Draws**: A round ends immediately upon 3-in-a-row (horizontal, vertical, or diagonal) or when all 9 cells are filled (draw). The winning line is highlighted in emerald green.
5. **Next Rounds**:
   - Click **New Round** to clear the board and start the next round while **retaining cumulative scores**.
   - Click **Reset Scores** (with confirmation) to reset session scores back to 0-0.
6. **Viewing Match History**: Click **Match History** at any time to open the persistent records table, review past matches, or clear records.

---

## 8. Data Storage Location

All completed matches are stored locally in:
`data/match_history.csv`

The file is automatically created on the first completed match and preserved across application restarts.
