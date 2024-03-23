package pt.ulisboa.tecnico.hdsledger.service.interfaces;

public interface INodeService {

    /*
     * Trigger the consensus algorithm to reach consensus on a value
     */
    public void reachConsensus(String serializedValue);
}
