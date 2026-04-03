#!/bin/bash

set -e

# Start services with docker compose
echo "Starting services with docker compose..."
docker compose up -d --build --wait

# Wait for app to be healthy (max 60 seconds)
echo "Waiting for app to be healthy..."
max_attempts=60
attempt=0
while [ $attempt -lt $max_attempts ]; do
  if curl -s http://localhost:8080/api/calculate?a=1&b=1 > /dev/null 2>&1; then
    echo "App is healthy"
    break
  fi
  attempt=$((attempt + 1))
  sleep 1
done

if [ $attempt -eq $max_attempts ]; then
  echo "ERROR: App failed to become healthy within 60 seconds"
  docker compose down -v
  exit 1
fi

# Test 1: Login and extract JWT token
echo "Test 1: Login with testuser/password..."
login_response=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password"}')

http_code=$(curl -s -o /dev/null -w '%{http_code}' -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password"}')

if [ "$http_code" != "200" ]; then
  echo "ERROR: Login failed with HTTP $http_code"
  echo "Response: $login_response"
  docker compose down -v
  exit 1
fi

token=$(echo "$login_response" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
if [ -z "$token" ]; then
  echo "ERROR: Failed to extract JWT token from login response"
  echo "Response: $login_response"
  docker compose down -v
  exit 1
fi
echo "Login successful, token: ${token:0:20}..."

# Test 2: Calculate with Bearer token
echo "Test 2: Calculate 7 + 3 with Bearer token..."
calc_response=$(curl -s -X GET 'http://localhost:8080/api/calculate?a=7&b=3&op=add' \
  -H "Authorization: Bearer $token")

http_code=$(curl -s -o /dev/null -w '%{http_code}' -X GET 'http://localhost:8080/api/calculate?a=7&b=3&op=add' \
  -H "Authorization: Bearer $token")

if [ "$http_code" != "200" ]; then
  echo "ERROR: Calculate failed with HTTP $http_code"
  echo "Response: $calc_response"
  docker compose down -v
  exit 1
fi

result=$(echo "$calc_response" | grep -o '"result":[0-9]*' | cut -d':' -f2)
if [ "$result" != "10" ]; then
  echo "ERROR: Expected result=10, got result=$result"
  echo "Response: $calc_response"
  docker compose down -v
  exit 1
fi
echo "Calculate successful, result: $result"

# Test 3: Verify UI is accessible
echo "Test 3: Verify UI is accessible at http://localhost:8080/..."
ui_response=$(curl -s http://localhost:8080/)

http_code=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/)

if [ "$http_code" != "200" ]; then
  echo "ERROR: UI request failed with HTTP $http_code"
  docker compose down -v
  exit 1
fi

if ! echo "$ui_response" | grep -q '</html>'; then
  echo "ERROR: UI response does not contain '</html>'"
  echo "Response: ${ui_response:0:200}..."
  docker compose down -v
  exit 1
fi
echo "UI is accessible and returns HTML"

# Cleanup
echo "Cleaning up..."
docker compose down -v

echo "All tests passed!"
exit 0
