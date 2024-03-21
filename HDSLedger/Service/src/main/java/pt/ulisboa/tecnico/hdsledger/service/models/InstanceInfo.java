package pt.ulisboa.tecnico.hdsledger.service.models;


import com.google.gson.GsonBuilder;

import pt.ulisboa.tecnico.hdsledger.communication.CommitMessage;
import pt.ulisboa.tecnico.hdsledger.utilities.Block;

public class InstanceInfo {

    private int currentRound = 1;
    private int preparedRound = -1;
    private Block preparedBlock;
    private CommitMessage commitMessage;
    private Block block;
    private int committedRound = -1;

    public InstanceInfo(Block block) {
        this.block = block;
    }

    public InstanceInfo(int currentRound, Block block) {
        this.currentRound = currentRound;
        this.block = block;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(int currentRound) {
        this.currentRound = currentRound;
    }

    public void incrementRound() {
        currentRound++;
    }

    public int getPreparedRound() {
        return preparedRound;
    }

    public void setPreparedRound(int preparedRound) {
        this.preparedRound = preparedRound;
    }

    public Block getPreparedBlock() {
        return preparedBlock;
    }

    public void setPreparedValue(Block preparedBlock) {
        this.preparedBlock = preparedBlock;
    }

    public Block getBlock() {
        return block;
    }

    public void setBlock(Block block) {
        this.block = block;
    }

    public int getCommittedRound() {
        return committedRound;
    }

    public void setCommittedRound(int committedRound) {
        this.committedRound = committedRound;
    }

    public CommitMessage getCommitMessage() {
        return commitMessage;
    }

    public void setCommitMessage(CommitMessage commitMessage) {
        this.commitMessage = commitMessage;
    }

    @Override
    public String toString() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }
}
