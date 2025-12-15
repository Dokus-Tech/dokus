@echo off
REM Dokus Cloud Management Script (Windows)
REM Usage: dokus.bat [command]

setlocal enabledelayedexpansion

cd /d "%~dp0"

REM Check if command provided
if "%1"=="" goto SHOW_MENU
if "%1"=="setup" goto INITIAL_SETUP
if "%1"=="start" goto START_SERVICES
if "%1"=="stop" goto STOP_SERVICES
if "%1"=="restart" goto RESTART_SERVICES
if "%1"=="status" goto SHOW_STATUS
if "%1"=="logs" goto SHOW_LOGS
if "%1"=="db" goto ACCESS_DB
goto SHOW_MENU

:SHOW_MENU
cls
echo.
echo ====================================================================
echo.
echo                  Dokus Cloud Management
echo.
echo ====================================================================
echo.
echo   What would you like to do?
echo.
echo   Service Management
echo     1  Initial Setup (first time only)
echo     2  Start services
echo     3  Stop services
echo     4  Restart services
echo     5  Show status
echo.
echo   Development Tools
echo     6  View logs
echo     7  Access database
echo.
echo   Mobile App
echo     8  Show mobile connection (QR code)
echo.
echo     0  Exit
echo.
set /p choice="   Enter choice [0-8]: "

if "%choice%"=="1" goto INITIAL_SETUP
if "%choice%"=="2" goto START_SERVICES
if "%choice%"=="3" goto STOP_SERVICES
if "%choice%"=="4" goto RESTART_SERVICES
if "%choice%"=="5" goto SHOW_STATUS
if "%choice%"=="6" goto SHOW_LOGS
if "%choice%"=="7" goto ACCESS_DB
if "%choice%"=="8" goto SHOW_MOBILE_CONNECTION
if "%choice%"=="0" (
    echo.
    echo   Goodbye!
    echo.
    exit /b 0
)

echo.
echo   [!] Invalid choice
timeout /t 2 /nobreak >nul
goto SHOW_MENU

:INITIAL_SETUP
cls
echo.
echo ====================================================================
echo   Initial Dokus Cloud Setup
echo ====================================================================
echo.
echo [*] Detected: Windows
echo.

REM Check Docker
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
        pause
        exit /b 1
    )
)
echo [+] Docker is installed

docker info >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [X] Docker is not running
    echo Please start Docker Desktop and run this script again
    pause
    exit /b 1
)
echo [+] Docker is running

docker compose version >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [X] Docker Compose not found
    pause
    exit /b 1
)
echo [+] Docker Compose is installed
echo.

REM Check for .env file
echo [2/6] Configuring environment...

if exist ".env" (
    echo [+] .env file exists
    echo.
    set /p RECONFIGURE="Would you like to reconfigure? (y/n): "
    if /i "!RECONFIGURE!"=="y" (
        echo [!] Backing up existing .env to .env.backup
        copy .env .env.backup >nul
        del .env
    ) else (
        goto SKIP_ENV_CONFIG
    )
)

echo [!] .env file not found - let's create one!
echo.

REM Generate passwords
for /f "delims=" %%i in ('powershell -Command "[Convert]::ToBase64String((1..24 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 })) -replace '[^a-zA-Z0-9]', '' | Select-Object -First 1" 2^>nul') do set "DB_PASS=%%i"
for /f "delims=" %%i in ('powershell -Command "[Convert]::ToBase64String((1..24 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 })) -replace '[^a-zA-Z0-9]', '' | Select-Object -First 1" 2^>nul') do set "REDIS_PASS=%%i"
for /f "delims=" %%i in ('powershell -Command "[Convert]::ToBase64String((1..24 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 })) -replace '[^a-zA-Z0-9]', '' | Select-Object -First 1" 2^>nul') do set "RABBITMQ_PASS=%%i"
for /f "delims=" %%i in ('powershell -Command "[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 })) -replace '[^a-zA-Z0-9]', '' | Select-Object -First 1" 2^>nul') do set "JWT_SECRET=%%i"

if "!DB_PASS!"=="" set "DB_PASS=changeme-db-%RANDOM%%RANDOM%%RANDOM%"
if "!REDIS_PASS!"=="" set "REDIS_PASS=changeme-redis-%RANDOM%%RANDOM%%RANDOM%"
if "!RABBITMQ_PASS!"=="" set "RABBITMQ_PASS=changeme-rabbitmq-%RANDOM%%RANDOM%%RANDOM%"
if "!JWT_SECRET!"=="" set "JWT_SECRET=changeme-jwt-%RANDOM%%RANDOM%%RANDOM%%RANDOM%%RANDOM%"

