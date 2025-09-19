package com.jpmc.midascore;

import com.jpmc.midascore.component.DatabaseConduit;
import com.jpmc.midascore.component.TransactionConsumer;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Transaction;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext
public class WilburBalanceTest {
    static final Logger logger = LoggerFactory.getLogger(WilburBalanceTest.class);

    @Autowired
    private UserPopulator userPopulator;

    @Autowired
    private FileLoader fileLoader;

    @Autowired
    private TransactionConsumer transactionConsumer;

    @Autowired
    private DatabaseConduit databaseConduit;

    @Test
    void calculateWilburBalance() throws InterruptedException {
        // Populate users
        userPopulator.populate();
        
        // Process all transactions from the test data
        String[] transactionLines = fileLoader.loadStrings("/test_data/alskdjfh.fhdjsk");
        for (String transactionLine : transactionLines) {
            String[] parts = transactionLine.split(", ");
            long senderId = Long.parseLong(parts[0]);
            long recipientId = Long.parseLong(parts[1]);
            float amount = Float.parseFloat(parts[2]);
            
            Transaction transaction = new Transaction(senderId, recipientId, amount);
            transactionConsumer.consumeTransaction(transaction);
        }
        
        // Find wilbur (ID 9 based on the user data order)
        UserRecord wilbur = databaseConduit.findUserById(9);
        
        logger.info("Wilbur's final balance: {}", wilbur.getBalance());
        logger.info("Wilbur's final balance rounded down: {}", (int) Math.floor(wilbur.getBalance()));
    }
}