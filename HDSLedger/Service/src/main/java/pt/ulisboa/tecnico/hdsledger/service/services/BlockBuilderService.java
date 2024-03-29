package pt.ulisboa.tecnico.hdsledger.service.services;

import java.text.MessageFormat;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.logging.Level;

import pt.ulisboa.tecnico.hdsledger.communication.client.TransferRequest;
import pt.ulisboa.tecnico.hdsledger.service.interfaces.IBlockBuilderService;
import pt.ulisboa.tecnico.hdsledger.service.models.Block;
import pt.ulisboa.tecnico.hdsledger.service.models.Transaction;
import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public class BlockBuilderService implements IBlockBuilderService {

    private static final CustomLogger LOGGER = new CustomLogger(BlockBuilderService.class.getName());

    private ProcessConfig config;
    
    private int initialCapacity = 10;
    private int minFee;
    private int maxFee;
    private int feeThreshold;
    private int availableFee = 0;
    private float minAmountFeeRatio;

    private PriorityQueue<Transaction> transactionPool = new PriorityQueue<Transaction>(this.initialCapacity, new Comparator<Transaction>() {
        @Override
        public int compare(Transaction t1, Transaction t2) {
            return t2.getTransferRequest().getFee() - t1.getTransferRequest().getFee(); // Higher fee first
        }
    });


    public BlockBuilderService(ProcessConfig config) {
        this.config = config;
        this.minFee = config.getMinFee();
        this.maxFee = config.getMaxFee();
        this.feeThreshold = config.getFeeThreshold();
        this.minAmountFeeRatio = config.getMinFeeAmountRatio();
    }
    
    @Override
    public Block buildBlock() {
        synchronized (transactionPool) {
            if (availableFee < feeThreshold) {
                LOGGER.log(Level.INFO, MessageFormat.format("{0} - Not enough fee to build a block", config.getId()));
                return null;
            }
            Block block = new Block();
            while (availableFee > feeThreshold) {
                Transaction transaction = transactionPool.poll();
                TransferRequest request = transaction.getTransferRequest();
                availableFee -= request.getFee();
                LOGGER.log(Level.INFO, MessageFormat.format("{0} - Adding transaction to block: {1}", config.getId(), transaction));
                block.addTransaction(transaction);
            }
            block.setMerkleTree();
            return block;
        }
    }

    @Override
    public boolean addTransaction(Transaction transaction) {
        if (!isTransactionValid(transaction)) {
            return false;
        }
        TransferRequest request = transaction.getTransferRequest();
        synchronized (transactionPool) {
            availableFee += request.getFee();
            return transactionPool.add(transaction);
        }
    }
    
    @Override
    public boolean removeTransaction(Transaction transaction) {
        TransferRequest request = transaction.getTransferRequest();
        synchronized (transactionPool) {
            Boolean removed = transactionPool.remove(transaction);
            if (removed)
                availableFee -= request.getFee();
            return removed;
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
        TransferRequest request = transaction.getTransferRequest();
        return request.getFee() >= minFee 
            && request.getFee() <= maxFee 
            && request.getFee() / request.getAmount() >= minAmountFeeRatio;
    }
    
}
