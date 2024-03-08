package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

public class RoundChange {
    
    // Consensus instance
    private Integer consensusInstance;
    // Round
    private Integer round;
    // Round that has been prepared
    private Integer lastPreparedRound = null;
    // Value that has been prepared
    private String lastPreparedValue = null;

    public RoundChange(Integer consensusInstance, Integer round) {
        this.consensusInstance = consensusInstance;
        this.round = round;
    }

    public RoundChange(Integer consensusInstance, Integer round, Integer lastPreparedRound, String lastPreparedValue) {
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

    public String getLastPreparedValue(){
        return this.lastPreparedValue;
    }

    public void setLastPreparedRound(Integer lastPreparedRound){
        this.lastPreparedRound = lastPreparedRound;
    }
        
    public void setLastPreparedValue(String lastPreparedValue){
        this.lastPreparedValue = lastPreparedValue;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}
