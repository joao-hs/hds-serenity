package pt.ulisboa.tecnico.hdsledger.communication.consensus;

import com.google.gson.Gson;

import pt.ulisboa.tecnico.hdsledger.utilities.Block;

public class PrepareMessage {
    
    // Value
    private Block block;

    public PrepareMessage(Block block) {
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}   
