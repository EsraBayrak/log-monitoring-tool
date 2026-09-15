@echo off
setlocal enabledelayedexpansion
title Log Monitoring Tool - Baslatici

cd /d "%~dp0"
echo ====================================================
echo      LOG MONITORING TOOL BASLATILIYOR
echo ====================================================

:: 1. Eski 8085 portunu kullanan süreç varsa temizle
echo [1/3] Port 8085 kontrol ediliyor...
powershell -NoProfile -Command "$p = (Get-NetTCPConnection -LocalPort 8085 -ErrorAction SilentlyContinue).OwningProcess; if ($p) { Stop-Process -Id $p -Force; Write-Host 'Eski islem sonlandirildi.' }"

:: 2. Spring Boot uygulamasini arka planda baslat
echo [2/3] Spring Boot uygulamasi baslatiliyor...
cd log-monitoring-tool
start "Log Monitoring Backend" cmd /c "mvnw.cmd spring-boot:run"

:: 3. Port 8085 hazir olana kadar bekle (Maksimum 60 saniye)
echo [3/3] Uygulama hazir olana kadar bekleniyor...
powershell -NoProfile -Command ^
  "$timeout = 60; $timer = [Diagnostics.Stopwatch]::StartNew();" ^
  "while ($timer.Elapsed.TotalSeconds -lt $timeout) {" ^
  "  try {" ^
  "    $client = New-Object System.Net.Sockets.TcpClient('127.0.0.1', 8085);" ^
  "    $client.Close();" ^
  "    exit 0;" ^
  "  } catch {" ^
  "    Start-Sleep -Milliseconds 800;" ^
  "  }" ^
  "}" ^
  "exit 1;"

if %ERRORLEVEL% EQU 0 (
    echo Uygulama hazir! Tarayici aciliyor...
    start http://localhost:8085/logMonitoring
) else (
    echo [UYARI] Zaman asimi! Uygulama henuz acilmamis olabilir, log konsolunu inceleyin.
)

exit