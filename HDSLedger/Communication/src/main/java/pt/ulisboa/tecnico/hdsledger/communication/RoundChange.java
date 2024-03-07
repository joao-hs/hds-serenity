package pt.ulisboa.tecnico.hdsledger.communication;

public class RoundChange extends ConsensusMessage {
    
    // Round that has been prepared
    private Integer lastPreparedRound = -1;
    // Value that has been prepared
    private String lastPreparedValue = null;

    public RoundChange(String senderId, Type type) {
        super(senderId, type);
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
}
