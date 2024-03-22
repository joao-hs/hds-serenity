package pt.ulisboa.tecnico.hdsledger.communication.consensus;

import pt.ulisboa.tecnico.hdsledger.communication.SharableMessage;
import pt.ulisboa.tecnico.hdsledger.utilities.ConsensusValue;

public class RoundChange extends SharableMessage {
    
    // Consensus instance
    private Integer consensusInstance;
    // Round
    private Integer round;
    // Round that has been prepared
    private Integer lastPreparedRound = null;
    // Value that has been prepared
    private ConsensusValue lastPreparedValue = null;

    public RoundChange(Integer consensusInstance, Integer round) {
        this.consensusInstance = consensusInstance;
        this.round = round;
    }

    public RoundChange(Integer consensusInstance, Integer round, Integer lastPreparedRound, ConsensusValue lastPreparedValue) {
        this.consensusInstance = consensusInstance;
        this.round = round;
        this.lastPreparedRound = lastPreparedRound;
        this.lastPreparedValue = lastPreparedValue;
    }

    public Integer getConsensusInstance() {
        return consensusInstance;
    }

    public Integer getRound() {
        return round;
    }

    public Integer getLastPreparedRound(){
        return this.lastPreparedRound;
    }

    public ConsensusValue getLastPreparedValue(){
        return this.lastPreparedValue;
    }

    public void setLastPreparedRound(Integer lastPreparedRound){
        this.lastPreparedRound = lastPreparedRound;
    }
        
    public void setLastPreparedValue(ConsensusValue lastPreparedValue){
        this.lastPreparedValue = lastPreparedValue;
    }

}
