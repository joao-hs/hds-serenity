package pt.ulisboa.tecnico.hdsledger.communication.consensus;

import com.google.gson.annotations.Expose;

import pt.ulisboa.tecnico.hdsledger.communication.SharableMessage;
import pt.ulisboa.tecnico.hdsledger.communication.interfaces.ConsensusMessageInterface;

public class PrepareMessage extends SharableMessage implements ConsensusMessageInterface {

    @Expose
    private String serializedValue;

    public PrepareMessage(String serializedValue) {
        this.serializedValue = serializedValue;
    }

    public String getSerializedValue() {
        return serializedValue;
    }

}   
