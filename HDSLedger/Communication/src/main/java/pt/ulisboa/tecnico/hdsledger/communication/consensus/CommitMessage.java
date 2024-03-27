package pt.ulisboa.tecnico.hdsledger.communication.consensus;

import java.util.Objects;

import pt.ulisboa.tecnico.hdsledger.communication.SharableMessage;
import pt.ulisboa.tecnico.hdsledger.communication.interfaces.ConsensusMessageInterface;

public class CommitMessage extends SharableMessage implements ConsensusMessageInterface {

    private String serializedValue;

    public CommitMessage(String serializedValue) {
        this.serializedValue = serializedValue;
    }

    public String getSerializedValue() {
        return serializedValue;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCreator(), getSignature(), serializedValue);
    }
}
