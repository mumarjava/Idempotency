package com.example.payment.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Payment charge response model.
 */
public class ChargeResponse {
    
    private String transactionId;
    private String status;
    private BigDecimal amount;
    private String currency;
    private Instant processedAt;
    private String message;
    
    public ChargeResponse() {
    }
    
    @JsonCreator
    public ChargeResponse(
            @JsonProperty("transactionId") String transactionId,
            @JsonProperty("status") String status,
            @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("currency") String currency,
            @JsonProperty("processedAt") Instant processedAt,
            @JsonProperty("message") String message) {
        this.transactionId = transactionId;
        this.status = status;
        this.amount = amount;
        this.currency = currency;
        this.processedAt = processedAt;
        this.message = message;
    }
    
    // Factory methods
    public static ChargeResponse success(String transactionId, BigDecimal amount, String currency) {
        return new ChargeResponse(
                transactionId,
                "SUCCESS",
                amount,
                currency,
                Instant.now(),
                "Payment processed successfully"
        );
    }
    
    public static ChargeResponse failed(String message) {
        return new ChargeResponse(
                null,
                "FAILED",
                null,
                null,
                Instant.now(),
                message
        );
    }
    
    // Getters and Setters
    public String getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public Instant getProcessedAt() {
        return processedAt;
    }
    
    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
