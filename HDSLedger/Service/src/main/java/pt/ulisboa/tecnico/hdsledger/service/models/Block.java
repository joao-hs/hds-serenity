package pt.ulisboa.tecnico.hdsledger.service.models;

import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import pt.ulisboa.tecnico.hdsledger.communication.client.TransferRequest;
import pt.ulisboa.tecnico.hdsledger.utilities.ConsensusValue;
import pt.ulisboa.tecnico.hdsledger.utilities.RSAEncryption;

public class Block extends ConsensusValue {
    // TODO: change to ordered set
    private Set<Transaction> transactions = new HashSet<>();

    private Block previousBlock;
    private String hash = null;

    public Block() {
        // empty block
    }

    public Block(Enumeration<String> accounts) {
        // Genesis block
        accounts.asIterator().forEachRemaining(accountId -> {
            TransferRequest ownRequest = new TransferRequest(
                null, // sender
                accountId, // receiver
                100, // ! initial amount
                0, // fee
                "2024-02-25 16:59:07", // random timestamp
                0 // nonce
            );
            ownRequest.setCreator(null);
            ownRequest.setSignature(null);

            transactions.add(new Transaction(ownRequest));
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
            this.hash = RSAEncryption.digest(concatTransactions() + previousBlock.hash);
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
