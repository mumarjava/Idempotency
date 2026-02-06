package com.example.payment.controller;

import com.example.payment.exception.IdempotencyConflictException;
import com.example.payment.model.ChargeRequest;
import com.example.payment.model.ChargeResponse;
import com.example.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * Payment API controller with idempotency support.
 * 
 * The Idempotency-Key header is required for all charge requests.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    
    private final PaymentService paymentService;
    
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    
    /**
     * POST /api/payments/charge
     * 
     * Process a payment charge with idempotency.
     * 
     * Required Headers:
     * - Idempotency-Key: Unique identifier for this request
     * 
     * Request Body:
     * {
     *   "customerId": "customer_123",
     *   "amount": 99.99,
     *   "currency": "USD",
     *   "description": "Premium subscription"
     * }
     * 
     * Responses:
     * - 200 OK: Payment processed successfully (or cached response)
     * - 400 Bad Request: Missing Idempotency-Key header
     * - 409 Conflict: Same key with different request
     * - 500 Internal Server Error: Processing error
     *
     * @param idempotencyKey unique key from header (required)
     * @param request        the charge request
     * @return the charge response
     */
    @PostMapping("/charge")
    public ResponseEntity<ChargeResponse> charge(
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody ChargeRequest request) {
        
        // Validate idempotency key is present
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            logger.warn("Charge request missing Idempotency-Key header");
            return ResponseEntity
                    .badRequest()
                    .body(ChargeResponse.failed("Idempotency-Key header is required"));
        }
        
        logger.info("Received charge request - Key: {}, Customer: {}, Amount: {} {}",
                idempotencyKey, request.getCustomerId(), request.getAmount(), request.getCurrency());
        
        try {
            ChargeResponse response = paymentService.charge(idempotencyKey, request);
            return ResponseEntity.ok(response);
            
        } catch (IdempotencyConflictException e) {
            logger.warn("Idempotency conflict for key: {} - {}", idempotencyKey, e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ChargeResponse.failed(e.getMessage()));
                    
        } catch (Exception e) {
            logger.error("Unexpected error processing charge for key: " + idempotencyKey, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ChargeResponse.failed("Internal server error"));
        }
    }
    
    /**
     * GET /api/payments/health
     * 
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Payment API is healthy");
    }
}
