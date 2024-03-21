package pt.ulisboa.tecnico.hdsledger.utilities;

import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public class Block {
    private Set<Transaction> transactions = new HashSet<>();

    private Block previousBlock;
    private String hash = null;

    public Block() {
        // empty block
    }

    public Block(Enumeration<String> accounts) {
        // Genesis block
        accounts.asIterator().forEachRemaining(accountId -> {
            transactions.add(
                // ! Amount is hardcoded to 100, should match the initial balance in Account
                new Transaction(null, accountId, 100, Instant.now().toString(), 0)
            );
        });

        this.previousBlock = null;

        try {
            this.hash = RSAEncryption.digest(concatTransactions());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public Block(Collection<Transaction> transactions) {
        this.transactions.addAll(transactions);
    }

    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
    }

    public Set<Transaction> getTransactions() {
        return transactions;
    }

    public void setHash() {
        if (hash != null) {
            return;
        }
        try {
            this.hash = RSAEncryption.digest(String.join(concatTransactions(), previousBlock.hash));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private String concatTransactions() {
        StringBuilder sb = new StringBuilder();
        transactions.forEach(transaction -> sb.append(transaction.toString()));
        return sb.toString();
    }
}
