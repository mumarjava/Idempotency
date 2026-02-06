package com.example.payment.exception;

/**
 * Exception thrown when an idempotency key is reused with a different request.
 */
public class IdempotencyConflictException extends RuntimeException {
    
    public IdempotencyConflictException(String message) {
        super(message);
    }
    
    public IdempotencyConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
