package com.sina.banking.common;

import com.sina.banking.domain.TransactionType;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class TransactionLogger implements TransactionObserver{
    private final String logFile;

    public TransactionLogger(String logFile) {
        this.logFile = logFile;
    }
    @Override
    public void onTransaction(String accountId, TransactionType transactionType, double amount) {
        try (FileWriter fw = new FileWriter(logFile, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println("Account: " + accountId + ", Type: " + transactionType + ", Amount: $" + amount);
        } catch (IOException e) {
            System.err.println("Error writing to log file: " + e.getMessage());
        }
    }
}
