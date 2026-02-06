package com.example.payment.model;

import java.time.Instant;

/**
 * Idempotency record stored in cache/database.
 */
public class IdempotencyRecord {
    
    private String idempotencyKey;
    private ChargeRequest request;
    private ChargeResponse response;
    private Instant createdAt;
    private Instant expiresAt;
    
    public IdempotencyRecord() {
    }
    
    public IdempotencyRecord(String idempotencyKey, ChargeRequest request,
                           ChargeResponse response, Instant expiresAt) {
        this.idempotencyKey = idempotencyKey;
        this.request = request;
        this.response = response;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
    }
    
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
    
    public boolean requestMatches(ChargeRequest otherRequest) {
        return this.request.equals(otherRequest);
    }
    
    // Getters and Setters
    public String getIdempotencyKey() {
        return idempotencyKey;
    }
    
    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
    
    public ChargeRequest getRequest() {
        return request;
    }
    
    public void setRequest(ChargeRequest request) {
        this.request = request;
    }
    
    public ChargeResponse getResponse() {
        return response;
    }
    
    public void setResponse(ChargeResponse response) {
        this.response = response;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
