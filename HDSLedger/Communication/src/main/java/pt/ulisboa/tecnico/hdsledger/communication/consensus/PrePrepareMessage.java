package pt.ulisboa.tecnico.hdsledger.communication.consensus;

import pt.ulisboa.tecnico.hdsledger.communication.SharableMessage;
import pt.ulisboa.tecnico.hdsledger.utilities.ConsensusValue;

public class PrePrepareMessage extends SharableMessage {
    
    // Value
    private ConsensusValue value;

    public PrePrepareMessage(ConsensusValue value) {
        this.value = value;
    }

    public ConsensusValue getValue() {
        return value;
    }

}   
