package pt.ulisboa.tecnico.hdsledger.communication.consensus;

import pt.ulisboa.tecnico.hdsledger.communication.SharableMessage;
import pt.ulisboa.tecnico.hdsledger.utilities.Block;

public class PrepareMessage extends SharableMessage {
    
    // Value
    private Block block;

    public PrepareMessage(Block block) {
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }

}   
