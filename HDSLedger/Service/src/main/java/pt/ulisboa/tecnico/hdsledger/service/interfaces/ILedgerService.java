package pt.ulisboa.tecnico.hdsledger.service.interfaces;

import pt.ulisboa.tecnico.hdsledger.communication.BalanceRequest;
import pt.ulisboa.tecnico.hdsledger.communication.BalanceResponse;
import pt.ulisboa.tecnico.hdsledger.communication.TransferRequest;
import pt.ulisboa.tecnico.hdsledger.communication.TransferResponse;
import pt.ulisboa.tecnico.hdsledger.utilities.Block;

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
     * This method is called when consensus is reached for a specific block.
     * It is responsible for handling the block after consensus is reached.
     *
     * @param block The block for which consensus has been reached.
     */
    public void uponConsensusReached(Block block);
}
