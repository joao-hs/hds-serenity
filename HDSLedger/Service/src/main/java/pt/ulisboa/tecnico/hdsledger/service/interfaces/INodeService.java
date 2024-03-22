package pt.ulisboa.tecnico.hdsledger.service.interfaces;

import pt.ulisboa.tecnico.hdsledger.utilities.ConsensusValue;

public interface INodeService {

    /*
     * Trigger the consensus algorithm to reach consensus on a value
     */
    public void reachConsensus(ConsensusValue value);
}
