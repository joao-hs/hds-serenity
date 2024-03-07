package pt.ulisboa.tecnico.hdsledger.communication;

public class RoundChange extends ConsensusMessage {
    
    // Round that has been prepared
    private int lastPreparedRound = -1;
    // Value that has been prepared
    private String lastPreparedValue = null;

    public RoundChange(String senderId, Type type) {
        super(senderId, type);
    }

    public int getLastPreparedRound(){
        return this.lastPreparedRound;
    }

    public String getLastPreparedValue(){
        return this.lastPreparedValue;
    }

    public void setLastPreparedRound(int lastPreparedRound){
        this.lastPreparedRound = lastPreparedRound;
    }
        
    public void setLastPreparedValue(int lastPreparedValue){
        this.lastPreparedValue = lastPreparedValue;
    }
}
