package pt.ulisboa.tecnico.hdsledger.communication.consensus;

import java.util.Objects;

import com.google.gson.annotations.Expose;

import pt.ulisboa.tecnico.hdsledger.communication.SharableMessage;
import pt.ulisboa.tecnico.hdsledger.communication.interfaces.ConsensusMessageInterface;

public class CommitMessage extends SharableMessage implements ConsensusMessageInterface {

    @Expose
    private String serializedHashValue;

    public CommitMessage(String serializedHashValue) {
        this.serializedHashValue = serializedHashValue;
    }

    public String getSerializedHashValue() {
        return serializedHashValue;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCreator(), getSignature(), serializedHashValue);
    }
}
