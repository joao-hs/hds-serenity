package pt.ulisboa.tecnico.hdsledger.service.services;

import java.util.Collection;
import java.util.Map;

import pt.ulisboa.tecnico.hdsledger.communication.client.BalanceRequest;
import pt.ulisboa.tecnico.hdsledger.communication.client.BalanceResponse;
import pt.ulisboa.tecnico.hdsledger.communication.client.TransferRequest;
import pt.ulisboa.tecnico.hdsledger.communication.client.TransferResponse;
import pt.ulisboa.tecnico.hdsledger.communication.consensus.CommitMessage;
import pt.ulisboa.tecnico.hdsledger.service.interfaces.ILedgerService;
import pt.ulisboa.tecnico.hdsledger.utilities.AccountNotFoundException;
import pt.ulisboa.tecnico.hdsledger.utilities.InsufficientFundsException;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public class LedgerServiceWrapper implements ILedgerService {

    private LedgerService ledgerService;

    protected Map<String, String> additionalInfo;

    private static LedgerServiceWrapper instance = null;

    public LedgerServiceWrapper(LedgerService instance, ProcessConfig nodeConfig,ProcessConfig[] clientConfigs
    ,ClientService clientService,NodeService nodeService,BlockBuilderService blockBuilderService) throws Exception {
        this.ledgerService.getInstance();
        this.ledgerService.setConfig(nodeConfig);
        this.ledgerService.addAllAccounts(clientConfigs);
        this.ledgerService.setClientService(clientService);
        this.ledgerService.setNodeService(nodeService);
        this.ledgerService.setBlockBuilderService(blockBuilderService);
        this.ledgerService.init();
        this.additionalInfo = nodeConfig.getAdditionalInfo();
    }

    public static LedgerService getInstance() {
        if (instance == null) {
            instance = new LedgerService();
        }
        return instance;
    }

    public void setConfig(ProcessConfig config) {
        this.ledgerService.setConfig(config);
    }

    public void setClientService(ClientService clientService) {
        this.ledgerService.setClientService(clientService);
    }

    public void setNodeService(NodeService nodeService) {
        this.ledgerService.setNodeService(nodeService);
    }

    public void setBlockBuilderService(BlockBuilderService blockBuilderService) {
        this.ledgerService.setBlockBuilderService(blockBuilderService);
    }

    public void init() throws Exception {
        this.ledgerService.init();
    }

    public void addAllAccounts(ProcessConfig[] clientConfigs) {
        this.ledgerService.addAllAccounts(clientConfigs);
    }

    public int getBalance(String id) throws AccountNotFoundException {
        return this.ledgerService.getBalance(id);
    }

    public void performTransfer(String senderId, String receiverId, int amount) throws AccountNotFoundException, InsufficientFundsException {
        this.ledgerService.performTransfer(senderId, receiverId, amount);
    }

    @Override
    public TransferResponse transfer(TransferRequest request) {
        return this.ledgerService.transfer(request);
    }

    @Override
    public BalanceResponse balance(BalanceRequest request) {
        return this.ledgerService.balance(request);

    }

    @Override
    public synchronized void uponConsensusReached(String serializedValue, Collection<CommitMessage> commitMessages) {
        this.ledgerService.uponConsensusReached(serializedValue, commitMessages);
    }
}
