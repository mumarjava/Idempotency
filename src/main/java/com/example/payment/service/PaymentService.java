package com.example.payment.service;

import com.example.payment.exception.IdempotencyConflictException;
import com.example.payment.model.ChargeRequest;
import com.example.payment.model.ChargeResponse;
import com.example.payment.model.IdempotencyRecord;
import com.example.payment.repository.IdempotencyStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Payment service with idempotency support.
 * 
 * Idempotency guarantees:
 * 1. Same key + same request = same response (no duplicate charge)
 * 2. Same key + different request = 409 Conflict
 * 3. Missing key = 400 Bad Request (handled in controller)
 * 4. Expired key = process as new request
 */
@Service
public class PaymentService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    
    private final IdempotencyStore idempotencyStore;
    private final Duration idempotencyKeyTTL;
    
    public PaymentService(
            IdempotencyStore idempotencyStore,
            @Value("${payment.idempotency.ttl:PT1H}") Duration idempotencyKeyTTL) {
        this.idempotencyStore = idempotencyStore;
        this.idempotencyKeyTTL = idempotencyKeyTTL;
        logger.info("PaymentService initialized with TTL: {}", idempotencyKeyTTL);
    }
    
    /**
     * Process a payment charge with idempotency.
     *
     * @param idempotencyKey unique key for this request
     * @param request        the charge request
     * @return the charge response
     * @throws IdempotencyConflictException if key exists with different request
     */
    public ChargeResponse charge(String idempotencyKey, ChargeRequest request) {
        logger.info("Processing charge with idempotency key: {}, customer: {}, amount: {}",
                idempotencyKey, request.getCustomerId(), request.getAmount());
        
        // Check for existing idempotency record
        Optional<IdempotencyRecord> existingRecord = idempotencyStore.findByKey(idempotencyKey);
        
        if (existingRecord.isPresent()) {
            IdempotencyRecord record = existingRecord.get();
            
            // Check if request matches
            if (record.requestMatches(request)) {
                logger.info("Returning cached response for idempotency key: {}", idempotencyKey);
                return record.getResponse();
            } else {
                logger.warn("Idempotency key {} used with different request. " +
                        "Original: {}, New: {}", idempotencyKey, record.getRequest(), request);
                throw new IdempotencyConflictException(
                        "Idempotency key already used with different request parameters"
                );
            }
        }
        
        // Process new payment
        logger.info("Processing new payment for customer: {}", request.getCustomerId());
        ChargeResponse response = processPayment(request);
        
        // Store idempotency record
        Instant expiresAt = Instant.now().plus(idempotencyKeyTTL);
        IdempotencyRecord record = new IdempotencyRecord(
                idempotencyKey,
                request,
                response,
                expiresAt
        );
        
        idempotencyStore.save(record);
        logger.info("Saved idempotency record for key: {}, expires at: {}", 
                idempotencyKey, expiresAt);
        
        return response;
    }
    
    /**
     * Simulate actual payment processing.
     * In production, this would integrate with payment gateway (Stripe, PayPal, etc.)
     */
    private ChargeResponse processPayment(ChargeRequest request) {
        try {
            // Simulate payment gateway processing time
            Thread.sleep(100);
            
            // Generate unique transaction ID
            String transactionId = "txn_" + UUID.randomUUID().toString().replace("-", "");
            
            logger.info("Payment processed successfully. Transaction ID: {}, Amount: {} {}",
                    transactionId, request.getAmount(), request.getCurrency());
            
            return ChargeResponse.success(
                    transactionId,
                    request.getAmount(),
                    request.getCurrency()
            );
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Payment processing interrupted", e);
            return ChargeResponse.failed("Payment processing failed: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Payment processing error", e);
            return ChargeResponse.failed("Payment processing failed: " + e.getMessage());
        }
    }
}
