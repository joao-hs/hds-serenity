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
    private double minFee;
    private double maxFee;
    private double feeThreshold;
    private double availableFee = 0;
    private double maxBalanceFeeMargin;
    private double maxBalanceAmountMargin;

    private PriorityQueue<Transaction> transactionPool = new PriorityQueue<Transaction>(this.initialCapacity, new Comparator<Transaction>() {
        @Override
        public int compare(Transaction t1, Transaction t2) {
            return Double.compare(t2.getTransferRequest().getFee(), t1.getTransferRequest().getFee()); // Higher fee first
        }
    });


    public BlockBuilderService(ProcessConfig config) {
        this.config = config;
        this.minFee = config.getMinFee();
        this.maxFee = config.getMaxFee();
        this.feeThreshold = config.getFeeThreshold();
        this.maxBalanceFeeMargin = config.getMaxBalanceFeeMargin();
        this.maxBalanceAmountMargin = config.getMaxBalanceAmountMargin();
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

            block.sign(config.getId(), config.getPrivKeyPath());
            return block;
        }
    }

    @Override
    public boolean addTransaction(Transaction transaction, double currentBalance) {
        if (!isTransactionAcceptable(transaction)) {
            return false;
        }
        if (!isTransactionRisky(transaction, currentBalance)) {
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
    public void setTransactionFilters(double minFee, double maxFee, double feeThreshold, double maxBalanceFeeMargin, double maxBalanceAmountMargin) {
        this.minFee = minFee;
        this.maxFee = maxFee;
        this.feeThreshold = feeThreshold;
        this.maxBalanceFeeMargin = maxBalanceFeeMargin;
        this.maxBalanceAmountMargin = maxBalanceAmountMargin;
    }

    @Override
    public boolean isTransactionAcceptable(Transaction transaction) {
        TransferRequest request = transaction.getTransferRequest();
        return request.getFee() >= minFee 
            && request.getFee() <= maxFee;
    }

    @Override
    public boolean isTransactionRisky(Transaction transaction, double currentBalance) {
        TransferRequest request = transaction.getTransferRequest();
        double delta = -transactionPool.stream()
            .filter(t -> t.getTransferRequest().getSender().equals(request.getSender()))
            .mapToDouble(t -> t.getTransferRequest().getAmount() + t.getTransferRequest().getFee())
            .sum();
        return request.getAmount() + request.getFee() <= (currentBalance + delta) * maxBalanceAmountMargin
            && request.getFee() <= (currentBalance + delta) * maxBalanceFeeMargin;
    }
    
}
