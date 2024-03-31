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
    private String lastPreparedSerializedValue = null;

    public RoundChangeMessage(Integer consensusInstance, Integer round) {
        this.consensusInstance = consensusInstance;
        this.round = round;
    }

    public RoundChangeMessage(Integer consensusInstance, Integer round, Integer lastPreparedRound, String lastPreparedSerializedValue) {
        this.consensusInstance = consensusInstance;
        this.round = round;
        this.lastPreparedRound = lastPreparedRound;
        this.lastPreparedSerializedValue = lastPreparedSerializedValue;
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

    public String getLastPreparedSerializedValue(){
        return this.lastPreparedSerializedValue;
    }

    public void setLastPreparedRound(Integer lastPreparedRound){
        this.lastPreparedRound = lastPreparedRound;
    }
        
    public void setLastPreparedValue(String lastPreparedSerializedValue){
        this.lastPreparedSerializedValue = lastPreparedSerializedValue;
    }

}
