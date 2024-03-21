package pt.ulisboa.tecnico.hdsledger.service.services;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import pt.ulisboa.tecnico.hdsledger.communication.BalanceRequest;
import pt.ulisboa.tecnico.hdsledger.communication.BalanceResponse;
import pt.ulisboa.tecnico.hdsledger.communication.TransferRequest;
import pt.ulisboa.tecnico.hdsledger.communication.TransferResponse;
import pt.ulisboa.tecnico.hdsledger.service.interfaces.ILedgerService;
import pt.ulisboa.tecnico.hdsledger.service.models.Account;
import pt.ulisboa.tecnico.hdsledger.utilities.AccountNotFoundException;
import pt.ulisboa.tecnico.hdsledger.utilities.Block;
import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;
import pt.ulisboa.tecnico.hdsledger.utilities.InsufficientFundsException;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;
import pt.ulisboa.tecnico.hdsledger.utilities.Transaction;

public class LedgerService implements ILedgerService {
    private static final CustomLogger LOGGER = new CustomLogger(LedgerService.class.getName());

    private static LedgerService instance = null;

    private ConcurrentHashMap<String, Account> accounts = new ConcurrentHashMap<>();

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

    public boolean validateTransaction(Transaction transaction) throws AccountNotFoundException, InsufficientFundsException {
        // TODO: Validate transaction (Ledger-logic)
        return true;
    }

    public void performTransfer(String senderId, String receiverId, int amount, String timestamp) throws AccountNotFoundException, InsufficientFundsException {
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

    @Override
    public TransferResponse transfer(TransferRequest request) {
        // TODO: Validate request (Ledger-logic)

        Transaction transaction = new Transaction(
            request.getSender(),
            request.getReceiver(),
            request.getAmount(),
            request.getTimestamp(),
            request.getFee());
        blockBuilderService.addTransaction(transaction);
        Block block = blockBuilderService.buildBlock();
        if (block != null) {
            nodeService.reachConsensus(block);
        }

        // TODO: Wait for transaction to be added to the blockchain


        // TODO: Get result of the transaction
        
        TransferResponse response = new TransferResponse(TransferResponse.Status.OK);
        return response;

    }

    @Override
    public BalanceResponse balance(BalanceRequest request) {
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
    public synchronized void uponConsensusReached(Block block) {
        // TODO: Validate block (ledger-logic)
        // if not valid, return false

        for (Transaction transaction : block.getTransactions()) {
            try {
                if (validateTransaction(transaction))
                    performTransfer(transaction.getSender(), transaction.getReceiver(), transaction.getAmount(), transaction.getTimestamp());
            } catch (AccountNotFoundException e) {
                LOGGER.log(Level.INFO, MessageFormat.format("{0} - Account {1} not found", config.getId(), transaction.getSender()));
            } catch (InsufficientFundsException e) {
                LOGGER.log(Level.INFO, MessageFormat.format("{0} - Insufficient funds in account {1}", config.getId(), transaction.getSender()));
            }
        }

        blockchain.add(block);
    }
}
