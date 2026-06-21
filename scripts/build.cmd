@echo off
setlocal

if not exist backend\out mkdir backend\out

javac -encoding UTF-8 -d backend\out backend\src\main\java\com\jsys\App.java
if errorlevel 1 (
  echo Build failed.
  exit /b 1
)

echo Build complete.
