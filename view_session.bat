@echo off
REM Helper script to view Redis session data as JSON (Windows version)
REM Usage: view_session.bat <session-id>

if "%1"=="" (
    echo Usage: %0 ^<session-id^>
    echo.
    echo Example:
    echo   %0 67cb4689-5e53-4c1d-aaac-f9939d3b78be
    echo.
    echo To list all session keys:
    echo   docker exec procsee-redis-1 redis-cli KEYS "spring:session:sessions:*"
    exit /b 1
)

set SESSION_ID=%1
set REDIS_KEY=spring:session:sessions:%SESSION_ID%

echo Fetching session data for: %SESSION_ID%
echo Redis key: %REDIS_KEY%
echo.

python redis_to_json.py "%REDIS_KEY%" --host localhost --port 6379
