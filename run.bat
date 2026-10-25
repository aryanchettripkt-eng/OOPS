@echo off
setlocal
echo =========================================================
echo  Multithreaded Tic-Tac-Toe: A Two-Player Timed Game
echo =========================================================
echo.

where javac >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] 'javac' compiler was not found in PATH.
    echo Please make sure Java 17 or newer JDK is installed.
    echo.
    pause
    exit /b 1
)

echo Compiling source files...
if not exist "bin" mkdir "bin"
dir /s /b "src\main\java\*.java" > sources.txt
javac -encoding UTF-8 -d bin @sources.txt
set COMPILE_ERR=%errorlevel%
del sources.txt

if %COMPILE_ERR% neq 0 (
    echo [ERROR] Compilation failed.
    pause
    exit /b %COMPILE_ERR%
)

echo Launching application...
start javaw -cp bin com.tictactoe.Main
echo Done!
endlocal
