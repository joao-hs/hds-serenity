package pt.ulisboa.tecnico.hdsledger.communication.consensus;

import com.google.gson.Gson;

import pt.ulisboa.tecnico.hdsledger.utilities.Block;

public class PrePrepareMessage {
    
    // Value
    private Block block;

    public PrePrepareMessage(Block block) {
        this.block = block;
    }

    public Block getValue() {
        return block;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}   
