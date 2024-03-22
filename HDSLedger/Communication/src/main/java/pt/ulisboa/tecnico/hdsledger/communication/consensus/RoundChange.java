package pt.ulisboa.tecnico.hdsledger.communication.consensus;

import pt.ulisboa.tecnico.hdsledger.communication.SharableMessage;
import pt.ulisboa.tecnico.hdsledger.utilities.Block;

public class RoundChange extends SharableMessage {
    
    // Consensus instance
    private Integer consensusInstance;
    // Round
    private Integer round;
    // Round that has been prepared
    private Integer lastPreparedRound = null;
    // Value that has been prepared
    private Block lastPreparedBlock = null;

    public RoundChange(Integer consensusInstance, Integer round) {
        this.consensusInstance = consensusInstance;
        this.round = round;
    }

    public RoundChange(Integer consensusInstance, Integer round, Integer lastPreparedRound, Block lastPreparedBlock) {
        this.consensusInstance = consensusInstance;
        this.round = round;
        this.lastPreparedRound = lastPreparedRound;
        this.lastPreparedBlock = lastPreparedBlock;
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

    public Block getLastPreparedBlock(){
        return this.lastPreparedBlock;
    }

    public void setLastPreparedRound(Integer lastPreparedRound){
        this.lastPreparedRound = lastPreparedRound;
    }
        
    public void setLastPreparedValue(Block lastPreparedBlock){
        this.lastPreparedBlock = lastPreparedBlock;
    }

}
