package pt.ulisboa.tecnico.hdsledger.service.models;

import java.text.MessageFormat;

import pt.ulisboa.tecnico.hdsledger.utilities.InsufficientFundsException;

public class Account {
    private final String id;
    private double balance = 100; // initial amount
    private final Object lock = new Object();

    public Account(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public double getBalance() {
        synchronized (lock) {
            return balance;
        }
    }

    public double incrementBalance(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        synchronized (lock) {
            balance += amount;
            return balance;
        }
    }

    public double decrementBalance(double amount) throws InsufficientFundsException {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        synchronized (lock) {
            if (balance < amount) {
                throw new InsufficientFundsException(MessageFormat.format("Insufficient funds to withdraw {0} from account {1}",
                        amount, id));
            }
            balance -= amount;
            return balance;
        }
    }

    public boolean boundedDeduct(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        double deducted = 0;
        synchronized (lock) {
            deducted = Math.min(balance, amount);
            balance -= deducted;
        }
        if (deducted == amount) {
            return true;
        }
        return false;
    }

    public boolean canReceiveFromClients() {
        return true;
    }
}
