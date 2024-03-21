package pt.ulisboa.tecnico.hdsledger.service.interfaces;

import pt.ulisboa.tecnico.hdsledger.utilities.Block;
import pt.ulisboa.tecnico.hdsledger.utilities.Transaction;

public interface IBlockBuilderService {
    
    /*
     * Builds a block with the transactions in the transaction pool
     * @return null if the available fee is below the fee threshold, the block otherwise
     */
    public Block buildBlock();

    /*
     * Adds a transaction to the transaction pool.
     * The block will be built with the transactions in the pool
     * @return true if the transaction was added, false otherwise
     */
    public boolean addTransaction(Transaction transaction);

    /*
     * Removes a transaction from the transaction pool
     * @return true if the transaction was removed, false otherwise
     */
    public boolean removeTransaction(Transaction transaction);

    /*
     * Sets the filters for the transactions that will be added to the block
     */
    public void setTransactionFilters(int minFee, int maxFee, int feeThreshold, float minAmountFeeRation);

    /*
     * @return if the transaction is acceptable by this node
     */
    public boolean isTransactionValid(Transaction transaction);
}
