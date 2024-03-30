package pt.ulisboa.tecnico.hdsledger.service.services;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import pt.ulisboa.tecnico.hdsledger.communication.client.BalanceRequest;
import pt.ulisboa.tecnico.hdsledger.communication.client.BalanceResponse;
import pt.ulisboa.tecnico.hdsledger.communication.client.TransferRequest;
import pt.ulisboa.tecnico.hdsledger.communication.client.TransferResponse;
import pt.ulisboa.tecnico.hdsledger.communication.consensus.CommitMessage;
import pt.ulisboa.tecnico.hdsledger.service.interfaces.ILedgerService;
import pt.ulisboa.tecnico.hdsledger.utilities.AccountNotFoundException;
import pt.ulisboa.tecnico.hdsledger.utilities.InsufficientFundsException;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public abstract class LedgerServiceWrapper implements ILedgerService {

    private LedgerService ledgerService;

    protected Map<String, String> additionalInfo;

    public LedgerServiceWrapper(LedgerServiceWrapper instance, ProcessConfig nodeConfig,ProcessConfig[] clientConfigs
    ,ClientServiceWrapper clientService,NodeServiceWrapper nodeService,BlockBuilderService blockBuilderService) throws Exception {
        this.ledgerService = new LedgerService(instance,nodeConfig,clientConfigs,clientService,nodeService,blockBuilderService);
    }

    public void setLedgerOnClientService(){
        this.ledgerService.setLedgerOnClientService();
    }

    public void setLedgerOnNodeService(){
        this.ledgerService.setLedgerOnNodeService();
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

    public boolean existsSender(TransferRequest request, Set<String> clientIds){
        return this.ledgerService.existsSender(request, clientIds);
    }

    public boolean existsReceiver(TransferRequest request, Set<String> clientIds){
        return this.ledgerService.existsReceiver(request, clientIds);
    }

    public boolean diffRecvSend(TransferRequest request){
        return this.ledgerService.diffRecvSend(request);
    }

    public boolean positiveAmount(TransferRequest request){
        return this.ledgerService.positiveAmount(request);
    }

    public boolean positiveFee(TransferRequest request){
        return this.ledgerService.positiveFee(request);
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
