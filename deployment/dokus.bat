@echo off
REM Dokus Server Installation and Startup Script
REM Supports: Windows 10/11
REM Usage: dokus.bat

setlocal enabledelayedexpansion

REM Banner
echo.
echo ============================================
echo.
echo              DOKUS SERVER
echo     Financial Management Platform
echo.
echo ============================================
echo.

echo [*] Detected: Windows
echo.

REM Check if Docker is installed
echo [1/6] Checking Docker installation...

where docker >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [!] Docker is not installed
    echo.
    set /p INSTALL_DOCKER="Would you like to install Docker Desktop? (y/n): "

    if /i "!INSTALL_DOCKER!"=="y" (
        echo [*] Opening Docker Desktop download page...
        start https://www.docker.com/products/docker-desktop/
        echo.
        echo Please:
        echo   1. Download and install Docker Desktop
        echo   2. Start Docker Desktop
        echo   3. Run this script again
        pause
        exit /b 0
    ) else (
        echo [X] Docker is required to run Dokus
        echo Please install Docker Desktop from: https://docker.com
        pause
        exit /b 1
    )
) else (
    echo [+] Docker is installed
)

REM Check if Docker is running
docker info >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [X] Docker is not running
    echo Please start Docker Desktop and run this script again
    pause
    exit /b 1
)
echo [+] Docker is running

REM Check if Docker Compose is available
docker compose version >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    docker-compose version >nul 2>nul
    if %ERRORLEVEL% NEQ 0 (
        echo [X] Docker Compose not found
        echo Please install Docker Desktop which includes Docker Compose
        pause
        exit /b 1
    )
)
echo [+] Docker Compose is installed
echo.

REM Check for .env file
echo [2/6] Checking configuration...

if not exist ".env" (
    echo [!] .env file not found
    echo.

    if exist ".env.example" (
        copy .env.example .env >nul
        echo [+] Created .env from .env.example
    ) else (
        echo [X] .env.example not found
        pause
        exit /b 1
    )

    echo.
    echo ============================================
    echo IMPORTANT: Configure your environment
    echo ============================================
    echo.
    echo A .env file has been created with default values.
    echo You MUST edit it and set secure passwords before continuing.
    echo.
    echo Required variables to set:
    echo   - DB_PASSWORD
    echo   - REDIS_PASSWORD
    echo   - RABBITMQ_PASSWORD
    echo   - JWT_SECRET (at least 32 characters^)
    echo.
    pause

    notepad .env

    echo.
    set /p CONFIGURED="Have you updated all required passwords? (y/n): "
    if /i not "!CONFIGURED!"=="y" (
        echo [X] Please configure .env and run this script again
        pause
        exit /b 1
    )
) else (
    echo [+] .env file exists
)
echo.

REM Configure Docker for insecure registry
echo [3/6] Configuring Docker registry...
echo.
echo [!] Please configure insecure registry in Docker Desktop:
echo   1. Open Docker Desktop
echo   2. Go to Settings -^> Docker Engine
echo   3. Add the following to the JSON config:
echo      "insecure-registries": ["94.111.226.82:5000"]
echo   4. Click 'Apply ^& Restart'
echo.
pause

REM Pull latest images
echo [4/6] Pulling latest Docker images...
echo This may take a few minutes...

docker compose pull
if %ERRORLEVEL% NEQ 0 (
    echo [X] Failed to pull images
    echo Check your internet connection and Docker registry configuration
    pause
    exit /b 1
)
echo [+] Images pulled successfully
echo.

REM Start services
echo [5/6] Starting Dokus services...

docker compose up -d
if %ERRORLEVEL% NEQ 0 (
    echo [X] Failed to start services
    pause
    exit /b 1
)
echo [+] Services started
echo.

REM Wait for services to be healthy
echo [6/6] Waiting for services to be ready...
echo This may take up to 2 minutes...

timeout /t 10 /nobreak >nul

set MAX_RETRIES=30
set RETRY_COUNT=0

:CHECK_HEALTH
docker compose ps | findstr /C:"healthy" >nul
if %ERRORLEVEL% EQU 0 (
    echo [+] Services are starting up...
)

set /a RETRY_COUNT+=1
if %RETRY_COUNT% GEQ %MAX_RETRIES% (
    echo [!] Some services may still be starting
    echo Run 'docker compose ps' to check service status
    goto AFTER_HEALTH
)

timeout /t 5 /nobreak >nul
goto CHECK_HEALTH

:AFTER_HEALTH
echo.

REM Configure auto-start
echo Configure Auto-Start
set /p AUTO_START="Would you like Dokus to start automatically with Docker Desktop? (y/n): "

if /i "!AUTO_START!"=="y" (
    REM For Windows, we rely on Docker Desktop's restart policy
    echo.
    echo [+] Auto-start is configured via Docker's restart policy
    echo Dokus will start automatically when Docker Desktop starts
    echo.
    echo To make Docker Desktop start on boot:
    echo   1. Open Docker Desktop Settings
    echo   2. Go to General
    echo   3. Enable "Start Docker Desktop when you log in"
) else (
    echo Skipping auto-start configuration
)

REM Display status
echo.
echo ============================================
echo [+] Dokus Server is running!
echo ============================================
echo.
echo Services available at:
echo   Auth Service:      http://localhost:6091
echo   Invoicing Service: http://localhost:6092
echo   Expense Service:   http://localhost:6093
echo   Payment Service:   http://localhost:6094
echo   Reporting Service: http://localhost:6095
echo   Audit Service:     http://localhost:6096
echo   Banking Service:   http://localhost:6097
echo.
echo   RabbitMQ UI:       http://localhost:15672
echo.
echo Useful commands:
echo   View logs:    docker compose logs -f
echo   Stop:         docker compose stop
echo   Restart:      docker compose restart
echo   Update:       docker compose pull ^&^& docker compose up -d
echo   Uninstall:    docker compose down -v
echo.
pause
