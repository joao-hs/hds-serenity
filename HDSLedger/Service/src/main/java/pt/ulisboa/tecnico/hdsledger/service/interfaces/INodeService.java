package pt.ulisboa.tecnico.hdsledger.service.interfaces;

import pt.ulisboa.tecnico.hdsledger.utilities.Block;

public interface INodeService {

    /*
     * Trigger the consensus algorithm to reach consensus on the block
     */
    public void reachConsensus(Block block);
}
