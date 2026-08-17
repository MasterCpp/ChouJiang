@echo off
setlocal

if not exist backend\out mkdir backend\out

javac -encoding UTF-8 -cp lib\sqlite-jdbc-3.53.2.0.jar -d backend\out backend\src\main\java\com\jsys\*.java
if errorlevel 1 (
  echo Build failed.
  exit /b 1
)

echo Build complete.
