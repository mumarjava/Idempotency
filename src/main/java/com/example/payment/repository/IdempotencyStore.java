package com.example.payment.repository;

import com.example.payment.model.IdempotencyRecord;

import java.util.Optional;

/**
 * Repository interface for storing and retrieving idempotency records.
 */
public interface IdempotencyStore {
    
    /**
     * Save an idempotency record.
     *
     * @param record the record to save
     */
    void save(IdempotencyRecord record);
    
    /**
     * Find an idempotency record by key.
     *
     * @param idempotencyKey the idempotency key
     * @return the record if found and not expired
     */
    Optional<IdempotencyRecord> findByKey(String idempotencyKey);
    
    /**
     * Delete an idempotency record.
     *
     * @param idempotencyKey the idempotency key
     */
    void delete(String idempotencyKey);
    
    /**
     * Clean up expired records.
     */
    void cleanupExpired();
}
