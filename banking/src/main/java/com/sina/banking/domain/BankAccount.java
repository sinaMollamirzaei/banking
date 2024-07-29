package com.sina.banking.domain;


import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.Date;

@Entity
@Data
public class BankAccount {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    private Date created;

    @UpdateTimestamp
    private Date updated;

    @Column(unique = true)
    private String accountNumber;

    private String accountHolderName;

    private Long balance;

    public void deposit(long amount) {
        balance += amount;
    }

    public void withdraw(long amount) {
        balance -= amount;
    }

    public void transfer(BankAccount toAccount, long amount) {
        this.withdraw(amount);
        toAccount.deposit(amount);
    }
}
