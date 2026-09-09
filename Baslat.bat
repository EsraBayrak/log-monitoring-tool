@echo off
title Log Monitoring Tool
cd /d "%~dp0"

echo ====================================================
echo   Log Monitoring Tool Baslatiliyor...
echo ====================================================

:: JAR henuz olusturulmamissa otomatik olarak derle
if not exist "target\log-monitoring-tool-0.0.1-SNAPSHOT.jar" (
    echo [*] Ilk calistirma tespit edildi. Uygulama derleniyor...
    call mvnw.cmd clean package -DskipTests
)

:: 4 saniye sonra tarayicida dashboard'u ac
start "" cmd /c "timeout /t 4 /nobreak >nul && start http://localhost:8085"

:: Paketlenmis JAR dosyasini calistir
java -jar target\log-monitoring-tool-0.0.1-SNAPSHOT.jar

pause