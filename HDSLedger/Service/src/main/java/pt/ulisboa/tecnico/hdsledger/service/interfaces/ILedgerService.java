package pt.ulisboa.tecnico.hdsledger.service.interfaces;

import java.util.Collection;

import pt.ulisboa.tecnico.hdsledger.communication.client.BalanceRequest;
import pt.ulisboa.tecnico.hdsledger.communication.client.BalanceResponse;
import pt.ulisboa.tecnico.hdsledger.communication.client.TransferRequest;
import pt.ulisboa.tecnico.hdsledger.communication.client.TransferResponse;
import pt.ulisboa.tecnico.hdsledger.communication.consensus.CommitMessage;

public interface ILedgerService {
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
