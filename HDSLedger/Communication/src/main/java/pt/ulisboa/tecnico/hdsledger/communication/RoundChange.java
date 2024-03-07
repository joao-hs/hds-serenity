package pt.ulisboa.tecnico.hdsledger.communication;

public class RoundChange extends Message {
    
    // Consensus instance
    private int consensusInstance;
    // Round to change to
    private int round;
    // Round that has been prepared
    private int lastPreparedRound = -1;
    // Value that has been prepared
    private String lastPreparedValue = null;

    public RoundChange(String senderId, Type type) {
        super(senderId, type);
    }

    

}
