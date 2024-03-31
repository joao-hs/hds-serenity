package pt.ulisboa.tecnico.hdsledger.communication.consensus;

import pt.ulisboa.tecnico.hdsledger.communication.SharableMessage;
import pt.ulisboa.tecnico.hdsledger.communication.interfaces.ConsensusMessageInterface;

public class ContextMessage extends SharableMessage implements ConsensusMessageInterface {
    
    private String serializedValue;

    public ContextMessage(String serializedValue) {
        this.serializedValue = serializedValue;
    }

    public String getSerializedValue() {
        return serializedValue;
    }

}   
