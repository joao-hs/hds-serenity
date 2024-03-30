package pt.ulisboa.tecnico.hdsledger.service.services;

import java.util.Map;

import pt.ulisboa.tecnico.hdsledger.communication.LinkWrapper;
import pt.ulisboa.tecnico.hdsledger.communication.client.BalanceRequest;
import pt.ulisboa.tecnico.hdsledger.communication.client.ClientRequest;
import pt.ulisboa.tecnico.hdsledger.communication.client.TransferRequest;
import pt.ulisboa.tecnico.hdsledger.service.interfaces.IClientService;
import pt.ulisboa.tecnico.hdsledger.service.interfaces.UDPService;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;


public abstract class ClientServiceWrapper implements UDPService, IClientService{

    private ClientService clientService;

    protected Map<String, String> additionalInfo;

    public ClientServiceWrapper(LinkWrapper link, ProcessConfig config, ProcessConfig[] clientsConfigs) {
        this.clientService = new ClientService(this, link, config, clientsConfigs);
        this.additionalInfo = config.getAdditionalInfo();
    }

    public ProcessConfig getConfig() {
        return this.clientService.getConfig();
    }

    public void setLedgerService(LedgerServiceWrapper ledger){
        this.clientService.setLedgerService(ledger);
    }

    public Map<String, ProcessConfig> getConfigs(){
        return this.clientService.getConfigs();
    }

    public boolean isFresh(String issuer, TransferRequest request) {
        return this.clientService.isFresh(issuer,request);
    }

    public boolean hasValidSignature(ClientRequest request) {
        return this.clientService.hasValidSignature(request);    
    }

    public boolean existsClient(String issuer) {
        return this.clientService.existsClient(issuer);
    }

    public boolean isSenderValid(String issuer, TransferRequest request) {
        return this.clientService.isSenderValid(issuer, request);
    }

    public synchronized void uponTransfer(String issuer, TransferRequest request) {
        this.clientService.uponTransfer(issuer, request);
    }

    public void uponBalance(String issuer, BalanceRequest request) {
        this.clientService.uponBalance(issuer, request);
    }


    @Override
    public void listen() {
        this.clientService.listen();
    }
}
