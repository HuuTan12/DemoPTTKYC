@echo off
cd /d "%~dp0"
echo Checking Docker Desktop...
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo Starting Docker Desktop... Please wait about 15-20 seconds for Docker to start.
    start "" "C:\Program Files\Docker\Docker\Docker Desktop.exe"
    :wait_docker
    timeout /t 3 /nobreak >nul
    docker info >nul 2>&1
    if %errorlevel% neq 0 goto wait_docker
)
echo Starting MySQL 8.0 container on port 3306 (User: root, Password: empty)...
docker compose up -d
echo Waiting 10 seconds for MySQL to initialize schema.sql and sample_data.sql...
timeout /t 10 /nobreak >nul
echo ============================================================================
echo MySQL 8 is RUNNING on 127.0.0.1:3306 (User: root, Password: empty, DB: fastship)
echo You can now click "Connect" in TablePlus!
echo ============================================================================
pause
