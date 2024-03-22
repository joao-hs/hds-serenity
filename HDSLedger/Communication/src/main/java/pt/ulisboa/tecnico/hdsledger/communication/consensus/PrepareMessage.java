package pt.ulisboa.tecnico.hdsledger.communication.consensus;

import pt.ulisboa.tecnico.hdsledger.communication.SharableMessage;
import pt.ulisboa.tecnico.hdsledger.utilities.ConsensusValue;

public class PrepareMessage extends SharableMessage {

    private ConsensusValue value;

    public PrepareMessage(ConsensusValue value) {
        this.value = value;
    }

    public ConsensusValue getValue() {
        return value;
    }

}   
