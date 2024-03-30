package pt.ulisboa.tecnico.hdsledger.service.interfaces;

import java.util.Collection;
import java.util.Set;

import pt.ulisboa.tecnico.hdsledger.communication.client.BalanceRequest;
import pt.ulisboa.tecnico.hdsledger.communication.client.BalanceResponse;
import pt.ulisboa.tecnico.hdsledger.communication.client.TransferRequest;
import pt.ulisboa.tecnico.hdsledger.communication.client.TransferResponse;
import pt.ulisboa.tecnico.hdsledger.communication.consensus.CommitMessage;
import pt.ulisboa.tecnico.hdsledger.utilities.AccountNotFoundException;
import pt.ulisboa.tecnico.hdsledger.utilities.InsufficientFundsException;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public interface ILedgerService {

    public void setLedgerOnClientService();

    public void setLedgerOnNodeService();

    public void init() throws Exception;

    public void addAllAccounts(ProcessConfig[] clientConfigs);

    public int getBalance(String id) throws AccountNotFoundException;

    public void performTransfer(String senderId, String receiverId, int amount) throws AccountNotFoundException, InsufficientFundsException;

    public boolean existsSender(TransferRequest request, Set<String> clientIds);

    public boolean existsReceiver(TransferRequest request, Set<String> clientIds);

    public boolean diffRecvSend(TransferRequest request);

    public boolean positiveAmount(TransferRequest request);

    public boolean positiveFee(TransferRequest request);

    /**
     * Transfer money from one account to another
     * Blocks until the transfer is completed
     * @return the response of the transfer
     */
    public TransferResponse transfer(TransferRequest request);

    /**
     * Get the balance of an account
     * Blocks until the balance is received
     * @return the response of the balance request
     */
    public BalanceResponse balance(BalanceRequest request);

    /**
     * This method is called when consensus is reached for a specific value.
     * It is responsible for handling the value after consensus is reached.
     *
     * @param value the value for which consensus was reached (needs to be a block)
     */
    public void uponConsensusReached(String serializedValue, Collection<CommitMessage> commitMessages);
}
