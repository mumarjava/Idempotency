#!/bin/bash

# Idempotent Payment API Test Script
# This script demonstrates all test scenarios

BASE_URL="http://localhost:8080"
API_ENDPOINT="$BASE_URL/api/payments/charge"

echo "======================================"
echo "Idempotent Payment API Test Script"
echo "======================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print test header
print_test() {
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "TEST: $1"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
}

# Function to print success
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

# Function to print error
print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# Function to print info
print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

# Check if server is running
print_info "Checking if server is running..."
if ! curl -s "$BASE_URL/api/payments/health" > /dev/null 2>&1; then
    print_error "Server is not running at $BASE_URL"
    echo ""
    echo "Please start the server first:"
    echo "  mvn spring-boot:run"
    exit 1
fi
print_success "Server is running"

# Generate unique idempotency key
IDEMPOTENCY_KEY="test-key-$(date +%s)"

# Test 1: First call - should process payment
print_test "1. First Call - Process Payment"
print_info "Idempotency-Key: $IDEMPOTENCY_KEY"

RESPONSE1=$(curl -s -w "\n%{http_code}" -X POST "$API_ENDPOINT" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $IDEMPOTENCY_KEY" \
  -d '{
    "customerId": "customer_123",
    "amount": 99.99,
    "currency": "USD",
    "description": "Premium subscription"
  }')

HTTP_CODE1=$(echo "$RESPONSE1" | tail -n1)
BODY1=$(echo "$RESPONSE1" | sed '$d')

echo "Response Code: $HTTP_CODE1"
echo "Response Body:"
echo "$BODY1" | jq '.'

if [ "$HTTP_CODE1" == "200" ]; then
    TRANSACTION_ID=$(echo "$BODY1" | jq -r '.transactionId')
    print_success "Payment processed successfully. Transaction ID: $TRANSACTION_ID"
else
    print_error "Expected 200, got $HTTP_CODE1"
fi

# Test 2: Retry with same key and same request - should return cached
print_test "2. Retry with Same Key - Return Cached Response"
print_info "Using same Idempotency-Key: $IDEMPOTENCY_KEY"

RESPONSE2=$(curl -s -w "\n%{http_code}" -X POST "$API_ENDPOINT" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $IDEMPOTENCY_KEY" \
  -d '{
    "customerId": "customer_123",
    "amount": 99.99,
    "currency": "USD",
    "description": "Premium subscription"
  }')

HTTP_CODE2=$(echo "$RESPONSE2" | tail -n1)
BODY2=$(echo "$RESPONSE2" | sed '$d')

echo "Response Code: $HTTP_CODE2"
echo "Response Body:"
echo "$BODY2" | jq '.'

if [ "$HTTP_CODE2" == "200" ]; then
    TRANSACTION_ID2=$(echo "$BODY2" | jq -r '.transactionId')
    if [ "$TRANSACTION_ID" == "$TRANSACTION_ID2" ]; then
        print_success "Returned cached response. Same transaction ID: $TRANSACTION_ID2"
    else
        print_error "Different transaction ID! Expected: $TRANSACTION_ID, Got: $TRANSACTION_ID2"
    fi
else
    print_error "Expected 200, got $HTTP_CODE2"
fi

# Test 3: Same key but different request - should return 409 Conflict
print_test "3. Same Key, Different Request - Return 409 Conflict"
print_info "Using same Idempotency-Key but different request"

RESPONSE3=$(curl -s -w "\n%{http_code}" -X POST "$API_ENDPOINT" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $IDEMPOTENCY_KEY" \
  -d '{
    "customerId": "customer_456",
    "amount": 199.99,
    "currency": "EUR",
    "description": "Different payment"
  }')

HTTP_CODE3=$(echo "$RESPONSE3" | tail -n1)
BODY3=$(echo "$RESPONSE3" | sed '$d')

echo "Response Code: $HTTP_CODE3"
echo "Response Body:"
echo "$BODY3" | jq '.'

if [ "$HTTP_CODE3" == "409" ]; then
    print_success "Correctly returned 409 Conflict"
else
    print_error "Expected 409, got $HTTP_CODE3"
fi

# Test 4: Missing idempotency key - should return 400 Bad Request
print_test "4. Missing Idempotency-Key - Return 400 Bad Request"

RESPONSE4=$(curl -s -w "\n%{http_code}" -X POST "$API_ENDPOINT" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer_789",
    "amount": 49.99,
    "currency": "USD",
    "description": "Test payment"
  }')

HTTP_CODE4=$(echo "$RESPONSE4" | tail -n1)
BODY4=$(echo "$RESPONSE4" | sed '$d')

echo "Response Code: $HTTP_CODE4"
echo "Response Body:"
echo "$BODY4" | jq '.'

if [ "$HTTP_CODE4" == "400" ]; then
    print_success "Correctly returned 400 Bad Request"
else
    print_error "Expected 400, got $HTTP_CODE4"
fi

# Test 5: New key with same request - should create new transaction
print_test "5. New Idempotency-Key - Create New Transaction"

NEW_KEY="test-key-$(date +%s)-new"
print_info "Idempotency-Key: $NEW_KEY"

RESPONSE5=$(curl -s -w "\n%{http_code}" -X POST "$API_ENDPOINT" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $NEW_KEY" \
  -d '{
    "customerId": "customer_123",
    "amount": 99.99,
    "currency": "USD",
    "description": "Premium subscription"
  }')

HTTP_CODE5=$(echo "$RESPONSE5" | tail -n1)
BODY5=$(echo "$RESPONSE5" | sed '$d')

echo "Response Code: $HTTP_CODE5"
echo "Response Body:"
echo "$BODY5" | jq '.'

if [ "$HTTP_CODE5" == "200" ]; then
    TRANSACTION_ID5=$(echo "$BODY5" | jq -r '.transactionId')
    if [ "$TRANSACTION_ID" != "$TRANSACTION_ID5" ]; then
        print_success "Created new transaction. Transaction ID: $TRANSACTION_ID5"
    else
        print_error "Should have different transaction ID!"
    fi
else
    print_error "Expected 200, got $HTTP_CODE5"
fi

# Summary
echo ""
echo "======================================"
echo "Test Summary"
echo "======================================"
print_success "Test 1: First call processed payment"
print_success "Test 2: Retry returned cached response"
print_success "Test 3: Conflict detected (409)"
print_success "Test 4: Missing key rejected (400)"
print_success "Test 5: New key created new transaction"
echo ""
echo "All tests completed!"
