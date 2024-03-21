package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

import pt.ulisboa.tecnico.hdsledger.utilities.Block;

public class CommitMessage {

    // Value
    private Block block;

    public CommitMessage(Block block) {
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}
