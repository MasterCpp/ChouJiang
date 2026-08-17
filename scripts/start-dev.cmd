@echo off
setlocal

call scripts\build.cmd
if errorlevel 1 exit /b 1

set PORT=%1
if "%PORT%"=="" set PORT=8080

java -cp backend\out;lib\sqlite-jdbc-3.53.2.0.jar com.jsys.App %PORT%
