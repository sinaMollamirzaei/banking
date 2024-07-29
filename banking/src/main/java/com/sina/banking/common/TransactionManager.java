package com.sina.banking.common;

import com.sina.banking.domain.TransactionType;

import java.util.ArrayList;
import java.util.List;

public class TransactionManager {
    private final List<TransactionObserver> observers = new ArrayList<>();

    public void addObserver(TransactionObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(TransactionObserver observer) {
        observers.remove(observer);
    }

    public void notifyObservers(String accountId, TransactionType transactionType, double amount) {
        for (TransactionObserver observer : observers) {
            observer.onTransaction(accountId, transactionType, amount);
        }
    }

    public void processTransaction(String accountId, TransactionType transactionType, double amount) {
        // Process the transaction here (e.g., update account balance)

        // Notify observers about the transaction
        notifyObservers(accountId, transactionType, amount);
    }
}
