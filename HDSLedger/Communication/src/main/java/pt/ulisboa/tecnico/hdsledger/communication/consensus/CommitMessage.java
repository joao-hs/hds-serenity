package pt.ulisboa.tecnico.hdsledger.communication.consensus;

import pt.ulisboa.tecnico.hdsledger.communication.SharableMessage;
import pt.ulisboa.tecnico.hdsledger.utilities.ConsensusValue;

public class CommitMessage extends SharableMessage {

    private ConsensusValue value;

    public CommitMessage(ConsensusValue value) {
        this.value = value;
    }

    public ConsensusValue getValue() {
        return value;
    }

}
