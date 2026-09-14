@echo off
chcp 65001 > nul
title Log Monitoring Tool - Launcher

echo ============================================================
echo   Log & Konfigurasyon Izleme Araci Baslatiliyor...
echo ============================================================

:: 1. Calisma dizinini .bat dosyasinin bulundugu klasore sabitle
cd /d "%~dp0"

:: Eger ic ice log-monitoring-tool klasoru varsa icine gir
if exist "log-monitoring-tool\pom.xml" (
    cd log-monitoring-tool
)

:: 2. Java kontrolu
java -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [HATA] Java bulunamadi! Lutfen Java 21'in PATH'e ekli oldugundan emin olun.
    pause
    exit /b 1
)

:: 3. Maven Wrapper kontrolu
if not exist "mvnw.cmd" (
    echo [HATA] mvnw.cmd dosyasi bulunamadi!
    echo Bulunulan Dizin: %cd%
    pause
    exit /b 1
)

:: 4. 8085 portunu kullanan eski surec varsa temizle
powershell -NoProfile -Command "Get-NetTCPConnection -LocalPort 8085 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }"

:: 5. Tarayiciyi Spring Boot ayaga kalktiktan sonra ac (8 saniye gecikmeli)
start "" powershell -NoProfile -WindowStyle Hidden -Command "Start-Sleep -Seconds 8; Start-Process 'http://localhost:8085/logMonitoring'"

echo [BILGI] Uygulama baslatiliyor (Port: 8085)...
echo [BILGI] Tarayici uygulama hazir olunca otomatik acilacak...

call mvnw.cmd spring-boot:run

if %ERRORLEVEL% NEQ 0 (
    echo [HATA] Uygulama calisirken bir sorun olustu.
    pause
)