package com.sina.banking.common;

import com.sina.banking.domain.TransactionType;

public interface TransactionObserver {
    void onTransaction(String accountId, TransactionType transactionType, double amount);
}
