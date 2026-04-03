#!/bin/bash

# Docker Compose integration test script
# Starts full stack (PostgreSQL + Spring Boot app) and runs end-to-end tests

set -e

echo '=== Starting Docker Compose services ==='
docker compose up -d --build --wait

echo '=== Waiting for app to be healthy ==='
max_attempts=60
attempt=0
while [ $attempt -lt $max_attempts ]; do
  if curl -s http://localhost:8080/api/calculate?a=1&b=1 > /dev/null 2>&1; then
    echo 'App is healthy'
    break
  fi
  attempt=$((attempt + 1))
  echo "Attempt $attempt/$max_attempts: waiting for app..."
  sleep 1
done

if [ $attempt -eq $max_attempts ]; then
  echo 'ERROR: App failed to become healthy within 60 seconds'
  docker compose down -v
  exit 1
fi

echo ''
echo '=== Test 1: Login and get JWT token ==='
login_response=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"testuser","password":"SecurePass123!"}')

echo "Login response: $login_response"
token=$(echo $login_response | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$token" ]; then
  echo 'ERROR: Failed to extract JWT token from login response'
  docker compose down -v
  exit 1
fi

echo "Token: $token"
echo 'Test 1 PASSED: Login successful and token obtained'

echo ''
echo '=== Test 2: Calculate with Bearer token ==='
calc_response=$(curl -s -X GET 'http://localhost:8080/api/calculate?a=7&b=3&op=add' \
  -H "Authorization: Bearer $token")

echo "Calculate response: $calc_response"
result=$(echo $calc_response | grep -o '"result":[0-9]*' | cut -d':' -f2)

if [ "$result" != "10" ]; then
  echo "ERROR: Expected result=10, got result=$result"
  docker compose down -v
  exit 1
fi

echo 'Test 2 PASSED: Calculation successful with correct result'

echo ''
echo '=== Test 3: Verify UI is accessible ==='
ui_response=$(curl -s -w '\n%{http_code}' http://localhost:8080/)
http_code=$(echo "$ui_response" | tail -n1)
html_content=$(echo "$ui_response" | head -n-1)

if [ "$http_code" != "200" ]; then
  echo "ERROR: Expected HTTP 200, got HTTP $http_code"
  docker compose down -v
  exit 1
fi

if ! echo "$html_content" | grep -q '</html>'; then
  echo 'ERROR: Response does not contain </html> tag'
  docker compose down -v
  exit 1
fi

echo 'Test 3 PASSED: UI is accessible and returns HTML'

echo ''
echo '=== All tests passed! Cleaning up ==='
docker compose down -v

echo 'SUCCESS: All integration tests passed'
exit 0
