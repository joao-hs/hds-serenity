package pt.ulisboa.tecnico.hdsledger.service.services;

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
import pt.ulisboa.tecnico.hdsledger.communication.client.ClientResponse;
import pt.ulisboa.tecnico.hdsledger.communication.client.TransferRequest;
import pt.ulisboa.tecnico.hdsledger.communication.client.TransferResponse;
import pt.ulisboa.tecnico.hdsledger.communication.consensus.CommitMessage;
import pt.ulisboa.tecnico.hdsledger.service.interfaces.ILedgerService;
import pt.ulisboa.tecnico.hdsledger.service.models.Account;
import pt.ulisboa.tecnico.hdsledger.service.models.Block;
import pt.ulisboa.tecnico.hdsledger.service.models.Transaction;
import pt.ulisboa.tecnico.hdsledger.service.models.ValidatorAccount;
import pt.ulisboa.tecnico.hdsledger.utilities.AccountNotFoundException;
import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;
import pt.ulisboa.tecnico.hdsledger.utilities.InsufficientFundsException;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public class LedgerService implements ILedgerService {
    private static final CustomLogger LOGGER = new CustomLogger(LedgerService.class.getName());

    private LedgerServiceWrapper self;

    private ConcurrentHashMap<String, Account> clientAccounts = new ConcurrentHashMap<>();

    private ConcurrentHashMap<String, ValidatorAccount> validatorAccounts = new ConcurrentHashMap<>(); // block's merkle root hash -> Block object

    private Map<String, Block> merkleRootBlockMap = new ConcurrentHashMap<>(); // block's merkle root hash -> Block object 

    private Map<String, String> reverseMerkleTree = new ConcurrentHashMap<>(); // transaction hash -> block's merkle root hash

    private Map<String, ClientResponse.Status> clientResponsesStatus = new ConcurrentHashMap<>(); // client request hash -> response status

    private ArrayList<Block> blockchain = new ArrayList<>();

    private ProcessConfig config = null;
    private ClientServiceWrapper clientService = null;
    private NodeServiceWrapper nodeService = null;
    private BlockBuilderService blockBuilderService = null;

    public LedgerService(LedgerServiceWrapper selfWrapper, ProcessConfig selfConfig, ProcessConfig[] clientConfigs, ProcessConfig[] nodeConfigs, 
        ClientServiceWrapper clientService, NodeServiceWrapper nodeService, BlockBuilderService blockBuilderService) {
        
        this.self = selfWrapper;
        this.config = selfConfig;
        this.clientService = clientService;
        this.nodeService = nodeService;
        this.blockBuilderService = blockBuilderService;
        for (ProcessConfig clientConfig : clientConfigs) {
            clientAccounts.put(clientConfig.getId(), new Account(clientConfig.getId()));
        }
        for (ProcessConfig validator : nodeConfigs) {
            validatorAccounts.put(validator.getId(), new ValidatorAccount(validator.getId()));
        }
        this.setLedgerOnClientService();
        this.setLedgerOnNodeService();
    }

    public void setLedgerOnClientService(){
        this.clientService.setLedgerService(self);
    }

    public void setLedgerOnNodeService(){
        this.nodeService.setLedgerService(self);
    }

    public void init() throws Exception {
        if (clientAccounts.isEmpty() || clientService == null || nodeService == null) {
            throw new Exception("All accounts, ClientService and NodeService must be set before calling init.");
        }
        // Create the genesis block
        Block genesisBlock = new Block(clientAccounts.keys());
        genesisBlock.setMerkleTree();
        blockchain.add(genesisBlock);

        nodeService.listen();
        clientService.listen();
    }

    public void addAllAccounts(ProcessConfig[] clientConfigs) {
        for (ProcessConfig config : clientConfigs) {
            clientAccounts.put(config.getId(), new Account(config.getId()));
        }
    }

    public void addAllValidatorAccounts(ProcessConfig[] nodeConfigs) {
        for (ProcessConfig config : nodeConfigs) {
            validatorAccounts.put(config.getId(), new ValidatorAccount(config.getId()));
        }
    }

    public double getBalance(String id) throws AccountNotFoundException {
        if (!clientAccounts.containsKey(id) && !validatorAccounts.containsKey(id)) {
            throw new AccountNotFoundException(MessageFormat.format("Account {0} not found", id));
        }
        Account account = clientAccounts.get(id);
        if (account == null) {
            account = validatorAccounts.get(id);
        }
        double balance = Double.valueOf(-1);

        synchronized (account) {
            balance = account.getBalance();
        }

        return balance;
    }

    public void performTransfer(String senderId, String receiverId, double amount) throws AccountNotFoundException, InsufficientFundsException {
        if (!clientAccounts.containsKey(senderId)) {
            throw new AccountNotFoundException(MessageFormat.format("Account {0} not found", senderId));
        }
        if (!clientAccounts.containsKey(receiverId) && !clientAccounts.get(receiverId).canReceiveFromClients()) {
            throw new AccountNotFoundException(MessageFormat.format("Account {0} not found", receiverId));
        }

        Account sender = clientAccounts.get(senderId);
        Account receiver = clientAccounts.get(receiverId);

        // no need to acquire lock on receiver since it will only increment
        // if the receiver is transfering money {B} (that relies on this transfer {A}) concurrently:
        // 1. (A did not happen yet) B won't have sufficient funds to transfer
        // 2. (A happened) B will have sufficient funds to transfer
        LOGGER.log(Level.INFO, MessageFormat.format("{0} - Transferring {1} from {2} to {3}", config.getId(), amount, senderId, receiverId));
        synchronized (sender) {
            sender.decrementBalance(amount);
            receiver.incrementBalance(amount);
        }

    }

    public boolean existsSender(TransferRequest request, Set<String> clientIds){
        return clientIds.contains(request.getSender());
    }

    public boolean existsReceiver(TransferRequest request, Set<String> clientIds){
        return clientIds.contains(request.getReceiver());
    }

    public boolean diffRecvSend(TransferRequest request){
        return !request.getSender().equals(request.getReceiver());
    }

    public boolean positiveAmount(TransferRequest request){
        return request.getAmount() > 0;
    }

    public boolean positiveFee(TransferRequest request){
        return request.getFee() > 0;
    }

    @Override
    public TransferResponse transfer(TransferRequest request) {
        Map<String, ProcessConfig> clientProcesses = clientService.getConfigs();

        String requestHash = request.digest();

        if (!existsSender(request, clientProcesses.keySet())){  
            TransferResponse response = new TransferResponse(TransferResponse.Status.BAD_SOURCE, requestHash);
            return response;
        }

        if (!existsReceiver(request, clientProcesses.keySet())){  
            TransferResponse response = new TransferResponse(TransferResponse.Status.BAD_DESTINATION, requestHash);
            return response;
        }
        
        if (!diffRecvSend(request)){
            TransferResponse response = new TransferResponse(TransferResponse.Status.BAD_DESTINATION, requestHash);
            return response;
        }

        if (!positiveAmount(request)){
            TransferResponse response = new TransferResponse(TransferResponse.Status.NO_AMOUNT, requestHash);
            return response;
        }

        if(!positiveFee(request)){
            TransferResponse response = new TransferResponse(TransferResponse.Status.NO_FEE, requestHash);
            return response;
        }

        LOGGER.log(Level.INFO, MessageFormat.format("{0} - Received valid transfer request: {1}", config.getId(), request.toJson()));

        Transaction transaction = new Transaction(request);
        if (!blockBuilderService.addTransaction(transaction)) {
            TransferResponse response = new TransferResponse(TransferResponse.Status.DENIED, requestHash);
            return response;
        }

        LOGGER.log(Level.INFO, MessageFormat.format("{0} - Transaction has been added to the pool: {1}", config.getId(), transaction.toString()));

        Block block = blockBuilderService.buildBlock();
        if (block != null) {
            LOGGER.log(Level.INFO, MessageFormat.format("{0} - Block built: {1}", config.getId(), block.toJson()));
            merkleRootBlockMap.put(block.getMerkleRootHash(), block);
            nodeService.reachConsensus(block.toJson());
        }

        synchronized (transaction) {
            // wait for transaction to be included in the blockchain
            try {
                transaction.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        String merkleRoot = reverseMerkleTree.get(transaction.digest()); // get the merkle root block hash where the transaction is included
        if (merkleRoot == null) {
            TransferResponse response = new TransferResponse(TransferResponse.Status.DENIED, requestHash);
            return response;
        }
        block = merkleRootBlockMap.get(merkleRoot);

        TransferResponse response = new TransferResponse(
            clientResponsesStatus.get(requestHash),
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

    @Override
    public BalanceResponse balance(BalanceRequest request) {

        String requestHash = request.digest();

        if(!clientAccounts.containsKey(request.getTarget()) && !validatorAccounts.containsKey(request.getTarget())){  
            BalanceResponse response = new BalanceResponse(ClientResponse.Status.ACCOUNT_NOT_FOUND, requestHash, request.getTarget());
            return response;
        }

        BalanceResponse response = new BalanceResponse(ClientResponse.Status.OK, requestHash, request.getTarget());
        try {
            response.setBalance(getBalance(request.getTarget()));
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
        if (merkleRootBlockMap.containsKey(blockKey)) {
            // Block is already created
            LOGGER.log(Level.INFO, MessageFormat.format("{0} - Block already created: {1}", config.getId(), blockKey));
            block = merkleRootBlockMap.get(blockKey);
        } else {
            // Block is new
            LOGGER.log(Level.INFO, MessageFormat.format("{0} - Block not created yet: {1}", config.getId(), blockKey));
            merkleRootBlockMap.put(blockKey, block);
        }
        block.addAllCommitMessages(commitMessages); // no need to verify since we trust ourselves

        for (Block bl : this.blockchain) {
            if (block.equals(bl)) {
                return;
            }
        }

        for (Transaction transaction : block.getTransactions()) {
            synchronized (transaction) {
                reverseMerkleTree.put(transaction.digest(), blockKey);
                TransferRequest request = transaction.getTransferRequest();
                String requestHash = request.digest();
                
                // Deduct fee
                try {
                    LOGGER.log(Level.INFO, MessageFormat.format("{0} - Deducting fee from account {1}", config.getId(), request.getSender()));
                    try {
                        Account sender = clientAccounts.get(request.getSender());
                        if (sender == null) {
                            throw new AccountNotFoundException(MessageFormat.format("Account {0} not found", request.getSender()));
                        }
                        sender.decrementBalance(request.getFee());
                    } catch (InsufficientFundsException e) {
                        LOGGER.log(Level.INFO, MessageFormat.format("{0} - {1}", config.getId(), e.getMessage()));
                        // Sender does not have enough funds to pay the fee -> transaction is ineffective
                        validatorAccounts.get(block.getCreator()).boundedDeduct(request.getFee());
                        // TODO: If the proposer doesn't have enough funds, will be blacklisted from proposing other blocks for t time
                        throw e;
                    }

                    LOGGER.log(Level.INFO, MessageFormat.format("{0} - Processing transaction: {1}", config.getId(), request.toJson()));
                    blockBuilderService.removeTransaction(transaction);
                    performTransfer(request.getSender(), request.getReceiver(), request.getAmount());

                    // half of the fee goes to the creator of the block
                    validatorAccounts.get(block.getCreator()).incrementBalance(request.getFee()/2);
                    // other half of the fee is distributed to all validators
                    distributeFeeAndAvoid(request.getFee()/2, block.getCreator());


                    clientResponsesStatus.put(requestHash, ClientResponse.Status.OK);
                } catch (AccountNotFoundException e) {
                    LOGGER.log(Level.INFO, MessageFormat.format("{0} - {1}", config.getId(), e.getMessage()));
                    clientResponsesStatus.put(requestHash, ClientResponse.Status.ACCOUNT_NOT_FOUND);
                } catch (InsufficientFundsException e) {
                    LOGGER.log(Level.INFO, MessageFormat.format("{0} - {1}", config.getId(), e.getMessage()));
                    
                    // half of the fee goes to other validators
                    distributeFeeAndAvoid(request.getFee()/2, block.getCreator());
                    // other half of the fee is burnt

                    clientResponsesStatus.put(requestHash, ClientResponse.Status.INSUFFICIENT_FUNDS);
                }
                transaction.notify();
            }
        }

        synchronized (blockchain) {
            block.setPreviousBlock(blockchain.get(blockchain.size() - 1));
            block.setChainHash();
            blockchain.add(block);
        }
    }

    private void distributeFeeAndAvoid(double fee, String avoidId) {
        int numValidators = validatorAccounts.size() - 1; // excluding avoidId
        validatorAccounts.values().stream()
            .filter(v -> !v.getId().equals(avoidId))
            .forEach(v -> v.incrementBalance(fee/numValidators));
    }
}
