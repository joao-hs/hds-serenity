package pt.ulisboa.tecnico.hdsledger.service.models;

import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicInteger;

import pt.ulisboa.tecnico.hdsledger.utilities.InsufficientFundsException;

public class Account {
    private final String id;
    private AtomicInteger balance = new AtomicInteger(100);

    public Account(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public int getBalance() {
        return balance.get();
    }

    public int incrementBalance(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        return balance.addAndGet(amount);
    }

    public int decrementBalance(int amount) throws InsufficientFundsException {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (balance.get() < amount) {
            throw new InsufficientFundsException(MessageFormat.format("Insufficient funds to withdraw {0} from account {1}",
                    amount, id));
        }
        return balance.addAndGet(-amount);
    }
}
