package pt.ulisboa.tecnico.hdsledger.communication.consensus;

import com.google.gson.annotations.Expose;

import pt.ulisboa.tecnico.hdsledger.communication.SharableMessage;
import pt.ulisboa.tecnico.hdsledger.communication.interfaces.ConsensusMessageInterface;

public class PrePrepareMessage extends SharableMessage implements ConsensusMessageInterface {
    
    @Expose
    private String serializedValue;

    public PrePrepareMessage(String serializedValue) {
        this.serializedValue = serializedValue;
    }

    public String getSerializedValue() {
        return serializedValue;
    }

}   
