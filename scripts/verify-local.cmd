@echo off
setlocal

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Stop';" ^
  "$health = Invoke-RestMethod 'http://127.0.0.1:8080/api/health';" ^
  "if ($health.status -ne 'ok') { throw 'Health check failed' }" ^
  "$homeResponse = Invoke-WebRequest -UseBasicParsing 'http://127.0.0.1:8080/';" ^
  "if ($homeResponse.StatusCode -ne 200) { throw 'Home page failed' }" ^
  "Write-Host 'Local verification passed: health ok, home page 200.'"
