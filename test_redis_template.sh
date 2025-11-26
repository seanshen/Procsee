#!/bin/bash
# Test script for RedisTemplate endpoints in java-session-demo
# This demonstrates various Redis operations using the RedisController

BASE_URL="http://localhost:8081/api/redis"

echo "========================================="
echo "Testing RedisTemplate Operations"
echo "========================================="
echo ""

# Test 1: Write and read a string
echo "1. Testing String Operations"
echo "   Writing string..."
curl -s -X POST "$BASE_URL/string" \
  -H "Content-Type: application/json" \
  -d '{"key": "test:string", "value": "Hello Redis!", "ttl": 300}' | jq '.'

echo "   Reading string..."
curl -s "$BASE_URL/string/test:string" | jq '.'
echo ""

# Test 2: Write and read a hash
echo "2. Testing Hash Operations"
echo "   Writing hash..."
curl -s -X POST "$BASE_URL/hash" \
  -H "Content-Type: application/json" \
  -d '{"key": "user:1001", "data": {"name": "Alice", "age": 30, "city": "New York", "role": "developer"}}' | jq '.'

echo "   Reading hash..."
curl -s "$BASE_URL/hash/user:1001" | jq '.'
echo ""

# Test 3: Write and read a list
echo "3. Testing List Operations"
echo "   Writing list..."
curl -s -X POST "$BASE_URL/list" \
  -H "Content-Type: application/json" \
  -d '{"key": "tasks:today", "values": ["Write code", "Review PRs", "Deploy to prod", "Update docs"]}' | jq '.'

echo "   Reading list..."
curl -s "$BASE_URL/list/tasks:today" | jq '.'
echo ""

# Test 4: Write and read a complex object
echo "4. Testing Complex Object Operations"
echo "   Writing object..."
curl -s -X POST "$BASE_URL/object" \
  -H "Content-Type: application/json" \
  -d '{"key": "person:2001", "data": {"name": "Bob", "age": 25, "hobbies": ["reading", "coding", "gaming"], "address": {"street": "123 Main St", "city": "Boston"}}}' | jq '.'

echo "   Reading object..."
curl -s "$BASE_URL/object/person:2001" | jq '.'
echo ""

# Test 5: Increment counter
echo "5. Testing Counter Operations"
echo "   Incrementing counter (first time)..."
curl -s -X POST "$BASE_URL/increment" \
  -H "Content-Type: application/json" \
  -d '{"key": "page:views", "delta": 1}' | jq '.'

echo "   Incrementing counter (second time)..."
curl -s -X POST "$BASE_URL/increment" \
  -H "Content-Type: application/json" \
  -d '{"key": "page:views", "delta": 5}' | jq '.'

echo "   Reading counter value..."
curl -s "$BASE_URL/string/page:views" | jq '.'
echo ""

# Test 6: Get keys by pattern
echo "6. Testing Key Pattern Matching"
echo "   Getting all keys matching 'user:*'..."
curl -s "$BASE_URL/keys?pattern=user:*" | jq '.'

echo "   Getting all keys matching 'test:*'..."
curl -s "$BASE_URL/keys?pattern=test:*" | jq '.'
echo ""

# Test 7: Delete a key
echo "7. Testing Key Deletion"
echo "   Deleting 'test:string'..."
curl -s -X DELETE "$BASE_URL/test:string" | jq '.'

echo "   Verifying deletion..."
curl -s "$BASE_URL/string/test:string" | jq '.'
echo ""

echo "========================================="
echo "Testing Complete!"
echo "========================================="
echo ""
echo "You can now use the Python redis_to_json.py script to view the data:"
echo "  python3 redis_to_json.py user:1001"
echo "  python3 redis_to_json.py person:2001"
echo "  python3 redis_to_json.py tasks:today"
echo ""
