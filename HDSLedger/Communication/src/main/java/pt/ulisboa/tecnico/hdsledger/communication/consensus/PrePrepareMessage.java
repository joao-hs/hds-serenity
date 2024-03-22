package pt.ulisboa.tecnico.hdsledger.communication.consensus;

import pt.ulisboa.tecnico.hdsledger.communication.SharableMessage;
import pt.ulisboa.tecnico.hdsledger.utilities.Block;

public class PrePrepareMessage extends SharableMessage {
    
    // Value
    private Block block;

    public PrePrepareMessage(Block block) {
        this.block = block;
    }

    public Block getValue() {
        return block;
    }

}   
