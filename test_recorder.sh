#!/bin/bash

echo "Testing Postman Collection Recorder"
echo "===================================="
echo ""

# Start recording
echo "1. Starting recording session..."
curl -s http://localhost:8080/newtest | jq .
echo ""
sleep 1

# Make some test requests
echo "2. Making test requests..."
echo "   - POST /api/session (set testAttr=100)"
curl -s -c /tmp/test-cookies.txt -X POST -H "Content-Type: application/json" \
  -d '{"testAttr": 100}' http://localhost:8080/api/session | jq .
echo ""
sleep 1

echo "   - GET /api/session"
curl -s -b /tmp/test-cookies.txt http://localhost:8080/api/session | jq .
echo ""
sleep 1

echo "   - POST /api/session (set testAttr=200)"
curl -s -b /tmp/test-cookies.txt -X POST -H "Content-Type: application/json" \
  -d '{"testAttr": 200}' http://localhost:8080/api/session | jq .
echo ""
sleep 1

echo "   - GET /api/session"
curl -s -b /tmp/test-cookies.txt http://localhost:8080/api/session | jq .
echo ""
sleep 1

# Stop recording
echo "3. Stopping recording and saving collection..."
curl -s http://localhost:8080/endtest | jq .
echo ""

echo "===================================="
echo "Test complete! Check the generated collection file."
