#!/usr/bin/env bash
set -e

echo "========================================================="
echo " Multithreaded Tic-Tac-Toe: A Two-Player Timed Game"
echo "========================================================="
echo ""

if ! command -v javac &> /dev/null; then
    echo "[ERROR] 'javac' compiler was not found in PATH."
    echo "Please make sure Java 17 or newer JDK is installed."
    exit 1
fi

echo "Compiling source files..."
mkdir -p bin
find src/main/java -name "*.java" > sources.txt
javac -encoding UTF-8 -d bin @sources.txt
rm sources.txt

echo "Launching application..."
java -cp bin com.tictactoe.Main &
echo "Done!"
