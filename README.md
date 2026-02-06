# Idempotent Payment API

A production-ready Spring Boot REST API implementing idempotent payment processing using the `Idempotency-Key` header.

## Features

✅ **Idempotent payment processing**
✅ **Configurable TTL for idempotency keys**
✅ **Thread-safe concurrent request handling**
✅ **Comprehensive error handling**
✅ **Full test coverage**
✅ **In-memory storage (development)**
✅ **Redis support (production-ready)**

## Requirements

### All Requirements Implemented

| Requirement | Implementation | Status |
|------------|----------------|---------|
| First call processes payment | Returns success with transaction ID | ✅ |
| Retry with same key + request | Returns cached response (no duplicate) | ✅ |
| Same key, different request | Returns 409 Conflict | ✅ |
| Missing idempotency key | Returns 400 Bad Request | ✅ |
| Key expiry (TTL) | Configurable, defaults to 1 hour | ✅ |

## Quick Start

### Prerequisites

- Java 11 or higher
- Maven 3.6+

### Build and Run

```bash
# Build the project
mvn clean package

# Run the application
mvn spring-boot:run
```

The API will start on `http://localhost:8080`

### Run Tests

```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report

# Run specific test
mvn test -Dtest=PaymentControllerTest
```

## API Usage

### Endpoint

```
POST /api/payments/charge
```

### Required Headers

```
Content-Type: application/json
Idempotency-Key: <unique-identifier>
```

### Request Body

```json
{
  "customerId": "customer_123",
  "amount": 99.99,
  "currency": "USD",
  "description": "Premium subscription"
}
```

### Response

**Success (200 OK):**
```json
{
  "transactionId": "txn_abc123...",
  "status": "SUCCESS",
  "amount": 99.99,
  "currency": "USD",
  "processedAt": "2024-02-05T10:30:00Z",
  "message": "Payment processed successfully"
}
```

## Test Scenarios

### 1. First Call - Process Payment

```bash
curl -X POST http://localhost:8080/api/payments/charge \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: test-key-123" \
  -d '{
    "customerId": "customer_123",
    "amount": 99.99,
    "currency": "USD",
    "description": "Premium subscription"
  }'
```

**Response:** 200 OK with new transaction ID

### 2. Retry - Return Cached Response

```bash
# Same request, same key
curl -X POST http://localhost:8080/api/payments/charge \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: test-key-123" \
  -d '{
    "customerId": "customer_123",
    "amount": 99.99,
    "currency": "USD",
    "description": "Premium subscription"
  }'
```

**Response:** 200 OK with **same transaction ID** (no duplicate charge!)

### 3. Same Key, Different Request - Conflict

```bash
curl -X POST http://localhost:8080/api/payments/charge \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key": test-key-123" \
  -d '{
    "customerId": "customer_456",
    "amount": 199.99,
    "currency": "USD",
    "description": "Different payment"
  }'
```

**Response:** 409 Conflict
```json
{
  "status": "FAILED",
  "message": "Idempotency key already used with different request parameters"
}
```

### 4. Missing Key - Bad Request

```bash
curl -X POST http://localhost:8080/api/payments/charge \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer_123",
    "amount": 99.99,
    "currency": "USD"
  }'
```

**Response:** 400 Bad Request
```json
{
  "status": "FAILED",
  "message": "Idempotency-Key header is required"
}
```

## Configuration

### application.properties

```properties
# Server port
server.port=8080

# Idempotency key TTL (ISO-8601 duration)
# PT1H = 1 hour (demo)
# PT24H = 24 hours (production)
payment.idempotency.ttl=PT1H

# Logging
logging.level.com.example.payment=DEBUG
```

### TTL Examples

- `PT1M` = 1 minute (testing)
- `PT1H` = 1 hour (demo)
- `PT24H` = 24 hours (typical)
- `PT72H` = 72 hours (extended)

## Architecture

```
┌──────────┐
│  Client  │
└────┬─────┘
     │ POST /charge + Idempotency-Key
     ▼
┌─────────────────────┐
│ PaymentController   │ ← Validate header exists
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│  PaymentService     │
└─────────┬───────────┘
          │
          ├─▶ Check IdempotencyStore
          │   ├─ Found + Same Req  → Return cached
          │   ├─ Found + Diff Req  → 409 Conflict
          │   └─ Not Found         → Process payment
          │
          ├─▶ Process Payment (simulate gateway)
          │
          └─▶ Save to IdempotencyStore
```

