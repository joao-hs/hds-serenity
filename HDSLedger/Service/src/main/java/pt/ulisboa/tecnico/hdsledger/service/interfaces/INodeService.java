package pt.ulisboa.tecnico.hdsledger.service.interfaces;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;

import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.communication.consensus.RoundChangeMessage;
import pt.ulisboa.tecnico.hdsledger.service.models.InstanceInfo;
import pt.ulisboa.tecnico.hdsledger.service.services.LedgerServiceWrapper;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public interface INodeService {

    public ProcessConfig getConfig();

    public void setLedgerService(LedgerServiceWrapper ledger);

    public Map<String, ProcessConfig> getConfigs();

    public int getConsensusInstance();

    public boolean isLeader(String id, int consensusInstance);

    public ProcessConfig getLeader(int consensusInstance);

    public boolean isConsensusValueValid(String serializedValue);

    public ConsensusMessage createConsensusMessage(String serializedValue, int instance, int round);
    
    public void startTimer(int consensusInstance);

    /*
     * Start an instance of consensus for a value
     * Only the current leader will start a consensus instance
     * the remaining nodes only update values.
     *
     * @param inputValue Value to value agreed upon
     */
    public void startConsensus(String serializedValue);

    public Optional<Pair<Integer, String>> highestPrepared(List<RoundChangeMessage> quorumRoundChange);

    public boolean justifyPrePrepare(int instanceId, int round);

    public boolean justifyRoundChange(int instanceId, InstanceInfo instanceInfo, List<RoundChangeMessage> quorumRoundChange);

    /*
     * Handle pre prepare messages and if the message
     * came from leader and is justified them broadcast prepare
     *
     * @param message Message to be handled
     */
    public void uponPrePrepare(ConsensusMessage message);
    /*
     * Handle prepare messages and if there is a valid quorum broadcast commit
     *
     * @param message Message to be handled
     */
    public void uponPrepare(ConsensusMessage message);


    /*
     * Handle commit messages and decide if there is a valid quorum
     *
     * @param message Message to be handled
     */
    public void uponCommit(ConsensusMessage message);

    /*
     * Handle round change request -> ROUND-CHANGE(consensusInstanceId, roundToChangeTo, lastPreparedRound?, lastPreparedValue?) 
     *                                                                                      [? means it can be null]
     * upon receiving f+1 round change requests, the node will broadcast a round change request
     *    why: at least one correct node is asking to changing round
     *    ROUND-CHANGE(consensusInstanceId, min(roundToChangeTo), lastPreparedRound?, lastPreparedValue?) -- last two values are associated with the second
     * upon receiving 2f+1 round change requests, 
     *    why: a majority of correct nodes are asking to change round
     */
    public void uponRoundChangeRequest(ConsensusMessage message);

    public void uponGetContext(ConsensusMessage message);

    public void uponReceivedContext(ConsensusMessage message);
    /*
     * Trigger the consensus algorithm to reach consensus on a value
     */
    public void reachConsensus(String serializedValue);
}
