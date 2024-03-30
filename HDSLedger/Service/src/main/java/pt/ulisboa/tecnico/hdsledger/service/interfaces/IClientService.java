package pt.ulisboa.tecnico.hdsledger.service.interfaces;

import java.util.Map;

import pt.ulisboa.tecnico.hdsledger.communication.client.BalanceRequest;
import pt.ulisboa.tecnico.hdsledger.communication.client.ClientRequest;
import pt.ulisboa.tecnico.hdsledger.communication.client.TransferRequest;
import pt.ulisboa.tecnico.hdsledger.service.services.LedgerServiceWrapper;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public interface IClientService {

    public ProcessConfig getConfig();
    
    public void setLedgerService(LedgerServiceWrapper ledger);

    public Map<String, ProcessConfig> getConfigs();

    public boolean isFresh(String issuer, TransferRequest request);
        
    public boolean hasValidSignature(ClientRequest request);

    public boolean existsClient(String issuer);

    public boolean isSenderValid(String issuer, TransferRequest request);

    public void uponTransfer(String issuer, TransferRequest request);

    public void uponBalance(String issuer, BalanceRequest request);
}
