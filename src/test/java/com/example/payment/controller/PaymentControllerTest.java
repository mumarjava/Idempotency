package com.example.payment.controller;

import com.example.payment.model.ChargeRequest;
import com.example.payment.model.ChargeResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for idempotent payment API.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private ChargeRequest testRequest;
    
    @BeforeEach
    void setUp() {
        testRequest = new ChargeRequest(
                "customer_123",
                new BigDecimal("99.99"),
                "USD",
                "Test payment"
        );
    }
    
    @Test
    @DisplayName("First call should process payment and return success")
    void testFirstCallProcessesPayment() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();
        
        MvcResult result = mockMvc.perform(post("/api/payments/charge")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.transactionId").isNotEmpty())
                .andExpect(jsonPath("$.amount").value(99.99))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.message").value("Payment processed successfully"))
                .andReturn();
        
        String responseBody = result.getResponse().getContentAsString();
        ChargeResponse response = objectMapper.readValue(responseBody, ChargeResponse.class);
        
        assertNotNull(response.getTransactionId());
        assertTrue(response.getTransactionId().startsWith("txn_"));
        assertEquals("SUCCESS", response.getStatus());
    }
    
    @Test
    @DisplayName("Retry with same key and same request should return cached response")
    void testRetryWithSameKeyReturnsCache() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();
        
        // First call
        MvcResult firstResult = mockMvc.perform(post("/api/payments/charge")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        ChargeResponse firstResponse = objectMapper.readValue(
                firstResult.getResponse().getContentAsString(),
                ChargeResponse.class
        );
        
        // Second call with same key and same request
        MvcResult secondResult = mockMvc.perform(post("/api/payments/charge")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        ChargeResponse secondResponse = objectMapper.readValue(
                secondResult.getResponse().getContentAsString(),
                ChargeResponse.class
        );
        
        // Verify responses are identical
        assertEquals(firstResponse.getTransactionId(), secondResponse.getTransactionId());
        assertEquals(firstResponse.getStatus(), secondResponse.getStatus());
        assertEquals(firstResponse.getAmount(), secondResponse.getAmount());
        assertEquals(firstResponse.getCurrency(), secondResponse.getCurrency());
    }
    
    @Test
    @DisplayName("Same key with different request should return 409 Conflict")
    void testSameKeyDifferentRequestReturnsConflict() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();
        
        // First call
        mockMvc.perform(post("/api/payments/charge")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk());
        
        // Second call with same key but different request
        ChargeRequest differentRequest = new ChargeRequest(
                "customer_456",  // Different customer
                new BigDecimal("199.99"),  // Different amount
                "EUR",  // Different currency
                "Different payment"
        );
        
        mockMvc.perform(post("/api/payments/charge")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(differentRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.message")
                        .value("Idempotency key already used with different request parameters"));
    }
    
    @Test
    @DisplayName("Missing Idempotency-Key header should return 400 Bad Request")
    void testMissingIdempotencyKeyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/payments/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.message").value("Idempotency-Key header is required"));
    }
    
    @Test
    @DisplayName("Empty Idempotency-Key header should return 400 Bad Request")
    void testEmptyIdempotencyKeyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/payments/charge")
                        .header("Idempotency-Key", "")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.message").value("Idempotency-Key header is required"));
    }
    
    @Test
    @DisplayName("Whitespace-only Idempotency-Key should return 400 Bad Request")
    void testWhitespaceIdempotencyKeyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/payments/charge")
                        .header("Idempotency-Key", "   ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }
    
    @Test
    @DisplayName("Concurrent requests with same key should all get same response")
    void testConcurrentRequestsWithSameKey() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        ChargeResponse[] responses = new ChargeResponse[threadCount];
        Exception[] exceptions = new Exception[threadCount];
        
        // Submit concurrent requests
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    MvcResult result = mockMvc.perform(post("/api/payments/charge")
                                    .header("Idempotency-Key", idempotencyKey)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(testRequest)))
                            .andExpect(status().isOk())
                            .andReturn();
                    
                    responses[index] = objectMapper.readValue(
                            result.getResponse().getContentAsString(),
                            ChargeResponse.class
                    );
                } catch (Exception e) {
                    exceptions[index] = e;
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for all requests to complete
        latch.await();
        executor.shutdown();
        
        // Verify no exceptions occurred
        for (int i = 0; i < threadCount; i++) {
            assertNull(exceptions[i], "Thread " + i + " threw exception: " + 
                    (exceptions[i] != null ? exceptions[i].getMessage() : ""));
        }
        
        // Verify all responses have the same transaction ID
        String firstTransactionId = responses[0].getTransactionId();
        assertNotNull(firstTransactionId);
        
        for (int i = 0; i < threadCount; i++) {
            assertNotNull(responses[i], "Response " + i + " is null");
            assertEquals(firstTransactionId, responses[i].getTransactionId(),
                    "Response " + i + " has different transaction ID");
            assertEquals("SUCCESS", responses[i].getStatus());
        }
    }
    
    @Test
    @DisplayName("Different idempotency keys should create different transactions")
    void testDifferentKeysCreateDifferentTransactions() throws Exception {
        String key1 = UUID.randomUUID().toString();
        String key2 = UUID.randomUUID().toString();
        
        // First request
        MvcResult result1 = mockMvc.perform(post("/api/payments/charge")
                        .header("Idempotency-Key", key1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        ChargeResponse response1 = objectMapper.readValue(
                result1.getResponse().getContentAsString(),
                ChargeResponse.class
        );
        
        // Second request with different key
        MvcResult result2 = mockMvc.perform(post("/api/payments/charge")
                        .header("Idempotency-Key", key2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        ChargeResponse response2 = objectMapper.readValue(
                result2.getResponse().getContentAsString(),
                ChargeResponse.class
        );
        
        // Verify different transaction IDs
        assertNotEquals(response1.getTransactionId(), response2.getTransactionId());
    }
    
    @Test
    @DisplayName("Invalid request should return 400 Bad Request")
    void testInvalidRequestReturnsBadRequest() throws Exception {
        ChargeRequest invalidRequest = new ChargeRequest(
                "",  // Empty customer ID
                new BigDecimal("0.00"),  // Invalid amount
                "",  // Empty currency
                null
        );
        
        mockMvc.perform(post("/api/payments/charge")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
