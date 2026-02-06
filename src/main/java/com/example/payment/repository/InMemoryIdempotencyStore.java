package com.example.payment.repository;

import com.example.payment.model.IdempotencyRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of IdempotencyStore.
 * Thread-safe using ConcurrentHashMap.
 * 
 * Note: Suitable for development and single-instance deployments.
 * For production distributed systems, use RedisIdempotencyStore.
 */
@Repository
@Primary
@ConditionalOnMissingBean(RedisTemplate.class)
public class InMemoryIdempotencyStore implements IdempotencyStore {
    
    private static final Logger logger = LoggerFactory.getLogger(InMemoryIdempotencyStore.class);
    
    private final Map<String, IdempotencyRecord> store = new ConcurrentHashMap<>();
    
    public InMemoryIdempotencyStore() {
        logger.info("Using InMemoryIdempotencyStore (development mode)");
    }
    
    @Override
    public void save(IdempotencyRecord record) {
        store.put(record.getIdempotencyKey(), record);
        logger.debug("Saved idempotency record for key: {}", record.getIdempotencyKey());
    }
    
    @Override
    public Optional<IdempotencyRecord> findByKey(String idempotencyKey) {
        IdempotencyRecord record = store.get(idempotencyKey);
        
        if (record == null) {
            logger.debug("No idempotency record found for key: {}", idempotencyKey);
            return Optional.empty();
        }
        
        // Check if expired
        if (record.isExpired()) {
            logger.debug("Idempotency record expired for key: {}", idempotencyKey);
            delete(idempotencyKey);
            return Optional.empty();
        }
        
        logger.debug("Found idempotency record for key: {}", idempotencyKey);
        return Optional.of(record);
    }
    
    @Override
    public void delete(String idempotencyKey) {
        store.remove(idempotencyKey);
        logger.debug("Deleted idempotency record for key: {}", idempotencyKey);
    }
    
    @Override
    public void cleanupExpired() {
        Instant now = Instant.now();
        int removed = 0;
        
        for (Map.Entry<String, IdempotencyRecord> entry : store.entrySet()) {
            if (entry.getValue().getExpiresAt().isBefore(now)) {
                store.remove(entry.getKey());
                removed++;
            }
        }
        
        if (removed > 0) {
            logger.info("Cleaned up {} expired idempotency records", removed);
        }
    }
}
