package pt.ulisboa.tecnico.hdsledger.service.services;

import java.util.Comparator;
import java.util.PriorityQueue;

import pt.ulisboa.tecnico.hdsledger.service.interfaces.IBlockBuilderService;
import pt.ulisboa.tecnico.hdsledger.utilities.Block;
import pt.ulisboa.tecnico.hdsledger.utilities.Transaction;

public class BlockBuilderService implements IBlockBuilderService {

    private int initialCapacity = 10;
    private int minFee;
    private int maxFee;
    private int feeThreshold;
    private int availableFee = 0;
    private float minAmountFeeRatio;

    private PriorityQueue<Transaction> transactionPool = new PriorityQueue<Transaction>(this.initialCapacity, new Comparator<Transaction>() {
        @Override
        public int compare(Transaction t1, Transaction t2) {
            return t2.getFee() - t1.getFee(); // Higher fee first
        }
    });


    public BlockBuilderService() {
        this.minFee = 0;
        this.maxFee = Integer.MAX_VALUE;
        this.feeThreshold = 0;
        this.minAmountFeeRatio = 0;
    }
    
    @Override
    public Block buildBlock() {
        synchronized (transactionPool) {
            if (availableFee < feeThreshold) {
                return null;
            }
            Block block = new Block();
            while (availableFee >= feeThreshold) {
                Transaction transaction = transactionPool.poll();
                availableFee -= transaction.getFee();
                block.addTransaction(transaction);
            }
            return block;
        }
    }

    @Override
    public boolean addTransaction(Transaction transaction) {
        if (!isTransactionValid(transaction)) {
            return false;
        }
        synchronized (transactionPool) {
            availableFee += transaction.getFee();
            return transactionPool.add(transaction);
        }
    }
    
    @Override
    public boolean removeTransaction(Transaction transaction) {
        synchronized (transactionPool) {
            availableFee -= transaction.getFee();
            return transactionPool.remove(transaction);
        }
    }

    @Override
    public void setTransactionFilters(int minFee, int maxFee, int feeThreshold, float minAmountFeeRation) {
        this.minFee = minFee;
        this.maxFee = maxFee;
        this.feeThreshold = feeThreshold;
        this.minAmountFeeRatio = minAmountFeeRation;
    }

    @Override
    public boolean isTransactionValid(Transaction transaction) {
        return transaction.getFee() >= minFee 
            && transaction.getFee() <= maxFee 
            && transaction.getAmount() * minAmountFeeRatio >= transaction.getFee();
    }
    
}
