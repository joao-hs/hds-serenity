package pt.ulisboa.tecnico.hdsledger.communication.consensus;

import pt.ulisboa.tecnico.hdsledger.communication.SharableMessage;
import pt.ulisboa.tecnico.hdsledger.communication.interfaces.ConsensusMessageInterface;

public class GetContextMessage extends SharableMessage implements ConsensusMessageInterface {

    private String serializedHashValue;

    public GetContextMessage(String serializedHashValue) {
        this.serializedHashValue = serializedHashValue;
    }

    public String getSerializedHashValue() {
        return serializedHashValue;
    }

}   
