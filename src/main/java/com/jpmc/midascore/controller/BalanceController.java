package com.jpmc.midascore.controller;

import com.jpmc.midascore.component.DatabaseConduit;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Balance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BalanceController {

    @Autowired
    private DatabaseConduit databaseConduit;

    @GetMapping("/balance")
    public Balance getBalance(@RequestParam Long userId) {
        try {
            UserRecord user = databaseConduit.findUserById(userId);
            if (user != null) {
                return new Balance(user.getBalance());
            } else {
                return new Balance(0.0f);
            }
        } catch (Exception e) {
            // If user doesn't exist or any error occurs, return balance of 0
            return new Balance(0.0f);
        }
    }
}