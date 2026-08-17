@echo off
setlocal

call scripts\build.cmd
if errorlevel 1 exit /b 1

if not exist backend\test-out mkdir backend\test-out

javac -encoding UTF-8 -cp backend\out;lib\sqlite-jdbc-3.53.2.0.jar -d backend\test-out backend\src\test\java\com\jsys\ChinaAccountMigrationIntegrationTest.java
if errorlevel 1 (
  echo Test compilation failed.
  exit /b 1
)

java -cp backend\test-out;backend\out;lib\sqlite-jdbc-3.53.2.0.jar com.jsys.ChinaAccountMigrationIntegrationTest
