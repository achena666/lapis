@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM ============================================================
REM  Lapis - database init script for Windows
REM  Creates lapis_admin_dev / lapis_uaa_dev / lapis_eventlog_dev
REM  and imports the SQL scripts in this folder.
REM
REM  Requires: MySQL 8.x installed and running.
REM  Tip: if Python is available, prefer init-lapis-databases.py
REM       (better output and supports --migrate / --dry-run).
REM ============================================================

REM ---------------- config (edit if needed) ----------------
set "MYSQL_BIN="
set "DB_HOST=127.0.0.1"
set "DB_PORT=3306"
set "DB_USER=root"
set "DB_PASS=123456"
set "MIGRATE_OLD=0"

if defined MYSQL_BIN (
    set "MYSQL=%MYSQL_BIN%\mysql.exe"
    set "MYSQLDUMP=%MYSQL_BIN%\mysqldump.exe"
) else (
    set "MYSQL=mysql"
    set "MYSQLDUMP=mysqldump"
)

set "CONN=-h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS%"

echo.
echo [1/5] Checking MySQL connection ...
"%MYSQL%" %CONN% -N -e "SELECT VERSION();" 2>nul
if errorlevel 1 (
    echo     FAILED. Please check:
    echo       1. MySQL service is running
    echo       2. Credentials are correct ^(current user: %DB_USER%^)
    echo       3. mysql.exe is in PATH, or set MYSQL_BIN at the top of this file
    goto :finish
)

echo.
echo [2/5] Creating databases ...
for %%D in (lapis_admin_dev lapis_uaa_dev lapis_eventlog_dev) do (
    "%MYSQL%" %CONN% -e "CREATE DATABASE IF NOT EXISTS %%D DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;" 2>nul
    if errorlevel 1 (
        echo     FAILED to create %%D
        goto :finish
    )
    echo     %%D created
)

if "%MIGRATE_OLD%"=="1" goto :migrate

echo.
echo [3/5] Importing SQL scripts ...
"%MYSQL%" %CONN% lapis_admin_dev < "%~dp0lapis_admin_dev.sql" 2>nul
if errorlevel 1 (echo     FAILED: lapis_admin_dev.sql & goto :finish)
echo     lapis_admin_dev.sql imported

"%MYSQL%" %CONN% lapis_uaa_dev < "%~dp0lapis_uaa_dev.sql" 2>nul
if errorlevel 1 (echo     FAILED: lapis_uaa_dev.sql & goto :finish)
echo     lapis_uaa_dev.sql imported

"%MYSQL%" %CONN% lapis_eventlog_dev < "%~dp0lapis_eventlog_dev.sql" 2>nul
if errorlevel 1 (echo     FAILED: lapis_eventlog_dev.sql & goto :finish)
echo     lapis_eventlog_dev.sql imported
goto :verify

:migrate
echo.
echo [3/5] Migrating data from old hauyne_*_dev databases ...
for %%P in ("lapis_admin_dev hauyne_admin_dev" "lapis_uaa_dev hauyne_uaa_dev" "lapis_eventlog_dev hauyne_eventlog_dev") do (
    for /f "tokens=1,2" %%A in (%%P) do (
        for /f "usebackq tokens=*" %%E in (`"%MYSQL%" %CONN% -N -e "SELECT SCHEMA_NAME FROM information_schema.schemata WHERE SCHEMA_NAME='%%B';" 2^>nul`) do set "OLDDB=%%E"
        if defined OLDDB (
            echo     %%B -^> %%A
            "%MYSQLDUMP%" %CONN% --single-transaction --routines --triggers %%B 2>nul | "%MYSQL%" %CONN% %%A 2>nul
        ) else (
            echo     skip %%A ^(old database %%B not found^)
        )
        set "OLDDB="
    )
)

:verify
echo.
echo [4/5] Verifying table counts ...
for %%D in (lapis_admin_dev lapis_uaa_dev lapis_eventlog_dev) do (
    for /f "usebackq tokens=*" %%N in (`"%MYSQL%" %CONN% -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='%%D';" 2^>nul`) do echo     %%D : %%N tables
)

echo.
echo [5/5] Verifying new authority records ...
for /f "usebackq tokens=*" %%N in (`"%MYSQL%" %CONN% -N -e "SELECT COUNT(*) FROM lapis_admin_dev.hyn_sys_authority WHERE authority_code IN ('sys-user:add','sys-user:update','sys-user:delete','sys-user:reset-password','sys-login-history:delete','sys-tool:generate-ddl');" 2^>nul`) do echo     authorities found: %%N (expected 6)

:finish
echo.
echo Done.
pause
