package pt.ulisboa.tecnico.hdsledger.communication.consensus;

import com.google.gson.annotations.Expose;

import pt.ulisboa.tecnico.hdsledger.communication.SharableMessage;
import pt.ulisboa.tecnico.hdsledger.communication.interfaces.ConsensusMessageInterface;

public class PrepareMessage extends SharableMessage implements ConsensusMessageInterface {

    @Expose
    private String serializedHashValue;

    public PrepareMessage(String serializedHashValue) {
        this.serializedHashValue = serializedHashValue;
    }

    public String getSerializedHashValue() {
        return serializedHashValue;
    }

}   
