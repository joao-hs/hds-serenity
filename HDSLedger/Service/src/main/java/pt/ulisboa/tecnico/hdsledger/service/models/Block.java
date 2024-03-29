package pt.ulisboa.tecnico.hdsledger.service.models;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import pt.ulisboa.tecnico.hdsledger.communication.client.TransferRequest;
import pt.ulisboa.tecnico.hdsledger.communication.consensus.CommitMessage;
import pt.ulisboa.tecnico.hdsledger.utilities.MerkleTree;
import pt.ulisboa.tecnico.hdsledger.utilities.RSAEncryption;

public class Block {
    // Block Header
    private Block previousBlock;
    private String chainHash = null;
    private Collection<CommitMessage> proofOfConsensus = new HashSet<>();
    private MerkleTree merkleTree = null;

    // Block Body - needs `Expose` annotation to be serialized
    @Expose
    private SortedSet<Transaction> transactions = new TreeSet<>(new Comparator<String>() {
        @Override
        public int compare(Transaction t1, Transaction t2) {
            TransferRequest tr1 = t1.getTransferRequest();
            TransferRequest tr2 = t2.getTransferRequest();
            int comp = tr1.getTimestamp().compareTo(tr2.getTimestamp());
            if (comp == 0 && !t1.equals(t2)){
                comp = -1;
            }
            return comp;
        }
    });


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

            transactions.add(new Transaction(ownRequest));
        });

        this.previousBlock = null;

        try {
            this.chainHash = RSAEncryption.digest(concatTransactions());
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

    public void addAllCommitMessages(Collection<CommitMessage> commitMessages) {
        proofOfConsensus.addAll(commitMessages);
    }

    public void setChainHash() {
        if (chainHash != null) {
            return;
        }
        try {
            this.chainHash = RSAEncryption.digest(concatTransactions() + previousBlock.chainHash);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public void setMerkleTree() {
        merkleTree = new MerkleTree(transactions.stream().map(Transaction::toString).toList());
    }

    public String getMerkleRootHash() {
        return merkleTree.getRoot();
    }

    public ArrayList<String> getProofOfInclusion(Transaction transaction) {
        return merkleTree.getProof(transaction.toString());
    }

    public Collection<CommitMessage> getProofOfConsensus() {
        return proofOfConsensus;
    }

    private String concatTransactions() {
        StringBuilder sb = new StringBuilder();
        transactions.forEach(transaction -> sb.append(transaction.toString()));
        return sb.toString();
    }

    public String getSerializedBlock() {
        Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();
        return gson.toJson(this);
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}
