package com.jpmc.midascore.entity;

import jakarta.persistence.*;

@Entity
public class TransactionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private UserRecord sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private UserRecord recipient;

    @Column(nullable = false)
    private float amount;

    @Column(nullable = false)
    private float incentive;

    protected TransactionRecord() {
        // JPA requires a default constructor
    }

    public TransactionRecord(UserRecord sender, UserRecord recipient, float amount, float incentive) {
        this.sender = sender;
        this.recipient = recipient;
        this.amount = amount;
        this.incentive = incentive;
    }

    public Long getId() {
        return id;
    }

    public UserRecord getSender() {
        return sender;
    }

    public void setSender(UserRecord sender) {
        this.sender = sender;
    }

    public UserRecord getRecipient() {
        return recipient;
    }

    public void setRecipient(UserRecord recipient) {
        this.recipient = recipient;
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    public float getIncentive() {
        return incentive;
    }

    public void setIncentive(float incentive) {
        this.incentive = incentive;
    }

    @Override
    public String toString() {
        return String.format("TransactionRecord[id=%d, senderId=%d, recipientId=%d, amount=%.2f, incentive=%.2f]", 
                           id, 
                           sender != null ? sender.getId() : null, 
                           recipient != null ? recipient.getId() : null, 
                           amount,
                           incentive);
    }
}