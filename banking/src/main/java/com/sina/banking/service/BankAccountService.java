package com.sina.banking.service;

import com.sina.banking.common.TransactionLogger;
import com.sina.banking.common.TransactionManager;
import com.sina.banking.domain.BankAccount;
import com.sina.banking.domain.TransactionType;
import com.sina.banking.repository.BankAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class BankAccountService {
    @Autowired
    private BankAccountRepository repository;

    @Lazy
    @Autowired
    private BankAccountService self;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10); // Configurable thread pool size

    private final TransactionManager transactionManager = new TransactionManager();
    private final TransactionLogger transactionLogger;

    public BankAccountService() {
        this.transactionLogger = new TransactionLogger("transactions.log");
        this.transactionManager.addObserver(transactionLogger);
    }

    public void startBankingConsole() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\nChoose an option:");
            System.out.println("1. Create Account");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Transfer");
            System.out.println("5. Display Account Info");
            System.out.println("6. Exit");

            int choice = scanner.nextInt();
            handleOption(choice, scanner);
        }
    }

    private void handleOption(int choice, Scanner scanner) {
        Future<?> future = executorService.submit(() -> {
            processOption(choice, scanner);
        });

        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("Error executing task: " + e.getMessage());
            Thread.currentThread().interrupt(); // Restore interrupted status
        }
    }

    private void processOption(int choice, Scanner scanner) {
        switch (choice) {
            case 1:
                self.createAccountInteractive(scanner);
                break;
            case 2:
                self.depositInteractive(scanner);
                break;
            case 3:
                self.withdrawInteractive(scanner);
                break;
            case 4:
                self.transferInteractive(scanner);
                break;
            case 5:
                self.displayAccountInfoInteractive(scanner);
                break;
            case 6:
                System.out.println("Exiting...");
                scanner.close();
                executorService.shutdownNow();
                System.exit(0);
                break;
            default:
                System.out.println("Invalid option, please choose again.");
        }
    }

    @Transactional
    public void createAccountInteractive(Scanner scanner) {
        System.out.print("Enter Account Number: ");
        String accountNumber = scanner.next();
        System.out.print("Enter Holder Name: ");
        String holderName = scanner.next();
        System.out.print("Enter Initial Balance: ");
        long initialBalance = scanner.nextLong();
        try {
            self.createAccount(accountNumber, holderName, initialBalance);
        } catch (Exception e) {
            System.out.println("Failed to create account: " + e.getMessage());
        }
    }

    @Transactional
    public synchronized void depositInteractive(Scanner scanner) {
        System.out.print("Enter Account Id: ");
        long accountId = scanner.nextLong();
        System.out.print("Enter Deposit Amount: ");
        long amount = scanner.nextLong();
        try {
            self.deposit(accountId, amount);
            transactionManager.processTransaction(String.valueOf(accountId), TransactionType.DEPOSIT, amount);
        } catch (Exception e) {
            System.out.println("Deposit failed: " + e.getMessage());
        }
    }

    @Transactional
    public synchronized void withdrawInteractive(Scanner scanner) {
        System.out.print("Enter Account Id: ");
        long accountId = scanner.nextLong();
        System.out.print("Enter Withdraw Amount: ");
        long amount = scanner.nextLong();
        try {
            self.withdraw(accountId, amount);
            transactionManager.processTransaction(String.valueOf(accountId), TransactionType.WITHDRAW, amount);
        } catch (Exception e) {
            System.out.println("Withdrawal failed: " + e.getMessage());
        }
    }

    @Transactional
    public synchronized void transferInteractive(Scanner scanner) {
        System.out.print("Enter From Account Id: ");
        Long fromAccount = scanner.nextLong();
        System.out.print("Enter To Account Id: ");
        Long toAccount = scanner.nextLong();
        System.out.print("Enter Transfer Amount: ");
        long amount = scanner.nextLong();
        try {
            self.transfer(fromAccount, toAccount, amount);
            transactionManager.processTransaction(fromAccount + " to " + toAccount, TransactionType.TRANSFER, amount);
        } catch (Exception e) {
            System.out.println("Transfer failed: " + e.getMessage());
        }
    }

    @Transactional
    public void displayAccountInfoInteractive(Scanner scanner) {
        System.out.print("Enter Your Account Number: ");
        String accountNumber = scanner.next();
        Optional<BankAccount> optionalAccount = repository.findBankAccountByAccountNumber(accountNumber);
        if (!self.checkAccountExists(optionalAccount)) {
            System.out.println("Error: Account does not exist");
            return;
        }
        BankAccount bankAccount = optionalAccount.get();
        System.out.println("Your Account Info : \n Account number " + bankAccount.getAccountNumber()
                + "\n Account Holder name : " + bankAccount.getAccountHolderName()
                + "\n Your initial balance is : " + bankAccount.getBalance());
    }


    @Transactional
    public void createAccount(String accountNumber, String holderName, long initialBalance) {
        if (self.checkAccountNumberIsNotDuplicated(accountNumber)) {
            System.out.println("Error: Duplicated account number");
            return;
        }
        BankAccount account = new BankAccount();
        account.setAccountNumber(accountNumber);
        account.setAccountHolderName(holderName);
        account.setBalance(initialBalance);
        repository.save(account);
        System.out.println("Account created successfully with Account Number: " + account.getAccountNumber());
    }

    @Transactional(readOnly = true)
    public boolean checkAccountNumberIsNotDuplicated(String accountNumber) {
        Optional<BankAccount> bankAccountByAccountNumber = repository.findBankAccountByAccountNumber(accountNumber);
        return bankAccountByAccountNumber.isPresent();
    }

    @Transactional
    public void deposit(Long accountId, long amount) {
        BankAccount account = repository.findById(accountId).orElseThrow();
        account.deposit(amount);
        repository.save(account);
        System.out.println("Deposited successfully. New Balance: " + account.getBalance());
    }

    @Transactional
    public void withdraw(Long accountId, long amount) {
        BankAccount account = repository.findById(accountId).orElseThrow();
        if (!self.checkBankAccountBalance(account, amount)) {
            System.out.println("Insufficient Account Balance");
            return;
        }
        account.withdraw(amount);
        repository.save(account);
        System.out.println("Withdrawn successfully. New Balance: " + account.getBalance());
    }

    public boolean checkBankAccountBalance(BankAccount account, long amount) {
        return account.getBalance() >= amount;
    }

    @Transactional
    public void transfer(Long fromAccount, Long toAccount, long amount) {
        BankAccount sender = repository.findById(fromAccount).orElseThrow();
        BankAccount receiver = repository.findById(toAccount).orElseThrow();
        sender.transfer(receiver, amount);
        repository.save(sender);
        repository.save(receiver);
        System.out.println("Transfer successful from Account " + fromAccount + " to Account " + toAccount);
    }

    private boolean checkAccountExists(Optional<BankAccount> account) {
        return account.isPresent();
    }
}
