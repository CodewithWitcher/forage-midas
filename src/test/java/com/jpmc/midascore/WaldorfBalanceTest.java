package com.jpmc.midascore;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import com.jpmc.midascore.component.DatabaseConduit;
import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Transaction;

@SpringBootTest
@DirtiesContext
public class WaldorfBalanceTest {
    static final Logger logger = LoggerFactory.getLogger(WaldorfBalanceTest.class);

    @Autowired
    private UserPopulator userPopulator;

    @Autowired
    private FileLoader fileLoader;

    @Autowired
    private DatabaseConduit databaseConduit;

    @Test
    void calculateWaldorfBalance() {
        // Populate users first
        userPopulator.populate();
        
        // Load transaction data
        String[] transactionLines = fileLoader.loadStrings("/test_data/mnbvcxz.vbnm");
        
        // Process each transaction manually
        for (String transactionLine : transactionLines) {
            String[] parts = transactionLine.split(", ");
            long senderId = Long.parseLong(parts[0]);
            long recipientId = Long.parseLong(parts[1]);
            float amount = Float.parseFloat(parts[2]);
            
            Transaction transaction = new Transaction(senderId, recipientId, amount);
            processTransaction(transaction);
        }
        
        // Find waldorf's final balance
        UserRecord waldorf = null;
        for (long i = 1; i <= 11; i++) {
            UserRecord user = databaseConduit.findUserById(i);
            if (user != null && "waldorf".equals(user.getName())) {
                waldorf = user;
                break;
            }
        }
        
        if (waldorf != null) {
            logger.info("Waldorf's final balance: {}", waldorf.getBalance());
            logger.info("Waldorf's final balance rounded down: {}", (int) Math.floor(waldorf.getBalance()));
        } else {
            logger.error("Waldorf not found!");
        }
    }
    
    private void processTransaction(Transaction transaction) {
        // Validate senderId
        UserRecord sender = databaseConduit.findUserById(transaction.getSenderId());
        if (sender == null) {
            logger.warn("Invalid senderId: {}", transaction.getSenderId());
            return;
        }

        // Validate recipientId
        UserRecord recipient = databaseConduit.findUserById(transaction.getRecipientId());
        if (recipient == null) {
            logger.warn("Invalid recipientId: {}", transaction.getRecipientId());
            return;
        }

        // Validate sender has sufficient balance
        if (sender.getBalance() < transaction.getAmount()) {
            logger.warn("Insufficient balance for senderId: {}. Required: {}, Available: {}", 
                       transaction.getSenderId(), transaction.getAmount(), sender.getBalance());
            return;
        }

        // All validations passed - process the transaction
        try {
            // Get incentive from API (for now using 0.0 since we don't have API integration in test)
            float incentive = 0.0f;
            
            // Update sender balance (subtract amount)
            sender.setBalance(sender.getBalance() - transaction.getAmount());
            databaseConduit.updateUser(sender);

            // Update recipient balance (add amount + incentive)
            recipient.setBalance(recipient.getBalance() + transaction.getAmount() + incentive);
            databaseConduit.updateUser(recipient);

            // Create and save transaction record with incentive
            TransactionRecord transactionRecord = new TransactionRecord(sender, recipient, transaction.getAmount(), incentive);
            databaseConduit.save(transactionRecord);

            logger.info("Transaction processed: {} (ID:{}) sent {} to {} (ID:{}) (incentive: {}). New balances - Sender: {}, Recipient: {}", 
                       sender.getName(), sender.getId(), transaction.getAmount(), recipient.getName(), recipient.getId(), incentive,
                       sender.getBalance(), recipient.getBalance());
        } catch (Exception e) {
            logger.error("Error processing transaction: {}", e.getMessage());
        }
    }
}