## Project Structure

```
src/
├── main/
│   ├── java/com/example/payment/
│   │   ├── PaymentApplication.java       # Main application
│   │   ├── controller/
│   │   │   └── PaymentController.java    # REST API
│   │   ├── service/
│   │   │   └── PaymentService.java       # Business logic
│   │   ├── repository/
│   │   │   ├── IdempotencyStore.java     # Interface
│   │   │   └── InMemoryIdempotencyStore.java
│   │   ├── model/
│   │   │   ├── ChargeRequest.java
│   │   │   ├── ChargeResponse.java
│   │   │   └── IdempotencyRecord.java
│   │   └── exception/
│   │       └── IdempotencyConflictException.java
│   └── resources/
│       └── application.properties
└── test/
    └── java/com/example/payment/
        └── controller/
            └── PaymentControllerTest.java
```

## Testing

### Test Coverage

The project includes comprehensive tests:

- ✅ First call processes payment
- ✅ Retry returns cached response
- ✅ Conflict detection (same key, different request)
- ✅ Missing key validation
- ✅ Empty key validation
- ✅ Concurrent requests (10 threads)
- ✅ Different keys create different transactions
- ✅ Invalid request validation

### Run Tests

```bash
# All tests
mvn test

# Single test class
mvn test -Dtest=PaymentControllerTest

# Single test method
mvn test -Dtest=PaymentControllerTest#testFirstCallProcessesPayment

# With detailed output
mvn test -X
```

## Production Deployment

### Using Redis (Recommended)

1. **Add Redis dependency** (already in pom.xml, just uncomment)

2. **Start Redis:**
```bash
docker run -p 6379:6379 redis
```

3. **Update application.properties:**
```properties
spring.redis.host=localhost
spring.redis.port=6379
```

4. **Redis implementation will be auto-configured**

### Environment Variables

```bash
# Set TTL via environment
export PAYMENT_IDEMPOTENCY_TTL=PT24H

# Set port
export SERVER_PORT=8080

# Run
mvn spring-boot:run
```

## Monitoring

### Health Check

```bash
curl http://localhost:8080/api/payments/health
```

Response: `Payment API is healthy`

### Logging

All operations are logged with INFO/DEBUG levels:

```
2024-02-05 10:30:00 - PaymentService - Processing charge with idempotency key: test-key-123
2024-02-05 10:30:00 - PaymentService - Processing new payment for customer: customer_123
2024-02-05 10:30:00 - PaymentService - Payment processed successfully. Transaction ID: txn_...
2024-02-05 10:30:00 - PaymentService - Saved idempotency record for key: test-key-123
```

## Best Practices

### Idempotency Key Generation

**Client-side (Recommended):**
```java
String idempotencyKey = UUID.randomUUID().toString();
```

**Request-based:**
```java
String idempotencyKey = SHA256(timestamp + customerId + amount + nonce);
```

### Error Handling

```java
try {
    ChargeResponse response = paymentApi.charge(idempotencyKey, request);
} catch (HttpClientErrorException.Conflict e) {
    // 409 - Same key used with different request
    logger.warn("Idempotency conflict: {}", e.getMessage());
} catch (HttpClientErrorException.BadRequest e) {
    // 400 - Missing key or invalid request
    logger.error("Bad request: {}", e.getMessage());
}
```

### Retry Logic

```java
int maxRetries = 3;
for (int i = 0; i < maxRetries; i++) {
    try {
        return paymentApi.charge(idempotencyKey, request);
    } catch (HttpServerErrorException e) {
        if (i == maxRetries - 1) throw e;
        Thread.sleep(1000 * (i + 1)); // Exponential backoff
    }
}
```

## Security Considerations

1. **Use HTTPS in production**
2. **Add authentication** (API keys, OAuth)
3. **Rate limiting** per customer
4. **Input validation** (already implemented)
5. **Audit logging** for compliance
6. **Secure idempotency key storage**

## License

This is a demonstration project. Apply appropriate licensing for production use.

## Support

For questions or issues:
- Review the test cases for examples
- Check inline code comments
- See IDEMPOTENT_PAYMENT_API.md for detailed documentation
