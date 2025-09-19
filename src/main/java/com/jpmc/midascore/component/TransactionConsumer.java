package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TransactionConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(TransactionConsumer.class);
    private int transactionCount = 0;

    @Autowired
    private DatabaseConduit databaseConduit;

    @Autowired
    private IncentiveService incentiveService;

    @KafkaListener(topics = "${general.kafka-topic}")
    @Transactional
    public void consumeTransaction(Transaction transaction) {
        transactionCount++;
        logger.info("Received transaction #{}: senderId={}, recipientId={}, amount={}", 
                    transactionCount, transaction.getSenderId(), transaction.getRecipientId(), transaction.getAmount());
        
        // Validate and process the transaction
        if (validateAndProcessTransaction(transaction)) {
            logger.info("Transaction #{} processed successfully", transactionCount);
        } else {
            logger.warn("Transaction #{} was invalid and discarded", transactionCount);
        }
    }

    private boolean validateAndProcessTransaction(Transaction transaction) {
        // Validate senderId
        UserRecord sender = databaseConduit.findUserById(transaction.getSenderId());
        if (sender == null) {
            logger.warn("Invalid senderId: {}", transaction.getSenderId());
            return false;
        }

        // Validate recipientId
        UserRecord recipient = databaseConduit.findUserById(transaction.getRecipientId());
        if (recipient == null) {
            logger.warn("Invalid recipientId: {}", transaction.getRecipientId());
            return false;
        }

        // Validate sender has sufficient balance
        if (sender.getBalance() < transaction.getAmount()) {
            logger.warn("Insufficient balance for senderId: {}. Required: {}, Available: {}", 
                       transaction.getSenderId(), transaction.getAmount(), sender.getBalance());
            return false;
        }

        // All validations passed - get incentive from API and process the transaction
        try {
            // Get incentive from API
            float incentive = incentiveService.getIncentive(transaction);
            
            // Update sender balance (subtract amount)
            sender.setBalance(sender.getBalance() - transaction.getAmount());
            databaseConduit.updateUser(sender);

            // Update recipient balance (add amount + incentive)
            recipient.setBalance(recipient.getBalance() + transaction.getAmount() + incentive);
            databaseConduit.updateUser(recipient);

            // Create and save transaction record with incentive
            TransactionRecord transactionRecord = new TransactionRecord(sender, recipient, transaction.getAmount(), incentive);
            databaseConduit.save(transactionRecord);

            logger.info("Transaction processed: {} sent {} to {} (incentive: {}). New balances - Sender: {}, Recipient: {}", 
                       sender.getName(), transaction.getAmount(), recipient.getName(), incentive,
                       sender.getBalance(), recipient.getBalance());

            return true;
        } catch (Exception e) {
            logger.error("Error processing transaction: {}", e.getMessage());
            return false;
        }
    }
}