echo ====================================================================
echo   Configuration
echo ====================================================================
echo.

echo Database username:
echo Default: dokus
set /p "INPUT_DB_USERNAME=Value (press Enter for default): "
if "!INPUT_DB_USERNAME!"=="" (
    set "DB_USERNAME=dokus"
) else (
    set "DB_USERNAME=!INPUT_DB_USERNAME!"
)

echo.
echo Database password:
echo Default: ^<auto-generated^>
set /p "INPUT_DB_PASSWORD=Value (press Enter for default): "
if "!INPUT_DB_PASSWORD!"=="" (
    set "DB_PASSWORD=!DB_PASS!"
) else (
    set "DB_PASSWORD=!INPUT_DB_PASSWORD!"
)

echo.
echo Redis password:
echo Default: ^<auto-generated^>
set /p "INPUT_REDIS_PASSWORD=Value (press Enter for default): "
if "!INPUT_REDIS_PASSWORD!"=="" (
    set "REDIS_PASSWORD=!REDIS_PASS!"
) else (
    set "REDIS_PASSWORD=!INPUT_REDIS_PASSWORD!"
)

echo.
echo RabbitMQ username:
echo Default: dokus
set /p "INPUT_RABBITMQ_USERNAME=Value (press Enter for default): "
if "!INPUT_RABBITMQ_USERNAME!"=="" (
    set "RABBITMQ_USERNAME=dokus"
) else (
    set "RABBITMQ_USERNAME=!INPUT_RABBITMQ_USERNAME!"
)

echo.
echo RabbitMQ password:
echo Default: ^<auto-generated^>
set /p "INPUT_RABBITMQ_PASSWORD=Value (press Enter for default): "
if "!INPUT_RABBITMQ_PASSWORD!"=="" (
    set "RABBITMQ_PASSWORD=!RABBITMQ_PASS!"
) else (
    set "RABBITMQ_PASSWORD=!INPUT_RABBITMQ_PASSWORD!"
)

echo.
echo JWT secret:
echo Default: ^<auto-generated^>
set /p "INPUT_JWT_SECRET=Value (press Enter for default): "
if "!INPUT_JWT_SECRET!"=="" (
    set "JWT_SECRET_FINAL=!JWT_SECRET!"
) else (
    set "JWT_SECRET_FINAL=!INPUT_JWT_SECRET!"
)

REM Create .env file
(
    echo # Dokus Cloud Environment Configuration
    echo # Generated on %date% %time%
    echo.
    echo # DATABASE
    echo DB_USERNAME=!DB_USERNAME!
    echo DB_PASSWORD=!DB_PASSWORD!
    echo.
    echo # REDIS
    echo REDIS_HOST=redis
    echo REDIS_PORT=6379
    echo REDIS_PASSWORD=!REDIS_PASSWORD!
    echo.
    echo # RABBITMQ
    echo RABBITMQ_USERNAME=!RABBITMQ_USERNAME!
    echo RABBITMQ_PASSWORD=!RABBITMQ_PASSWORD!
    echo.
    echo # JWT
    echo JWT_SECRET=!JWT_SECRET_FINAL!
    echo JWT_ISSUER=https://dokus.tech
    echo JWT_AUDIENCE=dokus-api
    echo.
    echo # CACHE
    echo CACHE_TYPE=redis
    echo.
    echo # LOGGING
    echo LOG_LEVEL=INFO
) > .env

echo.
echo [+] Configuration saved to .env
echo.

:SKIP_ENV_CONFIG
echo [3/6] Configuring Docker registry...
echo.
echo [!] Please configure insecure registry in Docker Desktop:
echo   1. Open Docker Desktop
echo   2. Go to Settings -^> Docker Engine
echo   3. Add: "insecure-registries": ["94.111.226.82:5000"]
echo   4. Click 'Apply ^& Restart'
echo.
pause

echo [4/6] Pulling latest Docker images...
docker compose pull
if %ERRORLEVEL% NEQ 0 (
    echo [X] Failed to pull images
    pause
    exit /b 1
)
echo [+] Images pulled
echo.

echo [5/6] Starting Dokus services...
docker compose up -d
if %ERRORLEVEL% NEQ 0 (
    echo [X] Failed to start services
    pause
    exit /b 1
)
echo [+] Services started
echo.

echo [6/6] Waiting for services to be ready...
timeout /t 10 /nobreak >nul

echo.
echo ====================================================================
echo [+] Dokus Server is running!
echo ====================================================================
echo.
call :PRINT_SERVICE_INFO
echo.
pause
goto SHOW_MENU

:START_SERVICES
cls
echo.
echo ====================================================================
echo   Starting Dokus Cloud Services
echo ====================================================================
echo.

