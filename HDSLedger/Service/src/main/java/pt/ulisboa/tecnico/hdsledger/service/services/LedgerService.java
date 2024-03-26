package pt.ulisboa.tecnico.hdsledger.service.services;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import com.google.gson.Gson;

import pt.ulisboa.tecnico.hdsledger.communication.client.BalanceRequest;
import pt.ulisboa.tecnico.hdsledger.communication.client.BalanceResponse;
import pt.ulisboa.tecnico.hdsledger.communication.client.TransferRequest;
import pt.ulisboa.tecnico.hdsledger.communication.client.TransferResponse;
import pt.ulisboa.tecnico.hdsledger.service.interfaces.ILedgerService;
import pt.ulisboa.tecnico.hdsledger.service.models.Account;
import pt.ulisboa.tecnico.hdsledger.service.models.Block;
import pt.ulisboa.tecnico.hdsledger.service.models.Transaction;
import pt.ulisboa.tecnico.hdsledger.utilities.AccountNotFoundException;
import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;
import pt.ulisboa.tecnico.hdsledger.utilities.InsufficientFundsException;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public class LedgerService implements ILedgerService {
    private static final CustomLogger LOGGER = new CustomLogger(LedgerService.class.getName());

    private static LedgerService instance = null;

    private ConcurrentHashMap<String, Account> accounts = new ConcurrentHashMap<>();

    private ConcurrentHashMap<Transaction,Object> transactionLocks = new ConcurrentHashMap<>();
    
    private ConcurrentHashMap<Transaction,TransferResponse.Status> transactionResps = new ConcurrentHashMap<>();

    private ArrayList<Block> blockchain = new ArrayList<>();

    private ProcessConfig config = null;
    private ClientService clientService = null;
    private NodeService nodeService = null;
    private BlockBuilderService blockBuilderService = null;

    private LedgerService() {
    }

    public static LedgerService getInstance() {
        if (instance == null) {
            instance = new LedgerService();
        }
        return instance;
    }

    public void setConfig(ProcessConfig config) {
        this.config = config;
    }

    public void setClientService(ClientService clientService) {
        this.clientService = clientService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setBlockBuilderService(BlockBuilderService blockBuilderService) {
        this.blockBuilderService = blockBuilderService;
    }

    public void init() throws Exception {
        if (accounts.isEmpty() || clientService == null || nodeService == null) {
            throw new Exception("All accounts, ClientService and NodeService must be set before calling init.");
        }
        // Create the genesis block
        Block genesisBlock = new Block(accounts.keys());
        blockchain.add(genesisBlock);

        nodeService.listen();
        clientService.listen();
    }

    public void addAllAccounts(ProcessConfig[] clientConfigs) {
        for (ProcessConfig config : clientConfigs) {
            accounts.put(config.getId(), new Account(config.getId()));
        }
    }

    public int getBalance(String id) throws AccountNotFoundException {
        if (!accounts.containsKey(id)) {
            throw new AccountNotFoundException(MessageFormat.format("Account {0} not found", id));
        }
        Account account = accounts.get(id);
        int balance = -1;

        synchronized (account) {
            balance = account.getBalance();
        }

        return balance;
    }

    public void performTransfer(String senderId, String receiverId, int amount) throws AccountNotFoundException, InsufficientFundsException {
        if (!accounts.contains(senderId)) {
            throw new AccountNotFoundException(MessageFormat.format("Account {0} not found", senderId));
        }
        if (!accounts.contains(receiverId)) {
            throw new AccountNotFoundException(MessageFormat.format("Account {0} not found", receiverId));
        }

        Account sender = accounts.get(senderId);
        Account receiver = accounts.get(receiverId);

        // no need to acquire lock on receiver since it will only increment
        // if the receiver is transfering money {B} (that relies on this transfer {A}) concurrently:
        // 1. (A did not happen yet) B won't have sufficient funds to transfer
        // 2. (A happened) B will have sufficient funds to transfer
        synchronized (sender) {
            sender.decrementBalance(amount);
            receiver.incrementBalance(amount);
        }

    }

    private synchronized Object getLockForTransaction(Transaction transaction) {
        transactionLocks.putIfAbsent(transaction, new Object());
        return transactionLocks.get(transaction);
    }

    private synchronized TransferResponse.Status getResponseForTransaction(Transaction transaction) {
        transactionResps.putIfAbsent(transaction, TransferResponse.Status.UNDEFINED);
        return transactionResps.get(transaction);
    }

    private synchronized void setResponseForTransaction(Transaction transaction ,TransferResponse.Status status) {
        transactionResps.put(transaction, status);
    }

    @Override
    public TransferResponse transfer(TransferRequest request) {
        // TODO: Validate request (Ledger-logic)

        Transaction transaction = new Transaction(request);
        blockBuilderService.addTransaction(transaction);
        Block block = blockBuilderService.buildBlock();
        if (block != null) {
            nodeService.reachConsensus(block.toJson());
        }

        
        getResponseForTransaction(transaction);


        // TODO: Wait for transaction to be added to the blockchain
        Object lock = getLockForTransaction(transaction);
        synchronized(lock){
                try {
                    uponConsensusReached(block.toJson());
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        }

        // TODO: Get result of the transaction
        
        TransferResponse response = new TransferResponse(getResponseForTransaction(transaction));
        return response;

    }

    @Override
    public BalanceResponse balance(BalanceRequest request) {
        // TODO: Validate request (Ledger-logic)
        BalanceResponse response = new BalanceResponse(request.getTarget());
        try {
            response.setBalance(getBalance(request.getTarget()));
            response.setStatus(BalanceResponse.Status.OK);
        } catch (AccountNotFoundException e) {
            LOGGER.log(Level.INFO, MessageFormat.format("{0} - Account {1} not found", config.getId(), request.getTarget()));
            response.setStatus(BalanceResponse.Status.ACCOUNT_NOT_FOUND);
        }
        return response;

    }

    @Override
    public synchronized void uponConsensusReached(String serializedValue) {
        Block block = new Gson().fromJson(serializedValue, Block.class);
        // TODO: Validate block (ledger-logic)
        // if not valid, return false

        for (Transaction transaction : block.getTransactions()) {
            TransferRequest request = transaction.getTransferRequest();
            try {
                // TODO remove same transaction from transaction pool
                performTransfer(request.getSender(), request.getReceiver(), request.getAmount());
            } catch (AccountNotFoundException e) {
                setResponseForTransaction(transaction, TransferResponse.Status.ACCOUNT_NOT_FOUND);
                LOGGER.log(Level.INFO, MessageFormat.format("{0} - Account {1} not found", config.getId(), request.getSender()));
            } catch (InsufficientFundsException e) {
                setResponseForTransaction(transaction, TransferResponse.Status.INSUFFICIENT_FUNDS);
                LOGGER.log(Level.INFO, MessageFormat.format("{0} - Insufficient funds in account {1}", config.getId(), request.getSender()));
            }

            Object lock = getLockForTransaction(transaction);
            synchronized(lock){
                lock.notify();
            }
        }

        // TODO: actually chain together nodes
        blockchain.add(block);
    }
}
