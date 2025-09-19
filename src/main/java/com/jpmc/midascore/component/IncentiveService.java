package com.jpmc.midascore.component;

import com.jpmc.midascore.foundation.Incentive;
import com.jpmc.midascore.foundation.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class IncentiveService {
    
    private static final Logger logger = LoggerFactory.getLogger(IncentiveService.class);
    private static final String INCENTIVE_API_URL = "http://localhost:8080/incentive";
    
    @Autowired
    private RestTemplate restTemplate;
    
    public float getIncentive(Transaction transaction) {
        try {
            logger.info("Calling incentive API for transaction: senderId={}, recipientId={}, amount={}", 
                       transaction.getSenderId(), transaction.getRecipientId(), transaction.getAmount());
            
            Incentive incentive = restTemplate.postForObject(INCENTIVE_API_URL, transaction, Incentive.class);
            
            if (incentive != null) {
                logger.info("Received incentive: {}", incentive.getAmount());
                return incentive.getAmount();
            } else {
                logger.warn("Received null incentive response, defaulting to 0.0");
                return 0.0f;
            }
        } catch (Exception e) {
            logger.error("Error calling incentive API: {}", e.getMessage());
            return 0.0f; // Default to 0 if API call fails
        }
    }
}