if not exist ".env" (
    echo [!] .env file not found!
    echo Please run Initial Setup first (Option 1)
    pause
    goto SHOW_MENU
)

echo [*] Pulling latest images...
docker compose pull -q

echo [*] Starting services...
docker compose up -d

if %ERRORLEVEL% EQU 0 (
    echo [+] Services started successfully
    echo.
    echo [*] Waiting for services to be ready...
    timeout /t 10 /nobreak >nul
    echo.
    call :PRINT_SERVICE_INFO
) else (
    echo [X] Failed to start services
)
echo.
pause
goto SHOW_MENU

:STOP_SERVICES
cls
echo.
echo ====================================================================
echo   Stopping Services
echo ====================================================================
echo.
docker compose down
echo [+] All services stopped
echo.
pause
goto SHOW_MENU

:RESTART_SERVICES
cls
echo.
echo ====================================================================
echo   Restarting Services
echo ====================================================================
echo.
echo [*] Stopping services...
docker compose down
echo.
echo [*] Starting services...
docker compose up -d
echo [+] Services restarted
echo.
pause
goto SHOW_MENU

:SHOW_STATUS
cls
echo.
echo ====================================================================
echo   Service Status Dashboard
echo ====================================================================
echo.
docker compose ps
echo.
pause
goto SHOW_MENU

:SHOW_LOGS
cls
echo.
echo ====================================================================
echo   Service Logs
echo ====================================================================
echo.
echo Press Ctrl+C to exit logs
echo.
timeout /t 2 /nobreak >nul
docker compose logs -f
goto SHOW_MENU

:ACCESS_DB
cls
echo.
echo ====================================================================
echo   Database CLI Access
echo ====================================================================
echo.
echo   Select database to access:
echo.
echo   1  Auth (dokus_auth) - localhost:15441
echo   2  Cashflow (dokus_cashflow) - localhost:15442
echo   3  Payment (dokus_payment) - localhost:15443
echo   4  Banking (dokus_banking) - localhost:15446
echo   5  Contacts (dokus_contacts) - localhost:15447
echo.
echo   0  Cancel
echo.
set /p db_choice="   Enter choice [0-5]: "

if "%db_choice%"=="1" docker compose exec postgres-auth psql -U dokus -d dokus_auth
if "%db_choice%"=="2" docker compose exec postgres-cashflow psql -U dokus -d dokus_cashflow
if "%db_choice%"=="3" docker compose exec postgres-payment psql -U dokus -d dokus_payment
if "%db_choice%"=="4" docker compose exec postgres-banking psql -U dokus -d dokus_banking
if "%db_choice%"=="5" docker compose exec postgres-contacts psql -U dokus -d dokus_contacts
if "%db_choice%"=="0" goto SHOW_MENU

echo.
pause
goto SHOW_MENU

:SHOW_MOBILE_CONNECTION
cls
echo.
echo ======================================================================
echo   Mobile App Connection
echo ======================================================================
echo.

REM Get local IP address
for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /c:"IPv4"') do (
    set "LOCAL_IP=%%a"
    goto :GOT_IP
)
:GOT_IP
REM Remove leading spaces from IP
for /f "tokens=* delims= " %%a in ("%LOCAL_IP%") do set "LOCAL_IP=%%a"

set "CONNECT_URL=dokus://connect?host=%LOCAL_IP%&port=8000&protocol=http"

echo   Server Connection Details
echo   -------------------------
echo.
echo   Manual Entry:
echo   -------------
echo   Protocol:  http
echo   Host:      %LOCAL_IP%
echo   Port:      8000
echo.
echo   Deep Link URL:
echo   %CONNECT_URL%
echo.
echo   [!] For QR code generation, copy the URL above to:
echo       https://www.qr-code-generator.com/
echo.
echo   In the Dokus app, tap 'Connect to Server' and scan the QR code
echo   or enter the connection details manually.
echo.
pause
goto SHOW_MENU

:PRINT_SERVICE_INFO
echo Services available at:
echo   Gateway:           http://localhost:8000
echo   Auth Service:      http://localhost:8000/api/v1/identity
echo   Cashflow Service:  http://localhost:8000/api/v1/invoices
echo   Payment Service:   http://localhost:8000/api/v1/payments
echo   Banking Service:   http://localhost:8000/api/v1/banking
echo   Contacts Service:  http://localhost:8000/api/v1/contacts
echo.
echo   RabbitMQ UI:       http://localhost:25673
echo   Traefik Dashboard: http://localhost:8080
echo.
echo Database Connection:
echo   PostgreSQL:  localhost:15432 - dokus
goto :EOF
