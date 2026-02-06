package com.example.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Idempotent Payment API Application.
 * 
 * Features:
 * - POST /api/payments/charge with Idempotency-Key header
 * - In-memory idempotency store (development)
 * - Configurable TTL for idempotency keys
 * 
 * Run with:
 * mvn spring-boot:run
 * 
 * Test with:
 * curl -X POST http://localhost:8080/api/payments/charge \
 *   -H "Content-Type: application/json" \
 *   -H "Idempotency-Key: test-key-123" \
 *   -d '{"customerId":"cust_123","amount":99.99,"currency":"USD","description":"Test"}'
 */
@SpringBootApplication
public class PaymentApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(PaymentApplication.class, args);
    }
}
