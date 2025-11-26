#!/bin/bash
# Helper script to view Redis session data as JSON
# Usage: ./view_session.sh <session-id>

if [ -z "$1" ]; then
    echo "Usage: $0 <session-id>"
    echo ""
    echo "Example:"
    echo "  $0 67cb4689-5e53-4c1d-aaac-f9939d3b78be"
    echo ""
    echo "To list all session keys:"
    echo "  docker exec procsee-redis-1 redis-cli KEYS 'spring:session:sessions:*'"
    exit 1
fi

SESSION_ID="$1"
REDIS_KEY="spring:session:sessions:${SESSION_ID}"

echo "Fetching session data for: $SESSION_ID"
echo "Redis key: $REDIS_KEY"
echo ""

python3 redis_to_json.py "$REDIS_KEY" --host localhost --port 6379
