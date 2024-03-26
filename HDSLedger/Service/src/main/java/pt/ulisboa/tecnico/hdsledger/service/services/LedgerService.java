package pt.ulisboa.tecnico.hdsledger.service.services;

import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.apache.commons.lang3.tuple.Pair;

import com.google.gson.Gson;

import pt.ulisboa.tecnico.hdsledger.communication.client.BalanceRequest;
import pt.ulisboa.tecnico.hdsledger.communication.client.BalanceResponse;
import pt.ulisboa.tecnico.hdsledger.communication.client.TransferRequest;
import pt.ulisboa.tecnico.hdsledger.communication.client.TransferResponse;
import pt.ulisboa.tecnico.hdsledger.communication.consensus.CommitMessage;
import pt.ulisboa.tecnico.hdsledger.service.interfaces.ILedgerService;
import pt.ulisboa.tecnico.hdsledger.service.models.Account;
import pt.ulisboa.tecnico.hdsledger.service.models.Block;
import pt.ulisboa.tecnico.hdsledger.service.models.Transaction;
import pt.ulisboa.tecnico.hdsledger.utilities.AccountNotFoundException;
import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;
import pt.ulisboa.tecnico.hdsledger.utilities.ErrorMessage;
import pt.ulisboa.tecnico.hdsledger.utilities.HDSSException;
import pt.ulisboa.tecnico.hdsledger.utilities.InsufficientFundsException;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;
import pt.ulisboa.tecnico.hdsledger.utilities.RSAEncryption;

public class LedgerService implements ILedgerService {
    private static final CustomLogger LOGGER = new CustomLogger(LedgerService.class.getName());

    private static LedgerService instance = null;

    private ConcurrentHashMap<String, Account> accounts = new ConcurrentHashMap<>();

    private Map<String, Block> pendingBlocks = new ConcurrentHashMap<>();

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

    private boolean existsSender(TransferRequest request, ProcessConfig[] clientProcesses){
        for (ProcessConfig process : clientProcesses) {
            if(process.getId().equals(request.getSender())){
                return true;
            }
        }    
        return false;
    }

    private boolean existsReceiver(TransferRequest request, Set<String> clientIds){
        return clientIds.contains(request.getReceiver());
    }

    private boolean diffRecvSend(TransferRequest request){
        return !request.getSender().equals(request.getReceiver());
    }

    private boolean positiveAmount(TransferRequest request){
        return request.getAmount() > 0;
    }

    private boolean positiveFee(TransferRequest request){
        return request.getFee() > 0;
    }

    @Override
    public TransferResponse transfer(TransferRequest request) {
        ProcessConfig[] clientProcesses = clientService.getConfigs();
        Map<String, ProcessConfig> nodeProcesses = nodeService.getConfigs();

        if(!existsSender(request, clientProcesses)){  
            TransferResponse response = new TransferResponse(TransferResponse.Status.BAD_SOURCE);
            return response;
        }

        if(!existsReceiver(request, nodeProcesses.keySet())){  
            TransferResponse response = new TransferResponse(TransferResponse.Status.BAD_DESTINATION);
            return response;
        }
        
        if(!diffRecvSend(request)){
            TransferResponse response = new TransferResponse(TransferResponse.Status.BAD_DESTINATION);
            return response;
        }

        if(!positiveFee(request)){
            TransferResponse response = new TransferResponse(TransferResponse.Status.NO_FEE);
            return response;
        }

        String requestHash = "";
        try {
            requestHash = RSAEncryption.digest(request.toJson()); // TODo
        } catch (NoSuchAlgorithmException e) {
            throw new HDSSException(ErrorMessage.HashingError);
        }

        Transaction transaction = new Transaction(request);
        blockBuilderService.addTransaction(transaction);
        Block block = blockBuilderService.buildBlock();
        if (block != null) {
            LOGGER.log(Level.INFO, MessageFormat.format("{0} - Block built: {1}", config.getId(), block.toJson()));
            pendingBlocks.put(block.getMerkleRootHash(), block);
            nodeService.reachConsensus(block.getSerializedBlock());
        }

        // TODO: Wait for transaction to be added to the blockchain
        try {
            Thread.sleep(3500); // TODO: remove this
        } catch (InterruptedException e) {
            e.printStackTrace();
        } 

        // TODO: Get result of the transaction

        TransferResponse response = new TransferResponse(
            TransferResponse.Status.OK,
            requestHash,
            Pair.of(block.getMerkleRootHash(), block.getProofOfInclusion(transaction)), // proof of belonging in a block
            block.getProofOfConsensus() // proof of the same block being agreed
        );
        LOGGER.log(Level.INFO, MessageFormat.format("{0} - Sending transfer response <{1},{2},<{3},{4}>,{5}>", config.getId(), response.getStatus().name(), response.getClientRequestHash(), response.getProofOfInclusion().getLeft(), new Gson().toJson(response.getProofOfInclusion().getRight()), response.getProofOfConsensus()));
        /*
        proof of block being in the blockchain will be done by the client
        by receiving a quorum of this responses
        */ 
        return response;

    }

    private boolean existsTarget(BalanceRequest request, ProcessConfig[] clientProcesses){
        for (ProcessConfig process : clientProcesses) {
            if(process.getId().equals(request.getTarget())){
                return true;
            }
        }    
        return false;
    }

    @Override
    public BalanceResponse balance(BalanceRequest request) {

        ProcessConfig[] clientProcesses = clientService.getConfigs();

        if(!existsTarget(request,clientProcesses)){  
            BalanceResponse response = new BalanceResponse(request.getTarget());
            response.setStatus(BalanceResponse.Status.ACCOUNT_NOT_FOUND);
            return response;
        }

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
    public synchronized void uponConsensusReached(String serializedValue, Collection<CommitMessage> commitMessages) {
        LOGGER.log(Level.INFO, MessageFormat.format("{0} - Consensus reached for block: {1}, Commit Messages: {2}", config.getId(), serializedValue, new Gson().toJson(commitMessages)));
        Block block = new Gson().fromJson(serializedValue, Block.class);
        block.setMerkleTree();
        String blockKey = block.getMerkleRootHash();
        if (pendingBlocks.containsKey(blockKey)) {
            // Block is already created
            block = pendingBlocks.get(blockKey);
        }
        block.addAllCommitMessages(commitMessages); // no need to verify since we trust ourselves
        // TODO: Validate block (ledger-logic)
        // if not valid, return false

        for (Transaction transaction : block.getTransactions()) {
            TransferRequest request = transaction.getTransferRequest();
            LOGGER.log(Level.INFO, MessageFormat.format("{0} - Processing transaction: {1}", config.getId(), request.toJson()));
            try {
                // TODO remove same transaction from transaction pool
                performTransfer(request.getSender(), request.getReceiver(), request.getAmount());
            } catch (AccountNotFoundException e) {
                LOGGER.log(Level.INFO, MessageFormat.format("{0} - Account {1} not found", config.getId(), request.getSender()));
            } catch (InsufficientFundsException e) {
                LOGGER.log(Level.INFO, MessageFormat.format("{0} - Insufficient funds in account {1}", config.getId(), request.getSender()));
            }
        }
        // TODO: actually chain together nodes
        blockchain.add(block);
    }
}
