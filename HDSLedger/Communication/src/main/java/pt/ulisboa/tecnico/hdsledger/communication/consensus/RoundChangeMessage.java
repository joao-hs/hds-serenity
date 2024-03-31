package pt.ulisboa.tecnico.hdsledger.communication.consensus;

import com.google.gson.annotations.Expose;

import pt.ulisboa.tecnico.hdsledger.communication.SharableMessage;
import pt.ulisboa.tecnico.hdsledger.communication.interfaces.ConsensusMessageInterface;

public class RoundChangeMessage extends SharableMessage implements ConsensusMessageInterface {
    
    // Consensus instance
    @Expose
    private Integer consensusInstance;
    // Round
    @Expose
    private Integer round;
    // Round that has been prepared
    @Expose
    private Integer lastPreparedRound = null;
    // Value that has been prepared
    @Expose
    private String lastPreparedSerializedHashValue = null;

    public RoundChangeMessage(Integer consensusInstance, Integer round) {
        this.consensusInstance = consensusInstance;
        this.round = round;
    }

    public RoundChangeMessage(Integer consensusInstance, Integer round, Integer lastPreparedRound, String lastPreparedSerializedHashValue) {
        this.consensusInstance = consensusInstance;
        this.round = round;
        this.lastPreparedRound = lastPreparedRound;
        this.lastPreparedSerializedHashValue = lastPreparedSerializedHashValue;
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

    public String getLastPreparedSerializedHashValue(){
        return this.lastPreparedSerializedHashValue;
    }

    public void setLastPreparedRound(Integer lastPreparedRound){
        this.lastPreparedRound = lastPreparedRound;
    }
        
    public void setLastPreparedHashValue(String lastPreparedSerializedHashValue){
        this.lastPreparedSerializedHashValue = lastPreparedSerializedHashValue;
    }

}
