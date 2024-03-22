package pt.ulisboa.tecnico.hdsledger.communication.consensus;

import pt.ulisboa.tecnico.hdsledger.communication.SharableMessage;
import pt.ulisboa.tecnico.hdsledger.utilities.Block;

public class CommitMessage extends SharableMessage {

    // Value
    private Block block;

    public CommitMessage(Block block) {
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }

}
