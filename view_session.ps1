# Helper script to view Redis session data as JSON (PowerShell version)
# Usage: .\view_session.ps1 <session-id>

param(
    [Parameter(Mandatory=$false)]
    [string]$SessionId
)

if (-not $SessionId) {
    Write-Host "Usage: .\view_session.ps1 <session-id>"
    Write-Host ""
    Write-Host "Example:"
    Write-Host "  .\view_session.ps1 67cb4689-5e53-4c1d-aaac-f9939d3b78be"
    Write-Host ""
    Write-Host "To list all session keys:"
    Write-Host '  docker exec procsee-redis-1 redis-cli KEYS "spring:session:sessions:*"'
    exit 1
}

$RedisKey = "spring:session:sessions:$SessionId"

Write-Host "Fetching session data for: $SessionId"
Write-Host "Redis key: $RedisKey"
Write-Host ""

python redis_to_json.py "$RedisKey" --host localhost --port 6379
